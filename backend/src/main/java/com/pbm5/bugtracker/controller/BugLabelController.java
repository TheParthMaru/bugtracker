package com.pbm5.bugtracker.controller;

import com.pbm5.bugtracker.dto.BugLabelResponse;
import com.pbm5.bugtracker.dto.CreateLabelRequest;
import com.pbm5.bugtracker.dto.UpdateLabelRequest;
import com.pbm5.bugtracker.entity.BugLabel;
import com.pbm5.bugtracker.service.BugLabelService;
import com.pbm5.bugtracker.service.ProjectService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bugtracker/v1/projects/{projectSlug}/bug-labels")
public class BugLabelController {

    private static final Logger logger = LoggerFactory.getLogger(BugLabelController.class);

    @Autowired
    private BugLabelService bugLabelService;

    @Autowired
    private ProjectService projectService;

    // Helper method to get project UUID from slug or UUID
    private UUID getProjectUuidFromSlug(String projectIdentifier, UUID currentUserId) {
        logger.debug("getProjectUuidFromSlug called with: '{}'", projectIdentifier);

        try {
            // First, try to parse as UUID
            UUID projectUuid = UUID.fromString(projectIdentifier);
            logger.debug("Project identifier is a valid UUID: {}", projectUuid);

            // Verify the project exists by trying to get it
            projectService.getProjectById(projectUuid, currentUserId);
            return projectUuid;

        } catch (IllegalArgumentException e) {
            // Not a UUID, try as slug
            logger.debug("Project identifier is not a UUID, trying as slug: {}", projectIdentifier);
            try {
                return projectService.getProjectBySlug(projectIdentifier, currentUserId).getId();
            } catch (Exception slugException) {
                logger.error("Failed to get project UUID from slug: {}", slugException.getMessage());
                throw new IllegalArgumentException("Project not found with identifier: " + projectIdentifier);
            }
        } catch (Exception e) {
            logger.error("Failed to get project UUID: {}", e.getMessage());
            throw new IllegalArgumentException("Project not found with identifier: " + projectIdentifier);
        }
    }

    // Helper method to extract user ID from authentication
    private UUID getCurrentUserId(Authentication authentication) {
        try {
            // Try to parse directly as UUID first
            UUID userId = UUID.fromString(authentication.getName());
            logger.debug("User ID parsed directly as UUID: {}", userId);
            return userId;
        } catch (IllegalArgumentException e) {
            // If that fails, try to extract UUID from User object string
            String authName = authentication.getName();
            if (authName.contains("id=")) {
                // Extract UUID from User object string format
                int startIndex = authName.indexOf("id=") + 3;
                int endIndex = authName.indexOf(",", startIndex);
                if (endIndex == -1) {
                    endIndex = authName.indexOf("}", startIndex);
                }
                String userIdStr = authName.substring(startIndex, endIndex);
                UUID userId = UUID.fromString(userIdStr);
                logger.debug("User ID extracted from User object: {}", userId);
                return userId;
            } else {
                throw new IllegalArgumentException(
                        "Could not extract user ID from authentication: " + authName);
            }
        }
    }

    /**
     * Get all labels for a project
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLabels(
            @PathVariable String projectSlug,
            @RequestParam(required = false) Boolean isSystem,
            Pageable pageable,
            Authentication authentication) {

        logger.debug("getLabels method called for project: {}", projectSlug);

        try {
            UUID userId = getCurrentUserId(authentication);
            UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

            List<BugLabel> labels;
            if (isSystem != null) {
                if (isSystem) {
                    labels = bugLabelService.getSystemLabels();
                } else {
                    // Get custom labels for this specific project
                    labels = bugLabelService.getCustomLabelsForProject(projectUuid);
                }
            } else {
                // Get all labels (system + custom) for this specific project
                labels = bugLabelService.getLabelsForProject(projectUuid);
            }

            List<BugLabelResponse> responses = labels.stream()
                    .map(BugLabelResponse::new)
                    .collect(Collectors.toList());

            // Create paginated response structure
            Map<String, Object> response = new HashMap<>();
            response.put("content", responses);
            response.put("pageable", Map.of(
                    "pageNumber", pageable.getPageNumber(),
                    "pageSize", pageable.getPageSize(),
                    "sort", Map.of("sorted", false, "unsorted", true, "empty", true),
                    "offset", pageable.getOffset(),
                    "paged", false,
                    "unpaged", true));
            response.put("totalElements", (long) responses.size());
            response.put("totalPages", 1);
            response.put("last", true);
            response.put("size", responses.size());
            response.put("number", 0);
            response.put("sort", Map.of("sorted", false, "unsorted", true, "empty", true));
            response.put("first", true);
            response.put("numberOfElements", responses.size());
            response.put("empty", responses.isEmpty());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching labels for project {}: {}", projectSlug, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch labels: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new label
     */
    @PostMapping
    public ResponseEntity<BugLabelResponse> createLabel(
            @PathVariable String projectSlug,
            @Valid @RequestBody CreateLabelRequest request,
            Authentication authentication) {

        logger.debug("createLabel method called for project: {}", projectSlug);

        try {
            // Validate request data
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            BugLabel label = bugLabelService.createLabel(
                    request.getName().trim(),
                    request.getColor() != null ? request.getColor() : "#3B82F6", // Default blue color
                    request.getDescription() != null ? request.getDescription().trim() : "" // Default empty description
            );

            logger.debug("Label created successfully with ID: {}", label.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(new BugLabelResponse(label));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid label creation request for project {}: {}", projectSlug, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating label for project {}: {}", projectSlug, e.getMessage(), e);
            throw new RuntimeException("Failed to create label: " + e.getMessage(), e);
        }
    }

    /**
     * Update a label
     */
    @PutMapping("/{labelId}")
    public ResponseEntity<BugLabelResponse> updateLabel(
            @PathVariable String projectSlug,
            @PathVariable Long labelId,
            @Valid @RequestBody UpdateLabelRequest request,
            Authentication authentication) {

        logger.debug("updateLabel method called for label ID: {}", labelId);

        BugLabel updatedLabel = bugLabelService.updateLabel(
                labelId,
                request.getName(),
                request.getColor(),
                request.getDescription());

        logger.debug("Label updated successfully with ID: {}", updatedLabel.getId());
        return ResponseEntity.ok(new BugLabelResponse(updatedLabel));
    }

    /**
     * Delete a label
     */
    @DeleteMapping("/{labelId}")
    public ResponseEntity<Void> deleteLabel(
            @PathVariable String projectSlug,
            @PathVariable Long labelId,
            Authentication authentication) {

        logger.debug("deleteLabel method called for label ID: {}", labelId);

        bugLabelService.deleteLabel(labelId);

        logger.debug("Label deleted successfully with ID: {}", labelId);
        return ResponseEntity.noContent().build();
    }
}