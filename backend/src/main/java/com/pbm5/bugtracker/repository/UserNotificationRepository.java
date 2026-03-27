package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for UserNotification entity operations.
 * 
 * Provides data access methods for managing user notifications including:
 * - Finding notifications by user with filtering and pagination
 * - Unread notification counts and queries
 * - Event type and date range filtering
 * - Bulk operations for marking notifications as read
 */
@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

        // Basic user notification queries
        Page<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

        List<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

        // Unread notification queries
        Page<UserNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

        List<UserNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

        long countByUserIdAndIsReadFalse(UUID userId);

        // Event type filtering
        Page<UserNotification> findByUserIdAndEventTypeOrderByCreatedAtDesc(UUID userId, String eventType,
                        Pageable pageable);

        List<UserNotification> findByUserIdAndEventTypeAndIsReadFalse(UUID userId, String eventType);

        // Date range queries
        @Query("SELECT n FROM UserNotification n WHERE n.user.id = :userId AND n.createdAt >= :startDate ORDER BY n.createdAt DESC")
        List<UserNotification> findByUserIdAndCreatedAtAfter(@Param("userId") UUID userId,
                        @Param("startDate") LocalDateTime startDate);

        // Related entity queries
        List<UserNotification> findByRelatedBugId(Long bugId);

        List<UserNotification> findByRelatedProjectId(UUID projectId);

        List<UserNotification> findByRelatedTeamId(UUID teamId);

        // Bulk operations
        @Modifying
        @Query("UPDATE UserNotification n SET n.isRead = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.isRead = false")
        int markAllAsReadByUserId(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);

        @Modifying
        @Query("UPDATE UserNotification n SET n.isRead = true, n.readAt = :readAt WHERE n.notificationId IN :notificationIds")
        int markAsReadByIds(@Param("notificationIds") List<Long> notificationIds,
                        @Param("readAt") LocalDateTime readAt);

        // Cleanup queries
        @Query("DELETE FROM UserNotification n WHERE n.createdAt < :cutoffDate AND n.isRead = true")
        int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

        // Statistics queries
        @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.user.id = :userId AND n.createdAt >= :startDate")
        long countByUserIdSince(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate);
}
