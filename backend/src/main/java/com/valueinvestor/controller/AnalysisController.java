package com.valueinvestor.controller;

import com.valueinvestor.model.dto.AnalysisDTO;
import com.valueinvestor.model.entity.AnalysisResults;
import com.valueinvestor.service.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);

    @Autowired
    private AnalysisService analysisService;

    /**
     * GET /api/analysis/stock/{symbol} - Get latest analysis for a stock
     */
    @GetMapping("/stock/{symbol}")
    public ResponseEntity<AnalysisDTO> getStockAnalysis(@PathVariable String symbol) {
        logger.info("GET /api/analysis/stock/{}", symbol);

        try {
            return analysisService.getLatestAnalysis(symbol)
                    .map(this::convertToDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            logger.error("Failed to get analysis for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/analysis/recent - Get recent analyses
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AnalysisDTO>> getRecentAnalyses(
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("GET /api/analysis/recent?limit={}", limit);

        try {
            List<AnalysisResults> analyses = analysisService.getRecentAnalyses(limit);
            List<AnalysisDTO> dtos = analyses.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            logger.error("Failed to get recent analyses", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/analysis/analyze/{symbol} - Trigger new analysis
     */
    @PostMapping("/analyze/{symbol}")
    public ResponseEntity<AnalysisDTO> analyzeStock(@PathVariable String symbol) {
        logger.info("POST /api/analysis/analyze/{}", symbol);

        try {
            AnalysisResults analysis = analysisService.analyzeStock(symbol);
            AnalysisDTO dto = convertToDTO(analysis);

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            logger.error("Failed to analyze {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/analysis/history/{symbol} - Get analysis history for a stock
     */
    @GetMapping("/history/{symbol}")
    public ResponseEntity<List<AnalysisDTO>> getAnalysisHistory(@PathVariable String symbol) {
        logger.info("GET /api/analysis/history/{}", symbol);

        try {
            List<AnalysisResults> analyses = analysisService.getStockAnalysisHistory(symbol);
            List<AnalysisDTO> dtos = analyses.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            logger.error("Failed to get analysis history for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/analysis/top-rated - Get top rated stocks
     */
    @GetMapping("/top-rated")
    public ResponseEntity<List<AnalysisDTO>> getTopRatedStocks(
            @RequestParam(defaultValue = "70.0") Double minScore) {

        logger.info("GET /api/analysis/top-rated?minScore={}", minScore);

        try {
            List<AnalysisResults> analyses = analysisService.getTopRatedStocks(minScore);
            List<AnalysisDTO> dtos = analyses.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            logger.error("Failed to get top rated stocks", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/analysis/status - Check if analysis service is available
     */
    @GetMapping("/status")
    public ResponseEntity<AnalysisStatusDTO> getAnalysisStatus() {
        logger.info("GET /api/analysis/status");

        try {
            boolean ollamaAvailable = analysisService.isOllamaAvailable();
            boolean modelAvailable = analysisService.isAnalysisModelAvailable();

            AnalysisStatusDTO status = new AnalysisStatusDTO();
            status.setOllamaAvailable(ollamaAvailable);
            status.setModelAvailable(modelAvailable);
            status.setReady(ollamaAvailable && modelAvailable);

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Failed to check analysis status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods
    private AnalysisDTO convertToDTO(AnalysisResults analysis) {
        AnalysisDTO dto = new AnalysisDTO();
        dto.setId(analysis.getId());
        dto.setSymbol(analysis.getSymbol());
        dto.setTimestamp(analysis.getTimestamp());
        dto.setAnalysisText(analysis.getAnalysisText());
        dto.setScore(analysis.getScore());
        dto.setRecommendation(analysis.getRecommendation());
        dto.setModel(analysis.getModel());
        return dto;
    }

    // Inner class for status response
    public static class AnalysisStatusDTO {
        private boolean ollamaAvailable;
        private boolean modelAvailable;
        private boolean ready;

        public boolean isOllamaAvailable() {
            return ollamaAvailable;
        }

        public void setOllamaAvailable(boolean ollamaAvailable) {
            this.ollamaAvailable = ollamaAvailable;
        }

        public boolean isModelAvailable() {
            return modelAvailable;
        }

        public void setModelAvailable(boolean modelAvailable) {
            this.modelAvailable = modelAvailable;
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }
    }
}
