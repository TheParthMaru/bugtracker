package com.pbm5.bugtracker.dto;

import java.util.Set;

import com.pbm5.bugtracker.entity.Role;
import com.pbm5.bugtracker.validation.PasswordsMatch;
import com.pbm5.bugtracker.validation.ValidPassword;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@PasswordsMatch
public class RegisterRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*[A-Za-z0-9]@[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]\\.[A-Za-z]{2,}$", message = "Email must be a valid format with proper domain (e.g., user@example.com, not j@s)")
    private String email;

    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;

    @NotBlank(message = "Password confirmation is required")
    @ValidPassword
    private String confirmPassword;

    @NotNull(message = "Role is required. Valid roles are: ADMIN, DEVELOPER, REPORTER")
    private Role role;

    private Set<String> skills;
}
