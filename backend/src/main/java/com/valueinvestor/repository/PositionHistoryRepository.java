package com.valueinvestor.repository;

import com.valueinvestor.model.entity.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    List<PositionHistory> findBySymbolOrderByTimestampDesc(String symbol);

    List<PositionHistory> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT p FROM PositionHistory p WHERE p.timestamp = " +
           "(SELECT MAX(p2.timestamp) FROM PositionHistory p2 WHERE p2.symbol = p.symbol) " +
           "ORDER BY p.symbol")
    List<PositionHistory> findLatestPositions();

    @Query("SELECT p FROM PositionHistory p WHERE p.symbol = ?1 " +
           "ORDER BY p.timestamp DESC LIMIT 1")
    Optional<PositionHistory> findLatestPositionBySymbol(String symbol);

    @Query("SELECT p FROM PositionHistory p WHERE p.timestamp >= ?1 ORDER BY p.timestamp DESC")
    List<PositionHistory> findPositionsSince(LocalDateTime since);

    @Query("SELECT DISTINCT p.symbol FROM PositionHistory p WHERE p.quantity > 0")
    List<String> findAllSymbolsWithPositions();
}
