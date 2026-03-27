package com.pbm5.bugtracker.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when there's an error in streak validation
 */
public class StreakValidationException extends GamificationException {

    public StreakValidationException(String message) {
        super(message, "STREAK_VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    public StreakValidationException(String message, Throwable cause) {
        super(message, "STREAK_VALIDATION_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}

