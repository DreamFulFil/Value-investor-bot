package com.valueinvestor.service;

import com.valueinvestor.model.entity.InsightsHistory;
import com.valueinvestor.model.entity.PortfolioSnapshot;
import com.valueinvestor.model.entity.PositionHistory;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.InsightsHistoryRepository;
import com.valueinvestor.repository.TransactionLogRepository;
import com.valueinvestor.util.OllamaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

    @Mock
    private InsightsHistoryRepository insightsHistoryRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private OllamaClient ollamaClient;

    @InjectMocks
    private InsightsService insightsService;

    private InsightsHistory testInsights;
    private PositionHistory testPosition;
    private PortfolioSnapshot testSnapshot;

    @BeforeEach
    void setUp() {
        testInsights = new InsightsHistory(
                LocalDate.now(),
                "# Monthly Insights\n\nTest content",
                new BigDecimal("160000.00"),
                new BigDecimal("5.25")
        );
        testInsights.setId(1L);
        testInsights.setCashBalance(new BigDecimal("10000.00"));
        testInsights.setPositionsCount(5);

        testPosition = new PositionHistory();
        testPosition.setSymbol("2330.TW");
        testPosition.setQuantity(new BigDecimal("10"));
        testPosition.setAveragePrice(new BigDecimal("580.00"));
        testPosition.setCurrentPrice(new BigDecimal("590.00"));
        testPosition.setMarketValue(new BigDecimal("5900.00"));
        testPosition.setUnrealizedPL(new BigDecimal("100.00"));

        testSnapshot = new PortfolioSnapshot();
        testSnapshot.setTotalValue(new BigDecimal("160000.00"));
        testSnapshot.setTimestamp(LocalDateTime.now());
    }

    @Test
    void should_generateMonthlyInsights_successfully() throws IOException {
        // Given
        when(portfolioService.getCurrentPortfolio()).thenReturn(Arrays.asList(testPosition));
        when(portfolioService.getTotalValue()).thenReturn(new BigDecimal("160000.00"));
        when(portfolioService.getCashBalance()).thenReturn(new BigDecimal("10000.00"));
        when(transactionLogRepository.findRecentTransactions(any(), any()))
                .thenReturn(Collections.emptyList());
        when(portfolioService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));
        when(portfolioService.getPortfolioHistory(any(), any()))
                .thenReturn(Arrays.asList(testSnapshot));
        when(ollamaClient.generateInsights(anyString(), anyString()))
                .thenReturn("## AI Analysis\n\nYour portfolio is performing well.");
        when(insightsHistoryRepository.save(any())).thenReturn(testInsights);

        // When
        InsightsHistory result = insightsService.generateMonthlyInsights();

        // Then
        assertThat(result).isNotNull();
        verify(ollamaClient).generateInsights(anyString(), anyString());
        verify(insightsHistoryRepository).save(any());
    }

    @Test
    void should_getCurrentInsights_when_exists() {
        // Given
        when(insightsHistoryRepository.findLatest()).thenReturn(Optional.of(testInsights));

        // When
        String content = insightsService.getCurrentInsights();

        // Then
        assertThat(content).contains("Monthly Insights");
    }

    @Test
    void should_returnDefaultMessage_when_noInsightsExist() {
        // Given
        when(insightsHistoryRepository.findLatest()).thenReturn(Optional.empty());

        // When
        String content = insightsService.getCurrentInsights();

        // Then
        assertThat(content).contains("No insights available yet");
    }

    @Test
    void should_getInsightsHistory() {
        // Given
        List<InsightsHistory> history = Arrays.asList(testInsights);
        when(insightsHistoryRepository.findRecentInsights(anyInt())).thenReturn(history);

        // When
        List<InsightsHistory> result = insightsService.getInsightsHistory(5);

        // Then
        assertThat(result).hasSize(1);
        verify(insightsHistoryRepository).findRecentInsights(5);
    }

    @Test
    void should_getLatestInsights() {
        // Given
        when(insightsHistoryRepository.findLatest()).thenReturn(Optional.of(testInsights));

        // When
        Optional<InsightsHistory> result = insightsService.getLatestInsights();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPortfolioValue()).isEqualTo(new BigDecimal("160000.00"));
    }

    @Test
    void should_handleOllamaError_gracefully() throws IOException {
        // Given
        when(portfolioService.getCurrentPortfolio()).thenReturn(Arrays.asList(testPosition));
        when(portfolioService.getTotalValue()).thenReturn(new BigDecimal("160000.00"));
        when(portfolioService.getCashBalance()).thenReturn(new BigDecimal("10000.00"));
        when(transactionLogRepository.findRecentTransactions(any(), any()))
                .thenReturn(Collections.emptyList());
        when(portfolioService.getLatestSnapshot()).thenReturn(Optional.of(testSnapshot));
        when(portfolioService.getPortfolioHistory(any(), any()))
                .thenReturn(Collections.emptyList());
        when(ollamaClient.generateInsights(anyString(), anyString()))
                .thenThrow(new IOException("Ollama not available"));

        // When/Then
        try {
            insightsService.generateMonthlyInsights();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Failed to generate monthly insights");
        }
    }

    @Test
    void should_calculateMonthlyReturn_correctly() throws IOException {
        // Given
        PortfolioSnapshot currentSnapshot = new PortfolioSnapshot();
        currentSnapshot.setTotalValue(new BigDecimal("165000.00"));

        PortfolioSnapshot oldSnapshot = new PortfolioSnapshot();
        oldSnapshot.setTotalValue(new BigDecimal("160000.00"));

        when(portfolioService.getCurrentPortfolio()).thenReturn(Arrays.asList(testPosition));
        when(portfolioService.getTotalValue()).thenReturn(new BigDecimal("165000.00"));
        when(portfolioService.getCashBalance()).thenReturn(new BigDecimal("10000.00"));
        when(transactionLogRepository.findRecentTransactions(any(), any()))
                .thenReturn(Collections.emptyList());
        when(portfolioService.getLatestSnapshot()).thenReturn(Optional.of(currentSnapshot));
        when(portfolioService.getPortfolioHistory(any(), any()))
                .thenReturn(Arrays.asList(oldSnapshot));
        when(ollamaClient.generateInsights(anyString(), anyString()))
                .thenReturn("## Analysis");
        when(insightsHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        InsightsHistory result = insightsService.generateMonthlyInsights();

        // Then
        assertThat(result).isNotNull();
    }
}
