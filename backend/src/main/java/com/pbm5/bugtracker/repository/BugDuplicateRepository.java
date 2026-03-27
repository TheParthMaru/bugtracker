package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.BugDuplicate;
import com.pbm5.bugtracker.entity.DuplicateDetectionMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for BugDuplicate entity operations.
 * 
 * Provides data access methods for managing duplicate relationships between
 * bugs.
 * Includes methods for finding duplicates, tracking detection methods,
 * and analyzing duplicate patterns across projects.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Repository
public interface BugDuplicateRepository extends JpaRepository<BugDuplicate, UUID> {

        // === Basic Queries ===

        /**
         * Find all duplicates of a specific bug (where bug is the original)
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.id = :bugId " +
                        "ORDER BY bd.confidenceScore DESC, bd.markedAt DESC")
        List<BugDuplicate> findDuplicatesOfBug(@Param("bugId") Long bugId);

        /**
         * Find the original bug for a duplicate bug
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.duplicateBug.id = :bugId")
        Optional<BugDuplicate> findOriginalOfDuplicate(@Param("bugId") Long bugId);

        /**
         * Find the original bug for a duplicate bug (alias for findOriginalOfDuplicate)
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.duplicateBug.id = :bugId")
        Optional<BugDuplicate> findByDuplicateBugId(@Param("bugId") Long bugId);

        /**
         * Find all duplicates of a specific bug (alias for findDuplicatesOfBug)
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.id = :bugId " +
                        "ORDER BY bd.confidenceScore DESC, bd.markedAt DESC")
        List<BugDuplicate> findByOriginalBugId(@Param("bugId") Long bugId);

        /**
         * Check if a bug is marked as duplicate of another specific bug
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.id = :originalBugId " +
                        "AND bd.duplicateBug.id = :duplicateBugId")
        Optional<BugDuplicate> findDuplicateRelationship(@Param("originalBugId") Long originalBugId,
                        @Param("duplicateBugId") Long duplicateBugId);

        /**
         * Check if a bug is involved in any duplicate relationship
         */
        @Query("SELECT COUNT(bd) > 0 FROM BugDuplicate bd WHERE " +
                        "bd.originalBug.id = :bugId OR bd.duplicateBug.id = :bugId")
        boolean isBugInvolvedInDuplicates(@Param("bugId") Long bugId);

        // === Project-Based Queries ===

        /**
         * Find all duplicate relationships in a project
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "ORDER BY bd.markedAt DESC")
        Page<BugDuplicate> findDuplicatesInProject(@Param("projectId") UUID projectId, Pageable pageable);

        /**
         * Find duplicates in project by detection method
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "AND bd.detectionMethod = :detectionMethod ORDER BY bd.markedAt DESC")
        List<BugDuplicate> findDuplicatesByDetectionMethod(@Param("projectId") UUID projectId,
                        @Param("detectionMethod") DuplicateDetectionMethod detectionMethod);

        /**
         * Count duplicate relationships in a project
         */
        @Query("SELECT COUNT(bd) FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId")
        long countDuplicatesInProject(@Param("projectId") UUID projectId);

        /**
         * Count duplicate relationships in a project (alias for
         * countDuplicatesInProject)
         */
        @Query("SELECT COUNT(bd) FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId")
        long countByProjectId(@Param("projectId") UUID projectId);

        /**
         * Find all duplicate relationships in a project (alias for
         * findDuplicatesInProject)
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "ORDER BY bd.markedAt DESC")
        List<BugDuplicate> findByProjectId(@Param("projectId") UUID projectId);

        // === User-Based Queries ===

        /**
         * Find duplicates marked by a specific user
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.markedByUser.id = :userId " +
                        "ORDER BY bd.markedAt DESC")
        Page<BugDuplicate> findDuplicatesMarkedByUser(@Param("userId") UUID userId, Pageable pageable);

        /**
         * Find duplicates marked by user in a specific project
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.markedByUser.id = :userId " +
                        "AND bd.originalBug.project.id = :projectId ORDER BY bd.markedAt DESC")
        List<BugDuplicate> findDuplicatesMarkedByUserInProject(@Param("userId") UUID userId,
                        @Param("projectId") UUID projectId);

        /**
         * Count duplicates marked by a user
         */
        @Query("SELECT COUNT(bd) FROM BugDuplicate bd WHERE bd.markedByUser.id = :userId")
        long countDuplicatesMarkedByUser(@Param("userId") UUID userId);

        // === Confidence and Quality Queries ===

        /**
         * Find high-confidence duplicates (confidence score above threshold)
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "AND bd.confidenceScore >= :confidenceThreshold ORDER BY bd.confidenceScore DESC")
        List<BugDuplicate> findHighConfidenceDuplicates(@Param("projectId") UUID projectId,
                        @Param("confidenceThreshold") Double confidenceThreshold);

        /**
         * Find low-confidence duplicates that may need review
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "AND bd.confidenceScore < :confidenceThreshold ORDER BY bd.confidenceScore ASC")
        List<BugDuplicate> findLowConfidenceDuplicates(@Param("projectId") UUID projectId,
                        @Param("confidenceThreshold") Double confidenceThreshold);

        // === Time-Based Queries ===

        /**
         * Find duplicates marked within a time period
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "AND bd.markedAt BETWEEN :startDate AND :endDate ORDER BY bd.markedAt DESC")
        List<BugDuplicate> findDuplicatesInTimePeriod(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Find recent duplicates (last N days)
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "AND bd.markedAt >= :sinceDate ORDER BY bd.markedAt DESC")
        List<BugDuplicate> findRecentDuplicates(@Param("projectId") UUID projectId,
                        @Param("sinceDate") LocalDateTime sinceDate);

        // === Analytics and Statistics ===

        /**
         * Get duplicate statistics by detection method
         */
        @Query("SELECT bd.detectionMethod, COUNT(bd), AVG(bd.confidenceScore) " +
                        "FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "GROUP BY bd.detectionMethod")
        List<Object[]> getDuplicateStatsByDetectionMethod(@Param("projectId") UUID projectId);

        /**
         * Get duplicate trends by month
         */
        @Query("SELECT EXTRACT(YEAR FROM bd.markedAt), EXTRACT(MONTH FROM bd.markedAt), COUNT(bd) " +
                        "FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "AND bd.markedAt BETWEEN :startDate AND :endDate " +
                        "GROUP BY EXTRACT(YEAR FROM bd.markedAt), EXTRACT(MONTH FROM bd.markedAt) " +
                        "ORDER BY EXTRACT(YEAR FROM bd.markedAt), EXTRACT(MONTH FROM bd.markedAt)")
        List<Object[]> getDuplicateTrends(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Get top users marking duplicates
         */
        @Query("SELECT bd.markedByUser.id, bd.markedByUser.firstName, bd.markedByUser.lastName, COUNT(bd) " +
                        "FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "GROUP BY bd.markedByUser.id, bd.markedByUser.firstName, bd.markedByUser.lastName " +
                        "ORDER BY COUNT(bd) DESC")
        List<Object[]> getTopDuplicateMarkers(@Param("projectId") UUID projectId);

        /**
         * Get average confidence score for automatic vs manual detection
         */
        @Query("SELECT bd.detectionMethod, AVG(bd.confidenceScore), COUNT(bd) " +
                        "FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "GROUP BY bd.detectionMethod")
        List<Object[]> getConfidenceStatsByMethod(@Param("projectId") UUID projectId);

        // === Complex Queries ===

        /**
         * Find potential duplicate chains (A->B, B->C patterns)
         */
        @Query("SELECT bd1.originalBug.id, bd1.duplicateBug.id, bd2.duplicateBug.id " +
                        "FROM BugDuplicate bd1 JOIN BugDuplicate bd2 ON bd1.duplicateBug.id = bd2.originalBug.id " +
                        "WHERE bd1.originalBug.project.id = :projectId")
        List<Object[]> findDuplicateChains(@Param("projectId") UUID projectId);

        /**
         * Find bugs that are frequently marked as originals (popular duplicate targets)
         */
        @Query("SELECT bd.originalBug.id, bd.originalBug.title, COUNT(bd) as duplicateCount " +
                        "FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "GROUP BY bd.originalBug.id, bd.originalBug.title " +
                        "HAVING COUNT(bd) >= :minDuplicates ORDER BY COUNT(bd) DESC")
        List<Object[]> findFrequentDuplicateTargets(@Param("projectId") UUID projectId,
                        @Param("minDuplicates") Integer minDuplicates);

        /**
         * Find automatic detections that were later manually confirmed
         */
        @Query("SELECT bd FROM BugDuplicate bd WHERE bd.originalBug.project.id = :projectId " +
                        "AND bd.detectionMethod = 'HYBRID' AND bd.confidenceScore >= :threshold " +
                        "ORDER BY bd.confidenceScore DESC")
        List<BugDuplicate> findValidatedAutomaticDetections(@Param("projectId") UUID projectId,
                        @Param("threshold") Double threshold);

        // === Validation Queries ===

        /**
         * Check for circular duplicate relationships (A->B, B->A)
         */
        @Query("SELECT COUNT(bd) > 0 FROM BugDuplicate bd WHERE bd.originalBug.id = :bugB " +
                        "AND bd.duplicateBug.id = :bugA AND EXISTS " +
                        "(SELECT 1 FROM BugDuplicate bd2 WHERE bd2.originalBug.id = :bugA AND bd2.duplicateBug.id = :bugB)")
        boolean hasCircularDuplicateRelationship(@Param("bugA") Long bugA, @Param("bugB") Long bugB);

        /**
         * Find inconsistent duplicate relationships in project
         */
        @Query("SELECT bd1.originalBug.id, bd1.duplicateBug.id, bd2.originalBug.id, bd2.duplicateBug.id " +
                        "FROM BugDuplicate bd1, BugDuplicate bd2 " +
                        "WHERE bd1.originalBug.project.id = :projectId " +
                        "AND bd2.originalBug.project.id = :projectId " +
                        "AND bd1.originalBug.id = bd2.duplicateBug.id " +
                        "AND bd1.duplicateBug.id = bd2.originalBug.id")
        List<Object[]> findInconsistentDuplicateRelationships(@Param("projectId") UUID projectId);
}