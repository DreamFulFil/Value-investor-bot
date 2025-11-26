package com.valueinvestor.service;

import com.valueinvestor.model.entity.InsightsHistory;
import com.valueinvestor.model.entity.PortfolioSnapshot;
import com.valueinvestor.model.entity.PositionHistory;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.InsightsHistoryRepository;
import com.valueinvestor.repository.TransactionLogRepository;
import com.valueinvestor.util.OllamaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class InsightsService {

    private static final Logger logger = LoggerFactory.getLogger(InsightsService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_FULL = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    @Autowired
    private InsightsHistoryRepository insightsHistoryRepository;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private OllamaClient ollamaClient;

    /**
     * Generate monthly insights after rebalance
     * This should be called after each monthly rebalance
     */
    @Transactional
    public InsightsHistory generateMonthlyInsights() {
        LocalDate today = LocalDate.now();
        logger.info("Generating monthly insights for {}", today);

        try {
            // Gather portfolio data
            List<PositionHistory> positions = portfolioService.getCurrentPortfolio();
            BigDecimal portfolioValue = portfolioService.getTotalValue();
            BigDecimal cashBalance = portfolioService.getCashBalance();

            // Get recent transactions for context
            List<TransactionLog> recentTransactions = transactionLogRepository
                    .findRecentTransactions(LocalDateTime.now().minusMonths(1), LocalDateTime.now());

            // Calculate monthly return
            BigDecimal monthlyReturn = calculateMonthlyReturn();

            // Build portfolio summary
            String portfolioData = buildPortfolioSummary(positions, portfolioValue, cashBalance, monthlyReturn);

            // Build market context
            String marketContext = buildMarketContext(recentTransactions);

            // Generate insights using Ollama
            logger.info("Calling Ollama to generate insights");
            String insightsContent = ollamaClient.generateInsights(portfolioData, marketContext);

            // Format as markdown
            String markdownContent = formatInsightsAsMarkdown(
                    insightsContent,
                    positions,
                    portfolioValue,
                    monthlyReturn,
                    cashBalance
            );

            // Save to database (no file writing)
            InsightsHistory insights = new InsightsHistory(
                    today,
                    markdownContent,
                    portfolioValue,
                    monthlyReturn
            );
            insights.setCashBalance(cashBalance);
            insights.setTotalInvested(calculateTotalInvested(positions));
            insights.setPositionsCount(positions.size());

            insights = insightsHistoryRepository.save(insights);
            logger.info("Successfully generated and saved monthly insights");

            return insights;

        } catch (Exception e) {
            logger.error("Failed to generate monthly insights", e);
            throw new RuntimeException("Failed to generate monthly insights", e);
        }
    }

    /**
     * Get current insights (from database)
     */
    public String getCurrentInsights() {
        Optional<InsightsHistory> latest = insightsHistoryRepository.findLatest();
        if (latest.isPresent()) {
            return latest.get().getInsightsContent();
        } else {
            return "# No insights available yet\n\nInsights will be generated after the first monthly rebalance.";
        }
    }

    /**
     * Get insights history
     */
    public List<InsightsHistory> getInsightsHistory(int limit) {
        return insightsHistoryRepository.findRecentInsights(limit);
    }

    /**
     * Get latest insights from database
     */
    public Optional<InsightsHistory> getLatestInsights() {
        return insightsHistoryRepository.findLatest();
    }

    /**
     * Build portfolio summary for LLM
     */
    private String buildPortfolioSummary(List<PositionHistory> positions,
                                         BigDecimal portfolioValue,
                                         BigDecimal cashBalance,
                                         BigDecimal monthlyReturn) {
        StringBuilder summary = new StringBuilder();

        summary.append("Total Portfolio Value: $").append(portfolioValue).append("\n");
        summary.append("Cash Balance: $").append(cashBalance).append("\n");
        summary.append("Monthly Return: ").append(monthlyReturn).append("%\n");
        summary.append("Number of Positions: ").append(positions.size()).append("\n\n");

        summary.append("Current Holdings:\n");
        for (PositionHistory position : positions) {
            summary.append("- ").append(position.getSymbol()).append(": ");
            summary.append(position.getQuantity()).append(" shares @ $").append(position.getCurrentPrice());
            summary.append(" (Value: $").append(position.getMarketValue()).append(", ");
            summary.append("P/L: ");
            if (position.getUnrealizedPLPercent() != null) {
                summary.append(position.getUnrealizedPLPercent()).append("%");
            }
            summary.append(")\n");
        }

        return summary.toString();
    }

    /**
     * Build market context for LLM
     */
    private String buildMarketContext(List<TransactionLog> recentTransactions) {
        StringBuilder context = new StringBuilder();

        context.append("Recent Activity:\n");
        context.append("Transactions in last month: ").append(recentTransactions.size()).append("\n");

        long buyCount = recentTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionLog.TransactionType.BUY)
                .count();

        long sellCount = recentTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionLog.TransactionType.SELL)
                .count();

        context.append("Buy transactions: ").append(buyCount).append("\n");
        context.append("Sell transactions: ").append(sellCount).append("\n");

        return context.toString();
    }

    /**
     * Calculate monthly return
     */
    private BigDecimal calculateMonthlyReturn() {
        Optional<PortfolioSnapshot> currentSnapshot = portfolioService.getLatestSnapshot();
        if (currentSnapshot.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Get snapshot from 30 days ago
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        List<PortfolioSnapshot> history = portfolioService.getPortfolioHistory(
                monthAgo.minusDays(5),
                monthAgo.plusDays(5)
        );

        if (history.isEmpty()) {
            return BigDecimal.ZERO;
        }

        PortfolioSnapshot oldSnapshot = history.get(history.size() - 1);
        BigDecimal currentValue = currentSnapshot.get().getTotalValue();
        BigDecimal oldValue = oldSnapshot.getTotalValue();

        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentValue.subtract(oldValue)
                .divide(oldValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate total invested amount
     */
    private BigDecimal calculateTotalInvested(List<PositionHistory> positions) {
        return positions.stream()
                .map(p -> p.getQuantity().multiply(p.getAveragePrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Format insights as markdown
     */
    private String formatInsightsAsMarkdown(String llmInsights,
                                           List<PositionHistory> positions,
                                           BigDecimal portfolioValue,
                                           BigDecimal monthlyReturn,
                                           BigDecimal cashBalance) {
        StringBuilder markdown = new StringBuilder();

        // Header
        markdown.append("# Monthly Investment Insights - ")
                .append(LocalDate.now().format(DATE_FORMATTER))
                .append("\n\n");

        markdown.append("*Generated on ")
                .append(LocalDate.now().format(DATE_FORMATTER_FULL))
                .append("*\n\n");

        // Summary Box
        markdown.append("## Portfolio Summary\n\n");
        markdown.append("| Metric | Value |\n");
        markdown.append("|--------|-------|\n");
        markdown.append("| **Total Value** | $").append(portfolioValue).append(" |\n");
        markdown.append("| **Cash Balance** | $").append(cashBalance).append(" |\n");
        markdown.append("| **Monthly Return** | ").append(monthlyReturn).append("% |\n");
        markdown.append("| **Positions** | ").append(positions.size()).append(" |\n\n");

        // LLM Generated Insights
        markdown.append(llmInsights).append("\n\n");

        // Top Holdings Table
        markdown.append("## Current Holdings Detail\n\n");
        markdown.append("| Symbol | Shares | Avg Price | Current Price | Value | Gain/Loss | % Change |\n");
        markdown.append("|--------|--------|-----------|---------------|-------|-----------|----------|\n");

        for (PositionHistory position : positions) {
            markdown.append("| ").append(position.getSymbol())
                    .append(" | ").append(position.getQuantity())
                    .append(" | $").append(position.getAveragePrice())
                    .append(" | $").append(position.getCurrentPrice())
                    .append(" | $").append(position.getMarketValue())
                    .append(" | $").append(position.getUnrealizedPL() != null ? position.getUnrealizedPL() : "0")
                    .append(" | ").append(position.getUnrealizedPLPercent() != null ?
                            position.getUnrealizedPLPercent() + "%" : "0%")
                    .append(" |\n");
        }

        // Footer
        markdown.append("\n---\n");
        markdown.append("*Generated by Value Investor Bot using llama3.1:8b-instruct-q5_K_M*\n");

        return markdown.toString();
    }
}
