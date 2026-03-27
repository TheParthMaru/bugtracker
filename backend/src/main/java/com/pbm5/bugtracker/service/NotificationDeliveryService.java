package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.entity.*;
import com.pbm5.bugtracker.repository.NotificationDeliveryLogRepository;
import com.pbm5.bugtracker.exception.EmailDeliveryException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for orchestrating notification delivery across multiple channels.
 * 
 * Handles complete delivery workflow including:
 * - User preference checking for each channel
 * - Multi-channel delivery coordination (email, in-app, toast)
 * - Template processing and content generation
 * - Delivery tracking and error handling
 * - Retry logic for failed deliveries
 * 
 * Key Features:
 * - Preference-based channel filtering
 * - Template-driven content generation
 * - Comprehensive delivery logging
 * - Error handling with retry support
 * - WebSocket integration for real-time notifications
 * 
 * Business Rules:
 * - Only deliver via channels enabled in user preferences
 * - All deliveries are logged for audit and retry purposes
 * - Failed email deliveries are retried with exponential backoff
 * - In-app notifications are always created (stored in database)
 * - Toast notifications are sent via WebSocket for real-time display
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationDeliveryService {

    private final NotificationPreferencesService notificationPreferencesService;
    private final NotificationTemplateService notificationTemplateService;
    private final ResendEmailNotificationService resendEmailService;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final WebSocketNotificationService webSocketService;

    /**
     * Process and deliver a notification across all appropriate channels.
     * 
     * @param notification      The notification to deliver
     * @param templateVariables Variables for template processing
     */
    public void processNotification(UserNotification notification, Map<String, Object> templateVariables) {
        log.debug("Processing notification {} for user {} with event type {}",
                notification.getNotificationId(),
                notification.getUser().getId(),
                notification.getEventType());

        // Get user preferences
        NotificationPreferences preferences = notificationPreferencesService
                .getOrCreatePreferences(notification.getUser().getId());

        // Deliver via each enabled channel
        if (shouldSendEmail(notification, preferences)) {
            deliverEmailNotification(notification, templateVariables);
        }

        if (shouldSendInApp(notification, preferences)) {
            deliverInAppNotification(notification, templateVariables);
        }

        if (shouldSendToast(notification, preferences)) {
            deliverToastNotification(notification, templateVariables);
        }

        log.info("Completed notification delivery for notification {}", notification.getNotificationId());
    }

    /**
     * Deliver email notification.
     */
    private void deliverEmailNotification(UserNotification notification, Map<String, Object> templateVariables) {
        NotificationDeliveryLog deliveryLog = createDeliveryLog(notification, NotificationChannel.EMAIL);

        try {
            // Process email templates
            String subject = notificationTemplateService.processTemplate(
                    notification.getEventType(), "subject", templateVariables);
            String htmlContent = notificationTemplateService.processTemplate(
                    notification.getEventType(), "html", templateVariables);
            String textContent = notificationTemplateService.processTemplate(
                    notification.getEventType(), "text", templateVariables);

            // Set recipient email
            deliveryLog.setRecipientEmail(notification.getUser().getEmail());

            // Send email
            String externalId = resendEmailService.sendNotificationEmail(
                    notification.getUser().getEmail(),
                    subject,
                    htmlContent,
                    textContent);

            // Update delivery log with success
            deliveryLog.setStatus(DeliveryStatus.SENT);
            deliveryLog.setSentAt(LocalDateTime.now());
            deliveryLog.setExternalId(externalId);

            log.info("Email notification sent successfully for notification {}", notification.getNotificationId());

        } catch (EmailDeliveryException e) {
            // Handle email delivery failure
            deliveryLog.setStatus(DeliveryStatus.FAILED);
            deliveryLog.setFailedAt(LocalDateTime.now());
            deliveryLog.setErrorMessage(e.getMessage());
            deliveryLog.setNextRetryAt(calculateNextRetry(deliveryLog.getRetryCount()));

            log.error("Email notification failed for notification {}: {}",
                    notification.getNotificationId(), e.getMessage());
        } finally {
            deliveryLogRepository.save(deliveryLog);
        }
    }

    /**
     * Deliver in-app notification (stored in database + real-time via WebSocket).
     */
    private void deliverInAppNotification(UserNotification notification, Map<String, Object> templateVariables) {
        NotificationDeliveryLog deliveryLog = createDeliveryLog(notification, NotificationChannel.IN_APP);

        try {
            // Process in-app template for WebSocket delivery
            String inAppContent = notificationTemplateService.processTemplate(
                    notification.getEventType(), "inapp", templateVariables);

            // Send real-time notification via WebSocket
            webSocketService.sendNotificationToUser(notification.getUser().getId(), notification);

            // Also send processed in-app content
            webSocketService.sendInAppNotification(
                    notification.getUser().getId(),
                    inAppContent,
                    notification.getNotificationId());

            deliveryLog.setStatus(DeliveryStatus.DELIVERED);
            deliveryLog.setSentAt(LocalDateTime.now());
            deliveryLog.setDeliveredAt(LocalDateTime.now());

            log.debug("In-app notification delivered for notification {} (database + WebSocket)",
                    notification.getNotificationId());

        } catch (Exception e) {
            deliveryLog.setStatus(DeliveryStatus.FAILED);
            deliveryLog.setFailedAt(LocalDateTime.now());
            deliveryLog.setErrorMessage(e.getMessage());

            log.error("In-app notification failed for notification {}: {}",
                    notification.getNotificationId(), e.getMessage());
        } finally {
            deliveryLogRepository.save(deliveryLog);
        }
    }

    /**
     * Deliver toast notification via WebSocket.
     */
    private void deliverToastNotification(UserNotification notification, Map<String, Object> templateVariables) {
        NotificationDeliveryLog deliveryLog = createDeliveryLog(notification, NotificationChannel.TOAST);

        try {
            // Process toast template
            String toastContent = notificationTemplateService.processTemplate(
                    notification.getEventType(), "toast", templateVariables);

            // Send toast notification via WebSocket
            webSocketService.sendToastNotification(
                    notification.getUser().getId(),
                    notification.getTitle(),
                    toastContent,
                    "info");

            deliveryLog.setStatus(DeliveryStatus.DELIVERED);
            deliveryLog.setSentAt(LocalDateTime.now());
            deliveryLog.setDeliveredAt(LocalDateTime.now());

            log.debug("Toast notification delivered for notification {} via WebSocket",
                    notification.getNotificationId());

        } catch (Exception e) {
            deliveryLog.setStatus(DeliveryStatus.FAILED);
            deliveryLog.setFailedAt(LocalDateTime.now());
            deliveryLog.setErrorMessage(e.getMessage());

            log.error("Toast notification failed for notification {}: {}",
                    notification.getNotificationId(), e.getMessage());
        } finally {
            deliveryLogRepository.save(deliveryLog);
        }
    }

    /**
     * Check if email should be sent based on user preferences.
     */
    private boolean shouldSendEmail(UserNotification notification, NotificationPreferences preferences) {
        return preferences.getEmailEnabled() &&
                notificationPreferencesService.isEmailEnabledForEvent(
                        notification.getUser().getId(),
                        notification.getEventType());
    }

    /**
     * Check if in-app notification should be created.
     */
    private boolean shouldSendInApp(UserNotification notification, NotificationPreferences preferences) {
        return preferences.getInAppEnabled();
    }

    /**
     * Check if toast notification should be sent.
     */
    private boolean shouldSendToast(UserNotification notification, NotificationPreferences preferences) {
        return preferences.getToastEnabled();
    }

    /**
     * Create a delivery log entry.
     */
    private NotificationDeliveryLog createDeliveryLog(UserNotification notification, NotificationChannel channel) {
        return NotificationDeliveryLog.builder()
                .notification(notification)
                .channel(channel)
                .status(DeliveryStatus.PENDING)
                .build();
    }

    /**
     * Calculate next retry time with exponential backoff.
     */
    private LocalDateTime calculateNextRetry(int retryCount) {
        // Exponential backoff: 1min, 5min, 15min, then stop
        int delayMinutes = switch (retryCount) {
            case 0 -> 1;
            case 1 -> 5;
            case 2 -> 15;
            default -> 0; // No more retries after 3 attempts
        };

        return delayMinutes > 0 ? LocalDateTime.now().plusMinutes(delayMinutes) : null;
    }
}
