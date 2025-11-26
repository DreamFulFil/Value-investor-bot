package com.valueinvestor.repository;

import com.valueinvestor.model.entity.InsightsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsightsHistoryRepository extends JpaRepository<InsightsHistory, Long> {

    /**
     * Find insights by date
     */
    Optional<InsightsHistory> findByGeneratedDate(LocalDate generatedDate);

    /**
     * Find latest insights
     */
    @Query("SELECT i FROM InsightsHistory i ORDER BY i.generatedDate DESC LIMIT 1")
    Optional<InsightsHistory> findLatest();

    /**
     * Find recent insights
     */
    @Query("SELECT i FROM InsightsHistory i ORDER BY i.generatedDate DESC")
    List<InsightsHistory> findAllOrderByGeneratedDateDesc();

    /**
     * Check if insights exist for date
     */
    boolean existsByGeneratedDate(LocalDate generatedDate);

    /**
     * Find insights in date range
     */
    List<InsightsHistory> findByGeneratedDateBetweenOrderByGeneratedDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * Get insights for last N months
     */
    @Query(value = "SELECT i FROM InsightsHistory i ORDER BY i.generatedDate DESC LIMIT :limit")
    List<InsightsHistory> findRecentInsights(int limit);
}
