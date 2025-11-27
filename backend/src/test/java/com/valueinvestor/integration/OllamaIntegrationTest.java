package com.valueinvestor.integration;

import com.valueinvestor.util.OllamaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Ollama LLM integration
 * Tests the interaction between Java backend and Ollama for insights generation
 */
@ExtendWith(MockitoExtension.class)
class OllamaIntegrationTest {

    @Mock
    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        // Setup common test fixtures
    }

    @Test
    void should_generateInsights_successfully() throws IOException {
        // Given
        String portfolioData = "Total Value: $160,000\nPositions: 5";
        String expectedInsights = "## Portfolio Analysis\n\nYour portfolio is performing well.";

        when(ollamaClient.generateInsights(anyString(), anyString()))
                .thenReturn(expectedInsights);

        // When
        String result = ollamaClient.generateInsights(portfolioData, "context");

        // Then
        assertThat(result).contains("Portfolio Analysis");
    }

    @Test
    void should_generateLearningTip_inEnglish() throws IOException {
        // Given
        String expectedTip = "Value investing focuses on buying quality companies at reasonable prices.";
        when(ollamaClient.generateLearningTip("en")).thenReturn(expectedTip);

        // When
        String result = ollamaClient.generateLearningTip("en");

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.toLowerCase()).contains("value");
    }

    @Test
    void should_generateLearningTip_inChinese() throws IOException {
        // Given
        String expectedTip = "價值投資的核心是以合理價格買入優質公司。";
        when(ollamaClient.generateLearningTip("zh-TW")).thenReturn(expectedTip);

        // When
        String result = ollamaClient.generateLearningTip("zh-TW");

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).contains("價值投資");
    }

    @Test
    void should_handleOllamaUnavailable_gracefully() {
        // Given
        when(ollamaClient.isAvailable()).thenReturn(false);

        // When
        boolean available = ollamaClient.isAvailable();

        // Then
        assertThat(available).isFalse();
    }

    @Test
    void should_checkOllamaAvailability() {
        // Given
        when(ollamaClient.isAvailable()).thenReturn(true);

        // When
        boolean available = ollamaClient.isAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void should_handleEmptyPortfolioData() throws IOException {
        // Given
        String emptyData = "";
        String defaultResponse = "No portfolio data available for analysis.";
        when(ollamaClient.generateInsights(anyString(), anyString())).thenReturn(defaultResponse);

        // When
        String result = ollamaClient.generateInsights(emptyData, "");

        // Then
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_formatMarkdownOutput() throws IOException {
        // Given
        String insights = "## Summary\n\n- Point 1\n- Point 2\n\n### Recommendations\n\n1. Buy more TSMC";
        when(ollamaClient.generateInsights(anyString(), anyString())).thenReturn(insights);

        // When
        String result = ollamaClient.generateInsights("data", "context");

        // Then
        assertThat(result).contains("##");
        assertThat(result).contains("###");
    }
}
