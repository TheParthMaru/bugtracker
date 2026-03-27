package com.pbm5.bugtracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detailed DTO for comprehensive project information.
 * 
 * Extends ProjectResponse with additional details for:
 * - GET /api/projects/{projectSlug} (detailed project view)
 * - Project management pages
 * - Admin dashboards
 * 
 * Contains sensitive information that should only be visible to:
 * - Project members (for members list)
 * - Project admins (for pending requests and detailed stats)
 * 
 * Security Note:
 * - Member details filtered based on requesting user's permissions
 * - Admin-only information excluded for regular members
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class ProjectDetailResponse extends ProjectResponse {

    /**
     * Constructor for creating ProjectDetailResponse with all fields.
     */
    public ProjectDetailResponse(UUID id, String name, String description, String projectSlug,
            UUID adminId, String adminFirstName, String adminLastName, Long memberCount,
            Long pendingRequestCount, LocalDateTime createdAt, LocalDateTime updatedAt,
            String userMembershipStatus, String userRole, Boolean isUserAdmin,
            List<ProjectMemberResponse> members, List<ProjectMemberResponse> pendingRequests,
            ProjectStatistics statistics) {
        super(id, name, description, projectSlug, adminId, adminFirstName, adminLastName,
                memberCount, pendingRequestCount, createdAt, updatedAt, userMembershipStatus, userRole, isUserAdmin);
        this.members = members;
        this.pendingRequests = pendingRequests;
        this.statistics = statistics;
    }

    /**
     * List of active project members with their roles.
     * Only visible to project members.
     * Excludes pending and rejected requests.
     * 
     * Used for:
     * - Member list display on project pages
     * - Role management interface (admins only)
     * - Team assignment workflows
     */
    @JsonProperty("members")
    private List<ProjectMemberResponse> members;

    /**
     * List of pending membership requests.
     * Only visible to project admins.
     * Used for approval workflow management.
     * 
     * Contains users waiting for admin approval to join project.
     */
    @JsonProperty("pendingRequests")
    private List<ProjectMemberResponse> pendingRequests;

    /**
     * Detailed statistics about project membership.
     * Visible to all project members, detailed info to admins only.
     * 
     * Used for:
     * - Project dashboard widgets
     * - Analytics and reporting
     * - Admin decision making
     */
    @JsonProperty("statistics")
    private ProjectStatistics statistics;

    /**
     * Nested class for project statistics.
     * Provides aggregated data about project activity and membership.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class ProjectStatistics {

        /**
         * Total number of active members.
         * Excludes pending and rejected requests.
         */
        @JsonProperty("activeMemberCount")
        private Long activeMemberCount;

        /**
         * Number of project admins.
         * Important for last-admin protection logic.
         */
        @JsonProperty("adminCount")
        private Long adminCount;

        /**
         * Number of regular members (non-admin).
         * Used for role distribution analysis.
         */
        @JsonProperty("memberCount")
        private Long memberCount;

        /**
         * Number of pending membership requests.
         * Only visible to admins - helps with approval workflow.
         */
        @JsonProperty("pendingRequestCount")
        private Long pendingRequestCount;

        /**
         * Number of rejected membership requests.
         * Administrative data for tracking and potential re-evaluation.
         */
        @JsonProperty("rejectedRequestCount")
        private Long rejectedRequestCount;

        /**
         * Total number of teams in this project.
         * Future integration with teams module.
         * Currently will be 0 until teams are linked to projects.
         */
        @JsonProperty("teamCount")
        private Long teamCount;
    }
}