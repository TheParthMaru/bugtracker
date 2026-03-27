package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.ProjectMember;
import com.pbm5.bugtracker.entity.ProjectRole;
import com.pbm5.bugtracker.entity.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ProjectMember entity operations.
 * Includes queries for membership management and security checking.
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    // Basic membership lookup - using correct field navigation
    Optional<ProjectMember> findByProject_IdAndUserId(UUID projectId, UUID userId);

    boolean existsByProject_IdAndUserId(UUID projectId, UUID userId);

    boolean existsByProject_IdAndUserIdAndStatus(UUID projectId, UUID userId, MemberStatus status);

    // Security checking methods - needed by ProjectSecurityService
    boolean existsByProject_IdAndUserIdAndStatusAndRole(UUID projectId, UUID userId, MemberStatus status,
            ProjectRole role);

    Optional<ProjectMember> findByProject_IdAndUserIdAndStatus(UUID projectId, UUID userId, MemberStatus status);

    long countByProject_IdAndRoleAndStatus(UUID projectId, ProjectRole role, MemberStatus status);

    // Project membership queries
    List<ProjectMember> findByProject_Id(UUID projectId);

    List<ProjectMember> findByProject_IdAndStatus(UUID projectId, MemberStatus status);

    List<ProjectMember> findByProject_IdAndRole(UUID projectId, ProjectRole role);

    Page<ProjectMember> findByProject_IdAndStatus(UUID projectId, MemberStatus status, Pageable pageable);

    long countByProject_Id(UUID projectId);

    long countByProject_IdAndStatus(UUID projectId, MemberStatus status);

    // User project queries
    List<ProjectMember> findByUserId(UUID userId);

    List<ProjectMember> findByUserIdAndStatus(UUID userId, MemberStatus status);

    List<ProjectMember> findByUserIdAndRole(UUID userId, ProjectRole role);

    Page<ProjectMember> findByUserIdAndStatus(UUID userId, MemberStatus status, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, MemberStatus status);

    // Status and role queries
    Page<ProjectMember> findByStatus(MemberStatus status, Pageable pageable);

    List<ProjectMember> findByRoleAndStatus(ProjectRole role, MemberStatus status);
}