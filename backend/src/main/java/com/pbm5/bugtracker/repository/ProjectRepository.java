package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Project entity operations.
 * Includes queries for project management, search, and validation.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

        // Basic queries
        Optional<Project> findByProjectSlug(String projectSlug);

        Optional<Project> findByProjectSlugAndIsActiveTrue(String projectSlug);

        boolean existsByProjectSlug(String projectSlug);

        // Active project queries
        Page<Project> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

        // Active project queries with dynamic sorting
        Page<Project> findByIsActiveTrue(Pageable pageable);

        long countByIsActiveTrue();

        // Search queries
        Page<Project> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);

        // Admin ownership queries
        List<Project> findByAdminId(UUID adminId);

        List<Project> findByAdminIdAndIsActiveTrue(UUID adminId);

        long countByAdminId(UUID adminId);

        // Validation queries using @Query for better performance
        @Query("SELECT CASE WHEN COUNT(p) = 0 THEN true ELSE false END FROM Project p WHERE LOWER(p.name) = LOWER(:name)")
        boolean isNameAvailable(@Param("name") String name);

        @Query("SELECT CASE WHEN COUNT(p) = 0 THEN true ELSE false END FROM Project p WHERE p.projectSlug = :projectSlug")
        boolean isProjectSlugAvailable(@Param("projectSlug") String projectSlug);
}