package com.pbm5.bugtracker.exception;

/**
 * Exception thrown when a user does not have permission to access a team or
 * perform an operation.
 */
public class TeamAccessDeniedException extends RuntimeException {

    public TeamAccessDeniedException(String message) {
        super(message);
    }

    public TeamAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}