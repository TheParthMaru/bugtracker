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

import com.pbm5.bugtracker.entity.ProjectLeaderboard;

@Repository
public interface ProjectLeaderboardRepository extends JpaRepository<ProjectLeaderboard, UUID> {

    /**
     * Find leaderboard entries by project ID, ordered by all-time points
     */
    List<ProjectLeaderboard> findByProjectIdOrderByAllTimePointsDesc(UUID projectId);

    /**
     * Find leaderboard entries by project ID, ordered by weekly points
     */
    List<ProjectLeaderboard> findByProjectIdOrderByWeeklyPointsDesc(UUID projectId);

    /**
     * Find leaderboard entries by project ID, ordered by monthly points
     */
    List<ProjectLeaderboard> findByProjectIdOrderByMonthlyPointsDesc(UUID projectId);

    /**
     * Find leaderboard entry by project ID and user ID
     */
    Optional<ProjectLeaderboard> findByProjectIdAndUserId(UUID projectId, UUID userId);

    /**
     * Find top performers by project with pagination
     */
    @Query("SELECT pl FROM ProjectLeaderboard pl WHERE pl.projectId = :projectId ORDER BY pl.allTimePoints DESC")
    Page<ProjectLeaderboard> findTopPerformersByProject(@Param("projectId") UUID projectId, Pageable pageable);

    /**
     * Find top performers by weekly points
     */
    @Query("SELECT pl FROM ProjectLeaderboard pl WHERE pl.projectId = :projectId ORDER BY pl.weeklyPoints DESC")
    Page<ProjectLeaderboard> findTopWeeklyPerformersByProject(@Param("projectId") UUID projectId, Pageable pageable);

    /**
     * Find top performers by monthly points
     */
    @Query("SELECT pl FROM ProjectLeaderboard pl WHERE pl.projectId = :projectId ORDER BY pl.monthlyPoints DESC")
    Page<ProjectLeaderboard> findTopMonthlyPerformersByProject(@Param("projectId") UUID projectId, Pageable pageable);

    /**
     * Find top performers by bugs resolved
     */
    @Query("SELECT pl FROM ProjectLeaderboard pl WHERE pl.projectId = :projectId ORDER BY pl.bugsResolved DESC")
    Page<ProjectLeaderboard> findTopBugResolversByProject(@Param("projectId") UUID projectId, Pageable pageable);

    /**
     * Find top performers by current streak
     */
    @Query("SELECT pl FROM ProjectLeaderboard pl WHERE pl.projectId = :projectId ORDER BY pl.currentStreak DESC")
    Page<ProjectLeaderboard> findTopStreakPerformersByProject(@Param("projectId") UUID projectId, Pageable pageable);

    /**
     * Find all leaderboard entries for a user across all projects
     */
    List<ProjectLeaderboard> findByUserIdOrderByAllTimePointsDesc(UUID userId);

    /**
     * Find leaderboard entries by project ID with pagination
     */
    Page<ProjectLeaderboard> findByProjectId(UUID projectId, Pageable pageable);
}

