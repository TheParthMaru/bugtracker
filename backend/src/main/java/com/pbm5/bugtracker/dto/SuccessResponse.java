package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic success response DTO for API operations.
 * 
 * Used to provide meaningful success messages instead of empty responses.
 * Contains operation details, timestamps, and optional metadata.
 * 
 * Examples:
 * - Team creation: "Team 'Frontend Developers' created successfully in project
 * 'E-commerce App'"
 * - Member addition: "John Doe added to team 'Backend Team' as Member"
 * - Team deletion: "Team 'QA Team' deleted successfully"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse {

    /**
     * Success message describing the operation.
     * Should be user-friendly and descriptive.
     */
    private String message;

    /**
     * Timestamp when the operation was completed.
     * Useful for audit trails and user feedback.
     */
    private LocalDateTime timestamp;

    /**
     * Type of operation performed.
     * Used for categorization and logging.
     */
    private String operation;

    /**
     * Optional metadata about the operation.
     * Can include IDs, names, counts, etc.
     */
    private Map<String, Object> metadata;

    /**
     * Constructor for simple success responses.
     */
    public SuccessResponse(String message, String operation) {
        this.message = message;
        this.operation = operation;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor for success responses with metadata.
     */
    public SuccessResponse(String message, String operation, Map<String, Object> metadata) {
        this.message = message;
        this.operation = operation;
        this.timestamp = LocalDateTime.now();
        this.metadata = metadata;
    }

    /**
     * Static factory method for team creation success.
     */
    public static SuccessResponse teamCreated(String teamName, String projectName, UUID teamId) {
        return new SuccessResponse(
                String.format("Team '%s' created successfully in project '%s'", teamName, projectName),
                "TEAM_CREATED",
                Map.of(
                        "teamId", teamId,
                        "teamName", teamName,
                        "projectName", projectName));
    }

    /**
     * Static factory method for team update success.
     */
    public static SuccessResponse teamUpdated(String teamName, String projectName) {
        return new SuccessResponse(
                String.format("Team '%s' updated successfully in project '%s'", teamName, projectName),
                "TEAM_UPDATED",
                Map.of(
                        "teamName", teamName,
                        "projectName", projectName));
    }

    /**
     * Static factory method for team deletion success.
     */
    public static SuccessResponse teamDeleted(String teamName, String projectName) {
        return new SuccessResponse(
                String.format("Team '%s' deleted successfully from project '%s'", teamName, projectName),
                "TEAM_DELETED",
                Map.of(
                        "teamName", teamName,
                        "projectName", projectName));
    }

    /**
     * Static factory method for member addition success.
     */
    public static SuccessResponse memberAdded(String memberName, String teamName, String role) {
        return new SuccessResponse(
                String.format("'%s' added to team '%s' as %s", memberName, teamName, role),
                "MEMBER_ADDED",
                Map.of(
                        "memberName", memberName,
                        "teamName", teamName,
                        "role", role));
    }

    /**
     * Static factory method for member removal success.
     */
    public static SuccessResponse memberRemoved(String memberName, String teamName) {
        return new SuccessResponse(
                String.format("'%s' removed from team '%s'", memberName, teamName),
                "MEMBER_REMOVED",
                Map.of(
                        "memberName", memberName,
                        "teamName", teamName));
    }

    /**
     * Static factory method for role update success.
     */
    public static SuccessResponse roleUpdated(String memberName, String teamName, String newRole) {
        return new SuccessResponse(
                String.format("'%s' role updated to %s in team '%s'", memberName, newRole, teamName),
                "ROLE_UPDATED",
                Map.of(
                        "memberName", memberName,
                        "teamName", teamName,
                        "newRole", newRole));
    }
}