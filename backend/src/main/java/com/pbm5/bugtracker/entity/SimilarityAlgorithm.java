package com.pbm5.bugtracker.entity;

/**
 * Enumeration representing different text similarity algorithms.
 * 
 * This enum defines the available algorithms for calculating text similarity
 * between bug reports, each with different strengths and use cases:
 * - COSINE: Vector-based similarity using TF-IDF
 * - JACCARD: Set-based similarity for keyword overlap
 * - LEVENSHTEIN: Character-level edit distance similarity
 * 
 * All algorithms are implemented using Apache Commons Text library.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public enum SimilarityAlgorithm {

    /**
     * Cosine similarity using TF-IDF vectors
     * Best for: Overall semantic similarity, handling different document lengths
     */
    COSINE("Cosine Similarity", "Vector-based similarity using TF-IDF", 0.6, 0.75),

    /**
     * Jaccard similarity for set overlap
     * Best for: Keyword matching, shared terminology detection
     */
    JACCARD("Jaccard Similarity", "Set-based overlap similarity", 0.3, 0.5),

    /**
     * Levenshtein distance converted to similarity
     * Best for: Detecting typos, minor text variations
     */
    LEVENSHTEIN("Levenshtein Similarity", "Character-level edit distance similarity", 0.1, 0.8);

    private final String displayName;
    private final String description;
    private final double defaultWeight;
    private final double defaultThreshold;

    SimilarityAlgorithm(String displayName, String description,
            double defaultWeight, double defaultThreshold) {
        this.displayName = displayName;
        this.description = description;
        this.defaultWeight = defaultWeight;
        this.defaultThreshold = defaultThreshold;
    }

    /**
     * Get human-readable display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get description of the algorithm
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get default weight for this algorithm in combined scoring
     */
    public double getDefaultWeight() {
        return defaultWeight;
    }

    /**
     * Get default threshold for similarity detection
     */
    public double getDefaultThreshold() {
        return defaultThreshold;
    }

    /**
     * Check if this is the primary algorithm (highest default weight)
     */
    public boolean isPrimary() {
        return this == COSINE;
    }

    /**
     * Check if this algorithm is character-based
     */
    public boolean isCharacterBased() {
        return this == LEVENSHTEIN;
    }

    /**
     * Check if this algorithm is token-based
     */
    public boolean isTokenBased() {
        return this == JACCARD;
    }

    /**
     * Check if this algorithm is vector-based
     */
    public boolean isVectorBased() {
        return this == COSINE;
    }

    /**
     * Get enum from string value (case-insensitive)
     */
    public static SimilarityAlgorithm fromString(String value) {
        if (value == null) {
            return COSINE; // Default to cosine
        }

        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COSINE; // Default to cosine if invalid value
        }
    }

    /**
     * Get all algorithms ordered by their default weights (highest first)
     */
    public static SimilarityAlgorithm[] getByWeightOrder() {
        return new SimilarityAlgorithm[] { COSINE, JACCARD, LEVENSHTEIN };
    }
}