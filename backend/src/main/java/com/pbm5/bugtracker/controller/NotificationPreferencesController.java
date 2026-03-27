package com.pbm5.bugtracker.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.pbm5.bugtracker.dto.NotificationPreferencesResponse;
import com.pbm5.bugtracker.dto.UpdateNotificationPreferencesRequest;
import com.pbm5.bugtracker.entity.NotificationPreferences;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.service.NotificationPreferencesService;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user notification preferences operations.
 * 
 * Provides endpoints for:
 * - Getting user notification preferences
 * - Updating notification preferences with validation
 * - Resetting preferences to default values
 * 
 * All endpoints require authentication and operate on the current user's
 * preferences.
 * 
 * Base URL: /api/bugtracker/v1/notification-preferences
 * 
 * Security:
 * - All endpoints require valid JWT authentication
 * - Users can only manage their own preferences
 * - No admin privileges required - user-scoped operations only
 */
@RestController
@RequestMapping("/api/bugtracker/v1/notification-preferences")
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesController {

    private final NotificationPreferencesService notificationPreferencesService;

    /**
     * Get current user's notification preferences.
     * Creates default preferences if none exist.
     * 
     * @param authentication Current user authentication
     * @return 200 OK with user's notification preferences
     */
    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        log.debug("Getting notification preferences for user {}", userId);

        NotificationPreferences preferences = notificationPreferencesService.getOrCreatePreferences(userId);
        NotificationPreferencesResponse response = convertToResponse(preferences);

        log.debug("Retrieved notification preferences for user {}", userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user's notification preferences.
     * 
     * @param request        The preference update request with validation
     * @param authentication Current user authentication
     * @return 200 OK with updated preferences
     */
    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @RequestBody @Valid UpdateNotificationPreferencesRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        log.debug("Updating notification preferences for user {}", userId);

        // Convert request to entity
        NotificationPreferences updatedPreferences = convertFromRequest(request);

        // Update preferences
        NotificationPreferences saved = notificationPreferencesService.updatePreferences(userId, updatedPreferences);
        NotificationPreferencesResponse response = convertToResponse(saved);

        log.info("Updated notification preferences for user {}", userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset user's notification preferences to default values.
     * 
     * @param authentication Current user authentication
     * @return 200 OK with reset preferences
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetToDefaults(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        log.debug("Resetting notification preferences to defaults for user {}", userId);

        NotificationPreferences defaultPreferences = notificationPreferencesService.resetToDefaults(userId);
        NotificationPreferencesResponse response = convertToResponse(defaultPreferences);

        log.info("Reset notification preferences to defaults for user {}", userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Preferences reset to defaults",
                "preferences", response));
    }

    /**
     * Convert NotificationPreferences entity to response DTO.
     */
    private NotificationPreferencesResponse convertToResponse(NotificationPreferences preferences) {
        return NotificationPreferencesResponse.builder()
                .preferenceId(preferences.getPreferenceId())
                .userId(preferences.getUser().getId())
                .inAppEnabled(preferences.getInAppEnabled())
                .emailEnabled(preferences.getEmailEnabled())
                .toastEnabled(preferences.getToastEnabled())
                .bugAssigned(preferences.getBugAssigned())
                .bugStatusChanged(preferences.getBugStatusChanged())
                .bugPriorityChanged(preferences.getBugPriorityChanged())
                .bugCommented(preferences.getBugCommented())
                .bugMentioned(preferences.getBugMentioned())
                .bugAttachmentAdded(preferences.getBugAttachmentAdded())

                .projectRoleChanged(preferences.getProjectRoleChanged())
                .projectMemberJoined(preferences.getProjectMemberJoined())

                .teamRoleChanged(preferences.getTeamRoleChanged())
                .teamMemberJoined(preferences.getTeamMemberJoined())
                .gamificationPoints(preferences.getGamificationPoints())
                .gamificationAchievements(preferences.getGamificationAchievements())
                .gamificationLeaderboard(preferences.getGamificationLeaderboard())
                .emailFrequency(preferences.getEmailFrequency())
                .timezone(preferences.getTimezone())
                .createdAt(preferences.getCreatedAt())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }

    /**
     * Convert request DTO to NotificationPreferences entity.
     */
    private NotificationPreferences convertFromRequest(UpdateNotificationPreferencesRequest request) {
        return NotificationPreferences.builder()
                .inAppEnabled(request.getInAppEnabled())
                .emailEnabled(request.getEmailEnabled())
                .toastEnabled(request.getToastEnabled())
                .bugAssigned(request.getBugAssigned())
                .bugStatusChanged(request.getBugStatusChanged())
                .bugPriorityChanged(request.getBugPriorityChanged())
                .bugCommented(request.getBugCommented())
                .bugMentioned(request.getBugMentioned())
                .bugAttachmentAdded(request.getBugAttachmentAdded())

                .projectRoleChanged(request.getProjectRoleChanged())
                .projectMemberJoined(request.getProjectMemberJoined())

                .teamRoleChanged(request.getTeamRoleChanged())
                .teamMemberJoined(request.getTeamMemberJoined())
                .gamificationPoints(request.getGamificationPoints())
                .gamificationAchievements(request.getGamificationAchievements())
                .gamificationLeaderboard(request.getGamificationLeaderboard())
                .emailFrequency(request.getEmailFrequency())
                .timezone(request.getTimezone())
                .build();
    }
}
