package com.valueinvestor.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistent trading configuration stored in database.
 * Once LIVE mode is activated, it is permanently stored and cannot be reverted to SIMULATION.
 */
@Entity
@Table(name = "trading_config")
public class TradingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 50)
    private String key;

    @Column(name = "config_value", nullable = false, length = 200)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Well-known config keys
    public static final String KEY_TRADING_MODE = "trading.mode";
    public static final String KEY_GO_LIVE_DATE = "trading.goLiveDate";
    public static final String KEY_GO_LIVE_OPTION = "trading.goLiveOption";

    public TradingConfig() {
        this.updatedAt = LocalDateTime.now();
    }

    public TradingConfig(String key, String value) {
        this.key = key;
        this.value = value;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
