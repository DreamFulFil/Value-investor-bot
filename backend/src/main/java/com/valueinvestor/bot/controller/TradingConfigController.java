package com.valueinvestor.bot.controller;

import com.valueinvestor.bot.model.entity.TradingConfig;
import com.valueinvestor.bot.service.TradingConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class TradingConfigController {

    private final TradingConfigService tradingConfigService;

    public TradingConfigController(TradingConfigService tradingConfigService) {
        this.tradingConfigService = tradingConfigService;
    }

    @GetMapping
    public ResponseEntity<TradingConfig> getTradingConfig() {
        return tradingConfigService.getTradingConfig()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new TradingConfig())); // Return default config if not found
    }

    @PostMapping
    public ResponseEntity<TradingConfig> saveTradingConfig(@RequestBody TradingConfig tradingConfig) {
        TradingConfig savedConfig = tradingConfigService.saveTradingConfig(tradingConfig);
        return ResponseEntity.ok(savedConfig);
    }
}
