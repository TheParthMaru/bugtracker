package com.pbm5.bugtracker.exception;

/**
 * Exception thrown when a requested team member is not found in a team.
 */
public class TeamMemberNotFoundException extends RuntimeException {

    public TeamMemberNotFoundException(String message) {
        super(message);
    }

    public TeamMemberNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}