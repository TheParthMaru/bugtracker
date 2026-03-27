package com.pbm5.bugtracker.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled jobs for gamification maintenance tasks
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GamificationScheduler {

    private final LeaderboardService leaderboardService;
    private final GamificationService gamificationService;
    private final UserRepository userRepository;

    // Daily login processing removed - now handled by login-triggered approach

    /**
     * Reset weekly points every Sunday at midnight
     */
    @Scheduled(cron = "0 0 0 * * SUN")
    public void resetWeeklyPoints() {
        log.info("Starting weekly points reset job");
        try {
            leaderboardService.resetWeeklyPoints();
            log.info("Weekly points reset completed successfully");
        } catch (Exception e) {
            log.error("Error during weekly points reset", e);
        }
    }

    /**
     * Reset monthly points on the first day of every month at midnight
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetMonthlyPoints() {
        log.info("Starting monthly points reset job");
        try {
            leaderboardService.resetMonthlyPoints();
            log.info("Monthly points reset completed successfully");
        } catch (Exception e) {
            log.error("Error during monthly points reset", e);
        }
    }
}
