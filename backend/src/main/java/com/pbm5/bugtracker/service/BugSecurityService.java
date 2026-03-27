package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.Project;
import com.pbm5.bugtracker.entity.ProjectMember;
import com.pbm5.bugtracker.entity.ProjectRole;
import com.pbm5.bugtracker.exception.BugAccessDeniedException;
import com.pbm5.bugtracker.exception.ProjectAccessDeniedException;
import com.pbm5.bugtracker.repository.ProjectMemberRepository;
import com.pbm5.bugtracker.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BugSecurityService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    /**
     * Check if user has access to project
     */
    public boolean hasProjectAccess(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProject_IdAndUserId(projectId, userId);
    }

    /**
     * Check if user has admin role in project
     */
    public boolean hasProjectAdminRole(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProject_IdAndUserIdAndStatusAndRole(projectId, userId,
                com.pbm5.bugtracker.entity.MemberStatus.ACTIVE, ProjectRole.ADMIN);
    }

    /**
     * Check if user has member role in project
     */
    public boolean hasProjectMemberRole(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProject_IdAndUserIdAndStatusAndRole(projectId, userId,
                com.pbm5.bugtracker.entity.MemberStatus.ACTIVE, ProjectRole.MEMBER);
    }

    /**
     * Validate user has access to project
     */
    public void validateProjectAccess(UUID projectId, UUID userId) {
        if (!hasProjectAccess(projectId, userId)) {
            throw new RuntimeException("User does not have access to project: " + projectId);
        }
    }

    /**
     * Validate user has admin role in project
     */
    public void validateProjectAdminRole(UUID projectId, UUID userId) {
        if (!hasProjectAdminRole(projectId, userId)) {
            throw new RuntimeException("User does not have admin role in project: " + projectId);
        }
    }

    /**
     * Check if user can view bug
     */
    public boolean canViewBug(Bug bug, UUID userId) {
        // Project members can view all bugs in the project
        return hasProjectAccess(bug.getProject().getId(), userId);
    }

    /**
     * Check if user can create bug
     */
    public boolean canCreateBug(UUID projectId, UUID userId) {
        // Project members can create bugs
        return hasProjectAccess(projectId, userId);
    }

    /**
     * Check if user can update bug
     */
    public boolean canUpdateBug(Bug bug, UUID userId) {
        // Project admins can update any bug
        if (hasProjectAdminRole(bug.getProject().getId(), userId)) {
            return true;
        }

        // Bug reporter can update their own bugs
        if (bug.getReporter().getId().equals(userId)) {
            return true;
        }

        // Bug assignee can update assigned bugs
        if (bug.getAssignee() != null && bug.getAssignee().getId().equals(userId)) {
            return true;
        }

        return false;
    }

    /**
     * Check if user can delete bug
     */
    public boolean canDeleteBug(Bug bug, UUID userId) {
        // Project admins can delete any bug
        if (hasProjectAdminRole(bug.getProject().getId(), userId)) {
            return true;
        }

        // Bug reporter can delete their own bugs
        if (bug.getReporter().getId().equals(userId)) {
            return true;
        }

        // Bug assignee can delete bugs assigned to them
        if (bug.getAssignee() != null && bug.getAssignee().getId().equals(userId)) {
            return true;
        }

        return false;
    }

    /**
     * Check if user can assign bug
     */
    public boolean canAssignBug(Bug bug, UUID userId) {
        // Project admins can assign bugs to any member
        if (hasProjectAdminRole(bug.getProject().getId(), userId)) {
            return true;
        }

        // Users can assign bugs to themselves
        return bug.getReporter().getId().equals(userId);
    }

    /**
     * Check if user can change bug status
     */
    public boolean canChangeBugStatus(Bug bug, UUID userId) {
        // Project admins can change any bug status
        if (hasProjectAdminRole(bug.getProject().getId(), userId)) {
            return true;
        }

        // Bug reporter can change status of their own bugs
        if (bug.getReporter().getId().equals(userId)) {
            return true;
        }

        // Bug assignee can change status of assigned bugs
        if (bug.getAssignee() != null && bug.getAssignee().getId().equals(userId)) {
            return true;
        }

        return false;
    }

    /**
     * Check if user can manage labels
     */
    public boolean canManageLabels(UUID projectId, UUID userId) {
        // Only project admins can manage labels
        return hasProjectAdminRole(projectId, userId);
    }

    /**
     * Validate user can view bug
     */
    public void validateBugViewAccess(Bug bug, UUID userId) {
        if (!canViewBug(bug, userId)) {
            throw new BugAccessDeniedException("User does not have permission to view this bug");
        }
    }

    /**
     * Validate user can create bug
     */
    public void validateBugCreateAccess(UUID projectId, UUID userId) {
        if (!canCreateBug(projectId, userId)) {
            throw new RuntimeException("User does not have permission to create bugs in this project");
        }
    }

    /**
     * Validate user can update bug
     */
    public void validateBugUpdateAccess(Bug bug, UUID userId) {
        if (!canUpdateBug(bug, userId)) {
            throw new BugAccessDeniedException("User does not have permission to update this bug");
        }
    }

    /**
     * Validate user can delete bug
     */
    public void validateBugDeleteAccess(Bug bug, UUID userId) {
        if (!canDeleteBug(bug, userId)) {
            throw new BugAccessDeniedException("User does not have permission to delete this bug");
        }
    }

    /**
     * Validate user can assign bug
     */
    public void validateBugAssignAccess(Bug bug, UUID userId) {
        if (!canAssignBug(bug, userId)) {
            throw new BugAccessDeniedException("User does not have permission to assign this bug");
        }
    }

    /**
     * Validate user can change bug status
     */
    public void validateBugStatusChangeAccess(Bug bug, UUID userId) {
        if (!canChangeBugStatus(bug, userId)) {
            throw new BugAccessDeniedException("User does not have permission to change status of this bug");
        }
    }

    /**
     * Validate user can manage labels
     */
    public void validateLabelManagementAccess(UUID projectId, UUID userId) {
        if (!canManageLabels(projectId, userId)) {
            throw new RuntimeException("User does not have permission to manage labels in this project");
        }
    }

    /**
     * Get user's role in project
     */
    public ProjectRole getUserProjectRole(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProject_IdAndUserId(projectId, userId)
                .map(ProjectMember::getRole)
                .orElse(null);
    }

    /**
     * Check if user is project admin
     */
    public boolean isProjectAdmin(UUID projectId, UUID userId) {
        return hasProjectAdminRole(projectId, userId);
    }

    /**
     * Check if user is project member
     */
    public boolean isProjectMember(UUID projectId, UUID userId) {
        return hasProjectAccess(projectId, userId);
    }
}