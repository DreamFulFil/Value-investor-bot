package com.valueinvestor.service;

import com.valueinvestor.model.entity.PositionHistory;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.PositionHistoryRepository;
import com.valueinvestor.repository.TransactionLogRepository;
import com.valueinvestor.util.PythonExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class TradingService {

    private static final Logger logger = LoggerFactory.getLogger(TradingService.class);

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private PositionHistoryRepository positionHistoryRepository;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private PythonExecutor pythonExecutor;

    // Track partial fill information for recovery
    private volatile String lastOrderError = null;
    private volatile boolean lastOrderPartiallyFilled = false;

    /**
     * Execute a buy order
     */
    @Transactional
    public TransactionLog executeBuy(String symbol, BigDecimal quantity, TransactionLog.TradingMode mode) {
        return executeBuy(symbol, quantity, mode, null);
    }

    /**
     * Execute a buy order with specific price
     */
    @Transactional
    public TransactionLog executeBuy(String symbol, BigDecimal quantity,
                                    TransactionLog.TradingMode mode, BigDecimal price) {
        logger.info("Executing BUY order: {} shares of {} in {} mode", quantity, symbol, mode);

        try {
            // Get current price if not provided
            if (price == null) {
                price = marketDataService.getQuote(symbol);
            }

            BigDecimal totalAmount = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);

            // Execute order based on mode
            if (mode == TransactionLog.TradingMode.LIVE) {
                // Execute via Shioaji with retry logic
                PythonExecutor.ShioajiOrderResult result = executeShioajiOrderWithRetry(
                        "BUY", symbol, quantity, price);

                if (!result.isSuccess()) {
                    lastOrderError = result.getMessage();
                    throw new LiveOrderException("Shioaji order failed: " + result.getMessage());
                }

                // Update quantity and price with filled values
                if (result.getFilledQuantity() != null) {
                    quantity = result.getFilledQuantity();
                    if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                        lastOrderPartiallyFilled = true;
                        throw new LiveOrderException("Order not filled - will retry");
                    }
                }
                if (result.getFilledPrice() != null) {
                    price = result.getFilledPrice();
                    totalAmount = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
                }
                
                lastOrderError = null;
                lastOrderPartiallyFilled = false;
            }

            // Log transaction
            TransactionLog transaction = new TransactionLog(
                    TransactionLog.TransactionType.BUY,
                    symbol,
                    quantity,
                    price,
                    totalAmount,
                    mode,
                    mode == TransactionLog.TradingMode.LIVE ? "Live order executed" : "Simulated order"
            );

            transaction = transactionLogRepository.save(transaction);

            // Update position
            updatePosition(symbol, quantity, price, true);

            logger.info("BUY order completed: {} shares of {} at ${}", quantity, symbol, price);
            return transaction;

        } catch (Exception e) {
            logger.error("Failed to execute BUY order for {}", symbol, e);
            throw new RuntimeException("Failed to execute BUY order: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a sell order
     */
    @Transactional
    public TransactionLog executeSell(String symbol, BigDecimal quantity, TransactionLog.TradingMode mode) {
        return executeSell(symbol, quantity, mode, null);
    }

    /**
     * Execute a sell order with specific price
     */
    @Transactional
    public TransactionLog executeSell(String symbol, BigDecimal quantity,
                                     TransactionLog.TradingMode mode, BigDecimal price) {
        logger.info("Executing SELL order: {} shares of {} in {} mode", quantity, symbol, mode);

        try {
            // Get current price if not provided
            if (price == null) {
                price = marketDataService.getQuote(symbol);
            }

            BigDecimal totalAmount = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);

            // Execute order based on mode
            if (mode == TransactionLog.TradingMode.LIVE) {
                // Execute via Shioaji with retry logic
                PythonExecutor.ShioajiOrderResult result = executeShioajiOrderWithRetry(
                        "SELL", symbol, quantity, price);

                if (!result.isSuccess()) {
                    lastOrderError = result.getMessage();
                    throw new LiveOrderException("Shioaji order failed: " + result.getMessage());
                }

                // Update quantity and price with filled values
                if (result.getFilledQuantity() != null) {
                    quantity = result.getFilledQuantity();
                }
                if (result.getFilledPrice() != null) {
                    price = result.getFilledPrice();
                    totalAmount = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
                }
                
                lastOrderError = null;
            }

            // Log transaction
            TransactionLog transaction = new TransactionLog(
                    TransactionLog.TransactionType.SELL,
                    symbol,
                    quantity,
                    price,
                    totalAmount,
                    mode,
                    mode == TransactionLog.TradingMode.LIVE ? "Live order executed" : "Simulated order"
            );

            transaction = transactionLogRepository.save(transaction);

            // Update position
            updatePosition(symbol, quantity, price, false);

            logger.info("SELL order completed: {} shares of {} at ${}", quantity, symbol, price);
            return transaction;

        } catch (Exception e) {
            logger.error("Failed to execute SELL order for {}", symbol, e);
            throw new RuntimeException("Failed to execute SELL order: " + e.getMessage(), e);
        }
    }

    /**
     * Update position after trade
     */
    private void updatePosition(String symbol, BigDecimal quantity, BigDecimal price, boolean isBuy) {
        Optional<PositionHistory> latestPosition = positionHistoryRepository.findLatestPositionBySymbol(symbol);

        BigDecimal newQuantity;
        BigDecimal newAveragePrice;

        if (latestPosition.isPresent() && latestPosition.get().getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            PositionHistory existing = latestPosition.get();

            if (isBuy) {
                // Calculate new average price for buy
                BigDecimal totalCost = existing.getQuantity()
                        .multiply(existing.getAveragePrice())
                        .add(quantity.multiply(price));

                newQuantity = existing.getQuantity().add(quantity);
                newAveragePrice = totalCost.divide(newQuantity, 4, RoundingMode.HALF_UP);
            } else {
                // Reduce quantity for sell
                newQuantity = existing.getQuantity().subtract(quantity);
                newAveragePrice = existing.getAveragePrice(); // Keep same average price
            }
        } else {
            // New position
            if (isBuy) {
                newQuantity = quantity;
                newAveragePrice = price;
            } else {
                logger.warn("Attempting to sell {} without existing position", symbol);
                newQuantity = quantity.negate();
                newAveragePrice = price;
            }
        }

        // Create new position record
        PositionHistory newPosition = new PositionHistory(symbol, newQuantity, newAveragePrice);
        newPosition.setCurrentPrice(price);
        newPosition.calculateMetrics();

        positionHistoryRepository.save(newPosition);
        logger.info("Updated position for {}: {} shares at avg price ${}", symbol, newQuantity, newAveragePrice);
    }

    /**
     * Calculate shares to buy with given amount
     */
    public BigDecimal calculateSharesToBuy(String symbol, BigDecimal amount) {
        BigDecimal price = marketDataService.getQuote(symbol);

        if (price.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Cannot get valid price for " + symbol);
        }

        return amount.divide(price, 8, RoundingMode.DOWN);
    }

    /**
     * Test Shioaji connection
     */
    public boolean testShioajiConnection() {
        return pythonExecutor.testShioajiConnection();
    }

    /**
     * Get current position for a symbol
     */
    public Optional<PositionHistory> getCurrentPosition(String symbol) {
        return positionHistoryRepository.findLatestPositionBySymbol(symbol);
    }
    
    /**
     * Create a deposit transaction (for simulation/backtest mode)
     */
    @Transactional
    public TransactionLog createDeposit(BigDecimal amount, TransactionLog.TradingMode mode, String notes) {
        logger.info("Creating deposit: NT${} in {} mode", amount, mode);
        
        TransactionLog deposit = new TransactionLog(
                TransactionLog.TransactionType.DEPOSIT,
                null, // No symbol for deposits
                null, // No quantity for deposits
                null, // No price for deposits
                amount,
                mode,
                notes != null ? notes : "Cash deposit"
        );
        
        return transactionLogRepository.save(deposit);
    }

    /**
     * Execute Shioaji order with retry logic.
     * 3 attempts with 2s exponential backoff.
     */
    @Retryable(
        retryFor = { LiveOrderException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public PythonExecutor.ShioajiOrderResult executeShioajiOrderWithRetry(
            String action, String symbol, BigDecimal quantity, BigDecimal price) throws Exception {
        
        logger.info("🔄 Attempting {} order for {} shares of {} at NT${}", action, quantity, symbol, price);
        
        try {
            PythonExecutor.ShioajiOrderResult result = pythonExecutor.executeShioajiOrder(
                    action, symbol, quantity, price);
            
            if (result.isSuccess()) {
                logger.info("✅ {} order succeeded for {}", action, symbol);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.warn("⚠️ {} order attempt failed for {}: {} - will retry...", action, symbol, e.getMessage());
            throw e;
        }
    }

    /**
     * Recovery method called after all retries exhausted.
     */
    @Recover
    public PythonExecutor.ShioajiOrderResult recoverFromOrderFailure(
            Exception e, String action, String symbol, BigDecimal quantity, BigDecimal price) {
        
        logger.error("❌ FINAL FAILURE: {} order for {} failed after 3 attempts: {}", action, symbol, e.getMessage());
        lastOrderError = "Order failed after 3 retries: " + e.getMessage();
        
        // Return a failure result
        PythonExecutor.ShioajiOrderResult failResult = new PythonExecutor.ShioajiOrderResult();
        failResult.setSuccess(false);
        failResult.setMessage("Order failed after 3 retries: " + e.getMessage());
        return failResult;
    }

    /**
     * Get the last order error message (for UI toast).
     */
    public String getLastOrderError() {
        return lastOrderError;
    }

    /**
     * Check if last order was partially filled.
     */
    public boolean wasLastOrderPartiallyFilled() {
        return lastOrderPartiallyFilled;
    }

    /**
     * Custom exception for live order failures that should trigger retry.
     */
    public static class LiveOrderException extends RuntimeException {
        public LiveOrderException(String message) {
            super(message);
        }
        
        public LiveOrderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
