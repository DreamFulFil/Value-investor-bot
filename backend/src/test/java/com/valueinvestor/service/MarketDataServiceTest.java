package com.valueinvestor.service;

import com.valueinvestor.model.entity.StockFundamentals;
import com.valueinvestor.repository.StockFundamentalsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private StockFundamentalsRepository fundamentalsRepository;

    @InjectMocks
    private MarketDataService marketDataService;

    private StockFundamentals testFundamentals;

    @BeforeEach
    void setUp() {
        testFundamentals = new StockFundamentals("AAPL", "Apple Inc.");
        testFundamentals.setCurrentPrice(new BigDecimal("150.00"));
        testFundamentals.setDividendYield(new BigDecimal("0.5"));
        testFundamentals.setPeRatio(new BigDecimal("25.0"));
        testFundamentals.setPbRatio(new BigDecimal("30.0"));
        testFundamentals.setMarketCap(new BigDecimal("2500000000000"));
        testFundamentals.setLastUpdated(LocalDateTime.now());
    }

    @Test
    void should_getFundamentals_when_cachedDataExists() {
        // Given
        when(fundamentalsRepository.findBySymbolIfRecent(eq("AAPL"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testFundamentals));

        // When
        StockFundamentals result = marketDataService.getFundamentals("AAPL");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getCurrentPrice()).isEqualByComparingTo("150.00");
        verify(fundamentalsRepository).findBySymbolIfRecent(eq("AAPL"), any(LocalDateTime.class));
        verify(fundamentalsRepository, never()).save(any());
    }

    @Test
    void should_getTopDividendStocks_when_limitIsProvided() {
        // Given
        List<StockFundamentals> stocks = new ArrayList<>();
        stocks.add(testFundamentals);
        stocks.add(new StockFundamentals("T", "AT&T Inc."));
        stocks.add(new StockFundamentals("VZ", "Verizon"));

        when(fundamentalsRepository.findTopDividendStocks()).thenReturn(stocks);

        // When
        List<StockFundamentals> result = marketDataService.getTopDividendStocks(2);

        // Then
        assertThat(result).hasSize(2);
        verify(fundamentalsRepository).findTopDividendStocks();
    }

    @Test
    void should_searchStocks_when_queryIsProvided() {
        // Given
        List<StockFundamentals> searchResults = new ArrayList<>();
        searchResults.add(testFundamentals);

        when(fundamentalsRepository.searchByNameOrSymbol("Apple")).thenReturn(searchResults);

        // When
        List<StockFundamentals> result = marketDataService.searchStocks("Apple");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
        verify(fundamentalsRepository).searchByNameOrSymbol("Apple");
    }

    @Test
    void should_getStocksByMinDividendYield_when_minYieldIsProvided() {
        // Given
        BigDecimal minYield = new BigDecimal("2.0");
        List<StockFundamentals> highYieldStocks = new ArrayList<>();
        highYieldStocks.add(testFundamentals);

        when(fundamentalsRepository.findStocksByMinDividendYield(minYield)).thenReturn(highYieldStocks);

        // When
        List<StockFundamentals> result = marketDataService.getStocksByMinDividendYield(minYield);

        // Then
        assertThat(result).hasSize(1);
        verify(fundamentalsRepository).findStocksByMinDividendYield(minYield);
    }

    @Test
    void should_refreshStaleData_when_dataIsOld() {
        // Given
        List<StockFundamentals> staleStocks = new ArrayList<>();
        StockFundamentals staleStock = new StockFundamentals("MSFT", "Microsoft");
        staleStock.setLastUpdated(LocalDateTime.now().minusDays(2));
        staleStocks.add(staleStock);

        when(fundamentalsRepository.findStaleData(any(LocalDateTime.class))).thenReturn(staleStocks);

        // When
        marketDataService.refreshStaleData();

        // Then
        verify(fundamentalsRepository).findStaleData(any(LocalDateTime.class));
    }

    @Test
    void should_returnZero_when_quoteNotFound() {
        // When
        BigDecimal quote = marketDataService.getQuote("INVALID");

        // Then
        assertThat(quote).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_returnZero_when_dividendYieldNotFound() {
        // When
        BigDecimal yield = marketDataService.getDividendYield("INVALID");

        // Then
        assertThat(yield).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_returnFalse_when_invalidSymbol() {
        // When
        boolean isValid = marketDataService.isValidSymbol("INVALID_SYMBOL_XYZ");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void should_returnEmptyList_when_noTopDividendStocks() {
        // Given
        when(fundamentalsRepository.findTopDividendStocks()).thenReturn(new ArrayList<>());

        // When
        List<StockFundamentals> result = marketDataService.getTopDividendStocks(10);

        // Then
        assertThat(result).isEmpty();
        verify(fundamentalsRepository).findTopDividendStocks();
    }
}
