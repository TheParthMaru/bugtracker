package com.pbm5.bugtracker.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.dto.LeaderboardEntryResponse;
import com.pbm5.bugtracker.entity.ProjectLeaderboard;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.exception.LeaderboardNotFoundException;
import com.pbm5.bugtracker.repository.ProjectLeaderboardRepository;
import com.pbm5.bugtracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing project leaderboards
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeaderboardService {

    private final ProjectLeaderboardRepository projectLeaderboardRepository;
    private final UserRepository userRepository;

    /**
     * Update project leaderboard entry for a user
     */
    public void updateProjectLeaderboard(UUID projectId, UUID userId, int points) {
        updateProjectLeaderboard(projectId, userId, points, 0);
    }

    /**
     * Update project leaderboard entry for a user with bugs resolved delta
     */
    public void updateProjectLeaderboard(UUID projectId, UUID userId, int points, int bugsResolvedDelta) {
        Optional<ProjectLeaderboard> existingEntry = projectLeaderboardRepository
                .findByProjectIdAndUserId(projectId, userId);

        ProjectLeaderboard leaderboardEntry;

        if (existingEntry.isPresent()) {
            leaderboardEntry = existingEntry.get();
            leaderboardEntry.addAllTimePoints(points);
            leaderboardEntry.addWeeklyPoints(points);
            leaderboardEntry.addMonthlyPoints(points);

            // Update bugs resolved count if delta is provided
            if (bugsResolvedDelta != 0) {
                int newBugsResolved = leaderboardEntry.getBugsResolved() + bugsResolvedDelta;
                // Ensure bugs resolved never goes below 0
                leaderboardEntry.setBugsResolved(Math.max(0, newBugsResolved));
                log.debug("Updated bugs resolved for project {} user {}: delta={}, new total={}",
                        projectId, userId, bugsResolvedDelta, leaderboardEntry.getBugsResolved());
            }
        } else {
            leaderboardEntry = createNewLeaderboardEntry(projectId, userId, points);

            // Set initial bugs resolved count if delta is provided
            if (bugsResolvedDelta > 0) {
                leaderboardEntry.setBugsResolved(bugsResolvedDelta);
                log.debug("Set initial bugs resolved for project {} user {}: {}",
                        projectId, userId, bugsResolvedDelta);
            }
        }

        projectLeaderboardRepository.save(leaderboardEntry);
        log.debug("Updated leaderboard for project {} user {}: +{} points, bugs resolved delta: {}",
                projectId, userId, points, bugsResolvedDelta);
    }

    /**
     * Get weekly leaderboard for a project
     */
    public List<LeaderboardEntryResponse> getWeeklyLeaderboard(UUID projectId) {
        List<ProjectLeaderboard> entries = projectLeaderboardRepository
                .findByProjectIdOrderByWeeklyPointsDesc(projectId);

        return mapToResponseList(entries, "weekly");
    }

    /**
     * Get monthly leaderboard for a project
     */
    public List<LeaderboardEntryResponse> getMonthlyLeaderboard(UUID projectId) {
        List<ProjectLeaderboard> entries = projectLeaderboardRepository
                .findByProjectIdOrderByMonthlyPointsDesc(projectId);

        return mapToResponseList(entries, "monthly");
    }

    /**
     * Get all-time leaderboard for a project
     */
    public List<LeaderboardEntryResponse> getAllTimeLeaderboard(UUID projectId) {
        List<ProjectLeaderboard> entries = projectLeaderboardRepository
                .findByProjectIdOrderByAllTimePointsDesc(projectId);

        return mapToResponseList(entries, "all-time");
    }

    /**
     * Get leaderboard entry for a specific user in a project
     */
    public LeaderboardEntryResponse getLeaderboardEntry(UUID projectId, UUID userId) {
        ProjectLeaderboard entry = projectLeaderboardRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new LeaderboardNotFoundException(
                        "Leaderboard entry not found for project: " + projectId + ", user: " + userId));

        return mapToResponse(entry, 0);
    }

    /**
     * Get paginated leaderboard for a project
     */
    public Page<LeaderboardEntryResponse> getProjectLeaderboard(UUID projectId, String timeframe, Pageable pageable) {
        Page<ProjectLeaderboard> entries;

        switch (timeframe.toLowerCase()) {
            case "weekly":
                entries = projectLeaderboardRepository.findTopWeeklyPerformersByProject(projectId, pageable);
                break;
            case "monthly":
                entries = projectLeaderboardRepository.findTopMonthlyPerformersByProject(projectId, pageable);
                break;
            case "all-time":
            default:
                entries = projectLeaderboardRepository.findTopPerformersByProject(projectId, pageable);
                break;
        }

        return entries.map(entry -> mapToResponse(entry, 0));
    }

    /**
     * Reset weekly points for all projects (called by scheduled job)
     */
    public void resetWeeklyPoints() {
        log.info("Resetting weekly points for all projects");
        // This would be implemented with a bulk update query
        // For now, we'll handle this in the scheduled job
    }

    /**
     * Reset monthly points for all projects (called by scheduled job)
     */
    public void resetMonthlyPoints() {
        log.info("Resetting monthly points for all projects");
        // This would be implemented with a bulk update query
        // For now, we'll handle this in the scheduled job
    }

    /**
     * Create new leaderboard entry
     */
    private ProjectLeaderboard createNewLeaderboardEntry(UUID projectId, UUID userId, int points) {
        log.debug("Creating new leaderboard entry for project {} user {}", projectId, userId);

        // Handle negative points by starting at 0 to avoid constraint violations
        int initialPoints = Math.max(0, points);

        return ProjectLeaderboard.builder()
                .projectId(projectId)
                .userId(userId)
                .weeklyPoints(initialPoints)
                .monthlyPoints(initialPoints)
                .allTimePoints(initialPoints)
                .bugsResolved(0)
                .currentStreak(0)
                .build();
    }

    /**
     * Map entity to response DTO
     */
    private LeaderboardEntryResponse mapToResponse(ProjectLeaderboard entry, int rank) {
        // Get actual user name from users table
        String userDisplayName = "Unknown User";
        try {
            Optional<User> user = userRepository.findById(entry.getUserId());
            if (user.isPresent()) {
                User userEntity = user.get();
                userDisplayName = userEntity.getFirstName() + " " + userEntity.getLastName();
                log.debug("Mapped user {} to display name: {}", entry.getUserId(), userDisplayName);
            } else {
                log.warn("User not found for leaderboard entry: {}", entry.getUserId());
            }
        } catch (Exception e) {
            log.error("Error fetching user name for leaderboard entry {}: {}", entry.getUserId(), e.getMessage());
        }

        return LeaderboardEntryResponse.builder()
                .leaderboardEntryId(entry.getLeaderboardEntryId())
                .projectId(entry.getProjectId())
                .userId(entry.getUserId())
                .weeklyPoints(entry.getWeeklyPoints())
                .monthlyPoints(entry.getMonthlyPoints())
                .allTimePoints(entry.getAllTimePoints())
                .bugsResolved(entry.getBugsResolved())
                .currentStreak(entry.getCurrentStreak())
                .updatedAt(entry.getUpdatedAt())
                .userDisplayName(userDisplayName)
                .rank(rank)
                .build();
    }

    /**
     * Map list of entities to response DTOs with ranking
     */
    private List<LeaderboardEntryResponse> mapToResponseList(List<ProjectLeaderboard> entries, String timeframe) {
        return entries.stream()
                .map(entry -> mapToResponse(entry, entries.indexOf(entry) + 1))
                .toList();
    }
}
