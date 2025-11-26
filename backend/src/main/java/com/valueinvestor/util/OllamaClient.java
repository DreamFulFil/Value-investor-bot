package com.valueinvestor.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class OllamaClient {

    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3.1:8b-instruct-q5_K_M";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public OllamaClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send a prompt to Ollama and get the response
     */
    public String sendPrompt(String prompt) throws IOException {
        return sendPrompt(DEFAULT_MODEL, prompt);
    }

    /**
     * Send a prompt to Ollama with a specific model
     */
    public String sendPrompt(String model, String prompt) throws IOException {
        logger.info("Sending prompt to Ollama model: {}", model);
        logger.debug("Prompt: {}", prompt);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(OLLAMA_BASE_URL + "/api/generate")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama request failed: " + response);
            }

            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);

            String generatedText = root.path("response").asText();
            logger.info("Ollama response received, length: {} chars", generatedText.length());

            return generatedText;
        }
    }

    /**
     * Send a prompt with streaming response (for future use)
     */
    public String sendPromptStreaming(String model, String prompt) throws IOException {
        logger.info("Sending streaming prompt to Ollama model: {}", model);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", true);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(OLLAMA_BASE_URL + "/api/generate")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        StringBuilder fullResponse = new StringBuilder();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama streaming request failed: " + response);
            }

            String responseBody = response.body().string();
            String[] lines = responseBody.split("\n");

            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    JsonNode node = objectMapper.readTree(line);
                    String chunk = node.path("response").asText();
                    fullResponse.append(chunk);
                }
            }
        }

        return fullResponse.toString();
    }

    /**
     * Check if Ollama is available
     */
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(OLLAMA_BASE_URL + "/api/tags")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            logger.warn("Ollama is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if a specific model is available
     */
    public boolean isModelAvailable(String model) {
        try {
            Request request = new Request.Builder()
                    .url(OLLAMA_BASE_URL + "/api/tags")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return false;
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode models = root.path("models");

                if (models.isArray()) {
                    for (JsonNode modelNode : models) {
                        String modelName = modelNode.path("name").asText();
                        if (modelName.equals(model)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to check model availability: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Create a prompt for stock analysis
     */
    public static String createStockAnalysisPrompt(String symbol, String fundamentalsData) {
        return String.format(
            "You are a value investing expert. Analyze the following stock for long-term investment potential.\n\n" +
            "Stock Symbol: %s\n" +
            "Fundamentals:\n%s\n\n" +
            "Please provide:\n" +
            "1. A brief analysis of the company's value investing characteristics\n" +
            "2. Assessment of dividend sustainability and growth potential\n" +
            "3. Evaluation of valuation metrics (PE ratio, PB ratio, etc.)\n" +
            "4. Overall recommendation (BUY, HOLD, or SELL) with a score from 0-100\n" +
            "5. Key risks to consider\n\n" +
            "Focus on: dividend yield, financial stability, competitive moat, and long-term income potential.\n" +
            "Format your response with clear sections and end with 'RECOMMENDATION: [BUY/HOLD/SELL]' and 'SCORE: [0-100]'",
            symbol,
            fundamentalsData
        );
    }

    /**
     * Generate portfolio insights with creative analysis
     * Temperature: 0.7 for creative insights
     */
    public String generateInsights(String portfolioData, String marketContext) throws IOException {
        logger.info("Generating portfolio insights with creative analysis");

        String prompt = String.format(
            "You are a value investing advisor. Analyze the following portfolio and market context to provide actionable insights.\n\n" +
            "Portfolio Summary:\n%s\n\n" +
            "Market Context:\n%s\n\n" +
            "Please provide a comprehensive monthly investment insights report with the following sections:\n" +
            "1. Portfolio Performance Analysis - How has the portfolio performed? Key metrics and trends.\n" +
            "2. Market Analysis - Current market conditions and their impact on dividend stocks.\n" +
            "3. Top Performers - Highlight best performing holdings with specific reasons.\n" +
            "4. Underperformers - Note any stocks lagging and potential reasons.\n" +
            "5. Recommendations - Specific actionable recommendations for next month.\n" +
            "6. Risk Assessment - Current portfolio risk level, diversification analysis.\n" +
            "7. Next Month Strategy - Focus areas and strategic guidance.\n\n" +
            "Keep insights practical, focused on value investing principles, and suitable for a long-term dividend investor.\n" +
            "Be specific with numbers and percentages where available.",
            portfolioData,
            marketContext
        );

        return sendPromptWithTemperature(DEFAULT_MODEL, prompt, 0.7);
    }

    /**
     * Generate a daily learning tip on value investing topics
     * Temperature: 0.7 for engaging educational content
     */
    public String generateLearningTip(String category) throws IOException {
        return generateLearningTip(category, "en");
    }

    /**
     * Generate a daily learning tip on value investing topics with locale support
     * Temperature: 0.7 for engaging educational content
     */
    public String generateLearningTip(String category, String locale) throws IOException {
        logger.info("Generating learning tip for category: {} in locale: {}", category, locale);

        String prompt;
        if ("zh-TW".equals(locale)) {
            prompt = String.format(
                "你是一位有耐心的價值投資教育者。請用繁體中文撰寫一則關於「%s」的簡短實用投資小知識。\n\n" +
                "要求：\n" +
                "- 保持簡潔（3-5句話）\n" +
                "- 專注於可執行的建議\n" +
                "- 盡可能包含具體的例子或數據\n" +
                "- 適合初學者但有深度\n" +
                "- 與股息投資和長期財富累積相關\n\n" +
                "類別說明：\n" +
                "- dividends（股息）：股息的可持續性、成長性、殖利率陷阱、股息再投資策略\n" +
                "- valuation（估值）：本益比、股價淨值比、PEG比率、內在價值、安全邊際\n" +
                "- risk（風險）：分散投資、部位配置、產業集中度、進場時機\n" +
                "- psychology（心理）：情緒紀律、耐心、避免追高、堅持策略\n" +
                "- strategy（策略）：定期定額、再平衡、節稅效率、複利效果\n\n" +
                "格式：只提供小知識內容，不要有前言或額外評論。請用繁體中文回答。",
                category
            );
        } else {
            prompt = String.format(
                "You are a patient value investing educator. Create a brief, practical learning tip about '%s'.\n\n" +
                "Requirements:\n" +
                "- Keep it concise (3-5 sentences)\n" +
                "- Focus on actionable advice\n" +
                "- Include a specific example or metric when possible\n" +
                "- Make it beginner-friendly but insightful\n" +
                "- Relate to dividend investing and long-term wealth building\n\n" +
                "Categories guide:\n" +
                "- dividends: Dividend sustainability, growth, yield traps, DRIP strategies\n" +
                "- valuation: PE ratios, PB ratios, PEG ratios, intrinsic value, margin of safety\n" +
                "- risk: Diversification, position sizing, sector concentration, market timing\n" +
                "- psychology: Emotional discipline, patience, avoiding FOMO, staying the course\n" +
                "- strategy: Dollar-cost averaging, rebalancing, tax efficiency, compounding\n\n" +
                "Format: Provide only the tip content, no preamble or meta-commentary.",
                category
            );
        }

        return sendPromptWithTemperature(DEFAULT_MODEL, prompt, 0.7);
    }

    /**
     * Generate portfolio recommendations based on current holdings
     * Temperature: 0.3 for analytical precision
     */
    public String generatePortfolioRecommendation(String portfolioSummary) throws IOException {
        logger.info("Generating portfolio recommendations with analytical precision");

        String prompt = String.format(
            "You are a value investing analyst. Review this portfolio and provide specific, data-driven recommendations.\n\n" +
            "Portfolio Summary:\n%s\n\n" +
            "Provide analysis on:\n" +
            "1. Sector Allocation - Is diversification adequate? Any overconcentration?\n" +
            "2. Dividend Health - Which holdings have sustainable/growing dividends?\n" +
            "3. Valuation Concerns - Any stocks looking overvalued or undervalued?\n" +
            "4. Position Sizing - Recommendations on increasing/decreasing positions\n" +
            "5. New Opportunities - Suggest 2-3 dividend stocks to research for potential addition\n" +
            "6. Risk Factors - Identify top 3 portfolio risks\n\n" +
            "Be specific with ticker symbols, target allocations, and quantitative metrics.\n" +
            "Format recommendations as clear action items.",
            portfolioSummary
        );

        return sendPromptWithTemperature(DEFAULT_MODEL, prompt, 0.3);
    }

    /**
     * Send prompt with custom temperature setting
     */
    private String sendPromptWithTemperature(String model, String prompt, double temperature) throws IOException {
        logger.info("Sending prompt to Ollama model: {} with temperature: {}", model, temperature);
        logger.debug("Prompt: {}", prompt);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        // Add options node with temperature
        ObjectNode options = objectMapper.createObjectNode();
        options.put("temperature", temperature);
        requestBody.set("options", options);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(OLLAMA_BASE_URL + "/api/generate")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama request failed: " + response);
            }

            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);

            String generatedText = root.path("response").asText();
            logger.info("Ollama response received, length: {} chars", generatedText.length());

            return generatedText;
        }
    }
}
