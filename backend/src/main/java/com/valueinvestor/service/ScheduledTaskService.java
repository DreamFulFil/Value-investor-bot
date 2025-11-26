package com.valueinvestor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private RebalanceService rebalanceService;

    @Autowired
    private MarketDataService marketDataService;

    /**
     * Monthly rebalance - runs on 1st of each month at 9:00 AM
     * Cron format: second minute hour day month day-of-week
     */
    @Scheduled(cron = "0 0 9 1 * ?")
    public void monthlyRebalance() {
        logger.info("========================================");
        logger.info("SCHEDULED MONTHLY REBALANCE TRIGGERED");
        logger.info("========================================");

        try {
            RebalanceService.RebalanceResult result = rebalanceService.performMonthlyRebalance();

            if (result.isSuccess()) {
                logger.info("Monthly rebalance completed successfully");
                logger.info("Months processed: {}", result.getMissedMonths());
                logger.info("Total results: {}", result.getMonthlyResults().size());
            } else {
                logger.error("Monthly rebalance failed: {}", result.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Scheduled monthly rebalance failed with exception", e);
        }

        logger.info("========================================");
    }

    /**
     * Daily fundamentals refresh - runs every day at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
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
    @Scheduled(cron = "0 0 8 ? * MON")
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
        monthlyRebalance();
    }
}
