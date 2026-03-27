package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.NotificationDeliveryLog;
import com.pbm5.bugtracker.entity.NotificationChannel;
import com.pbm5.bugtracker.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for NotificationDeliveryLog entity operations.
 * 
 * Provides data access methods for managing notification delivery tracking
 * including:
 * - Finding delivery logs by status and channel
 * - Retry logic support
 * - Delivery analytics and monitoring
 */
@Repository
public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, Long> {

    // Basic queries
    List<NotificationDeliveryLog> findByNotificationNotificationId(Long notificationId);

    List<NotificationDeliveryLog> findByChannel(NotificationChannel channel);

    List<NotificationDeliveryLog> findByStatus(DeliveryStatus status);

    // Retry queries
    List<NotificationDeliveryLog> findByStatusAndNextRetryAtBefore(DeliveryStatus status, LocalDateTime currentTime);

    @Query("SELECT d FROM NotificationDeliveryLog d WHERE d.status = 'FAILED' AND d.retryCount < 3 AND d.nextRetryAt <= :currentTime")
    List<NotificationDeliveryLog> findFailedDeliveriesReadyForRetry(@Param("currentTime") LocalDateTime currentTime);

    // External ID queries (for webhook correlation)
    Optional<NotificationDeliveryLog> findByExternalId(String externalId);

    List<NotificationDeliveryLog> findByExternalIdIsNotNull();

    // Channel-specific queries
    List<NotificationDeliveryLog> findByChannelAndStatus(NotificationChannel channel, DeliveryStatus status);

    @Query("SELECT d FROM NotificationDeliveryLog d WHERE d.channel = 'EMAIL' AND d.recipientEmail = :email")
    List<NotificationDeliveryLog> findEmailDeliveriesByRecipient(@Param("email") String email);

    // Analytics queries
    @Query("SELECT d.status, COUNT(d) FROM NotificationDeliveryLog d WHERE d.createdAt >= :startDate GROUP BY d.status")
    List<Object[]> getDeliveryStatsSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT d.channel, COUNT(d) FROM NotificationDeliveryLog d WHERE d.createdAt >= :startDate GROUP BY d.channel")
    List<Object[]> getChannelStatsSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(d.retryCount) FROM NotificationDeliveryLog d WHERE d.status = 'DELIVERED' AND d.createdAt >= :startDate")
    Double getAverageRetryCountSince(@Param("startDate") LocalDateTime startDate);

    // Cleanup queries
    @Query("DELETE FROM NotificationDeliveryLog d WHERE d.createdAt < :cutoffDate AND d.status IN ('DELIVERED', 'BOUNCED')")
    int deleteOldDeliveryLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Monitoring queries
    @Query("SELECT COUNT(d) FROM NotificationDeliveryLog d WHERE d.status = 'FAILED' AND d.retryCount >= 3")
    long countPermanentFailures();

    @Query("SELECT COUNT(d) FROM NotificationDeliveryLog d WHERE d.status = 'PENDING' AND d.createdAt < :thresholdTime")
    long countStuckDeliveries(@Param("thresholdTime") LocalDateTime thresholdTime);
}
