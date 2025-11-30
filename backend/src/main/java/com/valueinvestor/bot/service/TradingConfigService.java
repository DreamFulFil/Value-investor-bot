package com.valueinvestor.bot.service;

import com.valueinvestor.bot.model.entity.TradingConfig;
import com.valueinvestor.bot.repository.TradingConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TradingConfigService {

    private final TradingConfigRepository tradingConfigRepository;

    public TradingConfigService(TradingConfigRepository tradingConfigRepository) {
        this.tradingConfigRepository = tradingConfigRepository;
    }

    public Optional<TradingConfig> getTradingConfig() {
        // Assuming a single configuration record with ID 1
        return tradingConfigRepository.findById(1L);
    }

    public TradingConfig saveTradingConfig(TradingConfig config) {
        // Ensure it always saves to the single record with ID 1
        config.setId(1L);
        return tradingConfigRepository.save(config);
    }
}
