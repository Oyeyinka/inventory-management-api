package com.example.inventory.management.inventory_management.service;

import com.example.inventory.management.inventory_management.config.SettlementConfig;
import com.example.inventory.management.inventory_management.entity.FXRate;
import com.example.inventory.management.inventory_management.entity.LiquidityPool;
import com.example.inventory.management.inventory_management.entity.Revenue;
import com.example.inventory.management.inventory_management.entity.Transaction;
import com.example.inventory.management.inventory_management.exception.FXRateNotFoundException;
import com.example.inventory.management.inventory_management.exception.InsufficientFundsException;
import com.example.inventory.management.inventory_management.model.response.TransferResponse;
import com.example.inventory.management.inventory_management.repository.FXRateRepository;
import com.example.inventory.management.inventory_management.repository.LiquidityPoolRepository;
import com.example.inventory.management.inventory_management.repository.RevenueRepository;
import com.example.inventory.management.inventory_management.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class TransferServiceImpl implements TransferService {

    private final SettlementConfig settlementConfig;
    private final FXRateRepository fxRateRepository;
    private final LiquidityPoolRepository poolRepository;
    private final TransactionRepository transactionRepository;
    private static final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);

    @Value("${transaction.margin.percentage}")
    private BigDecimal marginPercentage;

    @Autowired
    private RevenueRepository revenueRepository;

    @Autowired
    public TransferServiceImpl(SettlementConfig settlementConfig,
                           FXRateRepository fxRateRepository,
                           LiquidityPoolRepository poolRepository,
                           TransactionRepository transactionRepository,
                           RevenueRepository revenueRepository) {
        this.settlementConfig = settlementConfig;
        this.fxRateRepository = fxRateRepository;
        this.poolRepository = poolRepository;
        this.transactionRepository = transactionRepository;
        this.revenueRepository = revenueRepository;
    }

    @Transactional
    public TransferResponse processTransfer(String fromCurrency, String toCurrency, BigDecimal amount) {
        logger.info("Initiating transfer from {} to {} with amount {}", fromCurrency, toCurrency, amount);

        Transaction transaction = null;
        try {
            FXRate fxRate = fxRateRepository.findByCurrencyPair(fromCurrency + "/" + toCurrency)
                    .orElseThrow(() -> new FXRateNotFoundException("FX Rate not available for " + fromCurrency + "/" + toCurrency));

            BigDecimal convertedAmount = amount.multiply(fxRate.getRate()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal margin = convertedAmount.multiply(marginPercentage).setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalDebit = convertedAmount.add(margin);

            LiquidityPool fromPool = poolRepository.findByCurrency(fromCurrency)
                    .orElseThrow(() -> new InsufficientFundsException("Liquidity not available for currency: " + fromCurrency));
            LiquidityPool toPool = poolRepository.findByCurrency(toCurrency)
                    .orElseThrow(() -> new InsufficientFundsException("Liquidity not available for currency: " + toCurrency));

            if (fromPool.getBalance().compareTo(totalDebit) < 0) {
                throw new InsufficientFundsException("Insufficient funds in " + fromCurrency + " pool");
            }

            fromPool.debit(totalDebit);
            poolRepository.save(fromPool);
            logger.info("Debited {} from {} pool", totalDebit, fromCurrency);

            transaction = new Transaction(fromCurrency, toCurrency, amount, convertedAmount, margin, margin);
            transaction.setStatus("PENDING");
            transactionRepository.save(transaction);

            int settlementTime = settlementConfig.getSettlementTime(toCurrency);
            if (settlementTime > 0) {
                logger.info("Applying settlement delay of {} seconds for {}", settlementTime, toCurrency);
                scheduleSettlement(toPool, convertedAmount, settlementTime, fromCurrency, toCurrency, amount, margin, transaction.getId());
            } else {
                toPool.credit(convertedAmount);
                poolRepository.save(toPool);
                transaction.setStatus("SETTLED");
                transactionRepository.save(transaction);
                logger.info("Credited {} to {} pool immediately", convertedAmount, toCurrency);
            }

            recordRevenue(transaction.getId(), toCurrency, margin);
            return createTransferResponse(fromCurrency, toCurrency, amount, convertedAmount, margin, totalDebit, "success", "Transfer completed successfully");

        } catch (FXRateNotFoundException | InsufficientFundsException ex) {
            logger.error("Transfer failed: {}", ex.getMessage());

            if (transaction == null) {
                transaction = new Transaction(fromCurrency, toCurrency, amount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);

            return createTransferResponse(fromCurrency, toCurrency, amount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "error", ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during transfer from {} to {}: {}", fromCurrency, toCurrency, ex.getMessage(), ex);
            if (transaction != null) {
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
            }
            return createTransferResponse(fromCurrency, toCurrency, amount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "error", "An unexpected error occurred during the transfer");
        }
    }

    private TransferResponse createTransferResponse(String fromCurrency, String toCurrency, BigDecimal originalAmount,
                                                    BigDecimal convertedAmount, BigDecimal margin, BigDecimal totalDebit,
                                                    String status, String message) {
        return new TransferResponse(
                fromCurrency,
                toCurrency,
                originalAmount,
                convertedAmount,
                margin,
                totalDebit,
                status,
                message
        );
    }

    private void scheduleSettlement(LiquidityPool toPool, BigDecimal convertedAmount, int settlementTime,
                                    String fromCurrency, String toCurrency, BigDecimal amount, BigDecimal margin, Long transactionId) {
        Runnable settlementTask = () -> {
            try {
                toPool.credit(convertedAmount);
                poolRepository.save(toPool);
                logger.info("Settled and credited {} to {} pool after delay of {} seconds. Original transfer amount: {}, Margin: {}",
                        convertedAmount, toCurrency, settlementTime, amount, margin);

                updateTransactionStatus(transactionId, "SETTLED");

            } catch (Exception e) {
                logger.error("Settlement failed for transaction from {} to {}: {}", fromCurrency, toCurrency, e.getMessage());

                updateTransactionStatus(transactionId, "FAILED");
            }
        };

        Executors.newSingleThreadScheduledExecutor().schedule(settlementTask, settlementTime, TimeUnit.SECONDS);
    }

    private void updateTransactionStatus(Long transactionId, String status) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for ID: " + transactionId));
        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }

    private void recordRevenue(Long transactionId, String currency, BigDecimal margin) {
        Revenue revenue = new Revenue(transactionId, currency, margin);
        revenueRepository.save(revenue);
        logger.info("Recorded revenue for transaction {}: {} {}", transactionId, currency, margin);
    }
}

