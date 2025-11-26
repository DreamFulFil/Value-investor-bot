package com.valueinvestor.model.dto;

import java.time.LocalDate;

public class StockUniverseDTO {
    private Long id;
    private String symbol;
    private String name;
    private String sector;
    private String market;
    private Boolean active;
    private LocalDate addedDate;

    // Constructors
    public StockUniverseDTO() {}

    public StockUniverseDTO(Long id, String symbol, String name, String sector,
                           String market, Boolean active, LocalDate addedDate) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.sector = sector;
        this.market = market;
        this.active = active;
        this.addedDate = addedDate;
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
}
