package com.valueinvestor.controller;

import com.valueinvestor.model.entity.StockFundamentals;
import com.valueinvestor.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketDataController.class)
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketDataService marketDataService;

    @Test
    void should_getQuote_when_symbolProvided() throws Exception {
        // Given
        when(marketDataService.getQuote(anyString())).thenReturn(new BigDecimal("150.00"));

        // When/Then
        mockMvc.perform(get("/api/market/quote/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").value(150.00));
    }

    @Test
    void should_getTopDividendStocks_when_requested() throws Exception {
        // Given
        List<StockFundamentals> stocks = new ArrayList<>();
        StockFundamentals stock = new StockFundamentals("T", "AT&T");
        stock.setDividendYield(new BigDecimal("5.5"));
        stocks.add(stock);

        when(marketDataService.getTopDividendStocks(anyInt())).thenReturn(stocks);

        // When/Then
        mockMvc.perform(get("/api/market/top-dividend?limit=10"))
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

        // When/Then
        mockMvc.perform(get("/api/market/by-yield?minYield=3.0"))
                .andExpect(status().isOk());
    }
}
