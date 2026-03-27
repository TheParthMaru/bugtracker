package com.pbm5.bugtracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom password validation annotation that enforces strong password
 * requirements.
 * 
 * Password Requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter (A-Z)
 * - At least one lowercase letter (a-z)
 * - At least one digit (0-9)
 * - At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
 * 
 * Usage:
 * 
 * @ValidPassword
 *                private String password;
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password does not meet requirements";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}