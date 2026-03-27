package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing user notification preferences.
 * 
 * This entity stores user preferences for:
 * - Notification channels (in-app, email, toast)
 * - Event type preferences (what events to be notified about)
 * - Email-specific settings (frequency, timezone)
 * 
 * Each user has exactly one preferences record with sensible defaults.
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Channel preferences (HOW to receive notifications)
    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private Boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = false;

    @Column(name = "toast_enabled", nullable = false)
    @Builder.Default
    private Boolean toastEnabled = true;

    // Bug notification preferences
    @Column(name = "bug_assigned", nullable = false)
    @Builder.Default
    private Boolean bugAssigned = true;

    @Column(name = "bug_status_changed", nullable = false)
    @Builder.Default
    private Boolean bugStatusChanged = true;

    @Column(name = "bug_priority_changed", nullable = false)
    @Builder.Default
    private Boolean bugPriorityChanged = true;

    @Column(name = "bug_commented", nullable = false)
    @Builder.Default
    private Boolean bugCommented = true;

    @Column(name = "bug_mentioned", nullable = false)
    @Builder.Default
    private Boolean bugMentioned = true;

    @Column(name = "bug_attachment_added", nullable = false)
    @Builder.Default
    private Boolean bugAttachmentAdded = true;

    // Project notification preferences

    @Column(name = "project_role_changed", nullable = false)
    @Builder.Default
    private Boolean projectRoleChanged = true;

    @Column(name = "project_member_joined", nullable = false)
    @Builder.Default
    private Boolean projectMemberJoined = true;

    // Team notification preferences

    @Column(name = "team_role_changed", nullable = false)
    @Builder.Default
    private Boolean teamRoleChanged = true;

    @Column(name = "team_member_joined", nullable = false)
    @Builder.Default
    private Boolean teamMemberJoined = true;

    // Gamification notification preferences
    @Column(name = "gamification_points", nullable = false)
    @Builder.Default
    private Boolean gamificationPoints = true;

    @Column(name = "gamification_achievements", nullable = false)
    @Builder.Default
    private Boolean gamificationAchievements = true;

    @Column(name = "gamification_leaderboard", nullable = false)
    @Builder.Default
    private Boolean gamificationLeaderboard = true;

    // Email specific settings
    @Enumerated(EnumType.STRING)
    @Column(name = "email_frequency", length = 20, nullable = false)
    @Builder.Default
    private EmailFrequency emailFrequency = EmailFrequency.IMMEDIATE;

    @Column(length = 50, nullable = false)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "NotificationPreferences{" +
                "preferenceId=" + preferenceId +
                ", emailEnabled=" + emailEnabled +
                ", inAppEnabled=" + inAppEnabled +
                ", toastEnabled=" + toastEnabled +
                ", emailFrequency=" + emailFrequency +
                '}';
    }
}
