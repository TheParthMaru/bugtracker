package com.pbm5.bugtracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.pbm5.bugtracker.entity.Role;
import com.pbm5.bugtracker.exception.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for the entire application.
 * 
 * Handles:
 * - Validation errors (@Valid annotations)
 * - Enum conversion errors (invalid Role values)
 * - JSON parsing errors
 * - Team-specific business logic exceptions
 * - Other common Spring Boot exceptions
 * 
 * Provides structured error responses with:
 * - Consistent error format
 * - Correlation IDs for tracking
 * - Environment-based stack trace handling
 * - Internationalization support
 * - Detailed logging
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Autowired(required = false)
    private MessageSource messageSource;

    /**
     * Build a consistent error response with correlation ID and optional stack
     * trace
     */
    private Map<String, Object> buildErrorResponse(HttpStatus status, String message, String code, Exception ex) {
        String correlationId = UUID.randomUUID().toString();

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("code", code);
        response.put("correlationId", correlationId);

        // Add stack trace in development environments
        if (isDevelopmentEnvironment() && ex != null) {
            response.put("stackTrace", getStackTraceAsString(ex));
        }

        // Log with correlation ID
        logger.warn("Error [{}] - {}: {}", correlationId, code, message);

        return response;
    }

    /**
     * Check if running in development environment
     */
    private boolean isDevelopmentEnvironment() {
        return "dev".equalsIgnoreCase(activeProfile) ||
                "development".equalsIgnoreCase(activeProfile) ||
                "local".equalsIgnoreCase(activeProfile);
    }

    /**
     * Convert exception stack trace to string
     */
    private String getStackTraceAsString(Exception ex) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Get localized message with fallback
     */
    private String getLocalizedMessage(String messageKey, String defaultMessage, Object... args) {
        try {
            if (messageSource != null) {
                return messageSource.getMessage(messageKey, args, LocaleContextHolder.getLocale());
            }
        } catch (Exception e) {
            logger.debug("Failed to get localized message for key: {}", messageKey);
        }
        return defaultMessage;
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        logger.warn("Validation failed: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            if (error instanceof FieldError) {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                fieldErrors.put(fieldName, errorMessage);
                logger.debug("Field validation error - {}: {}", fieldName, errorMessage);
            } else {
                // Handle class-level errors (like @PasswordsMatch)
                fieldErrors.put("general", error.getDefaultMessage());
                logger.debug("Class-level validation error: {}", error.getDefaultMessage());
            }
        });

        response.put("status", "error");
        response.put("message", "Validation failed");
        response.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle JSON parsing errors, especially enum conversion errors
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParsingException(HttpMessageNotReadableException ex) {
        logger.error("JSON parsing error: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();

        // Check if it's an enum conversion error
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException invalidFormatEx = (InvalidFormatException) ex.getCause();

            // Check if it's a Role enum error
            if (invalidFormatEx.getTargetType() != null &&
                    invalidFormatEx.getTargetType().equals(Role.class)) {

                String invalidValue = invalidFormatEx.getValue().toString();
                String validRoles = Arrays.stream(Role.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));

                logger.warn("Invalid role value '{}' provided. Valid roles are: {}", invalidValue, validRoles);

                Map<String, String> errors = new HashMap<>();
                errors.put("role", String.format("Invalid role '%s'. Valid roles are: %s", invalidValue, validRoles));

                response.put("status", "error");
                response.put("message", "Invalid role provided");
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }
        }

        // Generic JSON parsing error
        logger.warn("Generic JSON parsing error: {}", ex.getMessage());
        response.put("status", "error");
        response.put("message", "Invalid request format. Please check your JSON structure.");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle team not found exceptions
     */
    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTeamNotFoundException(TeamNotFoundException ex) {
        String message = getLocalizedMessage("team.not.found", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.NOT_FOUND, message, "TEAM_NOT_FOUND", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle team access denied exceptions
     */
    @ExceptionHandler(TeamAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleTeamAccessDeniedException(TeamAccessDeniedException ex) {
        String message = getLocalizedMessage("team.access.denied", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.FORBIDDEN, message, "TEAM_ACCESS_DENIED", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle team member not found exceptions
     */
    @ExceptionHandler(TeamMemberNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTeamMemberNotFoundException(TeamMemberNotFoundException ex) {
        String message = getLocalizedMessage("team.member.not.found", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.NOT_FOUND, message, "TEAM_MEMBER_NOT_FOUND", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle team name conflict exceptions
     */
    @ExceptionHandler(TeamNameConflictException.class)
    public ResponseEntity<Map<String, Object>> handleTeamNameConflictException(TeamNameConflictException ex) {
        String message = getLocalizedMessage("team.name.conflict", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.CONFLICT, message, "TEAM_NAME_CONFLICT", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle last admin exceptions
     */
    @ExceptionHandler(LastAdminException.class)
    public ResponseEntity<Map<String, Object>> handleLastAdminException(LastAdminException ex) {
        String message = getLocalizedMessage("team.last.admin.operation.denied", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST, message,
                "LAST_ADMIN_OPERATION_DENIED", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle invalid team operation exceptions
     */
    @ExceptionHandler(InvalidTeamOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTeamOperationException(InvalidTeamOperationException ex) {
        String message = getLocalizedMessage("team.invalid.operation", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST, message, "INVALID_TEAM_OPERATION",
                ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle Spring Security access denied exceptions (@PreAuthorize failures)
     * This fixes the critical bug where @PreAuthorize failures were returning HTTP
     * 500
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        String message = getLocalizedMessage("access.denied",
                "Access denied. You do not have permission to perform this operation.");
        Map<String, Object> response = buildErrorResponse(HttpStatus.FORBIDDEN, message, "ACCESS_DENIED", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle Spring Security authentication exceptions
     * This fixes the bug where authentication failures were returning HTTP 403
     * instead of 401
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        String message = getLocalizedMessage("authentication.failed",
                "Authentication failed. Please check your credentials.");
        Map<String, Object> response = buildErrorResponse(HttpStatus.UNAUTHORIZED, message, "AUTHENTICATION_FAILED",
                ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle user not found exceptions
     * This fixes the bug where user validation failures were returning HTTP 500
     * instead of 404
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex) {
        String message = getLocalizedMessage("user.not.found", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.NOT_FOUND, message, "USER_NOT_FOUND", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle method argument type mismatch exceptions (e.g., invalid UUID format)
     * This fixes the bug where invalid path parameters were returning HTTP 500
     * instead of 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String parameterName = ex.getName();
        Object value = ex.getValue();
        String parameterValue = value != null ? value.toString() : "null";
        Class<?> requiredType = ex.getRequiredType();
        String expectedType = requiredType != null ? requiredType.getSimpleName() : "unknown";

        String message = String.format("Invalid format for parameter '%s': '%s'. Expected format: %s",
                parameterName, parameterValue, expectedType);

        Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST, message, "INVALID_PARAMETER_FORMAT",
                ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ===============================
    // PROJECT EXCEPTION HANDLERS
    // ===============================

    /**
     * Handle project not found exceptions
     */
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProjectNotFoundException(ProjectNotFoundException ex) {
        String message = getLocalizedMessage("project.not.found", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.NOT_FOUND, message, "PROJECT_NOT_FOUND", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle project access denied exceptions
     */
    @ExceptionHandler(ProjectAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleProjectAccessDeniedException(ProjectAccessDeniedException ex) {
        String message = getLocalizedMessage("project.access.denied", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.FORBIDDEN, message, "PROJECT_ACCESS_DENIED", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle project name conflict exceptions
     */
    @ExceptionHandler(ProjectNameConflictException.class)
    public ResponseEntity<Map<String, Object>> handleProjectNameConflictException(ProjectNameConflictException ex) {
        String message = getLocalizedMessage("project.name.conflict", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.CONFLICT, message, "PROJECT_NAME_CONFLICT", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle project member not found exceptions
     */
    @ExceptionHandler(ProjectMemberNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProjectMemberNotFoundException(ProjectMemberNotFoundException ex) {
        String message = getLocalizedMessage("project.member.not.found", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.NOT_FOUND, message, "PROJECT_MEMBER_NOT_FOUND",
                ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle invalid project operation exceptions
     */
    @ExceptionHandler(InvalidProjectOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidProjectOperationException(
            InvalidProjectOperationException ex) {
        String message = getLocalizedMessage("project.invalid.operation", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST, message, "INVALID_PROJECT_OPERATION",
                ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle duplicate membership exceptions
     */
    @ExceptionHandler(DuplicateMembershipException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateMembershipException(DuplicateMembershipException ex) {
        String message = getLocalizedMessage("project.duplicate.membership", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(HttpStatus.CONFLICT, message, "DUPLICATE_MEMBERSHIP", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ===============================
    // GAMIFICATION EXCEPTION HANDLERS
    // ===============================

    /**
     * Handle gamification-related exceptions
     */
    @ExceptionHandler(com.pbm5.bugtracker.exception.GamificationException.class)
    public ResponseEntity<Map<String, Object>> handleGamificationException(
            com.pbm5.bugtracker.exception.GamificationException ex) {
        logger.warn("Gamification exception: {}", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(
                ex.getHttpStatus(),
                ex.getMessage(),
                ex.getErrorCode(),
                ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    // ===============================
    // GENERIC EXCEPTION HANDLERS
    // ===============================

    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred: {}", ex.getMessage(), ex);

        String message = getLocalizedMessage("internal.server.error",
                "An error occurred while processing your request");
        Map<String, Object> response = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message,
                "INTERNAL_SERVER_ERROR", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle any other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        String message = getLocalizedMessage("unexpected.error", "An unexpected error occurred");
        Map<String, Object> response = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, "UNEXPECTED_ERROR",
                ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}