package com.valueinvestor.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InsightsDTO {

    private Long id;
    private LocalDate generatedDate;
    private String insightsContent;
    private BigDecimal portfolioValue;
    private BigDecimal monthlyReturn;
    private BigDecimal cashBalance;
    private BigDecimal totalInvested;
    private Integer positionsCount;

    // Constructors
    public InsightsDTO() {
    }

    public InsightsDTO(Long id, LocalDate generatedDate, String insightsContent,
                       BigDecimal portfolioValue, BigDecimal monthlyReturn) {
        this.id = id;
        this.generatedDate = generatedDate;
        this.insightsContent = insightsContent;
        this.portfolioValue = portfolioValue;
        this.monthlyReturn = monthlyReturn;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(LocalDate generatedDate) {
        this.generatedDate = generatedDate;
    }

    public String getInsightsContent() {
        return insightsContent;
    }

    public void setInsightsContent(String insightsContent) {
        this.insightsContent = insightsContent;
    }

    public BigDecimal getPortfolioValue() {
        return portfolioValue;
    }

    public void setPortfolioValue(BigDecimal portfolioValue) {
        this.portfolioValue = portfolioValue;
    }

    public BigDecimal getMonthlyReturn() {
        return monthlyReturn;
    }

    public void setMonthlyReturn(BigDecimal monthlyReturn) {
        this.monthlyReturn = monthlyReturn;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BigDecimal getTotalInvested() {
        return totalInvested;
    }

    public void setTotalInvested(BigDecimal totalInvested) {
        this.totalInvested = totalInvested;
    }

    public Integer getPositionsCount() {
        return positionsCount;
    }

    public void setPositionsCount(Integer positionsCount) {
        this.positionsCount = positionsCount;
    }
}
