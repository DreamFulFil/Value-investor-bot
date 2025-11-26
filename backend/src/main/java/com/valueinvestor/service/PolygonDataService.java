package com.valueinvestor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valueinvestor.model.dto.QuoteDTO;
import com.valueinvestor.model.entity.StockPriceHistory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for fetching stock data from Polygon.io API
 * Free tier: 5 API calls per minute
 * Documentation: https://polygon.io/docs/stocks/getting-started
 */
@Service
public class PolygonDataService {

    private static final Logger logger = LoggerFactory.getLogger(PolygonDataService.class);
    private static final String BASE_URL = "https://api.polygon.io/v2";

    @Value("${polygon.api-key:demo}")
    private String apiKey;

    @Value("${polygon.base-url:https://api.polygon.io/v2}")
    private String baseUrl;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public PolygonDataService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get current quote for a stock
     * API: GET /v2/aggs/ticker/{ticker}/prev
     */
    public QuoteDTO getQuote(String symbol) {
        try {
            String url = String.format("%s/aggs/ticker/%s/prev?apiKey=%s",
                    baseUrl, symbol.toUpperCase(), apiKey);

            logger.debug("Fetching quote from Polygon.io: {}", symbol);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Polygon.io API error: {} - {}", response.code(), response.message());
                    return null;
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                if (!"OK".equals(root.path("status").asText())) {
                    logger.warn("Polygon.io returned non-OK status for {}", symbol);
                    return null;
                }

                JsonNode results = root.path("results");
                if (results.isArray() && results.size() > 0) {
                    JsonNode data = results.get(0);

                    QuoteDTO quote = new QuoteDTO();
                    quote.setSymbol(symbol);
                    quote.setPrice(BigDecimal.valueOf(data.path("c").asDouble()));
                    quote.setMarket("US");

                    logger.info("Polygon.io quote for {}: ${}", symbol, quote.getPrice());
                    return quote;
                }

            }
        } catch (IOException e) {
            logger.error("Error fetching quote from Polygon.io for {}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching quote from Polygon.io for {}: {}", symbol, e.getMessage());
        }
        return null;
    }

    /**
     * Get historical prices for a stock
     * API: GET /v2/aggs/ticker/{ticker}/range/{multiplier}/{timespan}/{from}/{to}
     */
    public List<StockPriceHistory> getHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        List<StockPriceHistory> prices = new ArrayList<>();

        try {
            String from = startDate.toString();
            String to = endDate.toString();
            String url = String.format("%s/aggs/ticker/%s/range/1/day/%s/%s?adjusted=true&sort=asc&limit=50000&apiKey=%s",
                    baseUrl, symbol.toUpperCase(), from, to, apiKey);

            logger.debug("Fetching historical data from Polygon.io: {} ({} to {})", symbol, from, to);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Polygon.io API error: {} - {}", response.code(), response.message());
                    return prices;
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                if (!"OK".equals(root.path("status").asText())) {
                    logger.warn("Polygon.io returned non-OK status for {}", symbol);
                    return prices;
                }

                JsonNode results = root.path("results");
                if (results.isArray()) {
                    for (JsonNode bar : results) {
                        try {
                            long timestamp = bar.path("t").asLong();
                            LocalDate date = Instant.ofEpochMilli(timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();

                            StockPriceHistory price = new StockPriceHistory();
                            price.setSymbol(symbol);
                            price.setDate(date);
                            price.setOpen(BigDecimal.valueOf(bar.path("o").asDouble()));
                            price.setHigh(BigDecimal.valueOf(bar.path("h").asDouble()));
                            price.setLow(BigDecimal.valueOf(bar.path("l").asDouble()));
                            price.setClose(BigDecimal.valueOf(bar.path("c").asDouble()));
                            price.setVolume(bar.path("v").asLong());
                            price.setAdjustedClose(BigDecimal.valueOf(bar.path("c").asDouble()));
                            price.setMarket("US");

                            prices.add(price);
                        } catch (Exception e) {
                            logger.error("Error parsing price data: {}", e.getMessage());
                        }
                    }

                    logger.info("Polygon.io returned {} historical prices for {}", prices.size(), symbol);
                }
            }
        } catch (IOException e) {
            logger.error("Error fetching historical data from Polygon.io for {}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching historical data from Polygon.io for {}: {}", symbol, e.getMessage());
        }

        return prices;
    }

    /**
     * Get fundamentals/details for a stock
     * API: GET /v3/reference/tickers/{ticker}
     */
    public QuoteDTO getFundamentals(String symbol) {
        try {
            String url = String.format("https://api.polygon.io/v3/reference/tickers/%s?apiKey=%s",
                    symbol.toUpperCase(), apiKey);

            logger.debug("Fetching fundamentals from Polygon.io: {}", symbol);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Polygon.io API error: {} - {}", response.code(), response.message());
                    return null;
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                if (!"OK".equals(root.path("status").asText())) {
                    logger.warn("Polygon.io returned non-OK status for {}", symbol);
                    return null;
                }

                JsonNode results = root.path("results");
                if (!results.isMissingNode()) {
                    QuoteDTO quote = new QuoteDTO();
                    quote.setSymbol(symbol);
                    quote.setName(results.path("name").asText());
                    quote.setMarket("US");

                    // Market cap if available
                    if (results.has("market_cap")) {
                        quote.setMarketCap(BigDecimal.valueOf(results.path("market_cap").asDouble()));
                    }

                    logger.info("Polygon.io fundamentals for {}: {}", symbol, quote.getName());
                    return quote;
                }
            }
        } catch (IOException e) {
            logger.error("Error fetching fundamentals from Polygon.io for {}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching fundamentals from Polygon.io for {}: {}", symbol, e.getMessage());
        }
        return null;
    }

    /**
     * Check if Polygon.io API is configured and accessible
     */
    public boolean isConfigured() {
        if (apiKey == null || apiKey.isEmpty() || "demo".equals(apiKey)) {
            logger.warn("Polygon.io API key not configured");
            return false;
        }
        return true;
    }

    /**
     * Test API connectivity
     */
    public boolean testConnection() {
        try {
            String url = String.format("https://api.polygon.io/v3/reference/tickers?limit=1&apiKey=%s", apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                boolean success = response.isSuccessful();
                if (success) {
                    logger.info("Polygon.io API connection test successful");
                } else {
                    logger.error("Polygon.io API connection test failed: {}", response.code());
                }
                return success;
            }
        } catch (Exception e) {
            logger.error("Polygon.io API connection test failed: {}", e.getMessage());
            return false;
        }
    }
}
