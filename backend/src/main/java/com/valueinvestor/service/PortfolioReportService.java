package com.valueinvestor.service;

import com.valueinvestor.model.entity.PositionHistory;
import com.valueinvestor.model.entity.StockFundamentals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PortfolioReportService {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioReportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final BigDecimal WEEKLY_GOAL = new BigDecimal("1600.00");

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private MarketDataService marketDataService;

    /**
     * Generate portfolio report
     * Scheduled to run at 7 AM every day
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void generatePortfolioReport() {
        logger.info("Generating daily portfolio report");

        try {
            List<PositionHistory> positions = portfolioService.getCurrentPortfolio();
            BigDecimal portfolioValue = portfolioService.getTotalValue();
            BigDecimal cashBalance = portfolioService.getCashBalance();
            BigDecimal totalInvested = calculateTotalInvested(positions);
            BigDecimal totalGain = portfolioValue.subtract(cashBalance).subtract(totalInvested);

            String markdown = buildPortfolioMarkdown(
                    positions,
                    portfolioValue,
                    cashBalance,
                    totalInvested,
                    totalGain
            );

            // Store in memory (no file writing)
            this.lastGeneratedReport = markdown;
            logger.info("Successfully generated portfolio report");

        } catch (Exception e) {
            logger.error("Failed to generate portfolio report", e);
        }
    }

    // In-memory cache for the latest report
    private String lastGeneratedReport = null;

    /**
     * Get current portfolio report (generated on-demand)
     */
    public String getCurrentReport() {
        if (lastGeneratedReport == null) {
            generatePortfolioReport();
        }
        if (lastGeneratedReport != null) {
            return lastGeneratedReport;
        }
        return "# Portfolio Report\n\nNo report available yet.";
    }

    /**
     * Manually trigger report generation
     */
    public void generateReportNow() {
        generatePortfolioReport();
    }

    /**
     * Build portfolio markdown report
     */
    private String buildPortfolioMarkdown(List<PositionHistory> positions,
                                         BigDecimal portfolioValue,
                                         BigDecimal cashBalance,
                                         BigDecimal totalInvested,
                                         BigDecimal totalGain) {
        StringBuilder markdown = new StringBuilder();

        // Header
        markdown.append("# Portfolio Report - ")
                .append(LocalDate.now().format(DATE_FORMATTER))
                .append("\n\n");

        // Summary Section
        markdown.append("## Summary\n\n");

        BigDecimal gainPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            gainPercent = totalGain.divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        markdown.append("| Metric | Value |\n");
        markdown.append("|--------|-------|\n");
        markdown.append("| **Total Value** | $").append(formatMoney(portfolioValue)).append(" |\n");
        markdown.append("| **Total Invested** | $").append(formatMoney(totalInvested)).append(" |\n");
        markdown.append("| **Total Gain/Loss** | $").append(formatMoney(totalGain))
                .append(" (").append(formatPercent(gainPercent)).append("%) |\n");
        markdown.append("| **Cash Balance** | $").append(formatMoney(cashBalance)).append(" |\n");
        markdown.append("| **Positions** | ").append(positions.size()).append(" |\n\n");

        // Current Positions Table
        markdown.append("## Current Positions (").append(positions.size()).append(")\n\n");
        markdown.append("| Symbol | Shares | Avg Price | Current Price | Value | Gain/Loss | % Change | Allocation |\n");
        markdown.append("|--------|--------|-----------|---------------|-------|-----------|----------|------------|\n");

        BigDecimal totalPositionValue = positions.stream()
                .map(PositionHistory::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (PositionHistory position : positions) {
            BigDecimal allocation = BigDecimal.ZERO;
            if (totalPositionValue.compareTo(BigDecimal.ZERO) > 0) {
                allocation = position.getMarketValue()
                        .divide(totalPositionValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            String gainLoss = position.getUnrealizedPL() != null ?
                    (position.getUnrealizedPL().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") +
                            formatMoney(position.getUnrealizedPL()) : "$0.00";

            String percentChange = position.getUnrealizedPLPercent() != null ?
                    (position.getUnrealizedPLPercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") +
                            formatPercent(position.getUnrealizedPLPercent()) + "%" : "0%";

            markdown.append("| ").append(position.getSymbol())
                    .append(" | ").append(formatShares(position.getQuantity()))
                    .append(" | $").append(formatMoney(position.getAveragePrice()))
                    .append(" | $").append(formatMoney(position.getCurrentPrice()))
                    .append(" | $").append(formatMoney(position.getMarketValue()))
                    .append(" | ").append(gainLoss)
                    .append(" | ").append(percentChange)
                    .append(" | ").append(formatPercent(allocation)).append("% |\n");
        }

        markdown.append("\n");

        // Allocation Visualization (ASCII)
        markdown.append("## Portfolio Allocation\n\n");
        markdown.append("```\n");
        for (PositionHistory position : positions) {
            BigDecimal allocation = BigDecimal.ZERO;
            if (totalPositionValue.compareTo(BigDecimal.ZERO) > 0) {
                allocation = position.getMarketValue()
                        .divide(totalPositionValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            int barLength = allocation.multiply(BigDecimal.valueOf(0.5)).intValue();
            markdown.append(String.format("%-6s ", position.getSymbol()));
            markdown.append("|");
            markdown.append("█".repeat(Math.max(0, barLength)));
            markdown.append(" ").append(formatPercent(allocation)).append("%\n");
        }
        markdown.append("```\n\n");

        // Dividend Income Projection
        markdown.append("## Dividend Income Projection\n\n");

        BigDecimal annualDividend = calculateAnnualDividend(positions);
        BigDecimal monthlyDividend = annualDividend.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal weeklyDividend = annualDividend.divide(BigDecimal.valueOf(52), 2, RoundingMode.HALF_UP);

        BigDecimal weeklyGoalPercent = BigDecimal.ZERO;
        if (WEEKLY_GOAL.compareTo(BigDecimal.ZERO) > 0) {
            weeklyGoalPercent = weeklyDividend.divide(WEEKLY_GOAL, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        markdown.append("| Period | Amount |\n");
        markdown.append("|--------|--------|\n");
        markdown.append("| **Annual** | $").append(formatMoney(annualDividend)).append(" |\n");
        markdown.append("| **Monthly** | $").append(formatMoney(monthlyDividend)).append(" |\n");
        markdown.append("| **Weekly** | $").append(formatMoney(weeklyDividend))
                .append(" (").append(formatPercent(weeklyGoalPercent)).append("% of $50 goal) |\n\n");

        // Next Rebalance
        markdown.append("## Next Rebalance\n\n");
        LocalDate nextRebalance = getNextRebalanceDate();
        markdown.append("| Item | Value |\n");
        markdown.append("|------|-------|\n");
        markdown.append("| **Date** | ").append(nextRebalance.format(DATE_FORMATTER)).append(" |\n");
        markdown.append("| **Expected Investment** | $500.00 |\n\n");

        // Performance Metrics
        markdown.append("## Performance Metrics\n\n");

        // Calculate best and worst performers
        PositionHistory bestPerformer = positions.stream()
                .max((p1, p2) -> {
                    BigDecimal pl1 = p1.getUnrealizedPLPercent() != null ? p1.getUnrealizedPLPercent() : BigDecimal.ZERO;
                    BigDecimal pl2 = p2.getUnrealizedPLPercent() != null ? p2.getUnrealizedPLPercent() : BigDecimal.ZERO;
                    return pl1.compareTo(pl2);
                })
                .orElse(null);

        PositionHistory worstPerformer = positions.stream()
                .min((p1, p2) -> {
                    BigDecimal pl1 = p1.getUnrealizedPLPercent() != null ? p1.getUnrealizedPLPercent() : BigDecimal.ZERO;
                    BigDecimal pl2 = p2.getUnrealizedPLPercent() != null ? p2.getUnrealizedPLPercent() : BigDecimal.ZERO;
                    return pl1.compareTo(pl2);
                })
                .orElse(null);

        if (bestPerformer != null) {
            markdown.append("**Best Performer:** ").append(bestPerformer.getSymbol())
                    .append(" (").append(bestPerformer.getUnrealizedPLPercent() != null ?
                            "+" + formatPercent(bestPerformer.getUnrealizedPLPercent()) + "%" : "0%")
                    .append(")\n\n");
        }

        if (worstPerformer != null) {
            markdown.append("**Worst Performer:** ").append(worstPerformer.getSymbol())
                    .append(" (").append(worstPerformer.getUnrealizedPLPercent() != null ?
                            formatPercent(worstPerformer.getUnrealizedPLPercent()) + "%" : "0%")
                    .append(")\n\n");
        }

        // Footer
        markdown.append("---\n");
        markdown.append("*Auto-generated daily report by Value Investor Bot*\n");

        return markdown.toString();
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
     * Calculate annual dividend income
     */
    private BigDecimal calculateAnnualDividend(List<PositionHistory> positions) {
        BigDecimal total = BigDecimal.ZERO;

        for (PositionHistory position : positions) {
            try {
                StockFundamentals fundamentals = marketDataService.getFundamentals(position.getSymbol());
                if (fundamentals != null && fundamentals.getDividendYield() != null) {
                    BigDecimal dividendYield = fundamentals.getDividendYield().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal positionDividend = position.getMarketValue().multiply(dividendYield);
                    total = total.add(positionDividend);
                }
            } catch (Exception e) {
                logger.warn("Failed to get dividend data for {}", position.getSymbol());
            }
        }

        return total;
    }

    /**
     * Get next rebalance date (1st of next month)
     */
    private LocalDate getNextRebalanceDate() {
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() == 1) {
            return today.plusMonths(1).withDayOfMonth(1);
        } else {
            return today.plusMonths(1).withDayOfMonth(1);
        }
    }

    /**
     * Format money values
     */
    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    /**
     * Format percentage values
     */
    private String formatPercent(BigDecimal percent) {
        if (percent == null) {
            return "0.00";
        }
        return percent.setScale(2, RoundingMode.HALF_UP).toString();
    }

    /**
     * Format share quantities
     */
    private String formatShares(BigDecimal shares) {
        if (shares == null) {
            return "0";
        }
        return shares.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
