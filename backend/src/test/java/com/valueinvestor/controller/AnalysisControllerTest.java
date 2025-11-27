package com.valueinvestor.controller;

import com.valueinvestor.model.entity.AnalysisResults;
import com.valueinvestor.service.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
@ActiveProfiles("test")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalysisService analysisService;

    @Test
    void should_analyzeStock_when_symbolProvided() throws Exception {
        // Given
        AnalysisResults analysis = new AnalysisResults("AAPL", "Buy recommendation", 85.0, "BUY", "Data");
        when(analysisService.analyzeStock(anyString())).thenReturn(analysis);

        // When/Then
        mockMvc.perform(post("/api/analysis/analyze/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.recommendation").value("BUY"));
    }

    @Test
    void should_getLatestAnalysis_when_symbolProvided() throws Exception {
        // Given
        AnalysisResults analysis = new AnalysisResults("AAPL", "Buy recommendation", 85.0, "BUY", "Data");
        when(analysisService.getLatestAnalysis(anyString())).thenReturn(Optional.of(analysis));

        // When/Then - Endpoint is /api/analysis/stock/{symbol}
        mockMvc.perform(get("/api/analysis/stock/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    void should_getRecentAnalyses_when_requested() throws Exception {
        // Given
        List<AnalysisResults> analyses = new ArrayList<>();
        when(analysisService.getRecentAnalyses(10)).thenReturn(analyses);

        // When/Then
        mockMvc.perform(get("/api/analysis/recent?limit=10"))
                .andExpect(status().isOk());
    }
}
