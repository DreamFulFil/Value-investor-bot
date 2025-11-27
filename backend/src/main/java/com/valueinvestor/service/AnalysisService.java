package com.valueinvestor.service;

import com.valueinvestor.model.entity.AnalysisResults;
import com.valueinvestor.model.entity.StockFundamentals;
import com.valueinvestor.repository.AnalysisResultsRepository;
import com.valueinvestor.util.OllamaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);
    private static final String MODEL = "llama3.1:8b-instruct-q5_K_M";

    @Autowired
    private AnalysisResultsRepository analysisRepository;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private OllamaClient ollamaClient;

    /**
     * Analyze a stock using LLM (with rule-based fallback)
     */
    @Transactional
    public AnalysisResults analyzeStock(String symbol) {
        logger.info("Starting analysis for stock: {}", symbol);

        try {
            // Get fundamentals
            StockFundamentals fundamentals = marketDataService.getFundamentals(symbol);

            // If no fundamentals, try rule-based analysis with defaults
            if (fundamentals == null) {
                logger.warn("No fundamentals found for {}, using rule-based fallback", symbol);
                return createRuleBasedAnalysis(symbol, null);
            }

            // Check if Ollama is available
            if (!ollamaClient.isAvailable()) {
                logger.warn("Ollama not available, using rule-based analysis for {}", symbol);
                return createRuleBasedAnalysis(symbol, fundamentals);
            }

            // Build fundamentals summary
            String fundamentalsData = buildFundamentalsSummary(fundamentals);

            // Create prompt
            String prompt = OllamaClient.createStockAnalysisPrompt(symbol, fundamentalsData);

            // Call Ollama
            logger.info("Calling Ollama for analysis of {}", symbol);
            String analysisText = ollamaClient.sendPrompt(MODEL, prompt);

            // Parse recommendation and score
            String recommendation = extractRecommendation(analysisText);
            Double score = extractScore(analysisText);

            // Save analysis
            AnalysisResults analysis = new AnalysisResults(
                    symbol,
                    analysisText,
                    score,
                    recommendation,
                    fundamentalsData
            );

            analysis = analysisRepository.save(analysis);
            logger.info("Analysis completed for {}: {} (Score: {})", symbol, recommendation, score);

            return analysis;

        } catch (Exception e) {
            logger.error("Failed to analyze stock: {}, using rule-based fallback", symbol, e);
            return createRuleBasedAnalysis(symbol, null);
        }
    }
    
    /**
     * Create a rule-based analysis when Ollama is not available
     */
    private AnalysisResults createRuleBasedAnalysis(String symbol, StockFundamentals fundamentals) {
        double score = 60.0; // Default moderate score
        String recommendation = "BUY"; // Default to BUY for dividend stocks in universe
        String analysisText;
        String fundamentalsSnapshot = "N/A";
        
        if (fundamentals != null) {
            fundamentalsSnapshot = buildFundamentalsSummary(fundamentals);
            
            // Simple rule-based scoring
            score = 50.0;
            
            // Dividend yield bonus
            if (fundamentals.getDividendYield() != null) {
                double yield = fundamentals.getDividendYield().doubleValue();
                if (yield >= 5.0) score += 20;
                else if (yield >= 3.0) score += 15;
                else if (yield >= 2.0) score += 10;
            }
            
            // P/E ratio consideration
            if (fundamentals.getPeRatio() != null) {
                double pe = fundamentals.getPeRatio().doubleValue();
                if (pe > 0 && pe < 15) score += 15;
                else if (pe >= 15 && pe < 25) score += 10;
                else if (pe >= 25) score -= 5;
            }
            
            // ROE consideration
            if (fundamentals.getRoe() != null) {
                double roe = fundamentals.getRoe().doubleValue();
                if (roe >= 15) score += 10;
                else if (roe >= 10) score += 5;
            }
            
            analysisText = String.format(
                "Rule-based analysis for %s:\n" +
                "- Dividend Yield: %s%%\n" +
                "- P/E Ratio: %s\n" +
                "- ROE: %s%%\n" +
                "RECOMMENDATION: %s\n" +
                "SCORE: %.0f",
                symbol,
                fundamentals.getDividendYield() != null ? fundamentals.getDividendYield() : "N/A",
                fundamentals.getPeRatio() != null ? fundamentals.getPeRatio() : "N/A",
                fundamentals.getRoe() != null ? fundamentals.getRoe() : "N/A",
                score >= 60 ? "BUY" : (score >= 40 ? "HOLD" : "SELL"),
                score
            );
        } else {
            // No fundamentals - still return BUY for stocks in universe (they're pre-screened)
            score = 65.0;
            analysisText = String.format(
                "Rule-based analysis for %s (no fundamentals data):\n" +
                "Stock is in the Taiwan dividend stock universe.\n" +
                "Default recommendation based on universe inclusion.\n" +
                "RECOMMENDATION: BUY\n" +
                "SCORE: 65",
                symbol
            );
        }
        
        recommendation = score >= 60 ? "BUY" : (score >= 40 ? "HOLD" : "SELL");
        
        AnalysisResults analysis = new AnalysisResults(
                symbol,
                analysisText,
                score,
                recommendation,
                fundamentalsSnapshot
        );
        
        return analysisRepository.save(analysis);
    }

    /**
     * Get latest analysis for a stock
     */
    public Optional<AnalysisResults> getLatestAnalysis(String symbol) {
        return analysisRepository.findLatestAnalysisBySymbol(symbol);
    }

    /**
     * Get recent analyses
     */
    public List<AnalysisResults> getRecentAnalyses(int limit) {
        return analysisRepository.findRecentAnalyses(limit);
    }

    /**
     * Get all analyses for a stock
     */
    public List<AnalysisResults> getStockAnalysisHistory(String symbol) {
        return analysisRepository.findBySymbolOrderByTimestampDesc(symbol);
    }

    /**
     * Get buy recommendations since a date
     */
    public List<AnalysisResults> getBuyRecommendationsSince(LocalDateTime since) {
        return analysisRepository.findBuyRecommendationsSince(since);
    }

    /**
     * Get top rated stocks
     */
    public List<AnalysisResults> getTopRatedStocks(Double minScore) {
        return analysisRepository.findByMinScore(minScore);
    }

    /**
     * Build fundamentals summary string
     */
    private String buildFundamentalsSummary(StockFundamentals fundamentals) {
        StringBuilder summary = new StringBuilder();

        summary.append("Company Name: ").append(fundamentals.getName()).append("\n");

        if (fundamentals.getSector() != null) {
            summary.append("Sector: ").append(fundamentals.getSector()).append("\n");
        }

        if (fundamentals.getCurrentPrice() != null) {
            summary.append("Current Price: $").append(fundamentals.getCurrentPrice()).append("\n");
        }

        if (fundamentals.getMarketCap() != null) {
            summary.append("Market Cap: $").append(formatLargeNumber(fundamentals.getMarketCap())).append("\n");
        }

        if (fundamentals.getDividendYield() != null) {
            summary.append("Dividend Yield: ").append(fundamentals.getDividendYield()).append("%\n");
        }

        if (fundamentals.getPeRatio() != null) {
            summary.append("P/E Ratio: ").append(fundamentals.getPeRatio()).append("\n");
        }

        if (fundamentals.getPbRatio() != null) {
            summary.append("P/B Ratio: ").append(fundamentals.getPbRatio()).append("\n");
        }

        if (fundamentals.getDebtToEquity() != null) {
            summary.append("Debt/Equity: ").append(fundamentals.getDebtToEquity()).append("\n");
        }

        if (fundamentals.getRoe() != null) {
            summary.append("ROE: ").append(fundamentals.getRoe()).append("%\n");
        }

        return summary.toString();
    }

    /**
     * Extract recommendation from analysis text
     */
    private String extractRecommendation(String analysisText) {
        Pattern pattern = Pattern.compile("RECOMMENDATION:\\s*(BUY|HOLD|SELL)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(analysisText);

        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        // Fallback: look for keywords in text
        String lowerText = analysisText.toLowerCase();
        if (lowerText.contains("strong buy") || lowerText.contains("recommend buying")) {
            return "BUY";
        } else if (lowerText.contains("hold") || lowerText.contains("neutral")) {
            return "HOLD";
        } else if (lowerText.contains("sell") || lowerText.contains("avoid")) {
            return "SELL";
        }

        return "HOLD"; // Default
    }

    /**
     * Extract score from analysis text
     */
    private Double extractScore(String analysisText) {
        Pattern pattern = Pattern.compile("SCORE:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(analysisText);

        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse score from analysis", e);
            }
        }

        // Default score based on recommendation
        String recommendation = extractRecommendation(analysisText);
        return switch (recommendation) {
            case "BUY" -> 75.0;
            case "HOLD" -> 50.0;
            case "SELL" -> 25.0;
            default -> 50.0;
        };
    }

    /**
     * Format large numbers (e.g., market cap)
     */
    private String formatLargeNumber(java.math.BigDecimal number) {
        double value = number.doubleValue();

        if (value >= 1_000_000_000_000.0) {
            return String.format("%.2fT", value / 1_000_000_000_000.0);
        } else if (value >= 1_000_000_000.0) {
            return String.format("%.2fB", value / 1_000_000_000.0);
        } else if (value >= 1_000_000.0) {
            return String.format("%.2fM", value / 1_000_000.0);
        } else {
            return String.format("%.2f", value);
        }
    }

    /**
     * Create error analysis
     */
    private AnalysisResults createErrorAnalysis(String symbol, String errorMessage) {
        AnalysisResults analysis = new AnalysisResults();
        analysis.setSymbol(symbol);
        analysis.setAnalysisText("Error: " + errorMessage);
        analysis.setRecommendation("HOLD");
        analysis.setScore(0.0);
        analysis.setFundamentalsSnapshot("N/A");
        return analysisRepository.save(analysis);
    }

    /**
     * Check if Ollama is available
     */
    public boolean isOllamaAvailable() {
        return ollamaClient.isAvailable();
    }

    /**
     * Check if analysis model is available
     */
    public boolean isAnalysisModelAvailable() {
        return ollamaClient.isModelAvailable(MODEL);
    }
}
