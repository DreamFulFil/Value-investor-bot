package com.valueinvestor.config;

import com.valueinvestor.service.TradingConfigService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * Application configuration - delegates to dedicated @ConfigurationProperties classes
 * and TradingConfigService for DB-driven trading mode.
 */
@Configuration
public class AppConfig {

    private final InvestmentProperties investmentProperties;
    private final TradingProperties tradingProperties;
    private final TradingConfigService tradingConfigService;

    public AppConfig(InvestmentProperties investmentProperties, 
                     TradingProperties tradingProperties,
                     @Lazy TradingConfigService tradingConfigService) {
        this.investmentProperties = investmentProperties;
        this.tradingProperties = tradingProperties;
        this.tradingConfigService = tradingConfigService;
    }

    public java.math.BigDecimal getMonthlyInvestment() {
        return java.math.BigDecimal.valueOf(investmentProperties.getMonthlyAmountTwd());
    }

    public int getTargetWeeklyTwd() {
        return investmentProperties.getTargetWeeklyTwd();
    }

    /**
     * Get trading mode - ALWAYS reads from database first.
     * Database LIVE mode overrides application.yml SIMULATION mode.
     */
    public com.valueinvestor.model.entity.TransactionLog.TradingMode getTradingMode() {
        // Database takes precedence - once LIVE is in DB, it's permanent
        if (tradingConfigService != null) {
            try {
                return tradingConfigService.getTradingMode();
            } catch (Exception e) {
                // Fall back to properties if DB not ready
            }
        }
        
        // Fallback to application.yml
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
