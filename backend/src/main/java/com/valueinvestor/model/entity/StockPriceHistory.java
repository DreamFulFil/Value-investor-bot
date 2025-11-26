package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_price_history",
       uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "date"}),
       indexes = {
           @Index(name = "idx_price_history_symbol", columnList = "symbol"),
           @Index(name = "idx_price_history_date", columnList = "date"),
           @Index(name = "idx_price_history_symbol_date", columnList = "symbol,date")
       })
public class StockPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal open;

    @Column(nullable = false)
    private BigDecimal high;

    @Column(nullable = false)
    private BigDecimal low;

    @Column(nullable = false)
    private BigDecimal close;

    @Column(nullable = false)
    private Long volume;

    @Column
    private BigDecimal adjustedClose;

    @Column(length = 10, nullable = false)
    private String market;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public StockPriceHistory() {
        this.createdAt = LocalDateTime.now();
        this.market = "US";
    }

    public StockPriceHistory(String symbol, LocalDate date, BigDecimal open, BigDecimal high,
                            BigDecimal low, BigDecimal close, Long volume, BigDecimal adjustedClose) {
        this.symbol = symbol;
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.adjustedClose = adjustedClose;
        this.market = "US";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public BigDecimal getAdjustedClose() {
        return adjustedClose;
    }

    public void setAdjustedClose(BigDecimal adjustedClose) {
        this.adjustedClose = adjustedClose;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
