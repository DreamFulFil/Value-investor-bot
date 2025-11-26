package com.valueinvestor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valueinvestor.model.entity.PortfolioSnapshot;
import com.valueinvestor.model.entity.PositionHistory;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.PortfolioSnapshotRepository;
import com.valueinvestor.repository.PositionHistoryRepository;
import com.valueinvestor.repository.TransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PortfolioService {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioService.class);
    private static final int TARGET_POSITIONS = 5;

    @Autowired
    private PositionHistoryRepository positionHistoryRepository;

    @Autowired
    private PortfolioSnapshotRepository snapshotRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private MarketDataService marketDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get current portfolio positions
     */
    public List<PositionHistory> getCurrentPortfolio() {
        List<PositionHistory> positions = positionHistoryRepository.findLatestPositions();

        // Update current prices and metrics
        for (PositionHistory position : positions) {
            if (position.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentPrice = marketDataService.getQuote(position.getSymbol());
                position.setCurrentPrice(currentPrice);
                position.calculateMetrics();
            }
        }

        return positions.stream()
                .filter(p -> p.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    /**
     * Get total portfolio value
     */
    public BigDecimal getTotalValue() {
        List<PositionHistory> positions = getCurrentPortfolio();
        BigDecimal totalValue = BigDecimal.ZERO;

        for (PositionHistory position : positions) {
            if (position.getMarketValue() != null) {
                totalValue = totalValue.add(position.getMarketValue());
            }
        }

        BigDecimal cash = getCashBalance();
        totalValue = totalValue.add(cash);

        logger.info("Total portfolio value: ${}", totalValue);
        return totalValue;
    }

    /**
     * Get cash balance
     */
    public BigDecimal getCashBalance() {
        // Calculate cash from deposits minus investments
        List<TransactionLog> deposits = transactionLogRepository.findByTypeOrderByTimestampDesc(
                TransactionLog.TransactionType.DEPOSIT);

        List<TransactionLog> buys = transactionLogRepository.findByTypeOrderByTimestampDesc(
                TransactionLog.TransactionType.BUY);

        List<TransactionLog> sells = transactionLogRepository.findByTypeOrderByTimestampDesc(
                TransactionLog.TransactionType.SELL);

        BigDecimal totalDeposits = deposits.stream()
                .map(TransactionLog::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBuys = buys.stream()
                .map(TransactionLog::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSells = sells.stream()
                .map(TransactionLog::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cash = totalDeposits.subtract(totalBuys).add(totalSells);
        logger.info("Cash balance: ${}", cash);
        return cash;
    }

    /**
     * Calculate target allocation for monthly investment
     */
    public Map<String, BigDecimal> calculateTargetAllocation(BigDecimal monthlyInvestment, List<String> selectedSymbols) {
        Map<String, BigDecimal> allocation = new HashMap<>();

        if (selectedSymbols == null || selectedSymbols.isEmpty()) {
            logger.warn("No symbols provided for allocation");
            return allocation;
        }

        // Equal weight allocation
        BigDecimal perStockAmount = monthlyInvestment.divide(
                BigDecimal.valueOf(selectedSymbols.size()),
                2,
                RoundingMode.HALF_UP
        );

        for (String symbol : selectedSymbols) {
            allocation.put(symbol, perStockAmount);
        }

        logger.info("Calculated target allocation for {} stocks: ${} each",
                   selectedSymbols.size(), perStockAmount);
        return allocation;
    }

    /**
     * Save portfolio snapshot
     */
    @Transactional
    public PortfolioSnapshot saveSnapshot(String snapshotType) {
        try {
            List<PositionHistory> positions = getCurrentPortfolio();
            BigDecimal totalValue = getTotalValue();
            BigDecimal cash = getCashBalance();

            // Calculate total invested amount
            BigDecimal investedAmount = positions.stream()
                    .map(p -> p.getQuantity().multiply(p.getAveragePrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate total P/L
            BigDecimal totalPL = positions.stream()
                    .map(PositionHistory::getUnrealizedPL)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Convert positions to JSON
            String positionsJson = convertPositionsToJson(positions);

            PortfolioSnapshot snapshot = new PortfolioSnapshot(
                    totalValue,
                    cash,
                    investedAmount,
                    totalPL,
                    positionsJson,
                    snapshotType
            );

            snapshot = snapshotRepository.save(snapshot);
            logger.info("Saved portfolio snapshot: {} - Total Value: ${}", snapshotType, totalValue);

            return snapshot;

        } catch (Exception e) {
            logger.error("Failed to save portfolio snapshot", e);
            throw new RuntimeException("Failed to save portfolio snapshot", e);
        }
    }

    /**
     * Get portfolio history
     */
    public List<PortfolioSnapshot> getPortfolioHistory(LocalDateTime startDate, LocalDateTime endDate) {
        return snapshotRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
    }

    /**
     * Get latest snapshot
     */
    public Optional<PortfolioSnapshot> getLatestSnapshot() {
        return snapshotRepository.findLatestSnapshot();
    }

    /**
     * Record a cash deposit
     */
    @Transactional
    public TransactionLog recordDeposit(BigDecimal amount, String notes) {
        TransactionLog deposit = new TransactionLog(
                TransactionLog.TransactionType.DEPOSIT,
                null,
                null,
                null,
                amount,
                TransactionLog.TradingMode.SIMULATION,
                notes
        );

        deposit = transactionLogRepository.save(deposit);
        logger.info("Recorded cash deposit: ${}", amount);

        // Save snapshot after deposit
        saveSnapshot("DEPOSIT");

        return deposit;
    }

    /**
     * Convert positions to JSON string
     */
    private String convertPositionsToJson(List<PositionHistory> positions) {
        try {
            List<Map<String, Object>> positionData = new ArrayList<>();

            for (PositionHistory position : positions) {
                Map<String, Object> data = new HashMap<>();
                data.put("symbol", position.getSymbol());
                data.put("quantity", position.getQuantity());
                data.put("averagePrice", position.getAveragePrice());
                data.put("currentPrice", position.getCurrentPrice());
                data.put("marketValue", position.getMarketValue());
                data.put("unrealizedPL", position.getUnrealizedPL());
                positionData.add(data);
            }

            return objectMapper.writeValueAsString(positionData);

        } catch (Exception e) {
            logger.error("Failed to convert positions to JSON", e);
            return "[]";
        }
    }

    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        BigDecimal totalValue = getTotalValue();
        BigDecimal cash = getCashBalance();
        List<PositionHistory> positions = getCurrentPortfolio();

        BigDecimal investedAmount = positions.stream()
                .map(p -> p.getQuantity().multiply(p.getAveragePrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPL = positions.stream()
                .map(PositionHistory::getUnrealizedPL)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal plPercentage = BigDecimal.ZERO;
        if (investedAmount.compareTo(BigDecimal.ZERO) > 0) {
            plPercentage = totalPL.divide(investedAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        metrics.put("totalValue", totalValue);
        metrics.put("cashBalance", cash);
        metrics.put("investedAmount", investedAmount);
        metrics.put("totalPL", totalPL);
        metrics.put("plPercentage", plPercentage);
        metrics.put("positionCount", positions.size());

        return metrics;
    }
}
