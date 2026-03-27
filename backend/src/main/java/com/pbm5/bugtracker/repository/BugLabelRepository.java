package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.BugLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BugLabelRepository extends JpaRepository<BugLabel, Long> {

        // Basic queries
        Optional<BugLabel> findByName(String name);

        List<BugLabel> findByIsSystem(boolean isSystem);

        List<BugLabel> findByIsSystemOrderByName(boolean isSystem);

        List<BugLabel> findAllByOrderByName();

        // Search queries
        List<BugLabel> findByNameContainingIgnoreCase(String name);

        List<BugLabel> findByNameContainingIgnoreCaseAndIsSystem(String name, boolean isSystem);

        // Analytics queries
        @Query("SELECT COUNT(l) FROM BugLabel l WHERE l.isSystem = :isSystem")
        long countByIsSystem(@Param("isSystem") boolean isSystem);

        @Query("SELECT DISTINCT l FROM BugLabel l JOIN l.bugs b WHERE b.project.id = :projectId")
        List<BugLabel> findLabelsUsedInProject(@Param("projectId") Long projectId);

        @Query("SELECT l FROM BugLabel l WHERE l.id IN " +
                        "(SELECT DISTINCT l2.id FROM BugLabel l2 JOIN l2.bugs b WHERE b.project.id = :projectId) " +
                        "ORDER BY (SELECT COUNT(b2) FROM Bug b2 JOIN b2.labels l3 WHERE l3.id = l.id AND b2.project.id = :projectId) DESC")
        List<BugLabel> findLabelsUsedInProjectOrderByUsage(@Param("projectId") Long projectId);

        @Query("SELECT l.name, COUNT(b) as usageCount FROM BugLabel l " +
                        "LEFT JOIN l.bugs b ON b.project.id = :projectId " +
                        "WHERE l.isSystem = false " +
                        "GROUP BY l.id, l.name " +
                        "ORDER BY usageCount DESC")
        List<Object[]> findLabelUsageStats(@Param("projectId") UUID projectId);

        // Exists queries for validation
        boolean existsByName(String name);

        boolean existsByNameAndIdNot(String name, Long id);

        // Custom label queries
        @Query("SELECT DISTINCT l FROM BugLabel l JOIN l.bugs b WHERE l.isSystem = false AND b.project.id = :projectId")
        List<BugLabel> findCustomLabelsUsedInProject(@Param("projectId") Long projectId);

        // Unused labels
        @Query("SELECT l FROM BugLabel l WHERE l.isSystem = false AND l.id NOT IN " +
                        "(SELECT DISTINCT l2.id FROM BugLabel l2 JOIN l2.bugs b)")
        List<BugLabel> findUnusedCustomLabels();

        // Labels by bug count
        @Query("SELECT l FROM BugLabel l WHERE l.id IN " +
                        "(SELECT l2.id FROM BugLabel l2 JOIN l2.bugs b GROUP BY l2.id HAVING COUNT(b) >= :minBugCount)")
        List<BugLabel> findLabelsWithMinimumBugCount(@Param("minBugCount") long minBugCount);
}