package com.valueinvestor.controller;

import com.valueinvestor.model.dto.*;
import com.valueinvestor.model.entity.PortfolioSnapshot;
import com.valueinvestor.model.entity.PositionHistory;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.service.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);

    @Autowired
    private PortfolioService portfolioService;

    /**
     * GET /api/portfolio/current - Get current portfolio
     */
    @GetMapping("/current")
    public ResponseEntity<PortfolioSummaryDTO> getCurrentPortfolio() {
        logger.info("GET /api/portfolio/current");

        try {
            Map<String, Object> metrics = portfolioService.getPerformanceMetrics();
            List<PositionHistory> positions = portfolioService.getCurrentPortfolio();

            PortfolioSummaryDTO summary = new PortfolioSummaryDTO();
            summary.setTotalValue((BigDecimal) metrics.get("totalValue"));
            summary.setCashBalance((BigDecimal) metrics.get("cashBalance"));
            summary.setInvestedAmount((BigDecimal) metrics.get("investedAmount"));
            summary.setTotalPL((BigDecimal) metrics.get("totalPL"));
            summary.setPlPercentage((BigDecimal) metrics.get("plPercentage"));
            summary.setPositionCount((Integer) metrics.get("positionCount"));

            // Convert positions to DTOs
            List<PositionDTO> positionDTOs = positions.stream()
                    .map(this::convertToPositionDTO)
                    .collect(Collectors.toList());

            summary.setPositions(positionDTOs);

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            logger.error("Failed to get current portfolio", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/portfolio/history - Get portfolio history
     * No required parameters - defaults to all history or last 365 days
     */
    @GetMapping("/history")
    public ResponseEntity<List<PortfolioSnapshot>> getPortfolioHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        logger.info("GET /api/portfolio/history?startDate={}&endDate={}", startDate, endDate);

        try {
            // Default to last 365 days if no dates provided (full backtest period)
            if (startDate == null) {
                startDate = LocalDateTime.now().minusDays(365);
            }
            if (endDate == null) {
                endDate = LocalDateTime.now().plusDays(1); // Include today
            }
            
            List<PortfolioSnapshot> snapshots = portfolioService.getPortfolioHistory(startDate, endDate);
            return ResponseEntity.ok(snapshots);

        } catch (Exception e) {
            logger.error("Failed to get portfolio history", e);
            return ResponseEntity.ok(List.of()); // Return empty list instead of error
        }
    }

    /**
     * GET /api/portfolio/snapshots - Get all snapshots
     */
    @GetMapping("/snapshots")
    public ResponseEntity<PortfolioSnapshot> getLatestSnapshot() {
        logger.info("GET /api/portfolio/snapshots");

        try {
            return portfolioService.getLatestSnapshot()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            logger.error("Failed to get latest snapshot", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/portfolio/deposit - Record a cash deposit
     */
    @PostMapping("/deposit")
    public ResponseEntity<TransactionDTO> recordDeposit(@RequestBody DepositRequest request) {
        logger.info("POST /api/portfolio/deposit - Amount: ${}", request.getAmount());

        try {
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().build();
            }

            TransactionLog transaction = portfolioService.recordDeposit(
                    request.getAmount(),
                    request.getNotes() != null ? request.getNotes() : "Manual deposit"
            );

            TransactionDTO dto = convertToTransactionDTO(transaction);
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            logger.error("Failed to record deposit", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/portfolio/metrics - Get performance metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        logger.info("GET /api/portfolio/metrics");

        try {
            Map<String, Object> metrics = portfolioService.getPerformanceMetrics();
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            logger.error("Failed to get performance metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods
    private PositionDTO convertToPositionDTO(PositionHistory position) {
        PositionDTO dto = new PositionDTO();
        dto.setSymbol(position.getSymbol());
        dto.setQuantity(position.getQuantity());
        dto.setAveragePrice(position.getAveragePrice());
        dto.setCurrentPrice(position.getCurrentPrice());
        dto.setMarketValue(position.getMarketValue());
        dto.setUnrealizedPL(position.getUnrealizedPL());

        // Calculate P/L percentage
        if (position.getAveragePrice() != null && position.getAveragePrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cost = position.getQuantity().multiply(position.getAveragePrice());
            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                dto.setPlPercentage(position.getUnrealizedPL()
                        .divide(cost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            }
        }

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
