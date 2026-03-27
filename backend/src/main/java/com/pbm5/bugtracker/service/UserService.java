package com.pbm5.bugtracker.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.dto.LoginResponse;
import com.pbm5.bugtracker.dto.RegisterRequest;
import com.pbm5.bugtracker.dto.UpdateProfileRequest;
import com.pbm5.bugtracker.dto.UserSearchResponse;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.exception.UserNotFoundException;
import com.pbm5.bugtracker.repository.UserRepository;
import com.pbm5.bugtracker.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for user-related operations
 * Handles user registration, authentication, and profile management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ProjectMemberRepository projectMemberRepository;
    private final GamificationService gamificationService;

    /**
     * Search users with pagination and filtering
     * 
     * @param search   search term for user name or email
     * @param role     filter by user role
     * @param pageable pagination parameters
     * @return UserSearchResponse with paginated results
     */
    public UserSearchResponse searchUsers(String search, String role, Pageable pageable) {
        Page<User> userPage;

        if (search != null && !search.trim().isEmpty()) {
            // Search by name or email - use optimized search for better performance
            if (role != null && !role.trim().isEmpty()) {
                userPage = userRepository.findBySearchTermAndRole(search.trim(), role.trim(), pageable);
            } else {
                // Use optimized search for invitation purposes
                userPage = userRepository.findBySearchTermOptimized(search.trim(), pageable);
            }
        } else if (role != null && !role.trim().isEmpty()) {
            // Filter by role only
            userPage = userRepository.findByRole(role.trim(), pageable);
        } else {
            // No filters, get all users
            userPage = userRepository.findAll(pageable);
        }

        return UserSearchResponse.fromPage(userPage);
    }

    /**
     * Search project members with pagination and filtering
     * 
     * @param projectId the project ID to search within
     * @param search    search term for user name or email
     * @param pageable  pagination parameters
     * @return UserSearchResponse with paginated results of project members only
     */
    public UserSearchResponse searchProjectMembers(UUID projectId, String search, Pageable pageable) {
        Page<User> userPage;

        if (search != null && !search.trim().isEmpty()) {
            // Search project members by name or email
            userPage = userRepository.findProjectMembersBySearchTerm(projectId, search.trim(), pageable);
        } else {
            // Get all project members
            userPage = userRepository.findProjectMembers(projectId, pageable);
        }

        return UserSearchResponse.fromPage(userPage);
    }

    /**
     * Get user by ID
     * 
     * @param userId the user ID
     * @return the user
     * @throws UserNotFoundException if user not found
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Get users by IDs (bulk operation)
     * 
     * @param userIds list of user IDs
     * @return list of users
     */
    public List<User> getUsersByIds(List<UUID> userIds) {
        return userRepository.findAllById(userIds);
    }

    /**
     * Registers a new user with the provided information
     * 
     * @param request the registration request containing user details
     * @return success message or error message
     */
    public String registerUser(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("User registration failed: email {} already exists", request.getEmail());
            return "User already exists";
        }

        // Create new user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .skills(request.getSkills())
                .build();

        // Save user to database
        User savedUser = userRepository.save(user);
        log.info("Successfully registered user: {} (ID: {})", savedUser.getEmail(), savedUser.getId());

        return "User registered successfully";
    }

    /**
     * Validates login credentials, and if valid, returns a JWT token with daily
     * login status.
     * Business logic belongs in the service layer, not the controller — this keeps
     * our architecture clean.
     */

    public LoginResponse login(String email, String rawPassword) {
        log.info("User login attempt for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Login failed for user {}: invalid password", email);
            throw new RuntimeException("Invalid password");
        }

        log.info("User {} successfully authenticated, processing gamification and generating JWT token", email);

        // Process daily login for gamification points - SINGLE SOURCE OF TRUTH
        boolean dailyLoginAwarded = false;
        try {
            log.info("Processing daily login for authenticated user: {} (ID: {}) - SINGLE SOURCE OF TRUTH", email,
                    user.getId());

            // Check eligibility BEFORE processing to avoid duplicate transactions
            boolean wasEligible = gamificationService.isUserEligibleForDailyLogin(user.getId());
            log.info("User {} eligible for daily login: {}", email, wasEligible);

            if (wasEligible) {
                // Process daily login - this is the ONLY place daily login should be called
                gamificationService.handleDailyLogin(user.getId());
                log.info("Daily login processed successfully for user: {} - points awarded", email);
                dailyLoginAwarded = true; // Points were awarded during this session
                log.info("Daily login points awarded during this session for user {}: {}", email, dailyLoginAwarded);
            } else {
                log.info("User {} not eligible for daily login today, no points awarded", email);
            }

        } catch (Exception e) {
            log.error("Error processing daily login for user {}: {}", email, e.getMessage(), e);
            // Don't fail login if gamification processing fails
        }

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail());
        log.info("JWT token generated successfully for user: {}", email);

        // Build and return login response with token and daily login status
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .dailyLoginAwarded(dailyLoginAwarded)
                .build();

        log.info("Login response built for user {}: dailyLoginAwarded={}", email, dailyLoginAwarded);
        return response;
    }

    /**
     * Update user profile information
     * 
     * @param userId  the user ID to update
     * @param request the profile update request
     * @return the updated user
     * @throws UserNotFoundException if user not found
     */
    public User updateUserProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        // Update fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setSkills(request.getSkills());

        // Save and return updated user
        return userRepository.save(user);
    }

}
