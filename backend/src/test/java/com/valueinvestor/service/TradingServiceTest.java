package com.valueinvestor.service;

import com.valueinvestor.model.entity.PositionHistory;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.PositionHistoryRepository;
import com.valueinvestor.repository.TransactionLogRepository;
import com.valueinvestor.util.PythonExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradingServiceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private PositionHistoryRepository positionHistoryRepository;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private PythonExecutor pythonExecutor;

    @InjectMocks
    private TradingService tradingService;

    private TransactionLog testTransaction;
    private PositionHistory testPosition;

    @BeforeEach
    void setUp() {
        testTransaction = new TransactionLog(
                TransactionLog.TransactionType.BUY,
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                new BigDecimal("1500.00"),
                TransactionLog.TradingMode.SIMULATION,
                "Test transaction"
        );

        testPosition = new PositionHistory("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
    }

    @Test
    void should_executeBuy_when_simulationMode() {
        // Given
        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("150.00"));
        when(transactionLogRepository.save(any(TransactionLog.class))).thenReturn(testTransaction);
        when(positionHistoryRepository.findLatestPositionBySymbol("AAPL"))
                .thenReturn(Optional.empty());
        when(positionHistoryRepository.save(any(PositionHistory.class))).thenReturn(testPosition);

        // When
        TransactionLog result = tradingService.executeBuy(
                "AAPL",
                new BigDecimal("10"),
                TransactionLog.TradingMode.SIMULATION
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getType()).isEqualTo(TransactionLog.TransactionType.BUY);
        verify(transactionLogRepository).save(any(TransactionLog.class));
        verify(positionHistoryRepository).save(any(PositionHistory.class));
    }

    @Test
    void should_executeBuy_when_priceProvided() {
        // Given
        BigDecimal specificPrice = new BigDecimal("155.00");
        when(transactionLogRepository.save(any(TransactionLog.class))).thenReturn(testTransaction);
        when(positionHistoryRepository.findLatestPositionBySymbol("AAPL"))
                .thenReturn(Optional.empty());
        when(positionHistoryRepository.save(any(PositionHistory.class))).thenReturn(testPosition);

        // When
        TransactionLog result = tradingService.executeBuy(
                "AAPL",
                new BigDecimal("10"),
                TransactionLog.TradingMode.SIMULATION,
                specificPrice
        );

        // Then
        assertThat(result).isNotNull();
        verify(marketDataService, never()).getQuote(anyString());
        verify(transactionLogRepository).save(any(TransactionLog.class));
    }

    @Test
    void should_executeSell_when_simulationMode() {
        // Given
        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("160.00"));
        // Return the argument passed to save so we get the actual SELL transaction
        when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));
        when(positionHistoryRepository.findLatestPositionBySymbol("AAPL"))
                .thenReturn(Optional.of(testPosition));
        when(positionHistoryRepository.save(any(PositionHistory.class))).thenReturn(testPosition);

        // When
        TransactionLog result = tradingService.executeSell(
                "AAPL",
                new BigDecimal("5"),
                TransactionLog.TradingMode.SIMULATION
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getType()).isEqualTo(TransactionLog.TransactionType.SELL);
        verify(transactionLogRepository).save(any(TransactionLog.class));
        verify(positionHistoryRepository).save(any(PositionHistory.class));
    }

    @Test
    void should_updatePosition_when_buyingExistingPosition() {
        // Given
        PositionHistory existingPosition = new PositionHistory("AAPL", new BigDecimal("5"), new BigDecimal("140.00"));

        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("150.00"));
        when(transactionLogRepository.save(any(TransactionLog.class))).thenReturn(testTransaction);
        when(positionHistoryRepository.findLatestPositionBySymbol("AAPL"))
                .thenReturn(Optional.of(existingPosition));
        when(positionHistoryRepository.save(any(PositionHistory.class))).thenAnswer(i -> i.getArgument(0));

        // When
        tradingService.executeBuy("AAPL", new BigDecimal("5"), TransactionLog.TradingMode.SIMULATION);

        // Then
        verify(positionHistoryRepository).save(argThat(position ->
            position.getQuantity().compareTo(new BigDecimal("10")) == 0
        ));
    }

    @Test
    void should_calculateSharesToBuy_when_amountProvided() {
        // Given
        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("150.00"));

        // When
        BigDecimal shares = tradingService.calculateSharesToBuy("AAPL", new BigDecimal("1500.00"));

        // Then
        assertThat(shares).isEqualByComparingTo("10");
        verify(marketDataService).getQuote("AAPL");
    }

    @Test
    void should_throwException_when_priceIsZero() {
        // Given
        when(marketDataService.getQuote("INVALID")).thenReturn(BigDecimal.ZERO);

        // When/Then
        assertThatThrownBy(() ->
            tradingService.calculateSharesToBuy("INVALID", new BigDecimal("1000.00"))
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Cannot get valid price");
    }

    @Test
    void should_testShioajiConnection_when_called() {
        // Given
        when(pythonExecutor.testShioajiConnection()).thenReturn(true);

        // When
        boolean result = tradingService.testShioajiConnection();

        // Then
        assertThat(result).isTrue();
        verify(pythonExecutor).testShioajiConnection();
    }

    @Test
    void should_getCurrentPosition_when_symbolProvided() {
        // Given
        when(positionHistoryRepository.findLatestPositionBySymbol("AAPL"))
                .thenReturn(Optional.of(testPosition));

        // When
        Optional<PositionHistory> result = tradingService.getCurrentPosition("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
        verify(positionHistoryRepository).findLatestPositionBySymbol("AAPL");
    }

    @Test
    void should_executeBuyWithLiveMode_when_orderSucceeds() throws Exception {
        // Given
        PythonExecutor.ShioajiOrderResult orderResult = new PythonExecutor.ShioajiOrderResult();
        orderResult.setSuccess(true);
        orderResult.setFilledQuantity(new BigDecimal("10"));
        orderResult.setFilledPrice(new BigDecimal("150.50"));

        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("150.00"));
        when(pythonExecutor.executeShioajiOrder(eq("BUY"), eq("AAPL"), any(), any()))
                .thenReturn(orderResult);
        when(transactionLogRepository.save(any(TransactionLog.class))).thenReturn(testTransaction);
        when(positionHistoryRepository.findLatestPositionBySymbol("AAPL"))
                .thenReturn(Optional.empty());
        when(positionHistoryRepository.save(any(PositionHistory.class))).thenReturn(testPosition);

        // When
        TransactionLog result = tradingService.executeBuy(
                "AAPL",
                new BigDecimal("10"),
                TransactionLog.TradingMode.LIVE
        );

        // Then
        assertThat(result).isNotNull();
        verify(pythonExecutor).executeShioajiOrder(eq("BUY"), eq("AAPL"), any(), any());
    }

    @Test
    void should_throwException_when_liveOrderFails() throws Exception {
        // Given
        PythonExecutor.ShioajiOrderResult orderResult = new PythonExecutor.ShioajiOrderResult();
        orderResult.setSuccess(false);
        orderResult.setMessage("Order rejected");

        when(marketDataService.getQuote("AAPL")).thenReturn(new BigDecimal("150.00"));
        when(pythonExecutor.executeShioajiOrder(eq("BUY"), eq("AAPL"), any(), any()))
                .thenReturn(orderResult);

        // When/Then
        assertThatThrownBy(() ->
            tradingService.executeBuy("AAPL", new BigDecimal("10"), TransactionLog.TradingMode.LIVE)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Shioaji order failed");
    }
}
