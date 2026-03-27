package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.entity.SimilarityAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SimilarityCalculator service.
 * 
 * Tests the core similarity algorithms used in duplicate bug detection:
 * - Cosine Similarity (TF-IDF based)
 * - Jaccard Similarity (set-based overlap)
 * - Levenshtein Similarity (character-level differences)
 * 
 * This is a critical component of the advanced duplicate detection system.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SimilarityCalculator Tests")
class SimilarityCalculatorTest {

    @Mock
    private TextPreprocessor textPreprocessor;

    @InjectMocks
    private SimilarityCalculator similarityCalculator;

    @BeforeEach
    void setUp() {
        // Setup common mock behaviors with lenient stubbing
        lenient().when(textPreprocessor.preprocessText(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            return text != null ? text.toLowerCase().trim() : "";
        });

        lenient().when(textPreprocessor.createCharacterFrequencyMap(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            Map<CharSequence, Integer> frequencyMap = new HashMap<>();
            if (text != null && !text.isEmpty()) {
                String[] words = text.split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        frequencyMap.merge(word, 1, Integer::sum);
                    }
                }
            }
            return frequencyMap;
        });
    }

    @Test
    @DisplayName("Should calculate cosine similarity correctly for identical texts")
    void shouldCalculateCosineSimilarityForIdenticalTexts() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Login button not working";

        // When
        double similarity = similarityCalculator.calculateCosineSimilarity(text1, text2);

        // Then
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should calculate cosine similarity correctly for orthogonal texts")
    void shouldCalculateCosineSimilarityForOrthogonalTexts() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Database connection timeout";

        // When
        double similarity = similarityCalculator.calculateCosineSimilarity(text1, text2);

        // Then
        assertThat(similarity).isBetween(0.0, 1.0);
        assertThat(similarity).isLessThan(0.5); // Should be quite different
    }

    @Test
    @DisplayName("Should handle null inputs in cosine similarity")
    void shouldHandleNullInputsInCosineSimilarity() {
        // When & Then
        assertThat(similarityCalculator.calculateCosineSimilarity(null, "test")).isEqualTo(0.0);
        assertThat(similarityCalculator.calculateCosineSimilarity("test", null)).isEqualTo(0.0);
        assertThat(similarityCalculator.calculateCosineSimilarity(null, null)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle empty inputs in cosine similarity")
    void shouldHandleEmptyInputsInCosineSimilarity() {
        // When & Then
        assertThat(similarityCalculator.calculateCosineSimilarity("", "test")).isEqualTo(0.0);
        assertThat(similarityCalculator.calculateCosineSimilarity("test", "")).isEqualTo(0.0);
        assertThat(similarityCalculator.calculateCosineSimilarity("", "")).isEqualTo(0.0);
        assertThat(similarityCalculator.calculateCosineSimilarity("   ", "test")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate Jaccard similarity correctly for identical texts")
    void shouldCalculateJaccardSimilarityForIdenticalTexts() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Login button not working";

        // When
        double similarity = similarityCalculator.calculateJaccardSimilarity(text1, text2);

        // Then
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should calculate Jaccard similarity correctly for different texts")
    void shouldCalculateJaccardSimilarityForDifferentTexts() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Database connection timeout";

        // When
        double similarity = similarityCalculator.calculateJaccardSimilarity(text1, text2);

        // Then
        assertThat(similarity).isBetween(0.0, 1.0);
        assertThat(similarity).isLessThan(0.5); // Should be quite different
    }

    @Test
    @DisplayName("Should handle null inputs in Jaccard similarity")
    void shouldHandleNullInputsInJaccardSimilarity() {
        // When & Then
        assertThat(similarityCalculator.calculateJaccardSimilarity(null, "test")).isEqualTo(0.0);
        assertThat(similarityCalculator.calculateJaccardSimilarity("test", null)).isEqualTo(0.0);
        assertThat(similarityCalculator.calculateJaccardSimilarity(null, null)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate Levenshtein similarity correctly for identical strings")
    void shouldCalculateLevenshteinSimilarityForIdenticalStrings() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Login button not working";

        // When
        double similarity = similarityCalculator.calculateLevenshteinSimilarity(text1, text2);

        // Then
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should calculate Levenshtein similarity correctly for similar strings")
    void shouldCalculateLevenshteinSimilarityForSimilarStrings() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Login button not responding";

        // When
        double similarity = similarityCalculator.calculateLevenshteinSimilarity(text1, text2);

        // Then
        assertThat(similarity).isBetween(0.0, 1.0);
        assertThat(similarity).isGreaterThan(0.7); // Should be quite similar
    }

    @Test
    @DisplayName("Should handle null inputs in Levenshtein similarity")
    void shouldHandleNullInputsInLevenshteinSimilarity() {
        // When & Then
        assertThat(similarityCalculator.calculateLevenshteinSimilarity(null, "test")).isEqualTo(0.0);
        assertThat(similarityCalculator.calculateLevenshteinSimilarity("test", null)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate weighted similarity correctly")
    void shouldCalculateWeightedSimilarityCorrectly() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Login button not responding";

        Map<SimilarityAlgorithm, Double> weights = new HashMap<>();
        weights.put(SimilarityAlgorithm.COSINE, 0.6);
        weights.put(SimilarityAlgorithm.JACCARD, 0.3);
        weights.put(SimilarityAlgorithm.LEVENSHTEIN, 0.1);

        // When
        double weightedSimilarity = similarityCalculator.calculateWeightedSimilarity(text1, text2, weights);

        // Then
        assertThat(weightedSimilarity).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Should handle null weights in weighted similarity")
    void shouldHandleNullWeightsInWeightedSimilarity() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Login button not responding";

        // When & Then
        assertThat(similarityCalculator.calculateWeightedSimilarity(text1, text2, null)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate all similarities correctly")
    void shouldCalculateAllSimilaritiesCorrectly() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Login button not responding";

        // When
        Map<SimilarityAlgorithm, Double> similarities = similarityCalculator.calculateAllSimilarities(text1, text2);

        // Then
        assertThat(similarities).hasSize(3);
        assertThat(similarities).containsKeys(
                SimilarityAlgorithm.COSINE,
                SimilarityAlgorithm.JACCARD,
                SimilarityAlgorithm.LEVENSHTEIN);

        similarities.values().forEach(score -> {
            assertThat(score).isBetween(0.0, 1.0);
        });
    }

    @Test
    @DisplayName("Should validate similarity scores correctly")
    void shouldValidateSimilarityScoresCorrectly() {
        // Given
        Map<SimilarityAlgorithm, Double> validScores = new HashMap<>();
        validScores.put(SimilarityAlgorithm.COSINE, 0.8);
        validScores.put(SimilarityAlgorithm.JACCARD, 0.6);
        validScores.put(SimilarityAlgorithm.LEVENSHTEIN, 0.4);

        Map<SimilarityAlgorithm, Double> invalidScores = new HashMap<>();
        invalidScores.put(SimilarityAlgorithm.COSINE, 1.5); // Invalid: > 1.0
        invalidScores.put(SimilarityAlgorithm.JACCARD, -0.1); // Invalid: < 0.0

        // When & Then
        assertThat(similarityCalculator.validateSimilarityScores(validScores)).isTrue();
        assertThat(similarityCalculator.validateSimilarityScores(invalidScores)).isFalse();
        assertThat(similarityCalculator.validateSimilarityScores(null)).isFalse();
        assertThat(similarityCalculator.validateSimilarityScores(new HashMap<>())).isFalse();
    }

    @Test
    @DisplayName("Should handle performance with large texts")
    void shouldHandlePerformanceWithLargeTexts() {
        // Given
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();

        // Create large texts with 1000 words each
        for (int i = 0; i < 1000; i++) {
            sb1.append("word").append(i).append(" ");
            sb2.append("word").append(i).append(" ");
        }

        String text1 = sb1.toString();
        String text2 = sb2.toString();

        // When
        long startTime = System.currentTimeMillis();
        double similarity = similarityCalculator.calculateCosineSimilarity(text1, text2);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(similarity).isBetween(0.0, 1.0);
        assertThat(endTime - startTime).isLessThan(100); // Should complete within 100ms
    }

    @Test
    @DisplayName("Should calculate similarity using specific algorithm")
    void shouldCalculateSimilarityUsingSpecificAlgorithm() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Login button not responding";

        // When
        double cosineSimilarity = similarityCalculator.calculateSimilarity(text1, text2, SimilarityAlgorithm.COSINE);
        double jaccardSimilarity = similarityCalculator.calculateSimilarity(text1, text2, SimilarityAlgorithm.JACCARD);
        double levenshteinSimilarity = similarityCalculator.calculateSimilarity(text1, text2,
                SimilarityAlgorithm.LEVENSHTEIN);

        // Then
        assertThat(cosineSimilarity).isBetween(0.0, 1.0);
        assertThat(jaccardSimilarity).isBetween(0.0, 1.0);
        assertThat(levenshteinSimilarity).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Should throw exception for null algorithm")
    void shouldThrowExceptionForNullAlgorithm() {
        // Given
        String text1 = "Login button not working";
        String text2 = "Login button not responding";

        // When & Then
        assertThatThrownBy(() -> similarityCalculator.calculateSimilarity(text1, text2, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Algorithm cannot be null");
    }
}
