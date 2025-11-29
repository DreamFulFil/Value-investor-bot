package com.valueinvestor.controller;

import com.valueinvestor.model.dto.RebalanceResultDTO;
import com.valueinvestor.model.dto.TransactionDTO;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.TransactionLogRepository;
import com.valueinvestor.service.ProgressService;
import com.valueinvestor.service.RebalanceService;
import com.valueinvestor.service.TradingConfigService;
import com.valueinvestor.service.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trading")
@CrossOrigin(origins = "*")
public class TradingController {

    private static final Logger logger = LoggerFactory.getLogger(TradingController.class);

    @Autowired
    private RebalanceService rebalanceService;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private TransactionLogRepository transactionLogRepository;
    
    @Autowired
    private ProgressService progressService;

    @Autowired
    private TradingConfigService tradingConfigService;

    /**
     * GET /api/trading/rebalance/progress - SSE endpoint for real-time progress updates
     */
    @GetMapping(value = "/rebalance/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getRebalanceProgress() {
        logger.info("GET /api/trading/rebalance/progress - SSE connection established");
        return progressService.createEmitter();
    }

    /**
     * POST /api/trading/rebalance - Manually trigger rebalance
     */
    @PostMapping("/rebalance")
    public ResponseEntity<RebalanceResultDTO> triggerRebalance() {
        logger.info("POST /api/trading/rebalance - Manual trigger");

        try {
            RebalanceService.RebalanceResult result = rebalanceService.triggerRebalance();
            RebalanceResultDTO dto = convertToDTO(result);
            
            // Send completion event
            if (result.isSuccess()) {
                progressService.sendComplete("Portfolio updated successfully!");
            } else {
                progressService.sendError(result.getErrorMessage());
            }

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            logger.error("Manual rebalance failed", e);
            progressService.sendError("Rebalance failed: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/trading/transactions - Get transaction history
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        logger.info("GET /api/trading/transactions?startDate={}&endDate={}", startDate, endDate);

        try {
            List<TransactionLog> transactions;

            if (startDate != null && endDate != null) {
                transactions = transactionLogRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
            } else {
                transactions = transactionLogRepository.findAll();
            }

            List<TransactionDTO> dtos = transactions.stream()
                    .map(this::convertToTransactionDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            logger.error("Failed to get transactions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/trading/status - Get trading system status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTradingStatus() {
        logger.info("GET /api/trading/status");

        try {
            Map<String, Object> status = new HashMap<>();

            // Check Shioaji connection
            boolean shioajiConnected = tradingService.testShioajiConnection();
            status.put("shioajiConnected", shioajiConnected);

            // Get recent transaction count
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            List<TransactionLog> recentTransactions = transactionLogRepository
                    .findByTimestampBetweenOrderByTimestampDesc(last24Hours, LocalDateTime.now());
            status.put("transactionsLast24Hours", recentTransactions.size());

            // Get last rebalance info
            TransactionLog lastRebalance = transactionLogRepository.findLastRebalanceTransaction();
            if (lastRebalance != null) {
                status.put("lastRebalance", lastRebalance.getTimestamp());
            } else {
                status.put("lastRebalance", null);
            }

            status.put("ready", true);

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Failed to get trading status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/trading/transactions/symbol/{symbol} - Get transactions for a specific symbol
     */
    @GetMapping("/transactions/symbol/{symbol}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsBySymbol(@PathVariable String symbol) {
        logger.info("GET /api/trading/transactions/symbol/{}", symbol);

        try {
            List<TransactionLog> transactions = transactionLogRepository.findBySymbolOrderByTimestampDesc(symbol);
            List<TransactionDTO> dtos = transactions.stream()
                    .map(this::convertToTransactionDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            logger.error("Failed to get transactions for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/trading/go-live - Activate LIVE trading mode permanently.
     * This is a ONE-WAY operation - once activated, cannot be reverted.
     */
    @PostMapping("/go-live")
    public ResponseEntity<Map<String, Object>> goLive(@RequestBody GoLiveRequest request) {
        logger.warn("========================================");
        logger.warn("🚨 GO LIVE REQUEST RECEIVED");
        logger.warn("Option: {}, Amount: {}", request.option, request.amount);
        logger.warn("========================================");

        Map<String, Object> response = new HashMap<>();

        try {
            // Check if already live
            if (tradingConfigService.hasEverGoneLive()) {
                response.put("success", false);
                response.put("message", "LIVE mode is already active - cannot activate again");
                response.put("alreadyLive", true);
                return ResponseEntity.ok(response);
            }

            // Activate LIVE mode permanently
            boolean activated = tradingConfigService.activateLiveMode(request.option);

            if (activated) {
                // Create initial deposit if amount > 0
                if (request.amount != null && request.amount.compareTo(BigDecimal.ZERO) > 0) {
                    tradingService.createDeposit(request.amount, TransactionLog.TradingMode.LIVE, 
                        "Initial LIVE deposit - " + request.option);
                }

                response.put("success", true);
                response.put("message", "🔴 LIVE MODE ACTIVATED - Real money trading is now enabled!");
                response.put("goLiveDate", tradingConfigService.getGoLiveDate());
                response.put("option", request.option);
                
                logger.warn("✅ LIVE MODE SUCCESSFULLY ACTIVATED");
            } else {
                response.put("success", false);
                response.put("message", "Failed to activate LIVE mode");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to activate LIVE mode", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * GET /api/trading/live-status - Check if LIVE mode is active
     */
    @GetMapping("/live-status")
    public ResponseEntity<Map<String, Object>> getLiveStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("isLive", tradingConfigService.isLiveMode());
        status.put("hasEverGoneLive", tradingConfigService.hasEverGoneLive());
        status.put("goLiveDate", tradingConfigService.getGoLiveDate());
        status.put("goLiveOption", tradingConfigService.getGoLiveOption());
        
        return ResponseEntity.ok(status);
    }

    // Request class for go-live
    public static class GoLiveRequest {
        public String option; // fresh, gradual, oneshot
        public BigDecimal amount;
    }

    // Helper methods
    private RebalanceResultDTO convertToDTO(RebalanceService.RebalanceResult result) {
        RebalanceResultDTO dto = new RebalanceResultDTO();
        dto.setSuccess(result.isSuccess());
        dto.setStartTime(result.getStartTime());
        dto.setEndTime(result.getEndTime());
        dto.setMissedMonths(result.getMissedMonths());
        dto.setErrorMessage(result.getErrorMessage());
        dto.setMessage(result.getMessage());

        // Convert monthly results
        List<RebalanceResultDTO.MonthlyRebalanceDTO> monthlyDTOs = result.getMonthlyResults().stream()
                .map(mr -> {
                    RebalanceResultDTO.MonthlyRebalanceDTO monthlyDTO = new RebalanceResultDTO.MonthlyRebalanceDTO();
                    monthlyDTO.setRebalanceDate(mr.getRebalanceDate().toString());
                    monthlyDTO.setSelectedStocks(mr.getSelectedStocks());
                    monthlyDTO.setStocksPurchased(mr.getStocksPurchased());
                    monthlyDTO.setTotalInvested(mr.getTotalInvested() != null ? mr.getTotalInvested().toString() : "0.00");
                    return monthlyDTO;
                })
                .collect(Collectors.toList());

        dto.setMonthlyResults(monthlyDTOs);

        // Calculate total transactions
        int totalTransactions = result.getMonthlyResults().stream()
                .mapToInt(RebalanceService.MonthlyRebalanceResult::getStocksPurchased)
                .sum();
        dto.setTotalTransactions(totalTransactions);

        return dto;
    }

    private TransactionDTO convertToTransactionDTO(TransactionLog transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setTimestamp(transaction.getTimestamp());
        dto.setType(transaction.getType().name());
        dto.setSymbol(transaction.getSymbol());
        dto.setQuantity(transaction.getQuantity());
        dto.setPrice(transaction.getPrice());
        dto.setTotalAmount(transaction.getTotalAmount());
        dto.setMode(transaction.getMode().name());
        dto.setNotes(transaction.getNotes());
        return dto;
    }
}
