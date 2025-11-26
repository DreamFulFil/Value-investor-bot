package com.valueinvestor.model.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DataStatusDTO {
    private int totalStocksInUniverse;
    private int stocksWithHistoricalData;
    private int stocksMissingData;
    private LocalDateTime lastFundamentalsRefresh;
    private LocalDateTime lastHistoricalRefresh;
    private LocalDateTime lastQuoteRefresh;
    private Map<String, Integer> dataGapsBySymbol;
    private Map<String, LocalDateTime> lastUpdateBySymbol;
    private boolean catchUpInProgress;
    private String status;

    // Constructors
    public DataStatusDTO() {
        this.dataGapsBySymbol = new HashMap<>();
        this.lastUpdateBySymbol = new HashMap<>();
        this.catchUpInProgress = false;
        this.status = "OK";
    }

    // Getters and Setters
    public int getTotalStocksInUniverse() {
        return totalStocksInUniverse;
    }

    public void setTotalStocksInUniverse(int totalStocksInUniverse) {
        this.totalStocksInUniverse = totalStocksInUniverse;
    }

    public int getStocksWithHistoricalData() {
        return stocksWithHistoricalData;
    }

    public void setStocksWithHistoricalData(int stocksWithHistoricalData) {
        this.stocksWithHistoricalData = stocksWithHistoricalData;
    }

    public int getStocksMissingData() {
        return stocksMissingData;
    }

    public void setStocksMissingData(int stocksMissingData) {
        this.stocksMissingData = stocksMissingData;
    }

    public LocalDateTime getLastFundamentalsRefresh() {
        return lastFundamentalsRefresh;
    }

    public void setLastFundamentalsRefresh(LocalDateTime lastFundamentalsRefresh) {
        this.lastFundamentalsRefresh = lastFundamentalsRefresh;
    }

    public LocalDateTime getLastHistoricalRefresh() {
        return lastHistoricalRefresh;
    }

    public void setLastHistoricalRefresh(LocalDateTime lastHistoricalRefresh) {
        this.lastHistoricalRefresh = lastHistoricalRefresh;
    }

    public LocalDateTime getLastQuoteRefresh() {
        return lastQuoteRefresh;
    }

    public void setLastQuoteRefresh(LocalDateTime lastQuoteRefresh) {
        this.lastQuoteRefresh = lastQuoteRefresh;
    }

    public Map<String, Integer> getDataGapsBySymbol() {
        return dataGapsBySymbol;
    }

    public void setDataGapsBySymbol(Map<String, Integer> dataGapsBySymbol) {
        this.dataGapsBySymbol = dataGapsBySymbol;
    }

    public Map<String, LocalDateTime> getLastUpdateBySymbol() {
        return lastUpdateBySymbol;
    }

    public void setLastUpdateBySymbol(Map<String, LocalDateTime> lastUpdateBySymbol) {
        this.lastUpdateBySymbol = lastUpdateBySymbol;
    }

    public boolean isCatchUpInProgress() {
        return catchUpInProgress;
    }

    public void setCatchUpInProgress(boolean catchUpInProgress) {
        this.catchUpInProgress = catchUpInProgress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
