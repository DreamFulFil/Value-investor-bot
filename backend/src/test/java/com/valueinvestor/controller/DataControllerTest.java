package com.valueinvestor.controller;

import com.valueinvestor.model.entity.DailyLearningTip;
import com.valueinvestor.service.LearningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataController.class)
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LearningService learningService;

    @Test
    void should_getDailyTip_when_requested() throws Exception {
        // Given
        DailyLearningTip tip = new DailyLearningTip(LocalDate.now(), "dividends", "Focus on growth");
        when(learningService.getDailyTip()).thenReturn(tip);

        // When/Then
        mockMvc.perform(get("/api/data/daily-tip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("dividends"));
    }

    @Test
    void should_getAvailableCategories_when_requested() throws Exception {
        // Given
        List<String> categories = List.of("dividends", "valuation", "risk");
        when(learningService.getAvailableCategories()).thenReturn(categories);

        // When/Then
        mockMvc.perform(get("/api/data/categories"))
                .andExpect(status().isOk());
    }
}
