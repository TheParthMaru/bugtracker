package com.pbm5.bugtracker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pbm5.bugtracker.entity.BugTeamAssignment;

/**
 * Repository interface for managing bug team assignments.
 * Provides methods for querying and managing the relationship between bugs and
 * teams.
 */
@Repository
public interface BugTeamAssignmentRepository extends JpaRepository<BugTeamAssignment, UUID> {

       /**
        * Find all team assignments for a specific bug
        * 
        * @param bugId the bug ID
        * @return list of team assignments for the bug
        */
       @Query("SELECT bta FROM BugTeamAssignment bta WHERE bta.bug.id = :bugId")
       List<BugTeamAssignment> findByBugId(@Param("bugId") Long bugId);

       /**
        * Find all team assignments for a specific team
        * 
        * @param teamId the team ID
        * @return list of team assignments for the team
        */
       @Query("SELECT bta FROM BugTeamAssignment bta WHERE bta.team.id = :teamId")
       List<BugTeamAssignment> findByTeamId(@Param("teamId") UUID teamId);

       /**
        * Find the primary team assignment for a bug
        * 
        * @param bugId the bug ID
        * @return optional containing the primary team assignment
        */
       @Query("SELECT bta FROM BugTeamAssignment bta WHERE bta.bug.id = :bugId AND bta.isPrimary = true")
       Optional<BugTeamAssignment> findByBugIdAndIsPrimaryTrue(@Param("bugId") Long bugId);

       /**
        * Find all team assignments made by a specific user
        * 
        * @param assignedById the user ID who made the assignments
        * @return list of team assignments made by the user
        */
       @Query("SELECT bta FROM BugTeamAssignment bta WHERE bta.assignedBy.id = :assignedById")
       List<BugTeamAssignment> findByAssignedById(@Param("assignedById") UUID assignedById);

       /**
        * Check if a bug has any team assignments
        * 
        * @param bugId the bug ID
        * @return true if the bug has team assignments, false otherwise
        */
       @Query("SELECT COUNT(bta) > 0 FROM BugTeamAssignment bta WHERE bta.bug.id = :bugId")
       boolean existsByBugId(@Param("bugId") Long bugId);

       /**
        * Check if a specific team is assigned to a specific bug
        * 
        * @param bugId  the bug ID
        * @param teamId the team ID
        * @return true if the team is assigned to the bug, false otherwise
        */
       @Query("SELECT COUNT(bta) > 0 FROM BugTeamAssignment bta WHERE bta.bug.id = :bugId AND bta.team.id = :teamId")
       boolean existsByBugIdAndTeamId(@Param("bugId") Long bugId, @Param("teamId") UUID teamId);

       /**
        * Count the number of team assignments for a bug
        * 
        * @param bugId the bug ID
        * @return the number of team assignments
        */
       @Query("SELECT COUNT(bta) FROM BugTeamAssignment bta WHERE bta.bug.id = :bugId")
       long countByBugId(@Param("bugId") Long bugId);

       /**
        * Count the number of team assignments for a team
        * 
        * @param teamId the team ID
        * @return the number of team assignments
        */
       @Query("SELECT COUNT(bta) FROM BugTeamAssignment bta WHERE bta.team.id = :teamId")
       long countByTeamId(@Param("teamId") UUID teamId);

       /**
        * Find all team assignments for bugs in a specific project
        * 
        * @param projectId the project ID
        * @return list of team assignments for bugs in the project
        */
       @Query("SELECT bta FROM BugTeamAssignment bta " +
                     "JOIN bta.bug b " +
                     "WHERE b.project.id = :projectId")
       List<BugTeamAssignment> findByProjectId(@Param("projectId") UUID projectId);

       /**
        * Find all team assignments for a specific bug, ordered by primary status and
        * assignment time
        * 
        * @param bugId the bug ID
        * @return list of team assignments ordered by primary status and assignment
        *         time
        */
       @Query("SELECT bta FROM BugTeamAssignment bta " +
                     "WHERE bta.bug.id = :bugId " +
                     "ORDER BY bta.isPrimary DESC, bta.assignedAt ASC")
       List<BugTeamAssignment> findByBugIdOrdered(@Param("bugId") Long bugId);

       /**
        * Find all team assignments for a specific team, ordered by assignment time
        * 
        * @param teamId the team ID
        * @return list of team assignments ordered by assignment time
        */
       @Query("SELECT bta FROM BugTeamAssignment bta " +
                     "WHERE bta.team.id = :teamId " +
                     "ORDER BY bta.assignedAt DESC")
       List<BugTeamAssignment> findByTeamIdOrdered(@Param("teamId") UUID teamId);

       /**
        * Delete all team assignments for a specific bug
        * 
        * @param bugId the bug ID
        */
       @Modifying(clearAutomatically = true)
       @Query("DELETE FROM BugTeamAssignment bta WHERE bta.bug.id = :bugId")
       int deleteByBugId(@Param("bugId") Long bugId);

       /**
        * Delete a specific team assignment for a bug
        * 
        * @param bugId  the bug ID
        * @param teamId the team ID
        */
       @Modifying(clearAutomatically = true)
       @Query("DELETE FROM BugTeamAssignment bta WHERE bta.bug.id = :bugId AND bta.team.id = :teamId")
       int deleteByBugIdAndTeamId(@Param("bugId") Long bugId, @Param("teamId") UUID teamId);

       /**
        * Find all team assignments for bugs with specific labels
        * 
        * @param labelIds the label IDs to search for
        * @return list of team assignments for bugs with the specified labels
        */
       @Query("SELECT bta FROM BugTeamAssignment bta " +
                     "JOIN bta.bug b " +
                     "JOIN b.labels l " +
                     "WHERE l.id IN :labelIds")
       List<BugTeamAssignment> findByBugLabels(@Param("labelIds") List<Long> labelIds);

}