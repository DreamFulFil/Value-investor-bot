package com.valueinvestor.bot.service;

import com.valueinvestor.bot.model.dto.BacktestResultDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BacktestServiceTest {

    @InjectMocks
    private BacktestService backtestService;

    @Test
    void generateBacktestChartData_shouldReturnStaticData() {
        BacktestResultDto result = backtestService.generateBacktestChartData();

        assertThat(result).isNotNull();
        assertThat(result.getDataPoints()).hasSize(7);
        assertThat(result.getDataPoints().get(0).getValue()).isEqualByComparingTo("10000");
    }
}
