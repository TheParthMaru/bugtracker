package com.pbm5.bugtracker.exception;

import java.util.UUID;

/**
 * Exception thrown when attempting operations on non-existent project members.
 * 
 * Common scenarios:
 * - Trying to update role of user who isn't a project member
 * - Attempting to remove user who isn't in the project
 * - Approving/rejecting non-existent join request
 * - Operations on soft-deleted memberships
 * 
 * Maps to HTTP 404 Not Found status code.
 */
public class ProjectMemberNotFoundException extends RuntimeException {

    private final String projectIdentifier;
    private final UUID userId;
    private final String operation;

    /**
     * Create exception for missing project member.
     * 
     * @param projectIdentifier Project ID or slug
     * @param userId            User ID that wasn't found as member
     * @param operation         Operation that was attempted
     */
    public ProjectMemberNotFoundException(String projectIdentifier, UUID userId, String operation) {
        super(String.format(
                "User %s is not a member of project %s. Cannot perform operation: %s",
                userId, projectIdentifier, operation));
        this.projectIdentifier = projectIdentifier;
        this.userId = userId;
        this.operation = operation;
    }

    /**
     * Create exception with custom message.
     * 
     * @param projectIdentifier Project ID or slug
     * @param userId            User ID
     * @param operation         Operation that was attempted
     * @param message           Custom error message
     */
    public ProjectMemberNotFoundException(String projectIdentifier, UUID userId, String operation, String message) {
        super(message);
        this.projectIdentifier = projectIdentifier;
        this.userId = userId;
        this.operation = operation;
    }

    /**
     * Create exception for membership ID lookup.
     * 
     * @param membershipId Membership ID that wasn't found
     */
    public static ProjectMemberNotFoundException forMembershipId(UUID membershipId) {
        return new ProjectMemberNotFoundException("unknown", null, "lookup",
                String.format("Project membership not found: %s", membershipId));
    }

    public String getProjectIdentifier() {
        return projectIdentifier;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOperation() {
        return operation;
    }
}