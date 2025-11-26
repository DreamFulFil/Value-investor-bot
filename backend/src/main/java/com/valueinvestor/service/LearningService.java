package com.valueinvestor.service;

import com.valueinvestor.model.entity.DailyLearningTip;
import com.valueinvestor.repository.DailyLearningTipRepository;
import com.valueinvestor.util.OllamaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class LearningService {

    private static final Logger logger = LoggerFactory.getLogger(LearningService.class);

    // Learning tip categories that rotate
    private static final List<String> CATEGORIES = Arrays.asList(
            "dividends",
            "valuation",
            "risk",
            "psychology",
            "strategy"
    );

    @Autowired
    private DailyLearningTipRepository learningTipRepository;

    @Autowired
    private OllamaClient ollamaClient;

    /**
     * Get daily learning tip (generates if not exists for today)
     */
    @Transactional
    public DailyLearningTip getDailyTip() {
        return getDailyTip("en");
    }

    /**
     * Get daily learning tip with locale support
     */
    @Transactional
    public DailyLearningTip getDailyTip(String locale) {
        LocalDate today = LocalDate.now();

        // Check if tip already exists for today
        Optional<DailyLearningTip> existingTip = learningTipRepository.findByTipDate(today);
        if (existingTip.isPresent()) {
            logger.info("Returning existing tip for {}", today);
            DailyLearningTip tip = existingTip.get();
            // If user wants zh-TW and we have it cached, or generate on-the-fly
            if ("zh-TW".equals(locale) && (tip.getTipContentZhTw() == null || tip.getTipContentZhTw().isEmpty())) {
                // Generate zh-TW version on-the-fly
                try {
                    String zhContent = ollamaClient.generateLearningTip(tip.getCategory(), "zh-TW");
                    tip.setTipContentZhTw(zhContent);
                    learningTipRepository.save(tip);
                } catch (Exception e) {
                    logger.warn("Failed to generate zh-TW tip, using English", e);
                }
            }
            return tip;
        }

        // Generate new tip
        return generateDailyTip(locale);
    }

    /**
     * Generate daily learning tip
     * Scheduled to run at 6 AM every day
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public DailyLearningTip generateDailyTip() {
        return generateDailyTip("en");
    }

    /**
     * Generate daily learning tip with locale support
     */
    @Transactional
    public DailyLearningTip generateDailyTip(String locale) {
        LocalDate today = LocalDate.now();

        // Check if already generated
        if (learningTipRepository.existsByTipDate(today)) {
            logger.info("Daily tip already generated for {}", today);
            return learningTipRepository.findByTipDate(today).orElse(null);
        }

        try {
            // Determine category based on day of week rotation
            String category = selectCategoryForToday();
            logger.info("Generating daily learning tip for category: {} in locale: {}", category, locale);

            // Generate tip content using Ollama in both languages
            String tipContentEn = ollamaClient.generateLearningTip(category, "en");
            String tipContentZhTw = ollamaClient.generateLearningTip(category, "zh-TW");

            // Save to database with both language versions
            DailyLearningTip tip = new DailyLearningTip(today, category, tipContentEn);
            tip.setTipContentZhTw(tipContentZhTw);
            tip = learningTipRepository.save(tip);

            logger.info("Successfully generated daily tip for {}: {} (both en and zh-TW)", today, category);
            return tip;

        } catch (Exception e) {
            logger.error("Failed to generate daily tip", e);

            // Create fallback tip with both languages
            String fallbackContentEn = getFallbackTip("en");
            String fallbackContentZhTw = getFallbackTip("zh-TW");
            DailyLearningTip fallbackTip = new DailyLearningTip(today, "strategy", fallbackContentEn);
            fallbackTip.setTipContentZhTw(fallbackContentZhTw);
            return learningTipRepository.save(fallbackTip);
        }
    }

    /**
     * Like or unlike a tip
     */
    @Transactional
    public DailyLearningTip toggleLike(Long tipId) {
        Optional<DailyLearningTip> tipOpt = learningTipRepository.findById(tipId);

        if (tipOpt.isEmpty()) {
            throw new IllegalArgumentException("Tip not found with id: " + tipId);
        }

        DailyLearningTip tip = tipOpt.get();
        tip.setLiked(!tip.getLiked());
        tip = learningTipRepository.save(tip);

        logger.info("Toggled like for tip {}: {}", tipId, tip.getLiked());
        return tip;
    }

    /**
     * Get all liked tips
     */
    public List<DailyLearningTip> getLikedTips() {
        return learningTipRepository.findByLikedTrue();
    }

    /**
     * Get tips by category
     */
    public List<DailyLearningTip> getTipsByCategory(String category) {
        return learningTipRepository.findByCategoryOrderByTipDateDesc(category);
    }

    /**
     * Get recent tips
     */
    public List<DailyLearningTip> getRecentTips(int limit) {
        List<DailyLearningTip> allTips = learningTipRepository.findRecentTips();
        return allTips.stream().limit(limit).toList();
    }

    /**
     * Select category for today based on rotation
     */
    private String selectCategoryForToday() {
        LocalDate today = LocalDate.now();
        int dayOfYear = today.getDayOfYear();
        int categoryIndex = dayOfYear % CATEGORIES.size();
        return CATEGORIES.get(categoryIndex);
    }

    /**
     * Get fallback tip when LLM fails
     */
    private String getFallbackTip(String locale) {
        if ("zh-TW".equals(locale)) {
            return "專注於連續5年以上穩定成長股息的公司。" +
                   "持續增加股息的紀錄顯示了公司的財務實力和管理層對股東回報的承諾。" +
                   "尋找股息成長率超過通膨的股票，確保您的實質收入隨時間增長。" +
                   "價值投資的核心是以合理價格買入優質公司，長期持有。";
        }
        return "Focus on companies with consistent dividend growth over 5+ years. " +
               "A track record of increasing dividends demonstrates financial strength and " +
               "management's commitment to returning value to shareholders. " +
               "Look for dividend growth rates that exceed inflation to ensure your real income grows over time.";
    }

    /**
     * Get fallback tip (default English)
     */
    private String getFallbackTip() {
        return getFallbackTip("en");
    }

    /**
     * Manual tip generation for specific category
     */
    @Transactional
    public DailyLearningTip generateTipForCategory(String category) {
        if (!CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }

        try {
            logger.info("Manually generating tip for category: {}", category);
            String tipContent = ollamaClient.generateLearningTip(category);

            DailyLearningTip tip = new DailyLearningTip(LocalDate.now(), category, tipContent);
            return learningTipRepository.save(tip);

        } catch (Exception e) {
            logger.error("Failed to generate tip for category: {}", category, e);
            throw new RuntimeException("Failed to generate learning tip", e);
        }
    }

    /**
     * Get available categories
     */
    public List<String> getAvailableCategories() {
        return CATEGORIES;
    }
}
