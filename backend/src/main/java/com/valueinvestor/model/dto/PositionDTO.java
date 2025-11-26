package com.valueinvestor.model.dto;

import java.math.BigDecimal;

public class PositionDTO {
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal unrealizedPL;
    private BigDecimal plPercentage;

    // Constructors
    public PositionDTO() {
    }

    public PositionDTO(String symbol, BigDecimal quantity, BigDecimal averagePrice,
                      BigDecimal currentPrice, BigDecimal marketValue, BigDecimal unrealizedPL) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.currentPrice = currentPrice;
        this.marketValue = marketValue;
        this.unrealizedPL = unrealizedPL;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getUnrealizedPL() {
        return unrealizedPL;
    }

    public void setUnrealizedPL(BigDecimal unrealizedPL) {
        this.unrealizedPL = unrealizedPL;
    }

    public BigDecimal getPlPercentage() {
        return plPercentage;
    }

    public void setPlPercentage(BigDecimal plPercentage) {
        this.plPercentage = plPercentage;
    }
}
