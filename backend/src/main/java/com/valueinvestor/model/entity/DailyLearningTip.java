package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_learning_tip")
public class DailyLearningTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tip_date", nullable = false, unique = true)
    private LocalDate tipDate;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_zh_tw", columnDefinition = "TEXT")
    private String tipContentZhTw;

    @Column(name = "liked")
    private Boolean liked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public DailyLearningTip() {
    }

    public DailyLearningTip(LocalDate tipDate, String category, String content) {
        this.tipDate = tipDate;
        this.category = category;
        this.content = content;
        this.liked = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getTipDate() {
        return tipDate;
    }

    public void setTipDate(LocalDate tipDate) {
        this.tipDate = tipDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getLiked() {
        return liked;
    }

    public void setLiked(Boolean liked) {
        this.liked = liked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTipContentZhTw() {
        return tipContentZhTw;
    }

    public void setTipContentZhTw(String tipContentZhTw) {
        this.tipContentZhTw = tipContentZhTw;
    }

    /**
     * Get content based on locale
     */
    public String getContentByLocale(String locale) {
        if ("zh-TW".equals(locale) && tipContentZhTw != null && !tipContentZhTw.isEmpty()) {
            return tipContentZhTw;
        }
        return content;
    }
}
