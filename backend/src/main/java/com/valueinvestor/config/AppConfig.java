package com.valueinvestor.config;

import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Application configuration - delegates to dedicated @ConfigurationProperties classes
 */
@Configuration
public class AppConfig {

    private final InvestmentProperties investmentProperties;
    private final TradingProperties tradingProperties;

    public AppConfig(InvestmentProperties investmentProperties, TradingProperties tradingProperties) {
        this.investmentProperties = investmentProperties;
        this.tradingProperties = tradingProperties;
    }

    public java.math.BigDecimal getMonthlyInvestment() {
        return java.math.BigDecimal.valueOf(investmentProperties.getMonthlyAmountTwd());
    }

    public int getTargetWeeklyTwd() {
        return investmentProperties.getTargetWeeklyTwd();
    }

    public com.valueinvestor.model.entity.TransactionLog.TradingMode getTradingMode() {
        String mode = tradingProperties.getMode();
        try {
            return com.valueinvestor.model.entity.TransactionLog.TradingMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return com.valueinvestor.model.entity.TransactionLog.TradingMode.SIMULATION;
        }
    }

    public List<String> getWatchlist() {
        return List.of();
    }
}
