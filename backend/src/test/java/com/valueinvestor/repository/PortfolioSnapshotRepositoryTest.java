package com.valueinvestor.repository;

import com.valueinvestor.model.entity.PortfolioSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PortfolioSnapshotRepositoryTest {

    @Autowired
    private PortfolioSnapshotRepository snapshotRepository;

    @Test
    void should_findLatestSnapshot_when_snapshotsExist() {
        // Given
        PortfolioSnapshot snapshot = new PortfolioSnapshot(
                new BigDecimal("10000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("9000.00"),
                new BigDecimal("1000.00"),
                "[]",
                "TEST"
        );
        snapshotRepository.save(snapshot);

        // When
        Optional<PortfolioSnapshot> found = snapshotRepository.findLatestSnapshot();

        // Then
        assertThat(found).isPresent();
    }

    @Test
    void should_findByTimestampBetween_when_dateRangeProvided() {
        // Given
        PortfolioSnapshot snapshot = new PortfolioSnapshot(
                new BigDecimal("10000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("9000.00"),
                new BigDecimal("1000.00"),
                "[]",
                "TEST"
        );
        snapshotRepository.save(snapshot);

        // When
        List<PortfolioSnapshot> found = snapshotRepository.findByTimestampBetweenOrderByTimestampDesc(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        // Then
        assertThat(found).isNotEmpty();
    }

    @Test
    void should_findLastMonthlyRebalanceSnapshot_when_rebalanceSnapshotExists() {
        // Given
        PortfolioSnapshot snapshot = new PortfolioSnapshot(
                new BigDecimal("10000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("9000.00"),
                new BigDecimal("1000.00"),
                "[]",
                "MONTHLY_REBALANCE"
        );
        snapshotRepository.save(snapshot);

        // When
        Optional<PortfolioSnapshot> found = snapshotRepository.findLastMonthlyRebalanceSnapshot();

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSnapshotType()).isEqualTo("MONTHLY_REBALANCE");
    }
}
