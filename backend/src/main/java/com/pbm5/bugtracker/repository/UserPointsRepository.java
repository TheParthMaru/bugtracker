package com.pbm5.bugtracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pbm5.bugtracker.entity.UserPoints;

@Repository
public interface UserPointsRepository extends JpaRepository<UserPoints, UUID> {

    /**
     * Find user points by user ID
     */
    Optional<UserPoints> findByUserId(UUID userId);

    /**
     * Find users with total points greater than minimum
     */
    List<UserPoints> findByTotalPointsGreaterThanOrderByTotalPointsDesc(Integer minPoints);

    /**
     * Find users by IDs (bulk operation)
     */
    @Query("SELECT up FROM UserPoints up WHERE up.userId IN :userIds")
    List<UserPoints> findByUserIds(@Param("userIds") List<UUID> userIds);

    /**
     * Find top performers by total points
     */
    Page<UserPoints> findByOrderByTotalPointsDesc(Pageable pageable);

    /**
     * Find top performers by current streak
     */
    Page<UserPoints> findByOrderByCurrentStreakDesc(Pageable pageable);

    /**
     * Find top performers by bugs resolved
     */
    Page<UserPoints> findByOrderByBugsResolvedDesc(Pageable pageable);

    /**
     * Find users with recent activity
     */
    @Query("SELECT up FROM UserPoints up WHERE up.lastActivity >= :since ORDER BY up.lastActivity DESC")
    List<UserPoints> findUsersWithRecentActivity(@Param("since") java.time.LocalDateTime since);

    /**
     * Check if user points exist for a given user ID
     */
    boolean existsByUserId(UUID userId);
}
