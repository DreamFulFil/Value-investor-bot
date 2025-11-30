package com.valueinvestor.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String shioajiApiUrl;

    @NestedConfigurationProperty
    private final TelegramProperties telegram = new TelegramProperties();

    public String getShioajiApiUrl() {
        return shioajiApiUrl;
    }

    public void setShioajiApiUrl(String shioajiApiUrl) {
        this.shioajiApiUrl = shioajiApiUrl;
    }

    public TelegramProperties getTelegram() {
        return telegram;
    }
}
