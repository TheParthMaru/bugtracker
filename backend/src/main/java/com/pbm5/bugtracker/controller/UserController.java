package com.pbm5.bugtracker.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.pbm5.bugtracker.dto.TeamResponse;
import com.pbm5.bugtracker.dto.UpdateProfileRequest;
import com.pbm5.bugtracker.dto.UserSearchResponse;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.service.TeamService;
import com.pbm5.bugtracker.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for user-related operations.
 * 
 * Provides endpoints for user-specific data including:
 * - User search and retrieval
 * - User's team memberships
 * - User profile information
 * 
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/bugtracker/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final TeamService teamService;
    private final UserService userService;

    /**
     * Search users with pagination and filtering
     * 
     * @param search   search term for user name or email
     * @param role     filter by user role
     * @param pageable pagination parameters
     * @return 200 OK with paginated user search results
     */
    @GetMapping("/search")
    public ResponseEntity<UserSearchResponse> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            Pageable pageable) {

        UserSearchResponse response = userService.searchUsers(search, role, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID
     * 
     * @param userId the user ID
     * @return 200 OK with user details
     */
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable UUID userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Get users by IDs (bulk operation)
     * 
     * @param userIds list of user IDs
     * @return 200 OK with list of users
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<User>> getUsersByIds(@RequestBody List<UUID> userIds) {
        List<User> users = userService.getUsersByIds(userIds);
        return ResponseEntity.ok(users);
    }

    /**
     * Get current user profile
     * 
     * @param authentication the current authentication context
     * @return 200 OK with current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<User> getCurrentUserProfile(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(currentUser);
    }

    /**
     * Get current user's teams.
     * Returns all teams where the authenticated user is a member.
     * 
     * @param authentication the current authentication context
     * @return 200 OK with list of user's teams
     */
    @GetMapping("/me/teams")
    public ResponseEntity<List<TeamResponse>> getCurrentUserTeams(
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        List<TeamResponse> teams = teamService.getUserTeams(currentUser.getId());

        return ResponseEntity.ok(teams);
    }

    /**
     * Update current user profile
     * 
     * @param request the profile update request
     * @param authentication the current authentication context
     * @return 200 OK with updated user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<User> updateCurrentUserProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        User updatedUser = userService.updateUserProfile(currentUser.getId(), request);

        return ResponseEntity.ok(updatedUser);
    }
}