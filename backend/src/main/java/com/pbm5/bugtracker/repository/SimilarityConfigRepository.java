package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.SimilarityAlgorithm;
import com.pbm5.bugtracker.entity.SimilarityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SimilarityConfig entity operations.
 * 
 * Provides data access methods for managing similarity algorithm configurations
 * per project. Includes methods for retrieving algorithm settings, weights,
 * and thresholds used in duplicate bug detection.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Repository
public interface SimilarityConfigRepository extends JpaRepository<SimilarityConfig, UUID> {

    // === Basic Configuration Queries ===

    /**
     * Find all configurations for a specific project
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.project.id = :projectId " +
            "ORDER BY sc.algorithmName")
    List<SimilarityConfig> findByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find enabled configurations for a specific project
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.project.id = :projectId " +
            "AND sc.isEnabled = true ORDER BY sc.algorithmName")
    List<SimilarityConfig> findEnabledByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find configuration for a specific algorithm in a project
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.project.id = :projectId " +
            "AND sc.algorithmName = :algorithm")
    Optional<SimilarityConfig> findByProjectIdAndAlgorithm(@Param("projectId") UUID projectId,
            @Param("algorithm") SimilarityAlgorithm algorithm);

    /**
     * Find enabled configuration for a specific algorithm in a project
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.project.id = :projectId " +
            "AND sc.algorithmName = :algorithm AND sc.isEnabled = true")
    Optional<SimilarityConfig> findEnabledByProjectIdAndAlgorithm(@Param("projectId") UUID projectId,
            @Param("algorithm") SimilarityAlgorithm algorithm);

    // === Algorithm-Specific Queries ===

    /**
     * Find projects using a specific algorithm
     */
    @Query("SELECT DISTINCT sc.project.id FROM SimilarityConfig sc " +
            "WHERE sc.algorithmName = :algorithm AND sc.isEnabled = true")
    List<UUID> findProjectsUsingAlgorithm(@Param("algorithm") SimilarityAlgorithm algorithm);

    /**
     * Find all configurations for a specific algorithm across all projects
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.algorithmName = :algorithm " +
            "ORDER BY sc.project.id, sc.weight DESC")
    List<SimilarityConfig> findByAlgorithm(@Param("algorithm") SimilarityAlgorithm algorithm);

    /**
     * Find configurations above a certain weight threshold
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.project.id = :projectId " +
            "AND sc.weight >= :weightThreshold AND sc.isEnabled = true " +
            "ORDER BY sc.weight DESC")
    List<SimilarityConfig> findByProjectIdAndWeightThreshold(@Param("projectId") UUID projectId,
            @Param("weightThreshold") Double weightThreshold);

    // === Weight and Threshold Queries ===

    /**
     * Get total weight of enabled algorithms for a project
     */
    @Query("SELECT COALESCE(SUM(sc.weight), 0) FROM SimilarityConfig sc " +
            "WHERE sc.project.id = :projectId AND sc.isEnabled = true")
    Double getTotalWeightForProject(@Param("projectId") UUID projectId);

    /**
     * Get configurations ordered by weight (highest first)
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.project.id = :projectId " +
            "AND sc.isEnabled = true ORDER BY sc.weight DESC, sc.algorithmName")
    List<SimilarityConfig> findByProjectIdOrderByWeight(@Param("projectId") UUID projectId);

    /**
     * Find configurations with threshold above a certain value
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.project.id = :projectId " +
            "AND sc.threshold >= :thresholdValue AND sc.isEnabled = true")
    List<SimilarityConfig> findByProjectIdAndThresholdAbove(@Param("projectId") UUID projectId,
            @Param("thresholdValue") Double thresholdValue);

    // === Status and Management Queries ===

    /**
     * Count enabled algorithms for a project
     */
    @Query("SELECT COUNT(sc) FROM SimilarityConfig sc WHERE sc.project.id = :projectId " +
            "AND sc.isEnabled = true")
    long countEnabledByProjectId(@Param("projectId") UUID projectId);

    /**
     * Check if any algorithms are enabled for a project
     */
    @Query("SELECT COUNT(sc) > 0 FROM SimilarityConfig sc WHERE sc.project.id = :projectId " +
            "AND sc.isEnabled = true")
    boolean hasEnabledAlgorithms(@Param("projectId") UUID projectId);

    /**
     * Find projects with no enabled algorithms
     */
    @Query("SELECT DISTINCT p.id FROM Project p WHERE p.id NOT IN " +
            "(SELECT DISTINCT sc.project.id FROM SimilarityConfig sc WHERE sc.isEnabled = true)")
    List<UUID> findProjectsWithoutEnabledAlgorithms();

    // === Configuration Statistics ===

    /**
     * Get average weights by algorithm across all projects
     */
    @Query("SELECT sc.algorithmName, AVG(sc.weight), COUNT(sc) FROM SimilarityConfig sc " +
            "WHERE sc.isEnabled = true GROUP BY sc.algorithmName ORDER BY AVG(sc.weight) DESC")
    List<Object[]> getAverageWeightsByAlgorithm();

    /**
     * Get average thresholds by algorithm across all projects
     */
    @Query("SELECT sc.algorithmName, AVG(sc.threshold), COUNT(sc) FROM SimilarityConfig sc " +
            "WHERE sc.isEnabled = true GROUP BY sc.algorithmName ORDER BY AVG(sc.threshold) DESC")
    List<Object[]> getAverageThresholdsByAlgorithm();

    /**
     * Find most commonly used algorithm (highest count of enabled configurations)
     */
    @Query("SELECT sc.algorithmName, COUNT(sc) as configCount FROM SimilarityConfig sc " +
            "WHERE sc.isEnabled = true GROUP BY sc.algorithmName ORDER BY COUNT(sc) DESC")
    List<Object[]> getMostUsedAlgorithms();

    // === Default Configuration Management ===

    /**
     * Check if project has default configurations
     */
    @Query("SELECT COUNT(sc) = 3 FROM SimilarityConfig sc WHERE sc.project.id = :projectId")
    boolean hasDefaultConfigurations(@Param("projectId") UUID projectId);

    /**
     * Find projects missing default configurations
     */
    @Query("SELECT p.id FROM Project p WHERE " +
            "(SELECT COUNT(sc) FROM SimilarityConfig sc WHERE sc.project.id = p.id) < 3")
    List<UUID> findProjectsMissingConfigurations();

    /**
     * Get default configuration template
     */
    @Query("SELECT sc.algorithmName, sc.weight, sc.threshold FROM SimilarityConfig sc " +
            "WHERE sc.project.id = :templateProjectId ORDER BY sc.algorithmName")
    List<Object[]> getConfigurationTemplate(@Param("templateProjectId") UUID templateProjectId);

    // === Bulk Operations ===

    /**
     * Find all configurations that need updates (older than specific date)
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.updatedAt < :cutoffDate")
    List<SimilarityConfig> findConfigurationsNeedingUpdate(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * Find configurations with unusual weights (outside standard range)
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.isEnabled = true " +
            "AND (sc.weight < 0.05 OR sc.weight > 0.95)")
    List<SimilarityConfig> findConfigurationsWithUnusualWeights();

    /**
     * Find configurations with unusual thresholds (outside standard range)
     */
    @Query("SELECT sc FROM SimilarityConfig sc WHERE sc.isEnabled = true " +
            "AND (sc.threshold < 0.3 OR sc.threshold > 0.95)")
    List<SimilarityConfig> findConfigurationsWithUnusualThresholds();

    // === Validation Queries ===

    /**
     * Check for duplicate configurations (same project and algorithm)
     */
    @Query("SELECT sc.project.id, sc.algorithmName, COUNT(sc) FROM SimilarityConfig sc " +
            "GROUP BY sc.project.id, sc.algorithmName HAVING COUNT(sc) > 1")
    List<Object[]> findDuplicateConfigurations();

    /**
     * Validate weight sum for projects (should typically sum to 1.0)
     */
    @Query("SELECT sc.project.id, SUM(sc.weight) FROM SimilarityConfig sc " +
            "WHERE sc.isEnabled = true GROUP BY sc.project.id " +
            "HAVING ABS(SUM(sc.weight) - 1.0) > 0.1")
    List<Object[]> findProjectsWithInvalidWeightSums();
}