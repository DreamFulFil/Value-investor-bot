package com.valueinvestor.service;

import com.valueinvestor.model.entity.StockUniverse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataRefreshScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataRefreshScheduler.class);

    // US Eastern Time Zone for market hours
    private static final ZoneId ET_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    @Autowired
    private StockUniverseService stockUniverseService;

    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private DataCatchUpService dataCatchUpService;

    private LocalDateTime lastFundamentalsRefresh;
    private LocalDateTime lastHistoricalRefresh;
    private LocalDateTime lastQuoteRefresh;

    /**
     * Daily fundamentals refresh - runs at 2 AM ET
     * Cron: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "America/New_York")
    public void refreshFundamentalsDaily() {
        logger.info("========================================");
        logger.info("Starting Daily Fundamentals Refresh");
        logger.info("========================================");

        List<StockUniverse> activeStocks = stockUniverseService.getActiveStocks();
        logger.info("Refreshing fundamentals for {} active stocks", activeStocks.size());

        // Note: Actual fundamentals refresh logic should be implemented
        // in a separate service that fetches data from Yahoo Finance
        logger.info("NOTE: Fundamentals refresh logic to be implemented");
        logger.info("This would update PE ratio, dividend yield, market cap, etc.");

        lastFundamentalsRefresh = LocalDateTime.now();
        logger.info("Fundamentals refresh completed at {}", lastFundamentalsRefresh);
    }

    /**
     * Weekly historical prices refresh - runs every Sunday at 1 AM ET
     * Cron: second, minute, hour, day, month, weekday (0 = Sunday)
     */
    @Scheduled(cron = "0 0 1 * * 0", zone = "America/New_York")
    public void refreshHistoricalPricesWeekly() {
        logger.info("========================================");
        logger.info("Starting Weekly Historical Prices Refresh");
        logger.info("========================================");

        List<StockUniverse> activeStocks = stockUniverseService.getActiveStocks();
        List<String> symbols = activeStocks.stream()
                .map(StockUniverse::getSymbol)
                .collect(Collectors.toList());

        logger.info("Refreshing historical prices for {} stocks", symbols.size());

        int totalUpdated = 0;
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);

            try {
                logger.info("[{}/{}] Refreshing historical prices for {}",
                        i + 1, symbols.size(), symbol);

                int recordsUpdated = historicalDataService.downloadMissingData(symbol);

                if (recordsUpdated > 0) {
                    logger.info("{}: Updated {} records", symbol, recordsUpdated);
                    totalUpdated += recordsUpdated;
                    successCount++;
                } else {
                    logger.debug("{}: No updates needed", symbol);
                    successCount++;
                }

                // Rate limiting
                if (i < symbols.size() - 1) {
                    Thread.sleep(1000);
                }

            } catch (InterruptedException e) {
                logger.error("Weekly refresh interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Failed to refresh data for {}: {}", symbol, e.getMessage());
                failCount++;
            }
        }

        lastHistoricalRefresh = LocalDateTime.now();
        logger.info("Weekly historical refresh completed: {} records updated, {} successful, {} failed",
                totalUpdated, successCount, failCount);
        logger.info("Completed at {}", lastHistoricalRefresh);
    }

    /**
     * Hourly quote refresh during market hours
     * Runs every hour from 9 AM to 5 PM ET on weekdays
     */
    @Scheduled(cron = "0 0 9-17 * * MON-FRI", zone = "America/New_York")
    public void refreshQuotesHourly() {
        // Check if market is actually open
        if (!isMarketOpen()) {
            logger.debug("Market is closed, skipping quote refresh");
            return;
        }

        logger.info("Starting hourly quote refresh (market hours)");

        List<StockUniverse> activeStocks = stockUniverseService.getActiveStocks();
        logger.info("Refreshing quotes for {} stocks", activeStocks.size());

        // Refresh recent data (last 3 days to ensure we have latest)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(3);

        int totalUpdated = 0;
        int successCount = 0;

        for (StockUniverse stock : activeStocks) {
            try {
                int recordsUpdated = historicalDataService.downloadHistoricalPrices(
                        stock.getSymbol(), startDate, endDate);

                if (recordsUpdated > 0) {
                    totalUpdated += recordsUpdated;
                    successCount++;
                }

                // Brief delay to avoid rate limiting
                Thread.sleep(500);

            } catch (InterruptedException e) {
                logger.error("Quote refresh interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Failed to refresh quote for {}: {}", stock.getSymbol(), e.getMessage());
            }
        }

        lastQuoteRefresh = LocalDateTime.now();
        logger.info("Hourly quote refresh completed: {} records updated from {} stocks",
                totalUpdated, successCount);
    }

    /**
     * Check if US stock market is currently open
     */
    private boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(ET_ZONE);
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        // Check if it's a weekday
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // Check if within market hours
        LocalTime currentTime = now.toLocalTime();
        return !currentTime.isBefore(MARKET_OPEN) && !currentTime.isAfter(MARKET_CLOSE);
    }

    /**
     * Manual refresh trigger for all data
     */
    public void manualRefreshAll() {
        logger.info("Manual refresh of all data triggered");

        // Trigger catch-up process which will update everything
        dataCatchUpService.forceCatchUp();

        lastFundamentalsRefresh = LocalDateTime.now();
        lastHistoricalRefresh = LocalDateTime.now();
        lastQuoteRefresh = LocalDateTime.now();
    }

    /**
     * Get last refresh timestamps
     */
    public LocalDateTime getLastFundamentalsRefresh() {
        return lastFundamentalsRefresh;
    }

    public LocalDateTime getLastHistoricalRefresh() {
        return lastHistoricalRefresh;
    }

    public LocalDateTime getLastQuoteRefresh() {
        return lastQuoteRefresh;
    }

    /**
     * Check market status
     */
    public String getMarketStatus() {
        if (isMarketOpen()) {
            return "OPEN";
        } else {
            ZonedDateTime now = ZonedDateTime.now(ET_ZONE);
            DayOfWeek dayOfWeek = now.getDayOfWeek();

            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                return "CLOSED_WEEKEND";
            } else {
                LocalTime currentTime = now.toLocalTime();
                if (currentTime.isBefore(MARKET_OPEN)) {
                    return "CLOSED_PRE_MARKET";
                } else {
                    return "CLOSED_POST_MARKET";
                }
            }
        }
    }
}
