package com.valueinvestor.bot.controller;

import com.valueinvestor.bot.model.dto.BacktestResultDto;
import com.valueinvestor.bot.service.BacktestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BacktestController.class)
class BacktestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BacktestService backtestService;

    @Test
    void getBacktestChart_shouldReturnChartData() throws Exception {
        List<BacktestResultDto.DataPoint> dataPoints = List.of(
            new BacktestResultDto.DataPoint(LocalDate.now().minusMonths(1), new BigDecimal("100")),
            new BacktestResultDto.DataPoint(LocalDate.now(), new BigDecimal("110"))
        );
        BacktestResultDto mockResult = new BacktestResultDto(dataPoints);

        when(backtestService.generateBacktestChartData()).thenReturn(mockResult);

        mockMvc.perform(get("/api/backtest/chart"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dataPoints").isArray())
            .andExpect(jsonPath("$.dataPoints.length()").value(2))
            .andExpect(jsonPath("$.dataPoints[0].value").value(100));
    }
}
