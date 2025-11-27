package com.valueinvestor.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.valueinvestor.model.entity.StockPriceHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShioajiDataService {

    private static final Logger logger = LoggerFactory.getLogger(ShioajiDataService.class);

    private final String shioajiApiUrl;
    private final RestTemplate restTemplate;
    private volatile boolean isAvailable = false;

    public ShioajiDataService(@Value("${shioaji.api-url:http://127.0.0.1:8888}") String shioajiApiUrl) {
        this.shioajiApiUrl = shioajiApiUrl;
        this.restTemplate = new RestTemplate();
        checkAvailability();
    }

    /**
     * Check if Shioaji API server is available
     */
    private void checkAvailability() {
        try {
            String url = shioajiApiUrl + "/health";
            HealthResponse response = restTemplate.getForObject(url, HealthResponse.class);
            isAvailable = response != null && response.connected;
            if (isAvailable) {
                logger.info("Shioaji API server is available at {}", shioajiApiUrl);
            } else {
                logger.warn("Shioaji API server is not connected");
            }
        } catch (Exception e) {
            isAvailable = false;
            logger.warn("Shioaji API server is not available at {}: {}", shioajiApiUrl, e.getMessage());
        }
    }

    /**
     * Check if Shioaji API is available
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Get current API quota status
     */
    public QuotaStatus getQuotaStatus() {
        try {
            String url = shioajiApiUrl + "/quota";
            QuotaResponse response = restTemplate.getForObject(url, QuotaResponse.class);
            
            if (response == null) {
                return QuotaStatus.defaultStatus();
            }
            
            return new QuotaStatus(
                response.usedMB != null ? response.usedMB : 0.0,
                response.limitMB != null ? response.limitMB : 500.0,
                response.remainingMB != null ? response.remainingMB : 500.0,
                response.percentageUsed != null ? response.percentageUsed : 0.0,
                response.fallbackActive != null ? response.fallbackActive : false
            );
        } catch (Exception e) {
            logger.warn("Could not fetch quota status: {}", e.getMessage());
            return QuotaStatus.defaultStatus();
        }
    }

    /**
     * Get current quote for a symbol
     */
    public QuoteDTO getQuote(String symbol) {
        try {
            String url = shioajiApiUrl + "/quote/" + symbol.toUpperCase();
            logger.info("Fetching Shioaji quote for {}: {}", symbol, url);

            QuoteResponse response = restTemplate.getForObject(url, QuoteResponse.class);

            if (response == null || !response.success) {
                logger.error("Shioaji quote failed for {}: {}", symbol, response != null ? response.error : "null response");
                return null;
            }

            QuoteDTO quote = new QuoteDTO();
            quote.setSymbol(response.symbol);
            quote.setPrice(response.price);
            quote.setMarket("US");

            logger.info("Shioaji quote for {}: ${}", symbol, quote.getPrice());
            return quote;

        } catch (ResourceAccessException e) {
            logger.error("Shioaji API not reachable: {}", e.getMessage());
            isAvailable = false;
            return null;
        } catch (Exception e) {
            logger.error("Error fetching Shioaji quote for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Get historical price data for a symbol
     */
    public List<StockPriceHistory> getHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        List<StockPriceHistory> prices = new ArrayList<>();

        try {
            String url = String.format("%s/history/%s?start_date=%s&end_date=%s",
                    shioajiApiUrl, symbol.toUpperCase(), startDate, endDate);

            logger.info("Fetching Shioaji history for {}: {}", symbol, url);

            HistoryResponse response = restTemplate.getForObject(url, HistoryResponse.class);

            if (response == null || !response.success) {
                logger.error("Shioaji history failed for {}: {}", symbol, response != null ? response.error : "null response");
                return prices;
            }

            if (response.prices == null || response.prices.isEmpty()) {
                logger.warn("No historical data from Shioaji for {}", symbol);
                return prices;
            }

            // Convert to StockPriceHistory entities
            for (PriceBar bar : response.prices) {
                try {
                    LocalDate date = LocalDate.parse(bar.date);

                    StockPriceHistory price = new StockPriceHistory();
                    price.setSymbol(symbol);
                    price.setDate(date);
                    price.setOpen(bar.open);
                    price.setHigh(bar.high);
                    price.setLow(bar.low);
                    price.setClose(bar.close);
                    price.setVolume(bar.volume);
                    price.setAdjustedClose(bar.adjustedClose);
                    price.setMarket("US");

                    prices.add(price);
                } catch (Exception e) {
                    logger.error("Error parsing price bar for {}: {}", symbol, e.getMessage());
                }
            }

            logger.info("Shioaji returned {} historical prices for {}", prices.size(), symbol);
            return prices;

        } catch (ResourceAccessException e) {
            logger.error("Shioaji API not reachable: {}", e.getMessage());
            isAvailable = false;
            return prices;
        } catch (Exception e) {
            logger.error("Error fetching Shioaji history for {}: {}", symbol, e.getMessage());
            return prices;
        }
    }

    // Response DTOs
    public static class QuoteDTO {
        private String symbol;
        private BigDecimal price;
        private String market;

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getMarket() { return market; }
        public void setMarket(String market) { this.market = market; }
    }

    private static class QuoteResponse {
        public boolean success;
        public String symbol;
        public BigDecimal price;
        public BigDecimal open;
        public BigDecimal high;
        public BigDecimal low;
        public Long volume;
        public String error;
    }

    private static class HistoryResponse {
        public boolean success;
        public String symbol;
        public List<PriceBar> prices;
        public Integer count;
        public String error;
    }

    private static class PriceBar {
        public String date;
        public BigDecimal open;
        public BigDecimal high;
        public BigDecimal low;
        public BigDecimal close;
        public Long volume;
        @JsonProperty("adjusted_close")
        public BigDecimal adjustedClose;
    }

    private static class HealthResponse {
        public String status;
        public boolean connected;
        public String message;
    }

    private static class QuotaResponse {
        public Boolean success;
        public Double usedMB;
        public Double limitMB;
        public Double remainingMB;
        public Double percentageUsed;
        public Boolean fallbackActive;
        public String error;
    }

    /**
     * Quota status DTO
     */
    public static class QuotaStatus {
        private final double usedMB;
        private final double limitMB;
        private final double remainingMB;
        private final double percentageUsed;
        private final boolean fallbackActive;

        public QuotaStatus(double usedMB, double limitMB, double remainingMB, double percentageUsed, boolean fallbackActive) {
            this.usedMB = usedMB;
            this.limitMB = limitMB;
            this.remainingMB = remainingMB;
            this.percentageUsed = percentageUsed;
            this.fallbackActive = fallbackActive;
        }

        public static QuotaStatus defaultStatus() {
            return new QuotaStatus(0, 500, 500, 0, false);
        }

        public double getUsedMB() { return usedMB; }
        public double getLimitMB() { return limitMB; }
        public double getRemainingMB() { return remainingMB; }
        public double getPercentageUsed() { return percentageUsed; }
        public boolean isFallbackActive() { return fallbackActive; }
    }
}
