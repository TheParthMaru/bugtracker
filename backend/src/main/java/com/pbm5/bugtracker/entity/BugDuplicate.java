package com.pbm5.bugtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing duplicate relationships between bugs.
 * Tracks when bugs are marked as duplicates and by whom.
 * 
 * This entity maintains the relationship between original bugs and their
 * duplicates,
 * including confidence scores and detection methods (manual, automatic, or
 * hybrid).
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Entity
@Table(name = "bug_duplicates", uniqueConstraints = @UniqueConstraint(columnNames = { "original_bug_id",
        "duplicate_bug_id" }))
public class BugDuplicate {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_bug_id", nullable = false)
    @NotNull(message = "Original bug is required")
    private Bug originalBug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duplicate_bug_id", nullable = false)
    @NotNull(message = "Duplicate bug is required")
    private Bug duplicateBug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_user_id", nullable = false)
    @NotNull(message = "User who marked duplicate is required")
    private User markedByUser;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Confidence score is required")
    @DecimalMin(value = "0.0", message = "Confidence score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Confidence score must be between 0.0 and 1.0")
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_method", nullable = false, length = 50)
    @NotNull(message = "Detection method is required")
    private DuplicateDetectionMethod detectionMethod = DuplicateDetectionMethod.MANUAL;

    @CreationTimestamp
    @Column(name = "marked_at", nullable = false, updatable = false)
    private LocalDateTime markedAt;

    @Column(name = "additional_context", columnDefinition = "TEXT")
    private String additionalContext;

    // Constructors
    public BugDuplicate() {
    }

    public BugDuplicate(Bug originalBug, Bug duplicateBug, User markedByUser,
            BigDecimal confidenceScore, DuplicateDetectionMethod detectionMethod) {
        this.originalBug = originalBug;
        this.duplicateBug = duplicateBug;
        this.markedByUser = markedByUser;
        this.confidenceScore = confidenceScore;
        this.detectionMethod = detectionMethod;
    }

    public BugDuplicate(Bug originalBug, Bug duplicateBug, User markedByUser,
            BigDecimal confidenceScore, DuplicateDetectionMethod detectionMethod,
            String additionalContext) {
        this(originalBug, duplicateBug, markedByUser, confidenceScore, detectionMethod);
        this.additionalContext = additionalContext;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Bug getOriginalBug() {
        return originalBug;
    }

    public void setOriginalBug(Bug originalBug) {
        this.originalBug = originalBug;
    }

    public Bug getDuplicateBug() {
        return duplicateBug;
    }

    public void setDuplicateBug(Bug duplicateBug) {
        this.duplicateBug = duplicateBug;
    }

    public User getMarkedByUser() {
        return markedByUser;
    }

    public void setMarkedByUser(User markedByUser) {
        this.markedByUser = markedByUser;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public DuplicateDetectionMethod getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(DuplicateDetectionMethod detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    public String getAdditionalContext() {
        return additionalContext;
    }

    public void setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
    }

    // Business Methods

    /**
     * Get confidence score as double for calculations
     */
    public double getConfidenceScoreAsDouble() {
        return confidenceScore.doubleValue();
    }

    /**
     * Check if this duplicate relationship was detected automatically
     */
    public boolean isAutomaticallyDetected() {
        return detectionMethod == DuplicateDetectionMethod.AUTOMATIC;
    }

    /**
     * Check if this duplicate relationship was marked manually
     */
    public boolean isManuallyMarked() {
        return detectionMethod == DuplicateDetectionMethod.MANUAL;
    }

    /**
     * Check if this duplicate relationship uses hybrid detection
     */
    public boolean isHybridDetection() {
        return detectionMethod == DuplicateDetectionMethod.HYBRID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BugDuplicate that = (BugDuplicate) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BugDuplicate{" +
                "id=" + id +
                ", originalBugId=" + (originalBug != null ? originalBug.getId() : null) +
                ", duplicateBugId=" + (duplicateBug != null ? duplicateBug.getId() : null) +
                ", confidenceScore=" + confidenceScore +
                ", detectionMethod=" + detectionMethod +
                ", markedAt=" + markedAt +
                ", markedByUserId=" + (markedByUser != null ? markedByUser.getId() : null) +
                '}';
    }
}