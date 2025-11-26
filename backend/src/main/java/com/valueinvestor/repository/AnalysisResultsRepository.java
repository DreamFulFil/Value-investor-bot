package com.valueinvestor.repository;

import com.valueinvestor.model.entity.AnalysisResults;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultsRepository extends JpaRepository<AnalysisResults, Long> {

    List<AnalysisResults> findBySymbolOrderByTimestampDesc(String symbol);

    List<AnalysisResults> findByRecommendationOrderByTimestampDesc(String recommendation);

    List<AnalysisResults> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT a FROM AnalysisResults a WHERE a.symbol = ?1 " +
           "ORDER BY a.timestamp DESC LIMIT 1")
    Optional<AnalysisResults> findLatestAnalysisBySymbol(String symbol);

    @Query("SELECT a FROM AnalysisResults a ORDER BY a.timestamp DESC LIMIT ?1")
    List<AnalysisResults> findRecentAnalyses(int limit);

    @Query("SELECT a FROM AnalysisResults a WHERE a.recommendation = 'BUY' " +
           "AND a.timestamp >= ?1 ORDER BY a.score DESC")
    List<AnalysisResults> findBuyRecommendationsSince(LocalDateTime since);

    @Query("SELECT a FROM AnalysisResults a WHERE a.score >= ?1 " +
           "ORDER BY a.score DESC, a.timestamp DESC")
    List<AnalysisResults> findByMinScore(Double minScore);

    @Query("SELECT DISTINCT a.symbol FROM AnalysisResults a WHERE a.timestamp >= ?1")
    List<String> findAnalyzedSymbolsSince(LocalDateTime since);
}
