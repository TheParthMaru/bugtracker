package com.pbm5.bugtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing similarity algorithm configuration for projects.
 * Allows customization of similarity detection parameters per project.
 * 
 * Each project can have different configurations for various similarity
 * algorithms
 * (Cosine, Jaccard, Levenshtein) with custom weights and thresholds.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Entity
@Table(name = "similarity_config", uniqueConstraints = @UniqueConstraint(columnNames = { "project_id",
        "algorithm_name" }))
public class SimilarityConfig {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @NotNull(message = "Project is required")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_name", nullable = false, length = 50)
    @NotNull(message = "Algorithm name is required")
    private SimilarityAlgorithm algorithmName;

    @Column(name = "weight", nullable = false, precision = 3, scale = 2)
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.0", message = "Weight must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Weight must be between 0.0 and 1.0")
    private BigDecimal weight = BigDecimal.valueOf(1.0);

    @Column(name = "threshold", nullable = false, precision = 3, scale = 2)
    @NotNull(message = "Threshold is required")
    @DecimalMin(value = "0.0", message = "Threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Threshold must be between 0.0 and 1.0")
    private BigDecimal threshold = BigDecimal.valueOf(0.75);

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public SimilarityConfig() {
    }

    public SimilarityConfig(Project project, SimilarityAlgorithm algorithmName,
            BigDecimal weight, BigDecimal threshold, boolean isEnabled) {
        this.project = project;
        this.algorithmName = algorithmName;
        this.weight = weight;
        this.threshold = threshold;
        this.isEnabled = isEnabled;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public SimilarityAlgorithm getAlgorithmName() {
        return algorithmName;
    }

    public void setAlgorithmName(SimilarityAlgorithm algorithmName) {
        this.algorithmName = algorithmName;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
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

    // Business Methods

    /**
     * Get weight as double for calculations
     */
    public double getWeightAsDouble() {
        return weight.doubleValue();
    }

    /**
     * Get threshold as double for calculations
     */
    public double getThresholdAsDouble() {
        return threshold.doubleValue();
    }

    /**
     * Enable this algorithm configuration
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * Disable this algorithm configuration
     */
    public void disable() {
        this.isEnabled = false;
    }

    /**
     * Update configuration with new values
     */
    public void updateConfiguration(BigDecimal weight, BigDecimal threshold, boolean isEnabled) {
        this.weight = weight;
        this.threshold = threshold;
        this.isEnabled = isEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SimilarityConfig that = (SimilarityConfig) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SimilarityConfig{" +
                "id=" + id +
                ", projectId=" + (project != null ? project.getId() : null) +
                ", algorithmName=" + algorithmName +
                ", weight=" + weight +
                ", threshold=" + threshold +
                ", isEnabled=" + isEnabled +
                ", updatedAt=" + updatedAt +
                '}';
    }
}