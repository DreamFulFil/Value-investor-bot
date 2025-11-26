package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "position_history", indexes = {
    @Index(name = "idx_position_timestamp", columnList = "timestamp"),
    @Index(name = "idx_position_symbol", columnList = "symbol")
})
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal averagePrice;

    @Column
    private BigDecimal currentPrice;

    @Column
    private BigDecimal marketValue;

    @Column
    private BigDecimal unrealizedPL;

    // Constructors
    public PositionHistory() {
        this.timestamp = LocalDateTime.now();
    }

    public PositionHistory(String symbol, BigDecimal quantity, BigDecimal averagePrice) {
        this.timestamp = LocalDateTime.now();
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

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

    public void calculateMetrics() {
        if (quantity != null && currentPrice != null) {
            this.marketValue = quantity.multiply(currentPrice);
            if (averagePrice != null) {
                this.unrealizedPL = marketValue.subtract(quantity.multiply(averagePrice));
            }
        }
    }

    public BigDecimal getUnrealizedPLPercent() {
        if (averagePrice != null && averagePrice.compareTo(BigDecimal.ZERO) > 0 && currentPrice != null) {
            return currentPrice.subtract(averagePrice)
                    .divide(averagePrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
}
