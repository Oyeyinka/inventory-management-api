package com.example.inventory.management.inventory_management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "liquidity")
public class LiquidityPoolConfig {

    private Map<String, BigDecimal> defaultBalances;
    private ThresholdMultipliers thresholdMultipliers;

    public Map<String, BigDecimal> getDefaultBalances() {
        return defaultBalances;
    }

    public void setDefaultBalances(Map<String, BigDecimal> defaultBalances) {
        this.defaultBalances = defaultBalances;
    }

    public ThresholdMultipliers getThresholdMultipliers() {
        return thresholdMultipliers;
    }

    public void setThresholdMultipliers(ThresholdMultipliers thresholdMultipliers) {
        this.thresholdMultipliers = thresholdMultipliers;
    }

    public static class ThresholdMultipliers {
        private BigDecimal upper;
        private BigDecimal lower;

        public BigDecimal getUpper() {
            return upper;
        }

        public void setUpper(BigDecimal upper) {
            this.upper = upper;
        }

        public BigDecimal getLower() {
            return lower;
        }

        public void setLower(BigDecimal lower) {
            this.lower = lower;
        }
    }
}

