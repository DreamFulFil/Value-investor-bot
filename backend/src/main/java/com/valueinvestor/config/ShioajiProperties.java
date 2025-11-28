package com.valueinvestor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "shioaji")
public class ShioajiProperties {
    private String pythonPath = "python3";
    private String apiKey;
    private String secretKey;
    private String apiUrl = "http://127.0.0.1:8888";
}
