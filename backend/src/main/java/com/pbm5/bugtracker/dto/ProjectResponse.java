package com.pbm5.bugtracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Basic DTO for project information in API responses.
 * 
 * Used in:
 * - GET /api/projects (project listing)
 * - GET /api/projects/{projectSlug} (basic project info)
 * - Project references in other DTOs
 * 
 * Contains essential project information without sensitive details.
 * For detailed project info, use ProjectDetailResponse.
 * 
 * User Context:
 * - userMembershipStatus indicates current user's relationship to project
 * - userRole shows current user's role if they are a member
 * - isUserAdmin indicates if current user can manage this project
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectResponse {

    /**
     * Unique project identifier.
     * Used for internal API calls and database references.
     */
    @JsonProperty("id")
    private UUID id;

    /**
     * Project display name.
     * Shown in project cards, headers, and navigation.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Project description (may be null).
     * Displayed in project cards and detail views.
     */
    @JsonProperty("description")
    private String description;

    /**
     * URL-friendly project identifier.
     * Used in URLs: /projects/{projectSlug}
     * Unique across all projects.
     */
    @JsonProperty("projectSlug")
    private String projectSlug;

    /**
     * ID of the project admin/owner.
     * Reference to User entity - admin has full project control.
     */
    @JsonProperty("adminId")
    private UUID adminId;

    /**
     * First name of the project admin/owner.
     * Used for display purposes in project cards and headers.
     */
    @JsonProperty("adminFirstName")
    private String adminFirstName;

    /**
     * Last name of the project admin/owner.
     * Used for display purposes in project cards and headers.
     */
    @JsonProperty("adminLastName")
    private String adminLastName;

    /**
     * Total number of active members in the project.
     * Excludes pending and rejected membership requests.
     */
    @JsonProperty("memberCount")
    private Long memberCount;

    /**
     * Number of pending membership requests.
     * Only visible to project admins for approval workflow.
     */
    @JsonProperty("pendingRequestCount")
    private Long pendingRequestCount;

    /**
     * Project creation timestamp.
     * Useful for sorting and display purposes.
     */
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    /**
     * Last project update timestamp.
     * Updated when project details or membership changes.
     */
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    /**
     * Current user's membership status in this project.
     * Values: null (not a member), "PENDING", "ACTIVE", "REJECTED"
     * 
     * Used by frontend to show appropriate actions:
     * - null: Show "Join Project" button
     * - PENDING: Show "Request Pending" message
     * - ACTIVE: Show project access and "Leave Project" option
     * - REJECTED: Show "Request Rejected" message
     */
    @JsonProperty("userMembershipStatus")
    private String userMembershipStatus;

    /**
     * Current user's role in this project (if member).
     * Values: null (not a member), "ADMIN", "MEMBER"
     * 
     * Used by frontend for permission-based UI rendering.
     */
    @JsonProperty("userRole")
    private String userRole;

    /**
     * Indicates if current user is an admin of this project.
     * Convenient boolean for frontend permission checking.
     * 
     * True = user can manage project, members, teams
     * False = user has limited or no access
     */
    @JsonProperty("isUserAdmin")
    private Boolean isUserAdmin;
}