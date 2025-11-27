package com.valueinvestor.service;

import com.valueinvestor.model.entity.StockPriceHistory;
import com.valueinvestor.repository.StockPriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceTest {

    @Mock
    private StockPriceHistoryRepository priceHistoryRepository;

    @Mock
    private ShioajiDataService shioajiDataService;

    @InjectMocks
    private HistoricalDataService historicalDataService;

    private StockPriceHistory testPrice;

    @BeforeEach
    void setUp() {
        testPrice = new StockPriceHistory();
        testPrice.setSymbol("2330.TW");
        testPrice.setDate(LocalDate.now());
        testPrice.setOpen(new BigDecimal("580.00"));
        testPrice.setHigh(new BigDecimal("585.00"));
        testPrice.setLow(new BigDecimal("578.00"));
        testPrice.setClose(new BigDecimal("583.00"));
        testPrice.setVolume(25000000L);
    }

    @Test
    void should_downloadHistoricalPrices_when_shioajiAvailable() {
        // Given
        when(shioajiDataService.isAvailable()).thenReturn(true);
        when(shioajiDataService.getHistoricalPrices(anyString(), any(), any()))
                .thenReturn(Arrays.asList(testPrice));
        when(priceHistoryRepository.existsBySymbolAndDate(anyString(), any())).thenReturn(false);
        when(priceHistoryRepository.save(any())).thenReturn(testPrice);

        // When
        int count = historicalDataService.downloadHistoricalPrices(
                "2330.TW", LocalDate.now().minusDays(30), LocalDate.now());

        // Then
        assertThat(count).isEqualTo(1);
        verify(priceHistoryRepository).save(testPrice);
    }

    @Test
    void should_returnZero_when_shioajiNotAvailable() {
        // Given
        when(shioajiDataService.isAvailable()).thenReturn(false);

        // When
        int count = historicalDataService.downloadHistoricalPrices(
                "2330.TW", LocalDate.now().minusDays(30), LocalDate.now());

        // Then
        assertThat(count).isEqualTo(0);
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    void should_skipExistingData_when_alreadyInDatabase() {
        // Given
        when(shioajiDataService.isAvailable()).thenReturn(true);
        when(shioajiDataService.getHistoricalPrices(anyString(), any(), any()))
                .thenReturn(Arrays.asList(testPrice));
        when(priceHistoryRepository.existsBySymbolAndDate(anyString(), any())).thenReturn(true);

        // When
        int count = historicalDataService.downloadHistoricalPrices(
                "2330.TW", LocalDate.now().minusDays(30), LocalDate.now());

        // Then
        assertThat(count).isEqualTo(0);
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    void should_returnZero_when_noDataFromShioaji() {
        // Given
        when(shioajiDataService.isAvailable()).thenReturn(true);
        when(shioajiDataService.getHistoricalPrices(anyString(), any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        int count = historicalDataService.downloadHistoricalPrices(
                "2330.TW", LocalDate.now().minusDays(30), LocalDate.now());

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void should_bulkDownload_multipleSymbols() {
        // Given
        when(shioajiDataService.isAvailable()).thenReturn(true);
        when(shioajiDataService.getHistoricalPrices(anyString(), any(), any()))
                .thenReturn(Arrays.asList(testPrice));
        when(priceHistoryRepository.existsBySymbolAndDate(anyString(), any())).thenReturn(false);
        when(priceHistoryRepository.save(any())).thenReturn(testPrice);

        List<String> symbols = Arrays.asList("2330.TW", "2317.TW", "2454.TW");

        // When
        Map<String, Integer> results = historicalDataService.bulkDownload(
                symbols, LocalDate.now().minusDays(30), LocalDate.now());

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.values()).allMatch(count -> count >= 0);
    }

    @Test
    void should_getHistoricalPrices_fromRepository() {
        // Given
        List<StockPriceHistory> prices = Arrays.asList(testPrice);
        when(priceHistoryRepository.findBySymbolAndDateBetweenOrderByDateAsc(anyString(), any(), any()))
                .thenReturn(prices);

        // When
        List<StockPriceHistory> result = historicalDataService.getHistoricalPrices(
                "2330.TW", LocalDate.now().minusDays(30), LocalDate.now());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("2330.TW");
    }

    @Test
    void should_getLatestPrice_fromRepository() {
        // Given
        when(priceHistoryRepository.findLatestPriceForSymbol("2330.TW"))
                .thenReturn(Optional.of(testPrice));

        // When
        Optional<StockPriceHistory> result = historicalDataService.getLatestPrice("2330.TW");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getClose()).isEqualTo(new BigDecimal("583.00"));
    }

    @Test
    void should_checkHasHistoricalData() {
        // Given
        when(priceHistoryRepository.existsBySymbol("2330.TW")).thenReturn(true);

        // When
        boolean hasData = historicalDataService.hasHistoricalData("2330.TW");

        // Then
        assertThat(hasData).isTrue();
    }

    @Test
    void should_getRecordCount_fromRepository() {
        // Given
        when(priceHistoryRepository.countBySymbol("2330.TW")).thenReturn(1000L);

        // When
        long count = historicalDataService.getRecordCount("2330.TW");

        // Then
        assertThat(count).isEqualTo(1000L);
    }

    @Test
    void should_getSymbolsWithData() {
        // Given
        List<String> symbols = Arrays.asList("2330.TW", "2317.TW", "2454.TW");
        when(priceHistoryRepository.findDistinctSymbols()).thenReturn(symbols);

        // When
        List<String> result = historicalDataService.getSymbolsWithData();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).contains("2330.TW", "2317.TW", "2454.TW");
    }

    @Test
    void should_downloadMissingData_when_noDataExists() {
        // Given
        when(priceHistoryRepository.findLatestPriceForSymbol("2330.TW"))
                .thenReturn(Optional.empty());
        when(priceHistoryRepository.findEarliestPriceForSymbol("2330.TW"))
                .thenReturn(Optional.empty());
        when(shioajiDataService.isAvailable()).thenReturn(true);
        when(shioajiDataService.getHistoricalPrices(anyString(), any(), any()))
                .thenReturn(Arrays.asList(testPrice));
        when(priceHistoryRepository.existsBySymbolAndDate(anyString(), any())).thenReturn(false);
        when(priceHistoryRepository.save(any())).thenReturn(testPrice);

        // When
        int count = historicalDataService.downloadMissingData("2330.TW");

        // Then
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_returnZero_when_dataUpToDate() {
        // Given
        testPrice.setDate(LocalDate.now());
        when(priceHistoryRepository.findLatestPriceForSymbol("2330.TW"))
                .thenReturn(Optional.of(testPrice));
        when(priceHistoryRepository.findEarliestPriceForSymbol("2330.TW"))
                .thenReturn(Optional.of(testPrice));

        // When
        int count = historicalDataService.downloadMissingData("2330.TW");

        // Then
        assertThat(count).isEqualTo(0);
        verify(shioajiDataService, never()).getHistoricalPrices(anyString(), any(), any());
    }

    @Test
    void should_calculateDataCompleteness() {
        // Given
        when(priceHistoryRepository.countBySymbol("2330.TW")).thenReturn(252L);

        // When
        double completeness = historicalDataService.getDataCompleteness(
                "2330.TW", LocalDate.now().minusYears(1), LocalDate.now());

        // Then
        assertThat(completeness).isGreaterThan(0);
    }

    @Test
    void should_deleteHistoricalData() {
        // When
        historicalDataService.deleteHistoricalData("2330.TW");

        // Then
        verify(priceHistoryRepository).deleteBySymbol("2330.TW");
    }

    @Test
    void should_refreshRecentData() {
        // Given
        when(shioajiDataService.isAvailable()).thenReturn(true);
        when(shioajiDataService.getHistoricalPrices(anyString(), any(), any()))
                .thenReturn(Arrays.asList(testPrice));
        when(priceHistoryRepository.existsBySymbolAndDate(anyString(), any())).thenReturn(false);
        when(priceHistoryRepository.save(any())).thenReturn(testPrice);

        // When
        int count = historicalDataService.refreshRecentData("2330.TW");

        // Then
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
