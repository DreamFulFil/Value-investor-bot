package com.valueinvestor.repository;

import com.valueinvestor.model.entity.StockFundamentals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockFundamentalsRepository extends JpaRepository<StockFundamentals, Long> {

    Optional<StockFundamentals> findBySymbol(String symbol);

    List<StockFundamentals> findByMarket(String market);

    List<StockFundamentals> findBySector(String sector);

    @Query("SELECT s FROM StockFundamentals s WHERE s.market = 'US' " +
           "AND s.dividendYield IS NOT NULL " +
           "ORDER BY s.dividendYield DESC")
    List<StockFundamentals> findTopDividendStocks();

    @Query("SELECT s FROM StockFundamentals s WHERE s.market = 'US' " +
           "AND s.dividendYield >= ?1 " +
           "ORDER BY s.dividendYield DESC")
    List<StockFundamentals> findStocksByMinDividendYield(BigDecimal minYield);

    @Query("SELECT s FROM StockFundamentals s WHERE s.symbol = ?1 " +
           "AND s.lastUpdated >= ?2")
    Optional<StockFundamentals> findBySymbolIfRecent(String symbol, LocalDateTime cutoffTime);

    @Query("SELECT s FROM StockFundamentals s WHERE s.market = 'US' " +
           "AND s.lastUpdated < ?1")
    List<StockFundamentals> findStaleData(LocalDateTime cutoffTime);

    @Query("SELECT s FROM StockFundamentals s WHERE s.market = 'US' " +
           "AND LOWER(s.name) LIKE LOWER(CONCAT('%', ?1, '%')) " +
           "OR LOWER(s.symbol) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<StockFundamentals> searchByNameOrSymbol(String query);
}
