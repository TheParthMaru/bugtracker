package com.pbm5.bugtracker.exception;

public class BugAccessDeniedException extends RuntimeException {

    public BugAccessDeniedException(String message) {
        super(message);
    }

    public BugAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}