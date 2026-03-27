package com.pbm5.bugtracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Class-level validation annotation to ensure password and confirmPassword
 * fields match.
 * 
 * This annotation should be applied at the class level (not field level)
 * because
 * it needs to compare two fields within the same object.
 * 
 * Usage:
 * 
 * @PasswordsMatch
 *                 public class RegisterRequest {
 *                 private String password;
 *                 private String confirmPassword;
 *                 }
 * 
 *                 The validator will look for fields named "password" and
 *                 "confirmPassword"
 *                 and ensure they contain the same value.
 */
@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordsMatch {
    String message() default "Passwords do not match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Name of the password field (defaults to "password")
     */
    String passwordField() default "password";

    /**
     * Name of the confirm password field (defaults to "confirmPassword")
     */
    String confirmPasswordField() default "confirmPassword";
}