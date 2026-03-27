package com.pbm5.bugtracker.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.pbm5.bugtracker.dto.NotificationResponse;
import com.pbm5.bugtracker.dto.UnreadCountResponse;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.entity.UserNotification;
import com.pbm5.bugtracker.service.NotificationService;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user notification operations.
 * 
 * Provides endpoints for:
 * - Retrieving user notifications with pagination and filtering
 * - Managing notification read/unread status
 * - Getting unread notification counts
 * - Dismissing notifications
 * 
 * All endpoints require authentication and operate on the current user's
 * notifications.
 * 
 * Base URL: /api/bugtracker/v1/notifications
 * 
 * Security:
 * - All endpoints require valid JWT authentication
 * - Users can only access their own notifications
 * - No admin privileges required - user-scoped operations only
 */
@RestController
@RequestMapping("/api/bugtracker/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get user's notifications with pagination and filtering.
     * 
     * @param page           Optional page number (default: 0)
     * @param size           Optional page size (default: 20)
     * @param unreadOnly     Optional filter for unread notifications only
     * @param type           Optional filter by event type
     * @param authentication Current user authentication
     * @return 200 OK with paginated notifications
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String type,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        log.debug("Getting notifications for user {} (page: {}, size: {}, unreadOnly: {}, type: {})",
                userId, page, size, unreadOnly, type);

        // Create pageable with bounds checking
        size = Math.min(size, 100); // Max 100 items per page
        Pageable pageable = Pageable.ofSize(size).withPage(page);

        Page<UserNotification> notifications;
        if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationService.getUnreadNotifications(userId, pageable);
        } else {
            notifications = notificationService.getUserNotifications(userId, pageable);
        }

        // Convert to response DTOs
        Page<NotificationResponse> response = notifications.map(this::convertToResponse);

        log.debug("Retrieved {} notifications for user {}", response.getNumberOfElements(), userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get unread notification count for current user.
     * 
     * @param authentication Current user authentication
     * @return 200 OK with unread count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        long unreadCount = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(new UnreadCountResponse(unreadCount));
    }

    /**
     * Mark a specific notification as read.
     * 
     * @param notificationId The notification ID to mark as read
     * @param authentication Current user authentication
     * @return 204 No Content on success
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        log.debug("Marking notification {} as read for user {}", notificationId, userId);

        notificationService.markAsRead(notificationId, userId);

        log.debug("Notification {} marked as read for user {}", notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mark all notifications as read for current user.
     * 
     * @param authentication Current user authentication
     * @return 200 OK with count of notifications marked as read
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        log.debug("Marking all notifications as read for user {}", userId);

        int updatedCount = notificationService.markAllAsRead(userId);

        log.info("Marked {} notifications as read for user {}", updatedCount, userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "updatedCount", updatedCount,
                "message", "All notifications marked as read"));
    }

    /**
     * Dismiss (soft delete) a notification.
     * 
     * @param notificationId The notification ID to dismiss
     * @param authentication Current user authentication
     * @return 204 No Content on success
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> dismissNotification(
            @PathVariable Long notificationId,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        log.debug("Dismissing notification {} for user {}", notificationId, userId);

        notificationService.dismissNotification(notificationId, userId);

        log.debug("Notification {} dismissed for user {}", notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convert UserNotification entity to response DTO.
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
