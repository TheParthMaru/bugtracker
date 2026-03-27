package com.pbm5.bugtracker.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for gamification-related errors
 */
public class GamificationException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public GamificationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public GamificationException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

