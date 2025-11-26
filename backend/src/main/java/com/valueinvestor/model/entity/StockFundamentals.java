package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_fundamentals", indexes = {
    @Index(name = "idx_fundamentals_symbol", columnList = "symbol", unique = true)
})
public class StockFundamentals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(length = 200)
    private String name;

    @Column(length = 100)
    private String sector;

    @Column
    private BigDecimal dividendYield;

    @Column
    private BigDecimal peRatio;

    @Column
    private BigDecimal pbRatio;

    @Column
    private BigDecimal marketCap;

    @Column
    private BigDecimal debtToEquity;

    @Column
    private BigDecimal roe;

    @Column
    private BigDecimal currentPrice;

    @Column(length = 10)
    private String market;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    // Constructors
    public StockFundamentals() {
        this.lastUpdated = LocalDateTime.now();
        this.market = "US";
    }

    public StockFundamentals(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
        this.lastUpdated = LocalDateTime.now();
        this.market = "US";
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public BigDecimal getDividendYield() {
        return dividendYield;
    }

    public void setDividendYield(BigDecimal dividendYield) {
        this.dividendYield = dividendYield;
    }

    public BigDecimal getPeRatio() {
        return peRatio;
    }

    public void setPeRatio(BigDecimal peRatio) {
        this.peRatio = peRatio;
    }

    public BigDecimal getPbRatio() {
        return pbRatio;
    }

    public void setPbRatio(BigDecimal pbRatio) {
        this.pbRatio = pbRatio;
    }

    public BigDecimal getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(BigDecimal marketCap) {
        this.marketCap = marketCap;
    }

    public BigDecimal getDebtToEquity() {
        return debtToEquity;
    }

    public void setDebtToEquity(BigDecimal debtToEquity) {
        this.debtToEquity = debtToEquity;
    }

    public BigDecimal getRoe() {
        return roe;
    }

    public void setRoe(BigDecimal roe) {
        this.roe = roe;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
