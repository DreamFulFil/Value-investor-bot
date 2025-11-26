package com.valueinvestor.controller;

import com.valueinvestor.model.entity.InsightsHistory;
import com.valueinvestor.service.InsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InsightsController.class)
class InsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InsightsService insightsService;

    @Test
    void should_getCurrentInsights_when_requested() throws Exception {
        // Given
        when(insightsService.getCurrentInsights()).thenReturn("# Monthly Insights");

        // When/Then
        mockMvc.perform(get("/api/insights/current"))
                .andExpect(status().isOk())
                .andExpect(content().string("# Monthly Insights"));
    }

    @Test
    void should_getInsightsHistory_when_requested() throws Exception {
        // Given
        List<InsightsHistory> history = new ArrayList<>();
        InsightsHistory insights = new InsightsHistory(
                LocalDate.now(),
                "Content",
                new BigDecimal("10000.00"),
                new BigDecimal("5.5")
        );
        history.add(insights);

        when(insightsService.getInsightsHistory(anyInt())).thenReturn(history);

        // When/Then
        mockMvc.perform(get("/api/insights/history?limit=10"))
                .andExpect(status().isOk());
    }
}
