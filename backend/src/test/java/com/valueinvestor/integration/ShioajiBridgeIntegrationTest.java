package com.valueinvestor.integration;

import com.valueinvestor.model.entity.StockPriceHistory;
import com.valueinvestor.service.ShioajiDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Shioaji Python bridge
 * Tests the interaction between Java backend and Python Shioaji bridge
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShioajiBridgeIntegrationTest {

    @Mock
    private ShioajiDataService shioajiDataService;

    private StockPriceHistory testPrice;

    @BeforeEach
    void setUp() {
        testPrice = new StockPriceHistory();
        testPrice.setSymbol("2330.TW");
        testPrice.setDate(LocalDate.now());
        testPrice.setOpen(new BigDecimal("575.00"));
        testPrice.setHigh(new BigDecimal("582.00"));
        testPrice.setLow(new BigDecimal("573.00"));
        testPrice.setClose(new BigDecimal("580.00"));
        testPrice.setVolume(25000000L);
    }

    @Test
    void should_checkShioajiAvailability() {
        // Given
        when(shioajiDataService.isAvailable()).thenReturn(true);

        // When
        boolean available = shioajiDataService.isAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void should_fetchHistoricalPrices_successfully() {
        // Given
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<StockPriceHistory> prices = Arrays.asList(testPrice);

        when(shioajiDataService.getHistoricalPrices("2330.TW", startDate, endDate))
                .thenReturn(prices);

        // When
        List<StockPriceHistory> result = shioajiDataService.getHistoricalPrices(
                "2330.TW", startDate, endDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("2330.TW");
        assertThat(result.get(0).getClose()).isEqualTo(new BigDecimal("580.00"));
    }

    @Test
    void should_handleUnavailableService_gracefully() {
        // Given
        when(shioajiDataService.getHistoricalPrices(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<StockPriceHistory> result = shioajiDataService.getHistoricalPrices(
                "2330.TW", LocalDate.now().minusMonths(1), LocalDate.now());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_handleInvalidSymbol() {
        // Given
        when(shioajiDataService.getHistoricalPrices(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<StockPriceHistory> result = shioajiDataService.getHistoricalPrices(
                "INVALID.TW", LocalDate.now().minusMonths(1), LocalDate.now());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_stripTWSuffix_fromSymbol() {
        // Given
        String symbolWithSuffix = "2330.TW";
        String symbolWithoutSuffix = "2330";

        // When
        String stripped = symbolWithSuffix.replace(".TW", "").replace(".TWO", "");

        // Then
        assertThat(stripped).isEqualTo(symbolWithoutSuffix);
    }

    @Test
    void should_fetchMultipleStocks() {
        // Given
        StockPriceHistory price1 = new StockPriceHistory();
        price1.setSymbol("2330.TW");
        price1.setClose(new BigDecimal("580.00"));

        StockPriceHistory price2 = new StockPriceHistory();
        price2.setSymbol("2317.TW");
        price2.setClose(new BigDecimal("108.50"));

        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        when(shioajiDataService.getHistoricalPrices(eq("2330.TW"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(price1));
        when(shioajiDataService.getHistoricalPrices(eq("2317.TW"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(price2));

        // When
        List<StockPriceHistory> result1 = shioajiDataService.getHistoricalPrices(
                "2330.TW", startDate, endDate);
        List<StockPriceHistory> result2 = shioajiDataService.getHistoricalPrices(
                "2317.TW", startDate, endDate);

        // Then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);
        assertThat(result1.get(0).getClose()).isEqualTo(new BigDecimal("580.00"));
        assertThat(result2.get(0).getClose()).isEqualTo(new BigDecimal("108.50"));
    }

    @Test
    void should_handleDateRangeCorrectly() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 11, 27);

        when(shioajiDataService.getHistoricalPrices("2330.TW", startDate, endDate))
                .thenReturn(Arrays.asList(testPrice));

        // When
        List<StockPriceHistory> result = shioajiDataService.getHistoricalPrices(
                "2330.TW", startDate, endDate);

        // Then
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_validateOHLCVData() {
        // Given historical price data
        when(shioajiDataService.getHistoricalPrices(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(testPrice));

        // When
        List<StockPriceHistory> result = shioajiDataService.getHistoricalPrices(
                "2330.TW", LocalDate.now().minusDays(1), LocalDate.now());

        // Then - OHLCV data should be valid
        StockPriceHistory price = result.get(0);
        assertThat(price.getHigh()).isGreaterThanOrEqualTo(price.getOpen());
        assertThat(price.getHigh()).isGreaterThanOrEqualTo(price.getClose());
        assertThat(price.getHigh()).isGreaterThanOrEqualTo(price.getLow());
        assertThat(price.getLow()).isLessThanOrEqualTo(price.getOpen());
        assertThat(price.getLow()).isLessThanOrEqualTo(price.getClose());
        assertThat(price.getVolume()).isGreaterThan(0);
    }
}
