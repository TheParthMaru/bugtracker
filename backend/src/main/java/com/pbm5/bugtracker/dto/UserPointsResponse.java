package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user gamification points and statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPointsResponse {

    private UUID userId;
    private Integer totalPoints;
    private Integer currentStreak;
    private Integer maxStreak;
    private Integer bugsResolved;
    private LocalDateTime lastActivity;
    private String userDisplayName;
}

