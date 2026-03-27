package com.pbm5.bugtracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pbm5.bugtracker.entity.UserStreak;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, UUID> {

    /**
     * Find user streak by user ID
     */
    Optional<UserStreak> findByUserId(UUID userId);

    /**
     * Find users with current streak greater than minimum
     */
    List<UserStreak> findByCurrentStreakGreaterThanOrderByCurrentStreakDesc(Integer minStreak);

    /**
     * Find users with max streak greater than minimum
     */
    List<UserStreak> findByMaxStreakGreaterThanOrderByMaxStreakDesc(Integer minStreak);

    /**
     * Find users with streak above threshold
     */
    @Query("SELECT us FROM UserStreak us WHERE us.currentStreak >= :minStreak")
    List<UserStreak> findUsersWithStreakAbove(@Param("minStreak") Integer minStreak);

    /**
     * Find top streak performers
     */
    List<UserStreak> findByOrderByCurrentStreakDesc();

    /**
     * Find top max streak performers
     */
    List<UserStreak> findByOrderByMaxStreakDesc();

    /**
     * Find users by IDs (bulk operation)
     */
    @Query("SELECT us FROM UserStreak us WHERE us.userId IN :userIds")
    List<UserStreak> findByUserIds(@Param("userIds") List<UUID> userIds);

    /**
     * Find users with recent login activity
     */
    @Query("SELECT us FROM UserStreak us WHERE us.lastLoginDate >= :since ORDER BY us.lastLoginDate DESC")
    List<UserStreak> findUsersWithRecentLogin(@Param("since") java.time.LocalDate since);
}

