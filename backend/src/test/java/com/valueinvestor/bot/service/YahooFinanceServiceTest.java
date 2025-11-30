package com.valueinvestor.bot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valueinvestor.bot.util.PythonExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YahooFinanceServiceTest {

    @Mock
    private PythonExecutor pythonExecutor;

    // We need a real ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private YahooFinanceService yahooFinanceService;
    
    @BeforeEach
    void setUp() {
        // Manually set the object mapper as it's not a mock
        yahooFinanceService = new YahooFinanceService(pythonExecutor, objectMapper);
    }

    @Test
    void getSector_shouldReturnSector_whenScriptSucceeds() throws Exception {
        String jsonResponse = "{\"ticker\": \"2330.TW\", \"sector\": \"Technology\"}";
        when(pythonExecutor.execute("shioaji_bridge/fetch_sector.py", "2330")).thenReturn(jsonResponse);

        Optional<String> sector = yahooFinanceService.getSector("2330");

        assertThat(sector).isPresent().contains("Technology");
    }

    @Test
    void getSector_shouldReturnEmpty_whenScriptFails() throws Exception {
        String jsonResponse = "{\"ticker\": \"-1\", \"error\": \"Some error\"}";
        when(pythonExecutor.execute("shioaji_bridge/fetch_sector.py", "-1")).thenReturn(jsonResponse);

        Optional<String> sector = yahooFinanceService.getSector("-1");

        assertThat(sector).isNotPresent();
    }
    
    @Test
    void getSector_shouldReturnEmpty_whenExecutorReturnsNull() throws Exception {
        when(pythonExecutor.execute(any(), anyString())).thenReturn(null);
        Optional<String> sector = yahooFinanceService.getSector("ANY");
        assertThat(sector).isNotPresent();
    }
}
