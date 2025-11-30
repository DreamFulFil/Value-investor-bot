package com.valueinvestor.bot.service;

import com.valueinvestor.bot.model.dto.BacktestResultDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BacktestService {

    /**
     * Generates sample backtest data.
     * In a real implementation, this service would use HistoricalDataService
     * and PortfolioService to calculate the portfolio's value over a specific period.
     *
     * @return A DTO containing a list of data points for the chart.
     */
    public BacktestResultDto generateBacktestChartData() {
        List<BacktestResultDto.DataPoint> dataPoints = List.of(
            new BacktestResultDto.DataPoint(LocalDate.now().minusMonths(6), new BigDecimal("10000")),
            new BacktestResultDto.DataPoint(LocalDate.now().minusMonths(5), new BigDecimal("10500")),
            new BacktestResultDto.DataPoint(LocalDate.now().minusMonths(4), new BigDecimal("11000")),
            new BacktestResultDto.DataPoint(LocalDate.now().minusMonths(3), new BigDecimal("10800")),
            new BacktestResultDto.DataPoint(LocalDate.now().minusMonths(2), new BigDecimal("11500")),
            new BacktestResultDto.DataPoint(LocalDate.now().minusMonths(1), new BigDecimal("12000")),
            new BacktestResultDto.DataPoint(LocalDate.now(), new BigDecimal("12500"))
        );
        return new BacktestResultDto(dataPoints);
    }
}
