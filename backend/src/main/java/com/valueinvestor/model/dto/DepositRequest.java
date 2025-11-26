package com.valueinvestor.model.dto;

import java.math.BigDecimal;

public class DepositRequest {
    private BigDecimal amount;
    private String notes;

    // Constructors
    public DepositRequest() {
    }

    public DepositRequest(BigDecimal amount, String notes) {
        this.amount = amount;
        this.notes = notes;
    }

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
