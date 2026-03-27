package com.pbm5.bugtracker.exception;

public class AttachmentNotFoundException extends RuntimeException {

    public AttachmentNotFoundException(String message) {
        super(message);
    }

    public AttachmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}