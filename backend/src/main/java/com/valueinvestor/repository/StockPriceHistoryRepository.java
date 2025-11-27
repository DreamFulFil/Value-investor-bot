package com.valueinvestor.repository;

import com.valueinvestor.model.entity.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {

    /**
     * Find historical prices for a symbol within a date range
     */
    List<StockPriceHistory> findBySymbolAndDateBetweenOrderByDateAsc(
            String symbol, LocalDate startDate, LocalDate endDate);

    /**
     * Find all prices for a symbol ordered by date
     */
    List<StockPriceHistory> findBySymbolOrderByDateDesc(String symbol);

    /**
     * Find the latest price for a symbol
     */
    @Query("SELECT s FROM StockPriceHistory s WHERE s.symbol = :symbol " +
           "ORDER BY s.date DESC LIMIT 1")
    Optional<StockPriceHistory> findLatestPriceForSymbol(@Param("symbol") String symbol);

    /**
     * Find the earliest price for a symbol
     */
    @Query("SELECT s FROM StockPriceHistory s WHERE s.symbol = :symbol " +
           "ORDER BY s.date ASC LIMIT 1")
    Optional<StockPriceHistory> findEarliestPriceForSymbol(@Param("symbol") String symbol);

    /**
     * Check if data exists for a symbol
     */
    boolean existsBySymbol(String symbol);

    /**
     * Check if data exists for a symbol on a specific date
     */
    boolean existsBySymbolAndDate(String symbol, LocalDate date);

    /**
     * Count records for a symbol
     */
    long countBySymbol(String symbol);

    /**
     * Get all distinct symbols with historical data
     */
    @Query("SELECT DISTINCT s.symbol FROM StockPriceHistory s ORDER BY s.symbol")
    List<String> findDistinctSymbols();

    /**
     * Find prices for a specific date across all symbols
     */
    List<StockPriceHistory> findByDate(LocalDate date);

    /**
     * Delete all prices for a symbol
     */
    void deleteBySymbol(String symbol);

    /**
     * Find price for a symbol on a specific date
     */
    Optional<StockPriceHistory> findBySymbolAndDate(String symbol, LocalDate date);

    /**
     * Find prices older than a specific date for a symbol
     */
    List<StockPriceHistory> findBySymbolAndDateBefore(String symbol, LocalDate date);

    /**
     * Find nearest price before a specific date (for fallback when exact date not found)
     */
    @Query("SELECT s FROM StockPriceHistory s WHERE s.symbol = :symbol " +
           "AND s.date <= :targetDate AND s.date >= :minDate " +
           "ORDER BY s.date DESC LIMIT 1")
    Optional<StockPriceHistory> findNearestPriceBeforeDate(
            @Param("symbol") String symbol, 
            @Param("targetDate") LocalDate targetDate,
            @Param("minDate") LocalDate minDate);
}
