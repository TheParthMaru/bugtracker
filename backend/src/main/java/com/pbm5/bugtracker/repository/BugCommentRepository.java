package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.BugComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BugCommentRepository extends JpaRepository<BugComment, Long> {

    // Basic queries
    List<BugComment> findByBugId(Long bugId);

    List<BugComment> findByBugIdOrderByCreatedAtAsc(Long bugId);

    List<BugComment> findByBugIdOrderByCreatedAtDesc(Long bugId);

    List<BugComment> findByAuthorId(UUID authorId);

    List<BugComment> findByBugIdAndAuthorId(Long bugId, UUID authorId);

    // Threading queries
    List<BugComment> findByBugIdAndParentIsNullOrderByCreatedAtAsc(Long bugId);

    List<BugComment> findByBugIdAndParentIsNullOrderByCreatedAtDesc(Long bugId);

    List<BugComment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    List<BugComment> findByParentIdOrderByCreatedAtDesc(Long parentId);

    // Date-based queries
    List<BugComment> findByBugIdAndCreatedAtBetween(Long bugId, LocalDateTime startDate, LocalDateTime endDate);

    List<BugComment> findByBugIdAndUpdatedAtBetween(Long bugId, LocalDateTime startDate, LocalDateTime endDate);

    List<BugComment> findByAuthorIdAndCreatedAtBetween(UUID authorId, LocalDateTime startDate, LocalDateTime endDate);

    // Project-based queries
    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.project.id = :projectId")
    List<BugComment> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.project.id = :projectId ORDER BY bc.createdAt DESC")
    List<BugComment> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") Long projectId);

    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.project.id = :projectId AND bc.author.id = :authorId")
    List<BugComment> findByProjectIdAndAuthorId(@Param("projectId") Long projectId, @Param("authorId") UUID authorId);

    // Search queries
    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.id = :bugId AND " +
            "LOWER(bc.content) LIKE LOWER('%' || :searchTerm || '%')")
    List<BugComment> findByBugIdAndContentContaining(@Param("bugId") Long bugId,
            @Param("searchTerm") String searchTerm);

    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.project.id = :projectId AND " +
            "LOWER(bc.content) LIKE LOWER('%' || :searchTerm || '%')")
    List<BugComment> findByProjectIdAndContentContaining(@Param("projectId") Long projectId,
            @Param("searchTerm") String searchTerm);

    // Analytics queries
    @Query("SELECT COUNT(bc) FROM BugComment bc WHERE bc.bug.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(bc) FROM BugComment bc WHERE bc.bug.id = :bugId")
    long countByBugId(@Param("bugId") Long bugId);

    @Query("SELECT COUNT(bc) FROM BugComment bc WHERE bc.bug.project.id = :projectId AND bc.author.id = :authorId")
    long countByProjectIdAndAuthorId(@Param("projectId") Long projectId, @Param("authorId") UUID authorId);

    @Query("SELECT COUNT(bc) FROM BugComment bc WHERE bc.bug.id = :bugId AND bc.parent IS NULL")
    long countTopLevelCommentsByBugId(@Param("bugId") Long bugId);

    @Query("SELECT COUNT(bc) FROM BugComment bc WHERE bc.parent.id = :parentId")
    long countRepliesByParentId(@Param("parentId") Long parentId);

    // Recent activity
    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.project.id = :projectId ORDER BY bc.createdAt DESC")
    Page<BugComment> findRecentCommentsByProjectId(@Param("projectId") Long projectId, Pageable pageable);

    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.id = :bugId ORDER BY bc.createdAt DESC")
    Page<BugComment> findRecentCommentsByBugId(@Param("bugId") Long bugId, Pageable pageable);

    // Most active users
    @Query("SELECT bc.author.id, COUNT(bc) as commentCount FROM BugComment bc " +
            "WHERE bc.bug.project.id = :projectId " +
            "GROUP BY bc.author.id " +
            "ORDER BY commentCount DESC")
    List<Object[]> findMostActiveUsersByProjectId(@Param("projectId") Long projectId);

    // Comment activity over time
    @Query("SELECT CAST(bc.createdAt AS DATE), COUNT(bc) FROM BugComment bc " +
            "WHERE bc.bug.project.id = :projectId AND bc.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(bc.createdAt AS DATE) " +
            "ORDER BY CAST(bc.createdAt AS DATE)")
    List<Object[]> findCommentActivityByProjectId(@Param("projectId") Long projectId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Thread depth analysis - temporarily simplified due to computed depth method
    // @Query("SELECT bc.depth, COUNT(bc) FROM BugComment bc " +
    //         "WHERE bc.bug.project.id = :projectId " +
    //         "GROUP BY bc.depth " +
    //         "ORDER BY bc.depth")
    // List<Object[]> findThreadDepthDistributionByProjectId(@Param("projectId") Long projectId);

    // Alternative: Count top-level vs reply comments
    @Query("SELECT 'TOP_LEVEL' as level, COUNT(bc) FROM BugComment bc " +
            "WHERE bc.bug.project.id = :projectId AND bc.parent IS NULL " +
            "UNION ALL " +
            "SELECT 'REPLIES' as level, COUNT(bc) FROM BugComment bc " +
            "WHERE bc.bug.project.id = :projectId AND bc.parent IS NOT NULL")
    List<Object[]> findThreadDepthDistributionByProjectId(@Param("projectId") Long projectId);

    // Exists queries for validation
    boolean existsByBugIdAndId(Long bugId, Long commentId);

    boolean existsByParentIdAndId(Long parentId, Long commentId);

    // Cleanup queries
    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.id = :bugId AND bc.id NOT IN :commentIds")
    List<BugComment> findOrphanedComments(@Param("bugId") Long bugId, @Param("commentIds") List<Long> commentIds);

    // Edited comments
    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.project.id = :projectId AND bc.updatedAt > bc.createdAt")
    List<BugComment> findEditedCommentsByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT bc FROM BugComment bc WHERE bc.bug.id = :bugId AND bc.updatedAt > bc.createdAt")
    List<BugComment> findEditedCommentsByBugId(@Param("bugId") Long bugId);
}