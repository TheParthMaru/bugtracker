package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for leaderboard entry details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryResponse {

    private UUID leaderboardEntryId;
    private UUID projectId;
    private UUID userId;
    private Integer weeklyPoints;
    private Integer monthlyPoints;
    private Integer allTimePoints;
    private Integer bugsResolved;
    private Integer currentStreak;
    private LocalDateTime updatedAt;
    private String userDisplayName;
    private Integer rank;
}

