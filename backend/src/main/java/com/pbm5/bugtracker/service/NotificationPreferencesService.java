package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.entity.NotificationPreferences;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.repository.NotificationPreferencesRepository;
import com.pbm5.bugtracker.repository.UserRepository;

import java.util.UUID;

/**
 * Service class for managing user notification preferences.
 * 
 * Handles complete preference lifecycle including:
 * - Default preference creation for new users
 * - Preference updates with validation
 * - Preference queries and filtering
 * - Reset to default functionality
 * 
 * Key Features:
 * - Automatic default preference creation
 * - Granular preference management
 * - Validation of preference combinations
 * - Audit logging for preference changes
 * 
 * Business Rules:
 * - Every user gets default preferences on first access
 * - Preferences are created programmatically, not via seed data
 * - Users can customize all preference settings
 * - Preference changes take effect immediately
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository notificationPreferencesRepository;
    private final UserRepository userRepository;

    /**
     * Get or create notification preferences for a user.
     * If preferences don't exist, create them with default values.
     */
    @Transactional
    public NotificationPreferences getOrCreatePreferences(UUID userId) {
        return notificationPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    /**
     * Update notification preferences for a user.
     */
    public NotificationPreferences updatePreferences(UUID userId, NotificationPreferences updatedPreferences) {
        NotificationPreferences existing = getOrCreatePreferences(userId);

        // Update all preference fields
        existing.setInAppEnabled(updatedPreferences.getInAppEnabled());
        existing.setEmailEnabled(updatedPreferences.getEmailEnabled());
        existing.setToastEnabled(updatedPreferences.getToastEnabled());

        // Bug preferences
        existing.setBugAssigned(updatedPreferences.getBugAssigned());
        existing.setBugStatusChanged(updatedPreferences.getBugStatusChanged());
        existing.setBugPriorityChanged(updatedPreferences.getBugPriorityChanged());
        existing.setBugCommented(updatedPreferences.getBugCommented());
        existing.setBugMentioned(updatedPreferences.getBugMentioned());
        existing.setBugAttachmentAdded(updatedPreferences.getBugAttachmentAdded());

        existing.setProjectRoleChanged(updatedPreferences.getProjectRoleChanged());
        existing.setProjectMemberJoined(updatedPreferences.getProjectMemberJoined());

        existing.setTeamRoleChanged(updatedPreferences.getTeamRoleChanged());
        existing.setTeamMemberJoined(updatedPreferences.getTeamMemberJoined());

        // Gamification preferences
        existing.setGamificationPoints(updatedPreferences.getGamificationPoints());
        existing.setGamificationAchievements(updatedPreferences.getGamificationAchievements());
        existing.setGamificationLeaderboard(updatedPreferences.getGamificationLeaderboard());

        // Email settings
        existing.setEmailFrequency(updatedPreferences.getEmailFrequency());
        existing.setTimezone(updatedPreferences.getTimezone());

        NotificationPreferences saved = notificationPreferencesRepository.save(existing);
        log.info("Updated notification preferences for user {}", userId);
        return saved;
    }

    /**
     * Reset preferences to default values for a user.
     */
    public NotificationPreferences resetToDefaults(UUID userId) {
        notificationPreferencesRepository.findByUserId(userId)
                .ifPresent(notificationPreferencesRepository::delete);

        NotificationPreferences defaultPrefs = createDefaultPreferences(userId);
        log.info("Reset notification preferences to defaults for user {}", userId);
        return defaultPrefs;
    }

    /**
     * Check if user has email notifications enabled for a specific event type.
     */
    @Transactional(readOnly = true)
    public boolean isEmailEnabledForEvent(UUID userId, String eventType) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        if (!prefs.getEmailEnabled()) {
            return false;
        }

        return switch (eventType) {
            case "BUG_ASSIGNED" -> prefs.getBugAssigned();
            case "BUG_STATUS_CHANGED" -> prefs.getBugStatusChanged();
            case "BUG_PRIORITY_CHANGED" -> prefs.getBugPriorityChanged();
            case "BUG_COMMENTED" -> prefs.getBugCommented();
            case "BUG_MENTIONED" -> prefs.getBugMentioned();
            case "BUG_ATTACHMENT_ADDED" -> prefs.getBugAttachmentAdded();

            case "PROJECT_ROLE_CHANGED" -> prefs.getProjectRoleChanged();
            case "PROJECT_MEMBER_JOINED" -> prefs.getProjectMemberJoined();

            case "TEAM_ROLE_CHANGED" -> prefs.getTeamRoleChanged();
            case "TEAM_MEMBER_JOINED" -> prefs.getTeamMemberJoined();
            case "GAMIFICATION_POINTS" -> prefs.getGamificationPoints();
            case "GAMIFICATION_ACHIEVEMENTS" -> prefs.getGamificationAchievements();
            case "GAMIFICATION_LEADERBOARD" -> prefs.getGamificationLeaderboard();
            default -> true; // Default to enabled for unknown event types
        };
    }

    /**
     * Create default notification preferences for a new user.
     * This method is called programmatically, not via seed data.
     */
    private NotificationPreferences createDefaultPreferences(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        NotificationPreferences preferences = NotificationPreferences.builder()
                .user(user)
                .build(); // Builder will apply all default values

        NotificationPreferences saved = notificationPreferencesRepository.save(preferences);
        log.info("Created default notification preferences for user {}", userId);
        return saved;
    }
}
