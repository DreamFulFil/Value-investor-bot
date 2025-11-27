package com.valueinvestor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service to dynamically fetch Taiwan's top dividend stocks
 * Fetches real-time data from Yahoo Finance to identify high-yield dividend stocks
 */
@Service
public class TaiwanStockScreenerService {

    private static final Logger logger = LoggerFactory.getLogger(TaiwanStockScreenerService.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Cache for dynamic stock data (refreshed periodically)
    private volatile List<StockInfo> cachedDividendStocks = null;
    private volatile long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000; // 24 hours
    
    /**
     * Taiwan 50 Index constituents and other major dividend stocks to screen
     * These are candidates - we fetch actual dividend yields dynamically
     */
    private static final List<String> CANDIDATE_SYMBOLS = Arrays.asList(
        // Taiwan 50 + High Dividend ETF constituents
        "2330.TW", "2317.TW", "2454.TW", "2308.TW", "2303.TW", "2357.TW", "2382.TW",
        "3008.TW", "2474.TW", "2408.TW", "2301.TW", "2412.TW", "3045.TW", "4904.TW",
        "2881.TW", "2882.TW", "2884.TW", "2886.TW", "2891.TW", "2892.TW", "2880.TW",
        "5880.TW", "2883.TW", "2887.TW", "2888.TW", "2890.TW", "5871.TW", "2801.TW",
        "1301.TW", "1303.TW", "1326.TW", "2002.TW", "1101.TW", "1102.TW", "1402.TW",
        "1216.TW", "2912.TW", "1227.TW", "9910.TW", "2105.TW", "2207.TW", "2327.TW",
        "2603.TW", "2609.TW", "2615.TW", "1504.TW", "2542.TW", "2545.TW", "9904.TW",
        "6505.TW", "3711.TW", "2395.TW", "2379.TW", "2345.TW", "3034.TW", "2377.TW",
        "6415.TW", "3231.TW", "2049.TW", "1590.TW"
    );
    
    // Fallback stock info (used when API fails)
    private static final Map<String, StockInfo> FALLBACK_STOCKS = new LinkedHashMap<>();
    static {
        FALLBACK_STOCKS.put("2330.TW", new StockInfo("2330.TW", "台積電", "Technology", new BigDecimal("3.0")));
        FALLBACK_STOCKS.put("2317.TW", new StockInfo("2317.TW", "鴻海", "Technology", new BigDecimal("5.5")));
        FALLBACK_STOCKS.put("2412.TW", new StockInfo("2412.TW", "中華電", "Telecom", new BigDecimal("4.8")));
        FALLBACK_STOCKS.put("2882.TW", new StockInfo("2882.TW", "國泰金", "Financials", new BigDecimal("4.2")));
        FALLBACK_STOCKS.put("2881.TW", new StockInfo("2881.TW", "富邦金", "Financials", new BigDecimal("4.5")));
        FALLBACK_STOCKS.put("2886.TW", new StockInfo("2886.TW", "兆豐金", "Financials", new BigDecimal("5.8")));
        FALLBACK_STOCKS.put("2884.TW", new StockInfo("2884.TW", "玉山金", "Financials", new BigDecimal("4.0")));
        FALLBACK_STOCKS.put("2891.TW", new StockInfo("2891.TW", "中信金", "Financials", new BigDecimal("5.2")));
        FALLBACK_STOCKS.put("5880.TW", new StockInfo("5880.TW", "合庫金", "Financials", new BigDecimal("5.5")));
        FALLBACK_STOCKS.put("2002.TW", new StockInfo("2002.TW", "中鋼", "Materials", new BigDecimal("6.0")));
        FALLBACK_STOCKS.put("1301.TW", new StockInfo("1301.TW", "台塑", "Materials", new BigDecimal("5.0")));
        FALLBACK_STOCKS.put("1303.TW", new StockInfo("1303.TW", "南亞", "Materials", new BigDecimal("4.5")));
        FALLBACK_STOCKS.put("1216.TW", new StockInfo("1216.TW", "統一", "Consumer Staples", new BigDecimal("3.8")));
        FALLBACK_STOCKS.put("2912.TW", new StockInfo("2912.TW", "統一超", "Consumer Staples", new BigDecimal("3.5")));
        FALLBACK_STOCKS.put("2454.TW", new StockInfo("2454.TW", "聯發科", "Technology", new BigDecimal("4.0")));
        FALLBACK_STOCKS.put("2303.TW", new StockInfo("2303.TW", "聯電", "Technology", new BigDecimal("6.5")));
        FALLBACK_STOCKS.put("3045.TW", new StockInfo("3045.TW", "台灣大", "Telecom", new BigDecimal("5.0")));
        FALLBACK_STOCKS.put("4904.TW", new StockInfo("4904.TW", "遠傳", "Telecom", new BigDecimal("5.2")));
        FALLBACK_STOCKS.put("2357.TW", new StockInfo("2357.TW", "華碩", "Technology", new BigDecimal("5.0")));
        FALLBACK_STOCKS.put("2382.TW", new StockInfo("2382.TW", "廣達", "Technology", new BigDecimal("4.5")));
    }
    
    /**
     * Get top N dividend stocks - dynamically fetched from Yahoo Finance
     * Sorted by dividend yield (highest first)
     */
    public List<StockInfo> getTopDividendStocks(int count) {
        List<StockInfo> stocks = fetchDividendStocksWithYield();
        return stocks.stream()
            .sorted((a, b) -> b.getDividendYield().compareTo(a.getDividendYield()))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all dividend stocks with yield data
     */
    public List<StockInfo> getAllDividendStocks() {
        return fetchDividendStocksWithYield();
    }
    
    /**
     * Fetch dividend stocks with real yield data from Yahoo Finance
     */
    private List<StockInfo> fetchDividendStocksWithYield() {
        // Check cache
        if (cachedDividendStocks != null && 
            (System.currentTimeMillis() - lastFetchTime) < CACHE_DURATION_MS) {
            logger.debug("Using cached dividend stock data");
            return cachedDividendStocks;
        }
        
        logger.info("Fetching dividend yields for {} candidate stocks", CANDIDATE_SYMBOLS.size());
        List<StockInfo> stocks = new ArrayList<>();
        int successCount = 0;
        
        for (String symbol : CANDIDATE_SYMBOLS) {
            try {
                StockInfo stockInfo = fetchStockInfo(symbol);
                if (stockInfo != null && stockInfo.getDividendYield().compareTo(BigDecimal.ZERO) > 0) {
                    stocks.add(stockInfo);
                    successCount++;
                }
            } catch (Exception e) {
                logger.debug("Failed to fetch data for {}: {}", symbol, e.getMessage());
            }
            
            // Rate limiting - don't hammer Yahoo Finance
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.info("Successfully fetched dividend data for {} stocks", successCount);
        
        // If we got less than 10 stocks, use fallback
        if (stocks.size() < 10) {
            logger.warn("Only got {} stocks from API, using fallback data", stocks.size());
            stocks = new ArrayList<>(FALLBACK_STOCKS.values());
        }
        
        // Update cache
        cachedDividendStocks = stocks;
        lastFetchTime = System.currentTimeMillis();
        
        return stocks;
    }
    
    /**
     * Fetch stock info including dividend yield from Yahoo Finance
     */
    private StockInfo fetchStockInfo(String symbol) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = String.format(
                "https://query1.finance.yahoo.com/v7/finance/quote?symbols=%s",
                symbol
            );
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode result = root.path("quoteResponse").path("result");
            
            if (result.isArray() && result.size() > 0) {
                JsonNode quote = result.get(0);
                
                String name = quote.path("shortName").asText(quote.path("longName").asText(symbol));
                String sector = determineSector(symbol);
                
                // Get dividend yield (trailingAnnualDividendYield is a ratio, multiply by 100 for percentage)
                BigDecimal dividendYield = BigDecimal.ZERO;
                if (quote.has("trailingAnnualDividendYield") && !quote.path("trailingAnnualDividendYield").isNull()) {
                    dividendYield = new BigDecimal(quote.path("trailingAnnualDividendYield").asText("0"))
                        .multiply(new BigDecimal("100"));
                } else if (quote.has("dividendYield") && !quote.path("dividendYield").isNull()) {
                    dividendYield = new BigDecimal(quote.path("dividendYield").asText("0"));
                }
                
                return new StockInfo(symbol, name, sector, dividendYield);
            }
        } catch (Exception e) {
            logger.debug("Failed to fetch stock info for {}: {}", symbol, e.getMessage());
        }
        
        // Return fallback if available
        return FALLBACK_STOCKS.get(symbol);
    }
    
    /**
     * Determine sector based on stock code pattern (Taiwan specific)
     */
    private String determineSector(String symbol) {
        String code = symbol.replace(".TW", "");
        try {
            int codeNum = Integer.parseInt(code);
            if (codeNum >= 1100 && codeNum < 1300) return "Materials";
            if (codeNum >= 1200 && codeNum < 1400) return "Consumer Staples";
            if (codeNum >= 1400 && codeNum < 1600) return "Materials";
            if (codeNum >= 1700 && codeNum < 1800) return "Materials";
            if (codeNum >= 2000 && codeNum < 2100) return "Materials";
            if (codeNum >= 2100 && codeNum < 2200) return "Consumer Discretionary";
            if (codeNum >= 2200 && codeNum < 2400) return "Consumer Discretionary";
            if (codeNum >= 2300 && codeNum < 2500) return "Technology";
            if (codeNum >= 2400 && codeNum < 2500) return "Telecom";
            if (codeNum >= 2500 && codeNum < 2700) return "Real Estate";
            if (codeNum >= 2600 && codeNum < 2700) return "Industrials";
            if (codeNum >= 2800 && codeNum < 3000) return "Financials";
            if (codeNum >= 3000 && codeNum < 4000) return "Technology";
            if (codeNum >= 4000 && codeNum < 5000) return "Telecom";
            if (codeNum >= 5800 && codeNum < 6000) return "Financials";
            if (codeNum >= 6000 && codeNum < 7000) return "Technology";
            if (codeNum >= 9900 && codeNum < 10000) return "Consumer Discretionary";
        } catch (NumberFormatException e) {
            // Ignore
        }
        return "Other";
    }
    
    /**
     * Verify if a stock has valid data on Yahoo Finance
     */
    public boolean verifyStockData(String symbol) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = String.format(
                "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=5d",
                symbol
            );
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode result = root.path("chart").path("result");
            return result.isArray() && result.size() > 0;
        } catch (Exception e) {
            logger.warn("Failed to verify stock data for {}: {}", symbol, e.getMessage());
            return false;
        }
    }
    
    /**
     * Force refresh the cache
     */
    public void refreshCache() {
        cachedDividendStocks = null;
        lastFetchTime = 0;
        fetchDividendStocksWithYield();
    }
    
    /**
     * Stock info container with dividend yield
     */
    public static class StockInfo {
        private final String symbol;
        private final String name;
        private final String sector;
        private final BigDecimal dividendYield;
        
        public StockInfo(String symbol, String name, String sector) {
            this(symbol, name, sector, BigDecimal.ZERO);
        }
        
        public StockInfo(String symbol, String name, String sector, BigDecimal dividendYield) {
            this.symbol = symbol;
            this.name = name;
            this.sector = sector;
            this.dividendYield = dividendYield != null ? dividendYield : BigDecimal.ZERO;
        }
        
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public String getSector() { return sector; }
        public BigDecimal getDividendYield() { return dividendYield; }
    }
}
