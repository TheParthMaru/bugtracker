package com.pbm5.bugtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.pbm5.bugtracker.entity.EmailFrequency;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating user notification preferences.
 * 
 * Contains all user preference settings with validation including:
 * - Channel preferences (email, in-app, toast)
 * - Event type preferences (bugs, projects, teams, gamification)
 * - Email-specific settings (frequency, timezone)
 * 
 * Validation Rules:
 * - All boolean preferences are required (not nullable)
 * - Email frequency must be valid enum value
 * - Timezone must be valid timezone identifier
 * 
 * Used in:
 * - PUT /api/bugtracker/v1/notification-preferences
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNotificationPreferencesRequest {

    // Channel preferences
    @NotNull(message = "In-app notifications preference is required")
    private Boolean inAppEnabled;

    @NotNull(message = "Email notifications preference is required")
    private Boolean emailEnabled;

    @NotNull(message = "Toast notifications preference is required")
    private Boolean toastEnabled;

    // Bug notification preferences
    @NotNull(message = "Bug assignment notifications preference is required")
    private Boolean bugAssigned;

    @NotNull(message = "Bug status change notifications preference is required")
    private Boolean bugStatusChanged;

    @NotNull(message = "Bug priority change notifications preference is required")
    private Boolean bugPriorityChanged;

    @NotNull(message = "Bug comment notifications preference is required")
    private Boolean bugCommented;

    @NotNull(message = "Bug mention notifications preference is required")
    private Boolean bugMentioned;

    @NotNull(message = "Bug attachment notifications preference is required")
    private Boolean bugAttachmentAdded;

    @NotNull(message = "Project role change notifications preference is required")
    private Boolean projectRoleChanged;

    @NotNull(message = "Project member joined notifications preference is required")
    private Boolean projectMemberJoined;

    @NotNull(message = "Team role change notifications preference is required")
    private Boolean teamRoleChanged;

    @NotNull(message = "Team member joined notifications preference is required")
    private Boolean teamMemberJoined;

    // Gamification notification preferences
    @NotNull(message = "Gamification points notifications preference is required")
    private Boolean gamificationPoints;

    @NotNull(message = "Gamification achievements notifications preference is required")
    private Boolean gamificationAchievements;

    @NotNull(message = "Gamification leaderboard notifications preference is required")
    private Boolean gamificationLeaderboard;

    // Email specific settings
    @NotNull(message = "Email frequency is required")
    private EmailFrequency emailFrequency;

    @NotNull(message = "Timezone is required")
    @Pattern(regexp = "^[A-Za-z]+/[A-Za-z_]+$|^UTC$", message = "Timezone must be a valid timezone identifier (e.g., UTC, America/New_York, Europe/London)")
    private String timezone;
}
