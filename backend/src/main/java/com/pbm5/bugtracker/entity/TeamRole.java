package com.pbm5.bugtracker.entity;

/**
 * Enum defining the roles a user can have within a team.
 * This determines what actions a user can perform within the team context.
 * 
 * Role Hierarchy:
 * - ADMIN: Full control over team settings and members
 * - MEMBER: Basic access to team resources
 * 
 * Database Storage:
 * - Stored as VARCHAR(20) in database
 * - JPA automatically converts enum to string value
 * - Database constraints ensure only valid values are stored
 */
public enum TeamRole {
    ADMIN("Team Administrator"),
    MEMBER("Team Member");

    private final String displayName;

    TeamRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}