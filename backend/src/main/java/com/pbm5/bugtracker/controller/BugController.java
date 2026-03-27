package com.pbm5.bugtracker.controller;

import com.pbm5.bugtracker.dto.BugResponse;
import com.pbm5.bugtracker.dto.CreateBugRequest;
import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation;
import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation.TeamAssignmentInfo;
import com.pbm5.bugtracker.entity.BugLabel;
import com.pbm5.bugtracker.entity.BugTeamAssignment;
import com.pbm5.bugtracker.entity.Project;
import com.pbm5.bugtracker.dto.UpdateBugRequest;
import com.pbm5.bugtracker.dto.UpdateBugStatusRequest;
import com.pbm5.bugtracker.dto.CreateCommentRequest;
import com.pbm5.bugtracker.dto.UpdateCommentRequest;
import com.pbm5.bugtracker.dto.BugCommentResponse;
import com.pbm5.bugtracker.dto.BugAttachmentResponse;
import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.BugComment;
import com.pbm5.bugtracker.entity.BugAttachment;
import com.pbm5.bugtracker.entity.BugPriority;
import com.pbm5.bugtracker.entity.BugStatus;
import com.pbm5.bugtracker.entity.BugType;
import com.pbm5.bugtracker.service.BugService;
import com.pbm5.bugtracker.service.BugSecurityService;
import com.pbm5.bugtracker.service.BugCommentService;
import com.pbm5.bugtracker.service.BugAttachmentService;
import com.pbm5.bugtracker.service.ProjectService;
import com.pbm5.bugtracker.service.TeamAssignmentService;
import com.pbm5.bugtracker.service.BugLabelService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bugtracker/v1/projects/{projectSlug}/bugs")
public class BugController {

        private static final Logger logger = LoggerFactory.getLogger(BugController.class);

        @Autowired
        private BugService bugService;

        @Autowired
        private BugSecurityService bugSecurityService;

        @Autowired
        private BugCommentService bugCommentService;

        @Autowired
        private BugAttachmentService bugAttachmentService;

        @Autowired
        private ProjectService projectService;

        @Autowired
        private TeamAssignmentService teamAssignmentService;

        @Autowired
        private BugLabelService bugLabelService;

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
                                throw new IllegalArgumentException(
                                                "Project not found with identifier: " + projectIdentifier);
                        }
                } catch (Exception e) {
                        logger.error("Failed to get project UUID: {}", e.getMessage());
                        throw new IllegalArgumentException("Project not found with identifier: " + projectIdentifier);
                }
        }

        // Helper method to create BugResponse with team assignments
        private BugResponse createBugResponseWithTeams(Bug bug) {
                logger.info("BugController -> createBugResponseWithTeams -> Starting response creation for bug ID: {}",
                                bug.getId());
                logger.info("BugController -> createBugResponseWithTeams -> Bug entity labels count: {}",
                                bug.getLabels() != null ? bug.getLabels().size() : 0);

                if (bug.getLabels() != null && !bug.getLabels().isEmpty()) {
                        String labelNames = bug.getLabels().stream()
                                        .map(label -> label.getName())
                                        .collect(Collectors.joining(", "));
                        logger.info("BugController -> createBugResponseWithTeams -> Bug entity labels: {}", labelNames);
                }

                BugResponse response = new BugResponse(bug);

                logger.info("BugController -> createBugResponseWithTeams -> BugResponse created. Response labels count: {}",
                                response.getLabels() != null ? response.getLabels().size() : 0);

                if (response.getLabels() != null && !response.getLabels().isEmpty()) {
                        String responseLabelNames = response.getLabels().stream()
                                        .map(label -> label.getName())
                                        .collect(Collectors.joining(", "));
                        logger.info("BugController -> createBugResponseWithTeams -> Response labels: {}",
                                        responseLabelNames);
                }

                logger.debug("Creating bug response for bug ID: {}", bug.getId());
                logger.debug("Bug labels count: {}", bug.getLabels() != null ? bug.getLabels().size() : 0);
                if (bug.getLabels() != null && !bug.getLabels().isEmpty()) {
                        logger.debug("Bug labels: {}", bug.getLabels().stream().map(label -> label.getName())
                                        .collect(Collectors.joining(", ")));
                }

                try {
                        // Populate team assignments
                        List<BugTeamAssignment> assignments = teamAssignmentService.getBugTeamAssignments(bug.getId());
                        logger.debug("Found {} team assignments for bug: {}", assignments.size(), bug.getId());

                        List<TeamAssignmentInfo> teamAssignments = assignments.stream()
                                        .map(assignment -> TeamAssignmentInfo.builder()
                                                        .teamId(assignment.getTeam().getId())
                                                        .teamName(assignment.getTeam().getName())
                                                        .teamSlug(assignment.getTeam().getTeamSlug())
                                                        .projectSlug(bug.getProject().getProjectSlug())
                                                        .memberCount(assignment.getTeam().getMemberCount())
                                                        .matchingLabels(new ArrayList<>())
                                                        .labelMatchScore(1.0)
                                                        .isPrimary(assignment.isPrimary())
                                                        .assignmentReason("Previously assigned")
                                                        .build())
                                        .collect(java.util.stream.Collectors.toList());

                        response.setAssignedTeams(teamAssignments);
                        logger.debug("Set {} team assignments in response", teamAssignments.size());
                } catch (Exception e) {
                        logger.warn("Failed to load team assignments for bug: {}", bug.getId(), e);
                        // Don't fail the response if team assignments can't be loaded
                }

                return response;
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

        // CRUD Operations

        /**
         * Create a new bug
         */
        @PostMapping
        public ResponseEntity<BugResponse> createBug(
                        @PathVariable String projectSlug,
                        @Valid @RequestBody CreateBugRequest request,
                        Authentication authentication) {

                logger.debug("createBug method called");
                logger.debug("Raw projectSlug parameter: '{}'", projectSlug);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                logger.debug("About to validate bug create access");
                // Validate user has access to create bugs in this project
                bugSecurityService.validateBugCreateAccess(projectUuid, userId);
                logger.debug("Bug create access validated successfully");

                logger.debug("About to create bug with title: '{}', type: {}, priority: {}",
                                request.getTitle(), request.getType(), request.getPriority());

                Bug bug = bugService.createBugWithTeamAssignment(
                                projectUuid,
                                request.getTitle(),
                                request.getDescription(),
                                request.getType(),
                                request.getPriority(),
                                userId, // reporter
                                request.getAssigneeId(),
                                request.getLabelIds(),
                                request.getTags(),
                                request.getAssignedTeamIds());

                logger.debug("Bug created successfully with ID: {}", bug.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(createBugResponseWithTeams(bug));
        }

        /**
         * Get bug by project ticket number
         */
        @GetMapping("/{projectTicketNumber}")
        public ResponseEntity<BugResponse> getBug(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        Authentication authentication) {

                logger.info("BugController -> getBug -> Starting bug retrieval for project: {}, ticket: {}",
                                projectSlug, projectTicketNumber);

                logger.debug("getBug method called for project ticket number: {}", projectTicketNumber);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                try {
                        // Try to parse directly as UUID first
                        userId = UUID.fromString(authentication.getName());
                        logger.debug("User ID parsed directly as UUID: {}", userId);
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
                                logger.debug("User ID extracted from User object: {}", userId);
                        } else {
                                throw new IllegalArgumentException(
                                                "Could not extract user ID from authentication: " + authName);
                        }
                }

                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                // Validate user has access to view this bug
                bugSecurityService.validateBugViewAccess(bug, userId);

                BugResponse response = createBugResponseWithTeams(bug);

                return ResponseEntity.ok(response);
        }

        /**
         * Update bug
         */
        @PutMapping("/{projectTicketNumber}")
        public ResponseEntity<BugResponse> updateBug(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @Valid @RequestBody UpdateBugRequest request,
                        Authentication authentication) {

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                // Validate user has access to update this bug
                bugSecurityService.validateBugUpdateAccess(bug, userId);

                Bug updatedBug = bugService.updateBug(
                                projectUuid,
                                bug.getId(), // Use the global ID for the service call
                                request.getTitle(),
                                request.getDescription(),
                                request.getType(),
                                request.getPriority(),
                                request.getAssigneeId(),
                                request.getLabelIds(),
                                request.getTags());

                return ResponseEntity.ok(new BugResponse(updatedBug));
        }

        /**
         * Delete bug
         */
        @DeleteMapping("/{projectTicketNumber}")
        public ResponseEntity<Void> deleteBug(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        Authentication authentication) {

                logger.debug("deleteBug method called for project ticket number: {}", projectTicketNumber);
                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                // Validate user has access to delete this bug
                bugSecurityService.validateBugDeleteAccess(bug, userId);

                bugService.deleteBug(projectUuid, bug.getId()); // Use the global ID for the service call

                logger.debug("Bug deleted successfully with project ticket number: {}", projectTicketNumber);
                return ResponseEntity.noContent().build();
        }

        // Status Management

        /**
         * Update bug status
         */
        @PatchMapping("/{projectTicketNumber}/status")
        public ResponseEntity<BugResponse> updateBugStatus(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @Valid @RequestBody UpdateBugStatusRequest request,
                        Authentication authentication) {

                logger.debug("updateBugStatus method called for project ticket number: {}", projectTicketNumber);
                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                Bug bug = bugService.updateBugStatus(projectUuid, projectTicketNumber, request.getStatus(), userId);

                return ResponseEntity.ok(new BugResponse(bug));
        }

        /**
         * Assign bug to user
         */
        @PatchMapping("/{projectTicketNumber}/assign")
        public ResponseEntity<BugResponse> assignBug(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @RequestParam UUID assigneeId,
                        Authentication authentication) {

                UUID userId = getCurrentUserId(authentication);
                String userEmail = authentication.getName();

                logger.info("BugController.assignBug - Request received: projectSlug={}, ticketNumber={}, assigneeId={}, requestedBy={}({})",
                                projectSlug, projectTicketNumber, assigneeId, userEmail, userId);

                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.info("BugController.assignBug - Project resolved: projectSlug={} -> projectId={}",
                                projectSlug, projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);
                logger.info("BugController.assignBug - Security validation passed for user={}({})", userEmail, userId);

                Bug bug = bugService.assignBug(projectUuid, projectTicketNumber, assigneeId, userId);
                logger.info("BugController.assignBug - Assignment completed: bugId={}, ticketNumber={}, assignedTo={}, requestedBy={}({})",
                                bug.getId(), projectTicketNumber,
                                bug.getAssignee() != null ? bug.getAssignee().getEmail() : "none",
                                userEmail, userId);

                return ResponseEntity.ok(new BugResponse(bug));
        }

        /**
         * Unassign bug
         */
        @PatchMapping("/{projectTicketNumber}/unassign")
        public ResponseEntity<BugResponse> unassignBug(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        Authentication authentication) {

                logger.debug("unassignBug method called for project ticket number: {}", projectTicketNumber);
                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                Bug bug = bugService.unassignBug(projectUuid, projectTicketNumber);

                return ResponseEntity.ok(new BugResponse(bug));
        }

        // Query Operations

        /**
         * Get bugs with filtering and pagination
         */
        @GetMapping
        public ResponseEntity<Page<BugResponse>> getBugs(
                        @PathVariable String projectSlug,
                        @RequestParam(required = false) BugStatus status,
                        @RequestParam(required = false) BugPriority priority,
                        @RequestParam(required = false) BugType type,
                        @RequestParam(required = false) String assignee,
                        @RequestParam(required = false) UUID reporterId,
                        @RequestParam(required = false) String searchTerm,
                        Pageable pageable,
                        Authentication authentication) {

                logger.debug("getBugs method called");
                logger.debug("Raw projectSlug parameter: '{}'", projectSlug);

                // Parse projectId as UUID
                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                logger.debug("About to parse userId from authentication.getName(): '{}'", authentication.getName());

                // Extract user ID from authentication
                try {
                        // Try to parse directly as UUID first
                        userId = UUID.fromString(authentication.getName());
                        logger.debug("User ID parsed directly as UUID: {}", userId);
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
                                userId = UUID.fromString(userIdStr);
                                logger.debug("User ID extracted from User object: {}", userId);
                        } else {
                                throw new IllegalArgumentException(
                                                "Could not extract user ID from authentication: " + authName);
                        }
                }

                // Validate user has access to project
                logger.debug("About to validate project access");
                bugSecurityService.validateProjectAccess(projectUuid, userId);
                logger.debug("Project access validated successfully");

                // Handle assignee filtering
                UUID assigneeId = null;
                Boolean assigneeIsNull = null; // null = no filter, true = unassigned, false = assigned

                if (assignee != null && !assignee.isEmpty()) {
                        logger.debug("Processing assignee filter: '{}'", assignee);
                        if ("null".equals(assignee)) {
                                assigneeIsNull = true; // Show only unassigned bugs
                                logger.debug("Filtering for unassigned bugs");
                        } else if ("not-null".equals(assignee)) {
                                assigneeIsNull = false; // Show only assigned bugs
                                logger.debug("Filtering for assigned bugs");
                        } else if ("ASSIGNED_TO_ME".equals(assignee)) {
                                assigneeId = userId; // Show bugs assigned to current user
                                logger.debug("Filtering for bugs assigned to current user: {}", userId);
                        } else {
                                try {
                                        assigneeId = UUID.fromString(assignee);
                                        logger.debug("Filtering for specific assignee: {}", assigneeId);
                                } catch (IllegalArgumentException e) {
                                        logger.warn("Invalid assignee UUID format: {}", assignee);
                                        // If it's not a valid UUID, treat as no filter
                                }
                        }
                }

                Page<Bug> bugs = bugService.getBugsWithFilters(
                                projectUuid, status, priority, type, assigneeId, assigneeIsNull, reporterId, searchTerm,
                                pageable);

                Page<BugResponse> bugResponses = bugs.map(BugResponse::new);

                return ResponseEntity.ok(bugResponses);
        }

        /**
         * Search bugs by term
         */
        @GetMapping("/search")
        public ResponseEntity<Page<BugResponse>> searchBugs(
                        @PathVariable String projectSlug,
                        @RequestParam String searchTerm,
                        Pageable pageable,
                        Authentication authentication) {

                logger.debug("searchBugs method called with searchTerm: '{}'", searchTerm);
                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                logger.debug("About to parse userId from authentication.getName(): '{}'", authentication.getName());

                // Extract user ID from authentication
                try {
                        // Try to parse directly as UUID first
                        userId = UUID.fromString(authentication.getName());
                        logger.debug("User ID parsed directly as UUID: {}", userId);
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
                                userId = UUID.fromString(userIdStr);
                                logger.debug("User ID extracted from User object: {}", userId);
                        } else {
                                throw new IllegalArgumentException(
                                                "Could not extract user ID from authentication: " + authName);
                        }
                }

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                Page<Bug> bugs = bugService.searchBugs(projectUuid, searchTerm, pageable);

                Page<BugResponse> bugResponses = bugs.map(BugResponse::new);

                return ResponseEntity.ok(bugResponses);
        }

        /**
         * Get bugs by status
         */
        @GetMapping("/status/{status}")
        public ResponseEntity<List<BugResponse>> getBugsByStatus(
                        @PathVariable String projectSlug,
                        @PathVariable BugStatus status,
                        Authentication authentication) {

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                List<Bug> bugs = bugService.getBugsByStatus(projectUuid, status);

                List<BugResponse> bugResponses = bugs.stream()
                                .map(BugResponse::new)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(bugResponses);
        }

        /**
         * Get bugs by priority
         */
        @GetMapping("/priority/{priority}")
        public ResponseEntity<List<BugResponse>> getBugsByPriority(
                        @PathVariable String projectSlug,
                        @PathVariable BugPriority priority,
                        Authentication authentication) {

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                List<Bug> bugs = bugService.getBugsByPriority(projectUuid, priority);

                List<BugResponse> bugResponses = bugs.stream()
                                .map(BugResponse::new)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(bugResponses);
        }

        /**
         * Get bugs by assignee
         */
        @GetMapping("/assignee/{assigneeId}")
        public ResponseEntity<List<BugResponse>> getBugsByAssignee(
                        @PathVariable String projectSlug,
                        @PathVariable UUID assigneeId,
                        Authentication authentication) {

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                List<Bug> bugs = bugService.getBugsByAssignee(projectUuid, assigneeId);

                List<BugResponse> bugResponses = bugs.stream()
                                .map(BugResponse::new)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(bugResponses);
        }

        /**
         * Get bugs by reporter
         */
        @GetMapping("/reporter/{reporterId}")
        public ResponseEntity<List<BugResponse>> getBugsByReporter(
                        @PathVariable String projectSlug,
                        @PathVariable UUID reporterId,
                        Authentication authentication) {

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                List<Bug> bugs = bugService.getBugsByReporter(projectUuid, reporterId);

                List<BugResponse> bugResponses = bugs.stream()
                                .map(BugResponse::new)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(bugResponses);
        }

        // Analytics

        /**
         * Get bug statistics
         */
        @GetMapping("/statistics")
        public ResponseEntity<BugService.BugStatistics> getBugStatistics(
                        @PathVariable String projectSlug,
                        Authentication authentication) {

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                BugService.BugStatistics statistics = bugService.getBugStatistics(projectUuid);

                return ResponseEntity.ok(statistics);
        }

        /**
         * Get recent bugs
         */
        @GetMapping("/recent")
        public ResponseEntity<Page<BugResponse>> getRecentBugs(
                        @PathVariable String projectSlug,
                        Pageable pageable,
                        Authentication authentication) {

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                Page<Bug> bugs = bugService.getRecentBugs(projectUuid, pageable);

                Page<BugResponse> bugResponses = bugs.map(BugResponse::new);

                return ResponseEntity.ok(bugResponses);
        }

        // Comment Management

        /**
         * Get comments for a bug
         */
        @GetMapping("/{projectTicketNumber}/comments")
        public ResponseEntity<Page<BugCommentResponse>> getComments(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "createdAt,desc") String sort,
                        Authentication authentication) {

                logger.debug("getComments method called for project ticket number: {}", projectTicketNumber);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                // Create pageable object
                String[] sortParts = sort.split(",");
                String sortField = sortParts[0];
                Sort.Direction sortDirection = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                                ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

                Page<BugComment> comments = bugCommentService.getRecentCommentsByBug(bug.getId(), pageable);

                Page<BugCommentResponse> commentResponses = comments.map(BugCommentResponse::new);

                return ResponseEntity.ok(commentResponses);
        }

        /**
         * Create a new comment
         */
        @PostMapping("/{projectTicketNumber}/comments")
        public ResponseEntity<BugCommentResponse> createComment(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @Valid @RequestBody CreateCommentRequest request,
                        Authentication authentication) {

                logger.debug("createComment method called for project ticket number: {}", projectTicketNumber);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to create comment via service");
                BugComment comment = bugCommentService.createComment(projectUuid, bug.getId(), request.getContent(),
                                userId,
                                request.getParentId());

                logger.debug("Comment created successfully with ID: {}", comment.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(new BugCommentResponse(comment));
        }

        /**
         * Update an existing comment
         */
        @PutMapping("/{projectTicketNumber}/comments/{commentId}")
        public ResponseEntity<BugCommentResponse> updateComment(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @PathVariable Long commentId,
                        @Valid @RequestBody UpdateCommentRequest request,
                        Authentication authentication) {

                logger.debug("updateComment method called for project ticket number: {}, commentId: {}",
                                projectTicketNumber, commentId);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to update comment via service");
                BugComment updatedComment = bugCommentService.updateComment(projectUuid, bug.getId(), commentId,
                                request.getContent(), userId);

                logger.debug("Comment updated successfully with ID: {}", updatedComment.getId());
                return ResponseEntity.ok(new BugCommentResponse(updatedComment));
        }

        /**
         * Delete a comment
         */
        @DeleteMapping("/{projectTicketNumber}/comments/{commentId}")
        public ResponseEntity<Void> deleteComment(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @PathVariable Long commentId,
                        Authentication authentication) {

                logger.debug("deleteComment method called for project ticket number: {}, commentId: {}",
                                projectTicketNumber, commentId);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to delete comment via service");
                bugCommentService.deleteComment(projectUuid, bug.getId(), commentId, userId);

                logger.debug("Comment deleted successfully");
                return ResponseEntity.noContent().build();
        }

        // Attachment Management

        /**
         * Get attachments for a bug
         */
        @GetMapping("/{projectTicketNumber}/attachments")
        public ResponseEntity<List<BugAttachmentResponse>> getAttachments(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        Authentication authentication) {

                logger.debug("getAttachments method called for project ticket number: {}", projectTicketNumber);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to get attachments from service");
                List<BugAttachment> attachments = bugAttachmentService.getAttachmentsByBugId(bug.getId());

                logger.debug("Found {} attachments", attachments.size());
                List<BugAttachmentResponse> attachmentResponses = attachments.stream()
                                .map(BugAttachmentResponse::new)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(attachmentResponses);
        }

        /**
         * Upload attachment to a bug
         */
        @PostMapping("/{projectTicketNumber}/attachments")
        public ResponseEntity<BugAttachmentResponse> uploadAttachment(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @RequestParam("file") MultipartFile file,
                        @RequestParam(value = "description", required = false) String description,
                        Authentication authentication) {

                logger.debug("uploadAttachment method called for project ticket number: {}", projectTicketNumber);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to upload attachment via service");
                BugAttachment attachment = bugAttachmentService.uploadAttachment(projectUuid, bug.getId(), file,
                                userId);

                logger.debug("Attachment uploaded successfully with ID: {}", attachment.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(new BugAttachmentResponse(attachment));
        }

        /**
         * Download attachment
         */
        @GetMapping("/{projectTicketNumber}/attachments/{attachmentId}")
        public ResponseEntity<Resource> downloadAttachment(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @PathVariable Long attachmentId,
                        Authentication authentication) {

                logger.debug("downloadAttachment method called for project ticket number: {}, attachmentId: {}",
                                projectTicketNumber, attachmentId);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to download attachment via service");
                Resource fileResource = bugAttachmentService.downloadAttachment(projectUuid, bug.getId(), attachmentId);

                logger.debug("Attachment downloaded successfully");
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + fileResource.getFilename() + "\"")
                                .body(fileResource);
        }

        /**
         * Preview attachment (for images)
         */
        @GetMapping("/{bugId}/attachments/{attachmentId}/preview")
        public ResponseEntity<Resource> previewAttachment(
                        @PathVariable String projectSlug,
                        @PathVariable Long bugId,
                        @PathVariable Long attachmentId,
                        Authentication authentication) {

                logger.debug("previewAttachment method called for bugId: {}, attachmentId: {}", bugId, attachmentId);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                logger.debug("About to parse userId from authentication.getName(): '{}'", authentication.getName());

                // Extract user ID from authentication
                try {
                        // Try to parse directly as UUID first
                        userId = UUID.fromString(authentication.getName());
                        logger.debug("User ID parsed directly as UUID: {}", userId);
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
                                userId = UUID.fromString(userIdStr);
                                logger.debug("User ID extracted from User object: {}", userId);
                        } else {
                                throw new IllegalArgumentException(
                                                "Could not extract user ID from authentication: " + authName);
                        }
                }

                logger.debug("About to validate project access");
                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                logger.debug("About to preview attachment via service");
                Resource resource = bugAttachmentService.downloadAttachment(projectUuid, bugId, attachmentId);

                logger.debug("Attachment preview generated successfully. Resource exists: {}, readable: {}",
                                resource.exists(), resource.isReadable());

                if (!resource.exists()) {
                        logger.error("Resource does not exist: {}", resource.getFilename());
                        return ResponseEntity.notFound().build();
                }

                if (!resource.isReadable()) {
                        logger.error("Resource is not readable: {}", resource.getFilename());
                        return ResponseEntity.status(500).build();
                }

                return ResponseEntity.ok()
                                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                                .body(resource);
        }

        /**
         * Delete attachment
         */
        @DeleteMapping("/{projectTicketNumber}/attachments/{attachmentId}")
        public ResponseEntity<Void> deleteAttachment(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @PathVariable Long attachmentId,
                        Authentication authentication) {

                logger.debug("deleteAttachment method called for project ticket number: {}, attachmentId: {}",
                                projectTicketNumber, attachmentId);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to delete attachment via service");
                bugAttachmentService.deleteAttachment(projectUuid, bug.getId(), attachmentId);

                logger.debug("Attachment deleted successfully");
                return ResponseEntity.noContent().build();
        }

        /**
         * Get team assignment recommendations for a bug based on labels and tags
         * This endpoint is used during bug creation to show which teams should be
         * assigned
         */
        @PostMapping("/team-assignment-recommendation")
        public ResponseEntity<TeamAssignmentRecommendation> getTeamAssignmentRecommendation(
                        @PathVariable String projectSlug,
                        @RequestBody CreateBugRequest request,
                        Authentication authentication) {

                logger.debug("getTeamAssignmentRecommendation called for project: {} with labels: {}",
                                projectSlug, request.getLabelIds());

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                try {
                        // Create a temporary bug entity for analysis
                        Bug tempBug = new Bug();
                        tempBug.setTitle(request.getTitle());
                        tempBug.setDescription(request.getDescription());

                        // Set labels if provided
                        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
                                Set<BugLabel> labels = bugLabelService.findByIds(request.getLabelIds());
                                tempBug.setLabels(labels);
                        }

                        // Set tags if provided
                        if (request.getTags() != null && !request.getTags().isEmpty()) {
                                tempBug.setTags(request.getTags());
                        }

                        // Set project (needed for team lookup) - create minimal project entity
                        Project project = new Project();
                        project.setId(projectUuid);
                        tempBug.setProject(project);

                        // Get team assignment recommendation
                        TeamAssignmentRecommendation recommendation = teamAssignmentService
                                        .getAssignmentRecommendation(tempBug);

                        logger.info("Team assignment recommendation generated for project: {} with {} teams",
                                        projectSlug, recommendation.getTeamCount());

                        return ResponseEntity.ok(recommendation);

                } catch (Exception e) {
                        logger.error("Failed to generate team assignment recommendation for project: {}", projectSlug,
                                        e);
                        throw new RuntimeException("Failed to generate team assignment recommendation", e);
                }
        }

        /**
         * Get team assignment recommendations for a bug
         */
        @GetMapping("/{projectTicketNumber}/team-recommendations")
        public ResponseEntity<TeamAssignmentRecommendation> getTeamAssignmentRecommendations(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        Authentication authentication) {

                logger.debug("getTeamAssignmentRecommendations method called for project ticket number: {}",
                                projectTicketNumber);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to get team assignment recommendations via service");
                TeamAssignmentRecommendation recommendations = teamAssignmentService.getAssignmentRecommendation(bug);

                logger.debug("Team assignment recommendations retrieved successfully");
                return ResponseEntity.ok(recommendations);
        }

        /**
         * Get current team assignments for a bug
         */
        @GetMapping("/{projectTicketNumber}/teams")
        public ResponseEntity<List<TeamAssignmentInfo>> getBugTeamAssignments(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        Authentication authentication) {

                logger.debug("getBugTeamAssignments called for project ticket number: {} in project: {}",
                                projectTicketNumber, projectSlug);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to get team assignments from service");
                List<BugTeamAssignment> assignments = teamAssignmentService.getBugTeamAssignments(bug.getId());

                logger.debug("Found {} team assignments", assignments.size());
                List<TeamAssignmentInfo> teamAssignments = assignments.stream()
                                .map(assignment -> TeamAssignmentInfo.builder()
                                                .teamId(assignment.getTeam().getId())
                                                .teamName(assignment.getTeam().getName())
                                                .teamSlug(assignment.getTeam().getTeamSlug())
                                                .projectSlug(projectSlug)
                                                .memberCount(assignment.getTeam().getMemberCount())
                                                .matchingLabels(new ArrayList<>()) // Not applicable for existing
                                                                                   // assignments
                                                .labelMatchScore(1.0) // Perfect match for existing assignments
                                                .isPrimary(assignment.isPrimary())
                                                .assignmentReason("Previously assigned")
                                                .build())
                                .collect(Collectors.toList());

                return ResponseEntity.ok(teamAssignments);
        }

        /**
         * Update team assignments for a bug
         */
        @PutMapping("/{projectTicketNumber}/teams")
        public ResponseEntity<List<TeamAssignmentInfo>> updateBugTeamAssignments(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @RequestBody Set<UUID> teamIds,
                        Authentication authentication) {

                logger.debug("updateBugTeamAssignments called for project ticket number: {} in project: {} with {} teams",
                                projectTicketNumber, projectSlug, teamIds.size());

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                try {
                        logger.debug("About to update team assignments via service");
                        List<BugTeamAssignment> assignments = teamAssignmentService.updateTeamAssignments(bug.getId(),
                                        teamIds, userId);

                        logger.debug("Found {} team assignments", assignments.size());
                        List<TeamAssignmentInfo> teamAssignments = assignments.stream()
                                        .map(assignment -> TeamAssignmentInfo.builder()
                                                        .teamId(assignment.getTeam().getId())
                                                        .teamName(assignment.getTeam().getName())
                                                        .teamSlug(assignment.getTeam().getTeamSlug())
                                                        .projectSlug(projectSlug)
                                                        .memberCount(assignment.getTeam().getMemberCount())
                                                        .matchingLabels(new ArrayList<>()) // Not applicable for
                                                                                           // existing assignments
                                                        .labelMatchScore(1.0) // Perfect match for existing assignments
                                                        .isPrimary(assignment.isPrimary())
                                                        .assignmentReason("Updated by user")
                                                        .build())
                                        .collect(Collectors.toList());

                        logger.info("Successfully updated team assignments for project ticket number: {} in project: {}",
                                        projectTicketNumber, projectSlug);
                        return ResponseEntity.ok(teamAssignments);

                } catch (Exception e) {
                        logger.error("Failed to update team assignments for project ticket number: {} in project: {}",
                                        projectTicketNumber, projectSlug, e);
                        throw new RuntimeException("Failed to update team assignments", e);
                }
        }

        /**
         * Remove a specific team assignment for a bug
         */
        @DeleteMapping("/{projectTicketNumber}/teams/{teamId}")
        public ResponseEntity<Void> removeBugTeamAssignment(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @PathVariable String teamId,
                        Authentication authentication) {

                logger.debug("removeBugTeamAssignment called for project ticket number: {} team: {} in project: {}",
                                projectTicketNumber, teamId, projectSlug);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                try {
                        UUID teamUuid = UUID.fromString(teamId);

                        // Get the bug to validate it exists and get its global ID
                        Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                        teamAssignmentService.removeTeamAssignment(bug.getId(), teamUuid, userId);

                        logger.info("Successfully removed team assignment: project ticket number {} -> team {} in project: {}",
                                        projectTicketNumber, teamId, projectSlug);
                        return ResponseEntity.noContent().build();

                } catch (Exception e) {
                        logger.error("Failed to remove team assignment: project ticket number {} -> team {} in project: {}",
                                        projectTicketNumber, teamId, projectSlug, e);
                        throw new RuntimeException("Failed to remove team assignment", e);
                }
        }

        /**
         * Assign a team to a bug
         */
        @PostMapping("/{projectTicketNumber}/teams")
        public ResponseEntity<TeamAssignmentInfo> assignTeamToBug(
                        @PathVariable String projectSlug,
                        @PathVariable Integer projectTicketNumber,
                        @RequestParam UUID teamId,
                        @RequestParam(defaultValue = "false") boolean isPrimary,
                        Authentication authentication) {

                logger.debug("assignTeamToBug called for project ticket number: {} in project: {} with team: {}",
                                projectTicketNumber, projectSlug, teamId);

                UUID userId = getCurrentUserId(authentication);
                UUID projectUuid = getProjectUuidFromSlug(projectSlug, userId);
                logger.debug("Project UUID parsed successfully: {}", projectUuid);

                // Validate user has access to project
                bugSecurityService.validateProjectAccess(projectUuid, userId);

                // Get the bug to validate it exists and get its global ID
                Bug bug = bugService.getBugByProjectTicketNumber(projectUuid, projectTicketNumber);

                logger.debug("About to assign team to bug via service");
                List<BugTeamAssignment> assignments = teamAssignmentService.saveTeamAssignments(bug, Set.of(teamId),
                                userId);

                logger.debug("Team assigned successfully to bug");
                BugTeamAssignment assignment = assignments.get(0); // Get the first (and only) assignment
                TeamAssignmentInfo teamAssignment = TeamAssignmentInfo.builder()
                                .teamId(assignment.getTeam().getId())
                                .teamName(assignment.getTeam().getName())
                                .teamSlug(assignment.getTeam().getTeamSlug())
                                .projectSlug(projectSlug)
                                .memberCount(assignment.getTeam().getMemberCount())
                                .matchingLabels(new ArrayList<>()) // Not applicable for manual assignment
                                .labelMatchScore(1.0) // Perfect match for manual assignment
                                .isPrimary(assignment.isPrimary())
                                .assignmentReason("Manually assigned by user")
                                .build();

                return ResponseEntity.ok(teamAssignment);
        }
}
