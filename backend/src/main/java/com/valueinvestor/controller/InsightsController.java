package com.valueinvestor.controller;

import com.valueinvestor.model.dto.InsightsDTO;
import com.valueinvestor.model.dto.LearningTipDTO;
import com.valueinvestor.model.entity.DailyLearningTip;
import com.valueinvestor.model.entity.InsightsHistory;
import com.valueinvestor.service.InsightsService;
import com.valueinvestor.service.LearningService;
import com.valueinvestor.service.PortfolioReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "*")
public class InsightsController {

    private static final Logger logger = LoggerFactory.getLogger(InsightsController.class);

    @Autowired
    private InsightsService insightsService;

    @Autowired
    private PortfolioReportService portfolioReportService;

    @Autowired
    private LearningService learningService;

    /**
     * GET /api/insights/current
     * Get latest insights content (from database)
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentInsights() {
        try {
            logger.info("Fetching current insights");
            String insights = insightsService.getCurrentInsights();

            Map<String, Object> response = new HashMap<>();
            response.put("content", insights);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch current insights", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch insights", "success", false));
        }
    }

    /**
     * GET /api/insights/portfolio
     * Get latest portfolio report (generated on-demand)
     */
    @GetMapping("/portfolio")
    public ResponseEntity<?> getPortfolioReport() {
        try {
            logger.info("Fetching portfolio report");
            String report = portfolioReportService.getCurrentReport();

            Map<String, Object> response = new HashMap<>();
            response.put("content", report);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch portfolio report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch portfolio report", "success", false));
        }
    }

    /**
     * POST /api/insights/generate
     * Manually trigger insights generation
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateInsights() {
        try {
            logger.info("Manually triggering insights generation");
            InsightsHistory insights = insightsService.generateMonthlyInsights();

            InsightsDTO dto = convertToDTO(insights);

            Map<String, Object> response = new HashMap<>();
            response.put("insights", dto);
            response.put("message", "Insights generated successfully");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to generate insights", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate insights: " + e.getMessage(), "success", false));
        }
    }

    /**
     * POST /api/insights/portfolio/generate
     * Manually trigger portfolio report generation
     */
    @PostMapping("/portfolio/generate")
    public ResponseEntity<?> generatePortfolioReport() {
        try {
            logger.info("Manually triggering portfolio report generation");
            portfolioReportService.generateReportNow();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Portfolio report generated successfully");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to generate portfolio report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate portfolio report: " + e.getMessage(), "success", false));
        }
    }

    /**
     * GET /api/insights/learning/daily
     * Get daily learning tip
     */
    @GetMapping("/learning/daily")
    public ResponseEntity<?> getDailyLearningTip() {
        try {
            logger.info("Fetching daily learning tip");
            DailyLearningTip tip = learningService.getDailyTip();

            LearningTipDTO dto = convertToLearningTipDTO(tip);

            Map<String, Object> response = new HashMap<>();
            response.put("tip", dto);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch daily learning tip", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch learning tip", "success", false));
        }
    }

    /**
     * POST /api/insights/learning/{id}/like
     * Like/unlike a learning tip
     */
    @PostMapping("/learning/{id}/like")
    public ResponseEntity<?> toggleLikeLearningTip(@PathVariable Long id) {
        try {
            logger.info("Toggling like for learning tip: {}", id);
            DailyLearningTip tip = learningService.toggleLike(id);

            LearningTipDTO dto = convertToLearningTipDTO(tip);

            Map<String, Object> response = new HashMap<>();
            response.put("tip", dto);
            response.put("message", tip.getLiked() ? "Tip liked" : "Tip unliked");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Learning tip not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Learning tip not found", "success", false));

        } catch (Exception e) {
            logger.error("Failed to toggle like for learning tip: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update learning tip", "success", false));
        }
    }

    /**
     * GET /api/insights/learning/recent
     * Get recent learning tips
     */
    @GetMapping("/learning/recent")
    public ResponseEntity<?> getRecentLearningTips(@RequestParam(defaultValue = "10") int limit) {
        try {
            logger.info("Fetching recent learning tips (limit: {})", limit);
            List<DailyLearningTip> tips = learningService.getRecentTips(limit);

            List<LearningTipDTO> dtos = tips.stream()
                    .map(this::convertToLearningTipDTO)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("tips", dtos);
            response.put("count", dtos.size());
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch recent learning tips", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch learning tips", "success", false));
        }
    }

    /**
     * GET /api/insights/learning/liked
     * Get all liked learning tips
     */
    @GetMapping("/learning/liked")
    public ResponseEntity<?> getLikedLearningTips() {
        try {
            logger.info("Fetching liked learning tips");
            List<DailyLearningTip> tips = learningService.getLikedTips();

            List<LearningTipDTO> dtos = tips.stream()
                    .map(this::convertToLearningTipDTO)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("tips", dtos);
            response.put("count", dtos.size());
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch liked learning tips", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch liked tips", "success", false));
        }
    }

    /**
     * GET /api/insights/learning/categories
     * Get available learning categories
     */
    @GetMapping("/learning/categories")
    public ResponseEntity<?> getLearningCategories() {
        try {
            List<String> categories = learningService.getAvailableCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("categories", categories);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch learning categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch categories", "success", false));
        }
    }

    /**
     * GET /api/insights/history
     * Get insights history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getInsightsHistory(@RequestParam(defaultValue = "6") int limit) {
        try {
            logger.info("Fetching insights history (limit: {})", limit);
            List<InsightsHistory> history = insightsService.getInsightsHistory(limit);

            List<InsightsDTO> dtos = history.stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("history", dtos);
            response.put("count", dtos.size());
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to fetch insights history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch insights history", "success", false));
        }
    }

    /**
     * Convert InsightsHistory entity to DTO
     */
    private InsightsDTO convertToDTO(InsightsHistory insights) {
        InsightsDTO dto = new InsightsDTO();
        dto.setId(insights.getId());
        dto.setGeneratedDate(insights.getGeneratedDate());
        dto.setInsightsContent(insights.getInsightsContent());
        dto.setPortfolioValue(insights.getPortfolioValue());
        dto.setMonthlyReturn(insights.getMonthlyReturn());
        dto.setCashBalance(insights.getCashBalance());
        dto.setTotalInvested(insights.getTotalInvested());
        dto.setPositionsCount(insights.getPositionsCount());
        return dto;
    }

    /**
     * Convert DailyLearningTip entity to DTO
     */
    private LearningTipDTO convertToLearningTipDTO(DailyLearningTip tip) {
        return new LearningTipDTO(
                tip.getId(),
                tip.getTipDate(),
                tip.getCategory(),
                tip.getContent(),
                tip.getLiked()
        );
    }
}
