package com.pbm5.bugtracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating new projects.
 * 
 * Used in POST /api/projects endpoint to capture project creation data.
 * The creator automatically becomes the project admin.
 * 
 * Validation Rules:
 * - Name: Required, 3-100 characters, will be used for slug generation
 * - Description: Optional, max 2000 characters
 * - Slug: Optional, will be auto-generated from name if not provided
 *
 * Business Rules:
 * - Project name must be unique across the system
 * - Slug must be unique and URL-friendly
 * - Creator becomes admin automatically (handled in service layer)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreateProjectRequest {

    /**
     * Project name - displayed to users and used for slug generation.
     * Must be unique across all projects in the system.
     * 
     * Examples: "Bug Tracker Development", "Mobile App Project"
     */
    @NotBlank(message = "Project name is required and cannot be empty")
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    @JsonProperty("name")
    private String name;

    /**
     * Optional project description providing additional context.
     * Displayed on project cards and detail pages.
     * 
     * Examples: "Main development project for the bug tracking application"
     */
    @Size(max = 2000, message = "Project description cannot exceed 2000 characters")
    @JsonProperty("description")
    private String description;

    /**
     * Optional custom project slug for URL generation.
     * If not provided, will be auto-generated from project name.
     * Must be lowercase, alphanumeric with hyphens only.
     * 
     * Format: "my-project-name" (lowercase, hyphens as separators)
     * Used in URLs like: /projects/my-project-name
     */
    @Size(min = 3, max = 120, message = "Custom project slug must be between 3 and 120 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Project slug must be lowercase alphanumeric with hyphens (e.g., 'my-project')")
    @JsonProperty("projectSlug")
    private String projectSlug;
}