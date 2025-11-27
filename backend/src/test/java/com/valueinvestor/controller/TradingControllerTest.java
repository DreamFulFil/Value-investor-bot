package com.valueinvestor.controller;

import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.TransactionLogRepository;
import com.valueinvestor.service.ProgressService;
import com.valueinvestor.service.RebalanceService;
import com.valueinvestor.service.TradingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradingController.class)
@ActiveProfiles("test")
class TradingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingService tradingService;
    
    @MockBean
    private RebalanceService rebalanceService;
    
    @MockBean
    private TransactionLogRepository transactionLogRepository;
    
    @MockBean
    private ProgressService progressService;

    @Test
    void should_getTradingStatus_when_requested() throws Exception {
        // Given
        when(tradingService.testShioajiConnection()).thenReturn(true);
        when(transactionLogRepository.findByTimestampBetweenOrderByTimestampDesc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(transactionLogRepository.findLastRebalanceTransaction()).thenReturn(null);

        // When/Then
        mockMvc.perform(get("/api/trading/status"))
                .andExpect(status().isOk());
    }

    @Test
    void should_triggerRebalance_when_requested() throws Exception {
        // Given
        RebalanceService.RebalanceResult result = new RebalanceService.RebalanceResult();
        result.setSuccess(true);
        result.setMessage("Rebalance completed");
        when(rebalanceService.triggerRebalance()).thenReturn(result);

        // When/Then
        mockMvc.perform(post("/api/trading/rebalance"))
                .andExpect(status().isOk());
    }
}
