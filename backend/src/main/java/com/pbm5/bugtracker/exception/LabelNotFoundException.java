package com.pbm5.bugtracker.exception;

public class LabelNotFoundException extends RuntimeException {

    public LabelNotFoundException(String message) {
        super(message);
    }

    public LabelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}