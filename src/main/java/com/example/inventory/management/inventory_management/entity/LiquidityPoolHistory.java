package com.example.inventory.management.inventory_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class LiquidityPoolHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "liquidity_pool_id", nullable = false)
    private LiquidityPool liquidityPool;

    private BigDecimal balance;
    private BigDecimal amountChanged;
    private String eventType;
    private LocalDateTime timestamp;

    public LiquidityPoolHistory() {}

    public LiquidityPoolHistory(LiquidityPool liquidityPool, BigDecimal balance, BigDecimal amountChanged, String eventType) {
        this.liquidityPool = liquidityPool;
        this.balance = balance;
        this.amountChanged = amountChanged;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LiquidityPool getLiquidityPool() {
        return liquidityPool;
    }

    public void setLiquidityPool(LiquidityPool liquidityPool) {
        this.liquidityPool = liquidityPool;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAmountChanged() {
        return amountChanged;
    }

    public void setAmountChanged(BigDecimal amountChanged) {
        this.amountChanged = amountChanged;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
