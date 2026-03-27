package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for NotificationPreferences entity operations.
 * 
 * Provides data access methods for managing user notification preferences
 * including:
 * - Finding preferences by user
 * - Querying users with specific preference settings
 * - Bulk preference updates
 */
@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, Long> {

    // Basic queries
    Optional<NotificationPreferences> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    // Channel preference queries
    @Query("SELECT p.user.id FROM NotificationPreferences p WHERE p.emailEnabled = true")
    List<UUID> findUserIdsWithEmailEnabled();

    @Query("SELECT p.user.id FROM NotificationPreferences p WHERE p.inAppEnabled = true")
    List<UUID> findUserIdsWithInAppEnabled();

    @Query("SELECT p.user.id FROM NotificationPreferences p WHERE p.toastEnabled = true")
    List<UUID> findUserIdsWithToastEnabled();

    // Event-specific preference queries
    @Query("SELECT p.user.id FROM NotificationPreferences p WHERE p.bugAssigned = true AND p.emailEnabled = true")
    List<UUID> findUserIdsWithBugAssignedEmailEnabled();

    @Query("SELECT p.user.id FROM NotificationPreferences p WHERE p.bugCommented = true AND p.inAppEnabled = true")
    List<UUID> findUserIdsWithBugCommentedInAppEnabled();

    @Query("SELECT p.user.id FROM NotificationPreferences p WHERE p.bugMentioned = true")
    List<UUID> findUserIdsWithBugMentionedEnabled();

    @Query("SELECT p.user.id FROM NotificationPreferences p WHERE p.gamificationPoints = true")
    List<UUID> findUserIdsWithGamificationPointsEnabled();

    // Email frequency queries
    @Query("SELECT p FROM NotificationPreferences p WHERE p.emailEnabled = true AND p.emailFrequency = 'DAILY'")
    List<NotificationPreferences> findUsersWithDailyEmailFrequency();

    @Query("SELECT p FROM NotificationPreferences p WHERE p.emailEnabled = true AND p.emailFrequency = 'WEEKLY'")
    List<NotificationPreferences> findUsersWithWeeklyEmailFrequency();

    // Validation queries
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM NotificationPreferences p WHERE p.user.id = :userId")
    boolean hasPreferences(@Param("userId") UUID userId);
}
