package com.valueinvestor.bot.service;

import com.valueinvestor.bot.model.entity.TradingConfig;
import com.valueinvestor.bot.repository.TradingConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TradingConfigServiceTest {

    @Mock
    private TradingConfigRepository tradingConfigRepository;

    @InjectMocks
    private TradingConfigService tradingConfigService;

    @Test
    void getTradingConfig_shouldReturnConfig_whenExists() {
        TradingConfig config = new TradingConfig();
        config.setId(1L);
        when(tradingConfigRepository.findById(1L)).thenReturn(Optional.of(config));

        Optional<TradingConfig> result = tradingConfigService.getTradingConfig();

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void saveTradingConfig_shouldSetIdAndSave() {
        TradingConfig configToSave = new TradingConfig();
        // ID should be set by the service, so we don't set it here.

        when(tradingConfigRepository.save(any(TradingConfig.class))).thenAnswer(invocation -> {
            TradingConfig savedConfig = invocation.getArgument(0);
            assertThat(savedConfig.getId()).isEqualTo(1L);
            return savedConfig;
        });

        tradingConfigService.saveTradingConfig(configToSave);

        verify(tradingConfigRepository).save(configToSave);
    }
}
