package com.pbm5.bugtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user notifications.
 * 
 * Contains all notification details for API responses including:
 * - Notification metadata (ID, type, timestamps)
 * - Content (title, message, data)
 * - Status (read, dismissed)
 * - Related entity references for navigation
 * 
 * Used in:
 * - GET /api/bugtracker/v1/notifications
 * - Notification list responses
 * - WebSocket notification payloads
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long notificationId;
    private String eventType;
    private String title;
    private String message;
    private String data;

    private Boolean isRead;
    private Boolean isDismissed;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    // Related entity IDs for navigation
    private Long relatedBugId;
    private UUID relatedProjectId;
    private UUID relatedTeamId;
    private UUID relatedUserId;

    // Additional navigation fields for proper routing
    private String projectSlug;
    private Integer projectTicketNumber;
}
