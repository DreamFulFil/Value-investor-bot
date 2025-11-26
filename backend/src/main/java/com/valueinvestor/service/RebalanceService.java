package com.valueinvestor.service;

import com.valueinvestor.config.AppConfig;
import com.valueinvestor.model.entity.AnalysisResults;
import com.valueinvestor.model.entity.PortfolioSnapshot;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.PortfolioSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RebalanceService {

    private static final Logger logger = LoggerFactory.getLogger(RebalanceService.class);
    private static final int TARGET_POSITIONS = 5;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private PortfolioSnapshotRepository snapshotRepository;

    /**
     * Perform monthly rebalance with catch-up logic
     */
    @Transactional
    public RebalanceResult performMonthlyRebalance() {
        logger.info("=== Starting Monthly Rebalance Process ===");

        RebalanceResult result = new RebalanceResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // Determine how many months we need to catch up
            LocalDate lastRebalanceDate = getLastRebalanceDate();
            LocalDate today = LocalDate.now();

            // BULLETPROOF DUPLICATE PREVENTION: Check if already rebalanced this month
            if (lastRebalanceDate != null && isSameMonth(lastRebalanceDate, today)) {
                logger.info("Rebalance already executed this month (last: {}). Skipping.", lastRebalanceDate);
                result.setSuccess(true);
                result.setMissedMonths(0);
                result.setEndTime(LocalDateTime.now());
                return result;
            }

            List<LocalDate> missedMonths = calculateMissedMonths(lastRebalanceDate, today);

            logger.info("Last rebalance: {}, Today: {}, Missed months: {}",
                       lastRebalanceDate, today, missedMonths.size());

            result.setMissedMonths(missedMonths.size());

            // Perform rebalance for each missed month
            for (LocalDate rebalanceDate : missedMonths) {
                logger.info("--- Executing rebalance for {} ---", rebalanceDate);

                MonthlyRebalanceResult monthResult = executeMonthlyRebalance(rebalanceDate);
                result.addMonthlyResult(monthResult);

                logger.info("Completed rebalance for {}: {} stocks purchased", rebalanceDate, monthResult.getStocksPurchased());
            }

            result.setSuccess(true);
            result.setEndTime(LocalDateTime.now());

            logger.info("=== Monthly Rebalance Process Completed Successfully ===");
            logger.info("Total months processed: {}", result.getMissedMonths());

        } catch (Exception e) {
            logger.error("Monthly rebalance failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setEndTime(LocalDateTime.now());
        }

        return result;
    }

    /**
     * Check if two dates are in the same month
     */
    private boolean isSameMonth(LocalDate date1, LocalDate date2) {
        return YearMonth.from(date1).equals(YearMonth.from(date2));
    }

    /**
     * Execute rebalance for a specific month
     */
    private MonthlyRebalanceResult executeMonthlyRebalance(LocalDate rebalanceDate) {
        MonthlyRebalanceResult result = new MonthlyRebalanceResult();
        result.setRebalanceDate(rebalanceDate);

        BigDecimal monthlyInvestment = appConfig.getMonthlyInvestment();
        TransactionLog.TradingMode mode = appConfig.getTradingMode();

        logger.info("Monthly investment: NT${}, Mode: {}", monthlyInvestment, mode);

        // Step 1: Select top 5 stocks
        List<String> selectedStocks = selectTopStocks();

        if (selectedStocks.size() < TARGET_POSITIONS) {
            logger.warn("Only found {} stocks, target is {}", selectedStocks.size(), TARGET_POSITIONS);
        }

        result.setSelectedStocks(selectedStocks);

        // Step 2: Calculate allocation
        Map<String, BigDecimal> allocation = portfolioService.calculateTargetAllocation(
                monthlyInvestment, selectedStocks);

        // Step 3: Execute buys using HISTORICAL prices for catch-up
        List<TransactionLog> transactions = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : allocation.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal amount = entry.getValue();

            try {
                // Use historical price for catch-up rebalancing (not current price)
                BigDecimal historicalPrice = marketDataService.getHistoricalClosePrice(symbol, rebalanceDate);
                
                if (historicalPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    logger.warn("Invalid historical price for {} on {}, skipping", symbol, rebalanceDate);
                    result.addError(symbol, "Invalid historical price");
                    continue;
                }

                // Calculate shares based on historical price
                BigDecimal shares = amount.divide(historicalPrice, 8, RoundingMode.DOWN);

                if (shares.compareTo(BigDecimal.ZERO) > 0) {
                    // Execute buy with historical price
                    TransactionLog transaction = tradingService.executeBuy(symbol, shares, mode, historicalPrice);
                    transactions.add(transaction);

                    logger.info("Purchased {} shares of {} at NT${} (historical price on {})", 
                        shares, symbol, historicalPrice, rebalanceDate);
                } else {
                    logger.warn("Cannot buy {} - price too high or amount too low", symbol);
                }

            } catch (Exception e) {
                logger.error("Failed to buy {}", symbol, e);
                result.addError(symbol, e.getMessage());
            }
        }

        result.setTransactions(transactions);
        result.setStocksPurchased(transactions.size());
        result.setTotalInvested(transactions.stream()
                .map(TransactionLog::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Step 4: Save snapshot
        PortfolioSnapshot snapshot = portfolioService.saveSnapshot("MONTHLY_REBALANCE");
        result.setSnapshotId(snapshot.getId());

        return result;
    }

    /**
     * Select top 5 stocks based on analysis
     */
    private List<String> selectTopStocks() {
        logger.info("Selecting top {} stocks", TARGET_POSITIONS);

        // Get top dividend stocks from database
        List<String> candidateSymbols = getCandidateStocks();

        logger.info("Found {} candidate stocks", candidateSymbols.size());

        // Analyze each stock
        List<AnalysisResults> analyses = new ArrayList<>();

        for (String symbol : candidateSymbols) {
            try {
                // Check if we have recent analysis (within 7 days)
                Optional<AnalysisResults> recentAnalysis = analysisService.getLatestAnalysis(symbol);

                if (recentAnalysis.isPresent() &&
                    recentAnalysis.get().getTimestamp().isAfter(LocalDateTime.now().minusDays(7))) {
                    analyses.add(recentAnalysis.get());
                    logger.info("Using cached analysis for {}", symbol);
                } else {
                    // Perform new analysis
                    logger.info("Analyzing {}", symbol);
                    AnalysisResults analysis = analysisService.analyzeStock(symbol);
                    analyses.add(analysis);
                }

            } catch (Exception e) {
                logger.error("Failed to analyze {}", symbol, e);
            }
        }

        // Filter for BUY recommendations and sort by score
        List<String> topStocks = analyses.stream()
                .filter(a -> "BUY".equals(a.getRecommendation()))
                .sorted(Comparator.comparing(AnalysisResults::getScore).reversed())
                .limit(TARGET_POSITIONS)
                .map(AnalysisResults::getSymbol)
                .collect(Collectors.toList());

        logger.info("Selected top {} stocks: {}", topStocks.size(), topStocks);

        return topStocks;
    }

    /**
     * Get candidate stocks for analysis
     */
    private List<String> getCandidateStocks() {
        // Use configured watchlist if available
        List<String> watchlist = appConfig.getWatchlist();

        if (watchlist != null && !watchlist.isEmpty()) {
            logger.info("Using configured watchlist: {}", watchlist);
            return watchlist;
        }

        // Otherwise, get top dividend stocks
        BigDecimal minDividendYield = new BigDecimal("2.0"); // 2% minimum
        return marketDataService.getStocksByMinDividendYield(minDividendYield)
                .stream()
                .limit(20) // Analyze top 20
                .map(f -> f.getSymbol())
                .collect(Collectors.toList());
    }

    /**
     * Get last rebalance date
     */
    private LocalDate getLastRebalanceDate() {
        Optional<PortfolioSnapshot> lastSnapshot = snapshotRepository.findLastMonthlyRebalanceSnapshot();

        if (lastSnapshot.isPresent()) {
            return lastSnapshot.get().getTimestamp().toLocalDate();
        }

        // If no previous rebalance, use account creation date or 1 month ago
        return LocalDate.now().minusMonths(1);
    }

    /**
     * Calculate missed months between last rebalance and today
     */
    private List<LocalDate> calculateMissedMonths(LocalDate lastRebalance, LocalDate today) {
        List<LocalDate> missedMonths = new ArrayList<>();

        // Calculate the next rebalance date after last rebalance
        LocalDate nextRebalance = lastRebalance.plusMonths(1).withDayOfMonth(1);

        // Add all months from next rebalance until current month
        while (!nextRebalance.isAfter(today.withDayOfMonth(1))) {
            missedMonths.add(nextRebalance);
            nextRebalance = nextRebalance.plusMonths(1);
        }

        return missedMonths;
    }

    /**
     * Manual rebalance trigger
     */
    @Transactional
    public RebalanceResult triggerRebalance() {
        logger.info("Manual rebalance triggered");
        return performMonthlyRebalance();
    }

    /**
     * Rebalance result class
     */
    public static class RebalanceResult {
        private boolean success;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int missedMonths;
        private List<MonthlyRebalanceResult> monthlyResults = new ArrayList<>();
        private String errorMessage;

        public void addMonthlyResult(MonthlyRebalanceResult result) {
            this.monthlyResults.add(result);
        }

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public int getMissedMonths() {
            return missedMonths;
        }

        public void setMissedMonths(int missedMonths) {
            this.missedMonths = missedMonths;
        }

        public List<MonthlyRebalanceResult> getMonthlyResults() {
            return monthlyResults;
        }

        public void setMonthlyResults(List<MonthlyRebalanceResult> monthlyResults) {
            this.monthlyResults = monthlyResults;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Monthly rebalance result class
     */
    public static class MonthlyRebalanceResult {
        private LocalDate rebalanceDate;
        private List<String> selectedStocks;
        private List<TransactionLog> transactions;
        private int stocksPurchased;
        private BigDecimal totalInvested;
        private Long snapshotId;
        private Map<String, String> errors = new HashMap<>();

        public void addError(String symbol, String error) {
            this.errors.put(symbol, error);
        }

        // Getters and setters
        public LocalDate getRebalanceDate() {
            return rebalanceDate;
        }

        public void setRebalanceDate(LocalDate rebalanceDate) {
            this.rebalanceDate = rebalanceDate;
        }

        public List<String> getSelectedStocks() {
            return selectedStocks;
        }

        public void setSelectedStocks(List<String> selectedStocks) {
            this.selectedStocks = selectedStocks;
        }

        public List<TransactionLog> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<TransactionLog> transactions) {
            this.transactions = transactions;
        }

        public int getStocksPurchased() {
            return stocksPurchased;
        }

        public void setStocksPurchased(int stocksPurchased) {
            this.stocksPurchased = stocksPurchased;
        }

        public BigDecimal getTotalInvested() {
            return totalInvested;
        }

        public void setTotalInvested(BigDecimal totalInvested) {
            this.totalInvested = totalInvested;
        }

        public Long getSnapshotId() {
            return snapshotId;
        }

        public void setSnapshotId(Long snapshotId) {
            this.snapshotId = snapshotId;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public void setErrors(Map<String, String> errors) {
            this.errors = errors;
        }
    }
}
