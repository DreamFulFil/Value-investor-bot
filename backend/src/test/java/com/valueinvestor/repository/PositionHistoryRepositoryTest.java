package com.valueinvestor.repository;

import com.valueinvestor.model.entity.PositionHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PositionHistoryRepositoryTest {

    @Autowired
    private PositionHistoryRepository positionHistoryRepository;

    @Test
    void should_findLatestPositions_when_positionsExist() {
        // Given
        PositionHistory position = new PositionHistory("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        positionHistoryRepository.save(position);

        // When
        List<PositionHistory> found = positionHistoryRepository.findLatestPositions();

        // Then
        assertThat(found).isNotEmpty();
    }

    @Test
    void should_findLatestPositionBySymbol_when_symbolProvided() {
        // Given
        PositionHistory position = new PositionHistory("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        positionHistoryRepository.save(position);

        // When
        Optional<PositionHistory> found = positionHistoryRepository.findLatestPositionBySymbol("AAPL");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void should_returnEmpty_when_positionNotFound() {
        // When
        Optional<PositionHistory> found = positionHistoryRepository.findLatestPositionBySymbol("INVALID");

        // Then
        assertThat(found).isEmpty();
    }
}
