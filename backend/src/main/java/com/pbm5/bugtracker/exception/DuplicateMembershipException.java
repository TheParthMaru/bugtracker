package com.pbm5.bugtracker.exception;

import java.util.UUID;

/**
 * Exception thrown when user attempts to join project they're already
 * associated with.
 * 
 * Common scenarios:
 * - User trying to join project they're already a member of
 * - User trying to join project with pending request
 * - User trying to join project after being rejected (without admin clearing
 * status)
 * - Duplicate membership creation attempts
 * 
 * Maps to HTTP 409 Conflict status code.
 */
public class DuplicateMembershipException extends RuntimeException {

    private final String projectIdentifier;
    private final UUID userId;
    private final String currentStatus;

    /**
     * Create exception for duplicate membership.
     * 
     * @param projectIdentifier Project ID or slug
     * @param userId            User attempting to join
     * @param currentStatus     Current membership status (ACTIVE, PENDING,
     *                          REJECTED)
     */
    public DuplicateMembershipException(String projectIdentifier, UUID userId, String currentStatus) {
        super(String.format(
                "User %s already has %s membership in project %s. %s",
                userId, currentStatus.toLowerCase(), projectIdentifier,
                getStatusMessage(currentStatus)));
        this.projectIdentifier = projectIdentifier;
        this.userId = userId;
        this.currentStatus = currentStatus;
    }

    /**
     * Create exception with custom message.
     * 
     * @param projectIdentifier Project ID or slug
     * @param userId            User attempting to join
     * @param currentStatus     Current membership status
     * @param message           Custom error message
     */
    public DuplicateMembershipException(String projectIdentifier, UUID userId, String currentStatus, String message) {
        super(message);
        this.projectIdentifier = projectIdentifier;
        this.userId = userId;
        this.currentStatus = currentStatus;
    }

    /**
     * Get appropriate message based on membership status.
     */
    private static String getStatusMessage(String status) {
        return switch (status) {
            case "ACTIVE" -> "Cannot join again.";
            case "PENDING" -> "Please wait for admin approval.";
            case "REJECTED" -> "Contact admin to reconsider your membership.";
            default -> "Cannot create duplicate membership.";
        };
    }

    public String getProjectIdentifier() {
        return projectIdentifier;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Check if this is an active membership duplicate.
     */
    public boolean isActiveMember() {
        return "ACTIVE".equals(currentStatus);
    }

    /**
     * Check if this is a pending request duplicate.
     */
    public boolean isPendingRequest() {
        return "PENDING".equals(currentStatus);
    }
}