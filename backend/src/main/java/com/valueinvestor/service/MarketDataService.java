package com.valueinvestor.service;

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

/**
 * Market data service for Taiwan stocks
 * Primary source: Local SQLite cache (populated by Shioaji)
 * Fallback: Yahoo Finance (rate-limited)
 */
@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    private static final int CACHE_HOURS = 24;
    private static final int YAHOO_RATE_LIMIT_MS = 2000; // 2 seconds between Yahoo calls

    @Autowired
    private StockFundamentalsRepository fundamentalsRepository;

    @Autowired
    private com.valueinvestor.repository.StockPriceHistoryRepository priceHistoryRepository;
    
    @Autowired
    private com.valueinvestor.repository.StockUniverseRepository stockUniverseRepository;

    // Rate limiting for Yahoo Finance
    private final AtomicInteger yahooFailureCount = new AtomicInteger(0);
    private volatile long lastYahooCall = 0;

    /**
     * Get current quote for a stock
     * Prefers cached data, falls back to Yahoo Finance with rate limiting
     */
    public BigDecimal getQuote(String symbol) {
        // 1. Try cached price first (most recent)
        Optional<com.valueinvestor.model.entity.StockPriceHistory> cached = 
            priceHistoryRepository.findLatestPriceForSymbol(symbol);
        
        if (cached.isPresent()) {
            LocalDate priceDate = cached.get().getDate();
            // If price is from today or yesterday, use it
            if (!priceDate.isBefore(LocalDate.now().minusDays(2))) {
                logger.debug("Using cached quote for {}: NT${}", symbol, cached.get().getClose());
                return cached.get().getClose();
            }
        }

        // 2. Fallback to Yahoo Finance with rate limiting
        return getYahooQuoteWithRateLimit(symbol);
    }

    /**
     * Get Yahoo Finance quote with rate limiting
     */
    private BigDecimal getYahooQuoteWithRateLimit(String symbol) {
        try {
            // Enforce rate limit
            long now = System.currentTimeMillis();
            long timeSinceLastCall = now - lastYahooCall;
            if (timeSinceLastCall < YAHOO_RATE_LIMIT_MS) {
                Thread.sleep(YAHOO_RATE_LIMIT_MS - timeSinceLastCall);
            }
            lastYahooCall = System.currentTimeMillis();

            Stock stock = YahooFinance.get(symbol);
            if (stock != null && stock.getQuote() != null && stock.getQuote().getPrice() != null) {
                BigDecimal price = stock.getQuote().getPrice();
                logger.info("Yahoo Finance quote for {}: NT${}", symbol, price);
                yahooFailureCount.set(0);
                return price;
            }
        } catch (IOException e) {
            logger.warn("Yahoo Finance failed for {}: {}", symbol, e.getMessage());
            yahooFailureCount.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Yahoo Finance error for {}: {}", symbol, e.getMessage());
            yahooFailureCount.incrementAndGet();
        }

        // Last resort: return most recent cached price
        Optional<com.valueinvestor.model.entity.StockPriceHistory> fallback = 
            priceHistoryRepository.findLatestPriceForSymbol(symbol);
        if (fallback.isPresent()) {
            logger.warn("Using stale cached price for {} from {}", symbol, fallback.get().getDate());
            return fallback.get().getClose();
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get dividend yield for a stock
     */
    public BigDecimal getDividendYield(String symbol) {
        // First check fundamentals cache
        Optional<StockFundamentals> cached = fundamentalsRepository.findBySymbol(symbol);
        if (cached.isPresent() && cached.get().getDividendYield() != null) {
            return cached.get().getDividendYield();
        }

        // Fallback to Yahoo Finance
        try {
            Thread.sleep(YAHOO_RATE_LIMIT_MS);
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
        LocalDateTime cacheThreshold = LocalDateTime.now().minusHours(CACHE_HOURS);
        Optional<StockFundamentals> cached = fundamentalsRepository.findBySymbolIfRecent(symbol, cacheThreshold);

        if (cached.isPresent()) {
            logger.debug("Using cached fundamentals for {}", symbol);
            return cached.get();
        }

        logger.info("Fetching fresh fundamentals for {}", symbol);
        return fetchAndSaveFundamentals(symbol);
    }

    /**
     * Fetch fundamentals from Yahoo Finance and save to database
     */
    private StockFundamentals fetchAndSaveFundamentals(String symbol) {
        try {
            Thread.sleep(YAHOO_RATE_LIMIT_MS);
            Stock stock = YahooFinance.get(symbol, true);

            if (stock != null) {
                StockFundamentals fundamentals = fundamentalsRepository.findBySymbol(symbol)
                        .orElse(new StockFundamentals(symbol, stock.getName()));

                fundamentals.setName(stock.getName());
                fundamentals.setMarket("TW");
                fundamentals.setLastUpdated(LocalDateTime.now());

                if (stock.getQuote() != null) {
                    fundamentals.setCurrentPrice(stock.getQuote().getPrice());
                }

                if (stock.getDividend() != null) {
                    StockDividend dividend = stock.getDividend();
                    fundamentals.setDividendYield(dividend.getAnnualYield());
                }

                if (stock.getStats() != null) {
                    StockStats stats = stock.getStats();
                    fundamentals.setPeRatio(stats.getPe());
                    fundamentals.setPbRatio(stats.getPriceBook());
                    fundamentals.setMarketCap(stats.getMarketCap());
                }

                fundamentals = fundamentalsRepository.save(fundamentals);
                logger.info("Saved fundamentals for {}", symbol);
                return fundamentals;
            }
        } catch (IOException e) {
            logger.warn("Yahoo Finance failed for fundamentals {}: {}", symbol, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error fetching fundamentals for {}: {}", symbol, e.getMessage());
        }

        logger.error("Failed to fetch fundamentals for {}", symbol);
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
     * Get list of active stock symbols from universe
     */
    public List<String> getActiveStockSymbols() {
        return stockUniverseRepository.findByActiveTrue()
                .stream()
                .map(stock -> stock.getSymbol())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Validate if a symbol exists
     */
    public boolean isValidSymbol(String symbol) {
        // Check local cache first
        if (priceHistoryRepository.existsBySymbol(symbol)) {
            return true;
        }
        
        // Then try Yahoo
        try {
            Thread.sleep(YAHOO_RATE_LIMIT_MS);
            Stock stock = YahooFinance.get(symbol);
            return stock != null && stock.getQuote() != null && stock.getQuote().getPrice() != null;
        } catch (Exception e) {
            logger.error("Failed to validate symbol: {}", symbol, e);
            return false;
        }
    }

    /**
     * Get historical closing price for a specific date (for catch-up rebalancing)
     * Uses smart fallback: exact date → nearest cached → most recent cached → Yahoo
     */
    public BigDecimal getHistoricalClosePrice(String symbol, LocalDate date) {
        try {
            // 1. Try exact date match from cache
            Optional<com.valueinvestor.model.entity.StockPriceHistory> exactPrice = 
                priceHistoryRepository.findBySymbolAndDate(symbol, date);
            
            if (exactPrice.isPresent()) {
                logger.info("Using exact cached price for {} on {}: NT${}", 
                    symbol, date, exactPrice.get().getClose());
                return exactPrice.get().getClose();
            }
            
            // 2. Try to find nearest price before requested date (within 7 days)
            LocalDate searchStart = date.minusDays(7);
            Optional<com.valueinvestor.model.entity.StockPriceHistory> nearestPrice = 
                priceHistoryRepository.findNearestPriceBeforeDate(symbol, date, searchStart);
            
            if (nearestPrice.isPresent()) {
                logger.info("Using nearest cached price for {} (wanted {}, got {}): NT${}", 
                    symbol, date, nearestPrice.get().getDate(), nearestPrice.get().getClose());
                return nearestPrice.get().getClose();
            }
            
            // 3. Fallback to most recent cached price (any date)
            Optional<com.valueinvestor.model.entity.StockPriceHistory> latestPrice = 
                priceHistoryRepository.findLatestPriceForSymbol(symbol);
            
            if (latestPrice.isPresent()) {
                logger.warn("Using most recent cached price for {} (no data near {}): NT${} from {}", 
                    symbol, date, latestPrice.get().getClose(), latestPrice.get().getDate());
                return latestPrice.get().getClose();
            }
            
            // 4. Last resort: try Yahoo Finance
            logger.warn("No cached price for {}, attempting Yahoo Finance fetch", symbol);
            BigDecimal yahooPrice = getYahooQuoteWithRateLimit(symbol);
            if (yahooPrice.compareTo(BigDecimal.ZERO) > 0) {
                return yahooPrice;
            }
            
            logger.error("CRITICAL: No price available for {} on any source", symbol);
            return BigDecimal.ZERO;
            
        } catch (Exception e) {
            logger.error("Failed to get historical price for {} on {}: {}", symbol, date, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
