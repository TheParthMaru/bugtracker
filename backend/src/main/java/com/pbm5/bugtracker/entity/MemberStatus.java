package com.pbm5.bugtracker.entity;

/**
 * Enum defining the status of a user's membership in a project.
 * This tracks the approval workflow from request to active participation.
 * 
 * Membership Workflow:
 * 1. User requests to join project → PENDING
 * 2. Admin approves request → ACTIVE
 * 2. Admin rejects request → REJECTED
 * 
 * Database Storage:
 * - Stored as VARCHAR(20) in database
 * - JPA automatically converts enum to string value
 * - Database constraints ensure only valid statuses are stored
 * 
 * Business Rules:
 * - Only ACTIVE members can participate in project activities
 * - PENDING requests require admin approval
 * - REJECTED requests can be re-submitted (new PENDING request)
 * - Status transitions are logged with timestamps and approver info
 */
public enum MemberStatus {

    /**
     * Pending Approval Status
     * 
     * Description:
     * - User has requested to join the project
     * - Waiting for admin approval or rejection
     * - User cannot access project resources yet
     * 
     * Database State:
     * - joined_at: NULL
     * - approved_by: NULL
     * - approved_at: NULL
     * - requested_at: Set to current timestamp
     * 
     * UI Display:
     * - Show "Waiting for approval" message to user
     * - Show approval buttons to project admins
     * - Highlight in admin dashboard as action required
     */
    PENDING("Pending Approval"),

    /**
     * Active Member Status
     * 
     * Description:
     * - User is an approved, active member of the project
     * - Can access project resources based on their role
     * - Can participate in teams within the project
     * 
     * Database State:
     * - joined_at: Set to approval timestamp
     * - approved_by: Set to admin who approved
     * - approved_at: Set to approval timestamp
     * - requested_at: Original request timestamp (preserved)
     * 
     * UI Display:
     * - Full access to project features
     * - Show member since date
     * - Display in active members list
     */
    ACTIVE("Active Member"),

    /**
     * Rejected Request Status
     * 
     * Description:
     * - Admin has rejected the user's join request
     * - User cannot access project resources
     * - User can submit a new request (creates new PENDING record)
     * 
     * Database State:
     * - joined_at: NULL
     * - approved_by: Set to admin who rejected
     * - approved_at: Set to rejection timestamp
     * - requested_at: Original request timestamp (preserved)
     * 
     * UI Display:
     * - Show "Request rejected" message
     * - Allow user to request again
     * - Show rejection info to admins for audit
     */
    REJECTED("Request Rejected");

    // Human-readable description of the status
    private final String description;

    /**
     * Constructor for enum values
     * 
     * @param description Human-readable description of the status
     */
    MemberStatus(String description) {
        this.description = description;
    }

    /**
     * Get the human-readable description of this status
     * 
     * @return Status description suitable for UI display
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this status allows project access
     * Used for permission checks throughout the application
     * 
     * @return true if user can access project resources
     */
    public boolean canAccessProject() {
        return this == ACTIVE;
    }

    /**
     * Check if this status is waiting for admin action
     * Used to highlight pending requests in admin dashboards
     * 
     * @return true if admin action is required
     */
    public boolean requiresAdminAction() {
        return this == PENDING;
    }

    /**
     * Check if this status is a final decision
     * Used to determine if status can be changed
     * 
     * @return true if this is a final status (ACTIVE or REJECTED)
     */
    public boolean isFinalDecision() {
        return this == ACTIVE || this == REJECTED;
    }

    /**
     * Check if user can submit a new join request
     * REJECTED users can re-request, PENDING users cannot duplicate
     * 
     * @return true if new request is allowed
     */
    public boolean canSubmitNewRequest() {
        return this == REJECTED;
    }

    /**
     * Get the default status for new membership requests
     * Used when users first request to join a project
     * 
     * @return Default status for new requests
     */
    public static MemberStatus getDefaultStatus() {
        return PENDING;
    }

    /**
     * Get valid statuses that can be transitioned to from current status
     * Used for validation in service layer
     * 
     * @param currentStatus Current membership status
     * @return Array of valid next statuses
     */
    public static MemberStatus[] getValidTransitions(MemberStatus currentStatus) {
        switch (currentStatus) {
            case PENDING:
                // Pending requests can be approved or rejected
                return new MemberStatus[] { ACTIVE, REJECTED };
            case ACTIVE:
                // Active members can only be removed (handled separately)
                return new MemberStatus[] {};
            case REJECTED:
                // Rejected requests cannot be directly changed
                // User must submit new request
                return new MemberStatus[] {};
            default:
                return new MemberStatus[] {};
        }
    }

    /**
     * Convert string value to enum (case-insensitive)
     * Used for parsing request parameters and form inputs
     * 
     * @param value String representation of status
     * @return MemberStatus enum value
     * @throws IllegalArgumentException if value is not a valid status
     */
    public static MemberStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Status value cannot be null or empty");
        }

        try {
            return MemberStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid member status: " + value +
                    ". Valid statuses are: PENDING, ACTIVE, REJECTED");
        }
    }

    /**
     * Validate if a status transition is allowed
     * Used in service layer before updating member status
     * 
     * @param from Current status
     * @param to   Target status
     * @return true if transition is valid
     */
    public static boolean isValidTransition(MemberStatus from, MemberStatus to) {
        if (from == null || to == null) {
            return false;
        }

        MemberStatus[] validTransitions = getValidTransitions(from);
        for (MemberStatus validStatus : validTransitions) {
            if (validStatus == to) {
                return true;
            }
        }
        return false;
    }
}