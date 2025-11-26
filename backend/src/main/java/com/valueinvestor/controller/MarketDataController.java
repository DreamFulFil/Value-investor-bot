package com.valueinvestor.controller;

import com.valueinvestor.model.dto.QuoteDTO;
import com.valueinvestor.model.entity.StockFundamentals;
import com.valueinvestor.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);

    @Autowired
    private MarketDataService marketDataService;

    /**
     * GET /api/market/quote/{symbol} - Get quote for a stock
     */
    @GetMapping("/quote/{symbol}")
    public ResponseEntity<QuoteDTO> getQuote(@PathVariable String symbol) {
        logger.info("GET /api/market/quote/{}", symbol);

        try {
            StockFundamentals fundamentals = marketDataService.getFundamentals(symbol);

            if (fundamentals == null) {
                return ResponseEntity.notFound().build();
            }

            QuoteDTO dto = convertToQuoteDTO(fundamentals);
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            logger.error("Failed to get quote for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/market/search - Search stocks by name or symbol
     */
    @GetMapping("/search")
    public ResponseEntity<List<QuoteDTO>> searchStocks(@RequestParam String query) {
        logger.info("GET /api/market/search?query={}", query);

        try {
            List<StockFundamentals> results = marketDataService.searchStocks(query);

            List<QuoteDTO> dtos = results.stream()
                    .map(this::convertToQuoteDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            logger.error("Failed to search stocks", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/market/top-dividends - Get top dividend stocks
     */
    @GetMapping("/top-dividends")
    public ResponseEntity<List<QuoteDTO>> getTopDividendStocks(
            @RequestParam(defaultValue = "20") int limit) {

        logger.info("GET /api/market/top-dividends?limit={}", limit);

        try {
            List<StockFundamentals> stocks = marketDataService.getTopDividendStocks(limit);

            List<QuoteDTO> dtos = stocks.stream()
                    .map(this::convertToQuoteDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            logger.error("Failed to get top dividend stocks", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/market/dividend-yield - Get stocks with minimum dividend yield
     */
    @GetMapping("/dividend-yield")
    public ResponseEntity<List<QuoteDTO>> getStocksByDividendYield(
            @RequestParam BigDecimal minYield) {

        logger.info("GET /api/market/dividend-yield?minYield={}", minYield);

        try {
            List<StockFundamentals> stocks = marketDataService.getStocksByMinDividendYield(minYield);

            List<QuoteDTO> dtos = stocks.stream()
                    .map(this::convertToQuoteDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            logger.error("Failed to get stocks by dividend yield", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/market/validate/{symbol} - Validate if symbol exists
     */
    @GetMapping("/validate/{symbol}")
    public ResponseEntity<ValidationResponse> validateSymbol(@PathVariable String symbol) {
        logger.info("GET /api/market/validate/{}", symbol);

        try {
            boolean isValid = marketDataService.isValidSymbol(symbol);

            ValidationResponse response = new ValidationResponse();
            response.setSymbol(symbol);
            response.setValid(isValid);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to validate symbol {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/market/refresh - Manually refresh stale data
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshData() {
        logger.info("POST /api/market/refresh - Manual trigger");

        try {
            marketDataService.refreshStaleData();
            return ResponseEntity.ok("Data refresh completed");

        } catch (Exception e) {
            logger.error("Failed to refresh market data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods
    private QuoteDTO convertToQuoteDTO(StockFundamentals fundamentals) {
        QuoteDTO dto = new QuoteDTO();
        dto.setSymbol(fundamentals.getSymbol());
        dto.setName(fundamentals.getName());
        dto.setPrice(fundamentals.getCurrentPrice());
        dto.setDividendYield(fundamentals.getDividendYield());
        dto.setPeRatio(fundamentals.getPeRatio());
        dto.setPbRatio(fundamentals.getPbRatio());
        dto.setMarketCap(fundamentals.getMarketCap());
        dto.setMarket(fundamentals.getMarket());
        return dto;
    }

    // Inner class for validation response
    public static class ValidationResponse {
        private String symbol;
        private boolean valid;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }
}
