package com.pbm5.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a team within a project.
 * 
 * Used in:
 * - POST /api/projects/{projectSlug}/teams
 * 
 * Business Rules:
 * - Only project admins can create teams
 * - Team names must be unique within the project scope
 * - Team slugs are auto-generated with project prefix
 */
@Data
public class CreateTeamRequest {

    /**
     * Team name (required).
     * Must be between 2-100 characters.
     * Must be unique within the project scope.
     */
    @NotBlank(message = "Team name is required")
    @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    private String name;

    /**
     * Team description (optional).
     * Maximum 500 characters.
     */
    @Size(max = 500, message = "Team description cannot exceed 500 characters")
    private String description;
}