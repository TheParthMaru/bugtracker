package com.pbm5.bugtracker.controller;

import com.pbm5.bugtracker.dto.ProjectResponse;
import com.pbm5.bugtracker.entity.SimilarityAlgorithm;
import com.pbm5.bugtracker.entity.SimilarityConfig;
import com.pbm5.bugtracker.repository.SimilarityConfigRepository;
import com.pbm5.bugtracker.service.ProjectService;
import com.pbm5.bugtracker.service.SimilarityConfigService;
import com.pbm5.bugtracker.util.AuthenticationUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for managing similarity algorithm configurations.
 * 
 * This controller allows project administrators to customize similarity
 * detection parameters including algorithm weights, thresholds, and
 * enabled/disabled status for different algorithms.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@RestController
@RequestMapping("/api/bugtracker/v1/projects/{projectSlug}/similarity-config")
@PreAuthorize("isAuthenticated()")
public class SimilarityConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SimilarityConfigController.class);

    @Autowired
    private SimilarityConfigRepository similarityConfigRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SimilarityConfigService similarityConfigService;

    /**
     * Get all similarity algorithm configurations for a project.
     * 
     * @param projectSlug    Project identifier slug
     * @param authentication Current user authentication
     * @return List of similarity configurations
     */
    @GetMapping
    @PreAuthorize("@projectSecurityService.canAccessProject(#projectSlug, authentication.principal.id)")
    public ResponseEntity<List<SimilarityConfig>> getConfigurations(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.debug("Getting similarity configurations for project: {}", projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            List<SimilarityConfig> configurations = similarityConfigRepository.findByProjectId(projectId);

            return ResponseEntity.ok(configurations);

        } catch (Exception e) {
            logger.error("Error getting configurations for project {}: {}", projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Initialize default similarity configurations for a project.
     * This endpoint creates default configurations if none exist.
     * 
     * @param projectSlug    Project identifier slug
     * @param authentication Current user authentication
     * @return List of initialized configurations
     */
    @PostMapping("/initialize")
    @PreAuthorize("@projectSecurityService.canAccessProject(#projectSlug, authentication.principal.id)")
    public ResponseEntity<List<SimilarityConfig>> initializeConfigurations(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.info("Initializing similarity configurations for project: {}", projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Initialize default configurations
            similarityConfigService.initializeDefaultConfigurations(projectId);

            // Return the newly created configurations
            List<SimilarityConfig> configurations = similarityConfigRepository.findByProjectId(projectId);

            logger.info("Initialized {} similarity configurations for project {}",
                    configurations.size(), projectSlug);

            return ResponseEntity.ok(configurations);

        } catch (Exception e) {
            logger.error("Error initializing configurations for project {}: {}",
                    projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get configuration for a specific algorithm.
     * 
     * @param projectSlug    Project identifier slug
     * @param algorithm      Algorithm name
     * @param authentication Current user authentication
     * @return Similarity configuration
     */
    @GetMapping("/{algorithm}")
    @PreAuthorize("@projectSecurityService.canAccessProject(#projectSlug, authentication.principal.id)")
    public ResponseEntity<SimilarityConfig> getAlgorithmConfiguration(
            @PathVariable String projectSlug,
            @PathVariable SimilarityAlgorithm algorithm,
            Authentication authentication) {

        logger.debug("Getting {} configuration for project: {}", algorithm, projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            Optional<SimilarityConfig> config = similarityConfigRepository
                    .findByProjectIdAndAlgorithm(projectId, algorithm);

            return config.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            logger.error("Error getting {} configuration for project {}: {}",
                    algorithm, projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update configuration for a specific algorithm.
     * 
     * @param projectSlug    Project identifier slug
     * @param algorithm      Algorithm name
     * @param request        Configuration update request
     * @param authentication Current user authentication
     * @return Updated configuration
     */
    @PutMapping("/{algorithm}")
    @PreAuthorize("@projectSecurityService.isProjectAdmin(#projectSlug, authentication.principal.id)")
    public ResponseEntity<SimilarityConfig> updateAlgorithmConfiguration(
            @PathVariable String projectSlug,
            @PathVariable SimilarityAlgorithm algorithm,
            @Valid @RequestBody ConfigurationUpdateRequest request,
            Authentication authentication) {

        logger.info("Updating {} configuration for project: {}", algorithm, projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            Optional<SimilarityConfig> existingConfig = similarityConfigRepository
                    .findByProjectIdAndAlgorithm(projectId, algorithm);

            if (existingConfig.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            SimilarityConfig config = existingConfig.get();

            // Update configuration
            if (request.getWeight() != null) {
                config.setWeight(BigDecimal.valueOf(request.getWeight()));
            }
            if (request.getThreshold() != null) {
                config.setThreshold(BigDecimal.valueOf(request.getThreshold()));
            }
            if (request.getIsEnabled() != null) {
                config.setEnabled(request.getIsEnabled());
            }

            SimilarityConfig savedConfig = similarityConfigRepository.save(config);

            logger.info("Updated {} configuration for project {}: weight={}, threshold={}, enabled={}",
                    algorithm, projectSlug, savedConfig.getWeight(),
                    savedConfig.getThreshold(), savedConfig.isEnabled());

            return ResponseEntity.ok(savedConfig);

        } catch (Exception e) {
            logger.error("Error updating {} configuration for project {}: {}",
                    algorithm, projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Enable or disable a specific algorithm.
     * 
     * @param projectSlug    Project identifier slug
     * @param algorithm      Algorithm name
     * @param enabled        Whether to enable or disable
     * @param authentication Current user authentication
     * @return Updated configuration
     */
    @PatchMapping("/{algorithm}/enabled")
    @PreAuthorize("@projectSecurityService.isProjectAdmin(#projectSlug, authentication.principal.id)")
    public ResponseEntity<SimilarityConfig> toggleAlgorithm(
            @PathVariable String projectSlug,
            @PathVariable SimilarityAlgorithm algorithm,
            @RequestParam boolean enabled,
            Authentication authentication) {

        logger.info("{} algorithm {} for project: {}",
                enabled ? "Enabling" : "Disabling", algorithm, projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            Optional<SimilarityConfig> existingConfig = similarityConfigRepository
                    .findByProjectIdAndAlgorithm(projectId, algorithm);

            if (existingConfig.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            SimilarityConfig config = existingConfig.get();
            config.setEnabled(enabled);

            SimilarityConfig savedConfig = similarityConfigRepository.save(config);

            return ResponseEntity.ok(savedConfig);

        } catch (Exception e) {
            logger.error("Error toggling {} for project {}: {}",
                    algorithm, projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reset configurations to default values.
     * 
     * @param projectSlug    Project identifier slug
     * @param authentication Current user authentication
     * @return List of reset configurations
     */
    @PostMapping("/reset")
    @PreAuthorize("@projectSecurityService.isProjectAdmin(#projectSlug, authentication.principal.id)")
    public ResponseEntity<List<SimilarityConfig>> resetToDefaults(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.info("Resetting similarity configurations to defaults for project: {}", projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            List<SimilarityConfig> configurations = similarityConfigRepository.findByProjectId(projectId);

            // Reset each configuration to default values
            for (SimilarityConfig config : configurations) {
                SimilarityAlgorithm algorithm = config.getAlgorithmName();
                config.setWeight(BigDecimal.valueOf(algorithm.getDefaultWeight()));
                config.setThreshold(BigDecimal.valueOf(algorithm.getDefaultThreshold()));
                config.setEnabled(true);
            }

            List<SimilarityConfig> savedConfigs = similarityConfigRepository.saveAll(configurations);

            logger.info("Reset {} configurations to defaults for project {}",
                    savedConfigs.size(), projectSlug);

            return ResponseEntity.ok(savedConfigs);

        } catch (Exception e) {
            logger.error("Error resetting configurations for project {}: {}",
                    projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get configuration validation results.
     * 
     * @param projectSlug    Project identifier slug
     * @param authentication Current user authentication
     * @return Validation results and recommendations
     */
    @GetMapping("/validation")
    @PreAuthorize("@projectSecurityService.canAccessProject(#projectSlug, authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> validateConfigurations(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.debug("Validating configurations for project: {}", projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            List<SimilarityConfig> configurations = similarityConfigRepository.findEnabledByProjectId(projectId);

            // Calculate total weight
            double totalWeight = configurations.stream()
                    .mapToDouble(SimilarityConfig::getWeightAsDouble)
                    .sum();

            // Validation checks
            boolean hasEnabledAlgorithms = !configurations.isEmpty();
            boolean weightsSum = Math.abs(totalWeight - 1.0) <= 0.1; // Allow 10% tolerance
            boolean hasValidThresholds = configurations.stream()
                    .allMatch(config -> config.getThresholdAsDouble() >= 0.3 && config.getThresholdAsDouble() <= 0.95);

            // Recommendations
            List<String> recommendations = List.of();
            if (!hasEnabledAlgorithms) {
                recommendations = List.of("Enable at least one similarity algorithm");
            } else if (!weightsSum) {
                recommendations = List.of("Algorithm weights should sum to approximately 1.0 for optimal results");
            } else if (!hasValidThresholds) {
                recommendations = List.of("Consider adjusting thresholds to be between 0.3 and 0.9");
            } else {
                recommendations = List.of("Configuration looks good!");
            }

            Map<String, Object> validation = Map.of(
                    "isValid", hasEnabledAlgorithms && weightsSum && hasValidThresholds,
                    "hasEnabledAlgorithms", hasEnabledAlgorithms,
                    "totalWeight", totalWeight,
                    "weightsSum", weightsSum,
                    "hasValidThresholds", hasValidThresholds,
                    "enabledAlgorithmCount", configurations.size(),
                    "recommendations", recommendations);

            return ResponseEntity.ok(validation);

        } catch (Exception e) {
            logger.error("Error validating configurations for project {}: {}",
                    projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // === Helper Methods ===

    // === Request DTOs ===

    /**
     * Request DTO for updating similarity configuration
     */
    public static class ConfigurationUpdateRequest {
        private Double weight;
        private Double threshold;
        private Boolean isEnabled;

        // Getters and setters
        public Double getWeight() {
            return weight;
        }

        public void setWeight(Double weight) {
            this.weight = weight;
        }

        public Double getThreshold() {
            return threshold;
        }

        public void setThreshold(Double threshold) {
            this.threshold = threshold;
        }

        public Boolean getIsEnabled() {
            return isEnabled;
        }

        public void setIsEnabled(Boolean enabled) {
            isEnabled = enabled;
        }
    }
}