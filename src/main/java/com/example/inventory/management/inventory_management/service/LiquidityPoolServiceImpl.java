package com.example.inventory.management.inventory_management.service;

import com.example.inventory.management.inventory_management.config.LiquidityPoolConfig;
import com.example.inventory.management.inventory_management.constant.Constants;
import com.example.inventory.management.inventory_management.entity.FXRate;
import com.example.inventory.management.inventory_management.entity.LiquidityPool;
import com.example.inventory.management.inventory_management.entity.OverflowUnderflowTransaction;
import com.example.inventory.management.inventory_management.model.ConversionResult;
import com.example.inventory.management.inventory_management.model.response.Response;
import com.example.inventory.management.inventory_management.repository.FXRateRepository;
import com.example.inventory.management.inventory_management.repository.LiquidityPoolRepository;
import com.example.inventory.management.inventory_management.repository.OverflowUnderflowTransactionRepository;
import com.example.inventory.management.inventory_management.repository.TransactionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LiquidityPoolServiceImpl implements LiquidityPoolService {

    private static final Logger logger = LoggerFactory.getLogger(LiquidityPoolServiceImpl.class);

    @Autowired
    private LiquidityPoolRepository poolRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OverflowUnderflowTransactionRepository overflowUnderflowTransactionRepository;

    @Autowired
    private LiquidityPoolConfig liquidityPoolConfig;

    @Autowired
    private FXRateRepository fxRateRepository;

    @PostConstruct
    public void initializeDefaultPools() {
        Map<String, BigDecimal> defaultBalances = liquidityPoolConfig.getDefaultBalances();

        defaultBalances.forEach((currency, initialBalance) -> {
            if (poolRepository.findByCurrency(currency).isEmpty()) {
                BigDecimal upperThreshold = initialBalance.multiply(liquidityPoolConfig.getThresholdMultipliers().getUpper());
                BigDecimal lowerThreshold = initialBalance.multiply(liquidityPoolConfig.getThresholdMultipliers().getLower());

                LiquidityPool newPool = new LiquidityPool(currency, initialBalance, upperThreshold, lowerThreshold);
                poolRepository.save(newPool);

                logger.info("Initialized liquidity pool for {} with balance: {}", currency, initialBalance);
            }
        });
    }

    public ResponseEntity<Response<String>> updateFXRate(String currencyPair, BigDecimal rate, String timestamp) {
        try {
            LocalDateTime parsedTimestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
            rate = rate.setScale(4, RoundingMode.HALF_UP);

            logger.info("Updating FX rate: [currencyPair={}, rate={}, timestamp={}]", currencyPair, rate, parsedTimestamp);
            FXRate newRateEntry = new FXRate(currencyPair, rate, parsedTimestamp);
            fxRateRepository.save(newRateEntry);

            logger.info("FX rate successfully updated and saved: [currencyPair={}, rate={}, timestamp={}]", currencyPair, rate, parsedTimestamp);

            Response<String> response = new Response<>(Constants.RESPONSE_STATUS_SUCCESS, Constants.FX_RATE_UPDATE_SUCCESS_MESSAGE);
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Failed to update FX rate for [currencyPair={}, rate={}, timestamp={}]: {}", currencyPair, rate, timestamp, ex.getMessage(), ex);

            Response<String> response = new Response<>(Constants.RESPONSE_STATUS_ERROR, Constants.FX_RATE_INVALID_DATA_ERROR);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
    }

    @Scheduled(fixedDelayString = "${rebalance.frequency.ms}")
    public void rebalanceLiquidityPools() {
        logger.info("Starting adaptive rebalancing check for liquidity pools");
        List<LiquidityPool> pools = poolRepository.findAll();

        pools.forEach(pool -> {
            BigDecimal currentBalance = pool.getBalance();

            BigDecimal upperThreshold = calculateDynamicUpperThreshold(pool);
            BigDecimal lowerThreshold = calculateDynamicLowerThreshold(pool);

            logger.info("Currency: {}, Current Balance: {}, Upper Threshold: {}, Lower Threshold: {}",
                    pool.getCurrency(), currentBalance, upperThreshold, lowerThreshold);

            if (currentBalance.compareTo(upperThreshold) > 0) {
                BigDecimal overflowAmount = currentBalance.subtract(upperThreshold);
                handleOverflow(pool, overflowAmount);
            } else if (currentBalance.compareTo(lowerThreshold) < 0) {
                BigDecimal underflowAmount = lowerThreshold.subtract(currentBalance);
                handleUnderflow(pool, underflowAmount);
            }
        });
        logger.info("Completed adaptive rebalancing check for liquidity pools");
    }

    private BigDecimal calculateDynamicUpperThreshold(LiquidityPool pool) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        BigDecimal recentVolume = transactionRepository.calculateRecentVolume(pool.getCurrency(), startTime);

        recentVolume = (recentVolume != null) ? recentVolume : BigDecimal.ZERO;

        BigDecimal upperThreshold = pool.getBaseUpperThreshold().add(recentVolume.multiply(BigDecimal.valueOf(0.2))); // example adjustment factor
        logger.info("Calculated upper threshold for {}: {}", pool.getCurrency(), upperThreshold);
        return upperThreshold;
    }

    private BigDecimal calculateDynamicLowerThreshold(LiquidityPool pool) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        BigDecimal recentVolume = transactionRepository.calculateRecentVolume(pool.getCurrency(), startTime);

        recentVolume = (recentVolume != null) ? recentVolume : BigDecimal.ZERO;

        BigDecimal lowerThreshold = pool.getBaseLowerThreshold().add(recentVolume.multiply(BigDecimal.valueOf(0.1))); // example adjustment factor
        logger.info("Calculated lower threshold for {}: {}", pool.getCurrency(), lowerThreshold);
        return lowerThreshold;
    }

    private void handleOverflow(LiquidityPool pool, BigDecimal overflowAmount) {
        String targetCurrency = determineTargetCurrencyForOverflow(pool);

        if (targetCurrency != null) {
            Optional<LiquidityPool> targetPoolOptional = poolRepository.findByCurrency(targetCurrency);

            if (targetPoolOptional.isPresent()) {
                ConversionResult conversionResult = convertCurrency(pool.getCurrency(), targetCurrency, overflowAmount);
                BigDecimal convertedAmount = conversionResult.getConvertedAmount();
                BigDecimal fxRate = conversionResult.getFxRate();

                LiquidityPool targetPool = targetPoolOptional.get();
                targetPool.credit(convertedAmount);
                pool.debit(overflowAmount);
                poolRepository.saveAll(Arrays.asList(pool, targetPool));

                logger.info("Rebalanced overflow for {}: removed {} and credited {} to {}",
                        pool.getCurrency(), overflowAmount, convertedAmount, targetCurrency);

                logRebalanceTransaction(pool.getCurrency(), targetCurrency, overflowAmount, convertedAmount, fxRate, "overflow");
            } else {
                logger.warn("Target liquidity pool not found for currency: {}", targetCurrency);
            }
        } else {
            logger.warn("No suitable target pool found for overflow from {}", pool.getCurrency());
        }
    }


    private void handleUnderflow(LiquidityPool pool, BigDecimal underflowAmount) {
        String sourceCurrency = determineSourceCurrencyForUnderflow(pool);

        if (sourceCurrency != null) {
            Optional<LiquidityPool> sourcePoolOptional = poolRepository.findByCurrency(sourceCurrency);

            if (sourcePoolOptional.isPresent()) {
                ConversionResult conversionResult = convertCurrency(sourceCurrency, pool.getCurrency(), underflowAmount);
                BigDecimal convertedAmount = conversionResult.getConvertedAmount();
                BigDecimal fxRate = conversionResult.getFxRate();

                LiquidityPool sourcePool = sourcePoolOptional.get();
                sourcePool.debit(convertedAmount);
                pool.credit(underflowAmount);
                poolRepository.saveAll(Arrays.asList(sourcePool, pool));

                logger.info("Rebalanced underflow for {}: added {} from {} pool", pool.getCurrency(), underflowAmount, sourceCurrency);

                logRebalanceTransaction(sourceCurrency, pool.getCurrency(), convertedAmount, underflowAmount, fxRate, "underflow");
            } else {
                logger.warn("Source liquidity pool not found for currency: {}", sourceCurrency);
            }
        } else {
            logger.warn("No suitable source pool found for underflow for {}", pool.getCurrency());
        }
    }

    private ConversionResult convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount) {
        String currencyPair = fromCurrency + "/" + toCurrency;

        FXRate fxRateEntity = fxRateRepository.findByCurrencyPair(currencyPair)
                .orElseThrow(() -> new IllegalArgumentException("FX rate not found for conversion: " + currencyPair));
        BigDecimal fxRate = fxRateEntity.getRate();

        BigDecimal convertedAmount = amount.multiply(fxRate).setScale(4, RoundingMode.HALF_UP);
        logger.info("Converted {} {} to {} {} using FX rate {}", amount, fromCurrency, convertedAmount, toCurrency, fxRate);

        return new ConversionResult(convertedAmount, fxRate);
    }


    private void logRebalanceTransaction(String fromCurrency, String toCurrency, BigDecimal originalAmount, BigDecimal convertedAmount, BigDecimal fxRate, String transactionType) {
        OverflowUnderflowTransaction transaction = new OverflowUnderflowTransaction(
                fromCurrency, toCurrency, originalAmount, convertedAmount, fxRate, LocalDateTime.now(), transactionType);
        overflowUnderflowTransactionRepository.save(transaction);

        logger.info("Logged {} transaction: {} to {}, original amount: {}, converted amount: {}, FX rate: {}",
                transactionType, fromCurrency, toCurrency, originalAmount, convertedAmount, fxRate);
    }

    private String determineTargetCurrencyForOverflow(LiquidityPool pool) {
        Pageable limitOne = PageRequest.of(0, 1);
        List<String> result = poolRepository.findLowBalanceCurrency(pool.getCurrency(), limitOne);
        return result.isEmpty() ? null : result.get(0);
    }

    private String determineSourceCurrencyForUnderflow(LiquidityPool pool) {
        Pageable limitOne = PageRequest.of(0, 1);
        List<String> result = poolRepository.findHighBalanceCurrency(pool.getCurrency(), limitOne);
        return result.isEmpty() ? null : result.get(0);
    }
}