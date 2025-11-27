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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    
    @Mock
    private ProgressService progressService;

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

        testAnalysis = new AnalysisResults("2330.TW", "Buy recommendation", 85.0, "BUY", "Data");
        
        // Default mock setup for progress service
        doNothing().when(progressService).sendProgress(any(), anyString(), anyInt());
    }

    @Test
    void should_performMonthlyRebalance_when_noRebalanceThisMonth() {
        // Given - last rebalance was 2 months ago
        when(snapshotRepository.findLastMonthlyRebalanceSnapshot())
                .thenReturn(Optional.of(testSnapshot));
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);
        // Provide a watchlist so selectTopStocks works
        when(appConfig.getWatchlist()).thenReturn(
                Arrays.asList("2330.TW", "2317.TW", "2454.TW", "2881.TW", "2882.TW"));
        when(tradingService.createDeposit(any(), any(), anyString())).thenReturn(new TransactionLog());

        Map<String, BigDecimal> allocation = new HashMap<>();
        allocation.put("2330.TW", new BigDecimal("3200.00"));

        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(allocation);
        when(marketDataService.getHistoricalClosePrice(anyString(), any())).thenReturn(new BigDecimal("580.00"));
        
        // Create a proper transaction with totalAmount
        TransactionLog buyTransaction = new TransactionLog();
        buyTransaction.setTotalAmount(new BigDecimal("3200.00"));
        when(tradingService.executeBuy(anyString(), any(), any(), any())).thenReturn(buyTransaction);
        
        // Mock to return a new snapshot for each call (handles catch-up months)
        when(portfolioService.saveSnapshot(anyString())).thenAnswer(invocation -> {
            PortfolioSnapshot snapshot = new PortfolioSnapshot();
            snapshot.setId(System.currentTimeMillis());
            snapshot.setSnapshotType("MONTHLY_REBALANCE");
            return snapshot;
        });

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void should_blockDuplicateRebalance_when_alreadyRebalancedThisMonth() {
        // Given - last rebalance was today
        PortfolioSnapshot recentSnapshot = new PortfolioSnapshot();
        recentSnapshot.setTimestamp(LocalDateTime.now());
        recentSnapshot.setSnapshotType("MONTHLY_REBALANCE");

        when(snapshotRepository.findLastMonthlyRebalanceSnapshot())
                .thenReturn(Optional.of(recentSnapshot));

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then - should succeed but not perform any transactions
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Already rebalanced this month");
        verify(tradingService, never()).executeBuy(anyString(), any(), any(), any());
    }

    @Test
    void should_catchUpMissedMonths_when_rebalanceDelayed() {
        // Given - last rebalance was 3 months ago
        PortfolioSnapshot oldSnapshot = new PortfolioSnapshot();
        oldSnapshot.setTimestamp(LocalDateTime.now().minusMonths(3));
        oldSnapshot.setSnapshotType("MONTHLY_REBALANCE");

        when(snapshotRepository.findLastMonthlyRebalanceSnapshot())
                .thenReturn(Optional.of(oldSnapshot));
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);
        when(appConfig.getWatchlist()).thenReturn(
                Arrays.asList("2330.TW", "2317.TW", "2454.TW", "2881.TW", "2882.TW"));
        when(tradingService.createDeposit(any(), any(), anyString())).thenReturn(new TransactionLog());
        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(new HashMap<>());
        when(portfolioService.saveSnapshot(anyString())).thenAnswer(invocation -> {
            PortfolioSnapshot snapshot = new PortfolioSnapshot();
            snapshot.setId(System.currentTimeMillis());
            snapshot.setSnapshotType("MONTHLY_REBALANCE");
            return snapshot;
        });

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMissedMonths()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void should_triggerRebalance_when_manuallyInvoked() {
        // Given
        when(snapshotRepository.findLastMonthlyRebalanceSnapshot()).thenReturn(Optional.of(testSnapshot));
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);
        when(appConfig.getWatchlist()).thenReturn(
                Arrays.asList("2330.TW", "2317.TW", "2454.TW", "2881.TW", "2882.TW"));
        when(tradingService.createDeposit(any(), any(), anyString())).thenReturn(new TransactionLog());
        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(new HashMap<>());
        when(portfolioService.saveSnapshot(anyString())).thenAnswer(invocation -> {
            PortfolioSnapshot snapshot = new PortfolioSnapshot();
            snapshot.setId(System.currentTimeMillis());
            snapshot.setSnapshotType("MONTHLY_REBALANCE");
            return snapshot;
        });

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
    
    @Test
    void should_handleFirstTimeRebalance_when_noHistory() {
        // Given - no previous rebalance
        when(snapshotRepository.findLastMonthlyRebalanceSnapshot()).thenReturn(Optional.empty());
        when(appConfig.getMonthlyInvestment()).thenReturn(new BigDecimal("16000.00"));
        when(appConfig.getTradingMode()).thenReturn(TransactionLog.TradingMode.SIMULATION);
        when(appConfig.getWatchlist()).thenReturn(
                Arrays.asList("2330.TW", "2317.TW", "2454.TW", "2881.TW", "2882.TW"));
        when(tradingService.createDeposit(any(), any(), anyString())).thenReturn(new TransactionLog());
        when(portfolioService.calculateTargetAllocation(any(), any())).thenReturn(new HashMap<>());
        when(portfolioService.saveSnapshot(anyString())).thenAnswer(invocation -> {
            PortfolioSnapshot snapshot = new PortfolioSnapshot();
            snapshot.setId(System.currentTimeMillis());
            snapshot.setSnapshotType("MONTHLY_REBALANCE");
            return snapshot;
        });

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }
}
