package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pbm5.bugtracker.dto.*;
import com.pbm5.bugtracker.entity.*;
import com.pbm5.bugtracker.exception.*;
import com.pbm5.bugtracker.repository.ProjectRepository;
import com.pbm5.bugtracker.repository.ProjectMemberRepository;
import com.pbm5.bugtracker.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for project membership management operations.
 * 
 * Handles complete membership lifecycle including:
 * - Join request creation and approval workflow
 * - Role management with admin promotion/demotion
 * - Member removal with last admin protection
 * - Membership queries and statistics
 * - Audit trail for all membership changes
 * 
 * Key Features:
 * - Approval workflow (PENDING -> ACTIVE/REJECTED)
 * - Role-based permission enforcement
 * - Last admin protection logic
 * - Comprehensive audit logging
 * - User context integration for responses
 * 
 * Business Rules:
 * - Users can request to join any project
 * - Only admins can approve/reject requests
 * - Only admins can manage member roles
 * - Cannot remove or demote last admin
 * - Members can leave projects themselves
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectSecurityService projectSecurityService;
    private final ProjectNotificationEventListener projectNotificationEventListener;

    // ===============================
    // MEMBERSHIP LIFECYCLE MANAGEMENT
    // ===============================

    /**
     * Request to join a project.
     * Creates a pending membership that requires admin approval.
     * 
     * @param projectId the project ID to join
     * @param userId    the user ID requesting to join
     * @param request   optional join request with message
     * @return the pending membership response
     * @throws ProjectNotFoundException     if project not found
     * @throws DuplicateMembershipException if user already has membership
     * @throws UserNotFoundException        if user not found
     */
    public ProjectMemberResponse requestToJoin(UUID projectId, UUID userId, JoinProjectRequest request) {
        log.info("User {} requesting to join project {}", userId, projectId);

        // Validate project exists and is active
        Project project = projectRepository.findById(projectId)
                .filter(Project::isActive)
                .orElseThrow(() -> {
                    log.error("Project not found or inactive for join request: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for join request: {}", userId);
                    return new UserNotFoundException("User not found");
                });

        // Check if user already has membership (any status)
        if (projectMemberRepository.existsByProject_IdAndUserId(projectId, userId)) {
            ProjectMember existingMember = projectMemberRepository
                    .findByProject_IdAndUserId(projectId, userId)
                    .orElse(null);

            if (existingMember != null) {
                String status = existingMember.getStatus().name();
                log.warn("User {} already has {} membership in project {}", userId, status, projectId);
                throw new DuplicateMembershipException(projectId.toString(), userId, status);
            }
        }

        // Create pending membership
        ProjectMember membership = new ProjectMember(project, userId);
        membership.setRequestedAt(LocalDateTime.now());

        // Add optional message if provided
        if (request != null && StringUtils.hasText(request.getMessage())) {
            // Note: ProjectMember entity would need a message field for this
            log.debug("Join request includes message: {}", request.getMessage());
        }

        ProjectMember savedMembership = projectMemberRepository.save(membership);
        log.info("Join request created: user {} for project {} with status PENDING", userId, projectId);

        // 🔔 Send notifications to project admins about the join request
        try {
            notifyAdminsAboutJoinRequest(project, user);
            log.debug("Join request notifications sent to admins for user {} in project {}", userId, projectId);
        } catch (Exception e) {
            log.error("Failed to send join request notifications: {}", e.getMessage());
            // Don't fail the join request for notification errors
        }

        return buildProjectMemberResponse(savedMembership, user, null);
    }

    /**
     * Approve a join request.
     * Changes membership status from PENDING to ACTIVE.
     * 
     * @param projectId  the project ID
     * @param userId     the user ID to approve
     * @param approverId the admin user ID approving the request
     * @return the approved membership response
     * @throws ProjectNotFoundException       if project not found
     * @throws ProjectMemberNotFoundException if pending membership not found
     * @throws ProjectAccessDeniedException   if approver is not admin
     */
    public ProjectMemberResponse approveJoinRequest(UUID projectId, UUID userId, UUID approverId) {
        log.info("Admin {} approving join request for user {} in project {}", approverId, userId, projectId);

        // Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found for approval: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Check admin permissions
        if (!projectSecurityService.isUserProjectAdmin(projectId, approverId)) {
            log.warn("Unauthorized approval attempt: user {} on project {}", approverId, projectId);
            throw new ProjectAccessDeniedException(projectId.toString(), approverId, "approve membership");
        }

        // Find pending membership
        ProjectMember membership = projectMemberRepository
                .findByProject_IdAndUserIdAndStatus(projectId, userId, MemberStatus.PENDING)
                .orElseThrow(() -> {
                    log.error("Pending membership not found: user {} in project {}", userId, projectId);
                    return new ProjectMemberNotFoundException(projectId.toString(), userId, "approve request");
                });

        // Approve membership
        membership.approve(approverId);
        ProjectMember approvedMembership = projectMemberRepository.save(membership);

        log.info("Membership approved: user {} in project {} by admin {}", userId, projectId, approverId);

        // Get user details for response
        User user = userRepository.findById(userId).orElse(null);
        User approver = userRepository.findById(approverId).orElse(null);

        // 🔔 Send notification to user about approved membership
        if (user != null && project != null) {
            try {
                projectNotificationEventListener.onProjectJoinRequestApproved(project, user, approver);
                log.debug("Membership approval notification sent for user {} in project {}", userId, projectId);
            } catch (Exception e) {
                log.error("Failed to send membership approval notification: {}", e.getMessage());
                // Don't fail the approval for notification errors
            }
        }

        return buildProjectMemberResponse(approvedMembership, user, approver);
    }

    /**
     * Reject a join request.
     * Changes membership status from PENDING to REJECTED.
     * 
     * @param projectId  the project ID
     * @param userId     the user ID to reject
     * @param approverId the admin user ID rejecting the request
     * @return the rejected membership response
     * @throws ProjectNotFoundException       if project not found
     * @throws ProjectMemberNotFoundException if pending membership not found
     * @throws ProjectAccessDeniedException   if approver is not admin
     */
    public ProjectMemberResponse rejectJoinRequest(UUID projectId, UUID userId, UUID approverId) {
        log.info("Admin {} rejecting join request for user {} in project {}", approverId, userId, projectId);

        // Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found for rejection: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Check admin permissions
        if (!projectSecurityService.isUserProjectAdmin(projectId, approverId)) {
            log.warn("Unauthorized rejection attempt: user {} on project {}", approverId, projectId);
            throw new ProjectAccessDeniedException(projectId.toString(), approverId, "reject membership");
        }

        // Find pending membership
        ProjectMember membership = projectMemberRepository
                .findByProject_IdAndUserIdAndStatus(projectId, userId, MemberStatus.PENDING)
                .orElseThrow(() -> {
                    log.error("Pending membership not found: user {} in project {}", userId, projectId);
                    return new ProjectMemberNotFoundException(projectId.toString(), userId, "reject request");
                });

        // Reject membership
        membership.reject(approverId);
        ProjectMember rejectedMembership = projectMemberRepository.save(membership);

        log.info("Membership rejected: user {} in project {} by admin {}", userId, projectId, approverId);

        // Get user details for response
        User user = userRepository.findById(userId).orElse(null);
        User approver = userRepository.findById(approverId).orElse(null);

        // 🔔 Send notification to user about rejected membership
        if (user != null && project != null) {
            try {
                projectNotificationEventListener.onProjectJoinRequestRejected(project, user, approver);
                log.debug("Membership rejection notification sent for user {} in project {}", userId, projectId);
            } catch (Exception e) {
                log.error("Failed to send membership rejection notification: {}", e.getMessage());
                // Don't fail the rejection for notification errors
            }
        }

        return buildProjectMemberResponse(rejectedMembership, user, approver);
    }

    /**
     * Remove a member from a project.
     * Only admins can remove members. Cannot remove last admin.
     * 
     * @param projectId the project ID
     * @param userId    the user ID to remove
     * @param adminId   the admin user ID performing the removal
     * @throws ProjectNotFoundException       if project not found
     * @throws ProjectMemberNotFoundException if member not found
     * @throws ProjectAccessDeniedException   if user is not admin
     * @throws LastAdminException             if trying to remove last admin
     */
    public void removeMember(UUID projectId, UUID userId, UUID adminId) {
        log.info("Admin {} removing member {} from project {}", adminId, userId, projectId);

        // Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found for member removal: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Check admin permissions
        if (!projectSecurityService.isUserProjectAdmin(projectId, adminId)) {
            log.warn("Unauthorized member removal attempt: admin {} on project {}", adminId, projectId);
            throw new ProjectAccessDeniedException(projectId.toString(), adminId, "remove member");
        }

        // Find active membership
        ProjectMember membership = projectMemberRepository
                .findByProject_IdAndUserIdAndStatus(projectId, userId, MemberStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.error("Active membership not found: user {} in project {}", userId, projectId);
                    return new ProjectMemberNotFoundException(projectId.toString(), userId, "remove member");
                });

        // Check last admin protection
        if (membership.getRole() == ProjectRole.ADMIN) {
            long adminCount = projectSecurityService.getProjectAdminCount(projectId);
            if (adminCount <= 1) {
                log.warn("Attempt to remove last admin: user {} from project {}", userId, projectId);
                throw new LastAdminException(projectId.toString(), userId, "remove");
            }
        }

        // Remove membership
        projectMemberRepository.delete(membership);
        log.info("Member removed successfully: user {} from project {} by admin {}", userId, projectId, adminId);
    }

    /**
     * Leave a project.
     * Members can leave projects themselves. Cannot leave if last admin.
     * 
     * @param projectId the project ID
     * @param userId    the user ID leaving the project
     * @throws ProjectNotFoundException       if project not found
     * @throws ProjectMemberNotFoundException if user is not a member
     * @throws LastAdminException             if trying to leave as last admin
     */
    public void leaveProject(UUID projectId, UUID userId) {
        log.info("User {} leaving project {}", userId, projectId);

        // Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found for leave operation: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Find active membership
        ProjectMember membership = projectMemberRepository
                .findByProject_IdAndUserIdAndStatus(projectId, userId, MemberStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.error("Active membership not found for leave: user {} in project {}", userId, projectId);
                    return new ProjectMemberNotFoundException(projectId.toString(), userId, "leave project");
                });

        // Check last admin protection
        if (membership.getRole() == ProjectRole.ADMIN) {
            long adminCount = projectSecurityService.getProjectAdminCount(projectId);
            if (adminCount <= 1) {
                log.warn("Last admin attempting to leave project: user {} in project {}", userId, projectId);
                throw new LastAdminException(projectId.toString(), userId, "leave",
                        "Cannot leave project as the last admin. Promote another member to admin first.");
            }
        }

        // Remove membership
        projectMemberRepository.delete(membership);
        log.info("User left project successfully: user {} from project {}", userId, projectId);
    }

    // ===============================
    // ROLE MANAGEMENT
    // ===============================

    /**
     * Update a member's role in a project.
     * Only admins can change member roles.
     * 
     * @param projectId the project ID
     * @param userId    the user ID whose role to update
     * @param newRole   the new role to assign
     * @param adminId   the admin user ID performing the change
     * @return the updated membership response
     * @throws ProjectNotFoundException       if project not found
     * @throws ProjectMemberNotFoundException if member not found
     * @throws ProjectAccessDeniedException   if user is not admin
     * @throws LastAdminException             if demoting last admin
     */
    public ProjectMemberResponse updateMemberRole(UUID projectId, UUID userId, ProjectRole newRole, UUID adminId) {
        log.info("Admin {} updating role for user {} in project {} to {}", adminId, userId, projectId, newRole);

        // Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found for role update: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Check admin permissions
        if (!projectSecurityService.isUserProjectAdmin(projectId, adminId)) {
            log.warn("Unauthorized role update attempt: admin {} on project {}", adminId, projectId);
            throw new ProjectAccessDeniedException(projectId.toString(), adminId, "update member role");
        }

        // Find active membership
        ProjectMember membership = projectMemberRepository
                .findByProject_IdAndUserIdAndStatus(projectId, userId, MemberStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.error("Active membership not found for role update: user {} in project {}", userId, projectId);
                    return new ProjectMemberNotFoundException(projectId.toString(), userId, "update role");
                });

        // Check last admin protection for demotion
        if (membership.getRole() == ProjectRole.ADMIN && newRole == ProjectRole.MEMBER) {
            long adminCount = projectSecurityService.getProjectAdminCount(projectId);
            if (adminCount <= 1) {
                log.warn("Attempt to demote last admin: user {} in project {}", userId, projectId);
                throw new LastAdminException(projectId.toString(), userId, "demote");
            }
        }

        // Store old role for notification
        ProjectRole oldRole = membership.getRole();

        // Update role
        membership.updateRole(newRole);
        ProjectMember updatedMembership = projectMemberRepository.save(membership);

        log.info("Role updated successfully: user {} in project {} to {} by admin {}",
                userId, projectId, newRole, adminId);

        // Get user details for response
        User user = userRepository.findById(userId).orElse(null);
        User admin = userRepository.findById(adminId).orElse(null);

        // 🔔 Send notification about role change
        if (user != null && project != null) {
            try {
                projectNotificationEventListener.onProjectRoleChanged(project, user, oldRole, newRole, admin);
                log.debug("Role change notification triggered for user {} in project {}", userId, projectId);
            } catch (Exception e) {
                log.error("Failed to send role change notification: {}", e.getMessage());
                // Don't fail the role update for notification errors
            }
        }

        return buildProjectMemberResponse(updatedMembership, user, admin);
    }

    /**
     * Promote a member to admin role.
     * Convenience method for role updates.
     * 
     * @param projectId the project ID
     * @param userId    the user ID to promote
     * @param adminId   the admin user ID performing the promotion
     * @return the updated membership response
     */
    public ProjectMemberResponse promoteToAdmin(UUID projectId, UUID userId, UUID adminId) {
        log.info("Promoting user {} to admin in project {} by admin {}", userId, projectId, adminId);
        return updateMemberRole(projectId, userId, ProjectRole.ADMIN, adminId);
    }

    /**
     * Demote an admin to member role.
     * Convenience method for role updates with last admin protection.
     * 
     * @param projectId the project ID
     * @param userId    the user ID to demote
     * @param adminId   the admin user ID performing the demotion
     * @return the updated membership response
     */
    public ProjectMemberResponse demoteFromAdmin(UUID projectId, UUID userId, UUID adminId) {
        log.info("Demoting admin {} to member in project {} by admin {}", userId, projectId, adminId);
        return updateMemberRole(projectId, userId, ProjectRole.MEMBER, adminId);
    }

    // ===============================
    // MEMBERSHIP QUERIES
    // ===============================

    /**
     * Get project members filtered by status.
     * 
     * @param projectId the project ID
     * @param status    optional status filter (null for all statuses)
     * @param pageable  pagination parameters
     * @return page of project members
     * @throws ProjectNotFoundException if project not found
     */
    @Transactional(readOnly = true)
    public Page<ProjectMemberResponse> getProjectMembers(UUID projectId, MemberStatus status, Pageable pageable) {
        log.debug("Getting project members: project={}, status={}, page={}",
                projectId, status, pageable.getPageNumber());

        // Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found for member listing: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Get members by status
        Page<ProjectMember> membersPage;
        if (status != null) {
            membersPage = projectMemberRepository.findByProject_IdAndStatus(projectId, status, pageable);
        } else {
            // Get all members if no status filter
            List<ProjectMember> allMembers = projectMemberRepository.findByProject_Id(projectId);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allMembers.size());
            List<ProjectMember> pageMembers = allMembers.subList(start, end);
            membersPage = new PageImpl<>(pageMembers, pageable, allMembers.size());
        }

        // Convert to responses with user details
        List<ProjectMemberResponse> memberResponses = membersPage.getContent().stream()
                .map(member -> {
                    User user = userRepository.findById(member.getUserId()).orElse(null);
                    User approver = member.getApprovedBy() != null
                            ? userRepository.findById(member.getApprovedBy()).orElse(null)
                            : null;
                    return buildProjectMemberResponse(member, user, approver);
                })
                .collect(Collectors.toList());

        log.debug("Found {} project members for project {}", memberResponses.size(), projectId);
        return new PageImpl<>(memberResponses, pageable, membersPage.getTotalElements());
    }

    /**
     * Get pending join requests for a project.
     * Admin-only operation.
     * 
     * @param projectId the project ID
     * @param pageable  pagination parameters
     * @return page of pending requests
     */
    @Transactional(readOnly = true)
    public Page<ProjectMemberResponse> getPendingRequests(UUID projectId, Pageable pageable) {
        log.debug("Getting pending requests for project {}", projectId);
        return getProjectMembers(projectId, MemberStatus.PENDING, pageable);
    }

    /**
     * Get user's membership status in a project.
     * 
     * @param projectId the project ID
     * @param userId    the user ID
     * @return the membership response or null if not a member
     */
    @Transactional(readOnly = true)
    public ProjectMemberResponse getUserMembershipStatus(UUID projectId, UUID userId) {
        log.debug("Getting membership status: project={}, user={}", projectId, userId);

        ProjectMember membership = projectMemberRepository
                .findByProject_IdAndUserId(projectId, userId)
                .orElse(null);

        if (membership == null) {
            return null;
        }

        User user = userRepository.findById(userId).orElse(null);
        User approver = membership.getApprovedBy() != null
                ? userRepository.findById(membership.getApprovedBy()).orElse(null)
                : null;

        return buildProjectMemberResponse(membership, user, approver);
    }

    /**
     * Check if user is project admin.
     * 
     * @param projectId the project ID
     * @param userId    the user ID
     * @return true if user is active admin, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isUserProjectAdmin(UUID projectId, UUID userId) {
        return projectSecurityService.isUserProjectAdmin(projectId, userId);
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    /**
     * Build project member response DTO.
     * 
     * @param membership the project member entity
     * @param user       the user entity (can be null)
     * @param approver   the approver user entity (can be null)
     * @return project member response DTO
     */
    private ProjectMemberResponse buildProjectMemberResponse(ProjectMember membership, User user, User approver) {
        return new ProjectMemberResponse(
                membership.getId(),
                membership.getProjectId(),
                membership.getUserId(),
                user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown User",
                user != null ? user.getFirstName() : null,
                user != null ? user.getLastName() : null,
                user != null ? user.getEmail() : "unknown@example.com",
                membership.getRole().name(),
                membership.getStatus().name(),
                membership.getJoinedAt(),
                membership.getRequestedAt(),
                membership.getApprovedBy(),
                approver != null ? approver.getFirstName() + " " + approver.getLastName() : null,
                membership.getApprovedAt(),
                true, // canBeRemoved - will be calculated by frontend based on user permissions
                true // canChangeRole - will be calculated by frontend based on user permissions
        );
    }

    /**
     * Notify project admins about a new join request.
     * 
     * @param project the project
     * @param user    the user requesting to join
     */
    private void notifyAdminsAboutJoinRequest(Project project, User user) {
        log.info(
                "🔔 Starting admin notification for join request: projectId={}, projectName='{}', requesterId={}, requesterEmail={}",
                project.getId(), project.getName(), user.getId(), user.getEmail());

        // Debug: Check what project members exist
        List<ProjectMember> allMembers = projectMemberRepository.findByProject_Id(project.getId());
        log.info("🔍 Debug: Project {} has {} total members:", project.getId(), allMembers.size());
        for (ProjectMember member : allMembers) {
            log.info("🔍   - User {}: role={}, status={}",
                    member.getUserId(), member.getRole(), member.getStatus());
        }

        // Find project admins using ProjectMemberRepository (more reliable)
        List<ProjectMember> adminMemberships = projectMemberRepository.findByProject_IdAndRole(project.getId(),
                ProjectRole.ADMIN);
        log.info("🔔 Found {} admin memberships for project {}: {}", adminMemberships.size(), project.getId(),
                adminMemberships.stream().map(m -> m.getUserId().toString()).collect(Collectors.joining(", ")));

        if (adminMemberships.isEmpty()) {
            log.warn("⚠️ No admins found for project {} - join request notification will not be sent", project.getId());
            return;
        }

        // Get admin users and send notifications
        for (ProjectMember adminMembership : adminMemberships) {
            // Only notify ACTIVE admins
            if (adminMembership.getStatus() != MemberStatus.ACTIVE) {
                log.debug("⏭️ Skipping admin {} (status: {}) for project {}",
                        adminMembership.getUserId(), adminMembership.getStatus(), project.getId());
                continue;
            }

            User admin = userRepository.findById(adminMembership.getUserId()).orElse(null);
            if (admin == null) {
                log.warn("⚠️ Admin user {} not found for project {}", adminMembership.getUserId(), project.getId());
                continue;
            }

            try {
                log.info("🔔 Sending join request notification to admin {} ({}) for project {}",
                        admin.getEmail(), admin.getId(), project.getId());

                // Check if admin has notification preferences enabled
                log.info("🔔 Admin {} notification preferences check - will be handled by NotificationService",
                        admin.getEmail());

                log.info("🔔 About to call projectNotificationEventListener.onProjectJoinRequestCreated for admin {}",
                        admin.getEmail());
                projectNotificationEventListener.onProjectJoinRequestCreated(project, user, admin);
                log.debug("✅ Join request notification sent to admin {} for project {}", admin.getId(),
                        project.getId());
            } catch (Exception e) {
                log.error("❌ Failed to send join request notification to admin {}: {}", admin.getId(), e.getMessage());
                log.error("❌ Full error details:", e);
            }
        }
    }
}