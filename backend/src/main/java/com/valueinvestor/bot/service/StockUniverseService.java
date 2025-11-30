package com.valueinvestor.bot.service;

import com.valueinvestor.bot.config.InvestmentProperties;
import com.valueinvestor.bot.model.entity.Stock;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockUniverseService {

    private final TaiwanStockScreenerService screenerService;
    private final YahooFinanceService yahooFinanceService;
    private final InvestmentProperties investmentProperties;

    public StockUniverseService(TaiwanStockScreenerService screenerService, YahooFinanceService yahooFinanceService, InvestmentProperties investmentProperties) {
        this.screenerService = screenerService;
        this.yahooFinanceService = yahooFinanceService;
        this.investmentProperties = investmentProperties;
    }

    public List<Stock> createStockUniverse() {
        // Assume screenerService returns a list of candidate stocks
        List<Stock> screenedStocks = screenerService.screenStocks();

        // Apply sector diversification logic
        Map<String, Integer> sectorCounts = new HashMap<>();
        List<Stock> diversifiedStocks = new ArrayList<>();
        int maxPerSector = investmentProperties.getMaxStocksPerSector();

        for (Stock stock : screenedStocks) {
            String sector = yahooFinanceService.getSector(stock.getStockId()).orElse("Unknown");
            int currentCount = sectorCounts.getOrDefault(sector, 0);

            if (currentCount < maxPerSector) {
                diversifiedStocks.add(stock);
                sectorCounts.put(sector, currentCount + 1);
            }
        }
        return diversifiedStocks;
    }
}
