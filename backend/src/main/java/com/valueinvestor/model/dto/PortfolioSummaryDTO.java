package com.valueinvestor.model.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioSummaryDTO {
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private BigDecimal investedAmount;
    private BigDecimal totalPL;
    private BigDecimal plPercentage;
    private int positionCount;
    private List<PositionDTO> positions;

    // Constructors
    public PortfolioSummaryDTO() {
    }

    // Getters and Setters
    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BigDecimal getInvestedAmount() {
        return investedAmount;
    }

    public void setInvestedAmount(BigDecimal investedAmount) {
        this.investedAmount = investedAmount;
    }

    public BigDecimal getTotalPL() {
        return totalPL;
    }

    public void setTotalPL(BigDecimal totalPL) {
        this.totalPL = totalPL;
    }

    public BigDecimal getPlPercentage() {
        return plPercentage;
    }

    public void setPlPercentage(BigDecimal plPercentage) {
        this.plPercentage = plPercentage;
    }

    public int getPositionCount() {
        return positionCount;
    }

    public void setPositionCount(int positionCount) {
        this.positionCount = positionCount;
    }

    public List<PositionDTO> getPositions() {
        return positions;
    }

    public void setPositions(List<PositionDTO> positions) {
        this.positions = positions;
    }
}
