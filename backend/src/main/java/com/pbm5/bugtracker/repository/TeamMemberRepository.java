package com.pbm5.bugtracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pbm5.bugtracker.entity.TeamMember;
import com.pbm5.bugtracker.entity.TeamRole;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    /**
     * Find a specific team membership by team ID and user ID
     * 
     * @param teamId the team ID
     * @param userId the user ID
     * @return Optional containing the team membership if found
     */
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Find all members of a specific team
     * 
     * @param teamId the team ID
     * @return list of team members
     */
    List<TeamMember> findByTeamId(UUID teamId);

    /**
     * Find all members of a specific team with pagination
     * 
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return page of team members
     */
    Page<TeamMember> findByTeamId(UUID teamId, Pageable pageable);

    /**
     * Find all teams a user is member of
     * 
     * @param userId the user ID
     * @return list of team memberships for the user
     */
    List<TeamMember> findByUserId(UUID userId);

    /**
     * Find all teams a user is member of with pagination
     * 
     * @param userId   the user ID
     * @param pageable pagination parameters
     * @return page of team memberships for the user
     */
    Page<TeamMember> findByUserId(UUID userId, Pageable pageable);

    /**
     * Check if a user is a member of a specific team
     * 
     * @param teamId the team ID
     * @param userId the user ID
     * @return true if user is member, false otherwise
     */
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Find all members of a team with a specific role
     * 
     * @param teamId the team ID
     * @param role   the team role
     * @return list of team members with the specified role
     */
    List<TeamMember> findByTeamIdAndRole(UUID teamId, TeamRole role);

    /**
     * Find all team admins for a specific team
     * 
     * @param teamId the team ID
     * @return list of team admins
     */
    default List<TeamMember> findTeamAdmins(UUID teamId) {
        return findByTeamIdAndRole(teamId, TeamRole.ADMIN);
    }

    /**
     * Find all regular members for a specific team
     * 
     * @param teamId the team ID
     * @return list of regular team members
     */
    default List<TeamMember> findTeamRegularMembers(UUID teamId) {
        return findByTeamIdAndRole(teamId, TeamRole.MEMBER);
    }

    /**
     * Check if a user is an admin of a specific team
     * 
     * @param teamId the team ID
     * @param userId the user ID
     * @return true if user is admin, false otherwise
     */
    boolean existsByTeamIdAndUserIdAndRole(UUID teamId, UUID userId, TeamRole role);

    /**
     * Check if a user is an admin of a specific team
     * 
     * @param teamId the team ID
     * @param userId the user ID
     * @return true if user is admin, false otherwise
     */
    default boolean isUserTeamAdmin(UUID teamId, UUID userId) {
        return existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.ADMIN);
    }

    /**
     * Count total members in a team
     * 
     * @param teamId the team ID
     * @return number of members in the team
     */
    long countByTeamId(UUID teamId);

    /**
     * Count members with a specific role in a team
     * 
     * @param teamId the team ID
     * @param role   the team role
     * @return number of members with the specified role
     */
    long countByTeamIdAndRole(UUID teamId, TeamRole role);

    /**
     * Count total teams a user is member of
     * 
     * @param userId the user ID
     * @return number of teams the user is member of
     */
    long countByUserId(UUID userId);

    /**
     * Delete all memberships for a specific team
     * This is useful when deleting a team
     * 
     * @param teamId the team ID
     */
    void deleteByTeamId(UUID teamId);

    /**
     * Delete all memberships for a specific user
     * This is useful when deleting a user
     * 
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Delete a specific membership
     * 
     * @param teamId the team ID
     * @param userId the user ID
     */
    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Find teams where user has admin role
     * 
     * @param userId the user ID
     * @return list of team memberships where user is admin
     */
    List<TeamMember> findByUserIdAndRole(UUID userId, TeamRole role);

    /**
     * Find teams where user has admin role
     * 
     * @param userId the user ID
     * @return list of team memberships where user is admin
     */
    default List<TeamMember> findUserAdminMemberships(UUID userId) {
        return findByUserIdAndRole(userId, TeamRole.ADMIN);
    }

    /**
     * Find the oldest member of a team (first to join)
     * 
     * @param teamId the team ID
     * @return Optional containing the oldest member if team has members
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId ORDER BY tm.joinedAt ASC")
    Optional<TeamMember> findOldestMemberByTeamId(@Param("teamId") UUID teamId);

    /**
     * Find the newest member of a team (last to join)
     * 
     * @param teamId the team ID
     * @return Optional containing the newest member if team has members
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId ORDER BY tm.joinedAt DESC")
    Optional<TeamMember> findNewestMemberByTeamId(@Param("teamId") UUID teamId);

    /**
     * Find all members of a team ordered by join date
     * 
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return page of team members ordered by join date
     */
    Page<TeamMember> findByTeamIdOrderByJoinedAtAsc(UUID teamId, Pageable pageable);

    /**
     * Find all members of a team ordered by role (admins first)
     * 
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return page of team members ordered by role
     */
    Page<TeamMember> findByTeamIdOrderByRoleAsc(UUID teamId, Pageable pageable);

    // ===== PROJECT-TEAMS INTEGRATION METHODS =====

    /**
     * Find role of a user in a specific team
     * 
     * @param teamId the team ID
     * @param userId the user ID
     * @return Optional containing the user's role in the team
     */
    @Query("SELECT tm.role FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.userId = :userId")
    Optional<TeamRole> findRoleByTeamIdAndUserId(@Param("teamId") UUID teamId, @Param("userId") UUID userId);

    /**
     * Find all team memberships for a user in a specific project
     * 
     * @param userId    the user ID
     * @param projectId the project ID
     * @return list of team memberships for the user in the project
     */
    @Query("SELECT tm FROM TeamMember tm JOIN Team t ON tm.teamId = t.id WHERE tm.userId = :userId AND t.projectId = :projectId")
    List<TeamMember> findByUserIdAndProjectId(@Param("userId") UUID userId, @Param("projectId") UUID projectId);

    /**
     * Count team memberships for a user in a specific project
     * 
     * @param userId    the user ID
     * @param projectId the project ID
     * @return number of team memberships for the user in the project
     */
    @Query("SELECT COUNT(tm) FROM TeamMember tm JOIN Team t ON tm.teamId = t.id WHERE tm.userId = :userId AND t.projectId = :projectId")
    long countByUserIdAndProjectId(@Param("userId") UUID userId, @Param("projectId") UUID projectId);
}