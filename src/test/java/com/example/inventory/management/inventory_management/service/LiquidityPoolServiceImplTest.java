package com.example.inventory.management.inventory_management.service;

import com.example.inventory.management.inventory_management.config.LiquidityPoolConfig;
import com.example.inventory.management.inventory_management.constant.Constants;
import com.example.inventory.management.inventory_management.entity.FXRate;
import com.example.inventory.management.inventory_management.entity.LiquidityPool;
import com.example.inventory.management.inventory_management.entity.OverflowUnderflowTransaction;
import com.example.inventory.management.inventory_management.model.response.Response;
import com.example.inventory.management.inventory_management.repository.FXRateRepository;
import com.example.inventory.management.inventory_management.repository.LiquidityPoolRepository;
import com.example.inventory.management.inventory_management.repository.OverflowUnderflowTransactionRepository;
import com.example.inventory.management.inventory_management.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiquidityPoolServiceImplTest {

    @Mock
    private LiquidityPoolRepository poolRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OverflowUnderflowTransactionRepository overflowUnderflowTransactionRepository;

    @Mock
    private LiquidityPoolConfig liquidityPoolConfig;

    @Mock
    private FXRateRepository fxRateRepository;

    @InjectMocks
    private LiquidityPoolServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Mockito.lenient().when(liquidityPoolConfig.getDefaultBalances()).thenReturn(Map.of(
                "USD", new BigDecimal("1000000"),
                "EUR", new BigDecimal("921658"),
                "JPY", new BigDecimal("109890110"),
                "GBP", new BigDecimal("750000"),
                "AUD", new BigDecimal("1349528")
        ));

        LiquidityPoolConfig.ThresholdMultipliers thresholdMultipliers = new LiquidityPoolConfig.ThresholdMultipliers();
        thresholdMultipliers.setUpper(BigDecimal.valueOf(1.2));
        thresholdMultipliers.setLower(BigDecimal.valueOf(0.8));
        Mockito.lenient().when(liquidityPoolConfig.getThresholdMultipliers()).thenReturn(thresholdMultipliers);

        Mockito.lenient().when(poolRepository.findByCurrency("USD")).thenReturn(Optional.of(new LiquidityPool("USD", new BigDecimal("1000000"), new BigDecimal("1200000"), new BigDecimal("800000"))));
        Mockito.lenient().when(poolRepository.findByCurrency("EUR")).thenReturn(Optional.of(new LiquidityPool("EUR", new BigDecimal("921658"), new BigDecimal("1105989.6"), new BigDecimal("737326.4"))));

        Mockito.lenient().when(transactionRepository.calculateRecentVolume(anyString(), any(LocalDateTime.class))).thenReturn(BigDecimal.valueOf(50000));
        Mockito.lenient().when(fxRateRepository.findByCurrencyPair(anyString())).thenReturn(Optional.of(new FXRate("USD/EUR", BigDecimal.valueOf(0.85), LocalDateTime.now())));
    }


    @Test
    void shouldInitializeDefaultPoolsIfNotExist() {
        when(poolRepository.findByCurrency("USD")).thenReturn(Optional.empty());
        when(poolRepository.findByCurrency("EUR")).thenReturn(Optional.empty());
        when(poolRepository.findByCurrency("JPY")).thenReturn(Optional.empty());
        when(poolRepository.findByCurrency("AUD")).thenReturn(Optional.empty());
        when(poolRepository.findByCurrency("GBP")).thenReturn(Optional.empty());

        service.initializeDefaultPools();

        verify(poolRepository, times(1)).save(argThat(pool -> "USD".equals(pool.getCurrency())));
        verify(poolRepository, times(1)).save(argThat(pool -> "EUR".equals(pool.getCurrency())));
        verify(poolRepository, times(1)).save(argThat(pool -> "JPY".equals(pool.getCurrency())));
        verify(poolRepository, times(1)).save(argThat(pool -> "AUD".equals(pool.getCurrency())));
        verify(poolRepository, times(1)).save(argThat(pool -> "GBP".equals(pool.getCurrency())));

    }

    @Test
    void shouldUpdateFXRateSuccessfully() {
        String currencyPair = "USD/EUR";
        BigDecimal rate = BigDecimal.valueOf(0.85);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        ResponseEntity<Response<String>> response = service.updateFXRate(currencyPair, rate, timestamp);

        verify(fxRateRepository, times(1)).save(any(FXRate.class));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.RESPONSE_STATUS_SUCCESS, response.getBody().getStatus());
    }

    @Test
    void shouldReturnErrorResponseWhenFXRateUpdateFails() {
        String currencyPair = "USD/EUR";
        BigDecimal rate = BigDecimal.valueOf(0.85);
        String invalidTimestamp = "invalid-date";

        ResponseEntity<Response<String>> response = service.updateFXRate(currencyPair, rate, invalidTimestamp);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(Constants.RESPONSE_STATUS_ERROR, response.getBody().getStatus());
    }

    @Test
    void shouldRebalanceLiquidityPoolsAndHandleOverflow() {
        LiquidityPool pool = new LiquidityPool("USD", BigDecimal.valueOf(1300000), BigDecimal.valueOf(1200000), BigDecimal.valueOf(800000));
        when(poolRepository.findAll()).thenReturn(List.of(pool));
        when(poolRepository.findLowBalanceCurrency("USD", PageRequest.of(0, 1))).thenReturn(List.of("EUR"));
        when(fxRateRepository.findByCurrencyPair("USD/EUR")).thenReturn(Optional.of(new FXRate("USD/EUR", BigDecimal.valueOf(0.85), LocalDateTime.now())));

        service.rebalanceLiquidityPools();

        verify(poolRepository, times(1)).saveAll(anyList());
        verify(overflowUnderflowTransactionRepository, times(1)).save(any(OverflowUnderflowTransaction.class));
    }

    @Test
    void shouldRebalanceLiquidityPoolsAndHandleUnderflow() {
        LiquidityPool pool = new LiquidityPool("EUR", BigDecimal.valueOf(300000), BigDecimal.valueOf(600000), BigDecimal.valueOf(400000));
        when(poolRepository.findAll()).thenReturn(List.of(pool));
        when(poolRepository.findHighBalanceCurrency("EUR", PageRequest.of(0, 1))).thenReturn(List.of("USD"));
        when(fxRateRepository.findByCurrencyPair("USD/EUR")).thenReturn(Optional.of(new FXRate("USD/EUR", BigDecimal.valueOf(0.85), LocalDateTime.now())));

        service.rebalanceLiquidityPools();

        verify(poolRepository, times(1)).saveAll(anyList());
        verify(overflowUnderflowTransactionRepository, times(1)).save(any(OverflowUnderflowTransaction.class));
    }

    @Test
    void shouldRebalanceLiquidityPoolsAndTriggerUnderflowHandling() {
        LiquidityPool pool = new LiquidityPool("EUR", BigDecimal.valueOf(300000), BigDecimal.valueOf(600000), BigDecimal.valueOf(400000));
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);

        when(poolRepository.findAll()).thenReturn(List.of(pool));
        when(transactionRepository.calculateRecentVolume("EUR", startTime)).thenReturn(BigDecimal.valueOf(30000));
        when(poolRepository.findHighBalanceCurrency("EUR", PageRequest.of(0, 1))).thenReturn(List.of("USD"));
        when(fxRateRepository.findByCurrencyPair("USD/EUR")).thenReturn(Optional.of(new FXRate("USD/EUR", BigDecimal.valueOf(1.15), LocalDateTime.now())));

        service.rebalanceLiquidityPools();

        verify(poolRepository, times(1)).saveAll(anyList());
        verify(overflowUnderflowTransactionRepository, times(1)).save(any(OverflowUnderflowTransaction.class));
    }
}
