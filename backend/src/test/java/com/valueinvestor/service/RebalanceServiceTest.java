package com.valueinvestor.service;

import com.valueinvestor.config.AppConfig;
import com.valueinvestor.model.entity.AnalysisResults;
import com.valueinvestor.model.entity.PortfolioSnapshot;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.PortfolioSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RebalanceServiceTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private TradingService tradingService;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    @InjectMocks
    private RebalanceService rebalanceService;

    private PortfolioSnapshot testSnapshot;
    private AnalysisResults testAnalysis;

    @BeforeEach
    void setUp() {
        testSnapshot = new PortfolioSnapshot();
        testSnapshot.setId(1L);
        testSnapshot.setTimestamp(LocalDateTime.now().minusMonths(2));
        testSnapshot.setSnapshotType("MONTHLY_REBALANCE");

        testAnalysis = new AnalysisResults("AAPL", "Buy recommendation", 85.0, "BUY", "Data");
    }

    @Test
    void should_performMonthlyRebalance_when_noMissedMonths() {
        // Given
        when(snapshotRepository.findLastMonthlyRebalanceSnapshot())
                .thenReturn(Optional.of(testSnapshot));
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);
        when(appConfig.getWatchlist()).thenReturn(Arrays.asList("AAPL", "MSFT", "GOOGL", "T", "VZ"));

        AnalysisResults analysis1 = new AnalysisResults("AAPL", "Buy", 85.0, "BUY", "Data");
        AnalysisResults analysis2 = new AnalysisResults("MSFT", "Buy", 80.0, "BUY", "Data");
        AnalysisResults analysis3 = new AnalysisResults("GOOGL", "Buy", 75.0, "BUY", "Data");
        AnalysisResults analysis4 = new AnalysisResults("T", "Buy", 70.0, "BUY", "Data");
        AnalysisResults analysis5 = new AnalysisResults("VZ", "Buy", 65.0, "BUY", "Data");

        when(analysisService.getLatestAnalysis(anyString()))
                .thenReturn(Optional.empty());
        when(analysisService.analyzeStock("AAPL")).thenReturn(analysis1);
        when(analysisService.analyzeStock("MSFT")).thenReturn(analysis2);
        when(analysisService.analyzeStock("GOOGL")).thenReturn(analysis3);
        when(analysisService.analyzeStock("T")).thenReturn(analysis4);
        when(analysisService.analyzeStock("VZ")).thenReturn(analysis5);

        Map<String, BigDecimal> allocation = new HashMap<>();
        allocation.put("AAPL", new BigDecimal("100.00"));

        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(allocation);
        when(tradingService.calculateSharesToBuy(anyString(), any())).thenReturn(new BigDecimal("1"));
        when(tradingService.executeBuy(anyString(), any(), any())).thenReturn(new TransactionLog());
        when(portfolioService.saveSnapshot(anyString())).thenReturn(testSnapshot);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void should_catchUpMissedMonths_when_rebalanceDelayed() {
        // Given
        PortfolioSnapshot oldSnapshot = new PortfolioSnapshot();
        oldSnapshot.setTimestamp(LocalDateTime.now().minusMonths(3));
        oldSnapshot.setSnapshotType("MONTHLY_REBALANCE");

        when(snapshotRepository.findLastMonthlyRebalanceSnapshot())
                .thenReturn(Optional.of(oldSnapshot));
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);
        when(appConfig.getWatchlist()).thenReturn(Arrays.asList("AAPL", "MSFT", "GOOGL", "T", "VZ"));

        when(analysisService.getLatestAnalysis(anyString())).thenReturn(Optional.empty());
        when(analysisService.analyzeStock(anyString())).thenReturn(testAnalysis);
        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(new HashMap<>());
        when(portfolioService.saveSnapshot(anyString())).thenReturn(testSnapshot);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMissedMonths()).isGreaterThan(0);
        verify(portfolioService, atLeastOnce()).saveSnapshot("MONTHLY_REBALANCE");
    }

    @Test
    void should_selectTopStocks_when_analysisAvailable() {
        // Given
        when(appConfig.getWatchlist()).thenReturn(Arrays.asList("AAPL", "MSFT", "GOOGL", "T", "VZ", "IBM"));
        when(snapshotRepository.findLastMonthlyRebalanceSnapshot()).thenReturn(Optional.of(testSnapshot));
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);

        AnalysisResults analysis1 = new AnalysisResults("AAPL", "Buy", 90.0, "BUY", "Data");
        AnalysisResults analysis2 = new AnalysisResults("MSFT", "Buy", 85.0, "BUY", "Data");
        AnalysisResults analysis3 = new AnalysisResults("GOOGL", "Hold", 50.0, "HOLD", "Data");
        AnalysisResults analysis4 = new AnalysisResults("T", "Buy", 80.0, "BUY", "Data");
        AnalysisResults analysis5 = new AnalysisResults("VZ", "Buy", 75.0, "BUY", "Data");
        AnalysisResults analysis6 = new AnalysisResults("IBM", "Buy", 70.0, "BUY", "Data");

        when(analysisService.getLatestAnalysis(anyString())).thenReturn(Optional.empty());
        when(analysisService.analyzeStock("AAPL")).thenReturn(analysis1);
        when(analysisService.analyzeStock("MSFT")).thenReturn(analysis2);
        when(analysisService.analyzeStock("GOOGL")).thenReturn(analysis3);
        when(analysisService.analyzeStock("T")).thenReturn(analysis4);
        when(analysisService.analyzeStock("VZ")).thenReturn(analysis5);
        when(analysisService.analyzeStock("IBM")).thenReturn(analysis6);

        Map<String, BigDecimal> allocation = new HashMap<>();
        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(allocation);
        when(portfolioService.saveSnapshot(anyString())).thenReturn(testSnapshot);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        verify(analysisService, times(6)).analyzeStock(anyString());
    }

    @Test
    void should_useRecentAnalysis_when_available() {
        // Given
        AnalysisResults recentAnalysis = new AnalysisResults("AAPL", "Buy", 85.0, "BUY", "Data");
        recentAnalysis.setTimestamp(LocalDateTime.now().minusDays(3));

        when(snapshotRepository.findLastMonthlyRebalanceSnapshot()).thenReturn(Optional.of(testSnapshot));
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);
        when(appConfig.getWatchlist()).thenReturn(Arrays.asList("AAPL"));
        when(analysisService.getLatestAnalysis("AAPL")).thenReturn(Optional.of(recentAnalysis));
        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(new HashMap<>());
        when(portfolioService.saveSnapshot(anyString())).thenReturn(testSnapshot);

        // When
        rebalanceService.performMonthlyRebalance();

        // Then
        verify(analysisService).getLatestAnalysis("AAPL");
        verify(analysisService, never()).analyzeStock("AAPL");
    }

    @Test
    void should_handleErrors_when_stockPurchaseFails() {
        // Given
        when(snapshotRepository.findLastMonthlyRebalanceSnapshot()).thenReturn(Optional.of(testSnapshot));
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);
        when(appConfig.getWatchlist()).thenReturn(Arrays.asList("AAPL"));

        when(analysisService.getLatestAnalysis(anyString())).thenReturn(Optional.empty());
        when(analysisService.analyzeStock("AAPL")).thenReturn(testAnalysis);

        Map<String, BigDecimal> allocation = new HashMap<>();
        allocation.put("AAPL", new BigDecimal("16000.00"));
        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(allocation);
        when(tradingService.calculateSharesToBuy(anyString(), any()))
                .thenThrow(new RuntimeException("Price unavailable"));
        when(portfolioService.saveSnapshot(anyString())).thenReturn(testSnapshot);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void should_triggerRebalance_when_manuallyInvoked() {
        // Given
        when(snapshotRepository.findLastMonthlyRebalanceSnapshot()).thenReturn(Optional.of(testSnapshot));
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);
        when(appConfig.getWatchlist()).thenReturn(new ArrayList<>());
        when(marketDataService.getStocksByMinDividendYield(any())).thenReturn(new ArrayList<>());
        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(new HashMap<>());
        when(portfolioService.saveSnapshot(anyString())).thenReturn(testSnapshot);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.triggerRebalance();

        // Then
        assertThat(result).isNotNull();
        verify(snapshotRepository).findLastMonthlyRebalanceSnapshot();
    }

    @Test
    void should_returnError_when_rebalanceFails() {
        // Given
        when(snapshotRepository.findLastMonthlyRebalanceSnapshot())
                .thenThrow(new RuntimeException("Database error"));

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
    }
}
