package com.pbm5.bugtracker.exception;

/**
 * Exception thrown when a requested project cannot be found.
 * 
 * Common scenarios:
 * - GET /api/projects/{slug} with non-existent slug
 * - Operations on soft-deleted projects
 * - Invalid project ID references
 * 
 * Maps to HTTP 404 Not Found status code.
 */
public class ProjectNotFoundException extends RuntimeException {

    private final String projectIdentifier;

    /**
     * Create exception with project identifier.
     * 
     * @param projectIdentifier The project ID or slug that was not found
     */
    public ProjectNotFoundException(String projectIdentifier) {
        super(String.format("Project not found: %s", projectIdentifier));
        this.projectIdentifier = projectIdentifier;
    }

    /**
     * Create exception with project identifier and custom message.
     * 
     * @param projectIdentifier The project ID or slug that was not found
     * @param message           Custom error message
     */
    public ProjectNotFoundException(String projectIdentifier, String message) {
        super(message);
        this.projectIdentifier = projectIdentifier;
    }

    /**
     * Get the project identifier that was not found.
     * Used for error response details and logging.
     */
    public String getProjectIdentifier() {
        return projectIdentifier;
    }
}