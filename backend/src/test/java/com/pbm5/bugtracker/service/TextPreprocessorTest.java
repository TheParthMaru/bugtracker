package com.pbm5.bugtracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TextPreprocessor service.
 * 
 * Tests text cleaning, tokenization, and preprocessing capabilities
 * required for similarity calculations using Apache Commons Text.
 * 
 * This is a critical component of the duplicate detection system.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TextPreprocessor Tests")
class TextPreprocessorTest {

    @InjectMocks
    private TextPreprocessor textPreprocessor;

    @Test
    @DisplayName("Should preprocess text correctly")
    void shouldPreprocessTextCorrectly() {
        // Given
        String input = "Login button NOT working! Please fix this issue.";
        String expected = "login button not working please fix this issue";

        // When
        String result = textPreprocessor.preprocessText(input);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should handle null and empty inputs")
    void shouldHandleNullAndEmptyInputs() {
        // When & Then
        assertThat(textPreprocessor.preprocessText(null)).isEmpty();
        assertThat(textPreprocessor.preprocessText("")).isEmpty();
        assertThat(textPreprocessor.preprocessText("   ")).isEmpty();
    }

    @Test
    @DisplayName("Should remove special characters and normalize whitespace")
    void shouldRemoveSpecialCharactersAndNormalizeWhitespace() {
        // Given
        String input = "Login@#$%button    not\t\nworking!!!";
        String expected = "login button not working";

        // When
        String result = textPreprocessor.preprocessText(input);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should remove code snippets from text")
    void shouldRemoveCodeSnippetsFromText() {
        // Given
        String input = "The login button is not working. Here's the code: `function test() { return null; }` Please fix it.";
        String expected = "the login button is not working here s the code please fix it";

        // When
        String result = textPreprocessor.preprocessText(input);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should remove URLs and email addresses")
    void shouldRemoveUrlsAndEmailAddresses() {
        // Given
        String input = "Login issue at https://example.com. Contact user@example.com for details.";
        String expected = "login issue at contact for details";

        // When
        String result = textPreprocessor.preprocessText(input);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should tokenize and filter text correctly")
    void shouldTokenizeAndFilterTextCorrectly() {
        // Given
        String input = "The login button is not working properly";

        // When
        List<String> tokens = textPreprocessor.tokenizeAndFilter(input);

        // Then
        assertThat(tokens).containsExactly("login", "button", "not", "working", "properly");
        assertThat(tokens).doesNotContain("the", "is"); // Stop words removed
        // Note: "not" is not in the stop words list in the actual implementation
    }

    @Test
    @DisplayName("Should handle null input in tokenization")
    void shouldHandleNullInputInTokenization() {
        // When
        List<String> tokens = textPreprocessor.tokenizeAndFilter(null);

        // Then
        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("Should filter out very short tokens")
    void shouldFilterOutVeryShortTokens() {
        // Given
        String input = "A B C login button working";

        // When
        List<String> tokens = textPreprocessor.tokenizeAndFilter(input);

        // Then
        assertThat(tokens).containsExactly("login", "button", "working");
        assertThat(tokens).doesNotContain("a", "b", "c"); // Short tokens removed
    }

    @Test
    @DisplayName("Should filter out pure numbers")
    void shouldFilterOutPureNumbers() {
        // Given
        String input = "Login button 123 not working 456 properly";

        // When
        List<String> tokens = textPreprocessor.tokenizeAndFilter(input);

        // Then
        assertThat(tokens).containsExactly("login", "button", "not", "working", "properly");
        assertThat(tokens).doesNotContain("123", "456"); // Pure numbers removed
    }

    @Test
    @DisplayName("Should calculate term frequency correctly")
    void shouldCalculateTermFrequencyCorrectly() {
        // Given
        List<String> tokens = List.of("login", "button", "login", "working", "button", "login");

        // When
        Map<String, Double> termFrequency = textPreprocessor.calculateTermFrequency(tokens);

        // Then
        assertThat(termFrequency).hasSize(3);
        assertThat(termFrequency.get("login")).isEqualTo(0.5); // 3/6
        assertThat(termFrequency.get("button")).isEqualTo(1.0 / 3); // 2/6
        assertThat(termFrequency.get("working")).isEqualTo(1.0 / 6); // 1/6
    }

    @Test
    @DisplayName("Should handle empty tokens in term frequency calculation")
    void shouldHandleEmptyTokensInTermFrequencyCalculation() {
        // Given
        List<String> tokens = List.of();

        // When
        Map<String, Double> termFrequency = textPreprocessor.calculateTermFrequency(tokens);

        // Then
        assertThat(termFrequency).isEmpty();
    }

    @Test
    @DisplayName("Should calculate inverse document frequency correctly")
    void shouldCalculateInverseDocumentFrequencyCorrectly() {
        // Given
        List<String> corpus = List.of(
                "login button not working",
                "database connection timeout",
                "user authentication failed");

        // When
        Map<String, Double> idfMap = textPreprocessor.calculateInverseDocumentFrequency(corpus);

        // Then
        assertThat(idfMap).isNotEmpty();
        assertThat(idfMap.values()).allMatch(score -> score >= 0.0);
    }

    @Test
    @DisplayName("Should handle empty corpus in IDF calculation")
    void shouldHandleEmptyCorpusInIdfCalculation() {
        // Given
        List<String> corpus = List.of();

        // When
        Map<String, Double> idfMap = textPreprocessor.calculateInverseDocumentFrequency(corpus);

        // Then
        assertThat(idfMap).isEmpty();
    }

    @Test
    @DisplayName("Should generate TF-IDF vector correctly")
    void shouldGenerateTfIdfVectorCorrectly() {
        // Given
        String text = "login button not working";
        List<String> corpus = List.of(
                "login button not working",
                "database connection timeout",
                "user authentication failed");

        // When
        Map<String, Double> tfidfVector = textPreprocessor.generateTfIdfVector(text, corpus);

        // Then
        assertThat(tfidfVector).isNotEmpty();
        assertThat(tfidfVector.values()).allMatch(score -> score >= 0.0);
    }

    @Test
    @DisplayName("Should handle empty text in TF-IDF generation")
    void shouldHandleEmptyTextInTfIdfGeneration() {
        // Given
        String text = "";
        List<String> corpus = List.of("login button not working");

        // When
        Map<String, Double> tfidfVector = textPreprocessor.generateTfIdfVector(text, corpus);

        // Then
        assertThat(tfidfVector).isEmpty();
    }

    @Test
    @DisplayName("Should create character frequency map correctly")
    void shouldCreateCharacterFrequencyMapCorrectly() {
        // Given
        String text = "login button working";

        // When
        Map<CharSequence, Integer> frequencyMap = textPreprocessor.createCharacterFrequencyMap(text);

        // Then
        assertThat(frequencyMap).hasSize(3);
        assertThat(frequencyMap.get("login")).isEqualTo(1);
        assertThat(frequencyMap.get("button")).isEqualTo(1);
        assertThat(frequencyMap.get("working")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle empty text in character frequency map")
    void shouldHandleEmptyTextInCharacterFrequencyMap() {
        // Given
        String text = "";

        // When
        Map<CharSequence, Integer> frequencyMap = textPreprocessor.createCharacterFrequencyMap(text);

        // Then
        assertThat(frequencyMap).isEmpty();
    }

    @Test
    @DisplayName("Should generate text fingerprint correctly")
    void shouldGenerateTextFingerprintCorrectly() {
        // Given
        String title = "Login Issue";
        String description = "Button not working";

        // When
        String fingerprint1 = textPreprocessor.generateTextFingerprint(title, description);
        String fingerprint2 = textPreprocessor.generateTextFingerprint(title, description);

        // Then
        assertThat(fingerprint1).isNotEmpty();
        assertThat(fingerprint1).isEqualTo(fingerprint2); // Should be consistent
    }

    @Test
    @DisplayName("Should generate different fingerprints for different texts")
    void shouldGenerateDifferentFingerprintsForDifferentTexts() {
        // Given
        String title1 = "Login Issue";
        String description1 = "Button not working";
        String title2 = "Database Issue";
        String description2 = "Connection timeout";

        // When
        String fingerprint1 = textPreprocessor.generateTextFingerprint(title1, description1);
        String fingerprint2 = textPreprocessor.generateTextFingerprint(title2, description2);

        // Then
        assertThat(fingerprint1).isNotEqualTo(fingerprint2);
    }

    @Test
    @DisplayName("Should check keyword overlap correctly")
    void shouldCheckKeywordOverlapCorrectly() {
        // Given
        String text1 = "login button not working";
        String text2 = "login button not responding";
        List<String> corpus = List.of(text1, text2, "database connection timeout");

        // When
        boolean hasOverlap = textPreprocessor.hasSignificantKeywordOverlap(text1, text2, corpus, 0.5);

        // Then
        assertThat(hasOverlap).isTrue(); // Should have significant overlap
    }

    @Test
    @DisplayName("Should return false for insufficient keyword overlap")
    void shouldReturnFalseForInsufficientKeywordOverlap() {
        // Given
        String text1 = "login button not working";
        String text2 = "database connection timeout";
        List<String> corpus = List.of(text1, text2, "user authentication failed");

        // When
        boolean hasOverlap = textPreprocessor.hasSignificantKeywordOverlap(text1, text2, corpus, 0.5);

        // Then
        assertThat(hasOverlap).isFalse(); // Should not have significant overlap
    }

    @Test
    @DisplayName("Should get preprocessing statistics correctly")
    void shouldGetPreprocessingStatisticsCorrectly() {
        // Given
        String originalText = "The login button is not working properly";

        // When
        Map<String, Object> stats = textPreprocessor.getPreprocessingStats(originalText);

        // Then
        assertThat(stats).containsKeys(
                "originalLength", "processedLength", "originalWordCount",
                "tokenCount", "uniqueTokenCount", "stopWordsRemoved");
        assertThat(stats.get("originalLength")).isEqualTo(originalText.length());
        assertThat(stats.get("tokenCount")).isInstanceOf(Integer.class);
        assertThat(stats.get("uniqueTokenCount")).isInstanceOf(Integer.class);
    }

    @Test
    @DisplayName("Should handle null input in preprocessing statistics")
    void shouldHandleNullInputInPreprocessingStatistics() {
        // When
        Map<String, Object> stats = textPreprocessor.getPreprocessingStats(null);

        // Then
        assertThat(stats).containsKeys(
                "originalLength", "processedLength", "originalWordCount",
                "tokenCount", "uniqueTokenCount", "stopWordsRemoved");
        assertThat(stats.get("originalLength")).isEqualTo(0);
        assertThat(stats.get("processedLength")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should extract keywords correctly")
    void shouldExtractKeywordsCorrectly() {
        // Given
        String text = "login button not working properly";
        List<String> corpus = List.of(text, "database connection timeout", "user authentication failed");
        int maxKeywords = 3;

        // When
        List<String> keywords = textPreprocessor.extractKeywords(text, corpus, maxKeywords);

        // Then
        assertThat(keywords).hasSizeLessThanOrEqualTo(maxKeywords);
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle empty text in keyword extraction")
    void shouldHandleEmptyTextInKeywordExtraction() {
        // Given
        String text = "";
        List<String> corpus = List.of("login button not working");
        int maxKeywords = 3;

        // When
        List<String> keywords = textPreprocessor.extractKeywords(text, corpus, maxKeywords);

        // Then
        assertThat(keywords).isEmpty();
    }
}
