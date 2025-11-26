package com.valueinvestor.model.dto;

import java.time.LocalDate;

public class LearningTipDTO {

    private Long id;
    private LocalDate tipDate;
    private String category;
    private String content;
    private Boolean liked;

    // Constructors
    public LearningTipDTO() {
    }

    public LearningTipDTO(Long id, LocalDate tipDate, String category, String content, Boolean liked) {
        this.id = id;
        this.tipDate = tipDate;
        this.category = category;
        this.content = content;
        this.liked = liked;
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
}
