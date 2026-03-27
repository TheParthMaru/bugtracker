package com.pbm5.bugtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing cached bug similarity calculations.
 * Used for performance optimization to avoid recalculating similarities.
 * 
 * This entity caches similarity scores between bugs using various algorithms
 * (Cosine, Jaccard, Levenshtein) as implemented with Apache Commons Text.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Entity
@Table(name = "bug_similarity_cache", uniqueConstraints = @UniqueConstraint(columnNames = { "bug_id",
        "similar_bug_id" }))
public class BugSimilarityCache {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bug_id", nullable = false)
    @NotNull(message = "Bug is required")
    private Bug bug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "similar_bug_id", nullable = false)
    @NotNull(message = "Similar bug is required")
    private Bug similarBug;

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Similarity score is required")
    @DecimalMin(value = "0.0", message = "Similarity score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Similarity score must be between 0.0 and 1.0")
    private BigDecimal similarityScore;

    @Column(name = "algorithm_used", nullable = false, length = 50)
    @NotBlank(message = "Algorithm is required")
    private String algorithmUsed = "COSINE";

    @Column(name = "text_fingerprint", nullable = false, length = 64)
    @NotBlank(message = "Text fingerprint is required")
    private String textFingerprint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Constructors
    public BugSimilarityCache() {
        // Set default expiration to 7 days from creation
        this.expiresAt = LocalDateTime.now().plusDays(7);
    }

    public BugSimilarityCache(Bug bug, Bug similarBug, BigDecimal similarityScore,
            String algorithmUsed, String textFingerprint) {
        this();
        this.bug = bug;
        this.similarBug = similarBug;
        this.similarityScore = similarityScore;
        this.algorithmUsed = algorithmUsed;
        this.textFingerprint = textFingerprint;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Bug getBug() {
        return bug;
    }

    public void setBug(Bug bug) {
        this.bug = bug;
    }

    public Bug getSimilarBug() {
        return similarBug;
    }

    public void setSimilarBug(Bug similarBug) {
        this.similarBug = similarBug;
    }

    public BigDecimal getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(BigDecimal similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getAlgorithmUsed() {
        return algorithmUsed;
    }

    public void setAlgorithmUsed(String algorithmUsed) {
        this.algorithmUsed = algorithmUsed;
    }

    public String getTextFingerprint() {
        return textFingerprint;
    }

    public void setTextFingerprint(String textFingerprint) {
        this.textFingerprint = textFingerprint;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // Business Methods

    /**
     * Check if this cache entry has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Extend the expiration time by the specified number of days
     */
    public void extendExpiration(int days) {
        this.expiresAt = LocalDateTime.now().plusDays(days);
    }

    /**
     * Get similarity score as double for calculations
     */
    public double getSimilarityScoreAsDouble() {
        return similarityScore.doubleValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BugSimilarityCache that = (BugSimilarityCache) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BugSimilarityCache{" +
                "id=" + id +
                ", bugId=" + (bug != null ? bug.getId() : null) +
                ", similarBugId=" + (similarBug != null ? similarBug.getId() : null) +
                ", similarityScore=" + similarityScore +
                ", algorithmUsed='" + algorithmUsed + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}