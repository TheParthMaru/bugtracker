package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing notification delivery tracking log.
 * 
 * This entity tracks the delivery status of notifications across different
 * channels:
 * - Email delivery via Resend
 * - In-app notification creation
 * - Toast notification display
 * 
 * Includes retry logic and error tracking for failed deliveries.
 */
@Entity
@Table(name = "notification_delivery_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long deliveryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private UserNotification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "recipient_email")
    private String recipientEmail;

    // Delivery timestamps
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "external_id")
    private String externalId; // Resend message ID

    // Retry information
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public String toString() {
        return "NotificationDeliveryLog{" +
                "deliveryId=" + deliveryId +
                ", channel=" + channel +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
