package com.valueinvestor.service;

import com.valueinvestor.model.entity.StockPriceHistory;
import com.valueinvestor.repository.StockPriceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class HistoricalDataService {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalDataService.class);
    private static final int BATCH_DELAY_MS = 1000; // 1 second delay between API calls for Shioaji

    @Autowired
    private StockPriceHistoryRepository priceHistoryRepository;

    @Autowired
    private ShioajiDataService shioajiDataService;

    /**
     * Download historical prices for a single symbol using Shioaji
     */
    @Transactional
    public int downloadHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        logger.info("Downloading historical data for {} from {} to {}", symbol, startDate, endDate);

        if (!shioajiDataService.isAvailable()) {
            logger.error("Shioaji API not available, cannot fetch data for {}", symbol);
            return 0;
        }

        try {
            // Fetch historical data via Shioaji HTTP API
            List<StockPriceHistory> shioajiPrices = shioajiDataService.getHistoricalPrices(symbol, startDate, endDate);

            if (shioajiPrices == null || shioajiPrices.isEmpty()) {
                logger.warn("No historical data from Shioaji for {}", symbol);
                return 0;
            }

            int savedCount = 0;

            for (StockPriceHistory price : shioajiPrices) {
                try {
                    // Check if data already exists for this date
                    if (priceHistoryRepository.existsBySymbolAndDate(symbol, price.getDate())) {
                        logger.debug("Data already exists for {} on {}, skipping", symbol, price.getDate());
                        continue;
                    }

                    priceHistoryRepository.save(price);
                    savedCount++;

                } catch (Exception e) {
                    logger.error("Failed to save Shioaji price data for {} on {}: {}",
                            symbol, price.getDate(), e.getMessage());
                }
            }

            logger.info("Successfully saved {} historical prices for {} (Shioaji)", savedCount, symbol);
            return savedCount;

        } catch (Exception e) {
            logger.error("Shioaji failed for {}: {}", symbol, e.getMessage());
            return 0;
        }
    }


    /**
     * Bulk download historical prices for multiple symbols
     */
    public Map<String, Integer> bulkDownload(List<String> symbols, LocalDate startDate, LocalDate endDate) {
        logger.info("Starting bulk download for {} symbols", symbols.size());

        Map<String, Integer> results = new HashMap<>();

        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            try {
                logger.info("Downloading historical data for {} ({}/{})", symbol, i + 1, symbols.size());

                int count = downloadHistoricalPrices(symbol, startDate, endDate);
                results.put(symbol, count);

                // Add delay to avoid rate limiting (2 seconds between each stock)
                if (i < symbols.size() - 1) {
                    Thread.sleep(BATCH_DELAY_MS);
                }

            } catch (InterruptedException e) {
                logger.error("Bulk download interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Failed to download data for {}: {}", symbol, e.getMessage());
                results.put(symbol, 0);
            }
        }

        int totalRecords = results.values().stream().mapToInt(Integer::intValue).sum();
        logger.info("Bulk download completed. Total records saved: {}", totalRecords);

        return results;
    }

    /**
     * Get historical prices for a symbol within a date range
     */
    public List<StockPriceHistory> getHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        return priceHistoryRepository.findBySymbolAndDateBetweenOrderByDateAsc(symbol, startDate, endDate);
    }

    /**
     * Get latest price for a symbol
     */
    public Optional<StockPriceHistory> getLatestPrice(String symbol) {
        return priceHistoryRepository.findLatestPriceForSymbol(symbol);
    }

    /**
     * Get earliest price for a symbol
     */
    public Optional<StockPriceHistory> getEarliestPrice(String symbol) {
        return priceHistoryRepository.findEarliestPriceForSymbol(symbol);
    }

    /**
     * Check if historical data exists for a symbol
     */
    public boolean hasHistoricalData(String symbol) {
        return priceHistoryRepository.existsBySymbol(symbol);
    }

    /**
     * Get count of historical records for a symbol
     */
    public long getRecordCount(String symbol) {
        return priceHistoryRepository.countBySymbol(symbol);
    }

    /**
     * Get all symbols with historical data
     */
    public List<String> getSymbolsWithData() {
        return priceHistoryRepository.findDistinctSymbols();
    }

    /**
     * Download missing data for a symbol
     * If no data exists, download last 5 years
     * If partial data exists, fill gaps
     */
    @Transactional
    public int downloadMissingData(String symbol) {
        Optional<StockPriceHistory> latest = getLatestPrice(symbol);
        Optional<StockPriceHistory> earliest = getEarliestPrice(symbol);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        if (latest.isEmpty()) {
            // No data exists, download last 5 years
            startDate = endDate.minusYears(5);
            logger.info("No data exists for {}, downloading 5 years of history", symbol);
        } else {
            // Data exists, check if it's current
            LocalDate latestDate = latest.get().getDate();
            if (latestDate.isBefore(endDate.minusDays(1))) {
                // Update from latest date to today
                startDate = latestDate.plusDays(1);
                logger.info("Updating {} from {} to {}", symbol, startDate, endDate);
            } else {
                logger.info("Data for {} is up to date", symbol);
                return 0;
            }
        }

        return downloadHistoricalPrices(symbol, startDate, endDate);
    }

    /**
     * Calculate data completeness percentage for a symbol
     */
    public double getDataCompleteness(String symbol, LocalDate startDate, LocalDate endDate) {
        long actualRecords = priceHistoryRepository.countBySymbol(symbol);

        // Estimate expected trading days (approximately 252 per year)
        long years = endDate.getYear() - startDate.getYear() + 1;
        long expectedRecords = years * 252;

        return (actualRecords / (double) expectedRecords) * 100.0;
    }

    /**
     * Delete all historical data for a symbol
     */
    @Transactional
    public void deleteHistoricalData(String symbol) {
        logger.warn("Deleting all historical data for {}", symbol);
        priceHistoryRepository.deleteBySymbol(symbol);
    }

    /**
     * Refresh recent data (last 30 days) for a symbol
     */
    @Transactional
    public int refreshRecentData(String symbol) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        logger.info("Refreshing recent data for {} (last 30 days)", symbol);
        return downloadHistoricalPrices(symbol, startDate, endDate);
    }
}
