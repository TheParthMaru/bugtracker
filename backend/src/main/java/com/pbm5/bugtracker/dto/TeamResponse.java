package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.pbm5.bugtracker.entity.TeamRole;

import lombok.Data;

/**
 * Response DTO for team information in project context.
 * 
 * Used in:
 * - GET /api/projects/{projectSlug}/teams (team listing)
 * - GET /api/projects/{projectSlug}/teams/{teamSlug} (team details)
 * - Team references in other DTOs
 * 
 * Contains essential team information with project context.
 * For detailed team info with members, use TeamDetailResponse.
 * 
 * User Context:
 * - currentUserRole indicates current user's role in the team
 * - canManage indicates if current user can manage this team
 */
@Data
public class TeamResponse {

    /**
     * Unique team identifier.
     * Used for internal API calls and database references.
     */
    private UUID id;

    /**
     * Team display name.
     * Shown in team cards, headers, and navigation.
     */
    private String name;

    /**
     * Team description (may be null).
     * Displayed in team cards and detail views.
     */
    private String description;

    /**
     * URL-friendly team identifier with project prefix.
     * Format: <project-projectSlug>-<team-teamSlug>
     * Used in URLs: /projects/{projectSlug}/teams/{teamSlug}
     * Unique within project scope.
     */
    private String teamSlug;

    /**
     * ID of the project this team belongs to.
     * Reference to Project entity.
     */
    private UUID projectId;

    /**
     * Project slug for easy navigation.
     * Used in URLs and breadcrumbs.
     */
    private String projectSlug;

    /**
     * ID of the team creator/admin.
     * Reference to User entity.
     */
    private UUID createdBy;

    /**
     * Full name of the team creator/admin.
     * Used for display purposes in team cards and headers.
     */
    private String creatorName;

    /**
     * Team creation timestamp.
     * Useful for sorting and display purposes.
     */
    private LocalDateTime createdAt;

    /**
     * Team last update timestamp.
     * Useful for tracking recent changes.
     */
    private LocalDateTime updatedAt;

    /**
     * Total number of members in the team.
     * Includes all active members.
     */
    private Integer memberCount;

    /**
     * Current user's role in this team (null if not a member).
     * Used for UI permission controls and role-based features.
     */
    private TeamRole currentUserRole;

    /**
     * Whether the current user can manage this team.
     * True if user is team admin OR project admin.
     * Used for UI permission controls.
     */
    private Boolean canManage;

    /**
     * Constructor for creating TeamResponse with all fields.
     */
    public TeamResponse(UUID id, String name, String description, String teamSlug,
            UUID projectId, String projectSlug, UUID createdBy, String creatorName,
            LocalDateTime createdAt, LocalDateTime updatedAt, Integer memberCount,
            TeamRole currentUserRole, Boolean canManage) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.teamSlug = teamSlug;
        this.projectId = projectId;
        this.projectSlug = projectSlug;
        this.createdBy = createdBy;
        this.creatorName = creatorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.memberCount = memberCount;
        this.currentUserRole = currentUserRole;
        this.canManage = canManage;
    }
}