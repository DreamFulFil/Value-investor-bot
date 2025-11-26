package com.valueinvestor.controller;

import com.valueinvestor.model.dto.RebalanceResultDTO;
import com.valueinvestor.model.dto.TransactionDTO;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.TransactionLogRepository;
import com.valueinvestor.service.RebalanceService;
import com.valueinvestor.service.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * POST /api/trading/rebalance - Manually trigger rebalance
     */
    @PostMapping("/rebalance")
    public ResponseEntity<RebalanceResultDTO> triggerRebalance() {
        logger.info("POST /api/trading/rebalance - Manual trigger");

        try {
            RebalanceService.RebalanceResult result = rebalanceService.triggerRebalance();
            RebalanceResultDTO dto = convertToDTO(result);

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            logger.error("Manual rebalance failed", e);
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

    // Helper methods
    private RebalanceResultDTO convertToDTO(RebalanceService.RebalanceResult result) {
        RebalanceResultDTO dto = new RebalanceResultDTO();
        dto.setSuccess(result.isSuccess());
        dto.setStartTime(result.getStartTime());
        dto.setEndTime(result.getEndTime());
        dto.setMissedMonths(result.getMissedMonths());
        dto.setErrorMessage(result.getErrorMessage());

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
