package com.valueinvestor.repository;

import com.valueinvestor.model.entity.DailyLearningTip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLearningTipRepository extends JpaRepository<DailyLearningTip, Long> {

    /**
     * Find tip by date
     */
    Optional<DailyLearningTip> findByTipDate(LocalDate tipDate);

    /**
     * Find all liked tips
     */
    List<DailyLearningTip> findByLikedTrue();

    /**
     * Find tips by category
     */
    List<DailyLearningTip> findByCategoryOrderByTipDateDesc(String category);

    /**
     * Find recent tips
     */
    @Query("SELECT t FROM DailyLearningTip t ORDER BY t.tipDate DESC")
    List<DailyLearningTip> findRecentTips();

    /**
     * Check if tip exists for date
     */
    boolean existsByTipDate(LocalDate tipDate);

    /**
     * Get latest tip
     */
    @Query("SELECT t FROM DailyLearningTip t ORDER BY t.tipDate DESC LIMIT 1")
    Optional<DailyLearningTip> findLatestTip();

    /**
     * Find tips in date range
     */
    List<DailyLearningTip> findByTipDateBetweenOrderByTipDateDesc(LocalDate startDate, LocalDate endDate);
}
