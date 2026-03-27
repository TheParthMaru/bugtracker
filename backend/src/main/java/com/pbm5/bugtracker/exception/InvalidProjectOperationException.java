package com.pbm5.bugtracker.exception;

import java.util.UUID;

/**
 * Exception thrown for general project business rule violations.
 * 
 * Used when operation violates business rules but doesn't fit other specific
 * exceptions.
 * Serves as a catch-all for project-related validation failures.
 * 
 * Common scenarios:
 * - Invalid state transitions
 * - Business rule violations during updates
 * - Data consistency issues
 * - Invalid parameter combinations
 * - Workflow validation failures
 * 
 * Maps to HTTP 400 Bad Request status code.
 */
public class InvalidProjectOperationException extends RuntimeException {

    private final String projectIdentifier;
    private final UUID userId;
    private final String operation;
    private final String violatedRule;

    /**
     * Create exception for general business rule violation.
     * 
     * @param projectIdentifier Project ID or slug
     * @param operation         Operation that was attempted
     * @param violatedRule      Description of the business rule that was violated
     */
    public InvalidProjectOperationException(String projectIdentifier, String operation, String violatedRule) {
        super(String.format(
                "Invalid operation '%s' on project %s: %s",
                operation, projectIdentifier, violatedRule));
        this.projectIdentifier = projectIdentifier;
        this.userId = null;
        this.operation = operation;
        this.violatedRule = violatedRule;
    }

    /**
     * Create exception with user context.
     * 
     * @param projectIdentifier Project ID or slug
     * @param userId            User attempting the operation
     * @param operation         Operation that was attempted
     * @param violatedRule      Description of the business rule that was violated
     */
    public InvalidProjectOperationException(String projectIdentifier, UUID userId, String operation,
            String violatedRule) {
        super(String.format(
                "Invalid operation '%s' by user %s on project %s: %s",
                operation, userId, projectIdentifier, violatedRule));
        this.projectIdentifier = projectIdentifier;
        this.userId = userId;
        this.operation = operation;
        this.violatedRule = violatedRule;
    }

    /**
     * Create exception with custom message.
     * 
     * @param projectIdentifier Project ID or slug
     * @param userId            User attempting the operation
     * @param operation         Operation that was attempted
     * @param violatedRule      Description of the business rule
     * @param message           Custom error message
     */
    public InvalidProjectOperationException(String projectIdentifier, UUID userId, String operation,
            String violatedRule, String message) {
        super(message);
        this.projectIdentifier = projectIdentifier;
        this.userId = userId;
        this.operation = operation;
        this.violatedRule = violatedRule;
    }

    /**
     * Create exception for validation failures.
     * 
     * @param message Validation error message
     */
    public static InvalidProjectOperationException validationError(String message) {
        return new InvalidProjectOperationException(null, null, "validation", message, message);
    }

    /**
     * Create exception for state transition failures.
     * 
     * @param projectIdentifier Project ID or slug
     * @param fromState         Current state
     * @param toState           Target state
     * @param reason            Why transition is not allowed
     */
    public static InvalidProjectOperationException stateTransitionError(String projectIdentifier, String fromState,
            String toState, String reason) {
        return new InvalidProjectOperationException(projectIdentifier,
                String.format("state_transition_%s_to_%s", fromState, toState),
                String.format("Cannot transition from %s to %s: %s", fromState, toState, reason));
    }

    public String getProjectIdentifier() {
        return projectIdentifier;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOperation() {
        return operation;
    }

    public String getViolatedRule() {
        return violatedRule;
    }
}