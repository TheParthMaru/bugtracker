package com.pbm5.bugtracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pbm5.bugtracker.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
        Optional<User> findByEmail(String email);

        boolean existsByEmail(String email);

        /**
         * Search users by name or email containing the search term
         * Optimized version with better performance
         */
        @Query("SELECT u FROM User u WHERE " +
                        "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
        Page<User> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

        /**
         * Fast search for invitation purposes - optimized for common search patterns
         * Searches for exact matches first, then partial matches
         */
        @Query("SELECT u FROM User u WHERE " +
                        "LOWER(u.firstName) = LOWER(:searchTerm) OR " +
                        "LOWER(u.lastName) = LOWER(:searchTerm) OR " +
                        "LOWER(u.email) = LOWER(:searchTerm) OR " +
                        "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(u.firstName) LIKE LOWER(CONCAT(:searchTerm, '%')) OR " +
                        "LOWER(u.lastName) LIKE LOWER(CONCAT(:searchTerm, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT(:searchTerm, '%'))")
        Page<User> findBySearchTermOptimized(@Param("searchTerm") String searchTerm, Pageable pageable);

        /**
         * Search users by name or email containing the search term and specific role
         */
        @Query("SELECT u FROM User u WHERE " +
                        "u.role = :role AND (" +
                        "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
        Page<User> findBySearchTermAndRole(@Param("searchTerm") String searchTerm,
                        @Param("role") String role,
                        Pageable pageable);

        /**
         * Find users by role
         */
        Page<User> findByRole(String role, Pageable pageable);

        /**
         * Find all users who are members of a specific project
         */
        @Query("SELECT u FROM User u " +
                        "JOIN ProjectMember pm ON u.id = pm.userId " +
                        "WHERE pm.project.id = :projectId AND pm.status = 'ACTIVE'")
        Page<User> findProjectMembers(@Param("projectId") UUID projectId, Pageable pageable);

        /**
         * Search project members by name or email containing the search term
         */
        @Query("SELECT u FROM User u " +
                        "JOIN ProjectMember pm ON u.id = pm.userId " +
                        "WHERE pm.project.id = :projectId AND pm.status = 'ACTIVE' AND (" +
                        "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
        Page<User> findProjectMembersBySearchTerm(@Param("projectId") UUID projectId,
                        @Param("searchTerm") String searchTerm, Pageable pageable);

        /**
         * Find all active users for bulk operations
         */
        List<User> findAll();
}
