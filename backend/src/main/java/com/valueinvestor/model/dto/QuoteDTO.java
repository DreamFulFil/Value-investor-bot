package com.valueinvestor.model.dto;

import java.math.BigDecimal;

public class QuoteDTO {
    private String symbol;
    private String name;
    private BigDecimal price;
    private BigDecimal dividendYield;
    private BigDecimal peRatio;
    private BigDecimal pbRatio;
    private BigDecimal marketCap;
    private String market;

    // Constructors
    public QuoteDTO() {
    }

    // Getters and Setters
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }
}
