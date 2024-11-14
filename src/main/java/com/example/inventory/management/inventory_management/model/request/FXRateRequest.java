package com.example.inventory.management.inventory_management.model.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class FXRateRequest {

    @NotNull
    private String pair;

    @NotNull
    private BigDecimal rate;

    @NotNull
    private String timestamp;

    public String getPair() {
        return pair;
    }

    public void setPair(String pair) {
        this.pair = pair;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "FXRateRequest{" +
                "pair='" + pair + '\'' +
                ", rate=" + rate +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
