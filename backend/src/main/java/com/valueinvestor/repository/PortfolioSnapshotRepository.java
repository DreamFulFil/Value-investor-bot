package com.valueinvestor.repository;

import com.valueinvestor.model.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startDate, LocalDateTime endDate);

    List<PortfolioSnapshot> findBySnapshotTypeOrderByTimestampDesc(String snapshotType);

    @Query("SELECT p FROM PortfolioSnapshot p ORDER BY p.timestamp DESC LIMIT 1")
    Optional<PortfolioSnapshot> findLatestSnapshot();

    @Query("SELECT p FROM PortfolioSnapshot p WHERE p.timestamp >= ?1 ORDER BY p.timestamp ASC")
    List<PortfolioSnapshot> findSnapshotsSince(LocalDateTime since);

    @Query("SELECT p FROM PortfolioSnapshot p WHERE p.snapshotType = 'MONTHLY_REBALANCE' " +
           "ORDER BY p.timestamp DESC")
    List<PortfolioSnapshot> findMonthlyRebalanceSnapshots();

    @Query("SELECT p FROM PortfolioSnapshot p WHERE p.snapshotType = 'MONTHLY_REBALANCE' " +
           "ORDER BY p.timestamp DESC LIMIT 1")
    Optional<PortfolioSnapshot> findLastMonthlyRebalanceSnapshot();
}
