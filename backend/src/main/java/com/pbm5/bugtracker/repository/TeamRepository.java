package com.pbm5.bugtracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pbm5.bugtracker.entity.Team;
import com.pbm5.bugtracker.entity.MemberStatus;

public interface TeamRepository extends JpaRepository<Team, UUID> {

        /**
         * Find a team by its unique slug
         * 
         * @param teamSlug the team slug
         * @return Optional containing the team if found
         */
        Optional<Team> findByTeamSlug(String teamSlug);

        /**
         * Find all teams created by a specific user
         * 
         * @param userId the creator's user ID
         * @return list of teams created by the user
         */
        List<Team> findByCreatedBy(UUID userId);

        /**
         * Find all teams created by a specific user with pagination
         * 
         * @param userId   the creator's user ID
         * @param pageable pagination parameters
         * @return page of teams created by the user
         */
        Page<Team> findByCreatedBy(UUID userId, Pageable pageable);

        /**
         * Search teams by name containing the given string (case-insensitive)
         * 
         * @param name the search term
         * @return list of teams matching the search
         */
        List<Team> findByNameContainingIgnoreCase(String name);

        /**
         * Search teams by name containing the given string with pagination
         * (case-insensitive)
         * 
         * @param name     the search term
         * @param pageable pagination parameters
         * @return page of teams matching the search
         */
        Page<Team> findByNameContainingIgnoreCase(String name, Pageable pageable);

        /**
         * Check if a team with the given slug exists
         * 
         * @param teamSlug the team slug
         * @return true if team exists, false otherwise
         */
        boolean existsByTeamSlug(String teamSlug);

        /**
         * Check if a team with the given name exists (case-insensitive)
         * 
         * @param name the team name
         * @return true if team exists, false otherwise
         */
        boolean existsByNameIgnoreCase(String name);

        /**
         * Find teams by name or description containing the search term
         * (case-insensitive)
         * This is a more comprehensive search method
         * 
         * @param searchTerm the search term
         * @param pageable   pagination parameters
         * @return page of teams matching the search in name or description
         */
        @Query("SELECT t FROM Team t WHERE " +
                        "LOWER(t.name) LIKE LOWER('%' || :searchTerm || '%') OR " +
                        "LOWER(t.description) LIKE LOWER('%' || :searchTerm || '%')")
        Page<Team> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

        /**
         * Count teams created by a specific user
         * 
         * @param userId the creator's user ID
         * @return number of teams created by the user
         */
        long countByCreatedBy(UUID userId);

        // ===== PROJECT-TEAMS INTEGRATION METHODS =====

        /**
         * Find project ID for a given team
         * 
         * @param teamId the team ID
         * @return the project ID, or null if team not found
         */
        @Query("SELECT t.projectId FROM Team t WHERE t.id = :teamId")
        UUID findProjectIdById(@Param("teamId") UUID teamId);

        /**
         * Find teams by project ID
         * 
         * @param projectId the project ID
         * @return list of teams in the project
         */
        List<Team> findByProjectId(UUID projectId);

        /**
         * Find teams by project ID with pagination
         * 
         * @param projectId the project ID
         * @param pageable  pagination parameters
         * @return page of teams in the project
         */
        Page<Team> findByProjectId(UUID projectId, Pageable pageable);

        /**
         * Find teams by project ID and search term with pagination
         * 
         * @param projectId the project ID
         * @param search    search term for team name/description
         * @param pageable  pagination parameters
         * @return page of matching teams in the project
         */
        @Query("SELECT t FROM Team t WHERE t.projectId = :projectId AND " +
                        "(LOWER(t.name) LIKE LOWER('%' || :search || '%') OR " +
                        "LOWER(t.description) LIKE LOWER('%' || :search || '%'))")
        Page<Team> findByProjectIdAndSearchTerm(@Param("projectId") UUID projectId,
                        @Param("search") String search, Pageable pageable);

        /**
         * Check if a team with the given name exists in a project (case-insensitive)
         * 
         * @param projectId the project ID
         * @param name      the team name
         * @return true if team exists in project, false otherwise
         */
        boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

        /**
         * Check if a team with the given slug exists in a project
         * 
         * @param projectId the project ID
         * @param teamSlug  the team slug
         * @return true if team exists in project, false otherwise
         */
        boolean existsByProjectIdAndTeamSlug(UUID projectId, String teamSlug);

        /**
         * Find team by project ID and slug
         * 
         * @param projectId the project ID
         * @param teamSlug  the team slug
         * @return Optional containing the team if found
         */
        Optional<Team> findByProjectIdAndTeamSlug(UUID projectId, String teamSlug);

        /**
         * Count teams in a project
         * 
         * @param projectId the project ID
         * @return number of teams in the project
         */
        long countByProjectId(UUID projectId);

        /**
         * Find teams from projects that a user has access to
         * 
         * @param userId   the user ID
         * @param pageable pagination parameters
         * @return page of teams from accessible projects
         */
        @Query("SELECT DISTINCT t FROM Team t " +
                        "JOIN ProjectMember pm ON t.projectId = pm.project.id " +
                        "WHERE pm.userId = :userId AND pm.status = :status")
        Page<Team> findByUserAccessibleProjects(@Param("userId") UUID userId,
                        @Param("status") MemberStatus status, Pageable pageable);

        /**
         * Find teams from projects that a user has access to with search
         * 
         * @param userId     the user ID
         * @param searchTerm search term for team name/description
         * @param pageable   pagination parameters
         * @return page of teams from accessible projects matching search
         */
        @Query("SELECT DISTINCT t FROM Team t " +
                        "JOIN ProjectMember pm ON t.projectId = pm.project.id " +
                        "WHERE pm.userId = :userId AND pm.status = :status AND " +
                        "(LOWER(t.name) LIKE LOWER('%' || :searchTerm || '%') OR " +
                        "LOWER(t.description) LIKE LOWER('%' || :searchTerm || '%'))")
        Page<Team> findByUserAccessibleProjectsAndSearchTerm(@Param("userId") UUID userId,
                        @Param("searchTerm") String searchTerm, @Param("status") MemberStatus status,
                        Pageable pageable);

}