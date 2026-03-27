package com.pbm5.bugtracker.entity;

import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing user login streak tracking.
 * Tracks consecutive days logged in and maintains the maximum streak achieved.
 */
@Entity
@Table(name = "user_streaks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStreak {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "max_streak", nullable = false)
    @Builder.Default
    private Integer maxStreak = 0;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

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
     * Increment the current streak
     */
    public void incrementStreak() {
        this.currentStreak++;
        if (this.currentStreak > this.maxStreak) {
            this.maxStreak = this.currentStreak;
        }
    }

    /**
     * Reset the current streak to 0
     */
    public void resetStreak() {
        this.currentStreak = 0;
    }

    /**
     * Update the last login date
     */
    public void updateLastLoginDate(LocalDate date) {
        this.lastLoginDate = date;
    }

    /**
     * Check if the user has a current streak
     */
    public boolean hasCurrentStreak() {
        return this.currentStreak > 0;
    }

    /**
     * Check if the user has achieved a max streak
     */
    public boolean hasMaxStreak() {
        return this.maxStreak > 0;
    }
}

