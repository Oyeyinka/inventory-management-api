package com.example.inventory.management.inventory_management.model;

import java.math.BigDecimal;

public class ConversionResult {
    private BigDecimal convertedAmount;
    private BigDecimal fxRate;

    public ConversionResult(BigDecimal convertedAmount, BigDecimal fxRate) {
        this.convertedAmount = convertedAmount;
        this.fxRate = fxRate;
    }

    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }

    public BigDecimal getFxRate() {
        return fxRate;
    }
}

