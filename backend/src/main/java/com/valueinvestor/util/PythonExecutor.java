package com.valueinvestor.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class PythonExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PythonExecutor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PYTHON_EXECUTABLE = "python3";
    private static final int TIMEOUT_SECONDS = 30;

    /**
     * Execute a Python script with arguments
     */
    public String executePython(String scriptPath, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(PYTHON_EXECUTABLE);
        command.add(scriptPath);

        for (String arg : args) {
            command.add(arg);
        }

        logger.info("Executing Python script: {} with args: {}", scriptPath, String.join(", ", args));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        // Set working directory if needed
        File scriptFile = new File(scriptPath);
        if (scriptFile.getParentFile() != null) {
            processBuilder.directory(scriptFile.getParentFile());
        }

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            process.destroy();
            throw new RuntimeException("Python script execution timed out after " + TIMEOUT_SECONDS + " seconds");
        }

        int exitCode = process.exitValue();

        if (exitCode != 0) {
            logger.error("Python script failed with exit code {}: {}", exitCode, output.toString());
            throw new RuntimeException("Python script failed with exit code " + exitCode + ": " + output.toString());
        }

        logger.info("Python script executed successfully");
        return output.toString().trim();
    }

    /**
     * Execute Shioaji order via Python bridge
     */
    public ShioajiOrderResult executeShioajiOrder(String action, String symbol,
                                                  BigDecimal quantity, BigDecimal price) throws Exception {
        // Path to the Shioaji bridge script
        String scriptPath = "/Users/gc/Downloads/work/US-stock/shioaji_bridge/execute_order.py";

        String[] args = {
            action,           // BUY or SELL
            symbol,           // Stock symbol
            quantity.toString(),
            price.toString()
        };

        logger.info("Executing Shioaji order: {} {} shares of {} at ${}",
                   action, quantity, symbol, price);

        String result = executePython(scriptPath, args);

        // Parse JSON response from Python
        return parseShioajiResponse(result);
    }

    /**
     * Parse Shioaji response from JSON
     */
    private ShioajiOrderResult parseShioajiResponse(String jsonResponse) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            ShioajiOrderResult result = new ShioajiOrderResult();
            result.setSuccess(root.path("success").asBoolean());
            result.setOrderId(root.path("order_id").asText());
            result.setMessage(root.path("message").asText());
            result.setStatus(root.path("status").asText());

            if (root.has("filled_quantity")) {
                result.setFilledQuantity(new BigDecimal(root.path("filled_quantity").asText()));
            }

            if (root.has("filled_price")) {
                result.setFilledPrice(new BigDecimal(root.path("filled_price").asText()));
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to parse Shioaji response: {}", jsonResponse, e);
            throw new RuntimeException("Failed to parse Shioaji response", e);
        }
    }

    /**
     * Test Shioaji connection
     */
    public boolean testShioajiConnection() {
        try {
            String scriptPath = "/Users/gc/Downloads/work/US-stock/shioaji_bridge/test_connection.py";
            String result = executePython(scriptPath);

            JsonNode root = objectMapper.readTree(result);
            return root.path("connected").asBoolean();

        } catch (Exception e) {
            logger.error("Failed to test Shioaji connection", e);
            return false;
        }
    }

    /**
     * Fetch current quote via Shioaji
     */
    public ShioajiQuoteResult fetchShioajiQuote(String symbol) throws Exception {
        String scriptPath = "/Users/gc/Downloads/work/US-stock/shioaji_bridge/fetch_quote.py";

        logger.info("Fetching Shioaji quote for: {}", symbol);

        String result = executePython(scriptPath, symbol);

        // Parse JSON response
        return parseShioajiQuoteResponse(result);
    }

    /**
     * Fetch historical data via Shioaji
     */
    public ShioajiHistoryResult fetchShioajiHistory(String symbol, String startDate, String endDate) throws Exception {
        String scriptPath = "/Users/gc/Downloads/work/US-stock/shioaji_bridge/fetch_history.py";

        logger.info("Fetching Shioaji history for: {} from {} to {}", symbol, startDate, endDate);

        String result = executePython(scriptPath, symbol, startDate, endDate);

        // Parse JSON response
        return parseShioajiHistoryResponse(result);
    }

    /**
     * Parse Shioaji quote response from JSON
     */
    private ShioajiQuoteResult parseShioajiQuoteResponse(String jsonResponse) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            ShioajiQuoteResult result = new ShioajiQuoteResult();
            result.setSuccess(root.path("success").asBoolean());
            result.setSymbol(root.path("symbol").asText());
            result.setError(root.path("error").asText(null));

            if (result.isSuccess() && root.has("price")) {
                result.setPrice(new BigDecimal(root.path("price").asText()));

                if (root.has("open") && !root.path("open").isNull()) {
                    result.setOpen(new BigDecimal(root.path("open").asText()));
                }
                if (root.has("high") && !root.path("high").isNull()) {
                    result.setHigh(new BigDecimal(root.path("high").asText()));
                }
                if (root.has("low") && !root.path("low").isNull()) {
                    result.setLow(new BigDecimal(root.path("low").asText()));
                }
                if (root.has("volume") && !root.path("volume").isNull()) {
                    result.setVolume(root.path("volume").asLong());
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to parse Shioaji quote response: {}", jsonResponse, e);
            throw new RuntimeException("Failed to parse Shioaji quote response", e);
        }
    }

    /**
     * Parse Shioaji history response from JSON
     */
    private ShioajiHistoryResult parseShioajiHistoryResponse(String jsonResponse) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            ShioajiHistoryResult result = new ShioajiHistoryResult();
            result.setSuccess(root.path("success").asBoolean());
            result.setSymbol(root.path("symbol").asText());
            result.setError(root.path("error").asText(null));

            if (result.isSuccess() && root.has("prices")) {
                List<PriceBar> prices = new ArrayList<>();
                JsonNode pricesNode = root.path("prices");

                for (JsonNode priceNode : pricesNode) {
                    PriceBar bar = new PriceBar();
                    bar.setDate(priceNode.path("date").asText());
                    bar.setOpen(new BigDecimal(priceNode.path("open").asText()));
                    bar.setHigh(new BigDecimal(priceNode.path("high").asText()));
                    bar.setLow(new BigDecimal(priceNode.path("low").asText()));
                    bar.setClose(new BigDecimal(priceNode.path("close").asText()));
                    bar.setVolume(priceNode.path("volume").asLong());
                    bar.setAdjustedClose(new BigDecimal(priceNode.path("adjusted_close").asText()));
                    prices.add(bar);
                }

                result.setPrices(prices);
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to parse Shioaji history response: {}", jsonResponse, e);
            throw new RuntimeException("Failed to parse Shioaji history response", e);
        }
    }

    /**
     * Result object for Shioaji orders
     */
    public static class ShioajiOrderResult {
        private boolean success;
        private String orderId;
        private String message;
        private String status;
        private BigDecimal filledQuantity;
        private BigDecimal filledPrice;

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getFilledQuantity() {
            return filledQuantity;
        }

        public void setFilledQuantity(BigDecimal filledQuantity) {
            this.filledQuantity = filledQuantity;
        }

        public BigDecimal getFilledPrice() {
            return filledPrice;
        }

        public void setFilledPrice(BigDecimal filledPrice) {
            this.filledPrice = filledPrice;
        }

        @Override
        public String toString() {
            return "ShioajiOrderResult{" +
                   "success=" + success +
                   ", orderId='" + orderId + '\'' +
                   ", message='" + message + '\'' +
                   ", status='" + status + '\'' +
                   ", filledQuantity=" + filledQuantity +
                   ", filledPrice=" + filledPrice +
                   '}';
        }
    }

    /**
     * Result object for Shioaji quote fetching
     */
    public static class ShioajiQuoteResult {
        private boolean success;
        private String symbol;
        private String error;
        private BigDecimal price;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private Long volume;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getOpen() { return open; }
        public void setOpen(BigDecimal open) { this.open = open; }
        public BigDecimal getHigh() { return high; }
        public void setHigh(BigDecimal high) { this.high = high; }
        public BigDecimal getLow() { return low; }
        public void setLow(BigDecimal low) { this.low = low; }
        public Long getVolume() { return volume; }
        public void setVolume(Long volume) { this.volume = volume; }
    }

    /**
     * Result object for Shioaji historical data fetching
     */
    public static class ShioajiHistoryResult {
        private boolean success;
        private String symbol;
        private String error;
        private List<PriceBar> prices;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public List<PriceBar> getPrices() { return prices; }
        public void setPrices(List<PriceBar> prices) { this.prices = prices; }
    }

    /**
     * Price bar data
     */
    public static class PriceBar {
        private String date;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
        private BigDecimal adjustedClose;

        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public BigDecimal getOpen() { return open; }
        public void setOpen(BigDecimal open) { this.open = open; }
        public BigDecimal getHigh() { return high; }
        public void setHigh(BigDecimal high) { this.high = high; }
        public BigDecimal getLow() { return low; }
        public void setLow(BigDecimal low) { this.low = low; }
        public BigDecimal getClose() { return close; }
        public void setClose(BigDecimal close) { this.close = close; }
        public Long getVolume() { return volume; }
        public void setVolume(Long volume) { this.volume = volume; }
        public BigDecimal getAdjustedClose() { return adjustedClose; }
        public void setAdjustedClose(BigDecimal adjustedClose) { this.adjustedClose = adjustedClose; }
    }
}
