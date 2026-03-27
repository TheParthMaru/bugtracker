package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.BugSimilarityCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for BugSimilarityCache entity operations.
 * 
 * Provides data access methods for cached similarity calculations between bugs.
 * Includes methods for finding similar bugs, managing cache expiration,
 * and optimizing performance through various query strategies.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Repository
public interface BugSimilarityCacheRepository extends JpaRepository<BugSimilarityCache, UUID> {

    // === Basic Queries ===

    /**
     * Find all cached similarities for a specific bug
     */
    @Query("SELECT bsc FROM BugSimilarityCache bsc WHERE bsc.bug.id = :bugId " +
            "AND bsc.expiresAt > CURRENT_TIMESTAMP ORDER BY bsc.similarityScore DESC")
    List<BugSimilarityCache> findValidSimilaritiesByBugId(@Param("bugId") Long bugId);

    /**
     * Find all cached similarities for bugs in a specific project
     */
    @Query("SELECT bsc FROM BugSimilarityCache bsc WHERE bsc.bug.project.id = :projectId " +
            "AND bsc.expiresAt > CURRENT_TIMESTAMP ORDER BY bsc.similarityScore DESC")
    List<BugSimilarityCache> findValidSimilaritiesByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find cached similarity between two specific bugs
     */
    @Query("SELECT bsc FROM BugSimilarityCache bsc WHERE " +
            "((bsc.bug.id = :bugId1 AND bsc.similarBug.id = :bugId2) OR " +
            "(bsc.bug.id = :bugId2 AND bsc.similarBug.id = :bugId1)) " +
            "AND bsc.expiresAt > CURRENT_TIMESTAMP")
    Optional<BugSimilarityCache> findValidSimilarityBetweenBugs(@Param("bugId1") Long bugId1,
            @Param("bugId2") Long bugId2);

    // === Similarity Threshold Queries ===

    /**
     * Find bugs similar to a given bug above a certain threshold
     */
    @Query("SELECT bsc FROM BugSimilarityCache bsc WHERE bsc.bug.id = :bugId " +
            "AND bsc.similarityScore >= :threshold AND bsc.expiresAt > CURRENT_TIMESTAMP " +
            "ORDER BY bsc.similarityScore DESC")
    List<BugSimilarityCache> findSimilarBugsAboveThreshold(@Param("bugId") Long bugId,
            @Param("threshold") Double threshold);

    /**
     * Find bugs in a project similar to a given bug above threshold
     */
    @Query("SELECT bsc FROM BugSimilarityCache bsc WHERE bsc.bug.id = :bugId " +
            "AND bsc.similarBug.project.id = :projectId " +
            "AND bsc.similarityScore >= :threshold AND bsc.expiresAt > CURRENT_TIMESTAMP " +
            "ORDER BY bsc.similarityScore DESC")
    List<BugSimilarityCache> findSimilarBugsInProjectAboveThreshold(@Param("bugId") Long bugId,
            @Param("projectId") UUID projectId,
            @Param("threshold") Double threshold);

    // === Algorithm-Specific Queries ===

    /**
     * Find similarities calculated using a specific algorithm
     */
    @Query("SELECT bsc FROM BugSimilarityCache bsc WHERE bsc.bug.id = :bugId " +
            "AND bsc.algorithmUsed = :algorithm AND bsc.expiresAt > CURRENT_TIMESTAMP " +
            "ORDER BY bsc.similarityScore DESC")
    List<BugSimilarityCache> findSimilaritiesByAlgorithm(@Param("bugId") Long bugId,
            @Param("algorithm") String algorithm);

    // === Text Fingerprint Queries ===

    /**
     * Find similarities by text fingerprint for quick lookup
     */
    @Query("SELECT bsc FROM BugSimilarityCache bsc WHERE bsc.textFingerprint = :fingerprint " +
            "AND bsc.expiresAt > CURRENT_TIMESTAMP ORDER BY bsc.similarityScore DESC")
    List<BugSimilarityCache> findByTextFingerprint(@Param("fingerprint") String fingerprint);

    /**
     * Check if similarity exists for given fingerprint and threshold
     */
    @Query("SELECT COUNT(bsc) > 0 FROM BugSimilarityCache bsc WHERE bsc.textFingerprint = :fingerprint " +
            "AND bsc.similarityScore >= :threshold AND bsc.expiresAt > CURRENT_TIMESTAMP")
    boolean existsSimilarityAboveThreshold(@Param("fingerprint") String fingerprint,
            @Param("threshold") Double threshold);

    // === Cache Management Queries ===

    /**
     * Remove expired cache entries
     */
    @Modifying
    @Query("DELETE FROM BugSimilarityCache bsc WHERE bsc.expiresAt <= CURRENT_TIMESTAMP")
    int deleteExpiredEntries();

    /**
     * Remove cache entries for a specific bug
     */
    @Modifying
    @Query("DELETE FROM BugSimilarityCache bsc WHERE bsc.bug.id = :bugId OR bsc.similarBug.id = :bugId")
    int deleteCacheEntriesForBug(@Param("bugId") Long bugId);

    /**
     * Remove cache entries for bugs in a specific project
     */
    @Modifying
    @Query("DELETE FROM BugSimilarityCache bsc WHERE bsc.bug.project.id = :projectId")
    int deleteCacheEntriesForProject(@Param("projectId") UUID projectId);

    /**
     * Update expiration time for cache entries
     */
    @Modifying
    @Query("UPDATE BugSimilarityCache bsc SET bsc.expiresAt = :newExpirationTime " +
            "WHERE bsc.bug.id = :bugId AND bsc.expiresAt > CURRENT_TIMESTAMP")
    int extendCacheExpiration(@Param("bugId") Long bugId, @Param("newExpirationTime") LocalDateTime newExpirationTime);

    // === Statistics and Analytics ===

    /**
     * Count valid cache entries for a bug
     */
    @Query("SELECT COUNT(bsc) FROM BugSimilarityCache bsc WHERE bsc.bug.id = :bugId " +
            "AND bsc.expiresAt > CURRENT_TIMESTAMP")
    long countValidEntriesForBug(@Param("bugId") Long bugId);

    /**
     * Count cache entries by algorithm
     */
    @Query("SELECT bsc.algorithmUsed, COUNT(bsc) FROM BugSimilarityCache bsc " +
            "WHERE bsc.bug.project.id = :projectId AND bsc.expiresAt > CURRENT_TIMESTAMP " +
            "GROUP BY bsc.algorithmUsed")
    List<Object[]> countCacheEntriesByAlgorithm(@Param("projectId") UUID projectId);

    /**
     * Get average similarity scores by algorithm
     */
    @Query("SELECT bsc.algorithmUsed, AVG(bsc.similarityScore) FROM BugSimilarityCache bsc " +
            "WHERE bsc.bug.project.id = :projectId AND bsc.expiresAt > CURRENT_TIMESTAMP " +
            "GROUP BY bsc.algorithmUsed")
    List<Object[]> getAverageSimilarityByAlgorithm(@Param("projectId") UUID projectId);

    /**
     * Find high similarity pairs (potential duplicates)
     */
    @Query("SELECT bsc FROM BugSimilarityCache bsc WHERE bsc.bug.project.id = :projectId " +
            "AND bsc.similarityScore >= :highThreshold AND bsc.expiresAt > CURRENT_TIMESTAMP " +
            "ORDER BY bsc.similarityScore DESC")
    List<BugSimilarityCache> findHighSimilarityPairs(@Param("projectId") UUID projectId,
            @Param("highThreshold") Double highThreshold);

    // === Bulk Operations ===

    /**
     * Check if any similar bugs exist above threshold for a project
     */
    @Query("SELECT COUNT(bsc) > 0 FROM BugSimilarityCache bsc WHERE bsc.bug.project.id = :projectId " +
            "AND bsc.similarityScore >= :threshold AND bsc.expiresAt > CURRENT_TIMESTAMP")
    boolean existsSimilarBugsInProject(@Param("projectId") UUID projectId, @Param("threshold") Double threshold);

    /**
     * Find recently cached similarities (within last N hours)
     */
    @Query("SELECT bsc FROM BugSimilarityCache bsc WHERE bsc.createdAt >= :sinceTime " +
            "AND bsc.expiresAt > CURRENT_TIMESTAMP ORDER BY bsc.createdAt DESC")
    List<BugSimilarityCache> findRecentSimilarities(@Param("sinceTime") LocalDateTime sinceTime);

    /**
     * Get cache hit rate statistics
     */
    @Query("SELECT " +
            "COUNT(CASE WHEN bsc.expiresAt > CURRENT_TIMESTAMP THEN 1 END), " +
            "COUNT(CASE WHEN bsc.expiresAt <= CURRENT_TIMESTAMP THEN 1 END), " +
            "COUNT(*) " +
            "FROM BugSimilarityCache bsc WHERE bsc.bug.project.id = :projectId")
    Object[] getCacheStatistics(@Param("projectId") UUID projectId);
}