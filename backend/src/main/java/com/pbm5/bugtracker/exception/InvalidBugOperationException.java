package com.pbm5.bugtracker.exception;

public class InvalidBugOperationException extends RuntimeException {

    public InvalidBugOperationException(String message) {
        super(message);
    }

    public InvalidBugOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}