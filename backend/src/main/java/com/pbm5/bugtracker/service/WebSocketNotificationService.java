package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.pbm5.bugtracker.dto.NotificationResponse;
import com.pbm5.bugtracker.entity.UserNotification;

import java.util.Map;
import java.util.UUID;

/**
 * Service for delivering real-time notifications via WebSocket.
 * 
 * Handles:
 * - Real-time notification delivery to connected users
 * - Toast notification broadcasting
 * - Unread count updates
 * - User-specific notification channels
 * 
 * WebSocket Channels:
 * - /user/{userId}/notifications/new - New notification delivery
 * - /user/{userId}/notifications/count - Unread count updates
 * - /user/{userId}/notifications/toast - Toast notifications
 * 
 * Integration:
 * - Used by NotificationDeliveryService for real-time delivery
 * - Converts entities to DTOs for frontend consumption
 * - Handles connection state gracefully (no errors if user offline)
 * 
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a new notification to a specific user via WebSocket.
     * 
     * Delivers the notification to the user's personal channel.
     * If the user is not connected, the message is silently dropped.
     * 
     * @param userId       The target user ID
     * @param notification The notification to send
     */
    public void sendNotificationToUser(UUID userId, UserNotification notification) {
        try {
            log.debug("Sending real-time notification {} to user {}",
                    notification.getNotificationId(), userId);

            // Convert to DTO for frontend
            NotificationResponse response = convertToResponse(notification);

            // Send to user-specific channel
            String destination = String.format("/user/%s/notifications/new", userId);
            messagingTemplate.convertAndSend(destination, response);

            log.debug("Real-time notification sent to user {} via WebSocket", userId);

        } catch (Exception e) {
            // Don't fail the entire notification flow if WebSocket fails
            log.warn("Failed to send real-time notification to user {} via WebSocket: {}",
                    userId, e.getMessage());
        }
    }

    /**
     * Send a toast notification to a specific user.
     * 
     * Toast notifications are lightweight, temporary notifications
     * that appear as popups in the frontend.
     * 
     * @param userId  The target user ID
     * @param title   Toast notification title
     * @param message Toast notification message
     * @param type    Toast type (success, info, warning, error)
     */
    public void sendToastNotification(UUID userId, String title, String message, String type) {
        try {
            log.debug("Sending toast notification to user {}: {} - {}", userId, title, message);

            Map<String, Object> toastData = Map.of(
                    "title", title,
                    "message", message,
                    "type", type != null ? type : "info",
                    "timestamp", System.currentTimeMillis());

            String destination = String.format("/user/%s/notifications/toast", userId);
            messagingTemplate.convertAndSend(destination, toastData);

            log.debug("Toast notification sent to user {} via WebSocket", userId);

        } catch (Exception e) {
            log.warn("Failed to send toast notification to user {} via WebSocket: {}",
                    userId, e.getMessage());
        }
    }

    /**
     * Send updated unread notification count to a specific user.
     * 
     * Used when notifications are marked as read/unread or new notifications
     * arrive.
     * 
     * @param userId      The target user ID
     * @param unreadCount The current unread count
     */
    public void sendUnreadCountUpdate(UUID userId, long unreadCount) {
        try {
            log.debug("Sending unread count update to user {}: {}", userId, unreadCount);

            Map<String, Object> countData = Map.of(
                    "count", unreadCount,
                    "timestamp", System.currentTimeMillis());

            String destination = String.format("/user/%s/notifications/count", userId);
            messagingTemplate.convertAndSend(destination, countData);

            log.debug("Unread count update sent to user {} via WebSocket", userId);

        } catch (Exception e) {
            log.warn("Failed to send unread count update to user {} via WebSocket: {}",
                    userId, e.getMessage());
        }
    }

    /**
     * Send a processed template notification via WebSocket.
     * 
     * Used for in-app notifications that have been processed through templates.
     * 
     * @param userId           The target user ID
     * @param processedContent The processed notification content
     * @param notificationId   The notification ID for tracking
     */
    public void sendInAppNotification(UUID userId, String processedContent, Long notificationId) {
        try {
            log.debug("Sending in-app notification {} to user {}", notificationId, userId);

            Map<String, Object> inAppData = Map.of(
                    "content", processedContent,
                    "notificationId", notificationId,
                    "timestamp", System.currentTimeMillis(),
                    "type", "in-app");

            String destination = String.format("/user/%s/notifications/in-app", userId);
            messagingTemplate.convertAndSend(destination, inAppData);

            log.debug("In-app notification sent to user {} via WebSocket", userId);

        } catch (Exception e) {
            log.warn("Failed to send in-app notification to user {} via WebSocket: {}",
                    userId, e.getMessage());
        }
    }

    /**
     * Broadcast a global notification to all connected users.
     * 
     * Use sparingly - only for system-wide announcements.
     * 
     * @param title   The notification title
     * @param message The notification message
     * @param type    The notification type
     */
    public void broadcastGlobalNotification(String title, String message, String type) {
        try {
            log.info("Broadcasting global notification: {} - {}", title, message);

            Map<String, Object> globalData = Map.of(
                    "title", title,
                    "message", message,
                    "type", type != null ? type : "info",
                    "timestamp", System.currentTimeMillis(),
                    "global", true);

            messagingTemplate.convertAndSend("/topic/notifications/global", globalData);

            log.info("Global notification broadcasted via WebSocket");

        } catch (Exception e) {
            log.error("Failed to broadcast global notification via WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Check if WebSocket messaging is available.
     * 
     * @return true if WebSocket service is ready
     */
    public boolean isWebSocketAvailable() {
        return messagingTemplate != null;
    }

    /**
     * Convert UserNotification entity to NotificationResponse DTO.
     */
    private NotificationResponse convertToResponse(UserNotification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .eventType(notification.getEventType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .isRead(notification.getIsRead())
                .isDismissed(notification.getIsDismissed())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .relatedBugId(notification.getRelatedBug() != null ? notification.getRelatedBug().getId() : null)
                .relatedProjectId(
                        notification.getRelatedProject() != null ? notification.getRelatedProject().getId() : null)
                .relatedTeamId(notification.getRelatedTeam() != null ? notification.getRelatedTeam().getId() : null)
                .relatedUserId(notification.getRelatedUser() != null ? notification.getRelatedUser().getId() : null)
                .projectSlug(
                        notification.getRelatedProject() != null ? notification.getRelatedProject().getProjectSlug()
                                : null)
                .projectTicketNumber(
                        notification.getRelatedBug() != null ? notification.getRelatedBug().getProjectTicketNumber()
                                : null)
                .build();
    }
}
