package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.BugAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BugAttachmentRepository extends JpaRepository<BugAttachment, Long> {

        // Basic queries
        List<BugAttachment> findByBugId(Long bugId);

        List<BugAttachment> findByBugIdOrderByCreatedAtDesc(Long bugId);

        List<BugAttachment> findByUploadedById(UUID uploadedById);

        List<BugAttachment> findByBugIdAndUploadedById(Long bugId, UUID uploadedById);

        // File type queries
        List<BugAttachment> findByBugIdAndMimeTypeStartingWith(Long bugId, String mimeTypePrefix);

        List<BugAttachment> findByBugIdAndMimeTypeIn(Long bugId, List<String> mimeTypes);

        // Size-based queries
        List<BugAttachment> findByBugIdAndFileSizeGreaterThan(Long bugId, Long fileSize);

        List<BugAttachment> findByBugIdAndFileSizeLessThan(Long bugId, Long fileSize);

        // Date-based queries
        List<BugAttachment> findByBugIdAndCreatedAtBetween(Long bugId, java.time.LocalDateTime startDate,
                        java.time.LocalDateTime endDate);

        // Project-based queries
        @Query("SELECT ba FROM BugAttachment ba WHERE ba.bug.project.id = :projectId")
        List<BugAttachment> findByProjectId(@Param("projectId") Long projectId);

        @Query("SELECT ba FROM BugAttachment ba WHERE ba.bug.project.id = :projectId ORDER BY ba.createdAt DESC")
        List<BugAttachment> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") Long projectId);

        // Analytics queries
        @Query("SELECT COUNT(ba) FROM BugAttachment ba WHERE ba.bug.project.id = :projectId")
        long countByProjectId(@Param("projectId") Long projectId);

        @Query("SELECT COUNT(ba) FROM BugAttachment ba WHERE ba.bug.id = :bugId")
        long countByBugId(@Param("bugId") Long bugId);

        @Query("SELECT COUNT(ba) FROM BugAttachment ba WHERE ba.bug.project.id = :projectId AND ba.mimeType LIKE :mimeTypePattern")
        long countByProjectIdAndMimeTypePattern(@Param("projectId") Long projectId,
                        @Param("mimeTypePattern") String mimeTypePattern);

        @Query("SELECT SUM(ba.fileSize) FROM BugAttachment ba WHERE ba.bug.project.id = :projectId")
        Long getTotalFileSizeByProjectId(@Param("projectId") Long projectId);

        @Query("SELECT SUM(ba.fileSize) FROM BugAttachment ba WHERE ba.bug.id = :bugId")
        Long getTotalFileSizeByBugId(@Param("bugId") Long bugId);

        // File type analytics
        @Query("SELECT ba.mimeType, COUNT(ba) FROM BugAttachment ba WHERE ba.bug.project.id = :projectId GROUP BY ba.mimeType ORDER BY COUNT(ba) DESC")
        List<Object[]> getFileTypeDistributionByProjectId(@Param("projectId") Long projectId);

        @Query("SELECT ba.mimeType, COUNT(ba) FROM BugAttachment ba WHERE ba.bug.id = :bugId GROUP BY ba.mimeType ORDER BY COUNT(ba) DESC")
        List<Object[]> getFileTypeDistributionByBugId(@Param("bugId") Long bugId);

        // Large files
        @Query("SELECT ba FROM BugAttachment ba WHERE ba.bug.project.id = :projectId AND ba.fileSize > :minSize ORDER BY ba.fileSize DESC")
        List<BugAttachment> findLargeFilesByProjectId(@Param("projectId") Long projectId,
                        @Param("minSize") Long minSize);

        // Recent uploads
        @Query("SELECT ba FROM BugAttachment ba WHERE ba.bug.project.id = :projectId ORDER BY ba.createdAt DESC")
        List<BugAttachment> findRecentUploadsByProjectId(@Param("projectId") Long projectId);

        // Exists queries for validation
        boolean existsByBugIdAndOriginalFilename(Long bugId, String originalFilename);

        boolean existsByBugIdAndId(Long bugId, Long attachmentId);

        // Find by bug ID and attachment ID
        @Query("SELECT ba FROM BugAttachment ba WHERE ba.bug.id = :bugId AND ba.id = :attachmentId")
        java.util.Optional<BugAttachment> findByBugIdAndId(@Param("bugId") Long bugId,
                        @Param("attachmentId") Long attachmentId);

        // Cleanup queries
        @Query("SELECT ba FROM BugAttachment ba WHERE ba.bug.id = :bugId AND ba.id NOT IN :attachmentIds")
        List<BugAttachment> findOrphanedAttachments(@Param("bugId") Long bugId,
                        @Param("attachmentIds") List<Long> attachmentIds);
}