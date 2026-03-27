package com.pbm5.bugtracker.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Custom password validator that enforces comprehensive password requirements.
 * 
 * Validation Rules:
 * 1. Minimum 8 characters length
 * 2. At least one uppercase letter (A-Z)
 * 3. At least one lowercase letter (a-z)
 * 4. At least one digit (0-9)
 * 5. At least one special character
 * 
 * Provides specific error messages for each failed requirement.
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // Regex patterns for different password requirements
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern
            .compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

    private static final int MIN_LENGTH = 8;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.trim().isEmpty()) {
            return false; // @NotBlank will handle this
        }

        password = password.trim();
        boolean isValid = true;

        // Disable default constraint violation to provide custom messages
        context.disableDefaultConstraintViolation();

        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            context.buildConstraintViolationWithTemplate(
                    "Password must be at least " + MIN_LENGTH + " characters long").addConstraintViolation();
            isValid = false;
        }

        // Check for uppercase letter
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one uppercase letter (A-Z)").addConstraintViolation();
            isValid = false;
        }

        // Check for lowercase letter
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one lowercase letter (a-z)").addConstraintViolation();
            isValid = false;
        }

        // Check for digit
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one digit (0-9)").addConstraintViolation();
            isValid = false;
        }

        // Check for special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}