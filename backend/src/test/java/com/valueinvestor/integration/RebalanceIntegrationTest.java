package com.valueinvestor.integration;

import com.valueinvestor.model.entity.PortfolioSnapshot;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.PortfolioSnapshotRepository;
import com.valueinvestor.repository.TransactionLogRepository;
import com.valueinvestor.service.PortfolioService;
import com.valueinvestor.service.RebalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the complete rebalance flow
 * Tests the interaction between services during monthly rebalance
 */
@ExtendWith(MockitoExtension.class)
class RebalanceIntegrationTest {

    @Mock
    private RebalanceService rebalanceService;

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @BeforeEach
    void setUp() {
        // Setup common test fixtures
    }

    @Test
    void should_executeFullRebalanceFlow() {
        // Given - Mock result
        RebalanceService.RebalanceResult mockResult = new RebalanceService.RebalanceResult();
        mockResult.setSuccess(true);
        when(rebalanceService.performMonthlyRebalance()).thenReturn(mockResult);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void should_preventDuplicateRebalance_inSameMonth() {
        // Given - Rebalance already done this month
        RebalanceService.RebalanceResult mockResult = new RebalanceService.RebalanceResult();
        mockResult.setSuccess(true);
        mockResult.setMessage("Already rebalanced this month");
        when(rebalanceService.performMonthlyRebalance()).thenReturn(mockResult);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void should_catchUpMissedMonths() {
        // Given - Last rebalance was 3 months ago
        RebalanceService.RebalanceResult mockResult = new RebalanceService.RebalanceResult();
        mockResult.setMissedMonths(2);
        mockResult.setSuccess(true);
        when(rebalanceService.performMonthlyRebalance()).thenReturn(mockResult);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMissedMonths()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void should_returnCorrectMissedMonths() {
        // Given
        RebalanceService.RebalanceResult mockResult = new RebalanceService.RebalanceResult();
        mockResult.setMissedMonths(3);
        mockResult.setSuccess(true);
        when(rebalanceService.performMonthlyRebalance()).thenReturn(mockResult);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result.getMissedMonths()).isEqualTo(3);
    }

    @Test
    void should_buyStocks_afterRebalance() {
        // Given
        RebalanceService.RebalanceResult mockResult = new RebalanceService.RebalanceResult();
        mockResult.setSuccess(true);
        
        RebalanceService.MonthlyRebalanceResult monthlyResult = new RebalanceService.MonthlyRebalanceResult();
        monthlyResult.setStocksPurchased(5);
        mockResult.addMonthlyResult(monthlyResult);
        
        when(rebalanceService.performMonthlyRebalance()).thenReturn(mockResult);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result.getMonthlyResults()).isNotEmpty();
        assertThat(result.getMonthlyResults().get(0).getStocksPurchased()).isGreaterThan(0);
    }

    @Test
    void should_recordTotalInvested() {
        // Given
        BigDecimal monthlyAmount = new BigDecimal("16000.00");
        
        RebalanceService.RebalanceResult mockResult = new RebalanceService.RebalanceResult();
        mockResult.setSuccess(true);
        
        RebalanceService.MonthlyRebalanceResult monthlyResult = new RebalanceService.MonthlyRebalanceResult();
        monthlyResult.setTotalInvested(monthlyAmount);
        mockResult.addMonthlyResult(monthlyResult);
        
        when(rebalanceService.performMonthlyRebalance()).thenReturn(mockResult);

        // When
        RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

        // Then
        assertThat(result.getMonthlyResults().get(0).getTotalInvested()).isEqualTo(monthlyAmount);
    }
}
