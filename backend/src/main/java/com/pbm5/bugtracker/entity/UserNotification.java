package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing a notification sent to a user.
 * 
 * This entity stores all user notifications including:
 * - Bug-related notifications (assignments, comments, mentions)
 * - Project-related notifications (invitations, role changes)
 * - Team-related notifications (invitations, membership changes)
 * - Gamification notifications (points, achievements)
 * 
 * The notification data is stored as JSONB for flexibility and includes
 * references to related entities for navigation purposes.
 */
@Entity
@Table(name = "user_notifications")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String data;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "is_dismissed", nullable = false)
    @Builder.Default
    private Boolean isDismissed = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // Related entity references for navigation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_bug_id")
    private Bug relatedBug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_project_id")
    private Project relatedProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_team_id")
    private Team relatedTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_user_id")
    private User relatedUser;

    @Override
    public String toString() {
        return "UserNotification{" +
                "notificationId=" + notificationId +
                ", eventType='" + eventType + '\'' +
                ", title='" + title + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}
