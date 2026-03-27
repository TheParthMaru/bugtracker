package com.pbm5.bugtracker.exception;

public class BugNotFoundException extends RuntimeException {

    public BugNotFoundException(String message) {
        super(message);
    }

    public BugNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}