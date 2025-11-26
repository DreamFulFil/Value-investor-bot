package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results", indexes = {
    @Index(name = "idx_analysis_symbol", columnList = "symbol"),
    @Index(name = "idx_analysis_timestamp", columnList = "timestamp")
})
public class AnalysisResults {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String analysisText;

    @Column
    private Double score;

    @Column(length = 50)
    private String recommendation;

    @Column(length = 100)
    private String model;

    @Column(columnDefinition = "TEXT")
    private String fundamentalsSnapshot;

    // Constructors
    public AnalysisResults() {
        this.timestamp = LocalDateTime.now();
        this.model = "llama3.1:8b-instruct-q5_K_M";
    }

    public AnalysisResults(String symbol, String analysisText, Double score,
                          String recommendation, String fundamentalsSnapshot) {
        this.symbol = symbol;
        this.timestamp = LocalDateTime.now();
        this.analysisText = analysisText;
        this.score = score;
        this.recommendation = recommendation;
        this.model = "llama3.1:8b-instruct-q5_K_M";
        this.fundamentalsSnapshot = fundamentalsSnapshot;
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

    public String getFundamentalsSnapshot() {
        return fundamentalsSnapshot;
    }

    public void setFundamentalsSnapshot(String fundamentalsSnapshot) {
        this.fundamentalsSnapshot = fundamentalsSnapshot;
    }
}
