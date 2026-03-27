package com.pbm5.bugtracker.util;

import com.pbm5.bugtracker.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Utility class for handling authentication-related operations consistently across controllers.
 * This centralizes the logic for extracting user information from Spring Security Authentication objects.
 */
public class AuthenticationUtils {
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticationUtils.class);
    
    /**
     * Extract the current user ID from the authentication object.
     * 
     * @param authentication the Spring Security authentication object
     * @return the user ID as UUID
     * @throws IllegalArgumentException if authentication is invalid or user ID cannot be extracted
     */
    public static UUID getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        try {
            // The principal is the User object, so we can cast it and get the ID
            if (authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                return user.getId();
            } else {
                log.error("Authentication principal is not a User entity: {}", 
                    authentication.getPrincipal().getClass().getSimpleName());
                throw new IllegalArgumentException("Authentication principal is not a User entity");
            }
        } catch (Exception e) {
            log.error("Error extracting user ID from authentication: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid user ID format");
        }
    }
    
    /**
     * Extract the current user from the authentication object.
     * 
     * @param authentication the Spring Security authentication object
     * @return the User entity
     * @throws IllegalArgumentException if authentication is invalid or user cannot be extracted
     */
    public static User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        try {
            if (authentication.getPrincipal() instanceof User) {
                return (User) authentication.getPrincipal();
            } else {
                log.error("Authentication principal is not a User entity: {}", 
                    authentication.getPrincipal().getClass().getSimpleName());
                throw new IllegalArgumentException("Authentication principal is not a User entity");
            }
        } catch (Exception e) {
            log.error("Error extracting user from authentication: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid user format");
        }
    }
    
    /**
     * Check if the current user has a specific role.
     * 
     * @param authentication the Spring Security authentication object
     * @param role the role to check (e.g., "ADMIN", "DEVELOPER")
     * @return true if user has the role, false otherwise
     */
    public static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
} 