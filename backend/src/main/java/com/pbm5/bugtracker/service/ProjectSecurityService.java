package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.entity.MemberStatus;
import com.pbm5.bugtracker.entity.ProjectRole;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.entity.Project;
import com.pbm5.bugtracker.repository.ProjectRepository;
import com.pbm5.bugtracker.repository.ProjectMemberRepository;
import com.pbm5.bugtracker.repository.UserRepository;

import java.util.UUID;

/**
 * Service class for project-related permission checks and security validations.
 * 
 * Provides centralized security service for project permissions with caching
 * optimization.
 * This service integrates with Spring Security and can be used by controllers
 * and
 * security annotations for fine-grained permission control.
 * 
 * Key Features:
 * - Permission checking for all project operations
 * - Spring Security context integration
 * - Performance optimization through caching
 * - Support for method-level security annotations
 * - Comprehensive audit logging for security events
 * 
 * Usage Examples:
 * - @PreAuthorize("@projectSecurityService.canEditProject(#projectId,
 * authentication.principal.id)")
 * - @PreAuthorize("@projectSecurityService.canManageMembers(#projectId,
 * authentication.principal.id)")
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProjectSecurityService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    // ===============================
    // CORE PERMISSION CHECKING METHODS
    // ===============================

    /**
     * Check if a user can view a specific project.
     * 
     * For projects, this is always true since all projects are public in the
     * open-source model.
     * This method exists for consistency with other security services and future
     * extensibility.
     * 
     * @param projectId the project ID to check
     * @param userId    the user ID requesting access
     * @return true (projects are always publicly viewable)
     */
    @Cacheable(value = "projectPermissions", key = "'canView:' + #projectId + ':' + #userId")
    public boolean canViewProject(UUID projectId, UUID userId) {
        log.debug("Checking view permission for project: {} and user: {}", projectId, userId);

        if (projectId == null) {
            log.warn("Project ID is null in canViewProject check");
            return false;
        }

        // Projects follow open-source model - all are publicly viewable
        // Future enhancement: Could check if project is deleted/inactive
        log.debug("Project view access granted (public model): project={}, user={}", projectId, userId);
        return true;
    }

    /**
     * Check if a user can edit a specific project.
     * Only project admins can edit project details.
     * 
     * @param projectId the project ID to check
     * @param userId    the user ID requesting access
     * @return true if user is project admin, false otherwise
     */
    @Cacheable(value = "projectPermissions", key = "'canEdit:' + #projectId + ':' + #userId")
    public boolean canEditProject(UUID projectId, UUID userId) {
        log.debug("Checking edit permission for project: {} and user: {}", projectId, userId);

        if (projectId == null || userId == null) {
            log.warn("Invalid parameters: projectId={}, userId={}", projectId, userId);
            return false;
        }

        try {
            // Check if project exists (let business logic handle ProjectNotFoundException)
            if (!projectRepository.existsById(projectId)) {
                log.debug("Project not found: {}, allowing access to proceed for proper 404 handling", projectId);
                return true; // Allow access to proceed - business logic will handle 404
            }

            // Check if user is project admin
            boolean isAdmin = isUserProjectAdmin(projectId, userId);
            log.debug("Edit permission result for user {} on project {}: {}", userId, projectId, isAdmin);
            return isAdmin;

        } catch (Exception e) {
            log.error("Error checking edit permission for project: {} and user: {}", projectId, userId, e);
            return false;
        }
    }

    /**
     * Check if a user can delete a specific project.
     * Only project admins can delete projects.
     * Additional business rules may apply (e.g., no active dependencies).
     * 
     * @param projectId the project ID to check
     * @param userId    the user ID requesting access
     * @return true if user is project admin, false otherwise
     */
    @Cacheable(value = "projectPermissions", key = "'canDelete:' + #projectId + ':' + #userId")
    public boolean canDeleteProject(UUID projectId, UUID userId) {
        log.debug("Checking delete permission for project: {} and user: {}", projectId, userId);

        if (projectId == null || userId == null) {
            log.warn("Invalid parameters: projectId={}, userId={}", projectId, userId);
            return false;
        }

        try {
            // Check if project exists (let business logic handle ProjectNotFoundException)
            if (!projectRepository.existsById(projectId)) {
                log.debug("Project not found: {}, allowing access to proceed for proper 404 handling", projectId);
                return true; // Allow access to proceed - business logic will handle 404
            }

            // Check if user is project admin
            boolean isAdmin = isUserProjectAdmin(projectId, userId);
            log.debug("Delete permission result for user {} on project {}: {}", userId, projectId, isAdmin);
            return isAdmin;

        } catch (Exception e) {
            log.error("Error checking delete permission for project: {} and user: {}", projectId, userId, e);
            return false;
        }
    }

    /**
     * Check if a user can manage members of a specific project.
     * Only project admins can manage project members (add, remove, change roles).
     * 
     * @param projectId the project ID to check
     * @param userId    the user ID requesting access
     * @return true if user is project admin, false otherwise
     */
    @Cacheable(value = "projectPermissions", key = "'canManageMembers:' + #projectId + ':' + #userId")
    public boolean canManageMembers(UUID projectId, UUID userId) {
        log.debug("Checking manage members permission for project: {} and user: {}", projectId, userId);

        if (projectId == null || userId == null) {
            log.warn("Invalid parameters: projectId={}, userId={}", projectId, userId);
            return false;
        }

        try {
            // Check if project exists (let business logic handle ProjectNotFoundException)
            if (!projectRepository.existsById(projectId)) {
                log.debug("Project not found: {}, allowing access to proceed for proper 404 handling", projectId);
                return true; // Allow access to proceed - business logic will handle 404
            }

            // Check if user is project admin
            boolean isAdmin = isUserProjectAdmin(projectId, userId);
            log.debug("Manage members permission result for user {} on project {}: {}", userId, projectId, isAdmin);
            return isAdmin;

        } catch (Exception e) {
            log.error("Error checking manage members permission for project: {} and user: {}", projectId, userId, e);
            return false;
        }
    }

    /**
     * Check if a user can join a specific project.
     * Users can join projects if they are not already members and the project
     * exists.
     * 
     * @param projectId the project ID to check
     * @param userId    the user ID requesting to join
     * @return true if user can join, false otherwise
     */
    @Cacheable(value = "projectPermissions", key = "'canJoin:' + #projectId + ':' + #userId")
    public boolean canJoinProject(UUID projectId, UUID userId) {
        log.debug("Checking join permission for project: {} and user: {}", projectId, userId);

        if (projectId == null || userId == null) {
            log.warn("Invalid parameters: projectId={}, userId={}", projectId, userId);
            return false;
        }

        try {
            // Check if project exists (let business logic handle ProjectNotFoundException)
            if (!projectRepository.existsById(projectId)) {
                log.debug("Project not found: {}, allowing access to proceed for proper 404 handling", projectId);
                return true; // Allow access to proceed - business logic will handle 404
            }

            // Check if user is already a member or has pending/rejected request
            boolean existingMembership = projectMemberRepository.existsByProject_IdAndUserId(projectId, userId);
            boolean canJoin = !existingMembership;

            log.debug("Join permission result for user {} on project {}: {} (existing membership: {})",
                    userId, projectId, canJoin, existingMembership);
            return canJoin;

        } catch (Exception e) {
            log.error("Error checking join permission for project: {} and user: {}", projectId, userId, e);
            return false;
        }
    }

    // ===============================
    // SECURITY UTILITY METHODS
    // ===============================

    /**
     * Get the user's role in a specific project.
     * 
     * @param projectId the project ID
     * @param userId    the user ID
     * @return the user's ProjectRole in the project, or null if not a member
     */
    @Cacheable(value = "projectMembership", key = "'userRole:' + #projectId + ':' + #userId")
    public ProjectRole getUserProjectRole(UUID projectId, UUID userId) {
        log.debug("Getting user role for project: {} and user: {}", projectId, userId);

        if (projectId == null || userId == null) {
            log.debug("Invalid parameters for getUserProjectRole: projectId={}, userId={}", projectId, userId);
            return null;
        }

        try {
            return projectMemberRepository
                    .findByProject_IdAndUserIdAndStatus(projectId, userId, MemberStatus.ACTIVE)
                    .map(member -> member.getRole())
                    .orElse(null);

        } catch (Exception e) {
            log.error("Error getting user role for project: {} and user: {}", projectId, userId, e);
            return null;
        }
    }

    /**
     * Generic project access validation method.
     * Used for custom permission checking scenarios.
     * 
     * @param projectId the project ID
     * @param userId    the user ID
     * @param operation the operation being attempted
     * @return true if access is granted, false otherwise
     */
    public boolean validateProjectAccess(UUID projectId, UUID userId, String operation) {
        log.debug("Validating project access: project={}, user={}, operation={}", projectId, userId, operation);

        if (projectId == null || userId == null || operation == null) {
            log.warn("Invalid parameters for validateProjectAccess");
            return false;
        }

        try {
            return switch (operation.toLowerCase()) {
                case "view" -> canViewProject(projectId, userId);
                case "edit" -> canEditProject(projectId, userId);
                case "delete" -> canDeleteProject(projectId, userId);
                case "manage_members" -> canManageMembers(projectId, userId);
                case "join" -> canJoinProject(projectId, userId);
                default -> {
                    log.warn("Unknown operation for project access validation: {}", operation);
                    yield false;
                }
            };
        } catch (Exception e) {
            log.error("Error validating project access: project={}, user={}, operation={}",
                    projectId, userId, operation, e);
            return false;
        }
    }

    /**
     * Check if a user is a member of a specific project.
     * 
     * @param projectId the project ID
     * @param userId    the user ID
     * @return true if user is an active project member, false otherwise
     */
    @Cacheable(value = "projectMembership", key = "'isMember:' + #projectId + ':' + #userId")
    public boolean isProjectMember(UUID projectId, UUID userId) {
        log.debug("Checking project membership: project={}, user={}", projectId, userId);

        if (projectId == null || userId == null) {
            log.debug("Invalid parameters for isProjectMember: projectId={}, userId={}", projectId, userId);
            return false;
        }

        try {
            boolean isMember = projectMemberRepository
                    .existsByProject_IdAndUserIdAndStatus(projectId, userId, MemberStatus.ACTIVE);
            log.debug("Membership check result: user {} is member of project {}: {}", userId, projectId, isMember);
            return isMember;

        } catch (Exception e) {
            log.error("Error checking project membership: project={}, user={}", projectId, userId, e);
            return false;
        }
    }

    /**
     * Check if a user is an admin of a specific project.
     * 
     * @param projectId the project ID
     * @param userId    the user ID
     * @return true if user is project admin, false otherwise
     */
    @Cacheable(value = "projectMembership", key = "'isAdmin:' + #projectId + ':' + #userId")
    public boolean isUserProjectAdmin(UUID projectId, UUID userId) {
        log.debug("Checking project admin status: project={}, user={}", projectId, userId);

        if (projectId == null || userId == null) {
            log.debug("Invalid parameters for isUserProjectAdmin: projectId={}, userId={}", projectId, userId);
            return false;
        }

        try {
            boolean isAdmin = projectMemberRepository
                    .existsByProject_IdAndUserIdAndStatusAndRole(
                            projectId, userId, MemberStatus.ACTIVE, ProjectRole.ADMIN);
            log.debug("Admin status check result: user {} is admin of project {}: {}", userId, projectId, isAdmin);
            return isAdmin;

        } catch (Exception e) {
            log.error("Error checking project admin status: project={}, user={}", projectId, userId, e);
            return false;
        }
    }

    /**
     * Get the count of active admins for a project.
     * Used for last admin protection logic.
     * 
     * @param projectId the project ID
     * @return count of active admins in the project
     */
    @Cacheable(value = "projectStats", key = "'adminCount:' + #projectId")
    public long getProjectAdminCount(UUID projectId) {
        log.debug("Getting admin count for project: {}", projectId);

        if (projectId == null) {
            log.debug("Invalid project ID for getProjectAdminCount: {}", projectId);
            return 0;
        }

        try {
            long adminCount = projectMemberRepository
                    .countByProject_IdAndRoleAndStatus(projectId, ProjectRole.ADMIN, MemberStatus.ACTIVE);
            log.debug("Admin count for project {}: {}", projectId, adminCount);
            return adminCount;

        } catch (Exception e) {
            log.error("Error getting admin count for project: {}", projectId, e);
            return 0;
        }
    }

    // ===============================
    // SPRING SECURITY INTEGRATION METHODS
    // ===============================

    /**
     * Check if the currently authenticated user can edit a specific project.
     * Convenience method for use in security expressions.
     * 
     * @param projectId the project ID
     * @return true if current user can edit the project, false otherwise
     */
    public boolean canEditProject(UUID projectId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            log.warn("No authenticated user found for canEditProject check");
            return false;
        }
        return canEditProject(projectId, currentUserId);
    }

    /**
     * Check if the currently authenticated user can manage members of a specific
     * project.
     * Convenience method for use in security expressions.
     * 
     * @param projectId the project ID
     * @return true if current user can manage members, false otherwise
     */
    public boolean canManageMembers(UUID projectId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            log.warn("No authenticated user found for canManageMembers check");
            return false;
        }
        return canManageMembers(projectId, currentUserId);
    }

    /**
     * Check if the currently authenticated user can delete a specific project.
     * Convenience method for use in security expressions.
     * 
     * @param projectId the project ID
     * @return true if current user can delete the project, false otherwise
     */
    public boolean canDeleteProject(UUID projectId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            log.warn("No authenticated user found for canDeleteProject check");
            return false;
        }
        return canDeleteProject(projectId, currentUserId);
    }

    /**
     * Check if a user can access a project by slug and user ID.
     * This method is designed for use in Spring Security expressions.
     * 
     * @param projectSlug the project slug
     * @param userId      the user ID (UUID string)
     * @return true if user can access the project, false otherwise
     */
    public boolean canAccessProject(String projectSlug, String userId) {
        log.debug("Checking project access: project={}, userId={}", projectSlug, userId);

        if (projectSlug == null || userId == null) {
            log.warn("Invalid parameters for canAccessProject: projectSlug={}, userId={}", projectSlug, userId);
            return false;
        }

        try {
            // Parse the user ID from string
            UUID userUuid = UUID.fromString(userId);

            // Find the user by ID
            User user = userRepository.findById(userUuid)
                    .orElse(null);

            if (user == null) {
                log.debug("User not found for ID: {}", userId);
                return false;
            }

            // Find the project by slug
            Project project = projectRepository.findByProjectSlug(projectSlug)
                    .orElse(null);

            if (project == null) {
                log.debug("Project not found for slug: {}", projectSlug);
                return false;
            }

            // For similarity configurations and basic project access, allow access if:
            // 1. User is a project member, OR
            // 2. Project is active (projects are publicly viewable in open-source model)
            boolean isMember = isProjectMember(project.getId(), userUuid);
            boolean hasAccess = isMember || project.isActive(); // Allow access to active projects

            log.debug("Project access result for user {} on project {}: {} (isMember: {}, projectActive: {})",
                    user.getEmail(), projectSlug, hasAccess, isMember, project.isActive());
            return hasAccess;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format for canAccessProject: projectSlug={}, userId={}", projectSlug, userId);
            return false;
        } catch (Exception e) {
            log.error("Error checking project access: project={}, userId={}", projectSlug, userId, e);
            return false;
        }
    }

    /**
     * Check if a user is a project admin by project slug and user ID.
     * This method is designed for use in Spring Security expressions.
     * 
     * @param projectSlug the project slug
     * @param userId      the user ID (UUID string)
     * @return true if user is project admin, false otherwise
     */
    public boolean isProjectAdmin(String projectSlug, String userId) {
        log.debug("Checking project admin status: project={}, userId={}", projectSlug, userId);

        if (projectSlug == null || userId == null) {
            log.warn("Invalid parameters for isProjectAdmin: projectSlug={}, userId={}", projectSlug, userId);
            return false;
        }

        try {
            // Parse the user ID from string
            UUID userUuid = UUID.fromString(userId);

            // Find the project by slug
            Project project = projectRepository.findByProjectSlug(projectSlug)
                    .orElse(null);

            if (project == null) {
                log.debug("Project not found for slug: {}", projectSlug);
                return false;
            }

            // Check if user is project admin
            boolean isAdmin = isUserProjectAdmin(project.getId(), userUuid);
            log.debug("Project admin check result for user {} on project {}: {}", userId, projectSlug, isAdmin);
            return isAdmin;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format for isProjectAdmin: projectSlug={}, userId={}", projectSlug, userId);
            return false;
        } catch (Exception e) {
            log.error("Error checking project admin status: project={}, userId={}", projectSlug, userId, e);
            return false;
        }
    }

    // ===============================
    // PRIVATE UTILITY METHODS
    // ===============================

    /**
     * Get the current authenticated user's ID from Spring Security context.
     * 
     * @return the current user's ID, or null if not authenticated
     */
    private UUID getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("No authentication found in security context");
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                log.debug("Current user ID extracted: {}", user.getId());
                return user.getId();
            } else {
                log.warn("Principal is not a User object: {}", principal.getClass().getName());
                return null;
            }

        } catch (Exception e) {
            log.error("Error extracting current user ID from security context", e);
            return null;
        }
    }
}