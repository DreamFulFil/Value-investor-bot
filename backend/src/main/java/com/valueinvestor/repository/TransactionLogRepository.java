package com.valueinvestor.repository;

import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.model.entity.TransactionLog.TransactionType;
import com.valueinvestor.model.entity.TransactionLog.TradingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    List<TransactionLog> findBySymbolOrderByTimestampDesc(String symbol);

    List<TransactionLog> findByTypeOrderByTimestampDesc(TransactionType type);

    List<TransactionLog> findByModeOrderByTimestampDesc(TradingMode mode);

    List<TransactionLog> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT t FROM TransactionLog t WHERE t.type = 'REBALANCE' ORDER BY t.timestamp DESC")
    List<TransactionLog> findAllRebalanceTransactions();

    @Query("SELECT t FROM TransactionLog t WHERE t.type = 'REBALANCE' ORDER BY t.timestamp DESC LIMIT 1")
    TransactionLog findLastRebalanceTransaction();

    @Query("SELECT t FROM TransactionLog t WHERE t.symbol = ?1 AND t.timestamp >= ?2 ORDER BY t.timestamp DESC")
    List<TransactionLog> findRecentTransactionsBySymbol(String symbol, LocalDateTime since);

    @Query("SELECT DISTINCT t.symbol FROM TransactionLog t WHERE t.type IN ('BUY', 'SELL') ORDER BY t.symbol")
    List<String> findAllTradedSymbols();

    default List<TransactionLog> findRecentTransactions(LocalDateTime startDate, LocalDateTime endDate) {
        return findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
    }
}
