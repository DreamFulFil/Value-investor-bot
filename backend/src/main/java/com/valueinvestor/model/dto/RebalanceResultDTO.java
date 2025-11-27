package com.valueinvestor.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RebalanceResultDTO {
    private boolean success;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int missedMonths;
    private int totalTransactions;
    private String errorMessage;
    private String message;
    private List<MonthlyRebalanceDTO> monthlyResults;

    // Constructors
    public RebalanceResultDTO() {
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getMissedMonths() {
        return missedMonths;
    }

    public void setMissedMonths(int missedMonths) {
        this.missedMonths = missedMonths;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }

    public List<MonthlyRebalanceDTO> getMonthlyResults() {
        return monthlyResults;
    }

    public void setMonthlyResults(List<MonthlyRebalanceDTO> monthlyResults) {
        this.monthlyResults = monthlyResults;
    }

    public static class MonthlyRebalanceDTO {
        private String rebalanceDate;
        private List<String> selectedStocks;
        private int stocksPurchased;
        private String totalInvested;

        // Getters and Setters
        public String getRebalanceDate() {
            return rebalanceDate;
        }

        public void setRebalanceDate(String rebalanceDate) {
            this.rebalanceDate = rebalanceDate;
        }

        public List<String> getSelectedStocks() {
            return selectedStocks;
        }

        public void setSelectedStocks(List<String> selectedStocks) {
            this.selectedStocks = selectedStocks;
        }

        public int getStocksPurchased() {
            return stocksPurchased;
        }

        public void setStocksPurchased(int stocksPurchased) {
            this.stocksPurchased = stocksPurchased;
        }

        public String getTotalInvested() {
            return totalInvested;
        }

        public void setTotalInvested(String totalInvested) {
            this.totalInvested = totalInvested;
        }
    }
}
