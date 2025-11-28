package com.valueinvestor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private StockUniverse stockUniverse = new StockUniverse();

    @Data
    public static class StockUniverse {
        private int initialSize = 50;
    }
}
