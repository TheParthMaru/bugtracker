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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for project management operations.
 * 
 * Handles complete project lifecycle including:
 * - CRUD operations with business rule enforcement
 * - Project search and listing with user context
 * - Member count aggregation and statistics
 * - Slug generation with conflict resolution
 * - Permission-based access control integration
 * 
 * Key Features:
 * - Auto-admin assignment on project creation
 * - Slug generation with conflict handling
 * - Soft delete with dependency validation
 * - User context integration for personalized responses
 * - Comprehensive audit logging for all operations
 * 
 * Business Rules:
 * - Project creator automatically becomes admin
 * - Project names must be unique across the system
 * - Slugs must be unique and URL-friendly
 * - Only admins can modify project details
 * - Soft delete preserves data integrity
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectSecurityService projectSecurityService;
    private final SlugService slugService;
    private final BugSimilarityService bugSimilarityService;

    // ===============================
    // CRUD OPERATIONS
    // ===============================

    /**
     * Create a new project with the specified details.
     * Project creator automatically becomes admin.
     * 
     * @param request       the project creation request
     * @param currentUserId the ID of the user creating the project
     * @return the created project response with user context
     * @throws ProjectNameConflictException if project name already exists
     * @throws UserNotFoundException        if current user not found
     */
    public ProjectResponse createProject(CreateProjectRequest request, UUID currentUserId) {
        log.info("Creating project with name: '{}' by user: {}", request.getName(), currentUserId);

        // Validate user exists
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.error("User not found during project creation: {}", currentUserId);
                    return new UserNotFoundException("Creator user not found");
                });

        // Check name uniqueness
        if (!projectRepository.isNameAvailable(request.getName())) {
            log.warn("Project creation failed: name '{}' already exists", request.getName());
            throw new ProjectNameConflictException(request.getName());
        }

        // Generate unique slug
        String projectSlug = generateUniqueSlug(request.getName(), request.getProjectSlug());
        log.debug("Generated slug for project '{}': {}", request.getName(), projectSlug);

        // Create project entity
        Project project = new Project();
        project.setName(request.getName().trim());
        project.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        project.setProjectSlug(projectSlug);
        project.setAdminId(currentUserId);
        project.setIsActive(true);

        // Save project
        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully: '{}' with ID: {} and projectSlug: {}",
                savedProject.getName(), savedProject.getId(), savedProject.getProjectSlug());

        // Create admin membership for creator
        ProjectMember adminMember = new ProjectMember(savedProject, currentUserId,
                ProjectRole.ADMIN, MemberStatus.ACTIVE);
        adminMember.setJoinedAt(LocalDateTime.now());
        adminMember.setRequestedAt(LocalDateTime.now());
        adminMember.setApprovedBy(currentUserId); // Self-approved as creator
        adminMember.setApprovedAt(LocalDateTime.now());

        projectMemberRepository.save(adminMember);
        log.info("Admin membership created for project creator: user {} in project {}",
                currentUserId, savedProject.getId());

        // Initialize default similarity configurations for the new project
        try {
            bugSimilarityService.initializeDefaultSimilarityConfigurations(savedProject.getId());
            log.info("Default similarity configurations initialized for new project: {}", savedProject.getId());
        } catch (Exception e) {
            log.error("Failed to initialize similarity configurations for project: {}", savedProject.getId(), e);
            // Don't fail project creation, just log the error
            // Similarity detection won't work, but project can still function
        }

        // Return project response with user context
        return buildProjectResponseWithContext(savedProject, currentUserId);
    }

    /**
     * Update project details.
     * Only project admins can update project information.
     * 
     * @param projectId     the project ID to update
     * @param request       the update request
     * @param currentUserId the ID of the current user
     * @return the updated project response
     * @throws ProjectNotFoundException     if project not found
     * @throws ProjectAccessDeniedException if user is not project admin
     * @throws ProjectNameConflictException if new name conflicts with existing
     *                                      project
     */
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, UUID currentUserId) {
        log.info("Updating project {} by user {}", projectId, currentUserId);

        // Find project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found for update: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Check admin permissions
        if (!projectSecurityService.isUserProjectAdmin(projectId, currentUserId)) {
            log.warn("Unauthorized project update attempt: user {} on project {}", currentUserId, projectId);
            throw new ProjectAccessDeniedException(projectId.toString(), currentUserId, "update project");
        }

        // Update name if provided
        if (StringUtils.hasText(request.getName())) {
            String newName = request.getName().trim();
            if (!newName.equals(project.getName())) {
                log.info("ProjectService -> updateProject -> Updating project name from '{}' to '{}'",
                        project.getName(), newName);

                // Check name uniqueness
                if (!projectRepository.isNameAvailable(newName)) {
                    log.warn("Project name conflict during update: '{}' already exists", newName);
                    throw new ProjectNameConflictException(newName);
                }

                // Update project name
                project.setName(newName);

                // Generate new project slug when name changes
                String oldSlug = project.getProjectSlug();
                String newProjectSlug = Project.generateProjectSlug(newName);

                log.info(
                        "ProjectService -> updateProject -> Generated new slug: '{}' for project name: '{}' (old slug: '{}')",
                        newProjectSlug, newName, oldSlug);

                // Check if the new slug conflicts with existing projects
                if (!newProjectSlug.equals(oldSlug) &&
                        projectRepository.isProjectSlugAvailable(newProjectSlug)) {
                    // Generate a unique slug by appending a counter
                    int counter = 1;
                    String uniqueSlug = newProjectSlug;
                    while (!projectRepository.isProjectSlugAvailable(uniqueSlug)) {
                        uniqueSlug = newProjectSlug + "-" + counter;
                        counter++;
                    }
                    newProjectSlug = uniqueSlug;
                    log.info(
                            "ProjectService -> updateProject -> Generated unique slug: '{}' to avoid conflict (original: '{}')",
                            uniqueSlug, newProjectSlug);
                }

                project.setProjectSlug(newProjectSlug);

                log.info("ProjectService -> updateProject -> Updated project slug from '{}' to '{}'",
                        oldSlug, newProjectSlug);
            }
        }

        // Update description if provided
        if (request.getDescription() != null) {
            String newDescription = StringUtils.hasText(request.getDescription()) ? request.getDescription().trim()
                    : null;
            project.setDescription(newDescription);
            log.debug("Updated project description");
        }

        // Update custom slug if provided
        if (StringUtils.hasText(request.getProjectSlug())) {
            String newProjectSlug = request.getProjectSlug().trim().toLowerCase();
            if (!newProjectSlug.equals(project.getProjectSlug())) {
                if (!projectRepository.isProjectSlugAvailable(newProjectSlug)) {
                    log.warn("Project slug conflict during update: '{}' already exists", newProjectSlug);
                    throw new ProjectNameConflictException(newProjectSlug + " (slug)");
                }
                project.setProjectSlug(newProjectSlug);
                log.debug("Updated project slug to: '{}'", newProjectSlug);
            }
        }

        // Save updated project
        Project updatedProject = projectRepository.save(project);
        log.info("Project updated successfully: {} by user {}", updatedProject.getName(), currentUserId);
        log.info("ProjectService -> updateProject -> Final project slug: '{}'", updatedProject.getProjectSlug());

        // Build response with updated context
        ProjectResponse response = buildProjectResponseWithContext(updatedProject, currentUserId);
        log.info("ProjectService -> updateProject -> Returning response with slug: '{}'", response.getProjectSlug());
        return response;
    }

    /**
     * Get project by ID with user context.
     * 
     * @param projectId     the project ID
     * @param currentUserId the ID of the current user
     * @return the project response with user context
     * @throws ProjectNotFoundException if project not found or inactive
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID projectId, UUID currentUserId) {
        log.debug("Getting project by ID: {} for user: {}", projectId, currentUserId);

        Project project = projectRepository.findById(projectId)
                .filter(Project::isActive) // Only return active projects
                .orElseThrow(() -> {
                    log.error("Active project not found: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        return buildProjectResponseWithContext(project, currentUserId);
    }

    /**
     * Get project by slug with user context.
     * 
     * @param slug          the project slug
     * @param currentUserId the ID of the current user
     * @return the project response with user context
     * @throws ProjectNotFoundException if project not found or inactive
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectBySlug(String projectSlug, UUID currentUserId) {
        log.debug("Getting project by project slug: '{}' for user: {}", projectSlug, currentUserId);

        Project project = projectRepository.findByProjectSlugAndIsActiveTrue(projectSlug)
                .orElseThrow(() -> {
                    log.error("Active project not found with project slug: {}", projectSlug);
                    return new ProjectNotFoundException(projectSlug);
                });

        return buildProjectResponseWithContext(project, currentUserId);
    }

    /**
     * Soft delete a project.
     * Only project admins can delete projects.
     * 
     * @param projectId     the project ID to delete
     * @param currentUserId the ID of the current user
     * @throws ProjectNotFoundException     if project not found
     * @throws ProjectAccessDeniedException if user is not project admin
     */
    public void deleteProject(UUID projectId, UUID currentUserId) {
        log.info("Soft deleting project {} by user {}", projectId, currentUserId);

        // Find project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found for deletion: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Check admin permissions
        if (!projectSecurityService.isUserProjectAdmin(projectId, currentUserId)) {
            log.warn("Unauthorized project deletion attempt: user {} on project {}", currentUserId, projectId);
            throw new ProjectAccessDeniedException(projectId.toString(), currentUserId, "delete project");
        }

        // Perform soft delete
        project.softDelete();
        projectRepository.save(project);

        log.info("Project soft deleted successfully: '{}' (ID: {}) by user {}",
                project.getName(), projectId, currentUserId);
    }

    /**
     * Restore a soft-deleted project.
     * Only project admins can restore projects.
     * 
     * @param projectId     the project ID to restore
     * @param currentUserId the ID of the current user
     * @return the restored project response
     * @throws ProjectNotFoundException     if project not found
     * @throws ProjectAccessDeniedException if user is not project admin
     */
    public ProjectResponse restoreProject(UUID projectId, UUID currentUserId) {
        log.info("Restoring project {} by user {}", projectId, currentUserId);

        // Find project (including inactive ones for restore)
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found for restoration: {}", projectId);
                    return new ProjectNotFoundException(projectId.toString());
                });

        // Check admin permissions (check against original admin, not current active
        // membership)
        if (!project.getAdminId().equals(currentUserId)) {
            log.warn("Unauthorized project restoration attempt: user {} on project {}", currentUserId, projectId);
            throw new ProjectAccessDeniedException(projectId.toString(), currentUserId, "restore project");
        }

        // Perform restore
        project.restore();
        projectRepository.save(project);

        log.info("Project restored successfully: '{}' (ID: {}) by user {}",
                project.getName(), projectId, currentUserId);

        return buildProjectResponseWithContext(project, currentUserId);
    }

    // ===============================
    // PROJECT LISTING & SEARCH
    // ===============================

    /**
     * Get all active projects with optional search and pagination.
     * 
     * @param pageable      pagination parameters
     * @param searchTerm    optional search term for project names
     * @param currentUserId the ID of the current user for context
     * @return page of projects with user context
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAllProjects(Pageable pageable, String searchTerm, UUID currentUserId) {

        // Set default sort if none specified
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending());
        }

        Page<Project> projectsPage;

        if (StringUtils.hasText(searchTerm)) {
            projectsPage = projectRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(
                    searchTerm.trim(), pageable);
        } else {

            // Log the total count of active projects before pagination
            long totalActiveProjects = projectRepository.countByIsActiveTrue();

            projectsPage = projectRepository.findByIsActiveTrue(pageable);

            // Verify the count matches
            if (totalActiveProjects != projectsPage.getTotalElements()) {
                log.warn("Count mismatch! Database count: {}, Page result count: {}",
                        totalActiveProjects, projectsPage.getTotalElements());
            }
        }

        // Convert to responses with user context
        List<ProjectResponse> projectResponses = projectsPage.getContent().stream()
                .map(project -> buildProjectResponseWithContext(project, currentUserId))
                .collect(Collectors.toList());

        Page<ProjectResponse> result = new PageImpl<>(projectResponses, pageable, projectsPage.getTotalElements());

        return result;
    }

    /**
     * Get projects for a specific user based on their membership status.
     * 
     * @param userId   the user ID
     * @param pageable pagination parameters
     * @return page of projects where user is a member
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getUserProjects(UUID userId, Pageable pageable) {
        log.debug("Getting projects for user {} with pagination: {}", userId, pageable.getPageNumber());

        // Get user's active memberships
        Page<ProjectMember> memberships = projectMemberRepository
                .findByUserIdAndStatus(userId, MemberStatus.ACTIVE, pageable);

        List<ProjectResponse> projectResponses = memberships.getContent().stream()
                .map(member -> member.getProject())
                .filter(Project::isActive) // Only active projects
                .map(project -> buildProjectResponseWithContext(project, userId))
                .collect(Collectors.toList());

        log.debug("Found {} projects for user {}", projectResponses.size(), userId);
        return new PageImpl<>(projectResponses, pageable, memberships.getTotalElements());
    }

    /**
     * Get projects where the specified user is an admin.
     * 
     * @param adminId  the admin user ID
     * @param pageable pagination parameters
     * @return page of projects where user is admin
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getProjectsByAdmin(UUID adminId, Pageable pageable) {

        List<Project> adminProjects = projectRepository.findByAdminIdAndIsActiveTrue(adminId);

        // Manual pagination for simplicity (can be optimized with native query if
        // needed)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), adminProjects.size());

        List<ProjectResponse> projectResponses = adminProjects.subList(start, end).stream()
                .map(project -> buildProjectResponseWithContext(project, adminId))
                .collect(Collectors.toList());

        log.debug("Found {} admin projects for user {}", adminProjects.size(), adminId);
        return new PageImpl<>(projectResponses, pageable, adminProjects.size());
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    /**
     * Generate unique slug with conflict resolution.
     * 
     * @param projectName the project name
     * @param customSlug  optional custom slug
     * @return unique slug
     */
    private String generateUniqueSlug(String projectName, String customSlug) {
        String baseSlug;

        if (StringUtils.hasText(customSlug)) {
            baseSlug = customSlug.trim().toLowerCase();
            if (!Project.isValidProjectSlugFormat(baseSlug)) {
                throw new InvalidProjectOperationException(null, "slug_validation",
                        "Custom slug format is invalid. Use lowercase alphanumeric with hyphens only.");
            }
        } else {
            baseSlug = Project.generateProjectSlug(projectName);
        }

        // Check if slug is available
        if (projectRepository.isProjectSlugAvailable(baseSlug)) {
            return baseSlug;
        }

        // If custom slug conflicts, throw exception
        if (StringUtils.hasText(customSlug)) {
            throw new ProjectNameConflictException(baseSlug + " (slug)");
        }

        // Generate unique slug with counter for auto-generated slugs
        int counter = 2;
        String uniqueSlug;
        do {
            uniqueSlug = slugService.generateUniqueSlug(baseSlug, counter);
            counter++;
        } while (!projectRepository.isProjectSlugAvailable(uniqueSlug) && counter < 100);

        if (counter >= 100) {
            throw new InvalidProjectOperationException(null, "slug_generation",
                    "Unable to generate unique slug after 100 attempts");
        }

        return uniqueSlug;
    }

    /**
     * Build project response with user context.
     * 
     * @param project       the project entity
     * @param currentUserId the current user ID
     * @return project response with user context
     */
    private ProjectResponse buildProjectResponseWithContext(Project project, UUID currentUserId) {
        if (currentUserId == null) {
            // No user context - return basic project info
            return buildProjectResponse(project, null, 0L, 0L, null, null, false);
        }

        // Get member counts
        long memberCount = projectMemberRepository.countByProject_IdAndStatus(project.getId(), MemberStatus.ACTIVE);
        long pendingRequestCount = projectMemberRepository.countByProject_IdAndStatus(
                project.getId(), MemberStatus.PENDING);

        // Get user membership context
        ProjectMember userMembership = projectMemberRepository
                .findByProject_IdAndUserId(project.getId(), currentUserId)
                .orElse(null);

        String userMembershipStatus = null;
        String userRole = null;
        boolean isUserAdmin = false;

        if (userMembership != null) {
            userMembershipStatus = userMembership.getStatus().name();
            userRole = userMembership.getRole().name();
            isUserAdmin = (userMembership.getStatus() == MemberStatus.ACTIVE &&
                    userMembership.getRole() == ProjectRole.ADMIN);
        }

        // Get admin user info
        User adminUser = userRepository.findById(project.getAdminId()).orElse(null);

        return buildProjectResponse(project, adminUser, memberCount, pendingRequestCount,
                userMembershipStatus, userRole, isUserAdmin);
    }

    /**
     * Build project response DTO.
     * 
     * @param project              the project entity
     * @param adminUser            the admin user (can be null)
     * @param memberCount          active member count
     * @param pendingRequestCount  pending request count
     * @param userMembershipStatus user's membership status
     * @param userRole             user's role in project
     * @param isUserAdmin          whether user is admin
     * @return project response DTO
     */
    private ProjectResponse buildProjectResponse(Project project, User adminUser,
            Long memberCount, Long pendingRequestCount, String userMembershipStatus,
            String userRole, Boolean isUserAdmin) {

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getProjectSlug(),
                project.getAdminId(),
                adminUser != null ? adminUser.getFirstName() : null,
                adminUser != null ? adminUser.getLastName() : null,
                memberCount,
                pendingRequestCount,
                project.getCreatedAt(),
                project.getUpdatedAt(),
                userMembershipStatus,
                userRole,
                isUserAdmin);
    }
}