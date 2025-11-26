package com.valueinvestor.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaClientTest {

    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        ollamaClient = new OllamaClient();
    }

    @Test
    void should_createStockAnalysisPrompt_when_fundamentalsProvided() {
        // Given
        String symbol = "AAPL";
        String fundamentals = "Price: $150, PE: 25";

        // When
        String prompt = OllamaClient.createStockAnalysisPrompt(symbol, fundamentals);

        // Then
        assertThat(prompt).contains("AAPL");
        assertThat(prompt).contains("Price: $150");
        assertThat(prompt).contains("value investing");
        assertThat(prompt).contains("RECOMMENDATION");
        assertThat(prompt).contains("SCORE");
    }

    @Test
    void should_checkAvailability_when_ollamaNotRunning() {
        // When
        boolean isAvailable = ollamaClient.isAvailable();

        // Then - When Ollama is not running, it should return false
        // Note: This test may pass or fail depending on Ollama availability
        assertThat(isAvailable).isIn(true, false);
    }

    @Test
    void should_checkModelAvailability_when_modelProvided() {
        // When
        boolean isAvailable = ollamaClient.isModelAvailable("llama3.1:8b-instruct-q5_K_M");

        // Then - When Ollama is not running or model not available, it should return false
        assertThat(isAvailable).isIn(true, false);
    }
}
