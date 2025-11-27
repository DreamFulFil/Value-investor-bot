package com.valueinvestor.controller;

import com.valueinvestor.service.ShioajiDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private ShioajiDataService shioajiDataService;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Value Investor Bot");
        response.put("version", "0.0.1");
        return response;
    }

    @GetMapping("/quota")
    public Map<String, Object> getQuota() {
        ShioajiDataService.QuotaStatus quota = shioajiDataService.getQuotaStatus();
        
        Map<String, Object> response = new HashMap<>();
        response.put("usedMB", quota.getUsedMB());
        response.put("limitMB", quota.getLimitMB());
        response.put("remainingMB", quota.getRemainingMB());
        response.put("percentageUsed", quota.getPercentageUsed());
        response.put("fallbackActive", quota.isFallbackActive());
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}
