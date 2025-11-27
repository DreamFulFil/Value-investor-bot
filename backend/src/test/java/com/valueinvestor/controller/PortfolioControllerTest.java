package com.valueinvestor.controller;

import com.valueinvestor.model.entity.PositionHistory;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioController.class)
@ActiveProfiles("test")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @Test
    void should_getCurrentPortfolio_when_requested() throws Exception {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalValue", new BigDecimal("10000.00"));
        metrics.put("cashBalance", new BigDecimal("1000.00"));
        metrics.put("investedAmount", new BigDecimal("9000.00"));
        metrics.put("totalPL", new BigDecimal("1000.00"));
        metrics.put("plPercentage", new BigDecimal("11.11"));
        metrics.put("positionCount", 3);

        List<PositionHistory> positions = new ArrayList<>();
        PositionHistory position = new PositionHistory("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        position.setCurrentPrice(new BigDecimal("160.00"));
        position.calculateMetrics();
        positions.add(position);

        when(portfolioService.getPerformanceMetrics()).thenReturn(metrics);
        when(portfolioService.getCurrentPortfolio()).thenReturn(positions);

        // When/Then
        mockMvc.perform(get("/api/portfolio/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(10000.00))
                .andExpect(jsonPath("$.positionCount").value(3));
    }

    @Test
    void should_getPerformanceMetrics_when_requested() throws Exception {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalValue", new BigDecimal("10000.00"));
        metrics.put("positionCount", 3);

        when(portfolioService.getPerformanceMetrics()).thenReturn(metrics);

        // When/Then
        mockMvc.perform(get("/api/portfolio/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(10000.00));
    }

    @Test
    void should_recordDeposit_when_validAmountProvided() throws Exception {
        // Given
        TransactionLog transaction = new TransactionLog(
                TransactionLog.TransactionType.DEPOSIT,
                null, null, null,
                new BigDecimal("1000.00"),
                TransactionLog.TradingMode.SIMULATION,
                "Test deposit"
        );
        when(portfolioService.recordDeposit(any(BigDecimal.class), anyString())).thenReturn(transaction);

        // When/Then
        mockMvc.perform(post("/api/portfolio/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 1000.00, \"notes\": \"Test deposit\"}"))
                .andExpect(status().isOk());
    }
}
