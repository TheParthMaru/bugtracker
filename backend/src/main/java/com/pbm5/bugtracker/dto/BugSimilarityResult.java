package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.BugPriority;
import com.pbm5.bugtracker.entity.BugStatus;
import com.pbm5.bugtracker.entity.SimilarityAlgorithm;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for bug similarity results.
 * 
 * This DTO contains information about bugs that are similar to a query bug,
 * including similarity scores, metadata, and algorithm-specific details.
 * Used in API responses for duplicate detection features.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public class BugSimilarityResult {

    private Long bugId;
    private Integer projectTicketNumber;
    private String title;
    private String description;
    private double similarityScore;
    private BugStatus status;
    private BugPriority priority;
    private String assigneeName;
    private String reporterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<SimilarityAlgorithm, Double> algorithmScores;
    private String textFingerprint;
    private boolean isAlreadyMarkedDuplicate;
    private Long originalBugId; // If this bug is already marked as duplicate

    // Constructors
    public BugSimilarityResult() {
    }

    public BugSimilarityResult(Long bugId, String title, String description, double similarityScore) {
        this.bugId = bugId;
        this.title = title;
        this.description = description;
        this.similarityScore = similarityScore;
    }

    // Getters and Setters
    public Long getBugId() {
        return bugId;
    }

    public void setBugId(Long bugId) {
        this.bugId = bugId;
    }

    public Integer getProjectTicketNumber() {
        return projectTicketNumber;
    }

    public void setProjectTicketNumber(Integer projectTicketNumber) {
        this.projectTicketNumber = projectTicketNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public BugStatus getStatus() {
        return status;
    }

    public void setStatus(BugStatus status) {
        this.status = status;
    }

    public BugPriority getPriority() {
        return priority;
    }

    public void setPriority(BugPriority priority) {
        this.priority = priority;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<SimilarityAlgorithm, Double> getAlgorithmScores() {
        return algorithmScores;
    }

    public void setAlgorithmScores(Map<SimilarityAlgorithm, Double> algorithmScores) {
        this.algorithmScores = algorithmScores;
    }

    public String getTextFingerprint() {
        return textFingerprint;
    }

    public void setTextFingerprint(String textFingerprint) {
        this.textFingerprint = textFingerprint;
    }

    public boolean isAlreadyMarkedDuplicate() {
        return isAlreadyMarkedDuplicate;
    }

    public void setAlreadyMarkedDuplicate(boolean alreadyMarkedDuplicate) {
        isAlreadyMarkedDuplicate = alreadyMarkedDuplicate;
    }

    public Long getOriginalBugId() {
        return originalBugId;
    }

    public void setOriginalBugId(Long originalBugId) {
        this.originalBugId = originalBugId;
    }

    // Utility Methods

    /**
     * Get similarity score as percentage
     */
    public double getSimilarityPercentage() {
        return similarityScore * 100.0;
    }

    /**
     * Check if similarity is above a threshold
     */
    public boolean isAboveThreshold(double threshold) {
        return similarityScore >= threshold;
    }

    /**
     * Check if this is a high similarity match (>= 80%)
     */
    public boolean isHighSimilarity() {
        return isAboveThreshold(0.8);
    }

    /**
     * Check if this is a medium similarity match (60-80%)
     */
    public boolean isMediumSimilarity() {
        return similarityScore >= 0.6 && similarityScore < 0.8;
    }

    /**
     * Check if this is a low similarity match (< 60%)
     */
    public boolean isLowSimilarity() {
        return similarityScore < 0.6;
    }

    /**
     * Get similarity category as string
     */
    public String getSimilarityCategory() {
        if (isHighSimilarity()) {
            return "HIGH";
        } else if (isMediumSimilarity()) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Get formatted display name for the bug
     */
    public String getDisplayName() {
        return String.format("#%d: %s", projectTicketNumber != null ? projectTicketNumber : bugId, title);
    }

    @Override
    public String toString() {
        return "BugSimilarityResult{" +
                "bugId=" + bugId +
                ", projectTicketNumber=" + projectTicketNumber +
                ", title='" + title + '\'' +
                ", similarityScore=" + similarityScore +
                ", status=" + status +
                ", priority=" + priority +
                ", createdAt=" + createdAt +
                ", isAlreadyMarkedDuplicate=" + isAlreadyMarkedDuplicate +
                '}';
    }
}