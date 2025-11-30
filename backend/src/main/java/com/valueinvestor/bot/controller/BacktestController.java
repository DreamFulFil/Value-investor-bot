package com.valueinvestor.bot.controller;

import com.valueinvestor.bot.model.dto.BacktestResultDto;
import com.valueinvestor.bot.service.BacktestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backtest")
public class BacktestController {

    private final BacktestService backtestService;

    public BacktestController(BacktestService backtestService) {
        this.backtestService = backtestService;
    }

    @GetMapping("/chart")
    public BacktestResultDto getBacktestChart() {
        return backtestService.generateBacktestChartData();
    }
}
