package com.pbm5.bugtracker.service;

import java.util.List;
import java.util.UUID;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.Team;
import com.pbm5.bugtracker.entity.TeamMember;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.repository.TeamMemberRepository;
import com.pbm5.bugtracker.repository.UserRepository;
import com.pbm5.bugtracker.repository.BugRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for automatically assigning users to bugs based on skill matching and
 * availability.
 * 
 * Responsibilities:
 * - Find users with matching skills for bug tags
 * - Calculate user availability scores
 * - Select the best user for assignment
 * - Handle skill matching algorithms (exact vs partial)
 * 
 * This service follows Single Responsibility Principle by focusing solely on
 * user assignment logic.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(UserAssignmentService.class);

    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BugRepository bugRepository;

    // Configuration constants
    private static final double MIN_SKILL_MATCH_SCORE = 0.2;
    private static final double SKILL_WEIGHT = 0.7;
    private static final double AVAILABILITY_WEIGHT = 0.3;
    private static final int MAX_ASSIGNMENTS_FOR_AVAILABILITY = 5;

    /**
     * Auto-assign a bug to the most suitable user based on skill matching and
     * availability.
     * This method should be called after team assignment is complete.
     * 
     * @param bug             the bug to assign
     * @param assignedTeamIds the IDs of teams already assigned to the bug
     * @return the assigned user, or null if no suitable user found
     */
    public User autoAssignUserToBug(Bug bug, List<UUID> assignedTeamIds) {
        logger.info("Auto-assigning user for bug: {} in teams: {}", bug.getId(), assignedTeamIds);

        if (assignedTeamIds == null || assignedTeamIds.isEmpty()) {
            logger.warn("No teams assigned to bug: {}, cannot auto-assign user", bug.getId());
            return null;
        }

        if (bug.getTags() == null || bug.getTags().isEmpty()) {
            logger.info("Bug {} has no tags, cannot perform skill-based user assignment", bug.getId());
            return null;
        }

        // Find the best user across all assigned teams
        User bestUser = null;
        double bestScore = 0.0;

        for (UUID teamId : assignedTeamIds) {
            User teamBestUser = findBestUserInTeam(teamId, bug.getTags(), bug.getProject().getId());
            if (teamBestUser != null) {
                double userScore = calculateUserAssignmentScore(teamBestUser, bug.getTags(), bug.getProject().getId());
                if (userScore > bestScore) {
                    bestScore = userScore;
                    bestUser = teamBestUser;
                }
            }
        }

        if (bestUser != null) {
            logger.info("Auto-assigned bug {} to user: {} ({} {}) with score: {:.2f}",
                    bug.getId(), bestUser.getId(), bestUser.getFirstName(), bestUser.getLastName(), bestScore);
        } else {
            logger.info("No suitable user found for bug: {} based on skill matching", bug.getId());
        }

        return bestUser;
    }

    /**
     * Find the best user in a specific team based on skill matching and
     * availability.
     * 
     * @param teamId    the team ID to search within
     * @param bugTags   the bug tags to match against
     * @param projectId the project ID for availability calculation
     * @return the best matching user, or null if none found
     */
    private User findBestUserInTeam(UUID teamId, Set<String> bugTags, UUID projectId) {
        logger.debug("Finding best user in team: {} for tags: {}", teamId, bugTags);

        List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(teamId);
        if (teamMembers.isEmpty()) {
            logger.debug("No members found in team: {}", teamId);
            return null;
        }

        User bestUser = null;
        double bestScore = 0.0;

        for (TeamMember member : teamMembers) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null || user.getSkills() == null || user.getSkills().isEmpty()) {
                continue;
            }

            double userScore = calculateUserAssignmentScore(user, bugTags, projectId);
            if (userScore > bestScore) {
                bestScore = userScore;
                bestUser = user;
            }
        }

        return bestUser;
    }

    /**
     * Calculate a comprehensive score for user assignment based on skill matching
     * and availability.
     * 
     * @param user      the user to score
     * @param bugTags   the bug tags to match against
     * @param projectId the project ID for availability calculation
     * @return the user's assignment score
     */
    private double calculateUserAssignmentScore(User user, Set<String> bugTags, UUID projectId) {
        double skillScore = calculateSkillMatchingScore(user.getSkills(), bugTags);
        double availabilityScore = calculateUserAvailabilityScore(user, projectId);

        // Weighted scoring: 70% skill matching, 30% availability
        double totalScore = (skillScore * SKILL_WEIGHT) + (availabilityScore * AVAILABILITY_WEIGHT);

        logger.debug("User {} assignment score - Skill: {:.2f}, Availability: {:.2f}, Total: {:.2f}",
                user.getEmail(), skillScore, availabilityScore, totalScore);

        return totalScore;
    }

    /**
     * Calculate skill matching score based on exact and partial matches.
     * 
     * @param userSkills the user's skills
     * @param bugTags    the bug tags to match against
     * @return the skill matching score (0.0 to 1.0)
     */
    private double calculateSkillMatchingScore(Set<String> userSkills, Set<String> bugTags) {
        if (userSkills == null || bugTags == null || userSkills.isEmpty() || bugTags.isEmpty()) {
            return 0.0;
        }

        int exactMatches = 0;
        int partialMatches = 0;
        int totalTags = bugTags.size();

        for (String bugTag : bugTags) {
            String normalizedBugTag = bugTag.toLowerCase().trim();
            boolean tagMatched = false;

            for (String userSkill : userSkills) {
                String normalizedUserSkill = userSkill.toLowerCase().trim();

                // Exact match (highest priority)
                if (normalizedUserSkill.equals(normalizedBugTag)) {
                    exactMatches++;
                    tagMatched = true;
                    break;
                }

                // Partial match (lower priority)
                if (!tagMatched && (normalizedUserSkill.contains(normalizedBugTag) ||
                        normalizedBugTag.contains(normalizedUserSkill))) {
                    partialMatches++;
                    tagMatched = true;
                    break;
                }
            }
        }

        // Scoring: exact matches get full points, partial matches get half points
        double score = (exactMatches + (partialMatches * 0.5)) / totalTags;

        logger.debug("Skill matching for user - Exact: {}, Partial: {}, Total: {}, Score: {:.2f}",
                exactMatches, partialMatches, totalTags, score);

        return score;
    }

    /**
     * Calculate user availability score based on current bug assignments.
     * This method calculates availability based on the number of bugs
     * currently assigned to the user in the specific project.
     * 
     * @param user      the user to check
     * @param projectId the project ID
     * @return the availability score (0.0 to 1.0, higher is more available)
     */
    private double calculateUserAvailabilityScore(User user, UUID projectId) {
        try {
            // Count current bug assignments for this user in this project
            long currentAssignments = bugRepository.countByProjectIdAndAssigneeId(projectId, user.getId());

            // Base availability: fewer assignments = higher availability
            // Normalize to 0.0-1.0 scale (0 assignments = 1.0, 5+ assignments = 0.0)
            double availabilityScore = Math.max(0.0,
                    1.0 - (currentAssignments / (double) MAX_ASSIGNMENTS_FOR_AVAILABILITY));

            logger.debug("User {} availability score: {} current assignments, score: {:.2f}",
                    user.getEmail(), currentAssignments, availabilityScore);

            return availabilityScore;

        } catch (Exception e) {
            logger.warn("Failed to calculate availability for user: {} in project: {}", user.getId(), projectId, e);
            // Return a default score if calculation fails
            return 0.5;
        }
    }
}