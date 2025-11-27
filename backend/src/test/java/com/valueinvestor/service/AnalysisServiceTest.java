package com.valueinvestor.service;

import com.valueinvestor.model.entity.AnalysisResults;
import com.valueinvestor.model.entity.StockFundamentals;
import com.valueinvestor.repository.AnalysisResultsRepository;
import com.valueinvestor.util.OllamaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalysisServiceTest {

    @Mock
    private AnalysisResultsRepository analysisRepository;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private OllamaClient ollamaClient;

    @InjectMocks
    private AnalysisService analysisService;

    private StockFundamentals testFundamentals;
    private AnalysisResults testAnalysis;

    @BeforeEach
    void setUp() {
        testFundamentals = new StockFundamentals("AAPL", "Apple Inc.");
        testFundamentals.setCurrentPrice(new BigDecimal("150.00"));
        testFundamentals.setDividendYield(new BigDecimal("0.5"));
        testFundamentals.setPeRatio(new BigDecimal("25.0"));
        testFundamentals.setMarketCap(new BigDecimal("2500000000000"));

        testAnalysis = new AnalysisResults(
                "AAPL",
                "Strong buy recommendation. RECOMMENDATION: BUY SCORE: 85",
                85.0,
                "BUY",
                "Fundamentals data"
        );
    }

    @Test
    void should_analyzeStock_when_fundamentalsAvailable() throws Exception {
        // Given
        when(marketDataService.getFundamentals("AAPL")).thenReturn(testFundamentals);
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.sendPrompt(anyString(), anyString()))
                .thenReturn("Strong buy recommendation. RECOMMENDATION: BUY SCORE: 85");
        when(analysisRepository.save(any(AnalysisResults.class))).thenReturn(testAnalysis);

        // When
        AnalysisResults result = analysisService.analyzeStock("AAPL");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getRecommendation()).isEqualTo("BUY");
        assertThat(result.getScore()).isEqualTo(85.0);
        verify(marketDataService).getFundamentals("AAPL");
        verify(ollamaClient).isAvailable();
        verify(ollamaClient).sendPrompt(anyString(), anyString());
        verify(analysisRepository).save(any(AnalysisResults.class));
    }

    @Test
    void should_createRuleBasedAnalysis_when_fundamentalsNotAvailable() {
        // Given - when fundamentals are null, service uses rule-based fallback with BUY
        when(marketDataService.getFundamentals("INVALID")).thenReturn(null);
        when(analysisRepository.save(any(AnalysisResults.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AnalysisResults result = analysisService.analyzeStock("INVALID");

        // Then - rule-based analysis returns BUY with score 65 for stocks in universe
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("INVALID");
        assertThat(result.getRecommendation()).isEqualTo("BUY"); // Default for universe stocks
        assertThat(result.getScore()).isEqualTo(65.0);
        verify(analysisRepository).save(any(AnalysisResults.class));
    }

    @Test
    void should_getLatestAnalysis_when_analysisExists() {
        // Given
        when(analysisRepository.findLatestAnalysisBySymbol("AAPL"))
                .thenReturn(Optional.of(testAnalysis));

        // When
        Optional<AnalysisResults> result = analysisService.getLatestAnalysis("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
        verify(analysisRepository).findLatestAnalysisBySymbol("AAPL");
    }

    @Test
    void should_getRecentAnalyses_when_limitProvided() {
        // Given
        List<AnalysisResults> analyses = new ArrayList<>();
        analyses.add(testAnalysis);

        when(analysisRepository.findRecentAnalyses(10)).thenReturn(analyses);

        // When
        List<AnalysisResults> result = analysisService.getRecentAnalyses(10);

        // Then
        assertThat(result).hasSize(1);
        verify(analysisRepository).findRecentAnalyses(10);
    }

    @Test
    void should_getStockAnalysisHistory_when_symbolProvided() {
        // Given
        List<AnalysisResults> history = new ArrayList<>();
        history.add(testAnalysis);

        when(analysisRepository.findBySymbolOrderByTimestampDesc("AAPL")).thenReturn(history);

        // When
        List<AnalysisResults> result = analysisService.getStockAnalysisHistory("AAPL");

        // Then
        assertThat(result).hasSize(1);
        verify(analysisRepository).findBySymbolOrderByTimestampDesc("AAPL");
    }

    @Test
    void should_getBuyRecommendations_when_dateProvided() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<AnalysisResults> buyRecommendations = new ArrayList<>();
        buyRecommendations.add(testAnalysis);

        when(analysisRepository.findBuyRecommendationsSince(since)).thenReturn(buyRecommendations);

        // When
        List<AnalysisResults> result = analysisService.getBuyRecommendationsSince(since);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecommendation()).isEqualTo("BUY");
        verify(analysisRepository).findBuyRecommendationsSince(since);
    }

    @Test
    void should_getTopRatedStocks_when_minScoreProvided() {
        // Given
        Double minScore = 70.0;
        List<AnalysisResults> topRated = new ArrayList<>();
        topRated.add(testAnalysis);

        when(analysisRepository.findByMinScore(minScore)).thenReturn(topRated);

        // When
        List<AnalysisResults> result = analysisService.getTopRatedStocks(minScore);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isGreaterThanOrEqualTo(minScore);
        verify(analysisRepository).findByMinScore(minScore);
    }

    @Test
    void should_checkOllamaAvailability_when_called() {
        // Given
        when(ollamaClient.isAvailable()).thenReturn(true);

        // When
        boolean result = analysisService.isOllamaAvailable();

        // Then
        assertThat(result).isTrue();
        verify(ollamaClient).isAvailable();
    }

    @Test
    void should_checkModelAvailability_when_called() {
        // Given
        when(ollamaClient.isModelAvailable(anyString())).thenReturn(true);

        // When
        boolean result = analysisService.isAnalysisModelAvailable();

        // Then
        assertThat(result).isTrue();
        verify(ollamaClient).isModelAvailable(anyString());
    }

    @Test
    void should_parseRecommendation_when_holdKeywordPresent() throws Exception {
        // Given
        when(marketDataService.getFundamentals("AAPL")).thenReturn(testFundamentals);
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.sendPrompt(anyString(), anyString()))
                .thenReturn("The stock is neutral, recommend to hold position. RECOMMENDATION: HOLD SCORE: 50");
        when(analysisRepository.save(any(AnalysisResults.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AnalysisResults result = analysisService.analyzeStock("AAPL");

        // Then
        assertThat(result.getRecommendation()).isEqualTo("HOLD");
    }

    @Test
    void should_parseRecommendation_when_sellKeywordPresent() throws Exception {
        // Given
        when(marketDataService.getFundamentals("AAPL")).thenReturn(testFundamentals);
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.sendPrompt(anyString(), anyString()))
                .thenReturn("Overvalued, recommend to sell. RECOMMENDATION: SELL SCORE: 25");
        when(analysisRepository.save(any(AnalysisResults.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AnalysisResults result = analysisService.analyzeStock("AAPL");

        // Then
        assertThat(result.getRecommendation()).isEqualTo("SELL");
        assertThat(result.getScore()).isEqualTo(25.0);
    }
}
