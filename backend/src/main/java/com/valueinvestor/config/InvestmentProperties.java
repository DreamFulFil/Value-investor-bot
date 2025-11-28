package com.valueinvestor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "investment")
public class InvestmentProperties {
    private int monthlyAmountTwd = 16000;
    private int targetWeeklyTwd = 1600;
}
