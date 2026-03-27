package com.pbm5.bugtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.pbm5.bugtracker.entity.EmailFrequency;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user notification preferences.
 * 
 * Contains all user preference settings for API responses including:
 * - Channel preferences (email, in-app, toast)
 * - Event type preferences (bugs, projects, teams, gamification)
 * - Email-specific settings (frequency, timezone)
 * - Metadata (timestamps, IDs)
 * 
 * Used in:
 * - GET /api/bugtracker/v1/notification-preferences
 * - PUT /api/bugtracker/v1/notification-preferences (response)
 * - POST /api/bugtracker/v1/notification-preferences/reset (response)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferencesResponse {

    private Long preferenceId;
    private UUID userId;

    // Channel preferences
    private Boolean inAppEnabled;
    private Boolean emailEnabled;
    private Boolean toastEnabled;

    // Bug notification preferences
    private Boolean bugAssigned;
    private Boolean bugStatusChanged;
    private Boolean bugPriorityChanged;
    private Boolean bugCommented;
    private Boolean bugMentioned;
    private Boolean bugAttachmentAdded;

    private Boolean projectRoleChanged;
    private Boolean projectMemberJoined;

    private Boolean teamRoleChanged;
    private Boolean teamMemberJoined;

    // Gamification notification preferences
    private Boolean gamificationPoints;
    private Boolean gamificationAchievements;
    private Boolean gamificationLeaderboard;

    // Email specific settings
    private EmailFrequency emailFrequency;
    private String timezone;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
