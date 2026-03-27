package com.pbm5.bugtracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.Size;

/**
 * DTO for project join requests.
 * 
 * Used in POST /api/projects/{slug}/join endpoint when users request to join
 * projects.
 * Creates a pending membership that requires admin approval.
 * 
 * Simple structure - most information comes from:
 * - User context (from Spring Security)
 * - Project context (from URL path parameter)
 * - System timestamps (auto-generated)
 * 
 * Validation Rules:
 * - Message is optional but has length limit if provided
 * - User must be authenticated (enforced by security layer)
 * - User cannot already be a member or have pending request (enforced by
 * service layer)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JoinProjectRequest {

    /**
     * Optional message from user to project admins.
     * Helps admins understand why user wants to join project.
     * 
     * Examples:
     * - "I'm a backend developer interested in contributing to this project"
     * - "Working on related features, would like access to project resources"
     * - "Team lead asked me to join for the next sprint"
     * 
     * Displayed to admins in approval workflow interface.
     */
    @Size(max = 500, message = "Join request message cannot exceed 500 characters")
    @JsonProperty("message")
    private String message;

    /**
     * User's preferred initial role (optional).
     * Default is "MEMBER" if not specified.
     * 
     * Note: Admins can override this during approval process.
     * This is just a suggestion/preference from the user.
     * 
     * Values: "MEMBER" (regular member), "ADMIN" (if user thinks they should be
     * admin)
     * In practice, most requests should be for MEMBER role.
     */
    @JsonProperty("requestedRole")
    private String requestedRole;
}