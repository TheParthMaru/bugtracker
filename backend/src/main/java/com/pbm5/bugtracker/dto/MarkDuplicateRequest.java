package com.pbm5.bugtracker.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for marking a bug as duplicate of another bug.
 * 
 * This DTO is used when a user manually marks a bug as a duplicate
 * of an existing bug, or when the system automatically detects duplicates
 * with high confidence.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public class MarkDuplicateRequest {

    @NotNull(message = "Original bug ID is required")
    private Long originalBugId;

    @DecimalMin(value = "0.0", message = "Confidence score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Confidence score must be between 0.0 and 1.0")
    private Double confidenceScore;

    @Size(max = 1000, message = "Additional context must not exceed 1000 characters")
    private String additionalContext;

    private Boolean isAutomaticDetection = false;

    // Constructors
    public MarkDuplicateRequest() {
    }

    public MarkDuplicateRequest(Long originalBugId) {
        this.originalBugId = originalBugId;
    }

    public MarkDuplicateRequest(Long originalBugId, Double confidenceScore) {
        this.originalBugId = originalBugId;
        this.confidenceScore = confidenceScore;
    }

    public MarkDuplicateRequest(Long originalBugId, Double confidenceScore, String additionalContext) {
        this.originalBugId = originalBugId;
        this.confidenceScore = confidenceScore;
        this.additionalContext = additionalContext;
    }

    // Getters and Setters
    public Long getOriginalBugId() {
        return originalBugId;
    }

    public void setOriginalBugId(Long originalBugId) {
        this.originalBugId = originalBugId;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getAdditionalContext() {
        return additionalContext;
    }

    public void setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
    }

    public Boolean getIsAutomaticDetection() {
        return isAutomaticDetection;
    }

    public void setIsAutomaticDetection(Boolean automaticDetection) {
        isAutomaticDetection = automaticDetection;
    }

    // Utility Methods

    /**
     * Get effective confidence score (use provided or default based on detection
     * type)
     */
    public double getEffectiveConfidenceScore() {
        if (confidenceScore != null) {
            return confidenceScore;
        }
        // Default confidence based on detection type
        return Boolean.TRUE.equals(isAutomaticDetection) ? 0.85 : 0.95;
    }

    /**
     * Check if this is a high-confidence duplicate marking
     */
    public boolean isHighConfidence() {
        return getEffectiveConfidenceScore() >= 0.8;
    }

    /**
     * Check if additional context is provided
     */
    public boolean hasAdditionalContext() {
        return additionalContext != null && !additionalContext.trim().isEmpty();
    }

    /**
     * Get trimmed additional context
     */
    public String getTrimmedAdditionalContext() {
        return additionalContext != null ? additionalContext.trim() : null;
    }

    @Override
    public String toString() {
        return "MarkDuplicateRequest{" +
                "originalBugId=" + originalBugId +
                ", confidenceScore=" + confidenceScore +
                ", hasAdditionalContext=" + hasAdditionalContext() +
                ", isAutomaticDetection=" + isAutomaticDetection +
                '}';
    }
}