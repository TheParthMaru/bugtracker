package com.pbm5.bugtracker.exception;

import java.util.UUID;

/**
 * Exception thrown when a user lacks sufficient permissions for project
 * operation.
 *
 * Common scenarios:
 * - Non-admin trying to update project details
 * - Non-member trying to access member-only information
 * - Trying to delete project without admin rights
 * - Attempting to manage members without admin role
 *
 * Maps to HTTP 403 Forbidden status code.
 */
public class ProjectAccessDeniedException extends RuntimeException {

    private final String projectIdentifier;
    private final UUID userId;
    private final String operation;

    /**
     * Create exception with basic information.
     *
     * @param projectIdentifier Project ID or slug
     * @param userId            User attempting the operation
     * @param operation         Operation that was denied
     */
    public ProjectAccessDeniedException(String projectIdentifier, UUID userId, String operation) {
        super(String.format("Access denied for user %s to perform '%s' on project %s",
                userId, operation, projectIdentifier));
        this.projectIdentifier = projectIdentifier;
        this.userId = userId;
        this.operation = operation;
    }

    /**
     * Create exception with custom message.
     *
     * @param projectIdentifier Project ID or slug
     * @param userId            User attempting the operation
     * @param operation         Operation that was denied
     * @param message           Custom error message
     */
    public ProjectAccessDeniedException(String projectIdentifier, UUID userId, String operation, String message) {
        super(String.format("Access denied for user %s to perform '%s' on project %s: %s",
                userId, operation, projectIdentifier, message));
        this.projectIdentifier = projectIdentifier;
        this.userId = userId;
        this.operation = operation;
    }

    /**
     * Create exception with simple message (for bug linking feature).
     *
     * @param message Custom error message
     */
    public ProjectAccessDeniedException(String message) {
        super(message);
        this.projectIdentifier = null;
        this.userId = null;
        this.operation = null;
    }

    /**
     * Create exception with simple message and cause (for bug linking feature).
     *
     * @param message Custom error message
     * @param cause   Cause of the exception
     */
    public ProjectAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
        this.projectIdentifier = null;
        this.userId = null;
        this.operation = null;
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