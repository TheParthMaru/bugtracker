package com.pbm5.bugtracker.entity;

/**
 * Enum representing notification delivery channels.
 * 
 * Defines the different ways notifications can be delivered:
 * - EMAIL: Email notifications via Resend
 * - IN_APP: In-app notifications displayed in the notification bell
 * - TOAST: Toast popup notifications for immediate feedback
 */
public enum NotificationChannel {
    EMAIL,
    IN_APP,
    TOAST
}
