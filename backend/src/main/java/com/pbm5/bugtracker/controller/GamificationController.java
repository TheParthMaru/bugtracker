package com.pbm5.bugtracker.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.pbm5.bugtracker.dto.LeaderboardEntryResponse;
import com.pbm5.bugtracker.dto.PointTransactionRequest;
import com.pbm5.bugtracker.dto.PointTransactionResponse;
import com.pbm5.bugtracker.dto.UserPointsResponse;
import com.pbm5.bugtracker.dto.StreakInfoResponse;
import com.pbm5.bugtracker.service.GamificationService;
import com.pbm5.bugtracker.service.LeaderboardService;
import com.pbm5.bugtracker.service.StreakService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for gamification-related operations
 */
@RestController
@RequestMapping("/api/bugtracker/v1/leaderboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GamificationController {

    private final GamificationService gamificationService;
    private final LeaderboardService leaderboardService;
    private final StreakService streakService;

    /**
     * Get user's gamification points and statistics
     */
    @GetMapping("/points/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPointsResponse> getUserPoints(
            @PathVariable UUID userId,
            Authentication authentication) {

        UUID currentUserId = getCurrentUserId(authentication);
        log.debug("Getting gamification points for user: {} (requested by: {})", userId, currentUserId);

        // Users can only view their own points
        if (!currentUserId.equals(userId)) {
            throw new AccessDeniedException("Users can only view their own gamification data");
        }

        UserPointsResponse response = gamificationService.getUserPoints(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's point transaction history
     */
    @GetMapping("/points/{userId}/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PointTransactionResponse>> getPointHistory(
            @PathVariable UUID userId,
            Pageable pageable,
            Authentication authentication) {

        UUID currentUserId = getCurrentUserId(authentication);
        log.debug("Getting point history for user: {} (requested by: {})", userId, currentUserId);

        // Users can only view their own transaction history
        if (!currentUserId.equals(userId)) {
            throw new AccessDeniedException("Users can only view their own transaction history");
        }

        Page<PointTransactionResponse> response = gamificationService.getPointHistory(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Award points to a user (typically called by event listeners)
     */
    @PostMapping("/points/award")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PointTransactionResponse> awardPoints(
            @Valid @RequestBody PointTransactionRequest request,
            Authentication authentication) {

        UUID currentUserId = getCurrentUserId(authentication);
        log.info("Awarding {} credited, {} deducted points to user: {} for reason: {} (requested by: {})",
                request.getPointsCredited(), request.getPointsDeducted(), request.getUserId(), request.getReason(),
                currentUserId);

        PointTransactionResponse response = gamificationService.awardPoints(
                request.getUserId(),
                request.getProjectId(),
                request.getPointsCredited() - request.getPointsDeducted(),
                request.getReason(),
                request.getBugId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get project leaderboard
     */
    @GetMapping("/leaderboard/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LeaderboardEntryResponse>> getProjectLeaderboard(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "all-time") String timeframe,
            Pageable pageable,
            Authentication authentication) {

        UUID currentUserId = getCurrentUserId(authentication);
        log.debug("Getting {} leaderboard for project: {} (requested by: {})", timeframe, projectId, currentUserId);

        Page<LeaderboardEntryResponse> response = leaderboardService.getProjectLeaderboard(projectId, timeframe,
                pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get specific user's project statistics
     */
    @GetMapping("/leaderboard/{projectId}/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaderboardEntryResponse> getUserProjectStats(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            Authentication authentication) {

        UUID currentUserId = getCurrentUserId(authentication);
        log.debug("Getting project stats for user: {} in project: {} (requested by: {})",
                userId, projectId, currentUserId);

        LeaderboardEntryResponse response = leaderboardService.getLeaderboardEntry(projectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's streak information
     */
    @GetMapping("/streaks/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreakInfoResponse> getUserStreak(
            @PathVariable UUID userId,
            Authentication authentication) {

        UUID currentUserId = getCurrentUserId(authentication);
        log.debug("Getting streak info for user: {} (requested by: {})", userId, currentUserId);

        // Users can only view their own streak data
        if (!currentUserId.equals(userId)) {
            throw new AccessDeniedException("Users can only view their own streak data");
        }

        StreakInfoResponse response = streakService.getUserStreak(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to get current user ID from authentication
     */
    private UUID getCurrentUserId(Authentication authentication) {
        com.pbm5.bugtracker.entity.User user = (com.pbm5.bugtracker.entity.User) authentication.getPrincipal();
        return user.getId();
    }
}
