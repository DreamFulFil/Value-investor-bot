package com.valueinvestor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Application configuration properties mapped from application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "")
public class AppConfig {

    private Markets markets;
    private Investment investment;
    private Trading trading;
    private Llm llm;
    private Shioaji shioaji;

    // Getters and Setters

    public Markets getMarkets() {
        return markets;
    }

    public void setMarkets(Markets markets) {
        this.markets = markets;
    }

    public Investment getInvestment() {
        return investment;
    }

    public void setInvestment(Investment investment) {
        this.investment = investment;
    }

    public Trading getTrading() {
        return trading;
    }

    public void setTrading(Trading trading) {
        this.trading = trading;
    }

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public Shioaji getShioaji() {
        return shioaji;
    }

    public void setShioaji(Shioaji shioaji) {
        this.shioaji = shioaji;
    }

    // Convenience methods
    public java.math.BigDecimal getMonthlyInvestment() {
        double amount = investment != null ? investment.getMonthlyAmountTwd() : 16000.0;
        return java.math.BigDecimal.valueOf(amount);
    }

    public int getTargetWeeklyTwd() {
        return investment != null ? investment.getTargetWeeklyTwd() : 1600;
    }

    public com.valueinvestor.model.entity.TransactionLog.TradingMode getTradingMode() {
        String mode = trading != null ? trading.getMode() : "SIMULATION";
        try {
            return com.valueinvestor.model.entity.TransactionLog.TradingMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return com.valueinvestor.model.entity.TransactionLog.TradingMode.SIMULATION;
        }
    }

    public List<String> getWatchlist() {
        // Return empty list for now - can be configured in application.yml if needed
        return List.of();
    }

    public static class Markets {
        private List<String> enabled;
        private String primary;

        public List<String> getEnabled() {
            return enabled;
        }

        public void setEnabled(List<String> enabled) {
            this.enabled = enabled;
        }

        public String getPrimary() {
            return primary;
        }

        public void setPrimary(String primary) {
            this.primary = primary;
        }
    }

    public static class Investment {
        private int monthlyAmountTwd = 16000;
        private int targetWeeklyTwd = 1600;

        public int getMonthlyAmountTwd() {
            return monthlyAmountTwd;
        }

        public void setMonthlyAmountTwd(int monthlyAmountTwd) {
            this.monthlyAmountTwd = monthlyAmountTwd;
        }

        public int getTargetWeeklyTwd() {
            return targetWeeklyTwd;
        }

        public void setTargetWeeklyTwd(int targetWeeklyTwd) {
            this.targetWeeklyTwd = targetWeeklyTwd;
        }
    }

    public static class Trading {
        private String mode;
        private boolean startFromZero;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public boolean isStartFromZero() {
            return startFromZero;
        }

        public void setStartFromZero(boolean startFromZero) {
            this.startFromZero = startFromZero;
        }
    }

    public static class Llm {
        private String provider;
        private String model;
        private String baseUrl;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Shioaji {
        private String pythonPath;
        private String apiKey;
        private String secretKey;

        public String getPythonPath() {
            return pythonPath;
        }

        public void setPythonPath(String pythonPath) {
            this.pythonPath = pythonPath;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }
}
