package com.pbm5.bugtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for unread notification count.
 * 
 * Simple response containing the count of unread notifications
 * for the current user.
 * 
 * Used in:
 * - GET /api/bugtracker/v1/notifications/unread-count
 * - Notification bell badge updates
 * - Real-time count updates via WebSocket
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {
    private long count;
}
