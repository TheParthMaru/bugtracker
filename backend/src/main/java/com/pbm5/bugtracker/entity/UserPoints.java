package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing user gamification statistics including total points,
 * streaks, and bugs resolved across all projects.
 */
@Entity
@Table(name = "user_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPoints {

    @Id
    @UuidGenerator
    @Column(name = "user_points_id")
    private UUID userPointsId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "max_streak", nullable = false)
    @Builder.Default
    private Integer maxStreak = 0;

    @Column(name = "bugs_resolved", nullable = false)
    @Builder.Default
    private Integer bugsResolved = 0;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Update the current streak and potentially the max streak
     */
    public void updateStreak(int newStreak) {
        this.currentStreak = newStreak;
        if (newStreak > this.maxStreak) {
            this.maxStreak = newStreak;
        }
    }

    /**
     * Add points to the user's total
     */
    public void addPoints(int points) {
        this.totalPoints = Math.max(0, this.totalPoints + points);
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Increment bugs resolved count
     */
    public void incrementBugsResolved() {
        this.bugsResolved++;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Update last activity timestamp
     */
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }
}
