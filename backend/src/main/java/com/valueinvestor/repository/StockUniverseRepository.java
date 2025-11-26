package com.valueinvestor.repository;

import com.valueinvestor.model.entity.StockUniverse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockUniverseRepository extends JpaRepository<StockUniverse, Long> {

    /**
     * Find stock by symbol
     */
    Optional<StockUniverse> findBySymbol(String symbol);

    /**
     * Find all active stocks in a specific market
     */
    List<StockUniverse> findByMarketAndActiveTrue(String market);

    /**
     * Find all active stocks
     */
    List<StockUniverse> findByActiveTrue();

    /**
     * Find all stocks in a market (active and inactive)
     */
    List<StockUniverse> findByMarket(String market);

    /**
     * Check if a symbol exists in the universe
     */
    boolean existsBySymbol(String symbol);

    /**
     * Find active stocks by sector
     */
    List<StockUniverse> findBySectorAndActiveTrue(String sector);

    /**
     * Get all distinct sectors
     */
    @Query("SELECT DISTINCT s.sector FROM StockUniverse s WHERE s.active = true AND s.sector IS NOT NULL ORDER BY s.sector")
    List<String> findDistinctActiveSectors();

    /**
     * Count active stocks in the universe
     */
    long countByActiveTrue();

    /**
     * Count active stocks by market
     */
    long countByMarketAndActiveTrue(String market);

    /**
     * Find stocks by sector in a specific market
     */
    List<StockUniverse> findByMarketAndSectorAndActiveTrue(String market, String sector);
}
