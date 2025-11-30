package com.valueinvestor.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "investment")
public class InvestmentProperties {

    private BigDecimal maxPositionSize = new BigDecimal("0.20"); // Default to 20%
    private int maxStocksPerSector = 2; // Default to 2

    public BigDecimal getMaxPositionSize() {
        return maxPositionSize;
    }

    public void setMaxPositionSize(BigDecimal maxPositionSize) {
        this.maxPositionSize = maxPositionSize;
    }

    public int getMaxStocksPerSector() {
        return maxStocksPerSector;
    }

    public void setMaxStocksPerSector(int maxStocksPerSector) {
        this.maxStocksPerSector = maxStocksPerSector;
    }
}
