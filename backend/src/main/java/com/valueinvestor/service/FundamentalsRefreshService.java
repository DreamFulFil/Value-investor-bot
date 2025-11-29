package com.valueinvestor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valueinvestor.config.ShioajiProperties;
import com.valueinvestor.model.entity.StockFundamentals;
import com.valueinvestor.repository.StockFundamentalsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service to refresh stock fundamentals data from Shioaji bridge.
 * Runs daily at 15:00 Taiwan time (after market close).
 * Fetches: dividend yield, P/E, P/B, ROE, EPS, market cap.
 */
@Service
public class FundamentalsRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(FundamentalsRefreshService.class);

    @Autowired
    private StockFundamentalsRepository fundamentalsRepository;

    @Autowired
    private ShioajiProperties shioajiProperties;

    @Autowired
    private TaiwanStockScreenerService screeningService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Taiwan high-dividend stock candidates to track fundamentals for.
     */
    private static final List<String> CANDIDATE_SYMBOLS = Arrays.asList(
        // Financial sector (high dividend)
        "2886.TW", "2884.TW", "2881.TW", "2882.TW", "2891.TW", "5880.TW", "2883.TW", "2887.TW", "2888.TW", "2890.TW",
        // Telecom (stable dividend)
        "2412.TW", "3045.TW", "4904.TW",
        // Technology (growth + dividend)
        "2330.TW", "2317.TW", "2454.TW", "2303.TW", "2357.TW", "2382.TW", "3034.TW",
        // Materials & Industrial
        "2002.TW", "1301.TW", "1303.TW", "1326.TW", "1101.TW", "1102.TW",
        // Consumer
        "1216.TW", "2912.TW", "1227.TW",
        // Others
        "2603.TW", "2609.TW", "6505.TW", "9910.TW"
    );

    /**
     * Scheduled task: Refresh fundamentals daily at 15:00 Taiwan time.
     * Cron: second minute hour day-of-month month day-of-week
     * 15:00 = after Taiwan market closes at 13:30
     */
    @Scheduled(cron = "0 0 15 * * ?", zone = "Asia/Taipei")
    @Transactional
    public void refreshFundamentalsScheduled() {
        logger.info("=== Starting scheduled fundamentals refresh ===");
        refreshAllFundamentals();
        logger.info("=== Scheduled fundamentals refresh completed ===");
    }

    /**
     * Refresh fundamentals for all candidate stocks.
     */
    @Transactional
    public int refreshAllFundamentals() {
        logger.info("Refreshing fundamentals for {} candidate stocks", CANDIDATE_SYMBOLS.size());
        
        int successCount = 0;
        int failCount = 0;

        for (String symbol : CANDIDATE_SYMBOLS) {
            try {
                boolean success = refreshFundamentalsForSymbol(symbol);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
                
                // Rate limit: 200ms between calls
                Thread.sleep(200);
                
            } catch (Exception e) {
                logger.error("Failed to refresh fundamentals for {}: {}", symbol, e.getMessage());
                failCount++;
            }
        }

        logger.info("Fundamentals refresh completed: {} success, {} failed", successCount, failCount);
        return successCount;
    }

    /**
     * Refresh fundamentals for a single symbol from Shioaji bridge.
     */
    @Transactional
    public boolean refreshFundamentalsForSymbol(String symbol) {
        try {
            String apiUrl = shioajiProperties.getApiUrl();
            String url = String.format("%s/fundamentals/%s", apiUrl, symbol.replace(".TW", ""));
            
            logger.debug("Fetching fundamentals from: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.warn("Failed to fetch fundamentals for {} - HTTP {}", symbol, response.getStatusCode());
                return refreshFromYahooFinanceFallback(symbol);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            
            if (!root.path("success").asBoolean(false)) {
                logger.warn("Shioaji returned error for {}: {}", symbol, root.path("error").asText());
                return refreshFromYahooFinanceFallback(symbol);
            }

            // Parse and save fundamentals
            StockFundamentals fundamentals = fundamentalsRepository.findBySymbol(symbol)
                    .orElse(new StockFundamentals(symbol, root.path("name").asText(symbol)));

            fundamentals.setName(root.path("name").asText(fundamentals.getName()));
            fundamentals.setSector(root.path("sector").asText(null));
            fundamentals.setMarket("TW");
            fundamentals.setLastUpdated(LocalDateTime.now());

            if (root.has("dividendYield") && !root.path("dividendYield").isNull()) {
                fundamentals.setDividendYield(new BigDecimal(root.path("dividendYield").asText("0")));
            }
            if (root.has("peRatio") && !root.path("peRatio").isNull()) {
                fundamentals.setPeRatio(new BigDecimal(root.path("peRatio").asText("0")));
            }
            if (root.has("pbRatio") && !root.path("pbRatio").isNull()) {
                fundamentals.setPbRatio(new BigDecimal(root.path("pbRatio").asText("0")));
            }
            if (root.has("roe") && !root.path("roe").isNull()) {
                fundamentals.setRoe(new BigDecimal(root.path("roe").asText("0")));
            }
            if (root.has("marketCap") && !root.path("marketCap").isNull()) {
                fundamentals.setMarketCap(new BigDecimal(root.path("marketCap").asText("0")));
            }
            if (root.has("currentPrice") && !root.path("currentPrice").isNull()) {
                fundamentals.setCurrentPrice(new BigDecimal(root.path("currentPrice").asText("0")));
            }

            fundamentalsRepository.save(fundamentals);
            logger.info("Updated fundamentals for {}: yield={}%, P/E={}, P/B={}, ROE={}%", 
                symbol, fundamentals.getDividendYield(), fundamentals.getPeRatio(), 
                fundamentals.getPbRatio(), fundamentals.getRoe());
            
            return true;

        } catch (Exception e) {
            logger.error("Error refreshing fundamentals for {}: {}", symbol, e.getMessage());
            return refreshFromYahooFinanceFallback(symbol);
        }
    }

    /**
     * Fallback: Use TaiwanStockScreenerService (Yahoo Finance) for fundamentals.
     */
    private boolean refreshFromYahooFinanceFallback(String symbol) {
        try {
            logger.info("Falling back to Yahoo Finance for {} fundamentals", symbol);
            
            TaiwanStockScreenerService.StockInfo stockInfo = null;
            for (TaiwanStockScreenerService.StockInfo info : screeningService.getAllDividendStocks()) {
                if (info.getSymbol().equals(symbol)) {
                    stockInfo = info;
                    break;
                }
            }
            
            if (stockInfo == null) {
                logger.warn("No fallback data available for {}", symbol);
                return false;
            }

            StockFundamentals fundamentals = fundamentalsRepository.findBySymbol(symbol)
                    .orElse(new StockFundamentals(symbol, stockInfo.getName()));

            fundamentals.setName(stockInfo.getName());
            fundamentals.setSector(stockInfo.getSector());
            fundamentals.setDividendYield(stockInfo.getDividendYield());
            fundamentals.setMarket("TW");
            fundamentals.setLastUpdated(LocalDateTime.now());

            fundamentalsRepository.save(fundamentals);
            logger.info("Updated fundamentals from Yahoo fallback for {}: yield={}%", 
                symbol, fundamentals.getDividendYield());
            
            return true;

        } catch (Exception e) {
            logger.error("Yahoo fallback also failed for {}: {}", symbol, e.getMessage());
            return false;
        }
    }

    /**
     * Get top stocks ranked by dividend yield descending, then P/E ascending.
     * This is the REAL ranking logic - no alphabetical fallback.
     */
    public List<StockFundamentals> getTopRankedStocks(int limit) {
        return fundamentalsRepository.findAll().stream()
                .filter(f -> f.getMarket() != null && f.getMarket().equals("TW"))
                .filter(f -> f.getDividendYield() != null && f.getDividendYield().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> {
                    // Primary: dividend yield descending
                    int yieldCompare = nullSafeCompare(b.getDividendYield(), a.getDividendYield());
                    if (yieldCompare != 0) return yieldCompare;
                    
                    // Secondary: P/E ratio ascending (lower is better)
                    return nullSafeCompare(a.getPeRatio(), b.getPeRatio());
                })
                .limit(limit)
                .toList();
    }

    private int nullSafeCompare(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;  // nulls last
        if (b == null) return -1;
        return a.compareTo(b);
    }
}
