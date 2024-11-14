package com.example.inventory.management.inventory_management.service;

import com.example.inventory.management.inventory_management.model.response.TransferResponse;

import java.math.BigDecimal;

public interface TransferService {
    TransferResponse processTransfer(String fromCurrency, String toCurrency, BigDecimal amount);
}
