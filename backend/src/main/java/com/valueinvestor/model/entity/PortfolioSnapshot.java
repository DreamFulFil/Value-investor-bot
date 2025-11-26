package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_snapshot", indexes = {
    @Index(name = "idx_snapshot_timestamp", columnList = "timestamp")
})
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private BigDecimal totalValue;

    @Column(nullable = false)
    private BigDecimal cashBalance;

    @Column(nullable = false)
    private BigDecimal investedAmount;

    @Column
    private BigDecimal totalPL;

    @Column(columnDefinition = "TEXT")
    private String positionsJson;

    @Column(length = 100)
    private String snapshotType;

    // Constructors
    public PortfolioSnapshot() {
        this.timestamp = LocalDateTime.now();
    }

    public PortfolioSnapshot(BigDecimal totalValue, BigDecimal cashBalance,
                            BigDecimal investedAmount, BigDecimal totalPL,
                            String positionsJson, String snapshotType) {
        this.timestamp = LocalDateTime.now();
        this.totalValue = totalValue;
        this.cashBalance = cashBalance;
        this.investedAmount = investedAmount;
        this.totalPL = totalPL;
        this.positionsJson = positionsJson;
        this.snapshotType = snapshotType;
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

    public String getPositionsJson() {
        return positionsJson;
    }

    public void setPositionsJson(String positionsJson) {
        this.positionsJson = positionsJson;
    }

    public String getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }
}
