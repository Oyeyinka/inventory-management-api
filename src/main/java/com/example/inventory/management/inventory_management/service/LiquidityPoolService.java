package com.example.inventory.management.inventory_management.service;

import com.example.inventory.management.inventory_management.model.response.Response;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

public interface LiquidityPoolService {
    ResponseEntity<Response<String>> updateFXRate(String currencyPair, BigDecimal rate, String timestamp);
}
