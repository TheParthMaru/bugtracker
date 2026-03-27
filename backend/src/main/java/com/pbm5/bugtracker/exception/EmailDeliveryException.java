package com.pbm5.bugtracker.exception;

/**
 * Exception thrown when email delivery fails.
 * 
 * This exception is used to handle email delivery failures including:
 * - Resend API failures
 * - Network connectivity issues
 * - Invalid email addresses
 * - Rate limiting
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
