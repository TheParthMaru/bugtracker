package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing project-specific leaderboard entries for users.
 * Tracks weekly, monthly, and all-time points for each user in each project.
 */
@Entity
@Table(name = "project_leaderboards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLeaderboard {

    @Id
    @UuidGenerator
    @Column(name = "leaderboard_entry_id")
    private UUID leaderboardEntryId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "weekly_points", nullable = false)
    @Builder.Default
    private Integer weeklyPoints = 0;

    @Column(name = "monthly_points", nullable = false)
    @Builder.Default
    private Integer monthlyPoints = 0;

    @Column(name = "all_time_points", nullable = false)
    @Builder.Default
    private Integer allTimePoints = 0;

    @Column(name = "bugs_resolved", nullable = false)
    @Builder.Default
    private Integer bugsResolved = 0;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Add points to all time tracking
     */
    public void addAllTimePoints(int points) {
        this.allTimePoints = Math.max(0, this.allTimePoints + points);
    }

    /**
     * Add points to weekly tracking
     */
    public void addWeeklyPoints(int points) {
        this.weeklyPoints = Math.max(0, this.weeklyPoints + points);
    }

    /**
     * Add points to monthly tracking
     */
    public void addMonthlyPoints(int points) {
        this.monthlyPoints = Math.max(0, this.monthlyPoints + points);
    }

    /**
     * Reset weekly points (called by scheduled job)
     */
    public void resetWeeklyPoints() {
        this.weeklyPoints = 0;
    }

    /**
     * Reset monthly points (called by scheduled job)
     */
    public void resetMonthlyPoints() {
        this.monthlyPoints = 0;
    }

    /**
     * Increment bugs resolved count
     */
    public void incrementBugsResolved() {
        this.bugsResolved++;
    }

    /**
     * Update current streak
     */
    public void updateCurrentStreak(int streak) {
        this.currentStreak = streak;
    }
}
