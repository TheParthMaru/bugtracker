package com.pbm5.bugtracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating existing projects.
 * 
 * Used in PUT /api/projects/{projectSlug} endpoint for project updates.
 * Only project admins can update project details.
 * 
 * Partial Update Support:
 * - All fields are optional (nullable)
 * - Only provided fields will be updated
 * - Null fields are ignored during update
 * 
 * Validation Rules:
 * - Name: If provided, 3-100 characters and must be unique
 * - Description: If provided, max 2000 characters
 * - Admin operations only (enforced in service layer)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateProjectRequest {

    /**
     * Updated project name (optional).
     * If provided, must be unique across all projects.
     * Will trigger slug regeneration if changed.
     * 
     * Null = don't update, String = update to new value
     */
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    @JsonProperty("name")
    private String name;

    /**
     * Updated project description (optional).
     * Can be set to empty string to remove description.
     * 
     * Null = don't update, String (including "") = update to new value
     */
    @Size(max = 2000, message = "Project description cannot exceed 2000 characters")
    @JsonProperty("description")
    private String description;

    /**
     * Updated custom project slug (optional).
     * If provided, must be unique and follow project slug format rules.
     * Use with caution as changing project slug breaks existing URLs.
     * 
     * Note: Project slug changes should be rare in production to avoid broken links
     */
    @Size(min = 3, max = 120, message = "Custom project slug must be between 3 and 120 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Project slug must be lowercase alphanumeric with hyphens (e.g., 'my-project')")
    @JsonProperty("projectSlug")
    private String projectSlug;
}