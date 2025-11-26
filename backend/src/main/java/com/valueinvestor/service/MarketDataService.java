package com.valueinvestor.service;

import com.valueinvestor.model.dto.QuoteDTO;
import com.valueinvestor.model.entity.StockFundamentals;
import com.valueinvestor.repository.StockFundamentalsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockDividend;
import yahoofinance.quotes.stock.StockStats;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    private static final int CACHE_HOURS = 24; // Cache fundamentals for 24 hours
    private static final int CIRCUIT_BREAKER_THRESHOLD = 3; // Number of failures before switching to Polygon

    @Autowired
    private StockFundamentalsRepository fundamentalsRepository;

    @Autowired
    private PolygonDataService polygonDataService;

    @Autowired
    private com.valueinvestor.repository.StockPriceHistoryRepository priceHistoryRepository;

    // Circuit breaker for Yahoo Finance
    private final AtomicInteger yahooFailureCount = new AtomicInteger(0);
    private volatile boolean usePolygonFallback = false;

    /**
     * Get current quote for a stock with fallback to Polygon.io
     */
    public BigDecimal getQuote(String symbol) {
        // Try Yahoo Finance first if circuit breaker is not triggered
        if (!usePolygonFallback) {
            try {
                Stock stock = YahooFinance.get(symbol);
                if (stock != null && stock.getQuote() != null && stock.getQuote().getPrice() != null) {
                    BigDecimal price = stock.getQuote().getPrice();
                    logger.info("Yahoo Finance quote for {}: ${}", symbol, price);
                    // Reset failure count on success
                    yahooFailureCount.set(0);
                    return price;
                }
            } catch (IOException e) {
                logger.warn("Yahoo Finance failed for {}: {}", symbol, e.getMessage());
                incrementYahooFailureCount();
            } catch (Exception e) {
                logger.error("Yahoo Finance error for {}: {}", symbol, e.getMessage());
                incrementYahooFailureCount();
            }
        }

        // Fallback to Polygon.io
        if (polygonDataService.isConfigured()) {
            logger.info("Using Polygon.io fallback for quote: {}", symbol);
            QuoteDTO quote = polygonDataService.getQuote(symbol);
            if (quote != null && quote.getPrice() != null) {
                return quote.getPrice();
            }
        }

        return BigDecimal.ZERO;
    }

    /**
     * Increment Yahoo failure count and trigger circuit breaker if threshold reached
     */
    private void incrementYahooFailureCount() {
        int failures = yahooFailureCount.incrementAndGet();
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            logger.warn("Yahoo Finance circuit breaker triggered after {} failures. Switching to Polygon.io", failures);
            usePolygonFallback = true;
        }
    }

    /**
     * Reset circuit breaker to try Yahoo Finance again
     */
    public void resetCircuitBreaker() {
        yahooFailureCount.set(0);
        usePolygonFallback = false;
        logger.info("Yahoo Finance circuit breaker reset");
    }

    /**
     * Get dividend yield for a stock
     */
    public BigDecimal getDividendYield(String symbol) {
        try {
            Stock stock = YahooFinance.get(symbol);
            if (stock != null && stock.getDividend() != null) {
                BigDecimal yield = stock.getDividend().getAnnualYield();
                logger.info("Dividend yield for {}: {}%", symbol, yield);
                return yield != null ? yield : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            logger.error("Failed to get dividend yield for {}", symbol, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get fundamentals for a stock (with caching)
     */
    public StockFundamentals getFundamentals(String symbol) {
        // Check cache first
        LocalDateTime cacheThreshold = LocalDateTime.now().minusHours(CACHE_HOURS);
        Optional<StockFundamentals> cached = fundamentalsRepository.findBySymbolIfRecent(symbol, cacheThreshold);

        if (cached.isPresent()) {
            logger.info("Using cached fundamentals for {}", symbol);
            return cached.get();
        }

        // Fetch fresh data
        logger.info("Fetching fresh fundamentals for {}", symbol);
        return fetchAndSaveFundamentals(symbol);
    }

    /**
     * Fetch fundamentals from Yahoo Finance and save to database with Polygon fallback
     */
    private StockFundamentals fetchAndSaveFundamentals(String symbol) {
        StockFundamentals fundamentals = null;

        // Try Yahoo Finance first if circuit breaker is not triggered
        if (!usePolygonFallback) {
            try {
                Stock stock = YahooFinance.get(symbol, true);

                if (stock != null) {
                    fundamentals = fundamentalsRepository.findBySymbol(symbol)
                            .orElse(new StockFundamentals(symbol, stock.getName()));

                    // Update basic info
                    fundamentals.setName(stock.getName());
                    fundamentals.setMarket("US");
                    fundamentals.setLastUpdated(LocalDateTime.now());

                    // Update price
                    if (stock.getQuote() != null) {
                        fundamentals.setCurrentPrice(stock.getQuote().getPrice());
                    }

                    // Update dividend info
                    if (stock.getDividend() != null) {
                        StockDividend dividend = stock.getDividend();
                        fundamentals.setDividendYield(dividend.getAnnualYield());
                    }

                    // Update stats
                    if (stock.getStats() != null) {
                        StockStats stats = stock.getStats();
                        fundamentals.setPeRatio(stats.getPe());
                        fundamentals.setPbRatio(stats.getPriceBook());
                        fundamentals.setMarketCap(stats.getMarketCap());
                    }

                    // Save to database
                    fundamentals = fundamentalsRepository.save(fundamentals);
                    logger.info("Saved fundamentals from Yahoo Finance for {}", symbol);

                    // Reset failure count on success
                    yahooFailureCount.set(0);
                    return fundamentals;
                }
            } catch (IOException e) {
                logger.warn("Yahoo Finance failed for fundamentals {}: {}", symbol, e.getMessage());
                incrementYahooFailureCount();
            } catch (Exception e) {
                logger.error("Yahoo Finance error for fundamentals {}: {}", symbol, e.getMessage());
                incrementYahooFailureCount();
            }
        }

        // Fallback to Polygon.io for basic info
        if (polygonDataService.isConfigured()) {
            logger.info("Using Polygon.io fallback for fundamentals: {}", symbol);
            try {
                QuoteDTO polygonQuote = polygonDataService.getFundamentals(symbol);
                if (polygonQuote != null) {
                    fundamentals = fundamentalsRepository.findBySymbol(symbol)
                            .orElse(new StockFundamentals(symbol, polygonQuote.getName()));

                    fundamentals.setName(polygonQuote.getName());
                    fundamentals.setMarket("US");
                    fundamentals.setLastUpdated(LocalDateTime.now());

                    if (polygonQuote.getMarketCap() != null) {
                        fundamentals.setMarketCap(polygonQuote.getMarketCap());
                    }

                    // Get current price from Polygon
                    QuoteDTO priceQuote = polygonDataService.getQuote(symbol);
                    if (priceQuote != null && priceQuote.getPrice() != null) {
                        fundamentals.setCurrentPrice(priceQuote.getPrice());
                    }

                    fundamentals = fundamentalsRepository.save(fundamentals);
                    logger.info("Saved fundamentals from Polygon.io for {}", symbol);
                    return fundamentals;
                }
            } catch (Exception e) {
                logger.error("Polygon.io also failed for fundamentals {}: {}", symbol, e.getMessage());
            }
        }

        logger.error("Failed to fetch fundamentals for {} from all sources", symbol);
        return null;
    }

    /**
     * Get top dividend stocks from database
     */
    public List<StockFundamentals> getTopDividendStocks(int limit) {
        List<StockFundamentals> stocks = fundamentalsRepository.findTopDividendStocks();
        return stocks.stream().limit(limit).toList();
    }

    /**
     * Search stocks by name or symbol
     */
    public List<StockFundamentals> searchStocks(String query) {
        return fundamentalsRepository.searchByNameOrSymbol(query);
    }

    /**
     * Refresh stale fundamentals data
     */
    public void refreshStaleData() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(CACHE_HOURS);
        List<StockFundamentals> staleData = fundamentalsRepository.findStaleData(cutoffTime);

        logger.info("Refreshing {} stale stock fundamentals", staleData.size());

        for (StockFundamentals fundamental : staleData) {
            fetchAndSaveFundamentals(fundamental.getSymbol());
        }
    }

    /**
     * Get stocks with minimum dividend yield
     */
    public List<StockFundamentals> getStocksByMinDividendYield(BigDecimal minYield) {
        return fundamentalsRepository.findStocksByMinDividendYield(minYield);
    }

    /**
     * Validate if a symbol exists
     */
    public boolean isValidSymbol(String symbol) {
        try {
            Stock stock = YahooFinance.get(symbol);
            return stock != null && stock.getQuote() != null && stock.getQuote().getPrice() != null;
        } catch (Exception e) {
            logger.error("Failed to validate symbol: {}", symbol, e);
            return false;
        }
    }

    /**
     * Get historical closing price for a specific date (for catch-up rebalancing)
     */
    public BigDecimal getHistoricalClosePrice(String symbol, LocalDate date) {
        try {
            // Try to get from price history repository first
            Optional<com.valueinvestor.model.entity.StockPriceHistory> historicalPrice = 
                getHistoricalPriceFromDb(symbol, date);
            
            if (historicalPrice.isPresent()) {
                logger.info("Using cached historical price for {} on {}: {}", 
                    symbol, date, historicalPrice.get().getClose());
                return historicalPrice.get().getClose();
            }
            
            // Fallback to current price if no historical data available
            logger.warn("No historical price found for {} on {}, using current price", symbol, date);
            return getQuote(symbol);
        } catch (Exception e) {
            logger.error("Failed to get historical price for {} on {}", symbol, date, e);
            return getQuote(symbol);
        }
    }

    private Optional<com.valueinvestor.model.entity.StockPriceHistory> getHistoricalPriceFromDb(String symbol, LocalDate date) {
        return priceHistoryRepository.findBySymbolAndDate(symbol, date);
    }
}
