package com.valueinvestor.service;

import com.valueinvestor.model.entity.PortfolioSnapshot;
import com.valueinvestor.model.entity.PositionHistory;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.PortfolioSnapshotRepository;
import com.valueinvestor.repository.PositionHistoryRepository;
import com.valueinvestor.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PositionHistoryRepository positionHistoryRepository;

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private PortfolioService portfolioService;

    private PositionHistory testPosition;
    private TransactionLog testTransaction;

    @BeforeEach
    void setUp() {
        testPosition = new PositionHistory("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        testPosition.setCurrentPrice(new BigDecimal("160.00"));
        testPosition.calculateMetrics();

        testTransaction = new TransactionLog(
                TransactionLog.TransactionType.BUY,
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                new BigDecimal("1500.00"),
                TransactionLog.TradingMode.SIMULATION,
                "Test transaction"
        );
    }

    @Test
    void should_getCurrentPortfolio_when_positionsExist() {
        // Given
        List<PositionHistory> positions = new ArrayList<>();
        positions.add(testPosition);

        when(positionHistoryRepository.findLatestPositions()).thenReturn(positions);
        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("160.00"));

        // When
        List<PositionHistory> result = portfolioService.getCurrentPortfolio();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
        verify(positionHistoryRepository).findLatestPositions();
    }

    @Test
    void should_getTotalValue_when_portfolioHasPositions() {
        // Given
        List<PositionHistory> positions = new ArrayList<>();
        positions.add(testPosition);

        when(positionHistoryRepository.findLatestPositions()).thenReturn(positions);
        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("160.00"));
        when(transactionLogRepository.findByTypeOrderByTimestampDesc(any())).thenReturn(new ArrayList<>());

        // When
        BigDecimal totalValue = portfolioService.getTotalValue();

        // Then
        assertThat(totalValue).isNotNull();
        assertThat(totalValue).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void should_getCashBalance_when_depositsAndTransactionsExist() {
        // Given
        List<TransactionLog> deposits = new ArrayList<>();
        TransactionLog deposit = new TransactionLog(
                TransactionLog.TransactionType.DEPOSIT,
                null, null, null,
                new BigDecimal("5000.00"),
                TransactionLog.TradingMode.SIMULATION,
                "Initial deposit"
        );
        deposits.add(deposit);

        when(transactionLogRepository.findByTypeOrderByTimestampDesc(TransactionLog.TransactionType.DEPOSIT))
                .thenReturn(deposits);
        when(transactionLogRepository.findByTypeOrderByTimestampDesc(TransactionLog.TransactionType.BUY))
                .thenReturn(new ArrayList<>());
        when(transactionLogRepository.findByTypeOrderByTimestampDesc(TransactionLog.TransactionType.SELL))
                .thenReturn(new ArrayList<>());

        // When
        BigDecimal cash = portfolioService.getCashBalance();

        // Then
        assertThat(cash).isEqualByComparingTo("5000.00");
        verify(transactionLogRepository, times(3)).findByTypeOrderByTimestampDesc(any());
    }

    @Test
    void should_calculateTargetAllocation_when_stocksAreProvided() {
        // Given
        BigDecimal monthlyInvestment = new BigDecimal("16000.00");
        List<String> selectedSymbols = List.of("AAPL", "MSFT", "GOOGL");

        // When
        Map<String, BigDecimal> allocation = portfolioService.calculateTargetAllocation(monthlyInvestment, selectedSymbols);

        // Then
        assertThat(allocation).hasSize(3);
        assertThat(allocation.get("AAPL")).isEqualByComparingTo("166.67");
        assertThat(allocation.get("MSFT")).isEqualByComparingTo("166.67");
        assertThat(allocation.get("GOOGL")).isEqualByComparingTo("166.67");
    }

    @Test
    void should_returnEmptyAllocation_when_noSymbolsProvided() {
        // Given
        BigDecimal monthlyInvestment = new BigDecimal("16000.00");
        List<String> selectedSymbols = new ArrayList<>();

        // When
        Map<String, BigDecimal> allocation = portfolioService.calculateTargetAllocation(monthlyInvestment, selectedSymbols);

        // Then
        assertThat(allocation).isEmpty();
    }

    @Test
    void should_saveSnapshot_when_snapshotTypeProvided() {
        // Given
        List<PositionHistory> positions = new ArrayList<>();
        positions.add(testPosition);

        when(positionHistoryRepository.findLatestPositions()).thenReturn(positions);
        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("160.00"));
        when(transactionLogRepository.findByTypeOrderByTimestampDesc(any())).thenReturn(new ArrayList<>());
        when(snapshotRepository.save(any(PortfolioSnapshot.class))).thenAnswer(i -> {
            PortfolioSnapshot snapshot = i.getArgument(0);
            snapshot.setId(1L);
            return snapshot;
        });

        // When
        PortfolioSnapshot snapshot = portfolioService.saveSnapshot("TEST");

        // Then
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getSnapshotType()).isEqualTo("TEST");
        verify(snapshotRepository).save(any(PortfolioSnapshot.class));
    }

    @Test
    void should_recordDeposit_when_amountIsValid() {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");
        String notes = "Test deposit";

        when(transactionLogRepository.save(any(TransactionLog.class))).thenReturn(testTransaction);
        when(positionHistoryRepository.findLatestPositions()).thenReturn(new ArrayList<>());
        when(transactionLogRepository.findByTypeOrderByTimestampDesc(any())).thenReturn(new ArrayList<>());
        when(snapshotRepository.save(any(PortfolioSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        // When
        TransactionLog result = portfolioService.recordDeposit(amount, notes);

        // Then
        assertThat(result).isNotNull();
        verify(transactionLogRepository).save(any(TransactionLog.class));
        verify(snapshotRepository).save(any(PortfolioSnapshot.class));
    }

    @Test
    void should_getPortfolioHistory_when_dateRangeProvided() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        List<PortfolioSnapshot> snapshots = new ArrayList<>();

        when(snapshotRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate))
                .thenReturn(snapshots);

        // When
        List<PortfolioSnapshot> result = portfolioService.getPortfolioHistory(startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        verify(snapshotRepository).findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
    }

    @Test
    void should_getLatestSnapshot_when_snapshotExists() {
        // Given
        PortfolioSnapshot snapshot = new PortfolioSnapshot();
        when(snapshotRepository.findLatestSnapshot()).thenReturn(Optional.of(snapshot));

        // When
        Optional<PortfolioSnapshot> result = portfolioService.getLatestSnapshot();

        // Then
        assertThat(result).isPresent();
        verify(snapshotRepository).findLatestSnapshot();
    }

    @Test
    void should_getPerformanceMetrics_when_portfolioExists() {
        // Given
        List<PositionHistory> positions = new ArrayList<>();
        positions.add(testPosition);

        when(positionHistoryRepository.findLatestPositions()).thenReturn(positions);
        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("160.00"));
        when(transactionLogRepository.findByTypeOrderByTimestampDesc(any())).thenReturn(new ArrayList<>());

        // When
        Map<String, Object> metrics = portfolioService.getPerformanceMetrics();

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics).containsKeys("totalValue", "cashBalance", "investedAmount", "totalPL", "plPercentage", "positionCount");
        assertThat(metrics.get("positionCount")).isEqualTo(1);
    }
}
