package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.entity.*;
import com.pbm5.bugtracker.repository.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Core service class for notification management operations.
 * 
 * Handles complete notification lifecycle including:
 * - Notification creation and event processing
 * - User preference checking and filtering
 * - Multi-channel notification routing (email, in-app, toast)
 * - Template processing and content generation
 * - Delivery tracking and retry logic
 * 
 * Key Features:
 * - Event-driven notification creation
 * - User preference-based filtering
 * - Multi-channel delivery coordination
 * - Template-based content generation
 * - Comprehensive delivery tracking
 * 
 * Business Rules:
 * - Notifications are only created if user preferences allow
 * - Templates are processed with variable substitution
 * - Failed deliveries are retried with exponential backoff
 * - All notification events are audited for compliance
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;
    private final UserRepository userRepository;
    private final NotificationDeliveryService notificationDeliveryService;
    private final WebSocketNotificationService webSocketService;

    /**
     * Create a new notification for a user based on an event.
     * 
     * @param userId         The user to notify
     * @param eventType      The type of event (e.g., "BUG_ASSIGNED")
     * @param title          Short notification title
     * @param message        Detailed notification message
     * @param data           Additional JSON data for the notification
     * @param relatedBug     Optional related bug
     * @param relatedProject Optional related project
     * @param relatedTeam    Optional related team
     * @param relatedUser    Optional related user (who triggered the event)
     * @return The created notification
     */
    public UserNotification createNotification(
            UUID userId,
            String eventType,
            String title,
            String message,
            String data,
            Bug relatedBug,
            Project relatedProject,
            Team relatedTeam,
            User relatedUser) {

        log.info(
                "NotificationService.createNotification - Starting notification creation: userId={}, eventType={}, title='{}'",
                userId, eventType, title);

        // Check if user preferences allow this notification
        if (!shouldCreateNotification(userId, eventType)) {
            log.info(
                    "NotificationService.createNotification - Notification creation skipped due to user preferences: userId={}, eventType={}",
                    userId, eventType);
            return null;
        }

        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        log.info("NotificationService.createNotification - Target user found: id={}, email={}, name={} {}",
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());

        // Create the notification
        UserNotification notification = UserNotification.builder()
                .user(user)
                .eventType(eventType)
                .title(title)
                .message(message)
                .data(data)
                .relatedBug(relatedBug)
                .relatedProject(relatedProject)
                .relatedTeam(relatedTeam)
                .relatedUser(relatedUser)
                .build();

        notification = userNotificationRepository.save(notification);
        log.info(
                "NotificationService.createNotification - Notification saved: id={}, userId={}, userEmail={}, eventType={}, title='{}'",
                notification.getNotificationId(), userId, user.getEmail(), eventType, title);

        return notification;
    }

    /**
     * Create a notification and immediately deliver it across all channels.
     * 
     * @param userId            The user to notify
     * @param eventType         The type of event (e.g., "BUG_ASSIGNED")
     * @param title             Short notification title
     * @param message           Detailed notification message
     * @param data              Additional JSON data for the notification
     * @param templateVariables Variables for template processing
     * @param relatedBug        Optional related bug
     * @param relatedProject    Optional related project
     * @param relatedTeam       Optional related team
     * @param relatedUser       Optional related user (who triggered the event)
     * @return The created notification
     */
    public UserNotification createAndDeliverNotification(
            UUID userId,
            String eventType,
            String title,
            String message,
            String data,
            Map<String, Object> templateVariables,
            Bug relatedBug,
            Project relatedProject,
            Team relatedTeam,
            User relatedUser) {

        log.info("NotificationService.createAndDeliverNotification - Starting: userId={}, eventType={}, title='{}'",
                userId, eventType, title);

        // Create the notification
        UserNotification notification = createNotification(
                userId, eventType, title, message, data,
                relatedBug, relatedProject, relatedTeam, relatedUser);

        if (notification != null) {
            log.info(
                    "NotificationService.createAndDeliverNotification - Notification created, starting delivery: id={}, userId={}, eventType={}",
                    notification.getNotificationId(), userId, eventType);

            // Deliver across all channels
            notificationDeliveryService.processNotification(notification, templateVariables);

            log.info(
                    "NotificationService.createAndDeliverNotification - Delivery completed: id={}, userId={}, eventType={}",
                    notification.getNotificationId(), userId, eventType);
        } else {
            log.info(
                    "NotificationService.createAndDeliverNotification - Notification creation was skipped, no delivery needed: userId={}, eventType={}",
                    userId, eventType);
        }

        return notification;
    }

    /**
     * Get paginated notifications for a user.
     */
    @Transactional(readOnly = true)
    public Page<UserNotification> getUserNotifications(UUID userId, Pageable pageable) {
        return userNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public Page<UserNotification> getUnreadNotifications(UUID userId, Pageable pageable) {
        return userNotificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get unread notification count for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return userNotificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a notification as read.
     */
    public void markAsRead(Long notificationId, UUID userId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        // Verify the notification belongs to the user
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Notification does not belong to user: " + userId);
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            userNotificationRepository.save(notification);

            // Send updated unread count via WebSocket
            long newUnreadCount = getUnreadCount(userId);
            webSocketService.sendUnreadCountUpdate(userId, newUnreadCount);

            log.debug("Marked notification {} as read for user {} (new unread count: {})",
                    notificationId, userId, newUnreadCount);
        }
    }

    /**
     * Mark all notifications as read for a user.
     */
    public int markAllAsRead(UUID userId) {
        int updatedCount = userNotificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());

        if (updatedCount > 0) {
            // Send updated unread count via WebSocket (should be 0 after marking all as
            // read)
            webSocketService.sendUnreadCountUpdate(userId, 0L);
        }

        log.info("Marked {} notifications as read for user {} (new unread count: 0)", updatedCount, userId);
        return updatedCount;
    }

    /**
     * Dismiss a notification (soft delete).
     */
    public void dismissNotification(Long notificationId, UUID userId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        // Verify the notification belongs to the user
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Notification does not belong to user: " + userId);
        }

        notification.setIsDismissed(true);
        userNotificationRepository.save(notification);
        log.debug("Dismissed notification {} for user {}", notificationId, userId);
    }

    /**
     * Check if a notification should be created based on user preferences.
     */
    private boolean shouldCreateNotification(UUID userId, String eventType) {
        return notificationPreferencesRepository.findByUserId(userId)
                .map(prefs -> isEventTypeEnabled(prefs, eventType))
                .orElse(true); // Default to true if no preferences found
    }

    /**
     * Check if a specific event type is enabled in user preferences.
     */
    private boolean isEventTypeEnabled(NotificationPreferences prefs, String eventType) {
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
}
