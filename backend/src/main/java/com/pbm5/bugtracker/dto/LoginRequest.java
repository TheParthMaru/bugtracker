package com.pbm5.bugtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object (DTO) that holds email and password during login.
 * Keeps controller input separate from internal models, enables validation like @NotBlank and @Email.
 */

@Data
public class LoginRequest {
    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
