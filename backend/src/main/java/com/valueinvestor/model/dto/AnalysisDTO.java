package com.valueinvestor.model.dto;

import java.time.LocalDateTime;

public class AnalysisDTO {
    private Long id;
    private String symbol;
    private LocalDateTime timestamp;
    private String analysisText;
    private Double score;
    private String recommendation;
    private String model;

    // Constructors
    public AnalysisDTO() {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAnalysisText() {
        return analysisText;
    }

    public void setAnalysisText(String analysisText) {
        this.analysisText = analysisText;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
