package com.valueinvestor.service;

import com.valueinvestor.model.entity.StockFundamentals;
import com.valueinvestor.model.entity.StockPriceHistory;
import com.valueinvestor.model.entity.StockUniverse;
import com.valueinvestor.repository.StockFundamentalsRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DataCatchUpService {

    private static final Logger logger = LoggerFactory.getLogger(DataCatchUpService.class);
    private static final int HISTORICAL_YEARS = 5;
    private static final int FUNDAMENTALS_STALE_HOURS = 24;

    private final AtomicBoolean catchUpInProgress = new AtomicBoolean(false);

    @Autowired
    private StockUniverseService stockUniverseService;

    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private StockFundamentalsRepository fundamentalsRepository;

    /**
     * Run data catch-up check on application startup
     */
    @PostConstruct
    public void checkAndCatchUp() {
        // Run in a separate thread to avoid blocking application startup
        Thread catchUpThread = new Thread(() -> {
            try {
                // Give the application time to fully start
                Thread.sleep(5000);
                runCatchUp();
            } catch (InterruptedException e) {
                logger.error("Catch-up thread interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error during catch-up: {}", e.getMessage(), e);
            }
        });

        catchUpThread.setName("DataCatchUpThread");
        catchUpThread.setDaemon(true);
        catchUpThread.start();

        logger.info("Data catch-up scheduled to run after application startup");
    }

    /**
     * Execute the full data catch-up process
     */
    public void runCatchUp() {
        if (!catchUpInProgress.compareAndSet(false, true)) {
            logger.warn("Data catch-up already in progress, skipping");
            return;
        }

        try {
            logger.info("========================================");
            logger.info("Starting Data Catch-Up Process");
            logger.info("========================================");

            List<StockUniverse> activeStocks = stockUniverseService.getActiveStocks();
            logger.info("Found {} active stocks in universe", activeStocks.size());

            if (activeStocks.isEmpty()) {
                logger.warn("No active stocks in universe. Please initialize the stock universe first.");
                return;
            }

            // Step 1: Check and download missing historical data
            catchUpHistoricalData(activeStocks);

            // Step 2: Check and refresh stale fundamentals
            catchUpFundamentals(activeStocks);

            logger.info("========================================");
            logger.info("Data Catch-Up Process Completed");
            logger.info("========================================");

        } finally {
            catchUpInProgress.set(false);
        }
    }

    /**
     * Catch up historical price data
     */
    private void catchUpHistoricalData(List<StockUniverse> stocks) {
        logger.info("--- Checking Historical Price Data ---");

        List<String> stocksNeedingData = new ArrayList<>();
        List<String> stocksNeedingUpdate = new ArrayList<>();

        for (StockUniverse stock : stocks) {
            String symbol = stock.getSymbol();

            if (!historicalDataService.hasHistoricalData(symbol)) {
                stocksNeedingData.add(symbol);
                logger.info("{}: No historical data found", symbol);
            } else {
                Optional<StockPriceHistory> latestPrice = historicalDataService.getLatestPrice(symbol);
                if (latestPrice.isPresent()) {
                    LocalDate latestDate = latestPrice.get().getDate();
                    LocalDate today = LocalDate.now();

                    // Check if data is outdated (more than 1 day old)
                    if (latestDate.isBefore(today.minusDays(1))) {
                        stocksNeedingUpdate.add(symbol);
                        logger.info("{}: Latest data is from {}, needs update", symbol, latestDate);
                    } else {
                        logger.debug("{}: Data is up to date (latest: {})", symbol, latestDate);
                    }
                }
            }
        }

        // Download missing data
        if (!stocksNeedingData.isEmpty()) {
            logger.info("Downloading historical data for {} stocks with missing data", stocksNeedingData.size());
            downloadHistoricalDataBatch(stocksNeedingData, true);
        }

        // Update outdated data
        if (!stocksNeedingUpdate.isEmpty()) {
            logger.info("Updating historical data for {} stocks with outdated data", stocksNeedingUpdate.size());
            downloadHistoricalDataBatch(stocksNeedingUpdate, false);
        }

        if (stocksNeedingData.isEmpty() && stocksNeedingUpdate.isEmpty()) {
            logger.info("All historical data is up to date");
        }
    }

    /**
     * Download historical data for a batch of stocks
     */
    private void downloadHistoricalDataBatch(List<String> symbols, boolean fullHistory) {
        LocalDate endDate = LocalDate.now();
        int totalDownloaded = 0;
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);

            try {
                int recordsDownloaded;

                if (fullHistory) {
                    // Download full 5-year history
                    LocalDate startDate = endDate.minusYears(HISTORICAL_YEARS);
                    logger.info("[{}/{}] Downloading 5-year history for {}...",
                            i + 1, symbols.size(), symbol);
                    recordsDownloaded = historicalDataService.downloadHistoricalPrices(symbol, startDate, endDate);
                } else {
                    // Download only missing recent data
                    logger.info("[{}/{}] Updating recent data for {}...",
                            i + 1, symbols.size(), symbol);
                    recordsDownloaded = historicalDataService.downloadMissingData(symbol);
                }

                if (recordsDownloaded > 0) {
                    logger.info("{}: Successfully downloaded {} records", symbol, recordsDownloaded);
                    totalDownloaded += recordsDownloaded;
                    successCount++;
                } else {
                    logger.warn("{}: No records downloaded", symbol);
                    failCount++;
                }

                // Rate limiting delay (2 seconds between stocks)
                if (i < symbols.size() - 1) {
                    Thread.sleep(2000);
                }

            } catch (InterruptedException e) {
                logger.error("Download interrupted for {}: {}", symbol, e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Failed to download data for {}: {}", symbol, e.getMessage());
                failCount++;
            }
        }

        logger.info("Historical data download complete: {} records, {} successful, {} failed",
                totalDownloaded, successCount, failCount);
    }

    /**
     * Catch up fundamentals data
     */
    private void catchUpFundamentals(List<StockUniverse> stocks) {
        logger.info("--- Checking Fundamentals Data ---");

        List<String> stocksNeedingFundamentals = new ArrayList<>();
        LocalDateTime staleThreshold = LocalDateTime.now().minusHours(FUNDAMENTALS_STALE_HOURS);

        for (StockUniverse stock : stocks) {
            String symbol = stock.getSymbol();

            Optional<StockFundamentals> fundamentals = fundamentalsRepository.findBySymbol(symbol);

            if (fundamentals.isEmpty()) {
                stocksNeedingFundamentals.add(symbol);
                logger.info("{}: No fundamentals data found", symbol);
            } else {
                LocalDateTime lastUpdated = fundamentals.get().getLastUpdated();
                if (lastUpdated.isBefore(staleThreshold)) {
                    stocksNeedingFundamentals.add(symbol);
                    logger.info("{}: Fundamentals stale (last updated: {})", symbol, lastUpdated);
                } else {
                    logger.debug("{}: Fundamentals are current (last updated: {})", symbol, lastUpdated);
                }
            }
        }

        if (!stocksNeedingFundamentals.isEmpty()) {
            logger.info("{} stocks need fundamentals refresh", stocksNeedingFundamentals.size());
            logger.info("NOTE: Fundamentals refresh will be handled by scheduled tasks");
            logger.info("Stocks needing refresh: {}", String.join(", ", stocksNeedingFundamentals));
        } else {
            logger.info("All fundamentals data is up to date");
        }
    }

    /**
     * Check if catch-up is in progress
     */
    public boolean isCatchUpInProgress() {
        return catchUpInProgress.get();
    }

    /**
     * Get catch-up status summary
     */
    public String getCatchUpStatus() {
        if (catchUpInProgress.get()) {
            return "IN_PROGRESS";
        }

        List<StockUniverse> activeStocks = stockUniverseService.getActiveStocks();
        long stocksWithData = activeStocks.stream()
                .filter(stock -> historicalDataService.hasHistoricalData(stock.getSymbol()))
                .count();

        if (stocksWithData == activeStocks.size()) {
            return "COMPLETE";
        } else {
            return "INCOMPLETE";
        }
    }

    /**
     * Force a manual catch-up run
     */
    public void forceCatchUp() {
        logger.info("Manual catch-up triggered");
        runCatchUp();
    }
}
