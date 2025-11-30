package com.valueinvestor.bot.service;

import com.valueinvestor.bot.config.InvestmentProperties;
import com.valueinvestor.bot.model.entity.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockUniverseServiceTest {

    @Mock
    private TaiwanStockScreenerService screenerService;

    @Mock
    private YahooFinanceService yahooFinanceService;

    @Mock
    private InvestmentProperties investmentProperties;

    @InjectMocks
    private StockUniverseService stockUniverseService;

    @BeforeEach
    void setUp() {
        when(investmentProperties.getMaxStocksPerSector()).thenReturn(2);
    }

    @Test
    void createStockUniverse_shouldFilterBySectorLimit() {
        // Given: 3 tech stocks, 1 finance stock from the screener
        Stock stock1 = new Stock("2330", "TSMC");
        Stock stock2 = new Stock("2317", "Hon Hai");
        Stock stock3 = new Stock("2454", "MediaTek");
        Stock stock4 = new Stock("2881", "Fubon Financial");

        when(screenerService.screenStocks()).thenReturn(List.of(stock1, stock2, stock3, stock4));

        // Mock the sector lookups
        when(yahooFinanceService.getSector("2330")).thenReturn(Optional.of("Technology"));
        when(yahooFinanceService.getSector("2317")).thenReturn(Optional.of("Technology"));
        when(yahooFinanceService.getSector("2454")).thenReturn(Optional.of("Technology"));
        when(yahooFinanceService.getSector("2881")).thenReturn(Optional.of("Finance"));

        // When
        List<Stock> universe = stockUniverseService.createStockUniverse();

        // Then: Should contain only 2 tech stocks and 1 finance stock
        assertThat(universe).hasSize(3);
        assertThat(universe).contains(stock1, stock2, stock4);
        assertThat(universe).doesNotContain(stock3);
    }

    @Test
    void createStockUniverse_shouldIncludeStocksWithUnknownSector() {
        Stock stock1 = new Stock("1101", "Taiwan Cement");
        Stock stock2 = new Stock("9999", "Unknown Stock");
        Stock stock3 = new Stock("8888", "Another Unknown");

        when(screenerService.screenStocks()).thenReturn(List.of(stock1, stock2, stock3));
        when(yahooFinanceService.getSector("1101")).thenReturn(Optional.of("Materials"));
        when(yahooFinanceService.getSector("9999")).thenReturn(Optional.empty()); // "Unknown"
        when(yahooFinanceService.getSector("8888")).thenReturn(Optional.empty()); // "Unknown"

        // When
        List<Stock> universe = stockUniverseService.createStockUniverse();
        
        // Then: It should take the first two "Unknown" stocks plus the "Materials" one
        assertThat(universe).hasSize(3);
        assertThat(universe).contains(stock1, stock2, stock3);
    }
}
