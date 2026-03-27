package com.pbm5.bugtracker.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

/**
 * Validator for the @PasswordsMatch annotation.
 * 
 * This validator uses reflection to access the password and confirmPassword
 * fields
 * from the object being validated and compares their values.
 * 
 * The validation fails if:
 * - Either field is null
 * - The field values are not equal
 * - The specified fields don't exist in the class
 */
public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordsMatch constraintAnnotation) {
        this.passwordField = constraintAnnotation.passwordField();
        this.confirmPasswordField = constraintAnnotation.confirmPasswordField();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true; // Let @NotNull handle null objects
        }

        try {
            // Get the password field value
            Object passwordValue = getFieldValue(obj, passwordField);
            Object confirmPasswordValue = getFieldValue(obj, confirmPasswordField);

            // If either field is null, let @NotBlank handle it
            if (passwordValue == null || confirmPasswordValue == null) {
                return true;
            }

            // Compare the values
            boolean isValid = passwordValue.equals(confirmPasswordValue);

            if (!isValid) {
                // Add custom error message
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Passwords do not match")
                        .addPropertyNode(confirmPasswordField)
                        .addConstraintViolation();
            }

            return isValid;

        } catch (Exception e) {
            // If reflection fails, log the error and return false
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Error validating password match: " + e.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }

    /**
     * Uses reflection to get the value of a field from an object.
     */
    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}