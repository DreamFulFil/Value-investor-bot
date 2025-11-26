package com.valueinvestor.repository;

import com.valueinvestor.model.entity.MarketIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketIndexRepository extends JpaRepository<MarketIndex, Long> {

    /**
     * Find index data within a date range
     */
    List<MarketIndex> findByIndexNameAndDateBetweenOrderByDateAsc(
            String indexName, LocalDate startDate, LocalDate endDate);

    /**
     * Find the latest index value
     */
    @Query("SELECT m FROM MarketIndex m WHERE m.indexName = :indexName " +
           "ORDER BY m.date DESC LIMIT 1")
    Optional<MarketIndex> findLatestByIndexName(@Param("indexName") String indexName);

    /**
     * Find index value for a specific date
     */
    Optional<MarketIndex> findByIndexNameAndDate(String indexName, LocalDate date);

    /**
     * Check if index data exists for a specific name and date
     */
    boolean existsByIndexNameAndDate(String indexName, LocalDate date);

    /**
     * Find all index data for a specific index ordered by date
     */
    List<MarketIndex> findByIndexNameOrderByDateDesc(String indexName);

    /**
     * Get all distinct index names
     */
    @Query("SELECT DISTINCT m.indexName FROM MarketIndex m ORDER BY m.indexName")
    List<String> findDistinctIndexNames();
}
