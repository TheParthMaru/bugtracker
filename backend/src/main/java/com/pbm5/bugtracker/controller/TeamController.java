package com.pbm5.bugtracker.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.pbm5.bugtracker.dto.*;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.service.TeamService;

import java.util.Map;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for team operations.
 * 
 * This controller handles both standalone teams and project-scoped teams.
 * Teams can be accessed either directly or within a project context.
 * 
 * Base URLs:
 * - /api/bugtracker/v1/teams (standalone teams)
 * - /api/bugtracker/v1/projects/{projectSlug}/teams (project-scoped teams)
 * 
 * Features:
 * - Create teams (standalone or within projects)
 * - List teams with search and pagination
 * - Get team details by ID or slug
 * - Update team information
 * - Delete teams
 * - Team membership management (join, leave, add, remove, update roles)
 * 
 * Security:
 * - All endpoints require authentication
 * - Team-level and project-level access control
 */
@RestController
@RequestMapping("/api/bugtracker/v1")
@RequiredArgsConstructor
@Slf4j
public class TeamController {

    private final TeamService teamService;

    // ===== STANDALONE TEAMS ENDPOINTS =====

    /**
     * Get all teams with optional search and pagination.
     * This endpoint provides access to teams across all projects for discovery.
     * 
     * @param search         optional search term for team name/description
     * @param pageable       pagination parameters
     * @param authentication the current user authentication
     * @return page of teams
     */
    @GetMapping
    public ResponseEntity<Page<TeamResponse>> getTeams(
            @RequestParam(required = false) String search,
            Pageable pageable,
            Authentication authentication) {

        log.info("Fetching teams with search: '{}'", search);

        UUID currentUserId = getCurrentUserId(authentication);
        Page<TeamResponse> teams = teamService.getTeams(pageable, search, currentUserId);

        log.info("Found {} teams", teams.getTotalElements());
        return ResponseEntity.ok(teams);
    }

    /**
     * Get team by ID.
     * 
     * @param teamId         the team ID
     * @param authentication the current user authentication
     * @return the team details
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeamById(
            @PathVariable UUID teamId,
            Authentication authentication) {

        log.info("Fetching team by ID: {}", teamId);

        UUID currentUserId = getCurrentUserId(authentication);
        TeamResponse team = teamService.getTeamById(teamId, currentUserId);

        log.info("Team '{}' found", team.getName());
        return ResponseEntity.ok(team);
    }

    /**
     * Get team by slug.
     * 
     * @param teamSlug       the team slug
     * @param authentication the current user authentication
     * @return the team details
     */
    @GetMapping("/teams/{teamSlug}")
    public ResponseEntity<TeamResponse> getTeamBySlug(
            @PathVariable String teamSlug,
            Authentication authentication) {

        log.info("Fetching team by slug: {}", teamSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        TeamResponse team = teamService.getTeamBySlug(teamSlug, currentUserId);

        log.info("Team '{}' found", team.getName());
        return ResponseEntity.ok(team);
    }

    /**
     * Create a new team within a project.
     * 
     * @param request        the team creation request with project slug
     * @param authentication the current user authentication
     * @return the created team
     */
    @PostMapping("/projects/{projectSlug}/teams")
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @PathVariable String projectSlug,
            Authentication authentication) {

        log.info("Creating team '{}' in project '{}'", request.getName(), projectSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        TeamResponse team = teamService.createTeamInProject(projectSlug, request, currentUserId);

        log.info("Team '{}' created successfully in project '{}'", team.getName(), projectSlug);
        return ResponseEntity.status(HttpStatus.CREATED).body(team);
    }

    // ===== PROJECT-SCOPED TEAMS ENDPOINTS =====

    /**
     * Get all teams in a specific project with optional search and pagination.
     * 
     * @param projectSlug    the project slug
     * @param search         optional search term for team name/description
     * @param pageable       pagination parameters
     * @param authentication the current user authentication
     * @return page of teams in the project
     */
    @GetMapping("/projects/{projectSlug}/teams")
    public ResponseEntity<Page<TeamResponse>> getTeamsInProject(
            @PathVariable String projectSlug,
            @RequestParam(required = false) String search,
            Pageable pageable,
            Authentication authentication) {

        log.info("Fetching teams in project '{}' with search: '{}'", projectSlug, search);

        UUID currentUserId = getCurrentUserId(authentication);
        Page<TeamResponse> teams = teamService.getTeamsInProject(projectSlug, search, pageable, currentUserId);

        log.info("Found {} teams in project '{}'", teams.getTotalElements(), projectSlug);
        return ResponseEntity.ok(teams);
    }

    /**
     * Get a specific team by slug within a project.
     * 
     * @param projectSlug    the project slug
     * @param teamSlug       the team slug
     * @param authentication the current user authentication
     * @return the team details
     */
    @GetMapping("/projects/{projectSlug}/teams/{teamSlug}")
    public ResponseEntity<TeamResponse> getTeamBySlugInProject(
            @PathVariable String projectSlug,
            @PathVariable String teamSlug,
            Authentication authentication) {

        log.info("Fetching team '{}' in project '{}'", teamSlug, projectSlug);
        log.debug("TeamController -> getTeamBySlugInProject -> Received parameters: projectSlug='{}', teamSlug='{}'",
                projectSlug, teamSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        TeamResponse team = teamService.getTeamInProject(projectSlug, teamSlug, currentUserId);

        log.info("Team '{}' found in project '{}'", team.getName(), projectSlug);
        return ResponseEntity.ok(team);
    }

    /**
     * Update team information within a project.
     * 
     * @param projectSlug    the project slug
     * @param teamSlug       the team slug
     * @param request        the update request
     * @param authentication the current user authentication
     * @return success message with updated team
     */
    @PutMapping("/projects/{projectSlug}/teams/{teamSlug}")
    public ResponseEntity<TeamResponse> updateTeamInProject(
            @PathVariable String projectSlug,
            @PathVariable String teamSlug,
            @Valid @RequestBody UpdateTeamRequest request,
            Authentication authentication) {

        log.info("Updating team '{}' in project '{}'", teamSlug, projectSlug);
        log.debug(
                "TeamController -> updateTeamInProject -> Received parameters: projectSlug='{}', teamSlug='{}', request={}",
                projectSlug, teamSlug, request);

        UUID currentUserId = getCurrentUserId(authentication);
        TeamResponse updatedTeam = teamService.updateTeamInProject(projectSlug, teamSlug, request, currentUserId);

        log.info("Team '{}' updated successfully in project '{}'", updatedTeam.getName(), projectSlug);

        return ResponseEntity.ok(updatedTeam);
    }

    /**
     * Delete a team from a project.
     * 
     * @param projectSlug    the project slug
     * @param teamSlug       the team slug
     * @param authentication the current user authentication
     * @return success message
     */
    @DeleteMapping("/projects/{projectSlug}/teams/{teamSlug}")
    public ResponseEntity<SuccessResponse> deleteTeamInProject(
            @PathVariable String projectSlug,
            @PathVariable String teamSlug,
            Authentication authentication) {

        log.info("Deleting team '{}' from project '{}'", teamSlug, projectSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        String teamName = teamService.deleteTeamFromProject(projectSlug, teamSlug, currentUserId);

        SuccessResponse response = SuccessResponse.teamDeleted(teamName, projectSlug);
        log.info("Team '{}' deleted successfully from project '{}'", teamName, projectSlug);

        return ResponseEntity.ok(response);
    }

    // ===== TEAM MEMBERSHIP OPERATIONS (BOTH MODES) =====

    /**
     * Join a team (project-scoped).
     * 
     * @param projectSlug    the project slug
     * @param teamSlug       the team slug
     * @param authentication the current user authentication
     * @return success message
     */
    @PostMapping("/projects/{projectSlug}/teams/{teamSlug}/join")
    public ResponseEntity<SuccessResponse> joinTeamBySlug(
            @PathVariable String projectSlug,
            @PathVariable String teamSlug,
            Authentication authentication) {

        log.debug("DEBUG: Join team endpoint called with projectSlug: '{}', teamSlug: '{}'", projectSlug, teamSlug);
        log.info("User joining team '{}' in project '{}'", teamSlug, projectSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        String teamName = teamService.joinProjectTeam(projectSlug, teamSlug, currentUserId);

        SuccessResponse response = new SuccessResponse(
                String.format("Successfully joined team '%s' in project '%s'", teamName, projectSlug),
                "TEAM_JOINED",
                Map.of("teamName", teamName, "projectSlug", projectSlug));

        log.info("User successfully joined team '{}'", teamName);
        return ResponseEntity.ok(response);
    }

    /**
     * Leave a team (project-scoped).
     * 
     * @param projectSlug    the project slug
     * @param teamSlug       the team slug
     * @param authentication the current user authentication
     * @return success message
     */
    @PostMapping("/projects/{projectSlug}/teams/{teamSlug}/leave")
    public ResponseEntity<SuccessResponse> leaveTeamBySlug(
            @PathVariable String projectSlug,
            @PathVariable String teamSlug,
            Authentication authentication) {

        log.debug("DEBUG: Leave team endpoint called with projectSlug: '{}', teamSlug: '{}'", projectSlug, teamSlug);
        log.info("User leaving team '{}' in project '{}'", teamSlug, projectSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        String teamName = teamService.leaveProjectTeam(projectSlug, teamSlug, currentUserId);

        SuccessResponse response = new SuccessResponse(
                String.format("Successfully left team '%s' in project '%s'", teamName, projectSlug),
                "TEAM_LEFT",
                Map.of("teamName", teamName, "projectSlug", projectSlug));

        log.info("User successfully left team '{}'", teamName);
        return ResponseEntity.ok(response);
    }

    /**
     * Get team members with pagination (project-scoped).
     * 
     * @param projectSlug    the project slug
     * @param teamSlug       the team slug
     * @param pageable       pagination parameters
     * @param authentication the current user authentication
     * @return page of team members
     */
    @GetMapping("/projects/{projectSlug}/teams/{teamSlug}/members")
    public ResponseEntity<Page<TeamMemberResponse>> getTeamMembersBySlug(
            @PathVariable String projectSlug,
            @PathVariable String teamSlug,
            Pageable pageable,
            Authentication authentication) {

        log.info("Fetching members of team '{}' in project '{}'", teamSlug, projectSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        Page<TeamMemberResponse> members = teamService.getTeamMembersInProject(projectSlug, teamSlug, pageable,
                currentUserId);

        log.info("Found {} members in team '{}'", members.getTotalElements(), teamSlug);
        return ResponseEntity.ok(members);
    }

    /**
     * Add a member to a team (project-scoped).
     * 
     * @param projectSlug    the project slug
     * @param teamSlug       the team slug
     * @param request        the add member request
     * @param authentication the current user authentication
     * @return success message with member details
     */
    @PostMapping("/projects/{projectSlug}/teams/{teamSlug}/members")
    public ResponseEntity<SuccessResponse> addMemberBySlug(
            @PathVariable String projectSlug,
            @PathVariable String teamSlug,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {

        log.info("Adding member to team '{}' in project '{}'", teamSlug, projectSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        TeamMemberResponse member = teamService.addMemberToTeamInProject(projectSlug, teamSlug, request, currentUserId);

        String memberName = member.getFirstName() + " " + member.getLastName();
        SuccessResponse response = SuccessResponse.memberAdded(memberName, teamSlug, member.getRole().name());

        log.info("Member '{}' added successfully to team '{}' as {}", memberName, teamSlug, member.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update member role in a team (project-scoped).
     * 
     * @param projectSlug    the project slug
     * @param teamSlug       the team slug
     * @param userId         the user ID to update
     * @param request        the role update request
     * @param authentication the current user authentication
     * @return success message
     */
    @PutMapping("/projects/{projectSlug}/teams/{teamSlug}/members/{userId}")
    public ResponseEntity<SuccessResponse> updateMemberRoleInProject(
            @PathVariable String projectSlug,
            @PathVariable String teamSlug,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            Authentication authentication) {

        log.info("Updating role for user {} in team '{}' in project '{}'", userId, teamSlug, projectSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        TeamMemberResponse member = teamService.updateMemberRoleInProject(projectSlug, teamSlug, userId, request,
                currentUserId);

        String memberName = member.getFirstName() + " " + member.getLastName();
        SuccessResponse response = SuccessResponse.roleUpdated(memberName, teamSlug, member.getRole().name());

        log.info("Role updated successfully for '{}' to {} in team '{}'", memberName, member.getRole(), teamSlug);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a member from a team (project-scoped).
     * 
     * @param projectSlug    the project slug
     * @param teamSlug       the team slug
     * @param userId         the user ID to remove
     * @param authentication the current user authentication
     * @return success message
     */
    @DeleteMapping("/projects/{projectSlug}/teams/{teamSlug}/members/{userId}")
    public ResponseEntity<SuccessResponse> removeMemberFromTeam(
            @PathVariable String projectSlug,
            @PathVariable String teamSlug,
            @PathVariable UUID userId,
            Authentication authentication) {

        log.info("Removing user {} from team '{}' in project '{}'", userId, teamSlug, projectSlug);

        UUID currentUserId = getCurrentUserId(authentication);
        String memberName = teamService.removeMemberFromProjectTeam(projectSlug, teamSlug, userId, currentUserId);

        SuccessResponse response = SuccessResponse.memberRemoved(memberName, teamSlug);
        log.info("Member '{}' removed successfully from team '{}'", memberName, teamSlug);

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
}