package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.entity.SimilarityAlgorithm;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core similarity calculation engine using Apache Commons Text algorithms.
 * 
 * This service implements various text similarity algorithms optimized for
 * bug report comparison. It uses industry-standard implementations from
 * Apache Commons Text library for reliability and performance.
 * 
 * Supported Algorithms:
 * - Cosine Similarity: Vector-based similarity using term frequencies
 * - Jaccard Similarity: Set-based overlap similarity
 * - Levenshtein Distance: Character-level edit distance similarity
 * 
 * Citations:
 * - Apache Software Foundation. (2024). Apache Commons Text.
 * https://commons.apache.org/proper/commons-text/
 * - Salton, G., & McGill, M. J. (1983). Introduction to Modern Information
 * Retrieval.
 * - Manning, C. D., Raghavan, P., & Schütze, H. (2008). Introduction to
 * Information Retrieval.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Component
public class SimilarityCalculator {

    @Autowired
    private TextPreprocessor textPreprocessor;

    // Algorithm instances from Apache Commons Text
    private final CosineSimilarity cosineSimilarity = new CosineSimilarity();
    private final JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    /**
     * Calculate cosine similarity using Apache Commons Text.
     * 
     * Cosine similarity measures the angle between two vectors in a
     * multi-dimensional space.
     * It's particularly effective for comparing documents of different lengths as
     * it
     * normalizes for document length.
     * 
     * Citation: Apache Software Foundation. (2024). "CosineSimilarity Class
     * Documentation."
     * Apache Commons Text API.
     * https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/similarity/CosineSimilarity.html
     * 
     * @param text1 First text to compare
     * @param text2 Second text to compare
     * @return Cosine similarity score (0.0 to 1.0)
     */
    public double calculateCosineSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.trim().isEmpty() || text2.trim().isEmpty()) {
            return 0.0;
        }

        // Preprocess texts
        String processed1 = textPreprocessor.preprocessText(text1);
        String processed2 = textPreprocessor.preprocessText(text2);

        if (processed1.isEmpty() || processed2.isEmpty()) {
            return 0.0;
        }

        // Create frequency maps for Apache Commons Text CosineSimilarity
        Map<CharSequence, Integer> vector1 = textPreprocessor.createCharacterFrequencyMap(processed1);
        Map<CharSequence, Integer> vector2 = textPreprocessor.createCharacterFrequencyMap(processed2);

        if (vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        // Use Apache Commons Text CosineSimilarity
        return cosineSimilarity.cosineSimilarity(vector1, vector2);
    }

    /**
     * Calculate Jaccard similarity using Apache Commons Text.
     * 
     * Jaccard similarity measures the overlap between two sets by dividing
     * the intersection by the union. It's effective for keyword-based comparison
     * and shared terminology detection.
     * 
     * Citation: Apache Software Foundation. (2024). "JaccardSimilarity Class
     * Documentation."
     * Apache Commons Text API.
     * https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/similarity/JaccardSimilarity.html
     * 
     * @param text1 First text to compare
     * @param text2 Second text to compare
     * @return Jaccard similarity score (0.0 to 1.0)
     */
    public double calculateJaccardSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.trim().isEmpty() || text2.trim().isEmpty()) {
            return 0.0;
        }

        // Preprocess texts
        String processed1 = textPreprocessor.preprocessText(text1);
        String processed2 = textPreprocessor.preprocessText(text2);

        if (processed1.isEmpty() || processed2.isEmpty()) {
            return 0.0;
        }

        // Use Apache Commons Text JaccardSimilarity
        return jaccardSimilarity.apply(processed1, processed2);
    }

    /**
     * Calculate Levenshtein similarity using Apache Commons Text.
     * 
     * Levenshtein distance measures the minimum number of single-character edits
     * required to transform one string into another. This method converts the
     * distance to a similarity score (0.0 to 1.0).
     * 
     * Citation: Apache Software Foundation. (2024). "LevenshteinDistance Class
     * Documentation."
     * Apache Commons Text API.
     * https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/similarity/LevenshteinDistance.html
     * 
     * @param text1 First text to compare
     * @param text2 Second text to compare
     * @return Levenshtein similarity score (0.0 to 1.0)
     */
    public double calculateLevenshteinSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        if (text1.equals(text2)) {
            return 1.0;
        }

        // Preprocess texts
        String processed1 = textPreprocessor.preprocessText(text1);
        String processed2 = textPreprocessor.preprocessText(text2);

        if (processed1.isEmpty() && processed2.isEmpty()) {
            return 1.0;
        }

        if (processed1.isEmpty() || processed2.isEmpty()) {
            return 0.0;
        }

        // Use Apache Commons Text LevenshteinDistance
        int distance = levenshteinDistance.apply(processed1, processed2);
        int maxLength = Math.max(processed1.length(), processed2.length());

        // Convert distance to similarity: similarity = 1 - (distance / max_length)
        return maxLength == 0 ? 1.0 : 1.0 - (double) distance / maxLength;
    }

    /**
     * Calculate weighted combination of all similarity measures.
     * 
     * This method combines multiple similarity algorithms using configurable
     * weights
     * to produce a comprehensive similarity score that leverages the strengths
     * of different approaches.
     * 
     * @param text1   First text to compare
     * @param text2   Second text to compare
     * @param weights Map of algorithm weights (should sum to 1.0)
     * @return Combined weighted similarity score (0.0 to 1.0)
     */
    public double calculateWeightedSimilarity(String text1, String text2, Map<SimilarityAlgorithm, Double> weights) {
        if (text1 == null || text2 == null || weights == null || weights.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<SimilarityAlgorithm, Double> entry : weights.entrySet()) {
            SimilarityAlgorithm algorithm = entry.getKey();
            double weight = entry.getValue();

            if (weight <= 0.0) {
                continue; // Skip disabled algorithms
            }

            double score = calculateSimilarity(text1, text2, algorithm);
            totalScore += score * weight;
            totalWeight += weight;
        }

        // Normalize by total weight (in case weights don't sum to 1.0)
        return totalWeight > 0.0 ? totalScore / totalWeight : 0.0;
    }

    /**
     * Calculate similarity using a specific algorithm.
     * 
     * @param text1     First text to compare
     * @param text2     Second text to compare
     * @param algorithm Algorithm to use
     * @return Similarity score (0.0 to 1.0)
     */
    public double calculateSimilarity(String text1, String text2, SimilarityAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }

        return switch (algorithm) {
            case COSINE -> calculateCosineSimilarity(text1, text2);
            case JACCARD -> calculateJaccardSimilarity(text1, text2);
            case LEVENSHTEIN -> calculateLevenshteinSimilarity(text1, text2);
        };
    }

    /**
     * Calculate all similarity scores for two texts.
     * 
     * @param text1 First text to compare
     * @param text2 Second text to compare
     * @return Map of all similarity scores by algorithm
     */
    public Map<SimilarityAlgorithm, Double> calculateAllSimilarities(String text1, String text2) {
        Map<SimilarityAlgorithm, Double> scores = new HashMap<>();

        for (SimilarityAlgorithm algorithm : SimilarityAlgorithm.values()) {
            double score = calculateSimilarity(text1, text2, algorithm);
            scores.put(algorithm, score);
        }

        return scores;
    }

    /**
     * Quick similarity check using keyword overlap as a pre-filter.
     * This method is much faster than full similarity calculation and can be
     * used to quickly eliminate obviously dissimilar texts.
     * 
     * @param text1     First text to compare
     * @param text2     Second text to compare
     * @param corpus    Corpus for keyword extraction
     * @param threshold Minimum keyword overlap ratio
     * @return true if texts pass the quick similarity check
     */
    public boolean passesQuickSimilarityCheck(String text1, String text2, List<String> corpus, double threshold) {
        return textPreprocessor.hasSignificantKeywordOverlap(text1, text2, corpus, threshold);
    }

    /**
     * Calculate similarity with performance optimization.
     * Uses quick pre-filtering to avoid expensive calculations for obviously
     * dissimilar texts.
     * 
     * @param text1               First text to compare
     * @param text2               Second text to compare
     * @param corpus              Corpus for optimization
     * @param algorithm           Algorithm to use
     * @param quickCheckThreshold Threshold for quick pre-filtering
     * @return Similarity score (0.0 to 1.0)
     */
    public double calculateOptimizedSimilarity(String text1, String text2, List<String> corpus,
            SimilarityAlgorithm algorithm, double quickCheckThreshold) {
        // Quick pre-filter using keyword overlap
        if (!passesQuickSimilarityCheck(text1, text2, corpus, quickCheckThreshold)) {
            return 0.0; // Early exit for obviously dissimilar texts
        }

        // Perform full similarity calculation
        return calculateSimilarity(text1, text2, algorithm);
    }

    /**
     * Get similarity calculation statistics for monitoring and debugging.
     * 
     * @param text1 First text
     * @param text2 Second text
     * @return Map containing calculation statistics
     */
    public Map<String, Object> getSimilarityStats(String text1, String text2) {
        Map<String, Object> stats = new HashMap<>();

        // Preprocessing stats
        Map<String, Object> preprocessStats1 = textPreprocessor.getPreprocessingStats(text1);
        Map<String, Object> preprocessStats2 = textPreprocessor.getPreprocessingStats(text2);

        stats.put("text1Stats", preprocessStats1);
        stats.put("text2Stats", preprocessStats2);

        // Algorithm scores
        Map<SimilarityAlgorithm, Double> allScores = calculateAllSimilarities(text1, text2);
        stats.put("algorithmScores", allScores);

        // Text fingerprints
        String fingerprint1 = textPreprocessor.generateTextFingerprint(text1, "");
        String fingerprint2 = textPreprocessor.generateTextFingerprint(text2, "");
        stats.put("fingerprint1", fingerprint1);
        stats.put("fingerprint2", fingerprint2);
        stats.put("fingerprintMatch", fingerprint1.equals(fingerprint2));

        return stats;
    }

    /**
     * Validate similarity calculation results for quality assurance.
     * 
     * @param scores Map of similarity scores
     * @return true if all scores are valid (between 0.0 and 1.0)
     */
    public boolean validateSimilarityScores(Map<SimilarityAlgorithm, Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return false;
        }

        for (Double score : scores.values()) {
            if (score == null || score < 0.0 || score > 1.0 || score.isNaN() || score.isInfinite()) {
                return false;
            }
        }

        return true;
    }
}