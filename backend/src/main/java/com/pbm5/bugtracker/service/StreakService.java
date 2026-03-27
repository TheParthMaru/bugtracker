package com.pbm5.bugtracker.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.dto.StreakInfoResponse;
import com.pbm5.bugtracker.entity.UserStreak;
import com.pbm5.bugtracker.exception.StreakValidationException;
import com.pbm5.bugtracker.repository.UserStreakRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing user login streaks
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StreakService {

    private final UserStreakRepository userStreakRepository;

    /**
     * Update user streak for daily login
     */
    public StreakInfoResponse updateUserStreak(UUID userId) {
        LocalDate today = LocalDate.now();

        Optional<UserStreak> existingStreak = userStreakRepository.findByUserId(userId);
        UserStreak userStreak;

        if (existingStreak.isPresent()) {
            userStreak = existingStreak.get();
            updateExistingStreak(userStreak, today);
        } else {
            userStreak = createNewStreak(userId, today);
        }

        UserStreak savedStreak = userStreakRepository.save(userStreak);
        log.info("Updated streak for user {}: current streak = {}, max streak = {}",
                userId, savedStreak.getCurrentStreak(), savedStreak.getMaxStreak());

        return mapToResponse(savedStreak);
    }

    /**
     * Calculate current streak for a user
     */
    public int calculateStreak(UUID userId) {
        Optional<UserStreak> userStreak = userStreakRepository.findByUserId(userId);
        return userStreak.map(UserStreak::getCurrentStreak).orElse(0);
    }

    /**
     * Get user streak information
     */
    public StreakInfoResponse getUserStreak(UUID userId) {
        UserStreak userStreak = userStreakRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating initial streak record for user: {}", userId);
                    UserStreak newStreak = createNewStreak(userId, LocalDate.now());
                    return userStreakRepository.save(newStreak);
                });

        return mapToResponse(userStreak);
    }

    /**
     * Update max streak if current streak exceeds it
     */
    public void updateMaxStreak(UUID userId, int currentStreak) {
        Optional<UserStreak> userStreak = userStreakRepository.findByUserId(userId);
        if (userStreak.isPresent()) {
            UserStreak streak = userStreak.get();
            if (currentStreak > streak.getMaxStreak()) {
                streak.setMaxStreak(currentStreak);
                userStreakRepository.save(streak);
                log.info("Updated max streak for user {}: {}", userId, currentStreak);
            }
        }
    }

    /**
     * Validate streak update
     */
    public boolean validateStreakUpdate(UUID userId, LocalDate loginDate) {
        if (loginDate == null) {
            throw new StreakValidationException("Login date cannot be null");
        }

        if (loginDate.isAfter(LocalDate.now())) {
            throw new StreakValidationException("Login date cannot be in the future");
        }

        return true;
    }

    /**
     * Update existing user streak
     */
    private void updateExistingStreak(UserStreak userStreak, LocalDate today) {
        LocalDate lastLogin = userStreak.getLastLoginDate();

        if (lastLogin == null) {
            // First login ever
            userStreak.setCurrentStreak(1);
            userStreak.setMaxStreak(1);
        } else if (lastLogin.equals(today)) {
            // Already logged in today, no change
            log.debug("User already logged in today, streak unchanged");
            return;
        } else if (lastLogin.equals(today.minusDays(1))) {
            // Consecutive day, increment streak
            userStreak.incrementStreak();
        } else {
            // Gap in login, reset streak
            userStreak.setCurrentStreak(1);
        }

        userStreak.setLastLoginDate(today);
    }

    /**
     * Create new user streak
     */
    private UserStreak createNewStreak(UUID userId, LocalDate today) {
        log.info("Creating new streak for user: {}", userId);
        return UserStreak.builder()
                .userId(userId)
                .currentStreak(1)
                .maxStreak(1)
                .lastLoginDate(today)
                .build();
    }

    /**
     * Map entity to response DTO
     */
    private StreakInfoResponse mapToResponse(UserStreak userStreak) {
        return StreakInfoResponse.builder()
                .userId(userStreak.getUserId())
                .currentStreak(userStreak.getCurrentStreak())
                .maxStreak(userStreak.getMaxStreak())
                .lastLoginDate(userStreak.getLastLoginDate())
                .streakStartDate(calculateStreakStartDate(userStreak))
                .build();
    }

    /**
     * Calculate when the current streak started
     */
    private LocalDate calculateStreakStartDate(UserStreak userStreak) {
        if (userStreak.getCurrentStreak() <= 0 || userStreak.getLastLoginDate() == null) {
            return null;
        }

        return userStreak.getLastLoginDate().minusDays(userStreak.getCurrentStreak() - 1);
    }
}
