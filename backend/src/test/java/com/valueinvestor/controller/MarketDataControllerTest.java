package com.valueinvestor.controller;

import com.valueinvestor.model.entity.StockFundamentals;
import com.valueinvestor.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketDataController.class)
@ActiveProfiles("test")
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketDataService marketDataService;

    @Test
    void should_getQuote_when_symbolProvided() throws Exception {
        // Given - Controller uses getFundamentals, not getQuote
        StockFundamentals fundamentals = new StockFundamentals("AAPL", "Apple Inc.");
        fundamentals.setCurrentPrice(new BigDecimal("150.00"));
        when(marketDataService.getFundamentals(anyString())).thenReturn(fundamentals);

        // When/Then - Endpoint is /api/market/quote/{symbol}
        mockMvc.perform(get("/api/market/quote/AAPL"))
                .andExpect(status().isOk());
    }

    @Test
    void should_getTopDividendStocks_when_requested() throws Exception {
        // Given
        List<StockFundamentals> stocks = new ArrayList<>();
        StockFundamentals stock = new StockFundamentals("T", "AT&T");
        stock.setDividendYield(new BigDecimal("5.5"));
        stocks.add(stock);

        when(marketDataService.getTopDividendStocks(anyInt())).thenReturn(stocks);

        // When/Then - Endpoint is /api/market/top-dividends
        mockMvc.perform(get("/api/market/top-dividends?limit=10"))
                .andExpect(status().isOk());
    }

    @Test
    void should_searchStocks_when_queryProvided() throws Exception {
        // Given
        List<StockFundamentals> results = new ArrayList<>();
        when(marketDataService.searchStocks(anyString())).thenReturn(results);

        // When/Then
        mockMvc.perform(get("/api/market/search?query=Apple"))
                .andExpect(status().isOk());
    }

    @Test
    void should_getStocksByMinYield_when_minYieldProvided() throws Exception {
        // Given
        List<StockFundamentals> stocks = new ArrayList<>();
        when(marketDataService.getStocksByMinDividendYield(any(BigDecimal.class))).thenReturn(stocks);

        // When/Then - Endpoint is /api/market/dividend-yield
        mockMvc.perform(get("/api/market/dividend-yield?minYield=3.0"))
                .andExpect(status().isOk());
    }
}
