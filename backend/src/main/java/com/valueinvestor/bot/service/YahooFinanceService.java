package com.valueinvestor.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valueinvestor.bot.util.PythonExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class YahooFinanceService {

    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceService.class);
    private final PythonExecutor pythonExecutor;
    private final ObjectMapper objectMapper;

    public YahooFinanceService(PythonExecutor pythonExecutor, ObjectMapper objectMapper) {
        this.pythonExecutor = pythonExecutor;
        this.objectMapper = objectMapper;
    }

    public Optional<String> getSector(String stockId) {
        try {
            String jsonResult = pythonExecutor.execute("shioaji_bridge/fetch_sector.py", stockId);
            if (jsonResult == null || jsonResult.isBlank()) {
                logger.error("Python script for getSector returned empty for stockId: {}", stockId);
                return Optional.empty();
            }

            JsonNode rootNode = objectMapper.readTree(jsonResult);
            if (rootNode.has("sector")) {
                return Optional.of(rootNode.get("sector").asText());
            } else {
                logger.warn("Could not fetch sector for stockId: {}. Reason: {}", stockId, rootNode.path("error").asText("Unknown error"));
                return Optional.empty();
            }
        } catch (IOException e) {
            logger.error("Failed to parse JSON from fetch_sector.py for stockId: {}", stockId, e);
            return Optional.empty();
        } catch (InterruptedException e) {
            logger.error("Python script execution was interrupted for stockId: {}", stockId, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
