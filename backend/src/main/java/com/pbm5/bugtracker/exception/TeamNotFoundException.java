package com.pbm5.bugtracker.exception;

/**
 * Exception thrown when a requested team is not found.
 */
public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(String message) {
        super(message);
    }

    public TeamNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}