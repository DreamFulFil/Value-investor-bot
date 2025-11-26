package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_index",
       uniqueConstraints = @UniqueConstraint(columnNames = {"index_name", "date"}),
       indexes = {
           @Index(name = "idx_market_index_name", columnList = "index_name"),
           @Index(name = "idx_market_index_date", columnList = "date")
       })
public class MarketIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "index_name", nullable = false, length = 20)
    private String indexName;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal value;

    @Column
    private BigDecimal change;

    @Column
    private BigDecimal changePercent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public MarketIndex() {
        this.createdAt = LocalDateTime.now();
    }

    public MarketIndex(String indexName, LocalDate date, BigDecimal value,
                      BigDecimal change, BigDecimal changePercent) {
        this.indexName = indexName;
        this.date = date;
        this.value = value;
        this.change = change;
        this.changePercent = changePercent;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getChange() {
        return change;
    }

    public void setChange(BigDecimal change) {
        this.change = change;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
