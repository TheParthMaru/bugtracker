package com.pbm5.bugtracker.entity;

/**
 * Enum defining the roles a user can have within a project.
 * This determines what actions a user can perform within the project context.
 * 
 * Role Hierarchy:
 * - ADMIN: Full control over project settings, teams, and members
 * - MEMBER: Basic access to project resources and teams they're assigned to
 * 
 * Database Storage:
 * - Stored as VARCHAR(20) in database
 * - JPA automatically converts enum to string value
 * - Database constraints ensure only valid values are stored
 * 
 * Usage Examples:
 * - Project creator automatically gets ADMIN role
 * - Users who join projects get MEMBER role by default
 * - ADMIN users can promote MEMBER users to ADMIN
 * - At least one ADMIN must exist per project (business rule)
 */
public enum ProjectRole {

    /**
     * Project Administrator Role
     * 
     * Permissions:
     * - Create, update, delete project settings
     * - Approve/reject membership requests
     * - Add/remove project members
     * - Promote/demote user roles (except last admin)
     * - Create and manage teams within project
     * - Delete project (with proper safeguards)
     * 
     * Business Rules:
     * - Project creator automatically becomes ADMIN
     * - Cannot remove the last ADMIN from a project
     * - Can transfer admin rights to other members
     */
    ADMIN("Project Administrator"),

    /**
     * Project Member Role
     * 
     * Permissions:
     * - View project details and member list
     * - Join teams within the project (if approved by team admin)
     * - Access project resources based on team memberships
     * - Leave the project voluntarily
     * 
     * Limitations:
     * - Cannot modify project settings
     * - Cannot manage other members
     * - Cannot create new teams (unless promoted)
     */
    MEMBER("Project Member"),

    /**
     * Pending Approval Role
     * 
     * This role indicates a user has requested to join but hasn't been approved
     * yet.
     * Users with this role have no access to the project until approved.
     * 
     * Permissions:
     * - None (awaiting approval)
     * 
     * Business Rules:
     * - Automatically assigned when user requests to join
     * - Changed to MEMBER or ADMIN after approval
     * - Cannot access project resources
     */
    PENDING("Pending Approval");

    // Human-readable description of the role
    private final String description;

    /**
     * Constructor for enum values
     * 
     * @param description Human-readable description of the role
     */
    ProjectRole(String description) {
        this.description = description;
    }

    /**
     * Get the human-readable description of this role
     * 
     * @return Role description suitable for UI display
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this role has administrative privileges
     * 
     * @return true if this role can perform admin actions
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * Check if this role can manage project members
     * Used for permission checks in service layer
     * 
     * @return true if this role can add/remove members
     */
    public boolean canManageMembers() {
        return this == ADMIN;
    }

    /**
     * Check if this role can modify project settings
     * Used for permission checks in service layer
     * 
     * @return true if this role can update project details
     */
    public boolean canModifyProject() {
        return this == ADMIN;
    }

    /**
     * Get the default role assigned to new project members
     * Used when users join a project
     * 
     * @return Default role for new members
     */
    public static ProjectRole getDefaultRole() {
        return MEMBER;
    }

    /**
     * Convert string value to enum (case-insensitive)
     * Used for parsing request parameters and form inputs
     * 
     * @param value String representation of role
     * @return ProjectRole enum value
     * @throws IllegalArgumentException if value is not a valid role
     */
    public static ProjectRole fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Role value cannot be null or empty");
        }

        try {
            return ProjectRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid project role: " + value +
                    ". Valid roles are: ADMIN, MEMBER");
        }
    }
}