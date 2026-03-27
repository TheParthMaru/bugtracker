package com.pbm5.bugtracker.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pbm5.bugtracker.entity.MemberStatus;
import com.pbm5.bugtracker.entity.ProjectRole;
import com.pbm5.bugtracker.entity.TeamRole;
import com.pbm5.bugtracker.repository.ProjectMemberRepository;
import com.pbm5.bugtracker.repository.TeamMemberRepository;
import com.pbm5.bugtracker.repository.TeamRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling team-related security and permission checks.
 * This service ensures proper access control for team operations within
 * projects.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamSecurityService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * Check if a user can create teams within a project.
     * Only project admins can create teams.
     * 
     * @param projectId The project ID
     * @param userId    The user ID
     * @return true if user can create teams, false otherwise
     */
    public boolean canCreateTeam(UUID projectId, UUID userId) {
        log.debug("Checking if user {} can create team in project {}", userId, projectId);

        boolean canCreate = projectMemberRepository.existsByProject_IdAndUserIdAndStatusAndRole(
                projectId, userId, MemberStatus.ACTIVE, ProjectRole.ADMIN);

        log.debug("User {} can create team in project {}: {}", userId, projectId, canCreate);
        return canCreate;
    }

    /**
     * Check if a user can manage a specific team.
     * User must be either team admin OR project admin.
     * 
     * @param teamId The team ID
     * @param userId The user ID
     * @return true if user can manage the team, false otherwise
     */
    public boolean canManageTeam(UUID teamId, UUID userId) {
        log.debug("Checking if user {} can manage team {}", userId, teamId);

        boolean isTeamAdmin = isTeamAdmin(teamId, userId);
        boolean isProjectAdmin = isProjectAdmin(teamId, userId);
        boolean canManage = isTeamAdmin || isProjectAdmin;

        log.debug("User {} can manage team {}: {} (teamAdmin: {}, projectAdmin: {})",
                userId, teamId, canManage, isTeamAdmin, isProjectAdmin);
        return canManage;
    }

    /**
     * Check if a user can join a team.
     * User must be an active project member.
     * 
     * @param teamId The team ID
     * @param userId The user ID
     * @return true if user can join the team, false otherwise
     */
    public boolean canJoinTeam(UUID teamId, UUID userId) {
        log.debug("Checking if user {} can join team {}", userId, teamId);

        UUID projectId = teamRepository.findProjectIdById(teamId);
        if (projectId == null) {
            log.warn("Project ID not found for team {}", teamId);
            return false;
        }

        boolean canJoin = projectMemberRepository.existsByProject_IdAndUserIdAndStatus(
                projectId, userId, MemberStatus.ACTIVE);

        log.debug("User {} can join team {}: {}", userId, teamId, canJoin);
        return canJoin;
    }

    /**
     * Check if a user is a team admin.
     * 
     * @param teamId The team ID
     * @param userId The user ID
     * @return true if user is team admin, false otherwise
     */
    public boolean isTeamAdmin(UUID teamId, UUID userId) {
        return teamMemberRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.ADMIN);
    }

    /**
     * Check if a user is a project admin for the project containing the team.
     * 
     * @param teamId The team ID
     * @param userId The user ID
     * @return true if user is project admin, false otherwise
     */
    public boolean isProjectAdminByTeam(UUID teamId, UUID userId) {
        UUID projectId = teamRepository.findProjectIdById(teamId);
        if (projectId == null) {
            return false;
        }

        return isProjectAdmin(projectId, userId);
    }

    /**
     * Check if a user is a project admin for a specific project.
     * 
     * @param projectId The project ID
     * @param userId    The user ID
     * @return true if user is project admin, false otherwise
     */
    public boolean isProjectAdmin(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProject_IdAndUserIdAndStatusAndRole(
                projectId, userId, MemberStatus.ACTIVE, ProjectRole.ADMIN);
    }

    /**
     * Check if a user is a project member.
     * 
     * @param projectId The project ID
     * @param userId    The user ID
     * @return true if user is project member, false otherwise
     */
    public boolean isProjectMember(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProject_IdAndUserIdAndStatus(
                projectId, userId, MemberStatus.ACTIVE);
    }

    /**
     * Check if a user is a team member.
     * 
     * @param teamId The team ID
     * @param userId The user ID
     * @return true if user is team member, false otherwise
     */
    public boolean isTeamMember(UUID teamId, UUID userId) {
        return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
    }

    /**
     * Get the role of a user within a team.
     * 
     * @param teamId The team ID
     * @param userId The user ID
     * @return The user's team role, or null if not a member
     */
    public TeamRole getUserTeamRole(UUID teamId, UUID userId) {
        return teamMemberRepository.findRoleByTeamIdAndUserId(teamId, userId).orElse(null);
    }

    /**
     * Check if a user can remove another user from a team.
     * User must be team admin OR project admin, and cannot remove themselves if
     * they're the only admin.
     * 
     * @param teamId        The team ID
     * @param currentUserId The current user ID
     * @param targetUserId  The target user ID to remove
     * @return true if user can remove the target user, false otherwise
     */
    public boolean canRemoveTeamMember(UUID teamId, UUID currentUserId, UUID targetUserId) {
        log.debug("Checking if user {} can remove user {} from team {}", currentUserId, targetUserId, teamId);

        // Cannot remove yourself
        if (currentUserId.equals(targetUserId)) {
            log.debug("User cannot remove themselves from team");
            return false;
        }

        // Must be team admin or project admin
        if (!canManageTeam(teamId, currentUserId)) {
            log.debug("User {} is not authorized to manage team {}", currentUserId, teamId);
            return false;
        }

        // Check if target user is the only admin
        if (isTeamAdmin(teamId, targetUserId)) {
            long adminCount = teamMemberRepository.countByTeamIdAndRole(teamId, TeamRole.ADMIN);
            if (adminCount <= 1) {
                log.debug("Cannot remove the only admin from team {}", teamId);
                return false;
            }
        }

        log.debug("User {} can remove user {} from team {}", currentUserId, targetUserId, teamId);
        return true;
    }

    /**
     * Check if a user can change another user's role in a team.
     * User must be team admin OR project admin.
     * 
     * @param teamId        The team ID
     * @param currentUserId The current user ID
     * @param targetUserId  The target user ID
     * @return true if user can change the target user's role, false otherwise
     */
    public boolean canChangeTeamMemberRole(UUID teamId, UUID currentUserId, UUID targetUserId) {
        log.debug("Checking if user {} can change role of user {} in team {}", currentUserId, targetUserId, teamId);

        // Cannot change your own role
        if (currentUserId.equals(targetUserId)) {
            log.debug("User cannot change their own role");
            return false;
        }

        // Must be team admin or project admin
        boolean canChange = canManageTeam(teamId, currentUserId);

        log.debug("User {} can change role of user {} in team {}: {}", currentUserId, targetUserId, teamId, canChange);
        return canChange;
    }
}