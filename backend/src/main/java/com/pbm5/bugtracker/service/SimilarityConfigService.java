package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.entity.Project;
import com.pbm5.bugtracker.entity.SimilarityAlgorithm;
import com.pbm5.bugtracker.entity.SimilarityConfig;
import com.pbm5.bugtracker.repository.SimilarityConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing similarity algorithm configurations.
 * 
 * This service handles the creation, updating, and management of similarity
 * algorithm configurations for projects. It ensures that new projects have
 * default configurations and provides utilities for configuration management.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@Transactional
public class SimilarityConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SimilarityConfigService.class);

    @Autowired
    private SimilarityConfigRepository similarityConfigRepository;

    /**
     * Initialize default similarity configurations for a new project.
     * This method should be called when a new project is created.
     * 
     * @param project The newly created project
     */
    public void initializeDefaultConfigurations(Project project) {
        logger.info("Initializing default similarity configurations for project: {}", project.getId());

        try {
            // Check if configurations already exist
            if (similarityConfigRepository.hasDefaultConfigurations(project.getId())) {
                logger.debug("Project {} already has configurations, skipping initialization", project.getId());
                return;
            }

            List<SimilarityConfig> defaultConfigs = createDefaultConfigurations(project);
            similarityConfigRepository.saveAll(defaultConfigs);

            logger.info("Created {} default similarity configurations for project {}",
                    defaultConfigs.size(), project.getId());

        } catch (Exception e) {
            logger.error("Failed to initialize similarity configurations for project {}: {}",
                    project.getId(), e.getMessage(), e);
            // Don't throw exception - configuration failure shouldn't prevent project
            // creation
        }
    }

    /**
     * Initialize default configurations for a project by ID.
     * 
     * @param projectId Project UUID
     */
    public void initializeDefaultConfigurations(UUID projectId) {
        logger.info("Initializing default similarity configurations for project ID: {}", projectId);

        try {
            // Check if configurations already exist
            if (similarityConfigRepository.hasDefaultConfigurations(projectId)) {
                logger.debug("Project {} already has configurations, skipping initialization", projectId);
                return;
            }

            // Create a minimal project entity for configuration creation
            Project project = new Project();
            project.setId(projectId);

            List<SimilarityConfig> defaultConfigs = createDefaultConfigurations(project);
            similarityConfigRepository.saveAll(defaultConfigs);

            logger.info("Created {} default similarity configurations for project {}",
                    defaultConfigs.size(), projectId);

        } catch (Exception e) {
            logger.error("Failed to initialize similarity configurations for project {}: {}",
                    projectId, e.getMessage(), e);
        }
    }

    /**
     * Ensure all existing projects have similarity configurations.
     * This method can be used for migration or maintenance purposes.
     * 
     * @return Number of projects that had configurations added
     */
    @Transactional
    public int ensureAllProjectsHaveConfigurations() {
        logger.info("Ensuring all projects have similarity configurations");

        try {
            List<UUID> projectsMissingConfigs = similarityConfigRepository.findProjectsMissingConfigurations();

            if (projectsMissingConfigs.isEmpty()) {
                logger.info("All projects already have similarity configurations");
                return 0;
            }

            for (UUID projectId : projectsMissingConfigs) {
                initializeDefaultConfigurations(projectId);
            }

            logger.info("Added configurations for {} projects", projectsMissingConfigs.size());
            return projectsMissingConfigs.size();

        } catch (Exception e) {
            logger.error("Error ensuring projects have configurations: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Validate and fix configuration weights for a project.
     * Ensures weights sum to approximately 1.0 for optimal results.
     * 
     * @param projectId Project UUID
     * @return true if configurations were modified
     */
    public boolean normalizeConfigurationWeights(UUID projectId) {
        logger.debug("Normalizing configuration weights for project: {}", projectId);

        try {
            List<SimilarityConfig> configs = similarityConfigRepository.findEnabledByProjectId(projectId);

            if (configs.isEmpty()) {
                logger.warn("No enabled configurations found for project {}", projectId);
                return false;
            }

            // Calculate current total weight
            double totalWeight = configs.stream()
                    .mapToDouble(SimilarityConfig::getWeightAsDouble)
                    .sum();

            // If weights are already normalized (within 10% tolerance), no change needed
            if (Math.abs(totalWeight - 1.0) <= 0.1) {
                logger.debug("Weights for project {} are already normalized (total: {})", projectId, totalWeight);
                return false;
            }

            // Normalize weights proportionally
            boolean modified = false;
            for (SimilarityConfig config : configs) {
                double currentWeight = config.getWeightAsDouble();
                double normalizedWeight = currentWeight / totalWeight;

                config.setWeight(BigDecimal.valueOf(normalizedWeight));
                modified = true;
            }

            if (modified) {
                similarityConfigRepository.saveAll(configs);
                logger.info("Normalized weights for project {} from total {} to 1.0", projectId, totalWeight);
            }

            return modified;

        } catch (Exception e) {
            logger.error("Error normalizing weights for project {}: {}", projectId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get configuration recommendations for a project.
     * 
     * @param projectId Project UUID
     * @return List of configuration recommendations
     */
    public List<String> getConfigurationRecommendations(UUID projectId) {
        List<String> recommendations = new ArrayList<>();

        try {
            List<SimilarityConfig> configs = similarityConfigRepository.findByProjectId(projectId);
            List<SimilarityConfig> enabledConfigs = similarityConfigRepository.findEnabledByProjectId(projectId);

            if (configs.isEmpty()) {
                recommendations.add("No similarity configurations found. Initialize default configurations.");
                return recommendations;
            }

            if (enabledConfigs.isEmpty()) {
                recommendations.add("No algorithms are enabled. Enable at least one similarity algorithm.");
                return recommendations;
            }

            // Check weight sum
            double totalWeight = enabledConfigs.stream()
                    .mapToDouble(SimilarityConfig::getWeightAsDouble)
                    .sum();

            if (Math.abs(totalWeight - 1.0) > 0.1) {
                recommendations.add(String.format(
                        "Algorithm weights sum to %.2f but should sum to 1.0. Consider normalizing weights.",
                        totalWeight));
            }

            // Check for unusual thresholds
            boolean hasUnusualThresholds = enabledConfigs.stream()
                    .anyMatch(config -> config.getThresholdAsDouble() < 0.3 || config.getThresholdAsDouble() > 0.95);

            if (hasUnusualThresholds) {
                recommendations.add(
                        "Some thresholds are outside the recommended range (0.3-0.9). Consider adjusting for better performance.");
            }

            // Check if all algorithms are disabled
            long disabledCount = configs.stream()
                    .mapToLong(config -> config.isEnabled() ? 0 : 1)
                    .sum();

            if (disabledCount == configs.size()) {
                recommendations
                        .add("All algorithms are disabled. Enable at least one algorithm for similarity detection.");
            }

            // Check if primary algorithm (Cosine) is enabled
            boolean cosineMissing = enabledConfigs.stream()
                    .noneMatch(config -> config.getAlgorithmName() == SimilarityAlgorithm.COSINE);

            if (cosineMissing) {
                recommendations.add(
                        "Cosine similarity is disabled. Consider enabling it as it's the most effective algorithm for text similarity.");
            }

            if (recommendations.isEmpty()) {
                recommendations.add("Configuration looks good! No recommendations at this time.");
            }

        } catch (Exception e) {
            logger.error("Error getting recommendations for project {}: {}", projectId, e.getMessage(), e);
            recommendations.add("Error analyzing configuration. Please check system logs.");
        }

        return recommendations;
    }

    // === Private Helper Methods ===

    private List<SimilarityConfig> createDefaultConfigurations(Project project) {
        List<SimilarityConfig> configurations = new ArrayList<>();

        for (SimilarityAlgorithm algorithm : SimilarityAlgorithm.values()) {
            SimilarityConfig config = new SimilarityConfig();
            config.setProject(project);
            config.setAlgorithmName(algorithm);
            config.setWeight(BigDecimal.valueOf(algorithm.getDefaultWeight()));
            config.setThreshold(BigDecimal.valueOf(algorithm.getDefaultThreshold()));
            config.setEnabled(true); // Enable all algorithms by default

            configurations.add(config);
        }

        return configurations;
    }
}