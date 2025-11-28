package com.valueinvestor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {
    private String mode = "SIMULATION";
    private boolean startFromZero = true;
}
