package com.valueinvestor.service;

import com.valueinvestor.model.entity.TradingConfig;
import com.valueinvestor.model.entity.TransactionLog;
import com.valueinvestor.repository.TradingConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service for managing trading configuration.
 * Reads/writes trading mode to database for persistence across restarts.
 * Once LIVE mode is activated, it is PERMANENT and cannot be reverted.
 */
@Service
public class TradingConfigService {

    private static final Logger logger = LoggerFactory.getLogger(TradingConfigService.class);

    @Autowired
    private TradingConfigRepository configRepository;

    // Cached trading mode for fast access
    private volatile TransactionLog.TradingMode cachedMode = null;
    private volatile boolean isLiveActivated = false;

    @PostConstruct
    public void init() {
        loadTradingMode();
        
        if (isLiveActivated) {
            logger.warn("========================================");
            logger.warn("🔴 LIVE MODE ACTIVE - REAL MONEY TRADING");
            logger.warn("========================================");
            logger.warn("Real orders will execute on the 1st of each month!");
        } else {
            logger.info("✅ Simulation mode active - no real trades will be executed");
        }
    }

    /**
     * Load trading mode from database.
     * If DB has LIVE, it overrides any application.yml setting.
     */
    private void loadTradingMode() {
        try {
            String modeStr = configRepository.getConfigValue(TradingConfig.KEY_TRADING_MODE, "SIMULATION");
            cachedMode = TransactionLog.TradingMode.valueOf(modeStr);
            isLiveActivated = cachedMode == TransactionLog.TradingMode.LIVE;
            logger.info("Trading mode loaded from database: {}", cachedMode);
        } catch (Exception e) {
            logger.warn("Could not load trading mode from DB, defaulting to SIMULATION: {}", e.getMessage());
            cachedMode = TransactionLog.TradingMode.SIMULATION;
            isLiveActivated = false;
        }
    }

    /**
     * Get current trading mode.
     * Always returns database value if LIVE was ever activated.
     */
    public TransactionLog.TradingMode getTradingMode() {
        if (cachedMode == null) {
            loadTradingMode();
        }
        return cachedMode;
    }

    /**
     * Check if LIVE mode is active.
     */
    public boolean isLiveMode() {
        return getTradingMode() == TransactionLog.TradingMode.LIVE;
    }

    /**
     * Activate LIVE mode permanently.
     * This is a one-way operation - once activated, cannot be reverted.
     * 
     * @param option The go-live option chosen (fresh, gradual, oneshot)
     * @return true if successfully activated, false if already live
     */
    @Transactional
    public boolean activateLiveMode(String option) {
        if (isLiveActivated) {
            logger.warn("LIVE mode is already active - cannot activate again");
            return false;
        }

        logger.warn("========================================");
        logger.warn("🚨 ACTIVATING LIVE MODE - PERMANENT!");
        logger.warn("========================================");

        // Save to database
        TradingConfig modeConfig = configRepository.findByKey(TradingConfig.KEY_TRADING_MODE)
                .orElse(new TradingConfig(TradingConfig.KEY_TRADING_MODE, "LIVE"));
        modeConfig.setValue("LIVE");
        modeConfig.setUpdatedBy("GoLiveWizard");
        configRepository.save(modeConfig);

        TradingConfig dateConfig = configRepository.findByKey(TradingConfig.KEY_GO_LIVE_DATE)
                .orElse(new TradingConfig(TradingConfig.KEY_GO_LIVE_DATE, LocalDate.now().toString()));
        dateConfig.setValue(LocalDate.now().toString());
        configRepository.save(dateConfig);

        TradingConfig optionConfig = configRepository.findByKey(TradingConfig.KEY_GO_LIVE_OPTION)
                .orElse(new TradingConfig(TradingConfig.KEY_GO_LIVE_OPTION, option));
        optionConfig.setValue(option);
        configRepository.save(optionConfig);

        // Update cache
        cachedMode = TransactionLog.TradingMode.LIVE;
        isLiveActivated = true;

        logger.warn("LIVE mode activated successfully on {} with option: {}", LocalDate.now(), option);
        return true;
    }

    /**
     * Get the date when LIVE mode was activated.
     */
    public LocalDate getGoLiveDate() {
        String dateStr = configRepository.getConfigValue(TradingConfig.KEY_GO_LIVE_DATE, null);
        if (dateStr != null) {
            try {
                return LocalDate.parse(dateStr);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get the go-live option that was selected.
     */
    public String getGoLiveOption() {
        return configRepository.getConfigValue(TradingConfig.KEY_GO_LIVE_OPTION, null);
    }

    /**
     * Check if LIVE mode has ever been activated (for hiding wizard).
     */
    public boolean hasEverGoneLive() {
        return isLiveActivated || getGoLiveDate() != null;
    }
}
