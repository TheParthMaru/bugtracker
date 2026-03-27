package com.pbm5.bugtracker.exception;

/**
 * Exception thrown when attempting to create/update project with existing name.
 * 
 * Common scenarios:
 * - Creating new project with name that already exists
 * - Updating project name to one that's already taken
 * - Database uniqueness constraint violations for project names
 * 
 * Maps to HTTP 409 Conflict status code.
 */
public class ProjectNameConflictException extends RuntimeException {

    private final String projectName;
    private final String conflictType;

    /**
     * Create exception for name conflict.
     * 
     * @param projectName The conflicting project name
     */
    public ProjectNameConflictException(String projectName) {
        super(String.format("Project name '%s' already exists. Please choose a different name.", projectName));
        this.projectName = projectName;
        this.conflictType = "NAME";
    }

    /**
     * Private constructor for internal use.
     * 
     * @param projectName  The conflicting project name
     * @param message      Custom error message
     * @param conflictType Type of conflict
     */
    private ProjectNameConflictException(String projectName, String message, String conflictType) {
        super(message);
        this.projectName = projectName;
        this.conflictType = conflictType;
    }

    /**
     * Create exception for slug conflict.
     * 
     * @param slug The conflicting project slug
     * @return ProjectNameConflictException configured for slug conflict
     */
    public static ProjectNameConflictException forSlugConflict(String projectSlug) {
        return new ProjectNameConflictException(projectSlug,
                String.format("Project slug '%s' already exists. Please choose a different name.", projectSlug),
                "projectSlug");
    }

    public String getProjectName() {
        return projectName;
    }

    public String getConflictType() {
        return conflictType;
    }

    /**
     * Check if this is a slug conflict specifically.
     */
    public boolean isSlugConflict() {
        return "projectSlug".equals(conflictType);
    }
}