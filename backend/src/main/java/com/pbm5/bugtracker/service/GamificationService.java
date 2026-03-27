package com.pbm5.bugtracker.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.dto.PointTransactionResponse;
import com.pbm5.bugtracker.dto.UserPointsResponse;
import com.pbm5.bugtracker.entity.PointTransaction;
import com.pbm5.bugtracker.entity.PointValue;
import com.pbm5.bugtracker.entity.TransactionReason;
import com.pbm5.bugtracker.entity.UserPoints;
import com.pbm5.bugtracker.entity.Project;
import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.repository.UserPointsRepository;
import com.pbm5.bugtracker.repository.ProjectRepository;
import com.pbm5.bugtracker.repository.BugRepository;
import com.pbm5.bugtracker.repository.UserRepository;
import com.pbm5.bugtracker.repository.PointTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Main service for coordinating gamification operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GamificationService {

    private final UserPointsRepository userPointsRepository;
    private final PointCalculationService pointCalculationService;
    private final LeaderboardService leaderboardService;
    private final StreakService streakService;
    private final ProjectRepository projectRepository;
    private final BugRepository bugRepository;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final NotificationService notificationService;

    /**
     * Initialize gamification data for a first-time user
     * This method is called ONCE when user first logs in to the platform
     */
    public void initializeUserGamificationData(UUID userId) {
        log.info("Initializing gamification data for first-time user: {}", userId);

        // Check if user already has gamification data to prevent duplicate
        // initialization
        if (userPointsRepository.existsByUserId(userId)) {
            log.warn("User {} already has gamification data, skipping initialization", userId);
            return;
        }

        // Create initial gamification data (welcome bonus, streaks, etc.)
        createInitialUserPoints(userId);

        log.info("Successfully initialized gamification data for user: {}", userId);
    }

    /**
     * Get user's gamification points and statistics
     * This method now has fallback initialization if the authentication event
     * listener fails
     */
    public UserPointsResponse getUserPoints(UUID userId) {
        UserPoints userPoints = userPointsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.warn(
                            "User points not found for user: {} - authentication event listener may have failed. Initializing now.",
                            userId);

                    // Fallback initialization
                    try {
                        initializeUserGamificationData(userId);
                        return userPointsRepository.findByUserId(userId)
                                .orElseThrow(() -> new RuntimeException(
                                        "Failed to initialize gamification data for user: " + userId));
                    } catch (Exception e) {
                        log.error("Fallback initialization failed for user {}: {}", userId, e.getMessage(), e);
                        throw new RuntimeException("Failed to initialize gamification data for user: " + userId, e);
                    }
                });

        return mapToUserPointsResponse(userPoints);
    }

    /**
     * Get user's point transaction history with pagination
     */
    public Page<PointTransactionResponse> getPointHistory(UUID userId, Pageable pageable) {
        log.debug("Fetching point transaction history for user: {} with pagination: {}", userId, pageable);

        // Get paginated transactions from repository
        Page<PointTransaction> transactions = pointTransactionRepository.findByUserIdOrderByEarnedAtDesc(userId,
                pageable);

        // Map to response DTOs
        List<PointTransactionResponse> responses = transactions.getContent().stream()
                .map(transaction -> PointTransactionResponse.builder()
                        .transactionId(transaction.getTransactionId())
                        .userId(transaction.getUserId())
                        .projectId(transaction.getProjectId())
                        .points(transaction.getNetPoints())
                        .pointsCredited(transaction.getPointsCredited())
                        .pointsDeducted(transaction.getPointsDeducted())
                        .reason(transaction.getReason())
                        .bugId(transaction.getBugId())
                        .earnedAt(transaction.getEarnedAt())
                        .build())
                .collect(Collectors.toList());

        // Create new page with mapped content
        return new PageImpl<>(responses, pageable, transactions.getTotalElements());
    }

    /**
     * Award points to a user and update all related tables
     */
    public PointTransactionResponse awardPoints(UUID userId, UUID projectId, int points, String reason, Long bugId) {
        // Create point transaction
        PointTransactionResponse transaction = pointCalculationService.createPointTransaction(
                com.pbm5.bugtracker.dto.PointTransactionRequest.builder()
                        .userId(userId)
                        .projectId(projectId)
                        .pointsCredited(points > 0 ? points : 0)
                        .pointsDeducted(points < 0 ? Math.abs(points) : 0)
                        .reason(reason)
                        .bugId(bugId)
                        .build(),
                userId);

        // Update user points
        updateUserPoints(userId, points, reason, bugId);

        // Update project leaderboard only if projectId is provided (project-specific
        // activities)
        if (projectId != null) {
            leaderboardService.updateProjectLeaderboard(projectId, userId, points);
        }

        log.info("Awarded {} points to user {} for reason: {} in project: {}",
                points, userId, reason, projectId != null ? projectId : "N/A");

        return transaction;
    }

    /**
     * Handle daily login for a user
     * This method is called by UserService.login() - SINGLE SOURCE OF TRUTH
     * Uses new enum structure and database constraints for deduplication
     */
    public void handleDailyLogin(UUID userId) {
        log.info("Daily login check triggered for user: {} - SINGLE SOURCE OF TRUTH", userId);

        // Check if user is eligible for daily login points today
        if (isUserEligibleForDailyLogin(userId)) {
            log.info("User {} is eligible for daily login points, awarding {} points", userId,
                    PointValue.DAILY_LOGIN.getPoints());

            // Award daily login points using new enum structure
            // Database constraint unique_daily_login_per_user_per_day prevents duplicates
            String reason = TransactionReason.DAILY_LOGIN.getValue();
            awardPoints(userId, null, PointValue.DAILY_LOGIN.getPoints(), reason, null);

            // Update streak
            streakService.updateUserStreak(userId);

            log.info("Daily login points and streak updated successfully for user: {} - using enum structure", userId);
        } else {
            log.debug("User {} is not eligible for daily login points today", userId);
        }
    }

    /**
     * Check if user is eligible for daily login points
     * NEW: 24-hour reset logic - user can get points once per day (resets at
     * midnight)
     */
    public boolean isUserEligibleForDailyLogin(UUID userId) {
        log.debug("Checking daily login eligibility for user: {}", userId);

        LocalDate today = LocalDate.now();
        log.debug("Current date: {}", today);

        // Find any daily login transactions for today
        List<PointTransaction> todayTransactions = pointCalculationService
                .findDailyLoginTransactionsForDate(userId, today);

        boolean hasDailyLoginToday = !todayTransactions.isEmpty();
        log.debug("User {} has daily login transactions today: {}", userId, hasDailyLoginToday);

        if (hasDailyLoginToday) {
            log.debug("User {} already received daily login points today, not eligible", userId);
            return false;
        }

        log.debug("User {} is eligible for daily login points today", userId);
        return true;
    }

    /**
     * Check if user has received daily login points today
     * DEPRECATED: This method is kept for backward compatibility but not used
     */
    @Deprecated
    public boolean hasUserReceivedDailyLoginToday(UUID userId) {
        log.warn("hasUserReceivedDailyLoginToday is deprecated, use isUserEligibleForDailyLogin instead");
        return !isUserEligibleForDailyLogin(userId);
    }

    // Removed wasDailyLoginAwardedToday method - now using session-based logic

    // Removed getSystemProjectId() and isSystemProject() methods
    // System activities now use NULL project_id instead of fake system project IDs

    /**
     * Handle bug resolution and award points
     */
    public void handleBugResolution(Long bugId, UUID userId, UUID projectId, String priority) {
        int points = calculateBugResolutionPoints(priority);

        // Get project name for the reason
        String projectName = getProjectName(projectId);
        String projectTicketNumber = getProjectTicketNumber(bugId, projectId);

        // Create enhanced reason with project details and points
        String enhancedReason = String.format("%s - %s (+%d points) | Project: %s | Ticket: %s",
                TransactionReason.fromValue("bug-resolution-" + priority.toLowerCase()).getValue(), priority, points,
                projectName, projectTicketNumber);

        // Award points and update project leaderboard with bugs resolved increment
        awardPoints(userId, projectId, points, enhancedReason, bugId);

        // Update project leaderboard with bugs resolved increment (+1)
        leaderboardService.updateProjectLeaderboard(projectId, userId, points, 1);

        // Update global user bugs resolved count (+1 for resolution)
        updateUserBugsResolved(userId, 1);

        // Send gamification notification to the user who earned points
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                log.info(
                        "GamificationService.handleBugResolution - Sending points notification to user: {} ({}) for bug resolution",
                        user.getEmail(), userId);

                notificationService.createAndDeliverNotification(
                        userId,
                        "GAMIFICATION_POINTS",
                        "Points earned for bug resolution!",
                        String.format("You earned %d points for resolving a %s priority bug in project %s", points,
                                priority, projectName),
                        null, // No specific data needed
                        Map.of("points", points, "reason", "bug resolution"),
                        null, // No bug context
                        null, // No project context
                        null, // No team context
                        null // No assigned by context
                );

                log.info(
                        "GamificationService.handleBugResolution - Points notification sent successfully to user: {} ({})",
                        user.getEmail(), userId);
            } else {
                log.warn("GamificationService.handleBugResolution - User not found for notification: userId={}",
                        userId);
            }
        } catch (Exception e) {
            log.warn("GamificationService.handleBugResolution - Failed to send points notification to user {}: {}",
                    userId, e.getMessage());
            // Don't fail the gamification process if notification fails
        }

        log.info("Awarded {} points to user {} for resolving bug {} with priority {} in project {}",
                points, userId, bugId, priority, projectName);
    }

    /**
     * Handle bug reopening and apply penalty
     */
    public void handleBugReopening(Long bugId, UUID userId, UUID projectId) {
        int penalty = PointValue.BUG_REOPENED_PENALTY.getPoints(); // Always -10 points for bug reopened

        // Get project name for the reason
        String projectName = getProjectName(projectId);
        String projectTicketNumber = getProjectTicketNumber(bugId, projectId);

        // Create enhanced reason with project details and penalty
        String enhancedReason = String.format("%s - Penalty (%d points) | Project: %s | Ticket: %s",
                TransactionReason.BUG_REOPENED.getValue(), penalty, projectName, projectTicketNumber);

        // Apply penalty (negative points) and update project leaderboard with bugs
        // resolved decrement
        awardPoints(userId, projectId, -penalty, enhancedReason, bugId);

        // Update project leaderboard with bugs resolved decrement (-1)
        leaderboardService.updateProjectLeaderboard(projectId, userId, -penalty, -1);

        // Update global user bugs resolved count (-1 for reopening)
        updateUserBugsResolved(userId, -1);

        // Send gamification notification to the user who received penalty
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                log.info(
                        "GamificationService.handleBugReopening - Sending penalty notification to user: {} ({}) for bug reopening",
                        user.getEmail(), userId);

                notificationService.createAndDeliverNotification(
                        userId,
                        "GAMIFICATION_POINTS",
                        "Points penalty for bug reopening",
                        String.format("You received a %d point penalty for reopening a bug in project %s", penalty,
                                projectName),
                        null, // No specific data needed
                        Map.of("points", -penalty, "reason", "bug reopening penalty"),
                        null, // No bug context
                        null, // No project context
                        null, // No team context
                        null // No assigned by context
                );

                log.info(
                        "GamificationService.handleBugReopening - Penalty notification sent successfully to user: {} ({})",
                        user.getEmail(), userId);
            } else {
                log.warn("GamificationService.handleBugReopening - User not found for notification: userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("GamificationService.handleBugReopening - Failed to send penalty notification to user {}: {}",
                    userId, e.getMessage());
            // Don't fail the gamification process if notification fails
        }

        log.info("Applied {} point penalty to user {} for reopening bug {} in project {}",
                penalty, userId, bugId, projectName);
    }

    /**
     * Calculate bug resolution points based on priority
     */
    private int calculateBugResolutionPoints(String priority) {
        return PointValue.getBugResolutionPoints(priority);
    }

    /**
     * Get project name by project ID
     */
    private String getProjectName(UUID projectId) {
        if (projectId == null) {
            return "System";
        }

        try {
            Optional<Project> project = projectRepository.findById(projectId);
            if (project.isPresent()) {
                return project.get().getName();
            } else {
                log.warn("Project not found for project ID: {}", projectId);
                return "Unknown Project";
            }
        } catch (Exception e) {
            log.warn("Failed to get project name for project ID: {}, error: {}", projectId, e.getMessage());
            return "Unknown Project";
        }
    }

    /**
     * Get project ticket number by bug ID and project ID
     */
    private String getProjectTicketNumber(Long bugId, UUID projectId) {
        if (bugId == null) {
            return "N/A";
        }

        try {
            Optional<Bug> bug = bugRepository.findById(bugId);
            if (bug.isPresent()) {
                Integer projectTicketNumber = bug.get().getProjectTicketNumber();
                return projectTicketNumber != null ? "#" + projectTicketNumber : "#" + bugId;
            } else {
                log.warn("Bug not found for bug ID: {}", bugId);
                return "#" + bugId;
            }
        } catch (Exception e) {
            log.warn("Failed to get project ticket number for bug ID: {}, error: {}", bugId, e.getMessage());
            return "#" + bugId;
        }
    }

    /**
     * Update user points
     */
    private void updateUserPoints(UUID userId, int points, String reason, Long bugId) {
        Optional<UserPoints> existingPoints = userPointsRepository.findByUserId(userId);
        UserPoints userPoints;

        if (existingPoints.isPresent()) {
            userPoints = existingPoints.get();
            userPoints.addPoints(points);

            if (reason.contains("bug-resolution")) {
                updateUserBugsResolved(userId, 1);
            }
        } else {
            userPoints = createInitialUserPoints(userId);
            userPoints.addPoints(points);

            if (reason.contains("bug-resolution")) {
                updateUserBugsResolved(userId, 1);
            }
        }

        userPointsRepository.save(userPoints);
    }

    /**
     * Update bugs resolved count for user points (global stats)
     * Handles both increment (+1 for resolution) and decrement (-1 for reopening)
     */
    private void updateUserBugsResolved(UUID userId, int delta) {
        Optional<UserPoints> userPoints = userPointsRepository.findByUserId(userId);
        if (userPoints.isPresent()) {
            UserPoints points = userPoints.get();
            int newBugsResolved = points.getBugsResolved() + delta;
            // Ensure bugs resolved never goes below 0
            points.setBugsResolved(Math.max(0, newBugsResolved));
            userPointsRepository.save(points);
            log.debug("Updated bugs resolved for user {} in global stats: delta={}, new total={}",
                    userId, delta, points.getBugsResolved());
        }
    }

    /**
     * Create initial user points record with welcome bonus
     */
    private UserPoints createInitialUserPoints(UUID userId) {
        log.info("Creating initial user points for user: {} with welcome bonus", userId);

        // Check if user already has a welcome bonus transaction to prevent duplicates
        // Database constraint unique_welcome_bonus_per_user prevents duplicates
        List<PointTransaction> existingWelcomeBonus = pointCalculationService.findTransactionsByReason(userId,
                TransactionReason.WELCOME_BONUS.getValue());

        if (!existingWelcomeBonus.isEmpty()) {
            log.warn("User {} already has {} welcome bonus transaction(s), skipping creation", userId,
                    existingWelcomeBonus.size());
            // Return existing user points or create minimal record
            return userPointsRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        UserPoints minimalPoints = UserPoints.builder()
                                .userId(userId)
                                .totalPoints(0)
                                .currentStreak(1) // Default starting streak
                                .maxStreak(1) // Default starting max streak
                                .bugsResolved(0)
                                .build();
                        return userPointsRepository.save(minimalPoints);
                    });
        }

        // Create initial point transaction for welcome bonus (system activity)
        // Database constraint unique_welcome_bonus_per_user prevents duplicates
        try {
            String enhancedReason = TransactionReason.WELCOME_BONUS.getValue();
            pointCalculationService.createPointTransaction(
                    com.pbm5.bugtracker.dto.PointTransactionRequest.builder()
                            .userId(userId)
                            .projectId(null) // NULL for system activities
                            .pointsCredited(PointValue.WELCOME_BONUS.getPoints())
                            .pointsDeducted(0)
                            .reason(enhancedReason)
                            .bugId(null)
                            .build(),
                    userId);

            log.info("Created welcome bonus transaction for user: {} using enum structure", userId);
        } catch (Exception e) {
            log.warn("Failed to create welcome bonus transaction for user: {}, error: {}", userId, e.getMessage());
        }

        // Create and save the UserPoints entity
        UserPoints userPoints = UserPoints.builder()
                .userId(userId)
                .totalPoints(PointValue.WELCOME_BONUS.getPoints()) // Welcome bonus point from enum
                .currentStreak(1) // Start with 1-day streak
                .maxStreak(1) // Best streak so far
                .bugsResolved(0)
                .build();

        // CRITICAL FIX: Save the UserPoints entity to the database
        return userPointsRepository.save(userPoints);
    }

    /**
     * Map entity to response DTO
     */
    private UserPointsResponse mapToUserPointsResponse(UserPoints userPoints) {
        Optional<User> user = userRepository.findById(userPoints.getUserId());
        String userDisplayName = user.isPresent() ? user.get().getFirstName() + " " + user.get().getLastName()
                : "User " + userPoints.getUserId();

        return UserPointsResponse.builder()
                .userId(userPoints.getUserId())
                .totalPoints(userPoints.getTotalPoints())
                .currentStreak(userPoints.getCurrentStreak())
                .maxStreak(userPoints.getMaxStreak())
                .bugsResolved(userPoints.getBugsResolved())
                .lastActivity(userPoints.getLastActivity())
                .userDisplayName(userDisplayName)
                .build();
    }
}
