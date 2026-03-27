package com.pbm5.bugtracker.controller;

import com.pbm5.bugtracker.dto.LoginRequest;
import com.pbm5.bugtracker.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pbm5.bugtracker.dto.RegisterRequest;
import com.pbm5.bugtracker.service.UserService;
import com.pbm5.bugtracker.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bugtracker/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        String response = userService.registerUser(request);
        if (response.equals("User already exists")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // REST endpoint for users to authenticate and receive a JWT token.
    // Exposes login API, maps to /api/bugtracker/v1/auth/login, returns 200 OK with
    // token or 401 Unauthorized.

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok().body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // Exception handling is now handled globally by GlobalExceptionHandler
    
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            passwordResetService.sendResetEmail(request.get("email"));
            return ResponseEntity.ok("Reset email sent");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send reset email");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        try {
            passwordResetService.resetPassword(request.get("token"), request.get("newPassword"));
            return ResponseEntity.ok("Password reset successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to reset password");
        }
    }
}
