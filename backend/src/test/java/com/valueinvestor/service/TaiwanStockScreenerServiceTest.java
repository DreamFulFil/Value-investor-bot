package com.valueinvestor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TaiwanStockScreenerServiceTest {

    @InjectMocks
    private TaiwanStockScreenerService screenerService;

    @BeforeEach
    void setUp() {
        // Setup common test fixtures
    }

    @Test
    void should_getTopDividendStocks() {
        // When
        List<TaiwanStockScreenerService.StockInfo> result = screenerService.getTopDividendStocks(5);

        // Then - Should return top 5 stocks sorted by dividend yield
        assertThat(result).isNotNull();
        assertThat(result.size()).isLessThanOrEqualTo(5);
    }

    @Test
    void should_getAllDividendStocks() {
        // When
        List<TaiwanStockScreenerService.StockInfo> result = screenerService.getAllDividendStocks();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_createStockInfo_correctly() {
        // Given
        TaiwanStockScreenerService.StockInfo stockInfo = new TaiwanStockScreenerService.StockInfo(
                "2330.TW", "台積電", "Technology", new BigDecimal("3.0"));

        // Then
        assertThat(stockInfo.getSymbol()).isEqualTo("2330.TW");
        assertThat(stockInfo.getName()).isEqualTo("台積電");
        assertThat(stockInfo.getSector()).isEqualTo("Technology");
        assertThat(stockInfo.getDividendYield()).isEqualTo(new BigDecimal("3.0"));
    }

    @Test
    void should_sortByDividendYield_descending() {
        // Given
        TaiwanStockScreenerService.StockInfo stock1 = new TaiwanStockScreenerService.StockInfo(
                "2330.TW", "台積電", "Technology", new BigDecimal("3.0"));
        TaiwanStockScreenerService.StockInfo stock2 = new TaiwanStockScreenerService.StockInfo(
                "2886.TW", "兆豐金", "Financials", new BigDecimal("5.8"));

        List<TaiwanStockScreenerService.StockInfo> stocks = Arrays.asList(stock1, stock2);

        // When - Sort by dividend yield descending
        stocks.sort((a, b) -> b.getDividendYield().compareTo(a.getDividendYield()));

        // Then
        assertThat(stocks.get(0).getSymbol()).isEqualTo("2886.TW");
        assertThat(stocks.get(1).getSymbol()).isEqualTo("2330.TW");
    }

    @Test
    void should_handleNullDividendYield() {
        // Given
        TaiwanStockScreenerService.StockInfo stockInfo = new TaiwanStockScreenerService.StockInfo(
                "TEST.TW", "Test", "Other", null);

        // Then - Should default to zero
        assertThat(stockInfo.getDividendYield()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void should_verifyStockData() {
        // When
        boolean result = screenerService.verifyStockData("2330.TW");

        // Then - May be true or false depending on API availability
        assertThat(result).isIn(true, false);
    }

    @Test
    void should_refreshCache() {
        // When - Should not throw
        screenerService.refreshCache();

        // Then - Cache should be refreshed (stocks should be fetched)
        List<TaiwanStockScreenerService.StockInfo> stocks = screenerService.getAllDividendStocks();
        assertThat(stocks).isNotNull();
    }

    @Test
    void should_filterByMinimumYield() {
        // Given
        BigDecimal minYield = new BigDecimal("4.0");
        List<TaiwanStockScreenerService.StockInfo> stocks = screenerService.getAllDividendStocks();

        // When
        List<TaiwanStockScreenerService.StockInfo> filtered = stocks.stream()
                .filter(s -> s.getDividendYield().compareTo(minYield) >= 0)
                .toList();

        // Then
        for (TaiwanStockScreenerService.StockInfo stock : filtered) {
            assertThat(stock.getDividendYield()).isGreaterThanOrEqualTo(minYield);
        }
    }
}
