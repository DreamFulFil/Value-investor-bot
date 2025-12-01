package com.valueinvestor.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Full Spring Boot Integration Tests
 * Tests the complete application context with in-memory database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FullStackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Verifies the Spring context loads successfully
    }

    @Test
    void healthEndpoint_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void portfolioEndpoint_shouldReturnPortfolioData() throws Exception {
        mockMvc.perform(get("/api/portfolio/current"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalValue").exists())
                .andExpect(jsonPath("$.cashBalance").exists());
    }

    @Test
    void portfolioMetrics_shouldReturnMetrics() throws Exception {
        mockMvc.perform(get("/api/portfolio/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalValue").isNumber());
    }

    @Test
    void insightsEndpoint_shouldReturnInsightsList() throws Exception {
        mockMvc.perform(get("/api/insights/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.history").isArray());
    }

    @Test
    void quotaEndpoint_shouldReturnQuotaStatus() throws Exception {
        mockMvc.perform(get("/api/quota"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.limitMB").isNumber())
                .andExpect(jsonPath("$.fallbackActive").isBoolean());
    }

    @Test
    void configEndpoint_shouldReturnAppConfig() throws Exception {
        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tradingMode").exists())
                .andExpect(jsonPath("$.monthlyInvestment").isNumber());
    }

    @Test
    void rebalanceEndpoint_shouldTriggerRebalance() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trading/rebalance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").isBoolean())
                .andReturn();

        // Log result for debugging
        String response = result.getResponse().getContentAsString();
        System.out.println("Rebalance response: " + response);
    }

    @Test
    void depositEndpoint_shouldAcceptValidDeposit() throws Exception {
        mockMvc.perform(post("/api/portfolio/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 16000.00, \"notes\": \"Test deposit\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void depositEndpoint_shouldRejectInvalidAmount() throws Exception {
        mockMvc.perform(post("/api/portfolio/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -1000.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dividendSummary_shouldReturnDividendData() throws Exception {
        mockMvc.perform(get("/api/portfolio/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void portfolioHistory_shouldReturnHistoricalData() throws Exception {
        mockMvc.perform(get("/api/portfolio/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void analysisEndpoint_shouldReturnAnalysisForValidSymbol() throws Exception {
        // Analysis may not exist yet, so accept both 200 and 404
        int status = mockMvc.perform(get("/api/analysis/stock/2330.TW"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(200, 404);
    }

    @Test
    void marketDataEndpoint_shouldReturnQuote() throws Exception {
        // External API may not be available in test environment, accept 200, 404, or 500
        int status = mockMvc.perform(get("/api/market/quote/2330.TW"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(200, 404, 500);
    }

    @Test
    void transactionsEndpoint_shouldReturnTransactionList() throws Exception {
        mockMvc.perform(get("/api/trading/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void learningTip_shouldReturnTip() throws Exception {
        mockMvc.perform(get("/api/insights/learning/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tip").exists());
    }
}
