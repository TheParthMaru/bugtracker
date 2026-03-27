package com.pbm5.bugtracker.exception;

/**
 * Exception thrown when a team name or slug already exists, causing a conflict.
 */
public class TeamNameConflictException extends RuntimeException {

    public TeamNameConflictException(String message) {
        super(message);
    }

    public TeamNameConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}