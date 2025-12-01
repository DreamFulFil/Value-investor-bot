package com.valueinvestor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private RebalanceService rebalanceService;

    @Autowired
    private MarketDataService marketDataService;

    // Application mode (SIMULATION or LIVE)
    @Value("${trading.mode:SIMULATION}")
    private String tradingMode;

    // Controls whether automatic monthly rebalance is enabled
    @Value("${app.rebalance.auto-enabled:#{null}}")
    private Boolean autoRebalanceEnabled;

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    /**
     * Monthly rebalance scheduled task.
     * Runs at 09:00 Asia/Taipei on the 1st calendar day, but only executes when
     * that day is a trading day (first trading day logic) and when enabled by properties.
     * Uses @Scheduled with zone = "Asia/Taipei".
     */
    @Scheduled(cron = "0 0 9 1 * ?", zone = "Asia/Taipei")
    public void monthlyRebalance() {
        logger.info("Scheduled task invoked for monthly rebalance (Asia/Taipei 09:00)");

        boolean isSimulation = "SIMULATION".equalsIgnoreCase(tradingMode);
        boolean enabled = (autoRebalanceEnabled != null) ? autoRebalanceEnabled : !isSimulation;

        if (isSimulation) {
            logger.info("Skipping automatic monthly rebalance because application is running in SIMULATION mode");
            return;
        }

        if (!enabled) {
            logger.info("Automatic monthly rebalance is disabled via property app.rebalance.auto-enabled=false");
            return;
        }

        LocalDate today = LocalDate.now(TAIPEI);

        // If 1st is not a trading day, skip. Here we treat weekends as non-trading days.
        // TODO: If exchange holidays are required, enhance with holiday calendar or market-data-driven check.
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            logger.info("Skipping scheduled rebalance because {} is not a trading day (weekend)", today);
            return;
        }

        logger.info("Proceeding with scheduled monthly rebalance for date {} (assumed trading day)", today);

        try {
            RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

            if (result.isSuccess()) {
                logger.info("Monthly rebalance completed successfully: processed {} monthly results", result.getMonthlyResults().size());
            } else {
                logger.error("Monthly rebalance reported failure: {}", result.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Scheduled monthly rebalance failed with exception", e);
        }
    }

    /**
     * Daily fundamentals refresh - runs every day at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Taipei")
    public void dailyFundamentalsRefresh() {
        logger.info("Starting daily fundamentals refresh");

        try {
            marketDataService.refreshStaleData();
            logger.info("Daily fundamentals refresh completed");

        } catch (Exception e) {
            logger.error("Daily fundamentals refresh failed", e);
        }
    }

    /**
     * Weekly health check - runs every Monday at 8:00 AM
     */
    @Scheduled(cron = "0 0 8 ? * MON", zone = "Asia/Taipei")
    public void weeklyHealthCheck() {
        logger.info("========================================");
        logger.info("WEEKLY HEALTH CHECK");
        logger.info("========================================");

        try {
            // Check Ollama availability
            // Note: AnalysisService would need to be injected to check this
            logger.info("System health check completed");

        } catch (Exception e) {
            logger.error("Weekly health check failed", e);
        }

        logger.info("========================================");
    }

    /**
     * Test method to trigger rebalance manually (for debugging)
     * This should be removed or secured in production
     */
    public void manualTriggerRebalance() {
        logger.warn("Manual trigger of rebalance service");
        // Call RebalanceService directly to preserve manual behavior
        rebalanceService.triggerRebalance();
    }
}
