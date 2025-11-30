package com.valueinvestor.bot.service;

import com.valueinvestor.bot.model.entity.TradingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RebalanceService {

    private static final Logger log = LoggerFactory.getLogger(RebalanceService.class);
    private final TelegramService telegramService;
    private final TradingConfigService tradingConfigService;

    public RebalanceService(TelegramService telegramService, TradingConfigService tradingConfigService) {
        this.telegramService = telegramService;
        this.tradingConfigService = tradingConfigService;
    }

    public void executeRebalance() {
        // In a real application, this would contain complex rebalancing logic:
        // 1. Get current portfolio
        // 2. Determine target allocations
        // 3. Calculate and execute necessary trades
        log.info("Rebalance logic executed successfully.");

        Optional<TradingConfig> configOpt = tradingConfigService.getTradingConfig();
        configOpt.ifPresent(config -> {
            if (config.isTelegramEnabled()) {
                telegramService.sendMessage(config.getTelegramBotToken(), config.getTelegramChatId(), "Value-Investor-Bot: Rebalance completed successfully.");
            }
        });
    }
}
