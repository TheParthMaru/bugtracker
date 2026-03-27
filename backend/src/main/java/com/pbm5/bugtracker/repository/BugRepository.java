package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.BugPriority;
import com.pbm5.bugtracker.entity.BugStatus;
import com.pbm5.bugtracker.entity.BugType;
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

@Repository
public interface BugRepository extends JpaRepository<Bug, Long> {

        // Basic queries
        Page<Bug> findByProjectId(UUID projectId, Pageable pageable);

        List<Bug> findByProjectId(UUID projectId);

        List<Bug> findByAssigneeId(UUID assigneeId);

        List<Bug> findByReporterId(UUID reporterId);

        List<Bug> findByProjectIdAndAssigneeId(UUID projectId, UUID assigneeId);

        List<Bug> findByProjectIdAndReporterId(UUID projectId, UUID reporterId);

        Optional<Bug> findByProjectIdAndProjectTicketNumber(UUID projectId, Integer projectTicketNumber);

        // Status-based queries
        List<Bug> findByProjectIdAndStatus(UUID projectId, BugStatus status);

        List<Bug> findByProjectIdAndStatusIn(UUID projectId, List<BugStatus> statuses);

        List<Bug> findByAssigneeIdAndStatus(UUID assigneeId, BugStatus status);

        List<Bug> findByReporterIdAndStatus(UUID reporterId, BugStatus status);

        // Priority-based queries
        List<Bug> findByProjectIdAndPriority(UUID projectId, BugPriority priority);

        List<Bug> findByProjectIdAndPriorityIn(UUID projectId, List<BugPriority> priorities);

        List<Bug> findByAssigneeIdAndPriority(UUID assigneeId, BugPriority priority);

        // Type-based queries
        List<Bug> findByProjectIdAndType(UUID projectId, BugType type);

        List<Bug> findByProjectIdAndTypeIn(UUID projectId, List<BugType> types);

        // Date-based queries
        List<Bug> findByProjectIdAndCreatedAtBetween(UUID projectId, LocalDateTime startDate, LocalDateTime endDate);

        List<Bug> findByProjectIdAndUpdatedAtBetween(UUID projectId, LocalDateTime startDate, LocalDateTime endDate);

        List<Bug> findByProjectIdAndClosedAtBetween(UUID projectId, LocalDateTime startDate, LocalDateTime endDate);

        // Search queries
        @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId AND " +
                        "(b.title LIKE %:searchTerm% OR " +
                        "b.description LIKE %:searchTerm%)")
        Page<Bug> findByProjectIdAndSearchTerm(@Param("projectId") UUID projectId,
                        @Param("searchTerm") String searchTerm,
                        Pageable pageable);

        // Label-based queries
        @Query("SELECT DISTINCT b FROM Bug b JOIN b.labels l WHERE b.project.id = :projectId AND l.name IN :labelNames")
        Page<Bug> findByProjectIdAndLabelNames(@Param("projectId") UUID projectId,
                        @Param("labelNames") List<String> labelNames,
                        Pageable pageable);

        // Complex filtering
        @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId " +
                        "AND (:status IS NULL OR b.status = :status) " +
                        "AND (:priority IS NULL OR b.priority = :priority) " +
                        "AND (:type IS NULL OR b.type = :type) " +
                        "AND (:assigneeId IS NULL OR b.assignee.id = :assigneeId) " +
                        "AND (:assigneeIsNull IS NULL OR " +
                        "     (:assigneeIsNull = true AND b.assignee IS NULL) OR " +
                        "     (:assigneeIsNull = false AND b.assignee IS NOT NULL)) " +
                        "AND (:reporterId IS NULL OR b.reporter.id = :reporterId) " +
                        "AND (:searchTerm IS NULL OR :searchTerm = '' OR (b.title LIKE %:searchTerm% OR " +
                        "b.description LIKE %:searchTerm%))")
        Page<Bug> findByProjectIdWithFilters(@Param("projectId") UUID projectId,
                        @Param("status") BugStatus status,
                        @Param("priority") BugPriority priority,
                        @Param("type") BugType type,
                        @Param("assigneeId") UUID assigneeId,
                        @Param("assigneeIsNull") Boolean assigneeIsNull,
                        @Param("reporterId") UUID reporterId,
                        @Param("searchTerm") String searchTerm,
                        Pageable pageable);

        // Analytics queries
        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId")
        long countByProjectId(@Param("projectId") UUID projectId);

        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId AND b.status = :status")
        long countByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") BugStatus status);

        // Find bug by project ticket number (the correct way to identify bugs within a
        // project)
        @Query("SELECT DISTINCT b FROM Bug b " +
                        "LEFT JOIN FETCH b.labels " +
                        "WHERE b.project.id = :projectId AND b.projectTicketNumber = :projectTicketNumber")
        Optional<Bug> findByProjectIdAndProjectTicketNumberWithLabels(@Param("projectId") UUID projectId,
                        @Param("projectTicketNumber") Integer projectTicketNumber);

        // Find bug by project ticket number with attachments
        @Query("SELECT DISTINCT b FROM Bug b " +
                        "LEFT JOIN FETCH b.attachments " +
                        "WHERE b.project.id = :projectId AND b.projectTicketNumber = :projectTicketNumber")
        Optional<Bug> findByProjectIdAndProjectTicketNumberWithAttachments(@Param("projectId") UUID projectId,
                        @Param("projectTicketNumber") Integer projectTicketNumber);

        // Find bug by project ticket number with comments
        @Query("SELECT DISTINCT b FROM Bug b " +
                        "LEFT JOIN FETCH b.comments " +
                        "WHERE b.project.id = :projectId AND b.projectTicketNumber = :projectTicketNumber")
        Optional<Bug> findByProjectIdAndProjectTicketNumberWithComments(@Param("projectId") UUID projectId,
                        @Param("projectTicketNumber") Integer projectTicketNumber);

        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId AND b.priority = :priority")
        long countByProjectIdAndPriority(@Param("projectId") UUID projectId, @Param("priority") BugPriority priority);

        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId AND b.type = :type")
        long countByProjectIdAndType(@Param("projectId") UUID projectId, @Param("type") BugType type);

        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId AND b.assignee.id = :assigneeId")
        long countByProjectIdAndAssigneeId(@Param("projectId") UUID projectId, @Param("assigneeId") UUID assigneeId);

        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId AND b.reporter.id = :reporterId")
        long countByProjectIdAndReporterId(@Param("projectId") UUID projectId, @Param("reporterId") UUID reporterId);

        // Date range analytics
        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId AND b.createdAt BETWEEN :startDate AND :endDate")
        long countByProjectIdAndCreatedAtBetween(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId AND b.closedAt BETWEEN :startDate AND :endDate")
        long countByProjectIdAndClosedAtBetween(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // High priority bugs requiring attention
        @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId AND b.priority IN ('CRASH', 'CRITICAL') AND b.assignee IS NULL")
        List<Bug> findHighPriorityUnassignedBugs(@Param("projectId") UUID projectId);

        // Recent bugs
        @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId ORDER BY b.createdAt DESC")
        Page<Bug> findRecentBugs(@Param("projectId") UUID projectId, Pageable pageable);

        // Bugs by team (assuming team members are assigned bugs)
        @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId AND b.assignee.id IN " +
                        "(SELECT tm.user.id FROM TeamMember tm WHERE tm.team.id = :teamId)")
        List<Bug> findByProjectIdAndTeamId(@Param("projectId") UUID projectId, @Param("teamId") UUID teamId);

        // Bugs that need immediate attention
        @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId AND " +
                        "((b.priority IN ('CRASH', 'CRITICAL') AND b.assignee IS NULL) OR " +
                        "(b.status = 'OPEN' AND b.priority = 'HIGH'))")
        List<Bug> findBugsRequiringAttention(@Param("projectId") UUID projectId);

        // Exists queries for validation
        boolean existsByProjectIdAndTitle(UUID projectId, String title);

        boolean existsByProjectIdAndId(UUID projectId, Long bugId);

        // Find by project and bug ID
        @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId AND b.id = :bugId")
        Optional<Bug> findByProjectIdAndId(@Param("projectId") UUID projectId, @Param("bugId") Long bugId);

        // Find by project and bug ID with assignee and reporter loaded
        @Query("SELECT b FROM Bug b LEFT JOIN FETCH b.assignee LEFT JOIN FETCH b.reporter WHERE b.project.id = :projectId AND b.id = :bugId")
        Optional<Bug> findByProjectIdAndIdWithUsers(@Param("projectId") UUID projectId, @Param("bugId") Long bugId);

        // Get next project ticket number
        @Query(value = "SELECT get_next_project_ticket_number(:projectId)", nativeQuery = true)
        Integer getNextProjectTicketNumber(@Param("projectId") UUID projectId);

        // Analytics and Reporting Methods

        // Distribution queries
        @Query("SELECT b.priority, COUNT(b) FROM Bug b WHERE b.project.id = :projectId GROUP BY b.priority")
        List<Object[]> getPriorityDistributionByProjectId(@Param("projectId") UUID projectId);

        @Query("SELECT b.type, COUNT(b) FROM Bug b WHERE b.project.id = :projectId GROUP BY b.type")
        List<Object[]> getTypeDistributionByProjectId(@Param("projectId") UUID projectId);

        @Query("SELECT b.status, COUNT(b) FROM Bug b WHERE b.project.id = :projectId GROUP BY b.status")
        List<Object[]> getStatusDistributionByProjectId(@Param("projectId") UUID projectId);

        // Team performance queries
        @Query("SELECT b.assignee.id, b.assignee.firstName, b.assignee.lastName, COUNT(b), COUNT(CASE WHEN b.status IN ('FIXED', 'CLOSED') THEN 1 END) "
                        +
                        "FROM Bug b WHERE b.project.id = :projectId AND b.assignee IS NOT NULL GROUP BY b.assignee.id, b.assignee.firstName, b.assignee.lastName")
        List<Object[]> getAssigneeStatisticsByProjectId(@Param("projectId") UUID projectId);

        @Query("SELECT b.reporter.id, b.reporter.firstName, b.reporter.lastName, COUNT(b) FROM Bug b WHERE b.project.id = :projectId GROUP BY b.reporter.id, b.reporter.firstName, b.reporter.lastName")
        List<Object[]> getReporterStatisticsByProjectId(@Param("projectId") UUID projectId);

        // Get average resolution time by assignee (in days)
        @Query(value = "SELECT b.assignee_id, AVG(EXTRACT(EPOCH FROM (b.closed_at - b.created_at))/86400) " +
                        "FROM bugs b WHERE b.project_id = :projectId AND b.assignee_id IS NOT NULL AND b.closed_at IS NOT NULL "
                        +
                        "GROUP BY b.assignee_id", nativeQuery = true)
        List<Object[]> getAverageResolutionTimeByAssignee(@Param("projectId") UUID projectId);

        // Date-filtered bug counting methods
        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId " +
                        "AND b.createdAt >= :startDate AND b.createdAt <= :endDate")
        long countBugsInPeriod(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(b) FROM Bug b WHERE b.project.id = :projectId " +
                        "AND b.status = :status " +
                        "AND b.createdAt >= :startDate AND b.createdAt <= :endDate")
        long countBugsInPeriodByStatus(@Param("projectId") UUID projectId,
                        @Param("status") BugStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Date-filtered team performance methods
        @Query("SELECT b.assignee.id, u.firstName, u.lastName, COUNT(b), " +
                        "COUNT(CASE WHEN b.status IN ('FIXED', 'CLOSED') THEN 1 END) " +
                        "FROM Bug b JOIN b.assignee u WHERE b.project.id = :projectId " +
                        "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
                        "GROUP BY b.assignee.id, u.firstName, u.lastName")
        List<Object[]> getAssigneeStatisticsByProjectIdInPeriod(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT b.reporter.id, u.firstName, u.lastName, COUNT(b) " +
                        "FROM Bug b JOIN b.reporter u WHERE b.project.id = :projectId " +
                        "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
                        "GROUP BY b.reporter.id, u.firstName, u.lastName")
        List<Object[]> getReporterStatisticsByProjectIdInPeriod(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Get average resolution time by assignee in a specific period (in days)
        @Query(value = "SELECT b.assignee_id, AVG(EXTRACT(EPOCH FROM (b.closed_at - b.created_at))/86400) " +
                        "FROM bugs b WHERE b.project_id = :projectId AND b.assignee_id IS NOT NULL AND b.closed_at IS NOT NULL "
                        +
                        "AND b.created_at >= :startDate AND b.created_at <= :endDate " +
                        "GROUP BY b.assignee_id", nativeQuery = true)
        List<Object[]> getAverageResolutionTimeByAssigneeInPeriod(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Date-filtered distribution methods
        @Query("SELECT b.priority, COUNT(b) FROM Bug b WHERE b.project.id = :projectId " +
                        "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
                        "GROUP BY b.priority")
        List<Object[]> getPriorityDistributionByProjectIdInPeriod(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT b.type, COUNT(b) FROM Bug b WHERE b.project.id = :projectId " +
                        "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
                        "GROUP BY b.type")
        List<Object[]> getTypeDistributionByProjectIdInPeriod(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT b.status, COUNT(b) FROM Bug b WHERE b.project.id = :projectId " +
                        "AND b.createdAt >= :startDate AND b.createdAt <= :endDate " +
                        "GROUP BY b.status")
        List<Object[]> getStatusDistributionByProjectIdInPeriod(@Param("projectId") UUID projectId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Risk analysis queries
        @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId AND b.createdAt < :thresholdDate")
        List<Bug> findBugsOlderThan(@Param("projectId") UUID projectId,
                        @Param("thresholdDate") LocalDateTime thresholdDate);

        // Find bug by project and ID with labels
        @Query("SELECT DISTINCT b FROM Bug b " +
                        "LEFT JOIN FETCH b.labels " +
                        "WHERE b.project.id = :projectId AND b.id = :bugId")
        Optional<Bug> findByProjectIdAndIdWithLabels(@Param("projectId") UUID projectId, @Param("bugId") Long bugId);

        // Find bug by project and ID with attachments
        @Query("SELECT DISTINCT b FROM Bug b " +
                        "LEFT JOIN FETCH b.attachments " +
                        "WHERE b.project.id = :projectId AND b.id = :bugId")
        Optional<Bug> findByProjectIdAndIdWithAttachments(@Param("projectId") UUID projectId,
                        @Param("bugId") Long bugId);

        // Find bug by project and ID with comments
        @Query("SELECT DISTINCT b FROM Bug b " +
                        "LEFT JOIN FETCH b.comments " +
                        "WHERE b.project.id = :projectId AND b.id = :bugId")
        Optional<Bug> findByProjectIdAndIdWithComments(@Param("projectId") UUID projectId, @Param("bugId") Long bugId);
}