package com.valueinvestor.bot.service;

import com.valueinvestor.bot.model.entity.TradingConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RebalanceServiceTest {

    @Mock
    private TelegramService telegramService;

    @Mock
    private TradingConfigService tradingConfigService;

    @InjectMocks
    private RebalanceService rebalanceService;

    @Test
    void executeRebalance_shouldSendTelegramNotification_whenEnabled() {
        TradingConfig config = new TradingConfig();
        config.setTelegramEnabled(true);
        config.setTelegramBotToken("token");
        config.setTelegramChatId("chatId");

        when(tradingConfigService.getTradingConfig()).thenReturn(Optional.of(config));

        rebalanceService.executeRebalance();

        verify(telegramService).sendMessage("token", "chatId", "Value-Investor-Bot: Rebalance completed successfully.");
    }

    @Test
    void executeRebalance_shouldNotSendTelegramNotification_whenDisabled() {
        TradingConfig config = new TradingConfig();
        config.setTelegramEnabled(false);

        when(tradingConfigService.getTradingConfig()).thenReturn(Optional.of(config));

        rebalanceService.executeRebalance();

        verify(telegramService, never()).sendMessage(any(), any(), any());
    }

    @Test
    void executeRebalance_shouldNotSendTelegramNotification_whenConfigNotPresent() {
        when(tradingConfigService.getTradingConfig()).thenReturn(Optional.empty());

        rebalanceService.executeRebalance();

        verify(telegramService, never()).sendMessage(any(), any(), any());
    }
}
