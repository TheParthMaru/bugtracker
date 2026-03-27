package com.pbm5.bugtracker.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when there's an error in point calculation
 */
public class PointCalculationException extends GamificationException {

    public PointCalculationException(String message) {
        super(message, "POINT_CALCULATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    public PointCalculationException(String message, Throwable cause) {
        super(message, "POINT_CALCULATION_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}

