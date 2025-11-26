package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "insights_history")
public class InsightsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generated_date", nullable = false, unique = true)
    private LocalDate generatedDate;

    @Column(name = "insights_content", nullable = false, columnDefinition = "TEXT")
    private String insightsContent;

    @Column(name = "portfolio_value")
    private BigDecimal portfolioValue;

    @Column(name = "monthly_return")
    private BigDecimal monthlyReturn;

    @Column(name = "cash_balance")
    private BigDecimal cashBalance;

    @Column(name = "total_invested")
    private BigDecimal totalInvested;

    @Column(name = "positions_count")
    private Integer positionsCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public InsightsHistory() {
    }

    public InsightsHistory(LocalDate generatedDate, String insightsContent,
                          BigDecimal portfolioValue, BigDecimal monthlyReturn) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
