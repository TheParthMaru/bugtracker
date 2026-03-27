package com.pbm5.bugtracker.repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pbm5.bugtracker.entity.PointTransaction;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    /**
     * Find transactions by user ID, ordered by most recent
     */
    List<PointTransaction> findByUserIdOrderByEarnedAtDesc(UUID userId);

    /**
     * Find transactions by project ID, ordered by most recent
     */
    List<PointTransaction> findByProjectIdOrderByEarnedAtDesc(UUID projectId);

    /**
     * Find transactions by user ID and project ID, ordered by most recent
     */
    List<PointTransaction> findByUserIdAndProjectIdOrderByEarnedAtDesc(UUID userId, UUID projectId);

    /**
     * Find transactions earned after a specific date
     */
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.earnedAt >= :startDate")
    List<PointTransaction> findByEarnedAtAfter(@Param("startDate") LocalDateTime startDate);

    /**
     * Find transactions by user ID with pagination
     */
    Page<PointTransaction> findByUserIdOrderByEarnedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find transactions by project ID with pagination
     */
    Page<PointTransaction> findByProjectIdOrderByEarnedAtDesc(UUID projectId, Pageable pageable);

    /**
     * Find transactions by reason
     */
    List<PointTransaction> findByReasonOrderByEarnedAtDesc(String reason);

    /**
     * Find transactions by user ID and reason
     */
    List<PointTransaction> findByUserIdAndReason(UUID userId, String reason);

    /**
     * Find transactions by bug ID
     */
    List<PointTransaction> findByBugIdOrderByEarnedAtDesc(Long bugId);

    /**
     * Find daily login transactions for a user on a specific date
     * Uses new enum-based reason format: 'daily-login'
     */
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.userId = :userId AND pt.reason = 'daily-login' AND DATE(pt.earnedAt) = :date")
    List<PointTransaction> findDailyLoginTransactionsForDate(@Param("userId") UUID userId,
            @Param("date") LocalDate date);
}
