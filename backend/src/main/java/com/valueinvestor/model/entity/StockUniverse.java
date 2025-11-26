package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_universe",
       indexes = {
           @Index(name = "idx_universe_symbol", columnList = "symbol", unique = true),
           @Index(name = "idx_universe_market_active", columnList = "market,active")
       })
public class StockUniverse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String symbol;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String sector;

    @Column(nullable = false, length = 10)
    private String market;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private LocalDate addedDate;

    @Column
    private LocalDate removedDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public StockUniverse() {
        this.market = "US";
        this.active = true;
        this.addedDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public StockUniverse(String symbol, String name, String sector) {
        this.symbol = symbol;
        this.name = name;
        this.sector = sector;
        this.market = "US";
        this.active = true;
        this.addedDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDate getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(LocalDate addedDate) {
        this.addedDate = addedDate;
    }

    public LocalDate getRemovedDate() {
        return removedDate;
    }

    public void setRemovedDate(LocalDate removedDate) {
        this.removedDate = removedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
