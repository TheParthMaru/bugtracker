package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation;
import com.pbm5.bugtracker.entity.*;
import com.pbm5.bugtracker.exception.BugNotFoundException;
import com.pbm5.bugtracker.exception.InvalidBugOperationException;
import com.pbm5.bugtracker.exception.ProjectNotFoundException;
import com.pbm5.bugtracker.exception.UserNotFoundException;
import com.pbm5.bugtracker.repository.BugRepository;
import com.pbm5.bugtracker.repository.ProjectRepository;
import com.pbm5.bugtracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@Transactional
public class BugService {

    private static final Logger logger = LoggerFactory.getLogger(BugService.class);

    @Autowired
    private BugRepository bugRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BugLabelService bugLabelService;

    @Autowired
    private TeamAssignmentService teamAssignmentService;

    @Autowired
    private AssignmentOrchestrator assignmentOrchestrator;

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private BugNotificationEventListener bugNotificationEventListener;

    // CRUD Operations

    /**
     * Create a new bug
     */
    public Bug createBug(UUID projectId, String title, String description, BugType type,
            BugPriority priority, UUID reporterId, UUID assigneeId, Set<Long> labelIds, Set<String> tags) {

        // Validate project exists
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with id: " + projectId));

        // Validate reporter exists
        var reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new UserNotFoundException("Reporter not found with id: " + reporterId));

        // Validate assignee if provided
        User assignee = null;
        if (assigneeId != null) {
            assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new UserNotFoundException("Assignee not found with id: " + assigneeId));
        }

        // Generate project-specific ticket number
        Integer nextTicketNumber = bugRepository.getNextProjectTicketNumber(projectId);

        // Create bug
        Bug bug = new Bug(title, description, type, priority, project, reporter);
        bug.setProjectTicketNumber(nextTicketNumber);

        // Set assignee if provided
        if (assignee != null) {
            bug.assignTo(assignee);
        }

        // Add labels if provided
        if (labelIds != null && !labelIds.isEmpty()) {
            logger.info("BugService -> createBug -> Adding labels to bug. Label IDs: {}", labelIds);
            Set<BugLabel> labels = bugLabelService.findByIds(labelIds);
            logger.info("BugService -> createBug -> Labels retrieved from service. Count: {}", labels.size());

            // Log before setting labels
            logger.info("BugService -> createBug -> About to set labels on bug. Bug ID will be: {}", bug.getId());
            bug.setLabels(labels);
            logger.info("BugService -> createBug -> Labels set on bug. Bug labels count: {}",
                    bug.getLabels() != null ? bug.getLabels().size() : 0);

            // Log the actual label objects
            if (bug.getLabels() != null && !bug.getLabels().isEmpty()) {
                String labelDetails = bug.getLabels().stream()
                        .map(label -> String.format("ID:%d,Name:%s", label.getId(), label.getName()))
                        .collect(Collectors.joining(", "));
                logger.info("BugService -> createBug -> Bug labels after setLabels(): {}", labelDetails);
            }
        } else {
            logger.info("BugService -> createBug -> No labels provided for bug");
        }

        // Add tags if provided
        if (tags != null && !tags.isEmpty()) {
            bug.setTags(tags);
        }

        // Auto-assign high priority bugs if no assignee specified
        if (assignee == null && priority.requiresImmediateAssignment()) {
            autoAssignHighPriorityBug(bug);
        }

        // Force immediate persistence to ensure labels are saved
        logger.info("BugService -> createBug -> About to save bug with labels. Labels count before save: {}",
                bug.getLabels() != null ? bug.getLabels().size() : 0);

        Bug savedBug = bugRepository.saveAndFlush(bug);

        logger.info("BugService -> createBug -> Bug saved with ID: {}, Labels count: {}",
                savedBug.getId(), savedBug.getLabels() != null ? savedBug.getLabels().size() : 0);

        // Log the actual label objects after save
        if (savedBug.getLabels() != null && !savedBug.getLabels().isEmpty()) {
            String labelDetails = savedBug.getLabels().stream()
                    .map(label -> String.format("ID:%d,Name:%s", label.getId(), label.getName()))
                    .collect(Collectors.joining(", "));
            logger.info("BugService -> createBug -> Saved bug labels: {}", labelDetails);
        } else {
            logger.warn("BugService -> createBug -> WARNING: Saved bug has no labels!");
        }

        return savedBug;
    }

    /**
     * Create a new bug with team assignment support
     */
    public Bug createBugWithTeamAssignment(UUID projectId, String title, String description, BugType type,
            BugPriority priority, UUID reporterId, UUID assigneeId, Set<Long> labelIds, Set<String> tags,
            Set<String> assignedTeamIds) {

        // Create the bug first
        Bug bug = createBug(projectId, title, description, type, priority, reporterId, assigneeId, labelIds, tags);

        // Handle team assignments if provided
        if (assignedTeamIds != null && !assignedTeamIds.isEmpty()) {
            try {
                // Convert string team IDs to UUIDs
                Set<UUID> teamUuids = assignedTeamIds.stream()
                        .map(UUID::fromString)
                        .collect(java.util.stream.Collectors.toSet());

                // Save team assignments
                teamAssignmentService.saveTeamAssignments(bug, teamUuids, reporterId);
                logger.info("Successfully assigned bug {} to {} teams", bug.getId(), teamUuids.size());
            } catch (Exception e) {
                logger.error("Failed to assign teams to bug: {}", bug.getId(), e);
                // Don't fail bug creation if team assignment fails
            }
        } else {
            // Auto-assign teams based on labels and tags
            autoAssignTeamsToBug(bug, reporterId);

            // Execute complete auto-assignment (teams + users)
            executeCompleteAutoAssignment(bug, reporterId);
        }

        return bug;
    }

    /**
     * Execute complete auto-assignment for a bug (teams + users)
     * This method should be called after bug creation to automatically assign
     * both teams and users based on labels and tags.
     * 
     * @param bug        the bug to auto-assign
     * @param reporterId the ID of the user who reported the bug
     * @return the assigned user, or null if no suitable user found
     */
    public User executeCompleteAutoAssignment(Bug bug, UUID reporterId) {
        logger.info("Executing complete auto-assignment for bug: {}", bug.getId());

        try {
            // Execute the complete auto-assignment workflow
            User assignedUser = assignmentOrchestrator.executeCompleteAutoAssignment(bug);

            if (assignedUser != null) {
                logger.info("Complete auto-assignment successful for bug: {} - User: {} ({} {})",
                        bug.getId(), assignedUser.getId(), assignedUser.getFirstName(), assignedUser.getLastName());

                // Update the bug with the assigned user
                bug.assignTo(assignedUser);
                bugRepository.save(bug);

                logger.info("Bug {} successfully assigned to user: {} ({} {})",
                        bug.getId(), assignedUser.getId(), assignedUser.getFirstName(), assignedUser.getLastName());
            } else {
                logger.info("Team assignment successful but no suitable user found for bug: {}", bug.getId());
            }

            return assignedUser;

        } catch (Exception e) {
            logger.error("Error during complete auto-assignment for bug: {}", bug.getId(), e);
            // Don't fail the entire operation if auto-assignment fails
            return null;
        }
    }

    /**
     * Get bug by ID with project validation
     */
    public Bug getBugById(UUID projectId, Long bugId) {
        // First get the basic bug with labels
        Bug bug = bugRepository.findByProjectIdAndIdWithLabels(projectId, bugId)
                .orElseThrow(() -> new BugNotFoundException("Bug not found with id: " + bugId));

        // Then fetch attachments and comments separately to avoid Hibernate
        // MultipleBagFetchException
        try {
            // Fetch attachments
            Optional<Bug> bugWithAttachments = bugRepository.findByProjectIdAndIdWithAttachments(projectId, bugId);
            if (bugWithAttachments.isPresent()) {
                bug.setAttachments(bugWithAttachments.get().getAttachments());
            }

            // Fetch comments
            Optional<Bug> bugWithComments = bugRepository.findByProjectIdAndIdWithComments(projectId, bugId);
            if (bugWithComments.isPresent()) {
                bug.setComments(bugWithComments.get().getComments());
            }
        } catch (Exception e) {
            // Log the error but don't fail the entire operation
            logger.warn("Failed to fetch attachments or comments for bug {}: {}", bugId, e.getMessage());
        }

        return bug;
    }

    /**
     * Get bug by project ticket number (the correct way to identify bugs within a
     * project)
     */
    public Bug getBugByProjectTicketNumber(UUID projectId, Integer projectTicketNumber) {
        logger.info("BugService -> getBugByProjectTicketNumber -> Starting bug retrieval for project: {}, ticket: {}",
                projectId, projectTicketNumber);

        // First get the basic bug with labels
        logger.info("BugService -> getBugByProjectTicketNumber -> Fetching bug with labels from repository");
        Bug bug = bugRepository.findByProjectIdAndProjectTicketNumberWithLabels(projectId, projectTicketNumber)
                .orElseThrow(() -> new BugNotFoundException(
                        "Bug not found with project ticket number: " + projectTicketNumber));

        logger.info(
                "BugService -> getBugByProjectTicketNumber -> Bug retrieved from repository - ID: {}, Title: {}, Labels: {}, Tags: {}",
                bug.getId(), bug.getTitle(),
                bug.getLabels() != null ? bug.getLabels().size() : 0,
                bug.getTags() != null ? bug.getTags().size() : 0);

        // Check if labels are actually loaded
        if (bug.getLabels() != null) {
            logger.info("BugService -> getBugByProjectTicketNumber -> Labels details - Count: {}, Type: {}",
                    bug.getLabels().size(), bug.getLabels().getClass().getSimpleName());

            if (!bug.getLabels().isEmpty()) {
                String labelDetails = bug.getLabels().stream()
                        .map(label -> String.format("ID:%d,Name:%s", label.getId(), label.getName()))
                        .collect(Collectors.joining(", "));
                logger.info("BugService -> getBugByProjectTicketNumber -> Label details: {}", labelDetails);
            } else {
                logger.warn("BugService -> getBugByProjectTicketNumber -> Labels collection is empty!");
            }
        } else {
            logger.warn("BugService -> getBugByProjectTicketNumber -> Labels is null");
        }

        // Then fetch attachments and comments separately to avoid Hibernate
        // MultipleBagFetchException
        try {
            logger.info("BugService -> getBugByProjectTicketNumber -> Fetching attachments");
            // Fetch attachments
            Optional<Bug> bugWithAttachments = bugRepository
                    .findByProjectIdAndProjectTicketNumberWithAttachments(projectId, projectTicketNumber);
            if (bugWithAttachments.isPresent()) {
                bug.setAttachments(bugWithAttachments.get().getAttachments());
                logger.info("BugService -> getBugByProjectTicketNumber -> Attachments loaded: {}",
                        bug.getAttachments().size());
            }

            logger.info("BugService -> getBugByProjectTicketNumber -> Fetching comments");
            // Fetch comments
            Optional<Bug> bugWithComments = bugRepository.findByProjectIdAndProjectTicketNumberWithComments(projectId,
                    projectTicketNumber);
            if (bugWithComments.isPresent()) {
                bug.setComments(bugWithComments.get().getComments());
                logger.info("BugService -> getBugByProjectTicketNumber -> Comments loaded: {}",
                        bug.getComments().size());
            }
        } catch (Exception e) {
            // Log the error but don't fail the entire operation
            logger.warn("Failed to fetch attachments or comments for bug {}: {}", projectTicketNumber, e.getMessage());
        }

        // Final check before returning
        logger.info(
                "BugService -> getBugByProjectTicketNumber -> Final bug object before return - ID: {}, Labels: {}, Tags: {}",
                bug.getId(),
                bug.getLabels() != null ? bug.getLabels().size() : 0,
                bug.getTags() != null ? bug.getTags().size() : 0);

        return bug;
    }

    /**
     * Update bug details
     */
    public Bug updateBug(UUID projectId, Long bugId, String title, String description,
            BugType type, BugPriority priority, UUID assigneeId, Set<Long> labelIds, Set<String> tags) {

        Bug bug = getBugById(projectId, bugId);

        // Store old values for notification purposes
        BugPriority oldPriority = bug.getPriority();
        BugStatus oldStatus = bug.getStatus();

        logger.debug("BugService.updateBug - Stored old values: oldPriority={}, oldStatus={}, bugId={}",
                oldPriority, oldStatus, bug.getId());

        // Update basic fields
        if (title != null) {
            bug.setTitle(title);
        }
        if (description != null) {
            bug.setDescription(description);
        }
        if (type != null) {
            bug.setType(type);
        }
        if (priority != null) {
            logger.debug("BugService.updateBug - Setting new priority: {} for bugId={}", priority, bug.getId());
            bug.setPriority(priority);
        }

        // Update assignee if provided
        if (assigneeId != null) {
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new UserNotFoundException("Assignee not found with id: " + assigneeId));
            bug.assignTo(assignee);
        }

        // Update labels if provided
        if (labelIds != null) {
            Set<BugLabel> labels = bugLabelService.findByIds(labelIds);
            bug.setLabels(labels);
        }

        // Update tags if provided
        if (tags != null) {
            bug.setTags(tags);
        }

        Bug savedBug = bugRepository.save(bug);

        // Trigger notification for priority change if it changed (async - won't affect
        // transaction)
        if (priority != null && !priority.equals(oldPriority)) {
            logger.info("BugService.updateBug - Priority change detected: oldPriority={}, newPriority={}, bugId={}",
                    oldPriority, priority, savedBug.getId());
            try {
                // Get current user from security context
                User currentUser = getCurrentUserFromSecurityContext();
                if (currentUser != null) {
                    logger.info(
                            "BugService.updateBug - Triggering async notification for priority change: bugId={}, oldPriority={}, newPriority={}, changedBy={}({})",
                            savedBug.getId(), oldPriority, priority, currentUser.getEmail(), currentUser.getId());
                    bugNotificationEventListener.onBugPriorityChanged(savedBug, oldPriority, priority, currentUser);
                    logger.info(
                            "BugService.updateBug - Async notification triggered for priority change: bugId={}, oldPriority={}, newPriority={}, changedBy={}({})",
                            savedBug.getId(), oldPriority, priority, currentUser.getEmail(), currentUser.getId());
                } else {
                    logger.warn(
                            "BugService.updateBug - Current user not found for priority change notification: bugId={}",
                            savedBug.getId());
                }
            } catch (Exception e) {
                logger.warn("BugService.updateBug - Notification processing failed for bug {} priority change: {}",
                        savedBug.getId(), e.getMessage());
                // Don't fail the bug update if notification fails
            }
        } else {
            logger.debug("BugService.updateBug - No priority change detected: oldPriority={}, newPriority={}, bugId={}",
                    oldPriority, priority, savedBug.getId());
        }

        return savedBug;
    }

    /**
     * Get current user from security context
     */
    private User getCurrentUserFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("User not authenticated in security context");
                return null;
            }

            // Extract user email from the User object in the principal
            String userEmail;
            if (authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                userEmail = user.getEmail();
                logger.debug("Extracted email from User principal: {}", userEmail);
            } else {
                // Fallback to getName() if principal is not a User object
                userEmail = authentication.getName();
                logger.debug("Using authentication.getName() as fallback: {}", userEmail);
            }

            if (userEmail == null || userEmail.isEmpty()) {
                logger.warn("User email not found in authentication");
                return null;
            }

            // Get user from database
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                return userOpt.get();
            } else {
                logger.warn("User not found in database for email: {}", userEmail);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error getting current user from security context: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Delete bug
     */
    public void deleteBug(UUID projectId, Long bugId) {
        Bug bug = getBugById(projectId, bugId);
        bugRepository.delete(bug);
    }

    // Status Management

    /**
     * Update bug status with validation and gamification integration
     */
    public Bug updateBugStatus(UUID projectId, Long bugId, BugStatus newStatus, UUID currentUserId) {
        Bug bug = getBugById(projectId, bugId);
        BugStatus oldStatus = bug.getStatus();

        // Validate status transition
        if (!bug.canTransitionTo(newStatus)) {
            throw new InvalidBugOperationException(
                    "Invalid status transition from " + oldStatus + " to " + newStatus);
        }

        // Perform status transition
        bug.transitionTo(newStatus);
        Bug savedBug = bugRepository.save(bug);

        // Handle gamification after successful status update
        try {
            handleGamificationForStatusChange(bug, oldStatus, newStatus, projectId, currentUserId);
        } catch (Exception e) {
            logger.warn("Gamification processing failed for bug {} status change from {} to {}: {}",
                    bugId, oldStatus, newStatus, e.getMessage());
            // Don't fail the status update if gamification fails
        }

        // Trigger notification for status change (async - won't affect transaction)
        try {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                logger.info(
                        "BugService.updateBugStatus - Triggering async notification for status change: bugId={}, oldStatus={}, newStatus={}, changedBy={}({})",
                        savedBug.getId(), oldStatus, newStatus, currentUser.getEmail(), currentUserId);
                bugNotificationEventListener.onBugStatusChanged(savedBug, oldStatus, newStatus, currentUser);
                logger.info(
                        "BugService.updateBugStatus - Async notification triggered for status change: bugId={}, oldStatus={}, newStatus={}, changedBy={}({})",
                        savedBug.getId(), oldStatus, newStatus, currentUser.getEmail(), currentUserId);
            } else {
                logger.warn("BugService.updateBugStatus - Current user not found for notification: userId={}",
                        currentUserId);
            }
        } catch (Exception e) {
            logger.warn("BugService.updateBugStatus - Notification processing failed for bug {} status change: {}",
                    savedBug.getId(), e.getMessage());
            // Don't fail the status update if notification fails
        }

        return savedBug;
    }

    /**
     * Update bug status by project ticket number with gamification integration
     */
    public Bug updateBugStatus(UUID projectId, Integer projectTicketNumber, BugStatus newStatus, UUID currentUserId) {
        Bug bug = getBugByProjectTicketNumber(projectId, projectTicketNumber);
        BugStatus oldStatus = bug.getStatus();

        // Validate status transition
        if (!bug.canTransitionTo(newStatus)) {
            throw new InvalidBugOperationException(
                    "Invalid status transition from " + oldStatus + " to " + newStatus);
        }

        // Perform status transition
        bug.transitionTo(newStatus);
        Bug savedBug = bugRepository.save(bug);

        // Handle gamification after successful status update
        try {
            handleGamificationForStatusChange(bug, oldStatus, newStatus, projectId, currentUserId);
        } catch (Exception e) {
            logger.warn("Gamification processing failed for bug {} status change from {} to {}: {}",
                    bug.getId(), oldStatus, newStatus, e.getMessage());
        }

        // Trigger notification for status change (async - won't affect transaction)
        try {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                logger.info(
                        "BugService.updateBugStatus by ticket - Triggering async notification for status change: bugId={}, ticketNumber={}, oldStatus={}, newStatus={}, changedBy={}({})",
                        savedBug.getId(), projectTicketNumber, oldStatus, newStatus, currentUser.getEmail(),
                        currentUserId);
                bugNotificationEventListener.onBugStatusChanged(savedBug, oldStatus, newStatus, currentUser);
                logger.info(
                        "BugService.updateBugStatus by ticket - Async notification triggered for status change: bugId={}, ticketNumber={}, oldStatus={}, newStatus={}, changedBy={}({})",
                        savedBug.getId(), projectTicketNumber, oldStatus, newStatus, currentUser.getEmail(),
                        currentUserId);
            } else {
                logger.warn("BugService.updateBugStatus by ticket - Current user not found for notification: userId={}",
                        currentUserId);
            }
        } catch (Exception e) {
            logger.warn(
                    "BugService.updateBugStatus by ticket - Notification processing failed for bug {} status change: {}",
                    savedBug.getId(), e.getMessage());
            // Don't fail the status update if notification fails
        }

        return savedBug;
    }

    /**
     * Handle gamification logic for bug status changes
     */
    private void handleGamificationForStatusChange(Bug bug, BugStatus oldStatus, BugStatus newStatus, UUID projectId,
            UUID currentUserId) {
        // Get the user who made the status change (assignee or reporter)
        UUID userId = bug.getAssignee() != null ? bug.getAssignee().getId() : bug.getReporter().getId();

        logger.debug("Processing gamification for bug {} status change: {} -> {} by user {} (current user: {})",
                bug.getId(), oldStatus, newStatus, userId, currentUserId);

        // Handle points-awarding transitions (resolution)
        if (isResolutionTransition(oldStatus, newStatus)) {
            String priority = bug.getPriority().name();
            logger.info("Awarding points for bug resolution: {} -> {} (Priority: {})", oldStatus, newStatus, priority);
            gamificationService.handleBugResolution(bug.getId(), userId, projectId, priority);
        }

        // Handle points-deducting transitions (reopening)
        else if (isReopeningTransition(oldStatus, newStatus)) {
            // IMPORTANT: Only apply penalty if the bug is being reopened due to incorrect
            // fix
            // AND the user who made the incorrect fix is still the assignee
            // This prevents penalizing users for admin decisions or reassignments

            if (bug.getAssignee() != null && bug.getAssignee().getId().equals(userId)) {
                // The user who made the incorrect fix is still assigned - apply penalty
                logger.info("Applying penalty for bug reopening: {} -> {} to user {} (incorrect fix)",
                        oldStatus, newStatus, userId);
                gamificationService.handleBugReopening(bug.getId(), userId, projectId);

                // Log that admin will see success message (not penalty)
                logger.info("Admin user {} reopened bug - penalty applied to fixer {}, admin will see success message",
                        currentUserId, userId);
            } else {
                // Bug was reassigned or admin is reopening - no penalty
                logger.info("Bug reopening: {} -> {} - no penalty applied (reassigned or admin decision)",
                        oldStatus, newStatus);
            }
        }

        // Handle neutral transitions (no points)
        else {
            logger.debug("No gamification needed for status transition: {} -> {}", oldStatus, newStatus);
        }
    }

    /**
     * Check if status transition awards points (resolution)
     */
    private boolean isResolutionTransition(BugStatus oldStatus, BugStatus newStatus) {
        return (oldStatus == BugStatus.OPEN || oldStatus == BugStatus.REOPENED) &&
                (newStatus == BugStatus.FIXED || newStatus == BugStatus.CLOSED);
    }

    /**
     * Check if status transition applies penalty (reopening)
     */
    private boolean isReopeningTransition(BugStatus oldStatus, BugStatus newStatus) {
        return (oldStatus == BugStatus.FIXED || oldStatus == BugStatus.CLOSED) &&
                newStatus == BugStatus.REOPENED;
    }

    /**
     * Assign bug to user
     */
    public Bug assignBug(UUID projectId, Long bugId, UUID assigneeId) {
        return assignBug(projectId, bugId, assigneeId, null);
    }

    /**
     * Assign bug to user with assigned by information
     */
    public Bug assignBug(UUID projectId, Long bugId, UUID assigneeId, UUID assignedByUserId) {
        logger.info("BugService.assignBug - Starting bug assignment: projectId={}, bugId={}, assigneeId={}",
                projectId, bugId, assigneeId);

        Bug bug = getBugById(projectId, bugId);
        logger.info("BugService.assignBug - Bug retrieved: id={}, title='{}', currentAssignee={}",
                bug.getId(), bug.getTitle(),
                bug.getAssignee() != null ? bug.getAssignee().getEmail() : "none");

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new UserNotFoundException("Assignee not found with id: " + assigneeId));

        logger.info("BugService.assignBug - Assignee found: id={}, email={}, name={} {}",
                assignee.getId(), assignee.getEmail(), assignee.getFirstName(), assignee.getLastName());

        User previousAssignee = bug.getAssignee();
        bug.assignTo(assignee);
        Bug savedBug = bugRepository.save(bug);

        logger.info("BugService.assignBug - Bug assignment saved: bugId={}, previousAssignee={}, newAssignee={}",
                savedBug.getId(),
                previousAssignee != null ? previousAssignee.getEmail() : "none",
                assignee.getEmail());

        // Trigger notification for the assignment (async - won't affect transaction)
        User assignedByUser = null;
        if (assignedByUserId != null) {
            assignedByUser = userRepository.findById(assignedByUserId).orElse(null);
            logger.info("BugService.assignBug - AssignedBy user found: id={}, email={}",
                    assignedByUser.getId(), assignedByUser.getEmail());
        }

        logger.info(
                "BugService.assignBug - Triggering async notification for bug assignment: bugId={}, assigneeEmail={}, assignedByEmail={}",
                savedBug.getId(), assignee.getEmail(),
                assignedByUser != null ? assignedByUser.getEmail() : "system");
        bugNotificationEventListener.onBugAssigned(savedBug, assignee, assignedByUser);
        logger.info(
                "BugService.assignBug - Async notification triggered: bugId={}, assigneeEmail={}, assignedByEmail={}",
                savedBug.getId(), assignee.getEmail(),
                assignedByUser != null ? assignedByUser.getEmail() : "system");

        return savedBug;
    }

    /**
     * Assign bug to user by project ticket number
     */
    public Bug assignBug(UUID projectId, Integer projectTicketNumber, UUID assigneeId) {
        return assignBug(projectId, projectTicketNumber, assigneeId, null);
    }

    /**
     * Assign bug to user by project ticket number with assigned by information
     */
    public Bug assignBug(UUID projectId, Integer projectTicketNumber, UUID assigneeId, UUID assignedByUserId) {
        logger.info(
                "BugService.assignBug - Starting bug assignment by ticket: projectId={}, ticketNumber={}, assigneeId={}",
                projectId, projectTicketNumber, assigneeId);

        Bug bug = getBugByProjectTicketNumber(projectId, projectTicketNumber);
        logger.info("BugService.assignBug - Bug retrieved by ticket: id={}, title='{}', currentAssignee={}",
                bug.getId(), bug.getTitle(),
                bug.getAssignee() != null ? bug.getAssignee().getEmail() : "none");

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new UserNotFoundException("Assignee not found with id: " + assigneeId));

        logger.info("BugService.assignBug - Assignee found: id={}, email={}, name={} {}",
                assignee.getId(), assignee.getEmail(), assignee.getFirstName(), assignee.getLastName());

        User previousAssignee = bug.getAssignee();
        bug.assignTo(assignee);
        Bug savedBug = bugRepository.save(bug);

        logger.info(
                "BugService.assignBug - Bug assignment by ticket saved: bugId={}, ticketNumber={}, previousAssignee={}, newAssignee={}",
                savedBug.getId(), projectTicketNumber,
                previousAssignee != null ? previousAssignee.getEmail() : "none",
                assignee.getEmail());

        // Trigger notification for the assignment (async - won't affect transaction)
        User assignedByUser = null;
        if (assignedByUserId != null) {
            assignedByUser = userRepository.findById(assignedByUserId).orElse(null);
            logger.info("BugService.assignBug - AssignedBy user found by ticket: id={}, email={}",
                    assignedByUser.getId(), assignedByUser.getEmail());
        }

        logger.info(
                "BugService.assignBug - Triggering async notification for bug assignment by ticket: bugId={}, ticketNumber={}, assigneeEmail={}, assignedByEmail={}",
                savedBug.getId(), projectTicketNumber, assignee.getEmail(),
                assignedByUser != null ? assignedByUser.getEmail() : "system");
        bugNotificationEventListener.onBugAssigned(savedBug, assignee, assignedByUser);
        logger.info(
                "BugService.assignBug - Async notification triggered by ticket: bugId={}, ticketNumber={}, assigneeEmail={}, assignedByEmail={}",
                savedBug.getId(), projectTicketNumber, assignee.getEmail(),
                assignedByUser != null ? assignedByUser.getEmail() : "system");

        return savedBug;
    }

    /**
     * Unassign bug
     */
    public Bug unassignBug(UUID projectId, Long bugId) {
        Bug bug = getBugById(projectId, bugId);
        bug.unassign();
        return bugRepository.save(bug);
    }

    /**
     * Unassign bug by project ticket number
     */
    public Bug unassignBug(UUID projectId, Integer projectTicketNumber) {
        Bug bug = getBugByProjectTicketNumber(projectId, projectTicketNumber);
        bug.unassign();
        return bugRepository.save(bug);
    }

    // Search and Filtering

    /**
     * Get bugs with advanced filtering
     */
    public Page<Bug> getBugsWithFilters(UUID projectId, BugStatus status, BugPriority priority,
            BugType type, UUID assigneeId, Boolean assigneeIsNull, UUID reporterId,
            String searchTerm, Pageable pageable) {
        return bugRepository.findByProjectIdWithFilters(projectId, status, priority, type,
                assigneeId, assigneeIsNull, reporterId, searchTerm, pageable);
    }

    /**
     * Search bugs by term
     */
    public Page<Bug> searchBugs(UUID projectId, String searchTerm, Pageable pageable) {
        return bugRepository.findByProjectIdAndSearchTerm(projectId, searchTerm, pageable);
    }

    /**
     * Get bugs by status
     */
    public List<Bug> getBugsByStatus(UUID projectId, BugStatus status) {
        return bugRepository.findByProjectIdAndStatus(projectId, status);
    }

    /**
     * Get bugs by priority
     */
    public List<Bug> getBugsByPriority(UUID projectId, BugPriority priority) {
        return bugRepository.findByProjectIdAndPriority(projectId, priority);
    }

    /**
     * Get bugs by assignee
     */
    public List<Bug> getBugsByAssignee(UUID projectId, UUID assigneeId) {
        return bugRepository.findByProjectIdAndAssigneeId(projectId, assigneeId);
    }

    /**
     * Get bugs by reporter
     */
    public List<Bug> getBugsByReporter(UUID projectId, UUID reporterId) {
        return bugRepository.findByProjectIdAndReporterId(projectId, reporterId);
    }

    /**
     * Get all bugs assigned to a specific user across all projects
     */
    public List<Bug> getBugsAssignedToUser(UUID userId) {
        return bugRepository.findByAssigneeId(userId);
    }

    // Analytics and Reporting

    /**
     * Get bug statistics for project
     */
    public BugStatistics getBugStatistics(UUID projectId) {
        long totalBugs = bugRepository.countByProjectId(projectId);
        long openBugs = bugRepository.countByProjectIdAndStatus(projectId, BugStatus.OPEN);
        long fixedBugs = bugRepository.countByProjectIdAndStatus(projectId, BugStatus.FIXED);
        long closedBugs = bugRepository.countByProjectIdAndStatus(projectId, BugStatus.CLOSED);
        long reopenedBugs = bugRepository.countByProjectIdAndStatus(projectId, BugStatus.REOPENED);

        return new BugStatistics(totalBugs, openBugs, fixedBugs, closedBugs, reopenedBugs);
    }

    /**
     * Get recent bugs
     */
    public Page<Bug> getRecentBugs(UUID projectId, Pageable pageable) {
        return bugRepository.findRecentBugs(projectId, pageable);
    }

    // Auto-assignment Logic

    /**
     * Auto-assign high priority bugs
     */
    private void autoAssignHighPriorityBug(Bug bug) {
        try {
            // Use the sophisticated assignment orchestrator for high priority bugs
            User assignedUser = assignmentOrchestrator.executeCompleteAutoAssignment(bug);
            if (assignedUser != null) {
                bug.assignTo(assignedUser);
                logger.info("High priority bug {} auto-assigned to user: {} ({} {})",
                        bug.getId(), assignedUser.getId(), assignedUser.getFirstName(), assignedUser.getLastName());
            } else {
                logger.warn("No suitable user found for high priority bug: {}", bug.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to auto-assign high priority bug: {}", bug.getId(), e);
            // No fallback assignment - sophisticated logic should handle all cases
            logger.warn("High priority bug {} could not be auto-assigned due to assignment failure", bug.getId());
        }
    }

    /**
     * Auto-assign teams to bug based on labels and tags
     * 
     * @param bug        the bug to assign teams to
     * @param assignedBy the user ID who triggered the assignment
     */
    private void autoAssignTeamsToBug(Bug bug, UUID assignedBy) {
        logger.info("Auto-assigning teams to bug: {} based on labels and tags", bug.getId());
        logger.info("Bug labels: {}",
                bug.getLabels().stream().map(BugLabel::getName).collect(Collectors.joining(", ")));
        logger.info("Bug tags: {}", bug.getTags() != null ? String.join(", ", bug.getTags()) : "none");

        // Get team assignment recommendation
        TeamAssignmentRecommendation recommendation = teamAssignmentService.getAssignmentRecommendation(bug);

        if (recommendation.hasTeams()) {
            logger.info("Auto-assigning bug {} to {} teams: {}",
                    bug.getId(), recommendation.getTeamCount(),
                    recommendation.getAssignmentSummary());

            // Extract team IDs from recommendation and save to database
            Set<UUID> teamIds = recommendation.getAssignedTeams().stream()
                    .map(team -> team.getTeamId()) // teamId is already UUID in backend
                    .collect(java.util.stream.Collectors.toSet());

            try {
                List<BugTeamAssignment> savedAssignments = teamAssignmentService.saveTeamAssignments(bug, teamIds,
                        assignedBy);
                logger.info("Successfully saved {} team assignments for bug: {}", savedAssignments.size(), bug.getId());
            } catch (Exception e) {
                logger.error("Failed to save team assignments for bug: {}", bug.getId(), e);
                // Don't fail bug creation if team assignment fails
            }

        } else {
            logger.info("No teams found for bug: {} with labels: {}",
                    bug.getId(), recommendation.getAnalyzedLabels());
            logger.info("Recommendation message: {}", recommendation.getMessage());
        }
    }

    // Validation Methods

    /**
     * Validate bug title uniqueness within project
     */
    public boolean isTitleUnique(UUID projectId, String title) {
        return !bugRepository.existsByProjectIdAndTitle(projectId, title);
    }

    /**
     * Validate bug exists in project
     */
    public boolean bugExistsInProject(UUID projectId, Long bugId) {
        return bugRepository.existsByProjectIdAndId(projectId, bugId);
    }

    // Statistics DTO
    public static class BugStatistics {
        private final long totalBugs;
        private final long openBugs;
        private final long fixedBugs;
        private final long closedBugs;
        private final long reopenedBugs;

        public BugStatistics(long totalBugs, long openBugs, long fixedBugs, long closedBugs, long reopenedBugs) {
            this.totalBugs = totalBugs;
            this.openBugs = openBugs;
            this.fixedBugs = fixedBugs;
            this.closedBugs = closedBugs;
            this.reopenedBugs = reopenedBugs;
        }

        // Getters
        public long getTotalBugs() {
            return totalBugs;
        }

        public long getOpenBugs() {
            return openBugs;
        }

        public long getFixedBugs() {
            return fixedBugs;
        }

        public long getClosedBugs() {
            return closedBugs;
        }

        public long getReopenedBugs() {
            return reopenedBugs;
        }
    }
}