package com.pbm5.bugtracker.controller;

import com.pbm5.bugtracker.dto.UserResponse;
import com.pbm5.bugtracker.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;

/**
 * A secured endpoint to test JWT authentication (only accessible with valid
 * token)
 */

@RestController
@RequestMapping("/api/bugtracker/v1/profile")
@Slf4j
public class ProfileController {

    @GetMapping
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        var user = (User) authentication.getPrincipal();

        log.info("Profile requested for user: {} (ID: {})", user.getEmail(), user.getId());
        log.info("User object: {}", user);
        log.info("User ID type: {}", user.getId() != null ? user.getId().getClass().getName() : "NULL");

        // Ensure we have a valid user ID
        String userId;
        if (user.getId() != null) {
            userId = user.getId().toString();
        } else {
            // Generate a new UUID if the user doesn't have one (this shouldn't happen in
            // normal cases)
            userId = UUID.randomUUID().toString();
            log.warn("User {} had no ID, generated new UUID: {}", user.getEmail(), userId);
        }

        var response = new UserResponse(userId, user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole(), user.getSkills());

        log.info("Returning profile response: {}", response);
        log.info("Response ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }
}
