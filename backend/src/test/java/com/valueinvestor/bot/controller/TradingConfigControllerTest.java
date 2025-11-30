package com.valueinvestor.bot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valueinvestor.bot.model.Market;
import com.valueinvestor.bot.model.entity.TradingConfig;
import com.valueinvestor.bot.service.TradingConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradingConfigController.class)
class TradingConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingConfigService tradingConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getTradingConfig_shouldReturnConfig() throws Exception {
        TradingConfig config = new TradingConfig();
        config.setMarket(Market.US);
        config.setTelegramEnabled(true);
        when(tradingConfigService.getTradingConfig()).thenReturn(Optional.of(config));

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("US"))
                .andExpect(jsonPath("$.telegramEnabled").value(true));
    }

    @Test
    void saveTradingConfig_shouldSaveAndReturnConfig() throws Exception {
        TradingConfig config = new TradingConfig();
        config.setMarket(Market.TW);
        config.setTelegramBotToken("new-token");

        when(tradingConfigService.saveTradingConfig(any(TradingConfig.class))).thenReturn(config);

        mockMvc.perform(post("/api/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.market").value("TW"))
                .andExpect(jsonPath("$.telegramBotToken").value("new-token"));
    }
}
