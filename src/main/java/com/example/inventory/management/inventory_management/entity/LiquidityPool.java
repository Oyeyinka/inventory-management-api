package com.example.inventory.management.inventory_management.entity;

import com.example.inventory.management.inventory_management.exception.InsufficientFundsException;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class LiquidityPool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal initialBalance;

    // Base thresholds for calculating dynamic thresholds
    @Column(nullable = false)
    private BigDecimal baseUpperThreshold;

    @Column(nullable = false)
    private BigDecimal baseLowerThreshold;

    @Transient
    private BigDecimal upperThreshold;

    @Transient
    private BigDecimal lowerThreshold;

    public LiquidityPool() {}

    public LiquidityPool(String currency, BigDecimal initialBalance, BigDecimal baseUpperThreshold, BigDecimal baseLowerThreshold) {
        this.currency = currency;
        this.initialBalance = initialBalance;
        this.balance = initialBalance;
        this.baseUpperThreshold = baseUpperThreshold;
        this.baseLowerThreshold = baseLowerThreshold;
        this.upperThreshold = baseUpperThreshold;
        this.lowerThreshold = baseLowerThreshold;
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough liquidity in pool");
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public boolean isOverflow() {
        return balance.compareTo(upperThreshold) > 0;
    }

    public boolean isUnderflow() {
        return balance.compareTo(lowerThreshold) < 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public BigDecimal getBaseUpperThreshold() {
        return baseUpperThreshold;
    }

    public void setBaseUpperThreshold(BigDecimal baseUpperThreshold) {
        this.baseUpperThreshold = baseUpperThreshold;
    }

    public BigDecimal getBaseLowerThreshold() {
        return baseLowerThreshold;
    }

    public void setBaseLowerThreshold(BigDecimal baseLowerThreshold) {
        this.baseLowerThreshold = baseLowerThreshold;
    }

    public BigDecimal getUpperThreshold() {
        return upperThreshold;
    }

    public void setUpperThreshold(BigDecimal upperThreshold) {
        this.upperThreshold = upperThreshold;
    }

    public BigDecimal getLowerThreshold() {
        return lowerThreshold;
    }

    public void setLowerThreshold(BigDecimal lowerThreshold) {
        this.lowerThreshold = lowerThreshold;
    }
}