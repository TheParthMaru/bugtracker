package com.pbm5.bugtracker.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.pbm5.bugtracker.dto.*;
import com.pbm5.bugtracker.entity.MemberStatus;
import com.pbm5.bugtracker.entity.ProjectRole;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.service.ProjectMemberService;
import com.pbm5.bugtracker.service.ProjectService;
import com.pbm5.bugtracker.service.UserService;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * REST controller for project membership management operations.
 * 
 * Provides complete membership API with approval workflows,
 * role management, and proper security controls.
 * 
 * Endpoints:
 * - Membership management (join, leave, remove)
 * - Admin approval workflow (approve, reject)
 * - Role management (promote, demote)
 * - Member listing and queries
 * 
 * Security:
 * - All endpoints require authentication
 * - Admin-only operations protected with permission checks
 * - Member operations validate user context
 * - Last admin protection enforced
 * 
 * HTTP Standards:
 * - RESTful URL patterns with project context
 * - Proper HTTP status codes (200, 201, 204, 400, 403, 404)
 * - Request/response validation
 * - Pagination support for member listings
 */
@RestController
@RequestMapping("/api/bugtracker/v1/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectMemberController {

        private final ProjectMemberService projectMemberService;
        private final ProjectService projectService;
        private final UserService userService;

        // ===============================
        // MEMBERSHIP MANAGEMENT
        // ===============================

        /**
         * Request to join a project.
         * 
         * POST /api/bugtracker/v1/projects/{slug}/join
         * 
         * Creates a pending membership request that requires admin approval.
         * Any authenticated user can request to join any project.
         * 
         * @param slug           the project slug
         * @param request        optional join request with message
         * @param authentication the current user authentication
         * @return pending membership response with 201 status
         */
        @PostMapping("/{projectSlug}/join")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProjectMemberResponse> joinProject(
                        @PathVariable String projectSlug,
                        @Valid @RequestBody(required = false) JoinProjectRequest request,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.info("User {} requesting to join project '{}'", currentUserId, projectSlug);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
                ProjectMemberResponse membership = projectMemberService.requestToJoin(
                                project.getId(), currentUserId, request);

                log.info("Join request created: user {} for project '{}' with status PENDING",
                                currentUserId, projectSlug);
                return ResponseEntity.status(HttpStatus.CREATED).body(membership);
        }

        /**
         * Leave a project.
         * 
         * POST /api/bugtracker/v1/projects/{slug}/leave
         * 
         * Members can leave projects themselves.
         * Cannot leave if user is the last admin.
         * 
         * @param slug           the project slug
         * @param authentication the current user authentication
         * @return 204 No Content on success
         */
        @PostMapping("/{projectSlug}/leave")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Void> leaveProject(
                        @PathVariable String projectSlug,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.info("User {} leaving project '{}'", currentUserId, projectSlug);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
                projectMemberService.leaveProject(project.getId(), currentUserId);

                log.info("User {} left project '{}' successfully", currentUserId, projectSlug);
                return ResponseEntity.noContent().build();
        }

        /**
         * Remove a member from a project.
         * 
         * DELETE /api/bugtracker/v1/projects/{slug}/members/{userId}
         * 
         * Admin-only operation. Cannot remove the last admin.
         * 
         * @param slug           the project slug
         * @param userId         the user ID to remove
         * @param authentication the current user authentication (must be admin)
         * @return 204 No Content on success
         */
        @DeleteMapping("/{projectSlug}/members/{userId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Void> removeMember(
                        @PathVariable String projectSlug,
                        @PathVariable UUID userId,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.info("Admin {} removing member {} from project '{}'", currentUserId, userId, projectSlug);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
                projectMemberService.removeMember(project.getId(), userId, currentUserId);

                log.info("Member {} removed from project '{}' by admin {}", userId, projectSlug, currentUserId);
                return ResponseEntity.noContent().build();
        }

        // ===============================
        // ADMIN APPROVAL WORKFLOW
        // ===============================

        /**
         * Get pending join requests for a project.
         * 
         * GET /api/bugtracker/v1/projects/{slug}/requests
         * 
         * Admin-only operation. Shows all pending membership requests
         * that require admin approval.
         * 
         * @param slug           the project slug
         * @param pageable       pagination parameters
         * @param authentication the current user authentication (must be admin)
         * @return page of pending membership requests
         */
        @GetMapping("/{projectSlug}/requests")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Page<ProjectMemberResponse>> getPendingRequests(
                        @PathVariable String projectSlug,
                        Pageable pageable,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.debug("Admin {} getting pending requests for project '{}'", currentUserId, projectSlug);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
                Page<ProjectMemberResponse> requests = projectMemberService.getPendingRequests(
                                project.getId(), pageable);

                log.debug("Retrieved {} pending requests for project '{}'",
                                requests.getNumberOfElements(), projectSlug);
                return ResponseEntity.ok(requests);
        }

        /**
         * Approve a join request.
         * 
         * POST /api/bugtracker/v1/projects/{slug}/members/{userId}/approve
         * 
         * Admin-only operation. Changes membership status from PENDING to ACTIVE.
         * 
         * @param slug           the project slug
         * @param userId         the user ID to approve
         * @param authentication the current user authentication (must be admin)
         * @return approved membership response
         */
        @PostMapping("/{projectSlug}/members/{userId}/approve")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProjectMemberResponse> approveJoinRequest(
                        @PathVariable String projectSlug,
                        @PathVariable UUID userId,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.info("Admin {} approving join request for user {} in project '{}'",
                                currentUserId, userId, projectSlug);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
                ProjectMemberResponse membership = projectMemberService.approveJoinRequest(
                                project.getId(), userId, currentUserId);

                log.info("Join request approved: user {} in project '{}' by admin {}",
                                userId, projectSlug, currentUserId);
                return ResponseEntity.ok(membership);
        }

        /**
         * Reject a join request.
         * 
         * POST /api/bugtracker/v1/projects/{slug}/members/{userId}/reject
         * 
         * Admin-only operation. Changes membership status from PENDING to REJECTED.
         * 
         * @param slug           the project slug
         * @param userId         the user ID to reject
         * @param authentication the current user authentication (must be admin)
         * @return rejected membership response
         */
        @PostMapping("/{projectSlug}/members/{userId}/reject")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProjectMemberResponse> rejectJoinRequest(
                        @PathVariable String projectSlug,
                        @PathVariable UUID userId,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.info("Admin {} rejecting join request for user {} in project '{}'",
                                currentUserId, userId, projectSlug);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
                ProjectMemberResponse membership = projectMemberService.rejectJoinRequest(
                                project.getId(), userId, currentUserId);

                log.info("Join request rejected: user {} in project '{}' by admin {}",
                                userId, projectSlug, currentUserId);
                return ResponseEntity.ok(membership);
        }

        // ===============================
        // ROLE MANAGEMENT
        // ===============================

        /**
         * Update a member's role.
         * 
         * PUT /api/bugtracker/v1/projects/{slug}/members/{userId}/role
         * 
         * Admin-only operation. Cannot demote the last admin.
         * 
         * @param slug           the project slug
         * @param userId         the user ID whose role to update
         * @param request        the role update request
         * @param authentication the current user authentication (must be admin)
         * @return updated membership response
         */
        @PutMapping("/{projectSlug}/members/{userId}/role")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProjectMemberResponse> updateMemberRole(
                        @PathVariable String projectSlug,
                        @PathVariable UUID userId,
                        @Valid @RequestBody UpdateMemberRoleRequest request,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.info("Admin {} updating role for user {} in project '{}' to {}",
                                currentUserId, userId, projectSlug, request.getRole());

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);

                // Convert string role to enum
                ProjectRole newRole = ProjectRole.valueOf(request.getRole().toUpperCase());
                ProjectMemberResponse membership = projectMemberService.updateMemberRole(
                                project.getId(), userId, newRole, currentUserId);

                log.info("Role updated: user {} in project '{}' to {} by admin {}",
                                userId, projectSlug, newRole, currentUserId);
                return ResponseEntity.ok(membership);
        }

        /**
         * Promote a member to admin.
         * 
         * POST /api/bugtracker/v1/projects/{slug}/members/{userId}/promote
         * 
         * Convenience endpoint for promoting members to admin role.
         * Admin-only operation.
         * 
         * @param slug           the project slug
         * @param userId         the user ID to promote
         * @param authentication the current user authentication (must be admin)
         * @return updated membership response
         */
        @PostMapping("/{projectSlug}/members/{userId}/promote")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProjectMemberResponse> promoteMember(
                        @PathVariable String projectSlug,
                        @PathVariable UUID userId,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.info("Admin {} promoting user {} to admin in project '{}'",
                                currentUserId, userId, projectSlug);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
                ProjectMemberResponse membership = projectMemberService.promoteToAdmin(
                                project.getId(), userId, currentUserId);

                log.info("Member promoted: user {} to admin in project '{}' by admin {}",
                                userId, projectSlug, currentUserId);
                return ResponseEntity.ok(membership);
        }

        /**
         * Demote an admin to member.
         * 
         * POST /api/bugtracker/v1/projects/{slug}/members/{userId}/demote
         * 
         * Convenience endpoint for demoting admins to member role.
         * Admin-only operation. Cannot demote the last admin.
         * 
         * @param slug           the project slug
         * @param userId         the user ID to demote
         * @param authentication the current user authentication (must be admin)
         * @return updated membership response
         */
        @PostMapping("/{projectSlug}/members/{userId}/demote")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProjectMemberResponse> demoteMember(
                        @PathVariable String projectSlug,
                        @PathVariable UUID userId,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.info("Admin {} demoting admin {} to member in project '{}'",
                                currentUserId, userId, projectSlug);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
                ProjectMemberResponse membership = projectMemberService.demoteFromAdmin(
                                project.getId(), userId, currentUserId);

                log.info("Admin demoted: user {} to member in project '{}' by admin {}",
                                userId, projectSlug, currentUserId);
                return ResponseEntity.ok(membership);
        }

        // ===============================
        // MEMBER QUERIES
        // ===============================

        /**
         * Get project members.
         * 
         * GET /api/bugtracker/v1/projects/{slug}/members?status=ACTIVE&page=0&size=20
         * 
         * Member-only access. Shows project members filtered by status.
         * 
         * @param slug           the project slug
         * @param status         optional status filter (ACTIVE, PENDING, REJECTED)
         * @param pageable       pagination parameters
         * @param authentication the current user authentication (must be member)
         * @return page of project members
         */
        @GetMapping("/{projectSlug}/members")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Page<ProjectMemberResponse>> getProjectMembers(
                        @PathVariable String projectSlug,
                        @RequestParam(required = false) String status,
                        Pageable pageable,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.debug("Getting members for project '{}' with status filter: {}", projectSlug, status);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);

                // Convert string status to enum if provided
                MemberStatus memberStatus = null;
                if (status != null && !status.trim().isEmpty()) {
                        try {
                                memberStatus = MemberStatus.valueOf(status.toUpperCase());
                        } catch (IllegalArgumentException e) {
                                log.warn("Invalid status filter provided: {}", status);
                                // Continue with null status (show all members)
                        }
                }

                Page<ProjectMemberResponse> members = projectMemberService.getProjectMembers(
                                project.getId(), memberStatus, pageable);

                log.debug("Retrieved {} members for project '{}'", members.getNumberOfElements(), projectSlug);
                return ResponseEntity.ok(members);
        }

        /**
         * Get current user's membership status in a project.
         * 
         * GET /api/bugtracker/v1/projects/{slug}/membership
         * 
         * Shows current user's membership details in the specified project.
         * 
         * @param slug           the project slug
         * @param authentication the current user authentication
         * @return membership response or 404 if not a member
         */
        @GetMapping("/{projectSlug}/membership")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProjectMemberResponse> getUserMembershipStatus(
                        @PathVariable String projectSlug,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.debug("Getting membership status for user {} in project '{}'", currentUserId, projectSlug);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);
                ProjectMemberResponse membership = projectMemberService.getUserMembershipStatus(
                                project.getId(), currentUserId);

                if (membership == null) {
                        log.debug("User {} is not a member of project '{}'", currentUserId, projectSlug);
                        return ResponseEntity.notFound().build();
                }

                log.debug("Found membership: user {} is {} {} in project '{}'",
                                currentUserId, membership.getStatus(), membership.getRole(), projectSlug);
                return ResponseEntity.ok(membership);
        }

        /**
         * Search project members with pagination and filtering.
         * 
         * GET /api/bugtracker/v1/projects/{slug}/members/search
         * 
         * Search for users who are members of the specified project.
         * Only returns users who are active project members.
         * 
         * @param slug           the project slug
         * @param search         search term for user name or email
         * @param pageable       pagination parameters
         * @param authentication the current user authentication
         * @return page of project members matching search criteria
         */
        @GetMapping("/{projectSlug}/members/search")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<UserSearchResponse> searchProjectMembers(
                        @PathVariable String projectSlug,
                        @RequestParam(required = false) String search,
                        Pageable pageable,
                        Authentication authentication) {

                UUID currentUserId = getCurrentUserId(authentication);
                log.debug("Searching project members in '{}' with term: '{}'", projectSlug, search);

                // Get project ID from slug
                ProjectResponse project = projectService.getProjectBySlug(projectSlug, currentUserId);

                // Search for project members
                UserSearchResponse response = userService.searchProjectMembers(
                                project.getId(), search, pageable);

                log.debug("Found {} project members matching search term '{}'",
                                response.getTotalElements(), search);
                return ResponseEntity.ok(response);
        }

        // ===============================
        // PRIVATE UTILITY METHODS
        // ===============================

        /**
         * Extract current user ID from authentication context.
         * 
         * @param authentication the authentication object
         * @return current user's ID
         * @throws RuntimeException if user cannot be extracted
         */
        private UUID getCurrentUserId(Authentication authentication) {
                if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                        log.error("Invalid authentication context - cannot extract user ID");
                        throw new RuntimeException("Authentication required");
                }

                User user = (User) authentication.getPrincipal();
                return user.getId();
        }

        /**
         * DTO for role update requests.
         * Simple wrapper for role changes.
         */
        public static class UpdateMemberRoleRequest {
                private String role;

                public String getRole() {
                        return role;
                }

                public void setRole(String role) {
                        this.role = role;
                }
        }
}