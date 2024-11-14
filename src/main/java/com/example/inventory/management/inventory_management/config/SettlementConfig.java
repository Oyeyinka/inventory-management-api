package com.example.inventory.management.inventory_management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "settlement")
public class SettlementConfig {

    private Map<String, Integer> times;

    public Map<String, Integer> getTimes() {
        return times;
    }

    public void setTimes(Map<String, Integer> times) {
        this.times = times;
    }

    public int getSettlementTime(String currency) {
        return times.getOrDefault(currency, times.getOrDefault("default", 0));
    }
}

