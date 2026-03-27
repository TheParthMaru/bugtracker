package com.pbm5.bugtracker.exception;

import java.util.UUID;

/**
 * Exception thrown when trying to remove or demote the last admin of a project.
 * 
 * Business Rule: Every project must have at least one admin.
 * This exception prevents operations that would leave a project without admins.
 * 
 * Common scenarios:
 * - Last admin trying to leave project
 * - Trying to remove the only admin
 * - Demoting the only admin to member role
 * - Admin transferring role but remaining admin count would be zero
 * 
 * Maps to HTTP 400 Bad Request status code.
 */
public class LastAdminException extends RuntimeException {

    private final String projectIdentifier;
    private final UUID adminId;
    private final String operation;

    /**
     * Create exception for last admin violation.
     * 
     * @param projectIdentifier Project ID or slug
     * @param adminId           ID of the admin that can't be removed/demoted
     * @param operation         Operation that was attempted
     */
    public LastAdminException(String projectIdentifier, UUID adminId, String operation) {
        super(String.format(
                "Cannot %s admin %s from project %s: this would leave the project without any administrators. " +
                        "Please promote another member to admin first.",
                operation, adminId, projectIdentifier));
        this.projectIdentifier = projectIdentifier;
        this.adminId = adminId;
        this.operation = operation;
    }

    /**
     * Create exception with custom message.
     * 
     * @param projectIdentifier Project ID or slug
     * @param adminId           ID of the admin
     * @param operation         Operation that was attempted
     * @param message           Custom error message
     */
    public LastAdminException(String projectIdentifier, UUID adminId, String operation, String message) {
        super(message);
        this.projectIdentifier = projectIdentifier;
        this.adminId = adminId;
        this.operation = operation;
    }

    public String getProjectIdentifier() {
        return projectIdentifier;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public String getOperation() {
        return operation;
    }
}