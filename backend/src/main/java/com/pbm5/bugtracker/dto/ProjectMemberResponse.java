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
 * DTO for project member information in API responses.
 * 
 * Used in:
 * - GET /api/projects/{projectSlug}/members (member listing)
 * - GET /api/projects/{projectSlug} (as part of detailed response)
 * - Membership management interfaces
 * - Approval workflow displays
 * 
 * Contains member-specific information with user details integration.
 * Sensitive information (like approval details) filtered based on requesting
 * user permissions.
 * 
 * User Context Filtering:
 * - Regular members see basic info only
 * - Admins see full audit trail and approval details
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectMemberResponse {

    /**
     * Unique project membership identifier.
     * Used for membership management operations.
     */
    @JsonProperty("id")
    private UUID id;

    /**
     * Project identifier this membership belongs to.
     * Used for context and validation.
     */
    @JsonProperty("projectId")
    private UUID projectId;

    /**
     * User identifier for this membership.
     * Links to User entity for user details.
     */
    @JsonProperty("userId")
    private UUID userId;

    /**
     * User's display name.
     * Integrated from User entity for convenient display.
     */
    @JsonProperty("userName")
    private String userName;

    /**
     * User's first name.
     * From User entity for detailed user information.
     */
    @JsonProperty("firstName")
    private String firstName;

    /**
     * User's last name.
     * From User entity for detailed user information.
     */
    @JsonProperty("lastName")
    private String lastName;

    /**
     * User's email address.
     * Integrated from User entity - may be filtered based on permissions.
     */
    @JsonProperty("userEmail")
    private String userEmail;

    /**
     * Member's role in the project.
     * Values: "ADMIN", "MEMBER"
     * 
     * Used for:
     * - Permission checking in frontend
     * - Role-based UI rendering
     * - Access control decisions
     */
    @JsonProperty("role")
    private String role;

    /**
     * Current membership status.
     * Values: "PENDING", "ACTIVE", "REJECTED"
     * 
     * Used for:
     * - Workflow state display
     * - Admin approval interfaces
     * - Member list filtering
     */
    @JsonProperty("status")
    private String status;

    /**
     * Timestamp when user joined the project (became ACTIVE).
     * Null for pending or rejected memberships.
     * 
     * Used for:
     * - Member tenure display
     * - Activity timeline
     * - Sorting member lists
     */
    @JsonProperty("joinedAt")
    private LocalDateTime joinedAt;

    /**
     * Timestamp when membership was requested.
     * Always present - shows when user first requested to join.
     * 
     * Used for:
     * - Request processing order
     * - Audit trail
     * - Pending request management
     */
    @JsonProperty("requestedAt")
    private LocalDateTime requestedAt;

    /**
     * ID of the user who approved this membership.
     * Null for pending memberships or auto-approved (project creator).
     * 
     * Admin-only information - filtered for regular members.
     * Used for audit trail and approval tracking.
     */
    @JsonProperty("approvedBy")
    private UUID approvedBy;

    /**
     * Name of the user who approved this membership.
     * Integrated from User entity for display purposes.
     * Admin-only information.
     */
    @JsonProperty("approvedByName")
    private String approvedByName;

    /**
     * Timestamp when membership was approved/rejected.
     * Null for pending memberships.
     * 
     * Admin-only information used for:
     * - Audit trail
     * - Approval workflow tracking
     * - Administrative reports
     */
    @JsonProperty("approvedAt")
    private LocalDateTime approvedAt;

    /**
     * Indicates if this member can be removed by the current user.
     * Based on:
     * - Current user is admin
     * - Target member is not the last admin
     * - Current user is not trying to remove themselves without successor
     * 
     * Frontend uses this for conditional UI rendering.
     */
    @JsonProperty("canBeRemoved")
    private Boolean canBeRemoved;

    /**
     * Indicates if this member's role can be changed by the current user.
     * Based on:
     * - Current user is admin
     * - Target member is not the last admin (for demotion)
     * - Role change would not violate business rules
     * 
     * Frontend uses this for role management UI.
     */
    @JsonProperty("canChangeRole")
    private Boolean canChangeRole;
}