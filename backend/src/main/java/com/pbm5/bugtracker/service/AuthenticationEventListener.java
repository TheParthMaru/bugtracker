package com.pbm5.bugtracker.service;

import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.repository.UserPointsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event listener for authentication events
 * Handles gamification data initialization for first-time users
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventListener {

    @PostConstruct
    public void init() {
        log.info("AuthenticationEventListener initialized and ready to handle authentication events");
    }

    private final GamificationService gamificationService;
    private final UserPointsRepository userPointsRepository;

    /**
     * Handle successful authentication and initialize gamification data for
     * first-time users ONLY
     * This ensures welcome bonus is awarded exactly once when user first joins the
     * platform
     * Daily login is handled by UserService.login() to avoid duplicate processing
     */
    @EventListener
    @Transactional
    public void handleSuccessfulAuthentication(AuthenticationSuccessEvent event) {
        try {
            log.info("AuthenticationSuccessEvent received: {}", event.getClass().getSimpleName());
            log.info("Authentication object: {}", event.getAuthentication());
            log.info("Authentication principal: {}", event.getAuthentication().getPrincipal());

            // Extract user from authentication event
            if (event.getAuthentication().getPrincipal() instanceof User) {
                User user = (User) event.getAuthentication().getPrincipal();
                UUID userId = user.getId();

                log.info("Processing AuthenticationSuccessEvent for user: {} (ID: {})", user.getEmail(), userId);

                // Check if user needs gamification initialization
                if (!userPointsRepository.existsByUserId(userId)) {
                    log.info("First-time user {} logging in, initializing gamification data", userId);

                    // Initialize gamification data (welcome bonus, streaks, etc.)
                    gamificationService.initializeUserGamificationData(userId);

                    log.info("Successfully initialized gamification data for first-time user: {}", userId);
                } else {
                    log.debug("User {} already has gamification data, skipping initialization", userId);
                }

                // REMOVED: Daily login processing - UserService.login() handles this to avoid
                // duplicate calls

            } else {
                log.warn("Principal is not a User instance: {}",
                        event.getAuthentication().getPrincipal().getClass().getSimpleName());
            }

        } catch (Exception e) {
            log.error("Error during gamification data initialization for AuthenticationSuccessEvent: {}",
                    e.getMessage(), e);
            // Don't throw exception to avoid breaking authentication flow
        }
    }

    // REMOVED: Redundant JWT authentication listener - UserService.login() handles
    // daily login

    // REMOVED: Redundant AbstractAuthenticationEvent listener - UserService.login()
    // handles daily login

    // REMOVED: Redundant generic event listener - UserService.login() handles daily
    // login
}
