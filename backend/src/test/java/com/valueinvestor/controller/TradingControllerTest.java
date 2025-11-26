package com.valueinvestor.controller;

import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.service.TradingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradingController.class)
class TradingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingService tradingService;

    @Test
    void should_executeBuy_when_validOrderProvided() throws Exception {
        // Given
        TransactionLog transaction = new TransactionLog(
                TransactionLog.TransactionType.BUY,
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                new BigDecimal("1500.00"),
                TransactionLog.TradingMode.SIMULATION,
                "Test buy"
        );
        when(tradingService.executeBuy(anyString(), any(BigDecimal.class), any())).thenReturn(transaction);

        // When/Then
        mockMvc.perform(post("/api/trading/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\": \"AAPL\", \"quantity\": 10, \"mode\": \"SIMULATION\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void should_executeSell_when_validOrderProvided() throws Exception {
        // Given
        TransactionLog transaction = new TransactionLog(
                TransactionLog.TransactionType.SELL,
                "AAPL",
                new BigDecimal("5"),
                new BigDecimal("160.00"),
                new BigDecimal("800.00"),
                TransactionLog.TradingMode.SIMULATION,
                "Test sell"
        );
        when(tradingService.executeSell(anyString(), any(BigDecimal.class), any())).thenReturn(transaction);

        // When/Then
        mockMvc.perform(post("/api/trading/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\": \"AAPL\", \"quantity\": 5, \"mode\": \"SIMULATION\"}"))
                .andExpect(status().isOk());
    }
}
