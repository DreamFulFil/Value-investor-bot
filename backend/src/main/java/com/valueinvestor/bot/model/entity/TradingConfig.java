package com.valueinvestor.bot.model.entity;

import com.valueinvestor.bot.model.Market;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

@Entity
public class TradingConfig {
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private Market market = Market.TW;

    private boolean telegramEnabled;
    private String telegramBotToken;
    private String telegramChatId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Market getMarket() { return market; }
    public void setMarket(Market market) { this.market = market; }
    public boolean isTelegramEnabled() { return telegramEnabled; }
    public void setTelegramEnabled(boolean telegramEnabled) { this.telegramEnabled = telegramEnabled; }
    public String getTelegramBotToken() { return telegramBotToken; }
    public void setTelegramBotToken(String telegramBotToken) { this.telegramBotToken = telegramBotToken; }
    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }
}
