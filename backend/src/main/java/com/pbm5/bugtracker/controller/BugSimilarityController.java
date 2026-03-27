package com.pbm5.bugtracker.controller;

import com.pbm5.bugtracker.dto.*;
import com.pbm5.bugtracker.entity.DuplicateDetectionMethod;
import com.pbm5.bugtracker.entity.SimilarityAlgorithm;
import com.pbm5.bugtracker.service.BugSimilarityService;
import com.pbm5.bugtracker.service.ProjectService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.BugDuplicate;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.repository.BugDuplicateRepository;
import com.pbm5.bugtracker.repository.BugRepository;

/**
 * REST Controller for bug similarity and duplicate detection features.
 * 
 * This controller provides endpoints for:
 * - Real-time similarity checking during bug creation
 * - Manual duplicate marking and management
 * - Similarity statistics and monitoring
 * - Algorithm configuration management
 * 
 * All endpoints require authentication and proper project access permissions.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@RestController
@RequestMapping("/api/bugtracker/v1/projects/{projectSlug}/bugs/similarity")
@PreAuthorize("isAuthenticated()")
public class BugSimilarityController {

    private static final Logger logger = LoggerFactory.getLogger(BugSimilarityController.class);

    @Autowired
    private BugSimilarityService bugSimilarityService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private BugDuplicateRepository bugDuplicateRepository;

    @Autowired
    private BugRepository bugRepository;

    /**
     * Check for similar bugs before creating a new bug report.
     * 
     * This endpoint is typically called in real-time as users type their bug
     * title and description to provide immediate feedback about potential
     * duplicates.
     * 
     * @param projectSlug    Project identifier slug
     * @param request        Similarity check request with title and description
     * @param authentication Current user authentication
     * @return List of similar bugs with similarity scores
     */
    @PostMapping("/check")
    public ResponseEntity<List<BugSimilarityResult>> checkSimilarity(
            @PathVariable String projectSlug,
            @Valid @RequestBody SimilarityCheckRequest request,
            Authentication authentication) {

        logger.debug("Checking similarity for bug in project: {}", projectSlug);
        logger.debug("Request details: title='{}', description length={}, projectId={}",
                request.getTitle(),
                request.getDescription() != null ? request.getDescription().length() : 0,
                request.getProjectId());

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            logger.debug("Setting project ID {} on request object", projectId);
            // Update request with project ID
            request.setProjectId(projectId);

            // Perform similarity check
            List<BugSimilarityResult> similarBugs = bugSimilarityService.findSimilarBugs(
                    request.getTitle(),
                    request.getDescription(),
                    projectId,
                    request.getEffectiveSimilarityThreshold(),
                    request.getEffectiveMaxResults(),
                    request.getIncludeClosedBugs());

            logger.info("Found {} similar bugs for project {} with threshold {}",
                    similarBugs.size(), projectSlug, request.getEffectiveSimilarityThreshold());

            return ResponseEntity.ok(similarBugs);

        } catch (Exception e) {
            logger.error("Error checking similarity in project {}: {}", projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get similarity analysis for all bugs in a project.
     * 
     * This endpoint provides comprehensive similarity analysis between all bugs
     * in a project, useful for identifying potential duplicates and analyzing
     * bug patterns.
     * 
     * @param projectSlug    Project identifier slug
     * @param threshold      Minimum similarity threshold (0.0 to 1.0)
     * @param searchTerm     Optional search term to filter bugs
     * @param sortBy         Sort field (similarityScore, title, createdAt, etc.)
     * @param sortDirection  Sort direction (asc or desc)
     * @param page           Page number for pagination
     * @param size           Page size for pagination
     * @param authentication Current user authentication
     * @return List of BugSimilarityResult with similarity scores
     */
    @GetMapping("/analysis")
    public ResponseEntity<Page<BugSimilarityRelationship>> getProjectSimilarityAnalysis(
            @PathVariable String projectSlug,
            @RequestParam(defaultValue = "0.4") double threshold,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "similarityScore") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {

        logger.debug(
                "Getting project similarity analysis for project: {} with threshold: {}, searchTerm: '{}', sortBy: {}, sortDirection: {}, page: {}, size: {}",
                projectSlug, threshold, searchTerm, sortBy, sortDirection, page, size);

        try {
            // Validate parameters
            if (threshold < 0.0 || threshold > 1.0) {
                logger.warn("Invalid threshold value: {} for project {}", threshold, projectSlug);
                return ResponseEntity.badRequest().build();
            }

            if (page < 0 || size < 1 || size > 100) {
                logger.warn("Invalid pagination parameters: page={}, size={} for project {}", page, size, projectSlug);
                return ResponseEntity.badRequest().build();
            }

            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Get similarity analysis
            Page<BugSimilarityRelationship> results = bugSimilarityService.getProjectSimilarityAnalysis(
                    projectId, threshold, searchTerm, sortBy, sortDirection, page, size, startDate, endDate);

            // Strategic logging for pagination debugging
            // logger.debug("=== CONTROLLER PAGINATION DEBUG ===");
            // logger.debug("Service returned Page object:");
            // logger.debug(" - content size: {}", results.getContent().size());
            // logger.debug(" - total elements: {}", results.getTotalElements());
            // logger.debug(" - total pages: {}", results.getTotalPages());
            // logger.debug(" - total pages: {}", results.getTotalPages());
            // logger.debug(" - current page number: {}", results.getNumber());
            // logger.debug(" - page size: {}", results.getSize());
            // logger.debug(" - has next: {}", results.hasNext());
            // logger.debug(" - has previous: {}", results.hasPrevious());
            // logger.debug("=== END CONTROLLER DEBUG ===");

            logger.info("Retrieved {} similarity results for project {} with threshold {} (page {} of {}, total: {})",
                    results.getContent().size(), projectSlug, threshold, page + 1, results.getTotalPages(),
                    results.getTotalElements());

            // Debug logging to investigate duplicate keys issue
            // logger.debug("Detailed similarity results for project {}: {}", projectSlug,
            // results.getContent().stream()
            // .map(result -> String.format(
            // "Bug A (ID: %d, Title: '%s') vs Bug B (ID: %d, Title: '%s'), Similarity:
            // %.3f",
            // result.getBugAId(), result.getBugATitle(), result.getBugBId(),
            // result.getBugBTitle(), result.getSimilarityScore()))
            // .collect(Collectors.joining(", ")));

            // Check for duplicate Bug A IDs
            // Map<Long, Long> bugIdCounts = results.getContent().stream()
            // .collect(Collectors.groupingBy(BugSimilarityRelationship::getBugAId,
            // Collectors.counting()));

            // List<Long> duplicateBugIds = bugIdCounts.entrySet().stream()
            // .filter(entry -> entry.getValue() > 1)
            // .map(Map.Entry::getKey)
            // .collect(Collectors.toList());

            // if (!duplicateBugIds.isEmpty()) {
            // logger.warn("Found duplicate bug IDs in similarity results for project {}:
            // {}",
            // projectSlug, duplicateBugIds);

            // // Log detailed info about duplicates
            // for (Long duplicateId : duplicateBugIds) {
            // List<BugSimilarityRelationship> duplicates = results.getContent().stream()
            // .filter(result -> result.getBugAId().equals(duplicateId))
            // .collect(Collectors.toList());

            // logger.debug("Bug A ID {} appears {} times with details: {}",
            // duplicateId, duplicates.size(),
            // duplicates.stream()
            // .map(result -> String.format(
            // "Similarity: %.3f, Bug A Title: '%s', Bug B Title: '%s'",
            // result.getSimilarityScore(), result.getBugATitle(), result.getBugBTitle()))
            // .collect(Collectors.joining(" | ")));
            // }
            // }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            logger.error("Error getting project similarity analysis for project {}: {}", projectSlug, e.getMessage(),
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark a bug as duplicate of another bug.
     * 
     * This endpoint allows users to manually mark a bug as a duplicate of an
     * existing bug,
     * or to confirm an automatically detected duplicate relationship.
     * 
     * @param projectSlug    Project identifier slug
     * @param bugId          Bug to mark as duplicate
     * @param request        Duplicate marking request
     * @param authentication Current user authentication
     * @return Success response
     */
    @PostMapping("/{bugId}/mark-duplicate")
    public ResponseEntity<SuccessResponse> markAsDuplicate(
            @PathVariable String projectSlug,
            @PathVariable Long bugId,
            @Valid @RequestBody MarkDuplicateRequest request,
            Authentication authentication) {

        logger.info("Marking bug {} as duplicate of {} in project {}",
                bugId, request.getOriginalBugId(), projectSlug);

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Determine detection method
            DuplicateDetectionMethod detectionMethod = Boolean.TRUE.equals(request.getIsAutomaticDetection())
                    ? DuplicateDetectionMethod.HYBRID // Automatic suggestion confirmed by user
                    : DuplicateDetectionMethod.MANUAL; // Manually identified by user

            // Mark as duplicate
            bugSimilarityService.markAsDuplicate(
                    projectId,
                    bugId,
                    request.getOriginalBugId(),
                    currentUserId,
                    request.getEffectiveConfidenceScore(),
                    detectionMethod,
                    request.getTrimmedAdditionalContext());

            return ResponseEntity.ok(new SuccessResponse(
                    "Bug marked as duplicate successfully",
                    LocalDateTime.now(),
                    "MARK_DUPLICATE",
                    null));

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid duplicate marking request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new SuccessResponse(
                            e.getMessage(),
                            LocalDateTime.now(),
                            "MARK_DUPLICATE",
                            null));
        } catch (Exception e) {
            logger.error("Error marking bug {} as duplicate: {}", bugId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SuccessResponse(
                            "Failed to mark bug as duplicate",
                            LocalDateTime.now(),
                            "MARK_DUPLICATE",
                            null));
        }
    }

    /**
     * Get detailed similarity scores for two specific bugs.
     * 
     * This endpoint is useful for debugging similarity calculations or providing
     * detailed information to users about why two bugs are considered similar.
     * 
     * @param projectSlug    Project identifier slug
     * @param bugId1         First bug ID
     * @param bugId2         Second bug ID
     * @param authentication Current user authentication
     * @return Map of algorithm-specific similarity scores
     */
    @GetMapping("/compare/{bugId1}/{bugId2}")
    public ResponseEntity<Map<SimilarityAlgorithm, Double>> compareSpecificBugs(
            @PathVariable String projectSlug,
            @PathVariable Long bugId1,
            @PathVariable Long bugId2,
            Authentication authentication) {

        logger.debug("Comparing bugs {} and {} in project {}", bugId1, bugId2, projectSlug);

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Perform bug comparison using the service
            Map<SimilarityAlgorithm, Double> scores = bugSimilarityService.compareSpecificBugs(projectId, bugId1,
                    bugId2);

            return ResponseEntity.ok(scores);

        } catch (Exception e) {
            logger.error("Error comparing bugs {} and {}: {}", bugId1, bugId2, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get similarity statistics for the project.
     * 
     * This endpoint provides insights into duplicate detection performance,
     * cache hit rates, and algorithm usage statistics for monitoring and
     * optimization.
     * 
     * @param projectSlug    Project identifier slug
     * @param authentication Current user authentication
     * @return Map of similarity statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or @projectSecurityService.isProjectAdmin(#projectSlug, authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> getSimilarityStatistics(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.debug("Getting similarity statistics for project: {}", projectSlug);

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Get statistics
            Map<String, Object> statistics = bugSimilarityService.getSimilarityStatistics(projectId);

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("Error getting similarity statistics for project {}: {}", projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Trigger cleanup of expired similarity cache entries.
     * 
     * This endpoint allows administrators to manually trigger cache cleanup
     * for performance maintenance.
     * 
     * @param authentication Current user authentication
     * @return Number of deleted cache entries
     */
    @PostMapping("/cache/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupCache(Authentication authentication) {

        logger.info("Manual cache cleanup triggered by user: {}", authentication.getName());

        try {
            int deletedCount = bugSimilarityService.cleanupExpiredCache();

            Map<String, Object> result = Map.of(
                    "deletedEntries", deletedCount,
                    "message", "Cache cleanup completed successfully");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error during cache cleanup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cache cleanup failed"));
        }
    }

    /**
     * Batch similarity check for multiple bug texts.
     * 
     * This endpoint allows checking similarity for multiple bug reports at once,
     * useful for data migration or bulk analysis scenarios.
     * 
     * @param projectSlug    Project identifier slug
     * @param requests       List of similarity check requests
     * @param authentication Current user authentication
     * @return List of similarity results for each request
     */
    @PostMapping("/batch-check")
    @PreAuthorize("hasRole('ADMIN') or @projectSecurityService.isProjectAdmin(#projectSlug, authentication.principal.id)")
    public ResponseEntity<List<List<BugSimilarityResult>>> batchCheckSimilarity(
            @PathVariable String projectSlug,
            @Valid @RequestBody List<SimilarityCheckRequest> requests,
            Authentication authentication) {

        logger.info("Batch similarity check for {} requests in project: {}", requests.size(), projectSlug);

        try {
            // Validate batch size
            if (requests.size() > 100) {
                return ResponseEntity.badRequest().build();
            }

            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Process each request
            List<List<BugSimilarityResult>> batchResults = requests.stream()
                    .map(request -> {
                        request.setProjectId(projectId);
                        return bugSimilarityService.findSimilarBugs(
                                request.getTitle(),
                                request.getDescription(),
                                projectId,
                                request.getEffectiveSimilarityThreshold(),
                                request.getEffectiveMaxResults(),
                                request.getIncludeClosedBugs());
                    })
                    .toList();

            return ResponseEntity.ok(batchResults);

        } catch (Exception e) {
            logger.error("Error in batch similarity check: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get configuration health check for similarity algorithms.
     * 
     * This endpoint checks if the project has proper similarity algorithm
     * configurations and provides recommendations for optimization.
     * 
     * @param projectSlug    Project identifier slug
     * @param authentication Current user authentication
     * @return Configuration health information
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getConfigurationHealth(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.debug("Checking similarity configuration health for project: {}", projectSlug);

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Get basic health information
            Map<String, Object> statistics = bugSimilarityService.getSimilarityStatistics(projectId);

            boolean isHealthy = Boolean.TRUE.equals(statistics.get("hasEnabledAlgorithms"));
            String status = isHealthy ? "HEALTHY" : "NEEDS_CONFIGURATION";

            Map<String, Object> health = Map.of(
                    "status", status,
                    "hasEnabledAlgorithms", statistics.get("hasEnabledAlgorithms"),
                    "totalCacheEntries", statistics.getOrDefault("totalCacheEntries", 0),
                    "recommendations",
                    isHealthy ? List.of("Configuration is optimal")
                            : List.of("Enable similarity algorithms for this project",
                                    "Configure algorithm weights and thresholds"));

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("Error checking configuration health for project {}: {}", projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "error", "Failed to check configuration health"));
        }
    }

    // === Helper Methods ===

    /**
     * Get duplicate information for a specific bug.
     * 
     * This endpoint provides comprehensive information about a bug's duplicate
     * status,
     * including whether it's a duplicate, its original bug, relationship details,
     * and other duplicates of the same original bug.
     * 
     * @param projectSlug    Project identifier slug
     * @param bugId          Bug ID to check for duplicate information
     * @param authentication Current user authentication
     * @return DuplicateInfoResponse containing duplicate status and relationship
     *         information
     */
    @GetMapping("/bugs/{bugId}/duplicate-info")
    public ResponseEntity<DuplicateInfoResponse> getDuplicateInfo(
            @PathVariable String projectSlug,
            @PathVariable Long bugId,
            Authentication authentication) {

        logger.debug("Getting duplicate info for bug {} in project: {}", bugId, projectSlug);

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Get duplicate information
            DuplicateInfoResponse duplicateInfo = bugSimilarityService.getDuplicateInfo(projectId, bugId);

            logger.debug("Retrieved duplicate info for bug {}: isDuplicate={}",
                    bugId, duplicateInfo.isDuplicate());

            return ResponseEntity.ok(duplicateInfo);

        } catch (Exception e) {
            logger.error("Error getting duplicate info for bug {} in project {}: {}",
                    bugId, projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DuplicateInfoResponse(false));
        }
    }

    /**
     * Get duplicate information for a bug by project ticket number.
     * This endpoint handles the mapping between project ticket numbers and actual
     * bug IDs.
     * 
     * @param projectSlug         Project identifier slug
     * @param projectTicketNumber Project ticket number
     * @param authentication      Current user authentication
     * @return DuplicateInfoResponse containing duplicate status and relationship
     *         information
     */
    @GetMapping("/bugs/ticket/{projectTicketNumber}/duplicate-info")
    public ResponseEntity<DuplicateInfoResponse> getDuplicateInfoByTicketNumber(
            @PathVariable String projectSlug,
            @PathVariable Integer projectTicketNumber,
            Authentication authentication) {

        logger.debug("Getting duplicate info for bug with ticket number {} in project: {}", projectTicketNumber,
                projectSlug);

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Get duplicate information by ticket number
            DuplicateInfoResponse duplicateInfo = bugSimilarityService.getDuplicateInfoByTicketNumber(projectId,
                    projectTicketNumber);

            logger.debug("Retrieved duplicate info for bug with ticket number {}: isDuplicate={}",
                    projectTicketNumber, duplicateInfo.isDuplicate());

            return ResponseEntity.ok(duplicateInfo);

        } catch (Exception e) {
            logger.error("Error getting duplicate info for bug with ticket number {} in project {}: {}",
                    projectTicketNumber, projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DuplicateInfoResponse(false));
        }
    }

    /**
     * Get all duplicates of a specific bug (for original bug view).
     * 
     * This endpoint is useful when viewing an original bug to see all its
     * duplicates.
     * 
     * @param projectSlug    Project identifier slug
     * @param bugId          Original bug ID
     * @param authentication Current user authentication
     * @return List of BugDuplicateSummaryResponse containing duplicate information
     */
    @GetMapping("/bugs/{bugId}/duplicates")
    public ResponseEntity<List<BugDuplicateSummaryResponse>> getDuplicatesOfBug(
            @PathVariable String projectSlug,
            @PathVariable Long bugId,
            Authentication authentication) {

        logger.debug("Getting duplicates for bug {} in project: {}", bugId, projectSlug);

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Get duplicates
            List<BugDuplicateSummaryResponse> duplicates = bugSimilarityService.getDuplicatesOfBug(projectId, bugId);

            logger.debug("Retrieved {} duplicates for bug {} in project {}",
                    duplicates.size(), bugId, projectSlug);

            return ResponseEntity.ok(duplicates);

        } catch (Exception e) {
            logger.error("Error getting duplicates for bug {} in project {}: {}",
                    bugId, projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    /**
     * Get the original bug for a duplicate bug.
     * 
     * This endpoint is useful when viewing a duplicate bug to navigate to its
     * original.
     * 
     * @param projectSlug    Project identifier slug
     * @param bugId          Duplicate bug ID
     * @param authentication Current user authentication
     * @return BugSummaryResponse containing original bug information
     */
    @GetMapping("/bugs/{bugId}/original-bug")
    public ResponseEntity<BugSummaryResponse> getOriginalBug(
            @PathVariable String projectSlug,
            @PathVariable Long bugId,
            Authentication authentication) {

        logger.debug("Getting original bug for duplicate {} in project: {}", bugId, projectSlug);

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Get original bug
            BugSummaryResponse originalBug = bugSimilarityService.getOriginalBug(projectId, bugId);

            logger.debug("Retrieved original bug {} for duplicate {} in project {}",
                    originalBug.getId(), bugId, projectSlug);

            return ResponseEntity.ok(originalBug);

        } catch (Exception e) {
            logger.error("Error getting original bug for duplicate {} in project {}: {}",
                    bugId, projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get duplicate analytics for a project.
     * 
     * This endpoint provides comprehensive analytics about duplicate bugs in a
     * project,
     * including counts by detection method and by user who marked them.
     * 
     * @param projectSlug    Project identifier slug
     * @param authentication Current user authentication
     * @return DuplicateAnalyticsResponse containing analytics information
     */
    @GetMapping("/duplicate-analytics")
    public ResponseEntity<DuplicateAnalyticsResponse> getDuplicateAnalytics(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.debug("Getting duplicate analytics for project: {}", projectSlug);

        try {
            // Get project ID and validate access
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Get duplicate analytics
            DuplicateAnalyticsResponse analytics = bugSimilarityService.getDuplicateAnalytics(projectId);

            logger.debug("Retrieved duplicate analytics for project {}: totalDuplicates={}",
                    projectSlug, analytics.getTotalDuplicates());

            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            logger.error("Error getting duplicate analytics for project {}: {}",
                    projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DuplicateAnalyticsResponse(0L, Map.of(), Map.of()));
        }
    }

    /**
     * Get diagnostic information for debugging duplicate detection issues.
     * 
     * @param projectSlug    Project identifier slug
     * @param authentication Current user authentication
     * @return Map containing diagnostic information
     */
    @GetMapping("/diagnostic")
    @PreAuthorize("@projectSecurityService.canAccessProject(#projectSlug, authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> getDiagnosticInfo(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.info("Getting diagnostic info for project: {}", projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            Map<String, Object> diagnostic = bugSimilarityService.getDiagnosticInfo(projectId);

            logger.info("Retrieved diagnostic info for project {}", projectSlug);
            return ResponseEntity.ok(diagnostic);

        } catch (Exception e) {
            logger.error("Error getting diagnostic info for project {}: {}", projectSlug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Manually initialize similarity configurations for an existing project.
     * This endpoint is useful for projects created before automatic initialization
     * was implemented.
     * 
     * @param projectSlug    Project identifier slug
     * @param authentication Current user authentication
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/initialize-configs")
    @PreAuthorize("@projectSecurityService.canAccessProject(#projectSlug, authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> initializeSimilarityConfigurations(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.info("Manually initializing similarity configurations for project: {}", projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            boolean wasInitialized = bugSimilarityService.initializeSimilarityConfigurationsIfMissing(projectId);

            Map<String, Object> response = new HashMap<>();
            if (wasInitialized) {
                response.put("message", "Similarity configurations initialized successfully");
                response.put("initialized", true);
                logger.info("Successfully initialized similarity configurations for project: {}", projectSlug);
            } else {
                response.put("message", "Similarity configurations already exist for this project");
                response.put("initialized", false);
                logger.info("Similarity configurations already exist for project: {}, skipping initialization",
                        projectSlug);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error initializing similarity configurations for project {}: {}", projectSlug, e.getMessage(),
                    e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to initialize similarity configurations");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Debug endpoint to show current duplicate relationships in the database.
     * This helps diagnose ID mapping issues between ticket numbers and bug IDs.
     */
    @GetMapping("/debug/duplicates")
    @PreAuthorize("@projectSecurityService.canAccessProject(#projectSlug, authentication.principal.id)")
    public ResponseEntity<Map<String, Object>> debugDuplicateRelationships(
            @PathVariable String projectSlug,
            Authentication authentication) {

        logger.info("Debug: Getting duplicate relationships for project: {}", projectSlug);

        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId(authentication);
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
            UUID projectId = project.getId();

            // Get all duplicate relationships
            List<BugDuplicate> allDuplicates = bugDuplicateRepository.findByProjectId(projectId);

            // Get all bugs in the project
            List<Bug> allBugs = bugRepository.findByProjectId(projectId);

            Map<String, Object> debugInfo = new HashMap<>();

            // Map of bug ID to ticket number for easy lookup
            // Map<Long, Integer> bugIdToTicketNumber = allBugs.stream()
            // .collect(Collectors.toMap(Bug::getId, Bug::getProjectTicketNumber));

            // Duplicate relationships with clear ID mapping
            List<Map<String, Object>> duplicateRelationships = allDuplicates.stream()
                    .map(dup -> {
                        Map<String, Object> rel = new HashMap<>();
                        rel.put("relationshipId", dup.getId());
                        rel.put("originalBugId", dup.getOriginalBug().getId());
                        rel.put("originalBugTicketNumber", dup.getOriginalBug().getProjectTicketNumber());
                        rel.put("duplicateBugId", dup.getDuplicateBug().getId());
                        rel.put("duplicateBugTicketNumber", dup.getDuplicateBug().getProjectTicketNumber());
                        rel.put("markedBy", getUserDisplayName(dup.getMarkedByUser()));
                        rel.put("markedAt", dup.getMarkedAt());
                        return rel;
                    })
                    .collect(Collectors.toList());

            // All bugs with their IDs and ticket numbers
            List<Map<String, Object>> bugMappings = allBugs.stream()
                    .map(bug -> {
                        Map<String, Object> bugInfo = new HashMap<>();
                        bugInfo.put("bugId", bug.getId());
                        bugInfo.put("ticketNumber", bug.getProjectTicketNumber());
                        bugInfo.put("title", bug.getTitle());
                        bugInfo.put("status", bug.getStatus());
                        return bugInfo;
                    })
                    .collect(Collectors.toList());

            debugInfo.put("projectId", projectId);
            debugInfo.put("totalBugs", allBugs.size());
            debugInfo.put("totalDuplicateRelationships", allDuplicates.size());
            debugInfo.put("duplicateRelationships", duplicateRelationships);
            debugInfo.put("bugMappings", bugMappings);

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            logger.error("Error getting debug info for project {}: {}", projectSlug, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get debug information");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Simple debug endpoint to show bug ID to ticket number mapping.
     * This helps diagnose ID mapping issues.
     */
    @GetMapping("/debug/bug-mapping")
    public ResponseEntity<Map<String, Object>> debugBugMapping(
            @PathVariable String projectSlug) {

        logger.info("Debug: Getting bug mapping for project: {}", projectSlug);

        try {
            // Get project ID
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, UUID.randomUUID()); // Use dummy UUID
                                                                                                       // for now
            UUID projectId = project.getId();

            // Get all bugs in the project
            List<Bug> allBugs = bugRepository.findByProjectId(projectId);

            // Get all duplicate relationships
            List<BugDuplicate> allDuplicates = bugDuplicateRepository.findByProjectId(projectId);

            Map<String, Object> debugInfo = new HashMap<>();

            // Simple bug mapping
            List<Map<String, Object>> bugMappings = allBugs.stream()
                    .map(bug -> {
                        Map<String, Object> bugInfo = new HashMap<>();
                        bugInfo.put("bugId", bug.getId());
                        bugInfo.put("ticketNumber", bug.getProjectTicketNumber());
                        bugInfo.put("title", bug.getTitle());
                        bugInfo.put("status", bug.getStatus());
                        return bugInfo;
                    })
                    .collect(Collectors.toList());

            // Simple duplicate relationships
            List<Map<String, Object>> duplicateRelationships = allDuplicates.stream()
                    .map(dup -> {
                        Map<String, Object> rel = new HashMap<>();
                        rel.put("originalBugId", dup.getOriginalBug().getId());
                        rel.put("originalBugTicketNumber", dup.getOriginalBug().getProjectTicketNumber());
                        rel.put("duplicateBugId", dup.getDuplicateBug().getId());
                        rel.put("duplicateBugTicketNumber", dup.getDuplicateBug().getProjectTicketNumber());
                        return rel;
                    })
                    .collect(Collectors.toList());

            debugInfo.put("projectId", projectId);
            debugInfo.put("totalBugs", allBugs.size());
            debugInfo.put("totalDuplicateRelationships", allDuplicates.size());
            debugInfo.put("bugMappings", bugMappings);
            debugInfo.put("duplicateRelationships", duplicateRelationships);

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            logger.error("Error getting debug info for project {}: {}", projectSlug, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get debug information");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Helper method to get user display name.
     * This is a placeholder and would require a proper UserService to be fully
     * functional.
     * For now, it returns the username.
     */
    private String getUserDisplayName(User user) {
        // In a real application, you would fetch the user's display name from a
        // UserService
        // For now, return a placeholder or the user's username
        if (user != null) {
            String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
            String lastName = user.getLastName() != null ? user.getLastName().trim() : "";

            if (firstName.isEmpty() && lastName.isEmpty()) {
                return "Unknown User";
            } else if (firstName.isEmpty()) {
                return lastName;
            } else if (lastName.isEmpty()) {
                return firstName;
            } else {
                return firstName + " " + lastName;
            }
        }
        return "Unknown User";
    }
}