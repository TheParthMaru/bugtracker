package com.pbm5.bugtracker.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.dto.*;
import com.pbm5.bugtracker.entity.Project;
import com.pbm5.bugtracker.entity.Team;
import com.pbm5.bugtracker.entity.TeamMember;
import com.pbm5.bugtracker.entity.TeamRole;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.entity.MemberStatus;
import com.pbm5.bugtracker.exception.*;
import com.pbm5.bugtracker.repository.ProjectRepository;
import com.pbm5.bugtracker.repository.TeamMemberRepository;
import com.pbm5.bugtracker.repository.TeamRepository;
import com.pbm5.bugtracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class for team management operations.
 * Handles team CRUD operations, membership management, and permission checking.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

        private static final Logger logger = LoggerFactory.getLogger(TeamService.class);

        private final TeamRepository teamRepository;
        private final TeamMemberRepository teamMemberRepository;
        private final UserRepository userRepository;
        private final ProjectRepository projectRepository;
        private final SlugService slugService;
        private final TeamSecurityService teamSecurityService;
        private final TeamNotificationEventListener teamNotificationEventListener;

        // ===== TEAM CRUD OPERATIONS =====

        /**
         * Create a new team within a project with the specified details.
         * Only project admins can create teams.
         * Team creator automatically becomes team admin.
         * 
         * @param projectSlug   the project slug
         * @param request       the team creation request
         * @param currentUserId the ID of the user creating the team
         * @return the created team response
         * @throws ProjectNotFoundException     if project not found
         * @throws ProjectAccessDeniedException if user is not project admin
         * @throws TeamNameConflictException    if team name already exists in project
         */
        public TeamResponse createTeamInProject(String projectSlug, CreateTeamRequest request, UUID currentUserId) {
                logger.info("Creating team '{}' in project '{}' by user: {}", request.getName(), projectSlug,
                                currentUserId);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlug(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Check if user is project admin
                if (!teamSecurityService.isProjectAdmin(project.getId(), currentUserId)) {
                        logger.warn("Team creation failed: user {} is not admin of project {}", currentUserId,
                                        projectSlug);
                        throw new ProjectAccessDeniedException(projectSlug, currentUserId, "create team",
                                        "Only project admins can create teams");
                }

                // Check if team name already exists in this project
                if (teamRepository.existsByProjectIdAndNameIgnoreCase(project.getId(), request.getName())) {
                        logger.warn("Team creation failed: name '{}' already exists in project '{}'", request.getName(),
                                        projectSlug);
                        throw new TeamNameConflictException("Team name already exists in this project");
                }

                // Generate project-scoped slug
                String baseSlug = slugService.generateSlug(request.getName());
                String projectScopedSlug = slugService.generateProjectTeamSlug(project.getProjectSlug(), baseSlug);

                // Verify user exists
                User creator = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Create team
                Team team = Team.builder()
                                .name(request.getName())
                                .description(request.getDescription())
                                .teamSlug(projectScopedSlug)
                                .projectId(project.getId())
                                .createdBy(currentUserId)
                                .build();

                Team savedTeam = teamRepository.save(team);

                // Add creator as team admin
                TeamMember adminMember = TeamMember.builder()
                                .teamId(savedTeam.getId())
                                .userId(currentUserId)
                                .role(TeamRole.ADMIN)
                                .addedBy(currentUserId)
                                .build();

                teamMemberRepository.save(adminMember);

                logger.info("Team created successfully: {} with slug: {} in project: {}",
                                savedTeam.getName(), savedTeam.getTeamSlug(), projectSlug);

                return mapToTeamResponse(savedTeam, project, creator, 1, TeamRole.ADMIN, true);
        }

        // ===== PROJECT-TEAMS INTEGRATION METHODS =====

        /**
         * Get teams in a project with optional search and pagination.
         * 
         * @param projectSlug   the project slug
         * @param search        optional search term
         * @param pageable      pagination parameters
         * @param currentUserId the current user ID
         * @return page of teams in the project
         */
        @Transactional(readOnly = true)
        public Page<TeamResponse> getTeamsInProject(String projectSlug, String search, Pageable pageable,
                        UUID currentUserId) {
                logger.info("Fetching teams in project '{}' with search: '{}'", projectSlug, search);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlug(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Check if user is project member
                if (!teamSecurityService.isProjectMember(project.getId(), currentUserId)) {
                        logger.warn("Access denied: user {} is not member of project {}", currentUserId, projectSlug);
                        throw new ProjectAccessDeniedException(projectSlug, currentUserId, "view teams",
                                        "Only project members can view teams");
                }

                Page<Team> teams;
                if (search != null && !search.trim().isEmpty()) {
                        teams = teamRepository.findByProjectIdAndSearchTerm(project.getId(), search.trim(), pageable);
                } else {
                        teams = teamRepository.findByProjectId(project.getId(), pageable);
                }

                return teams.map(team -> {
                        User creator = userRepository.findById(team.getCreatedBy()).orElse(null);
                        TeamRole userRole = getUserTeamRole(team.getId(), currentUserId);
                        boolean canManage = isUserTeamAdmin(team.getId(), currentUserId)
                                        || teamSecurityService.isProjectAdmin(project.getId(), currentUserId);
                        int memberCount = (int) teamMemberRepository.countByTeamId(team.getId());

                        return mapToTeamResponse(team, project, creator, memberCount, userRole, canManage);
                });
        }

        /**
         * Get a specific team in a project by slug.
         * 
         * @param projectSlug   the project slug
         * @param teamSlug      the team slug
         * @param currentUserId the current user ID
         * @return the team details
         */
        @Transactional(readOnly = true)
        public TeamResponse getTeamInProject(String projectSlug, String teamSlug, UUID currentUserId) {
                logger.info("Fetching team '{}' in project '{}'", teamSlug, projectSlug);
                logger.debug("TeamService -> getTeamInProject -> Parameters: projectSlug='{}', teamSlug='{}'",
                                projectSlug,
                                teamSlug);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlug(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Check if user is project member
                if (!teamSecurityService.isProjectMember(project.getId(), currentUserId)) {
                        logger.warn("Access denied: user {} is not member of project {}", currentUserId, projectSlug);
                        throw new ProjectAccessDeniedException(projectSlug, currentUserId, "view team",
                                        "Only project members can view teams");
                }

                // Find team in project
                Team team = teamRepository.findByProjectIdAndTeamSlug(project.getId(), teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found in project"));

                User creator = userRepository.findById(team.getCreatedBy()).orElse(null);
                TeamRole userRole = getUserTeamRole(team.getId(), currentUserId);
                boolean canManage = isUserTeamAdmin(team.getId(), currentUserId)
                                || teamSecurityService.isProjectAdmin(project.getId(), currentUserId);
                int memberCount = (int) teamMemberRepository.countByTeamId(team.getId());

                return mapToTeamResponse(team, project, creator, memberCount, userRole, canManage);
        }

        /**
         * Update team information in a project.
         * 
         * @param projectSlug   the project slug
         * @param teamSlug      the team slug
         * @param request       the update request
         * @param currentUserId the current user ID
         * @return the updated team
         */
        public TeamResponse updateTeamInProject(String projectSlug, String teamSlug, UpdateTeamRequest request,
                        UUID currentUserId) {
                logger.info("Updating team '{}' in project '{}'", teamSlug, projectSlug);
                logger.debug("TeamService -> updateTeamInProject -> Parameters: projectSlug='{}', teamSlug='{}', request={}",
                                projectSlug, teamSlug, request);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlug(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Find team in project
                Team team = teamRepository.findByProjectIdAndTeamSlug(project.getId(), teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found in project"));

                // Check if user can manage team
                if (!isUserTeamAdmin(team.getId(), currentUserId)
                                && !teamSecurityService.isProjectAdmin(project.getId(), currentUserId)) {
                        logger.warn("Access denied: user {} cannot update team {}", currentUserId, teamSlug);
                        throw new TeamAccessDeniedException("Only team admins or project admins can update teams");
                }

                // Update team fields
                if (request.getName() != null && !request.getName().trim().isEmpty()) {
                        // Check if new name conflicts with existing team in project
                        if (!request.getName().equalsIgnoreCase(team.getName()) &&
                                        teamRepository.existsByProjectIdAndNameIgnoreCase(project.getId(),
                                                        request.getName())) {
                                throw new TeamNameConflictException("Team name already exists in this project");
                        }

                        // Update team name
                        team.setName(request.getName().trim());

                        // Generate new team slug when name changes
                        String newTeamSlug = slugService.generateTeamSlug(project.getProjectSlug(),
                                        request.getName().trim());

                        // Check if the new slug conflicts with existing teams in the project
                        if (!newTeamSlug.equals(team.getTeamSlug()) &&
                                        teamRepository.existsByProjectIdAndTeamSlug(project.getId(), newTeamSlug)) {
                                // Generate a unique slug by appending a counter
                                int counter = 1;
                                String uniqueSlug = newTeamSlug;
                                while (teamRepository.existsByProjectIdAndTeamSlug(project.getId(), uniqueSlug)) {
                                        uniqueSlug = slugService.generateUniqueSlug(newTeamSlug, counter);
                                        counter++;
                                }
                                newTeamSlug = uniqueSlug;
                                logger.debug("TeamService -> updateTeamInProject -> Generated unique slug: '{}' to avoid conflict",
                                                uniqueSlug);
                        }

                        team.setTeamSlug(newTeamSlug);

                        logger.debug("TeamService -> updateTeamInProject -> Generated new slug: '{}' for team name: '{}'",
                                        newTeamSlug, request.getName().trim());
                }

                if (request.getDescription() != null) {
                        team.setDescription(request.getDescription().trim());
                }

                // Log what fields are being updated
                if (request.getName() != null && !request.getName().trim().isEmpty()) {
                        logger.debug("TeamService -> updateTeamInProject -> Updating team name from '{}' to '{}'",
                                        team.getName(), request.getName().trim());
                }
                if (request.getDescription() != null) {
                        logger.debug("TeamService -> updateTeamInProject -> Updating team description");
                }

                Team updatedTeam = teamRepository.save(team);
                User creator = userRepository.findById(updatedTeam.getCreatedBy()).orElse(null);
                TeamRole userRole = getUserTeamRole(updatedTeam.getId(), currentUserId);
                boolean canManage = isUserTeamAdmin(updatedTeam.getId(), currentUserId)
                                || teamSecurityService.isProjectAdmin(project.getId(), currentUserId);
                int memberCount = (int) teamMemberRepository.countByTeamId(updatedTeam.getId());

                logger.info("Team '{}' updated successfully in project '{}'", updatedTeam.getName(), projectSlug);
                return mapToTeamResponse(updatedTeam, project, creator, memberCount, userRole, canManage);
        }

        /**
         * Delete a team from a project.
         * 
         * @param projectSlug   the project slug
         * @param teamSlug      the team slug
         * @param currentUserId the current user ID
         * @return the team name that was deleted
         */
        public String deleteTeamFromProject(String projectSlug, String teamSlug, UUID currentUserId) {
                logger.info("Deleting team '{}' from project '{}'", teamSlug, projectSlug);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlug(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Find team in project
                Team team = teamRepository.findByProjectIdAndTeamSlug(project.getId(), teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found in project"));

                // Check if user can delete team
                if (!isUserTeamAdmin(team.getId(), currentUserId)
                                && !teamSecurityService.isProjectAdmin(project.getId(), currentUserId)) {
                        logger.warn("Access denied: user {} cannot delete team {}", currentUserId, teamSlug);
                        throw new TeamAccessDeniedException("Only team admins or project admins can delete teams");
                }

                String teamName = team.getName();

                // Delete team members first
                teamMemberRepository.deleteByTeamId(team.getId());

                // Delete team
                teamRepository.delete(team);

                logger.info("Team '{}' deleted successfully from project '{}'", teamName, projectSlug);
                return teamName;
        }

        /**
         * Get team members in a project context.
         * 
         * @param projectSlug   the project slug
         * @param teamSlug      the team slug
         * @param pageable      pagination parameters
         * @param currentUserId the current user ID
         * @return page of team members
         */
        @Transactional(readOnly = true)
        public Page<TeamMemberResponse> getTeamMembersInProject(String projectSlug, String teamSlug, Pageable pageable,
                        UUID currentUserId) {
                logger.info("Fetching members of team '{}' in project '{}'", teamSlug, projectSlug);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlug(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Check if user is project member
                if (!teamSecurityService.isProjectMember(project.getId(), currentUserId)) {
                        logger.warn("Access denied: user {} is not member of project {}", currentUserId, projectSlug);
                        throw new ProjectAccessDeniedException(projectSlug, currentUserId, "view team members",
                                        "Only project members can view team members");
                }

                // Find team in project
                Team team = teamRepository.findByProjectIdAndTeamSlug(project.getId(), teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found in project"));

                Page<TeamMember> members = teamMemberRepository.findByTeamIdOrderByRoleAsc(team.getId(), pageable);

                return members.map(member -> {
                        User user = userRepository.findById(member.getUserId()).orElse(null);
                        User addedByUser = userRepository.findById(member.getAddedBy()).orElse(null);
                        return mapToTeamMemberResponse(member, user, addedByUser);
                });
        }

        /**
         * Add a member to a team in a project context.
         * 
         * @param projectSlug   the project slug
         * @param teamSlug      the team slug
         * @param request       the add member request
         * @param currentUserId the current user ID
         * @return the added member details
         */
        public TeamMemberResponse addMemberToTeamInProject(String projectSlug, String teamSlug,
                        AddMemberRequest request,
                        UUID currentUserId) {
                logger.info("Adding member to team '{}' in project '{}'", teamSlug, projectSlug);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlugAndIsActiveTrue(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Find team in project
                Team team = teamRepository.findByProjectIdAndTeamSlug(project.getId(), teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found in project"));

                // Check if user can manage team
                if (!isUserTeamAdmin(team.getId(), currentUserId)
                                && !teamSecurityService.isProjectAdmin(project.getId(), currentUserId)) {
                        logger.warn("Access denied: user {} cannot add members to team {}", currentUserId, teamSlug);
                        throw new TeamAccessDeniedException("Only team admins or project admins can add members");
                }

                // Verify user exists and is project member
                User user = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (!teamSecurityService.isProjectMember(project.getId(), request.getUserId())) {
                        logger.warn("Cannot add user {} to team: not a project member", request.getUserId());
                        throw new RuntimeException(
                                        "User must be a project member to join teams. Only project members can be added to teams within that project.");
                }

                // Check if user is already a team member
                if (isUserTeamMember(team.getId(), request.getUserId())) {
                        logger.warn("User {} is already a member of team {}", request.getUserId(), teamSlug);
                        throw new RuntimeException(
                                        "User is already a member of this team. Cannot add the same user twice.");
                }

                // Add member to team
                TeamMember member = TeamMember.builder()
                                .teamId(team.getId())
                                .userId(request.getUserId())
                                .role(request.getRole() != null ? request.getRole() : TeamRole.MEMBER)
                                .addedBy(currentUserId)
                                .build();

                TeamMember savedMember = teamMemberRepository.save(member);

                User addedByUser = userRepository.findById(currentUserId).orElse(null);
                logger.info("Member '{}' added successfully to team '{}'",
                                user.getFirstName() + " " + user.getLastName(),
                                teamSlug);

                // 🔔 Send notification about team member joined
                if (user != null && team != null) {
                        try {
                                teamNotificationEventListener.onTeamMemberJoined(team, user, addedByUser,
                                                request.getRole() != null ? request.getRole() : TeamRole.MEMBER);
                                logger.debug("Team member joined notification triggered for user {} in team {}",
                                                request.getUserId(), team.getId());
                        } catch (Exception e) {
                                logger.error("Failed to send team member joined notification: {}", e.getMessage());
                                // Don't fail the member addition for notification errors
                        }
                }

                return mapToTeamMemberResponse(savedMember, user, addedByUser);
        }

        /**
         * Update member role in a team within project context.
         * 
         * @param projectSlug   the project slug
         * @param teamSlug      the team slug
         * @param userId        the user ID to update
         * @param request       the role update request
         * @param currentUserId the current user ID
         * @return the updated member details
         */
        public TeamMemberResponse updateMemberRoleInProject(String projectSlug, String teamSlug, UUID userId,
                        UpdateMemberRoleRequest request, UUID currentUserId) {
                logger.info("Updating role for user {} in team '{}' in project '{}'", userId, teamSlug, projectSlug);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlugAndIsActiveTrue(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Find team in project
                Team team = teamRepository.findByProjectIdAndTeamSlug(project.getId(), teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found in project"));

                // Check if user can manage team
                if (!isUserTeamAdmin(team.getId(), currentUserId)
                                && !teamSecurityService.isProjectAdmin(project.getId(), currentUserId)) {
                        logger.warn("Access denied: user {} cannot update member roles in team {}", currentUserId,
                                        teamSlug);
                        throw new TeamAccessDeniedException(
                                        "Only team admins or project admins can update member roles");
                }

                // Find team member
                TeamMember member = teamMemberRepository.findByTeamIdAndUserId(team.getId(), userId)
                                .orElseThrow(() -> new TeamMemberNotFoundException("Team member not found"));

                // Store old role for notification
                TeamRole oldRole = member.getRole();

                // Update role
                member.setRole(request.getRole());
                TeamMember updatedMember = teamMemberRepository.save(member);

                User user = userRepository.findById(userId).orElse(null);
                User updatedByUser = userRepository.findById(currentUserId).orElse(null);

                logger.info("Role updated successfully for user {} to {} in team '{}'", userId, request.getRole(),
                                teamSlug);

                // 🔔 Send notification about team role change
                if (user != null && team != null && !oldRole.equals(request.getRole())) {
                        try {
                                teamNotificationEventListener.onTeamRoleChanged(team, user, oldRole, request.getRole(),
                                                updatedByUser);
                                logger.debug("Team role change notification triggered for user {} in team {}", userId,
                                                team.getId());
                        } catch (Exception e) {
                                logger.error("Failed to send team role change notification: {}", e.getMessage());
                                // Don't fail the role update for notification errors
                        }
                }

                return mapToTeamMemberResponse(updatedMember, user, updatedByUser);
        }

        /**
         * Remove a member from a team in project context.
         * 
         * @param projectSlug   the project slug
         * @param teamSlug      the team slug
         * @param userId        the user ID to remove
         * @param currentUserId the current user ID
         * @return the removed member's name
         */
        public String removeMemberFromProjectTeam(String projectSlug, String teamSlug, UUID userId,
                        UUID currentUserId) {
                logger.info("Removing user {} from team '{}' in project '{}'", userId, teamSlug, projectSlug);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlugAndIsActiveTrue(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Find team in project
                Team team = teamRepository.findByProjectIdAndTeamSlug(project.getId(), teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found in project"));

                // Check if user can manage team
                if (!isUserTeamAdmin(team.getId(), currentUserId)
                                && !teamSecurityService.isProjectAdmin(project.getId(), currentUserId)) {
                        logger.warn("Access denied: user {} cannot remove members from team {}", currentUserId,
                                        teamSlug);
                        throw new TeamAccessDeniedException("Only team admins or project admins can remove members");
                }

                // Find team member
                TeamMember member = teamMemberRepository.findByTeamIdAndUserId(team.getId(), userId)
                                .orElseThrow(() -> new TeamMemberNotFoundException("Team member not found"));

                // Get user name before deletion
                User user = userRepository.findById(userId).orElse(null);
                String memberName = user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown User";

                // Remove member
                teamMemberRepository.delete(member);

                logger.info("Member '{}' removed successfully from team '{}'", memberName, teamSlug);
                return memberName;
        }

        /**
         * Join a team in project context.
         * 
         * @param projectSlug   the project slug
         * @param teamSlug      the team slug
         * @param currentUserId the current user ID
         * @return the team name
         */
        public String joinProjectTeam(String projectSlug, String teamSlug, UUID currentUserId) {
                logger.info("User {} joining team '{}' in project '{}'", currentUserId, teamSlug, projectSlug);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlug(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Find team in project
                Team team = teamRepository.findByProjectIdAndTeamSlug(project.getId(), teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found in project"));

                // Check if user is already a member
                if (isUserTeamMember(team.getId(), currentUserId)) {
                        logger.warn("User {} is already a member of team {}", currentUserId, teamSlug);
                        throw new RuntimeException("You are already a member of this team");
                }

                // Check if user can join the team (must be a project member)
                if (!teamSecurityService.isProjectMember(project.getId(), currentUserId)) {
                        logger.warn("Access denied: user {} cannot join team {} (not a project member)",
                                        currentUserId, teamSlug);
                        throw new RuntimeException("Only project members can join teams");
                }

                // Create membership with MEMBER role
                TeamMember member = TeamMember.builder()
                                .teamId(team.getId())
                                .userId(currentUserId)
                                .role(TeamRole.MEMBER)
                                .addedBy(currentUserId) // User adds themselves
                                .build();

                teamMemberRepository.save(member);

                logger.info("User {} successfully joined team '{}'", currentUserId, team.getName());
                return team.getName();
        }

        /**
         * Leave a team in project context.
         * 
         * @param projectSlug   the project slug
         * @param teamSlug      the team slug
         * @param currentUserId the current user ID
         * @return the team name
         */
        public String leaveProjectTeam(String projectSlug, String teamSlug, UUID currentUserId) {
                logger.info("User {} leaving team '{}' in project '{}'", currentUserId, teamSlug, projectSlug);

                // Find project and verify access
                Project project = projectRepository.findByProjectSlug(projectSlug)
                                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

                // Find team in project
                Team team = teamRepository.findByProjectIdAndTeamSlug(project.getId(), teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found in project"));

                // Check if user is team member
                if (!isUserTeamMember(team.getId(), currentUserId)) {
                        logger.warn("User {} is not a member of team {}", currentUserId, teamSlug);
                        throw new RuntimeException("You are not a member of this team");
                }

                // Check if user is the last admin
                if (isUserTeamAdmin(team.getId(), currentUserId)
                                && teamMemberRepository.countByTeamIdAndRole(team.getId(), TeamRole.ADMIN) <= 1) {
                        logger.warn("Cannot leave team: user {} is the last admin", currentUserId);
                        throw new RuntimeException(
                                        "Cannot leave team: you are the last admin. Please promote another member or delete the team.");
                }

                // Find and remove team member
                TeamMember member = teamMemberRepository.findByTeamIdAndUserId(team.getId(), currentUserId)
                                .orElseThrow(() -> new TeamMemberNotFoundException("Team member not found"));

                teamMemberRepository.delete(member);

                logger.info("User {} successfully left team '{}'", currentUserId, team.getName());
                return team.getName();
        }

        /**
         * Get teams with optional search and filtering.
         * 
         * @param pageable      pagination parameters
         * @param search        search term (optional)
         * @param currentUserId the ID of the current user
         * @return page of teams matching the criteria
         */
        @Transactional(readOnly = true)
        public Page<TeamResponse> getTeams(Pageable pageable, String search, UUID currentUserId) {
                logger.debug("Getting teams with search: {}, user: {}", search, currentUserId);

                Page<Team> teams;

                if (search != null && !search.trim().isEmpty()) {
                        teams = teamRepository.findByUserAccessibleProjectsAndSearchTerm(currentUserId, search.trim(),
                                        MemberStatus.ACTIVE, pageable);
                } else {
                        teams = teamRepository.findByUserAccessibleProjects(currentUserId, MemberStatus.ACTIVE,
                                        pageable);
                }

                List<TeamResponse> teamResponses = teams.getContent().stream()
                                .map(team -> {
                                        User creator = userRepository.findById(team.getCreatedBy()).orElse(null);
                                        Project project = projectRepository.findById(team.getProjectId()).orElse(null);
                                        int memberCount = (int) teamMemberRepository.countByTeamId(team.getId());
                                        TeamRole userRole = getUserTeamRole(team.getId(), currentUserId);
                                        boolean canManage = isUserTeamAdmin(team.getId(), currentUserId) ||
                                                        (project != null && teamSecurityService.isProjectAdmin(
                                                                        project.getId(), currentUserId));

                                        if (project != null) {
                                                return mapToTeamResponse(team, project, creator, memberCount, userRole,
                                                                canManage);
                                        } else {
                                                // Fallback for teams without project context
                                                return mapToTeamResponse(team, creator, memberCount, userRole);
                                        }
                                })
                                .collect(Collectors.toList());

                return new PageImpl<>(teamResponses, pageable, teams.getTotalElements());
        }

        /**
         * Get team by ID.
         * 
         * @param teamId        the team ID
         * @param currentUserId the ID of the current user
         * @return the team response
         * @throws TeamNotFoundException     if team not found
         * @throws TeamAccessDeniedException if user cannot access private team
         */
        @Transactional(readOnly = true)
        public TeamResponse getTeamById(UUID teamId, UUID currentUserId) {
                logger.debug("Getting team by ID: {} for user: {}", teamId, currentUserId);

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                // Check access permissions
                checkTeamAccess(team, currentUserId);

                User creator = userRepository.findById(team.getCreatedBy()).orElse(null);
                Project project = projectRepository.findById(team.getProjectId()).orElse(null);
                int memberCount = (int) teamMemberRepository.countByTeamId(teamId);
                TeamRole userRole = getUserTeamRole(teamId, currentUserId);
                boolean canManage = isUserTeamAdmin(teamId, currentUserId) ||
                                (project != null && teamSecurityService.isProjectAdmin(project.getId(), currentUserId));

                if (project != null) {
                        return mapToTeamResponse(team, project, creator, memberCount, userRole, canManage);
                } else {
                        // Fallback for teams without project context
                        return mapToTeamResponse(team, creator, memberCount, userRole);
                }
        }

        /**
         * Get team by slug.
         * 
         * @param teamSlug      the team slug
         * @param currentUserId the ID of the current user
         * @return the team response
         * @throws TeamNotFoundException     if team not found
         * @throws TeamAccessDeniedException if user cannot access private team
         */
        @Transactional(readOnly = true)
        public TeamResponse getTeamBySlug(String teamSlug, UUID currentUserId) {
                logger.debug("Getting team by slug: {} for user: {}", teamSlug, currentUserId);

                Team team = teamRepository.findByTeamSlug(teamSlug)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                // Check access permissions
                checkTeamAccess(team, currentUserId);

                User creator = userRepository.findById(team.getCreatedBy()).orElse(null);
                Project project = projectRepository.findById(team.getProjectId()).orElse(null);
                int memberCount = (int) teamMemberRepository.countByTeamId(team.getId());
                TeamRole userRole = getUserTeamRole(team.getId(), currentUserId);
                boolean canManage = isUserTeamAdmin(team.getId(), currentUserId) ||
                                (project != null && teamSecurityService.isProjectAdmin(project.getId(), currentUserId));

                if (project != null) {
                        return mapToTeamResponse(team, project, creator, memberCount, userRole, canManage);
                } else {
                        // Fallback for teams without project context
                        return mapToTeamResponse(team, creator, memberCount, userRole);
                }
        }

        /**
         * Update team details.
         * Only team admins can update team details.
         * 
         * @param teamId        the team ID
         * @param request       the update request
         * @param currentUserId the ID of the current user
         * @return the updated team response
         * @throws TeamNotFoundException     if team not found
         * @throws TeamAccessDeniedException if user is not team admin
         * @throws TeamNameConflictException if new name already exists
         */
        public TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request, UUID currentUserId) {
                logger.info("Updating team: {} by user: {}", teamId, currentUserId);

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                // Check admin permissions
                if (!isUserTeamAdmin(teamId, currentUserId)) {
                        throw new TeamAccessDeniedException("Only team admins can update team details");
                }

                // Update fields if provided
                if (request.getName() != null) {
                        // Check if new name conflicts with existing team (excluding current team)
                        if (!request.getName().equalsIgnoreCase(team.getName()) &&
                                        teamRepository.existsByNameIgnoreCase(request.getName())) {
                                throw new TeamNameConflictException("Team name already exists");
                        }
                        team.setName(request.getName());

                        // Update slug if name changed
                        String newSlug = slugService.generateSlug(request.getName());
                        if (!newSlug.equals(team.getTeamSlug())) {
                                team.setTeamSlug(generateUniqueSlug(newSlug));
                        }
                }

                if (request.getDescription() != null) {
                        team.setDescription(request.getDescription());
                }

                Team updatedTeam = teamRepository.save(team);
                User creator = userRepository.findById(team.getCreatedBy()).orElse(null);
                int memberCount = (int) teamMemberRepository.countByTeamId(teamId);
                TeamRole userRole = getUserTeamRole(teamId, currentUserId);

                logger.info("Team updated successfully: {}", updatedTeam.getName());

                return mapToTeamResponse(updatedTeam, creator, memberCount, userRole);
        }

        /**
         * Delete a team.
         * Only team admins can delete teams.
         * 
         * @param teamId        the team ID
         * @param currentUserId the ID of the current user
         * @throws TeamNotFoundException     if team not found
         * @throws TeamAccessDeniedException if user is not team admin
         */
        public void deleteTeam(UUID teamId, UUID currentUserId) {
                logger.info("Deleting team: {} by user: {}", teamId, currentUserId);

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                // Check admin permissions
                if (!isUserTeamAdmin(teamId, currentUserId)) {
                        throw new TeamAccessDeniedException("Only team admins can delete teams");
                }

                // Delete all team members first (cascade should handle this, but explicit is
                // better)
                teamMemberRepository.deleteByTeamId(teamId);

                // Delete the team
                teamRepository.delete(team);

                logger.info("Team deleted successfully: {}", team.getName());
        }

        // ===== MEMBERSHIP MANAGEMENT =====

        /**
         * Add a member to a team.
         * Only team admins or project admins can add members.
         * 
         * @param teamId        the team ID
         * @param request       the add member request
         * @param currentUserId the ID of the current user
         * @return the team member response
         * @throws TeamNotFoundException     if team not found
         * @throws TeamAccessDeniedException if user is not team admin or project admin
         * @throws RuntimeException          if user to add not found or already member
         */
        public TeamMemberResponse addMember(UUID teamId, AddMemberRequest request, UUID currentUserId) {
                logger.info("Adding member {} to team {} by user {}", request.getUserId(), teamId, currentUserId);

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                // Check admin permissions - allow both team admins and project admins
                boolean isTeamAdmin = isUserTeamAdmin(teamId, currentUserId);
                boolean isProjectAdmin = teamSecurityService.isProjectAdmin(team.getProjectId(), currentUserId);

                if (!isTeamAdmin && !isProjectAdmin) {
                        logger.warn("Access denied: user {} cannot add members to team {} (not team admin or project admin)",
                                        currentUserId, teamId);
                        throw new TeamAccessDeniedException("Only team admins or project admins can add members");
                }

                // Check if user to add exists
                User userToAdd = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new UserNotFoundException("User not found"));

                // Check if user is already a member
                if (teamMemberRepository.existsByTeamIdAndUserId(teamId, request.getUserId())) {
                        throw new InvalidTeamOperationException("User is already a member of this team");
                }

                // Create membership
                TeamMember member = TeamMember.builder()
                                .teamId(teamId)
                                .userId(request.getUserId())
                                .role(request.getRole())
                                .addedBy(currentUserId)
                                .build();

                TeamMember savedMember = teamMemberRepository.save(member);

                User addedByUser = userRepository.findById(currentUserId).orElse(null);

                logger.info("Member added successfully: {} to team {} by {} (team admin: {}, project admin: {})",
                                userToAdd.getEmail(), team.getName(),
                                addedByUser != null ? addedByUser.getEmail() : currentUserId,
                                isTeamAdmin, isProjectAdmin);

                // 🔔 Send notification about team member joined
                if (userToAdd != null && team != null) {
                        try {
                                teamNotificationEventListener.onTeamMemberJoined(team, userToAdd, addedByUser,
                                                request.getRole());
                                logger.debug("Team member joined notification triggered for user {} in team {}",
                                                request.getUserId(), teamId);
                        } catch (Exception e) {
                                logger.error("Failed to send team member joined notification: {}", e.getMessage());
                                // Don't fail the member addition for notification errors
                        }
                }

                return mapToTeamMemberResponse(savedMember, userToAdd, addedByUser);
        }

        /**
         * Update a team member's role.
         * Only team admins can update member roles.
         * 
         * @param teamId        the team ID
         * @param userId        the user ID whose role to update
         * @param request       the role update request
         * @param currentUserId the ID of the current user
         * @return the updated team member response
         * @throws TeamNotFoundException       if team not found
         * @throws TeamMemberNotFoundException if member not found
         * @throws TeamAccessDeniedException   if user is not team admin
         * @throws LastAdminException          if trying to demote the last admin
         */
        public TeamMemberResponse updateMemberRole(UUID teamId, UUID userId, UpdateMemberRoleRequest request,
                        UUID currentUserId) {
                logger.info("Updating role for user {} in team {} to {} by user {}", userId, teamId, request.getRole(),
                                currentUserId);

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                // Check admin permissions
                if (!isUserTeamAdmin(teamId, currentUserId)) {
                        throw new TeamAccessDeniedException("Only team admins can update member roles");
                }

                TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                                .orElseThrow(() -> new TeamMemberNotFoundException("Team member not found"));

                // Prevent removing last admin
                if (member.getRole() == TeamRole.ADMIN && request.getRole() == TeamRole.MEMBER) {
                        long adminCount = teamMemberRepository.countByTeamIdAndRole(teamId, TeamRole.ADMIN);
                        if (adminCount <= 1) {
                                throw new LastAdminException(teamId.toString(), userId, "demote");
                        }
                }

                // Store old role for notification
                TeamRole oldRole = member.getRole();
                member.setRole(request.getRole());
                TeamMember updatedMember = teamMemberRepository.save(member);

                User user = userRepository.findById(userId).orElse(null);
                User updatedByUser = userRepository.findById(currentUserId).orElse(null);

                logger.info("Member role updated successfully: {} to {} in team {}",
                                user != null ? user.getEmail() : userId,
                                request.getRole(), team.getName());

                // 🔔 Send notification about team role change
                if (user != null && team != null && !oldRole.equals(request.getRole())) {
                        try {
                                teamNotificationEventListener.onTeamRoleChanged(team, user, oldRole, request.getRole(),
                                                updatedByUser);
                                logger.debug("Team role change notification triggered for user {} in team {}", userId,
                                                teamId);
                        } catch (Exception e) {
                                logger.error("Failed to send team role change notification: {}", e.getMessage());
                                // Don't fail the role update for notification errors
                        }
                }

                return mapToTeamMemberResponse(updatedMember, user, updatedByUser);
        }

        /**
         * Remove a member from a team.
         * Only team admins can remove members.
         * Cannot remove the last admin.
         * 
         * @param teamId        the team ID
         * @param userId        the user ID to remove
         * @param currentUserId the ID of the current user
         * @throws TeamNotFoundException       if team not found
         * @throws TeamMemberNotFoundException if member not found
         * @throws TeamAccessDeniedException   if user is not team admin
         * @throws LastAdminException          if trying to remove the last admin
         */
        public void removeMember(UUID teamId, UUID userId, UUID currentUserId) {
                logger.info("Removing user {} from team {} by user {}", userId, teamId, currentUserId);

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                // Check admin permissions
                if (!isUserTeamAdmin(teamId, currentUserId)) {
                        throw new TeamAccessDeniedException("Only team admins can remove members");
                }

                TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                                .orElseThrow(() -> new TeamMemberNotFoundException("Team member not found"));

                // Prevent removing last admin
                if (member.getRole() == TeamRole.ADMIN) {
                        long adminCount = teamMemberRepository.countByTeamIdAndRole(teamId, TeamRole.ADMIN);
                        if (adminCount <= 1) {
                                throw new LastAdminException(teamId.toString(), userId, "remove");
                        }
                }

                teamMemberRepository.delete(member);

                User user = userRepository.findById(userId).orElse(null);
                logger.info("Member removed successfully: {} from team {}", user != null ? user.getEmail() : userId,
                                team.getName());
        }

        /**
         * Leave a team.
         * Members can leave teams themselves.
         * Cannot leave if you're the last admin.
         * 
         * @param teamId        the team ID
         * @param currentUserId the ID of the current user
         * @throws TeamNotFoundException       if team not found
         * @throws TeamMemberNotFoundException if user is not a member
         * @throws LastAdminException          if trying to leave as the last admin
         */
        public void leaveTeam(UUID teamId, UUID currentUserId) {
                logger.info("User {} leaving team {}", currentUserId, teamId);

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                                .orElseThrow(() -> new TeamMemberNotFoundException(
                                                "You are not a member of this team"));

                // Prevent last admin from leaving
                if (member.getRole() == TeamRole.ADMIN) {
                        long adminCount = teamMemberRepository.countByTeamIdAndRole(teamId, TeamRole.ADMIN);
                        if (adminCount <= 1) {
                                throw new LastAdminException(teamId.toString(), currentUserId, "leave",
                                                "Cannot leave team as the last admin. Transfer admin privileges to another member first");
                        }
                }

                teamMemberRepository.delete(member);

                User user = userRepository.findById(currentUserId).orElse(null);
                logger.info("User left team successfully: {} from team {}",
                                user != null ? user.getEmail() : currentUserId,
                                team.getName());
        }

        /**
         * Get user's teams.
         * 
         * @param userId the user ID
         * @return list of teams user is member of
         */
        @Transactional(readOnly = true)
        public List<TeamResponse> getUserTeams(UUID userId) {
                logger.debug("Getting teams for user: {}", userId);

                List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);

                return memberships.stream()
                                .map(membership -> {
                                        Team team = teamRepository.findById(membership.getTeamId()).orElse(null);
                                        if (team == null)
                                                return null;

                                        User creator = userRepository.findById(team.getCreatedBy()).orElse(null);
                                        int memberCount = (int) teamMemberRepository.countByTeamId(team.getId());

                                        // Get project information for the team
                                        Project project = null;
                                        if (team.getProjectId() != null) {
                                                project = projectRepository.findById(team.getProjectId()).orElse(null);
                                        }

                                        boolean canManage = isUserTeamAdmin(team.getId(), userId) ||
                                                        (project != null && teamSecurityService
                                                                        .isProjectAdmin(project.getId(), userId));

                                        if (project != null) {
                                                return mapToTeamResponse(team, project, creator, memberCount,
                                                                membership.getRole(), canManage);
                                        } else {
                                                // Fallback for teams without project context
                                                return mapToTeamResponse(team, creator, memberCount,
                                                                membership.getRole());
                                        }
                                })
                                .filter(teamResponse -> teamResponse != null)
                                .collect(Collectors.toList());
        }

        /**
         * Get team members with pagination.
         * 
         * @param teamId        the team ID
         * @param pageable      pagination parameters
         * @param currentUserId the ID of the current user
         * @return page of team members
         * @throws TeamNotFoundException     if team not found
         * @throws TeamAccessDeniedException if user cannot access private team
         */
        @Transactional(readOnly = true)
        public Page<TeamMemberResponse> getTeamMembers(UUID teamId, Pageable pageable, UUID currentUserId) {
                logger.debug("Getting members for team: {} by user: {}", teamId, currentUserId);

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                // Check access permissions
                checkTeamAccess(team, currentUserId);

                Page<TeamMember> members = teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(teamId, pageable);

                List<TeamMemberResponse> memberResponses = members.getContent().stream()
                                .map(member -> {
                                        User user = userRepository.findById(member.getUserId()).orElse(null);
                                        User addedByUser = userRepository.findById(member.getAddedBy()).orElse(null);
                                        return mapToTeamMemberResponse(member, user, addedByUser);
                                })
                                .collect(Collectors.toList());

                return new PageImpl<>(memberResponses, pageable, members.getTotalElements());
        }

        /**
         * Join a team (user joins themselves).
         * Any project member can join a team in their project.
         * 
         * @param teamId        the team ID
         * @param currentUserId the ID of the user joining
         * @return the team member response
         * @throws TeamNotFoundException     if team not found
         * @throws TeamAccessDeniedException if user cannot join the team
         * @throws RuntimeException          if user is already a member
         */
        public TeamMemberResponse joinTeam(UUID teamId, UUID currentUserId) {
                logger.info("User {} joining team {}", currentUserId, teamId);

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

                // Check if user is already a member
                if (isUserTeamMember(teamId, currentUserId)) {
                        logger.warn("User {} is already a member of team {}", currentUserId, teamId);
                        throw new RuntimeException("You are already a member of this team");
                }

                // Check if user can join the team (must be a project member)
                if (!teamSecurityService.isProjectMember(team.getProjectId(), currentUserId)) {
                        logger.warn("Access denied: user {} cannot join team {} (not a project member)",
                                        currentUserId, teamId);
                        throw new TeamAccessDeniedException("Only project members can join teams");
                }

                // Create membership with MEMBER role
                TeamMember member = TeamMember.builder()
                                .teamId(teamId)
                                .userId(currentUserId)
                                .role(TeamRole.MEMBER)
                                .addedBy(currentUserId) // User adds themselves
                                .build();

                TeamMember savedMember = teamMemberRepository.save(member);

                User user = userRepository.findById(currentUserId).orElse(null);

                logger.info("User {} successfully joined team {}",
                                user != null ? user.getEmail() : currentUserId, team.getName());

                return mapToTeamMemberResponse(savedMember, user, user);
        }

        // ===== UTILITY METHODS =====

        /**
         * Generate unique slug by checking for conflicts and appending counter if
         * needed.
         */
        private String generateUniqueSlug(String baseSlug) {
                if (!teamRepository.existsByTeamSlug(baseSlug)) {
                        return baseSlug;
                }

                int counter = 2;
                String uniqueSlug;
                do {
                        uniqueSlug = slugService.generateUniqueSlug(baseSlug, counter);
                        counter++;
                } while (teamRepository.existsByTeamSlug(uniqueSlug));

                return uniqueSlug;
        }

        /**
         * Check if user can access team (all teams are public).
         */
        private void checkTeamAccess(Team team, UUID userId) {
                // All teams are public and accessible to all users
                return;
        }

        /**
         * Check if user is a team member.
         */
        @Transactional(readOnly = true)
        public boolean isUserTeamMember(UUID teamId, UUID userId) {
                return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
        }

        /**
         * Check if user is a team admin.
         */
        @Transactional(readOnly = true)
        public boolean isUserTeamAdmin(UUID teamId, UUID userId) {
                return teamMemberRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.ADMIN);
        }

        /**
         * Get user's role in team.
         */
        @Transactional(readOnly = true)
        public TeamRole getUserTeamRole(UUID teamId, UUID userId) {
                return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                                .map(TeamMember::getRole)
                                .orElse(null);
        }

        /**
         * Map Team entity to TeamResponse DTO (legacy method for backward
         * compatibility).
         */
        private TeamResponse mapToTeamResponse(Team team, User creator, int memberCount, TeamRole userRole) {
                String creatorName = creator != null ? creator.getFirstName() + " " + creator.getLastName() : "Unknown";

                return new TeamResponse(
                                team.getId(),
                                team.getName(),
                                team.getDescription(),
                                team.getTeamSlug(),
                                team.getProjectId(),
                                null, // projectSlug - will be null for legacy teams
                                team.getCreatedBy(),
                                creatorName,
                                team.getCreatedAt(),
                                team.getUpdatedAt(),
                                memberCount,
                                userRole,
                                false); // canManage - default to false for legacy
        }

        /**
         * Map Team entity to TeamResponse DTO with project context.
         */
        private TeamResponse mapToTeamResponse(Team team, Project project, User creator, int memberCount,
                        TeamRole userRole,
                        boolean canManage) {
                String creatorName = creator != null ? creator.getFirstName() + " " + creator.getLastName() : "Unknown";

                return new TeamResponse(
                                team.getId(),
                                team.getName(),
                                team.getDescription(),
                                team.getTeamSlug(),
                                team.getProjectId(),
                                project.getProjectSlug(),
                                team.getCreatedBy(),
                                creatorName,
                                team.getCreatedAt(),
                                team.getUpdatedAt(),
                                memberCount,
                                userRole,
                                canManage);
        }

        /**
         * Map TeamMember entity to TeamMemberResponse DTO.
         */
        private TeamMemberResponse mapToTeamMemberResponse(TeamMember member, User user, User addedByUser) {
                String addedByName = addedByUser != null ? addedByUser.getFirstName() + " " + addedByUser.getLastName()
                                : "Unknown";

                return new TeamMemberResponse(
                                member.getId(),
                                member.getUserId(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getEmail(),
                                member.getRole(),
                                member.getJoinedAt(),
                                member.getAddedBy(),
                                addedByName);
        }
}