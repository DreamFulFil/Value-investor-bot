package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_log")
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(length = 20)
    private String symbol;

    @Column
    private BigDecimal quantity;

    @Column
    private BigDecimal price;

    @Column
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradingMode mode;

    @Column(length = 500)
    private String notes;

    public enum TransactionType {
        BUY, SELL, DEPOSIT, REBALANCE
    }

    public enum TradingMode {
        SIMULATION, LIVE
    }

    // Constructors
    public TransactionLog() {
        this.timestamp = LocalDateTime.now();
    }

    public TransactionLog(TransactionType type, String symbol, BigDecimal quantity,
                         BigDecimal price, BigDecimal totalAmount, TradingMode mode, String notes) {
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = totalAmount;
        this.mode = mode;
        this.notes = notes;
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public TradingMode getMode() {
        return mode;
    }

    public void setMode(TradingMode mode) {
        this.mode = mode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Alias method for compatibility
    public TransactionType getTransactionType() {
        return type;
    }
}
