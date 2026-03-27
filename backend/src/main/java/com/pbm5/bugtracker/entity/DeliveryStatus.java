package com.pbm5.bugtracker.entity;

/**
 * Enum representing notification delivery status.
 * 
 * Tracks the status of notification delivery:
 * - PENDING: Notification queued for delivery
 * - SENT: Notification sent to external service (e.g., Resend)
 * - DELIVERED: Notification successfully delivered (confirmed via webhook)
 * - FAILED: Notification delivery failed (will retry)
 * - BOUNCED: Notification bounced (permanent failure)
 */
public enum DeliveryStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    BOUNCED
}
