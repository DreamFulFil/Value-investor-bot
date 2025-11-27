package com.valueinvestor.service;

import com.valueinvestor.model.entity.DailyLearningTip;
import com.valueinvestor.repository.DailyLearningTipRepository;
import com.valueinvestor.util.OllamaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private DailyLearningTipRepository learningTipRepository;

    @Mock
    private OllamaClient ollamaClient;

    @InjectMocks
    private LearningService learningService;

    private DailyLearningTip testTip;

    @BeforeEach
    void setUp() {
        testTip = new DailyLearningTip(LocalDate.now(), "dividends", "Focus on dividend growth");
        testTip.setId(1L);
    }

    @Test
    void should_getDailyTip_when_tipExistsForToday() throws Exception {
        // Given
        when(learningTipRepository.findByTipDate(any(LocalDate.class))).thenReturn(Optional.of(testTip));

        // When
        DailyLearningTip result = learningService.getDailyTip();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTipDate()).isEqualTo(LocalDate.now());
        verify(learningTipRepository).findByTipDate(any(LocalDate.class));
        verify(ollamaClient, never()).generateLearningTip(anyString());
    }

    @Test
    void should_generateDailyTip_when_tipDoesNotExist() throws Exception {
        // Given
        when(learningTipRepository.findByTipDate(any(LocalDate.class))).thenReturn(Optional.empty());
        when(learningTipRepository.existsByTipDate(any(LocalDate.class))).thenReturn(false);
        when(ollamaClient.generateLearningTip(anyString(), anyString())).thenReturn("Focus on dividend growth");
        when(learningTipRepository.save(any(DailyLearningTip.class))).thenReturn(testTip);

        // When
        DailyLearningTip result = learningService.getDailyTip();

        // Then
        assertThat(result).isNotNull();
        verify(ollamaClient, atLeastOnce()).generateLearningTip(anyString(), anyString());
        verify(learningTipRepository).save(any(DailyLearningTip.class));
    }

    @Test
    void should_toggleLike_when_tipExists() {
        // Given
        when(learningTipRepository.findById(1L)).thenReturn(Optional.of(testTip));
        when(learningTipRepository.save(any(DailyLearningTip.class))).thenReturn(testTip);

        // When
        DailyLearningTip result = learningService.toggleLike(1L);

        // Then
        assertThat(result).isNotNull();
        verify(learningTipRepository).save(any(DailyLearningTip.class));
    }

    @Test
    void should_throwException_when_toggleLikeForNonExistentTip() {
        // Given
        when(learningTipRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> learningService.toggleLike(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tip not found");
    }

    @Test
    void should_getLikedTips_when_called() {
        // Given
        List<DailyLearningTip> likedTips = List.of(testTip);
        when(learningTipRepository.findByLikedTrue()).thenReturn(likedTips);

        // When
        List<DailyLearningTip> result = learningService.getLikedTips();

        // Then
        assertThat(result).hasSize(1);
        verify(learningTipRepository).findByLikedTrue();
    }

    @Test
    void should_getTipsByCategory_when_categoryProvided() {
        // Given
        List<DailyLearningTip> tips = List.of(testTip);
        when(learningTipRepository.findByCategoryOrderByTipDateDesc("dividends")).thenReturn(tips);

        // When
        List<DailyLearningTip> result = learningService.getTipsByCategory("dividends");

        // Then
        assertThat(result).hasSize(1);
        verify(learningTipRepository).findByCategoryOrderByTipDateDesc("dividends");
    }

    @Test
    void should_getRecentTips_when_limitProvided() {
        // Given
        List<DailyLearningTip> tips = List.of(testTip);
        when(learningTipRepository.findRecentTips()).thenReturn(tips);

        // When
        List<DailyLearningTip> result = learningService.getRecentTips(10);

        // Then
        assertThat(result).hasSize(1);
        verify(learningTipRepository).findRecentTips();
    }

    @Test
    void should_generateTipForCategory_when_validCategory() throws Exception {
        // Given
        when(ollamaClient.generateLearningTip("dividends")).thenReturn("Dividend tip");
        when(learningTipRepository.save(any(DailyLearningTip.class))).thenReturn(testTip);

        // When
        DailyLearningTip result = learningService.generateTipForCategory("dividends");

        // Then
        assertThat(result).isNotNull();
        verify(ollamaClient).generateLearningTip("dividends");
    }

    @Test
    void should_throwException_when_invalidCategory() {
        // When/Then
        assertThatThrownBy(() -> learningService.generateTipForCategory("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid category");
    }

    @Test
    void should_getAvailableCategories_when_called() {
        // When
        List<String> categories = learningService.getAvailableCategories();

        // Then
        assertThat(categories).isNotEmpty();
        assertThat(categories).contains("dividends", "valuation", "risk", "psychology", "strategy");
    }

    @Test
    void should_useFallbackTip_when_ollamaFails() throws Exception {
        // Given
        when(learningTipRepository.existsByTipDate(any())).thenReturn(false);
        when(ollamaClient.generateLearningTip(anyString(), anyString())).thenThrow(new RuntimeException("Ollama error"));
        when(learningTipRepository.save(any(DailyLearningTip.class))).thenReturn(testTip);

        // When
        DailyLearningTip result = learningService.generateDailyTip();

        // Then
        assertThat(result).isNotNull();
        verify(learningTipRepository).save(any(DailyLearningTip.class));
    }
}
