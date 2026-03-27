package com.pbm5.bugtracker.controller;

import com.pbm5.bugtracker.dto.BugResponse;
import com.pbm5.bugtracker.service.BugService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bugtracker/v1/bugs")
@Component
public class GeneralBugController {

    private static final Logger logger = LoggerFactory.getLogger(GeneralBugController.class);

    @Autowired
    private BugService bugService;

    /**
     * Test endpoint to verify controller registration
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        logger.info("=== Test endpoint called ===");
        return ResponseEntity.ok("GeneralBugController is working!");
    }

    /**
     * Get all bugs assigned to the current user across all projects
     */
    @GetMapping("/my-assigned")
    public ResponseEntity<List<BugResponse>> getMyAssignedBugs(Authentication authentication) {
        logger.info("=== getMyAssignedBugs method called ===");
        logger.info("Authentication: {}", authentication);
        logger.info("Authentication name: {}", authentication.getName());

        try {
            UUID userId = getCurrentUserId(authentication);
            logger.info("User ID extracted: {}", userId);

            List<BugResponse> bugs = bugService.getBugsAssignedToUser(userId)
                    .stream()
                    .map(BugResponse::new)
                    .collect(Collectors.toList());

            logger.info("Found {} bugs assigned to user {}", bugs.size(), userId);
            return ResponseEntity.ok(bugs);
        } catch (Exception e) {
            logger.error("Error in getMyAssignedBugs: ", e);
            throw e;
        }
    }

    /**
     * Get all bugs for the current user across all projects
     */
    @GetMapping("/all")
    public ResponseEntity<List<BugResponse>> getAllBugs(Authentication authentication) {
        logger.info("=== getAllBugs method called ===");

        try {
            UUID userId = getCurrentUserId(authentication);
            logger.info("User ID extracted: {}", userId);

            // Get all bugs assigned to the user across all projects
            List<BugResponse> bugs = bugService.getBugsAssignedToUser(userId)
                    .stream()
                    .map(BugResponse::new)
                    .collect(Collectors.toList());

            logger.info("Found {} bugs for user {} across all projects", bugs.size(), userId);
            return ResponseEntity.ok(bugs);
        } catch (Exception e) {
            logger.error("Error in getAllBugs: ", e);
            throw e;
        }
    }

    /**
     * Helper method to extract user ID from authentication
     */
    private UUID getCurrentUserId(Authentication authentication) {
        try {
            // Try to parse directly as UUID first
            UUID userId = UUID.fromString(authentication.getName());
            logger.debug("User ID parsed directly as UUID: {}", userId);
            return userId;
        } catch (IllegalArgumentException e) {
            // If that fails, try to extract UUID from User object string
            String authName = authentication.getName();
            if (authName.contains("id=")) {
                // Extract UUID from User object string format
                int startIndex = authName.indexOf("id=") + 3;
                int endIndex = authName.indexOf(",", startIndex);
                if (endIndex == -1) {
                    endIndex = authName.indexOf("}", startIndex);
                }
                String userIdStr = authName.substring(startIndex, endIndex);
                UUID userId = UUID.fromString(userIdStr);
                logger.debug("User ID extracted from User object: {}", userId);
                return userId;
            } else {
                throw new IllegalArgumentException(
                        "Could not extract user ID from authentication: " + authName);
            }
        }
    }
}