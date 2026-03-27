package com.pbm5.bugtracker.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation;
import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.repository.BugRepository;

import lombok.RequiredArgsConstructor;

/**
 * Orchestrator service that coordinates team and user assignment operations.
 * 
 * Responsibilities:
 * - Coordinate the sequence of team assignment followed by user assignment
 * - Ensure proper order of operations
 * - Handle the complete auto-assignment workflow
 * - Provide a single entry point for the complete assignment process
 * 
 * This service follows the Orchestrator pattern and Single Responsibility
 * Principle
 * by focusing solely on coordinating the assignment workflow.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentOrchestrator.class);

    private final TeamAssignmentService teamAssignmentService;
    private final UserAssignmentService userAssignmentService;
    private final BugRepository bugRepository;

    /**
     * Execute the complete auto-assignment workflow for a bug.
     * This method orchestrates both team and user assignment in the correct
     * sequence.
     * 
     * @param bug the bug to auto-assign
     * @return the assigned user, or null if no suitable user found
     */
    public User executeCompleteAutoAssignment(Bug bug) {
        logger.info("Starting complete auto-assignment workflow for bug: {}", bug.getId());

        try {
            // Step 1: Auto-assign teams based on bug labels
            logger.info("Step 1: Auto-assigning teams for bug: {}", bug.getId());
            TeamAssignmentRecommendation teamRecommendation = teamAssignmentService.getAssignmentRecommendation(bug);

            if (teamRecommendation.getAssignedTeams().isEmpty()) {
                logger.warn("No teams assigned to bug: {}, cannot proceed with user assignment", bug.getId());
                return null;
            }

            // Extract team IDs from the recommendation
            List<UUID> assignedTeamIds = teamRecommendation.getAssignedTeams().stream()
                    .map(team -> team.getTeamId())
                    .collect(Collectors.toList());

            logger.info("Successfully assigned {} teams to bug: {}", assignedTeamIds.size(), bug.getId());

            // Step 2: Auto-assign user based on bug tags and assigned teams
            logger.info("Step 2: Auto-assigning user for bug: {} based on tags and assigned teams", bug.getId());
            User assignedUser = userAssignmentService.autoAssignUserToBug(bug, assignedTeamIds);

            if (assignedUser != null) {
                logger.info("Complete auto-assignment successful for bug: {} - User: {} ({} {})",
                        bug.getId(), assignedUser.getId(), assignedUser.getFirstName(), assignedUser.getLastName());
            } else {
                logger.info("Team assignment successful but no suitable user found for bug: {}", bug.getId());
            }

            return assignedUser;

        } catch (Exception e) {
            logger.error("Error during complete auto-assignment workflow for bug: {}", bug.getId(), e);
            throw new RuntimeException("Failed to execute complete auto-assignment workflow", e);
        }
    }

    /**
     * Execute the complete auto-assignment workflow for a bug by ID.
     * 
     * @param bugId the bug ID to auto-assign
     * @return the assigned user, or null if no suitable user found
     */
    public User executeCompleteAutoAssignmentById(Long bugId) {
        logger.info("Starting complete auto-assignment workflow for bug ID: {}", bugId);

        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new IllegalArgumentException("Bug not found with ID: " + bugId));

        return executeCompleteAutoAssignment(bug);
    }

    /**
     * Execute the complete auto-assignment workflow for a bug by project ticket
     * number.
     * 
     * @param projectId           the project ID
     * @param projectTicketNumber the project ticket number
     * @return the assigned user, or null if no suitable user found
     */
    public User executeCompleteAutoAssignmentByProjectTicket(UUID projectId, Integer projectTicketNumber) {
        logger.info("Starting complete auto-assignment workflow for project: {}, ticket: {}",
                projectId, projectTicketNumber);

        Bug bug = bugRepository.findByProjectIdAndProjectTicketNumber(projectId, projectTicketNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Bug not found with project: %s, ticket: %d", projectId, projectTicketNumber)));

        return executeCompleteAutoAssignment(bug);
    }
}