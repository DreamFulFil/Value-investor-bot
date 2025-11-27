package com.valueinvestor.repository;

import com.valueinvestor.model.entity.TransactionLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TransactionLogRepositoryTest {

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Test
    void should_findByType_when_transactionsExist() {
        // Given
        TransactionLog transaction = new TransactionLog(
                TransactionLog.TransactionType.BUY,
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                new BigDecimal("1500.00"),
                TransactionLog.TradingMode.SIMULATION,
                "Test"
        );
        transactionLogRepository.save(transaction);

        // When
        List<TransactionLog> found = transactionLogRepository.findByTypeOrderByTimestampDesc(
                TransactionLog.TransactionType.BUY);

        // Then
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void should_findBySymbol_when_transactionsExist() {
        // Given
        TransactionLog transaction = new TransactionLog(
                TransactionLog.TransactionType.BUY,
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                new BigDecimal("1500.00"),
                TransactionLog.TradingMode.SIMULATION,
                "Test"
        );
        transactionLogRepository.save(transaction);

        // When
        List<TransactionLog> found = transactionLogRepository.findBySymbolOrderByTimestampDesc("AAPL");

        // Then
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getType()).isEqualTo(TransactionLog.TransactionType.BUY);
    }

    @Test
    void should_findRecentTransactions_when_dateRangeProvided() {
        // Given
        TransactionLog transaction = new TransactionLog(
                TransactionLog.TransactionType.BUY,
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                new BigDecimal("1500.00"),
                TransactionLog.TradingMode.SIMULATION,
                "Test"
        );
        transactionLogRepository.save(transaction);

        // When
        List<TransactionLog> found = transactionLogRepository.findRecentTransactions(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        // Then
        assertThat(found).isNotEmpty();
    }
}
