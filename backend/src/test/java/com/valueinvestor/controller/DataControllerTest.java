package com.valueinvestor.controller;

import com.valueinvestor.model.entity.StockUniverse;
import com.valueinvestor.service.DataCatchUpService;
import com.valueinvestor.service.DataRefreshScheduler;
import com.valueinvestor.service.HistoricalDataService;
import com.valueinvestor.service.StockUniverseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataController.class)
@ActiveProfiles("test")
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockUniverseService stockUniverseService;
    
    @MockBean
    private HistoricalDataService historicalDataService;
    
    @MockBean
    private DataCatchUpService dataCatchUpService;
    
    @MockBean
    private DataRefreshScheduler dataRefreshScheduler;

    @Test
    void should_getStockUniverse_when_requested() throws Exception {
        // Given
        when(stockUniverseService.getActiveStocksAsDTO()).thenReturn(new ArrayList<>());

        // When/Then
        mockMvc.perform(get("/api/data/universe"))
                .andExpect(status().isOk());
    }

    @Test
    void should_getSectors_when_requested() throws Exception {
        // Given
        List<String> sectors = List.of("Technology", "Finance", "Healthcare");
        when(stockUniverseService.getAllSectors()).thenReturn(sectors);

        // When/Then
        mockMvc.perform(get("/api/data/universe/sectors"))
                .andExpect(status().isOk());
    }
}
