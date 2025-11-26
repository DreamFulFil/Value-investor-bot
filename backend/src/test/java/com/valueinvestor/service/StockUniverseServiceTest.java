package com.valueinvestor.service;

import com.valueinvestor.model.dto.StockUniverseDTO;
import com.valueinvestor.model.entity.StockUniverse;
import com.valueinvestor.repository.StockUniverseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockUniverseServiceTest {

    @Mock
    private StockUniverseRepository stockUniverseRepository;

    @InjectMocks
    private StockUniverseService stockUniverseService;

    private StockUniverse testStock;

    @BeforeEach
    void setUp() {
        testStock = new StockUniverse("AAPL", "Apple Inc.", "Technology");
        testStock.setId(1L);
    }

    @Test
    void should_addStock_when_stockDoesNotExist() {
        // Given
        when(stockUniverseRepository.findBySymbol("AAPL")).thenReturn(Optional.empty());
        when(stockUniverseRepository.save(any(StockUniverse.class))).thenReturn(testStock);

        // When
        StockUniverse result = stockUniverseService.addStock("AAPL", "Apple Inc.", "Technology");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        verify(stockUniverseRepository).save(any(StockUniverse.class));
    }

    @Test
    void should_reactivateStock_when_inactiveStockExists() {
        // Given
        testStock.setActive(false);
        when(stockUniverseRepository.findBySymbol("AAPL")).thenReturn(Optional.of(testStock));
        when(stockUniverseRepository.save(any(StockUniverse.class))).thenReturn(testStock);

        // When
        StockUniverse result = stockUniverseService.addStock("AAPL", "Apple Inc.", "Technology");

        // Then
        assertThat(result).isNotNull();
        verify(stockUniverseRepository).save(any(StockUniverse.class));
    }

    @Test
    void should_removeStock_when_stockExists() {
        // Given
        when(stockUniverseRepository.findBySymbol("AAPL")).thenReturn(Optional.of(testStock));
        when(stockUniverseRepository.save(any(StockUniverse.class))).thenReturn(testStock);

        // When
        stockUniverseService.removeStock("AAPL");

        // Then
        verify(stockUniverseRepository).save(any(StockUniverse.class));
    }

    @Test
    void should_getActiveStocks_when_called() {
        // Given
        List<StockUniverse> stocks = Arrays.asList(testStock);
        when(stockUniverseRepository.findByActiveTrue()).thenReturn(stocks);

        // When
        List<StockUniverse> result = stockUniverseService.getActiveStocks();

        // Then
        assertThat(result).hasSize(1);
        verify(stockUniverseRepository).findByActiveTrue();
    }

    @Test
    void should_getActiveUSStocks_when_called() {
        // Given
        List<StockUniverse> stocks = Arrays.asList(testStock);
        when(stockUniverseRepository.findByMarketAndActiveTrue("TW")).thenReturn(stocks);

        // When
        List<StockUniverse> result = stockUniverseService.getActiveTWStocks();

        // Then
        assertThat(result).hasSize(1);
        verify(stockUniverseRepository).findByMarketAndActiveTrue("TW");
    }

    @Test
    void should_getStockBySymbol_when_symbolProvided() {
        // Given
        when(stockUniverseRepository.findBySymbol("AAPL")).thenReturn(Optional.of(testStock));

        // When
        Optional<StockUniverse> result = stockUniverseService.getStockBySymbol("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
        verify(stockUniverseRepository).findBySymbol("AAPL");
    }

    @Test
    void should_getActiveStocksAsDTO_when_called() {
        // Given
        List<StockUniverse> stocks = Arrays.asList(testStock);
        when(stockUniverseRepository.findByActiveTrue()).thenReturn(stocks);

        // When
        List<StockUniverseDTO> result = stockUniverseService.getActiveStocksAsDTO();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void should_getActiveStocksBySector_when_sectorProvided() {
        // Given
        List<StockUniverse> stocks = Arrays.asList(testStock);
        when(stockUniverseRepository.findBySectorAndActiveTrue("Technology")).thenReturn(stocks);

        // When
        List<StockUniverse> result = stockUniverseService.getActiveStocksBySector("Technology");

        // Then
        assertThat(result).hasSize(1);
        verify(stockUniverseRepository).findBySectorAndActiveTrue("Technology");
    }

    @Test
    void should_getAllSectors_when_called() {
        // Given
        List<String> sectors = Arrays.asList("Technology", "Healthcare");
        when(stockUniverseRepository.findDistinctActiveSectors()).thenReturn(sectors);

        // When
        List<String> result = stockUniverseService.getAllSectors();

        // Then
        assertThat(result).hasSize(2);
        verify(stockUniverseRepository).findDistinctActiveSectors();
    }

    @Test
    void should_getActiveStockCount_when_called() {
        // Given
        when(stockUniverseRepository.countByActiveTrue()).thenReturn(50L);

        // When
        long result = stockUniverseService.getActiveStockCount();

        // Then
        assertThat(result).isEqualTo(50L);
        verify(stockUniverseRepository).countByActiveTrue();
    }

    @Test
    void should_existsBySymbol_when_symbolProvided() {
        // Given
        when(stockUniverseRepository.existsBySymbol("AAPL")).thenReturn(true);

        // When
        boolean result = stockUniverseService.existsBySymbol("AAPL");

        // Then
        assertThat(result).isTrue();
        verify(stockUniverseRepository).existsBySymbol("AAPL");
    }

    @Test
    void should_getAllActiveSymbols_when_called() {
        // Given
        List<StockUniverse> stocks = Arrays.asList(testStock);
        when(stockUniverseRepository.findByActiveTrue()).thenReturn(stocks);

        // When
        List<String> result = stockUniverseService.getAllActiveSymbols();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("AAPL");
    }
}
