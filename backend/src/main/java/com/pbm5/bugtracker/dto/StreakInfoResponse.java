package com.pbm5.bugtracker.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user streak information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakInfoResponse {

    private UUID userId;
    private Integer currentStreak;
    private Integer maxStreak;
    private LocalDate lastLoginDate;
    private LocalDate streakStartDate;
}

