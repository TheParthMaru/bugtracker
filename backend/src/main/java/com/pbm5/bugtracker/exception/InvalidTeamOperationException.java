package com.pbm5.bugtracker.exception;

/**
 * Exception thrown when an invalid operation is attempted on a team.
 * This covers general business logic violations that don't fit into more
 * specific exceptions.
 */
public class InvalidTeamOperationException extends RuntimeException {

    public InvalidTeamOperationException(String message) {
        super(message);
    }

    public InvalidTeamOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}