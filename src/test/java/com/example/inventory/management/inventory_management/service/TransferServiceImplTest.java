package com.example.inventory.management.inventory_management.service;

import com.example.inventory.management.inventory_management.config.SettlementConfig;
import com.example.inventory.management.inventory_management.entity.FXRate;
import com.example.inventory.management.inventory_management.entity.LiquidityPool;
import com.example.inventory.management.inventory_management.entity.Transaction;
import com.example.inventory.management.inventory_management.model.response.TransferResponse;
import com.example.inventory.management.inventory_management.repository.FXRateRepository;
import com.example.inventory.management.inventory_management.repository.LiquidityPoolRepository;
import com.example.inventory.management.inventory_management.repository.RevenueRepository;
import com.example.inventory.management.inventory_management.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferServiceImplTest {

    @InjectMocks
    private TransferServiceImpl transferService;

    @Mock
    private SettlementConfig settlementConfig;

    @Mock
    private FXRateRepository fxRateRepository;

    @Mock
    private LiquidityPoolRepository poolRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RevenueRepository revenueRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(transferService, "marginPercentage", BigDecimal.valueOf(0.1));
    }

    @Test
    void shouldProcessTransferSuccessfully() {
        FXRate fxRate = new FXRate("USD/EUR", BigDecimal.valueOf(0.85), null);
        LiquidityPool fromPool = new LiquidityPool("USD", BigDecimal.valueOf(1000), BigDecimal.valueOf(1200), BigDecimal.valueOf(800));
        LiquidityPool toPool = new LiquidityPool("EUR", BigDecimal.valueOf(500), BigDecimal.valueOf(700), BigDecimal.valueOf(300));
        Transaction savedTransaction = new Transaction("USD", "EUR", BigDecimal.valueOf(100), BigDecimal.valueOf(85), BigDecimal.valueOf(8.5), BigDecimal.valueOf(8.5));
        savedTransaction.setId(1L);

        when(fxRateRepository.findByCurrencyPair("USD/EUR")).thenReturn(Optional.of(fxRate));
        when(poolRepository.findByCurrency("USD")).thenReturn(Optional.of(fromPool));
        when(poolRepository.findByCurrency("EUR")).thenReturn(Optional.of(toPool));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(settlementConfig.getSettlementTime("EUR")).thenReturn(0);

        TransferResponse response = transferService.processTransfer("USD", "EUR", BigDecimal.valueOf(100));

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Transfer completed successfully", response.getMessage());

        verify(fxRateRepository).findByCurrencyPair("USD/EUR");
        verify(poolRepository, times(2)).save(any(LiquidityPool.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void shouldFailTransferDueToInsufficientFunds() {
        FXRate fxRate = new FXRate("USD/EUR", BigDecimal.valueOf(0.85), null);
        LiquidityPool fromPool = new LiquidityPool("USD", BigDecimal.valueOf(50), BigDecimal.valueOf(1200), BigDecimal.valueOf(800)); // Low balance
        LiquidityPool toPool = new LiquidityPool("EUR", BigDecimal.valueOf(500), BigDecimal.valueOf(700), BigDecimal.valueOf(300)); // Sufficient balance, if needed

        when(fxRateRepository.findByCurrencyPair("USD/EUR")).thenReturn(Optional.of(fxRate));
        when(poolRepository.findByCurrency("USD")).thenReturn(Optional.of(fromPool));
        when(poolRepository.findByCurrency("EUR")).thenReturn(Optional.of(toPool));

        TransferResponse response = transferService.processTransfer("USD", "EUR", BigDecimal.valueOf(100));

        assertNotNull(response);
        assertEquals("error", response.getStatus());
        assertTrue(response.getMessage().contains("Insufficient funds"));

        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
        verifyNoMoreInteractions(revenueRepository);
    }

    @Test
    void shouldFailTransferDueToFXRateNotFound() {
        when(fxRateRepository.findByCurrencyPair("USD/EUR")).thenReturn(Optional.empty());

        TransferResponse response = transferService.processTransfer("USD", "EUR", BigDecimal.valueOf(100));

        assertNotNull(response);
        assertEquals("error", response.getStatus());
        assertTrue(response.getMessage().contains("FX Rate not available"));

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }
}