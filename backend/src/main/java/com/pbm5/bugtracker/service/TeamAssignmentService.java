package com.pbm5.bugtracker.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation;
import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation.AssignmentType;
import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation.TeamAssignmentInfo;
import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation.TeamMemberSkillMatch;
import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.BugLabel;
import com.pbm5.bugtracker.entity.BugTeamAssignment;
import com.pbm5.bugtracker.entity.Team;
import com.pbm5.bugtracker.entity.TeamMember;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.repository.BugLabelRepository;
import com.pbm5.bugtracker.repository.BugTeamAssignmentRepository;
import com.pbm5.bugtracker.repository.TeamMemberRepository;
import com.pbm5.bugtracker.repository.TeamRepository;
import com.pbm5.bugtracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for automatically assigning bugs to teams based on labels and tags.
 * 
 * Features:
 * - Find teams matching bug labels using intelligent inference
 * - Recommend team members based on bug tags and user skills
 * - Support multi-team assignment for complex bugs
 * - Provide detailed assignment recommendations with confidence scores
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TeamAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(TeamAssignmentService.class);

    private final TeamRepository teamRepository;
    private final BugLabelRepository bugLabelRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final BugTeamAssignmentRepository bugTeamAssignmentRepository;

    // Configuration constants
    private static final double MIN_LABEL_MATCH_SCORE = 0.3;
    private static final double MIN_SKILL_MATCH_SCORE = 0.2;
    private static final int MAX_TEAMS_PER_BUG = 5; // Prevent too many team assignments

    /**
     * Generate team assignment recommendation for a bug based on its labels and
     * tags.
     * 
     * @param bug the bug to analyze
     * @return comprehensive assignment recommendation
     */
    public TeamAssignmentRecommendation getAssignmentRecommendation(Bug bug) {
        logger.info("Generating team assignment recommendation for bug: {}", bug.getId());

        Set<String> analyzedLabels = getLabelNames(bug.getLabels());
        Set<String> analyzedTags = bug.getTags() != null ? bug.getTags() : new HashSet<>();

        // Find teams matching the bug labels
        List<Team> matchingTeams = findTeamsByLabels(bug.getLabels(), bug.getProject().getId());

        if (matchingTeams.isEmpty()) {
            logger.info("No teams found matching labels: {}", analyzedLabels);
            return createNoTeamFoundRecommendation(analyzedLabels, analyzedTags);
        }

        // Limit the number of teams to prevent overwhelming assignments
        if (matchingTeams.size() > MAX_TEAMS_PER_BUG) {
            logger.warn("Too many matching teams ({}) for bug {}, limiting to {}",
                    matchingTeams.size(), bug.getId(), MAX_TEAMS_PER_BUG);
            matchingTeams = matchingTeams.subList(0, MAX_TEAMS_PER_BUG);
        }

        // Create team assignment information
        List<TeamAssignmentInfo> teamAssignments = createTeamAssignments(matchingTeams, bug);

        // Find skilled team members for each team
        Map<UUID, List<TeamMemberSkillMatch>> teamMemberSkills = findSkilledTeamMembers(matchingTeams, analyzedTags);

        // Determine assignment type
        AssignmentType assignmentType = determineAssignmentType(teamAssignments);

        // Calculate confidence score
        double confidenceScore = calculateConfidenceScore(teamAssignments, teamMemberSkills);

        logger.info("Team assignment recommendation generated: {} teams, confidence: {:.2f}",
                teamAssignments.size(), confidenceScore);

        return TeamAssignmentRecommendation.builder()
                .assignmentType(assignmentType)
                .message(generateAssignmentMessage(assignmentType, teamAssignments, analyzedLabels))
                .assignedTeams(teamAssignments)
                .teamMemberSkills(teamMemberSkills)
                .analyzedLabels(analyzedLabels)
                .analyzedTags(analyzedTags)
                .generatedAt(LocalDateTime.now())
                .confidenceScore(confidenceScore)
                .build();
    }

    /**
     * Find teams that match the given bug labels within a project.
     * Uses intelligent inference to match team names/descriptions with labels.
     * 
     * @param labels    the bug labels to match against
     * @param projectId the project ID to search within
     * @return list of matching teams
     */
    public List<Team> findTeamsByLabels(Set<BugLabel> labels, UUID projectId) {
        if (labels == null || labels.isEmpty()) {
            logger.debug("No labels provided for team matching");
            return new ArrayList<>();
        }

        logger.debug("Finding teams for labels: {} in project: {}",
                labels.stream().map(BugLabel::getName).collect(Collectors.toSet()), projectId);

        // Get all teams in the project
        List<Team> projectTeams = teamRepository.findByProjectId(projectId);
        if (projectTeams.isEmpty()) {
            logger.debug("No teams found in project: {}", projectId);
            return new ArrayList<>();
        }

        logger.info("Found {} teams in project {}: {}",
                projectTeams.size(), projectId,
                projectTeams.stream().map(Team::getName).collect(Collectors.joining(", ")));

        // Score each team based on label matching
        List<TeamMatchScore> teamScores = new ArrayList<>();

        for (Team team : projectTeams) {
            double matchScore = calculateTeamLabelMatchScore(team, labels);
            logger.debug("Team '{}' match score: {:.2f} for labels: {}",
                    team.getName(), matchScore,
                    labels.stream().map(BugLabel::getName).collect(Collectors.joining(", ")));

            if (matchScore >= MIN_LABEL_MATCH_SCORE) {
                teamScores.add(new TeamMatchScore(team, matchScore));
                logger.info("Team '{}' qualifies with score {:.2f} (>= {})",
                        team.getName(), matchScore, MIN_LABEL_MATCH_SCORE);
            } else {
                logger.debug("Team '{}' disqualified with score {:.2f} (< {})",
                        team.getName(), matchScore, MIN_LABEL_MATCH_SCORE);
            }
        }

        // Sort by score (highest first) and return teams
        teamScores.sort((a, b) -> Double.compare(b.score, a.score));

        List<Team> matchingTeams = teamScores.stream()
                .map(TeamMatchScore::getTeam)
                .collect(Collectors.toList());

        logger.info("Final result: {} matching teams for labels in project: {}", matchingTeams.size(), projectId);
        if (!matchingTeams.isEmpty()) {
            logger.info("Matching teams: {}",
                    matchingTeams.stream().map(Team::getName).collect(Collectors.joining(", ")));
        }

        return matchingTeams;
    }

    /**
     * Find team members with skills matching the bug tags.
     * 
     * @param teams   the teams to search within
     * @param bugTags the bug tags to match against
     * @return map of team ID to list of skilled members
     */
    public Map<UUID, List<TeamMemberSkillMatch>> findSkilledTeamMembers(List<Team> teams, Set<String> bugTags) {
        if (bugTags == null || bugTags.isEmpty()) {
            logger.debug("No bug tags provided for skill matching");
            return new HashMap<>();
        }

        Map<UUID, List<TeamMemberSkillMatch>> teamMemberSkills = new HashMap<>();

        for (Team team : teams) {
            List<TeamMemberSkillMatch> skilledMembers = findSkilledTeamMembers(team, bugTags);
            teamMemberSkills.put(team.getId(), skilledMembers);
        }

        return teamMemberSkills;
    }

    /**
     * Find skilled team members within a specific team.
     * 
     * @param team    the team to search within
     * @param bugTags the bug tags to match against
     * @return list of team members with relevant skills
     */
    private List<TeamMemberSkillMatch> findSkilledTeamMembers(Team team, Set<String> bugTags) {
        logger.debug("Finding skilled members in team: {} for tags: {}", team.getName(), bugTags);

        // Get all team members
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(team.getId());
        if (teamMembers.isEmpty()) {
            logger.debug("No members found in team: {}", team.getName());
            return new ArrayList<>();
        }

        List<TeamMemberSkillMatch> skilledMembers = new ArrayList<>();

        for (TeamMember member : teamMembers) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null || user.getSkills() == null) {
                continue;
            }

            // Calculate skill relevance
            double skillRelevance = calculateSkillRelevance(user.getSkills(), bugTags);
            if (skillRelevance >= MIN_SKILL_MATCH_SCORE) {
                TeamMemberSkillMatch skillMatch = createTeamMemberSkillMatch(user, bugTags, skillRelevance);
                skilledMembers.add(skillMatch);
            }
        }

        // Sort by skill relevance (highest first)
        skilledMembers.sort((a, b) -> Double.compare(b.getSkillRelevanceScore(), a.getSkillRelevanceScore()));

        logger.debug("Found {} skilled members in team: {}", skilledMembers.size(), team.getName());
        return skilledMembers;
    }

    // Helper methods

    private Set<String> getLabelNames(Set<BugLabel> labels) {
        if (labels == null)
            return new HashSet<>();
        return labels.stream()
                .map(BugLabel::getName)
                .collect(Collectors.toSet());
    }

    private TeamAssignmentRecommendation createNoTeamFoundRecommendation(Set<String> analyzedLabels,
            Set<String> analyzedTags) {
        String message = String.format("No specific team found for labels: %s. " +
                "Consider creating teams for these areas or manually assigning the bug.",
                String.join(", ", analyzedLabels));

        return TeamAssignmentRecommendation.builder()
                .assignmentType(AssignmentType.NO_TEAM_FOUND)
                .message(message)
                .assignedTeams(new ArrayList<>())
                .teamMemberSkills(new HashMap<>())
                .analyzedLabels(analyzedLabels)
                .analyzedTags(analyzedTags)
                .generatedAt(LocalDateTime.now())
                .confidenceScore(0.0)
                .build();
    }

    private List<TeamAssignmentInfo> createTeamAssignments(List<Team> teams, Bug bug) {
        List<TeamAssignmentInfo> assignments = new ArrayList<>();

        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            Set<BugLabel> teamLabels = bug.getLabels(); // For now, assume all labels match

            TeamAssignmentInfo assignment = TeamAssignmentInfo.builder()
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .teamSlug(team.getTeamSlug())
                    .projectSlug(bug.getProject().getProjectSlug())
                    .memberCount(team.getMemberCount())
                    .matchingLabels(new ArrayList<>(getLabelNames(teamLabels)))
                    .labelMatchScore(1.0) // Perfect match for now
                    .isPrimary(i == 0) // First team is primary
                    .assignmentReason("Auto-detected based on bug labels")
                    .build();

            assignments.add(assignment);
        }

        return assignments;
    }

    private AssignmentType determineAssignmentType(List<TeamAssignmentInfo> teamAssignments) {
        if (teamAssignments.isEmpty()) {
            return AssignmentType.NO_TEAM_FOUND;
        } else if (teamAssignments.size() == 1) {
            return AssignmentType.SINGLE_TEAM;
        } else {
            return AssignmentType.MULTI_TEAM;
        }
    }

    private String generateAssignmentMessage(AssignmentType type, List<TeamAssignmentInfo> teams, Set<String> labels) {
        switch (type) {
            case SINGLE_TEAM:
                return String.format("Bug assigned to team: %s", teams.get(0).getTeamName());
            case MULTI_TEAM:
                return String.format("Bug assigned to %d teams for coordinated resolution", teams.size());
            case NO_TEAM_FOUND:
                return String.format("No specific team found for labels: %s", String.join(", ", labels));
            default:
                return "Team assignment recommendation generated";
        }
    }

    private double calculateConfidenceScore(List<TeamAssignmentInfo> teamAssignments,
            Map<UUID, List<TeamMemberSkillMatch>> teamMemberSkills) {
        if (teamAssignments.isEmpty()) {
            return 0.0;
        }

        // Base confidence from team matching
        double teamMatchConfidence = teamAssignments.stream()
                .mapToDouble(TeamAssignmentInfo::getLabelMatchScore)
                .average()
                .orElse(0.0);

        // Skill match confidence
        double skillMatchConfidence = 0.0;
        int totalMembers = 0;

        for (List<TeamMemberSkillMatch> members : teamMemberSkills.values()) {
            if (!members.isEmpty()) {
                skillMatchConfidence += members.stream()
                        .mapToDouble(TeamMemberSkillMatch::getSkillRelevanceScore)
                        .max()
                        .orElse(0.0);
                totalMembers++;
            }
        }

        if (totalMembers > 0) {
            skillMatchConfidence /= totalMembers;
        }

        // Weighted combination: 70% team match, 30% skill match
        return (teamMatchConfidence * 0.7) + (skillMatchConfidence * 0.3);
    }

    private double calculateTeamLabelMatchScore(Team team, Set<BugLabel> labels) {
        if (labels == null || labels.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        int labelCount = labels.size();

        for (BugLabel label : labels) {
            double labelScore = calculateSingleLabelMatchScore(team, label);
            totalScore += labelScore;
        }

        return totalScore / labelCount;
    }

    private double calculateSingleLabelMatchScore(Team team, BugLabel label) {
        String labelName = label.getName().toLowerCase();
        String teamName = team.getName().toLowerCase();
        String teamDescription = team.getDescription() != null ? team.getDescription().toLowerCase() : "";

        logger.debug("Calculating match score for label '{}' vs team '{}' (desc: '{}')",
                labelName, teamName, teamDescription);

        // Exact match in team name
        if (teamName.contains(labelName)) {
            logger.debug("Exact match in team name: '{}' contains '{}' -> score 1.0", teamName, labelName);
            return 1.0;
        }

        // Partial match in team name
        if (labelName.contains(teamName) || teamName.contains(labelName)) {
            logger.debug("Partial match in team name: '{}' vs '{}' -> score 0.8", labelName, teamName);
            return 0.8;
        }

        // Match in team description
        if (teamDescription.contains(labelName)) {
            logger.debug("Match in team description: '{}' contains '{}' -> score 0.6", teamDescription, labelName);
            return 0.6;
        }

        // Fuzzy matching for common variations
        if (isLabelTeamMatch(labelName, teamName)) {
            logger.debug("Fuzzy match found for '{}' vs '{}' -> score 0.7", labelName, teamName);
            return 0.7;
        }

        logger.debug("No match found for '{}' vs '{}' -> score 0.0", labelName, teamName);
        return 0.0;
    }

    private boolean isLabelTeamMatch(String labelName, String teamName) {
        // Common label-team name variations
        Map<String, List<String>> labelVariations = Map.of(
                "frontend", Arrays.asList("frontend", "front-end", "front end", "ui", "ux", "client"),
                "backend", Arrays.asList("backend", "back-end", "back end", "server", "api"),
                "database", Arrays.asList("database", "db", "data", "sql"),
                "devops", Arrays.asList("devops", "dev-ops", "deployment", "infrastructure"),
                "testing", Arrays.asList("testing", "test", "qa", "quality"),
                "security", Arrays.asList("security", "sec", "infosec"));

        for (Map.Entry<String, List<String>> entry : labelVariations.entrySet()) {
            if (labelName.contains(entry.getKey())) {
                return entry.getValue().stream().anyMatch(teamName::contains);
            }
        }

        return false;
    }

    private double calculateSkillRelevance(Set<String> userSkills, Set<String> bugTags) {
        if (userSkills == null || bugTags == null || userSkills.isEmpty() || bugTags.isEmpty()) {
            return 0.0;
        }

        int matchingSkills = 0;
        int totalSkills = userSkills.size();

        for (String userSkill : userSkills) {
            String normalizedUserSkill = userSkill.toLowerCase().trim();

            for (String bugTag : bugTags) {
                String normalizedBugTag = bugTag.toLowerCase().trim();

                if (normalizedUserSkill.equals(normalizedBugTag) ||
                        normalizedUserSkill.contains(normalizedBugTag) ||
                        normalizedBugTag.contains(normalizedUserSkill)) {
                    matchingSkills++;
                    break;
                }
            }
        }

        return (double) matchingSkills / totalSkills;
    }

    private TeamMemberSkillMatch createTeamMemberSkillMatch(User user, Set<String> bugTags, double skillRelevance) {
        // Find matching tags
        Set<String> matchingTags = new HashSet<>();
        if (user.getSkills() != null && bugTags != null) {
            for (String userSkill : user.getSkills()) {
                String normalizedUserSkill = userSkill.toLowerCase().trim();

                for (String bugTag : bugTags) {
                    String normalizedBugTag = bugTag.toLowerCase().trim();

                    if (normalizedUserSkill.equals(normalizedBugTag) ||
                            normalizedUserSkill.contains(normalizedBugTag) ||
                            normalizedBugTag.contains(normalizedUserSkill)) {
                        matchingTags.add(bugTag);
                    }
                }
            }
        }

        // Find primary skill (most relevant)
        String primarySkill = matchingTags.stream()
                .findFirst()
                .orElse(user.getSkills() != null && !user.getSkills().isEmpty() ? user.getSkills().iterator().next()
                        : "Unknown");

        return TeamMemberSkillMatch.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .relevantSkills(user.getSkills() != null ? user.getSkills() : new HashSet<>())
                .matchingTags(matchingTags)
                .skillRelevanceScore(skillRelevance)
                .primarySkill(primarySkill)
                .isAvailable(true) // For now, assume all members are available
                .build();
    }

    // Helper class for team scoring
    private static class TeamMatchScore {
        private final Team team;
        private final double score;

        public TeamMatchScore(Team team, double score) {
            this.team = team;
            this.score = score;
        }

        public Team getTeam() {
            return team;
        }

        public double getScore() {
            return score;
        }
    }

    // Team Assignment Persistence Methods

    /**
     * Save team assignments for a bug to the database.
     * This replaces any existing assignments with new ones.
     * 
     * @param bug        the bug to assign teams to
     * @param teamIds    the list of team IDs to assign
     * @param assignedBy the user ID who is making the assignment
     * @return list of created team assignments
     */
    public List<BugTeamAssignment> saveTeamAssignments(Bug bug, Set<UUID> teamIds, UUID assignedBy) {
        logger.info("Saving team assignments for bug: {} to {} teams", bug.getId(), teamIds.size());

        // Remove existing assignments for this bug
        bugTeamAssignmentRepository.deleteByBugId(bug.getId());

        if (teamIds == null || teamIds.isEmpty()) {
            logger.info("No teams to assign for bug: {}", bug.getId());
            return new ArrayList<>();
        }

        List<BugTeamAssignment> assignments = new ArrayList<>();

        for (UUID teamId : teamIds) {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

            User user = userRepository.findById(assignedBy)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + assignedBy));

            BugTeamAssignment assignment = BugTeamAssignment.builder()
                    .bug(bug)
                    .team(team)
                    .assignedBy(user)
                    .isPrimary(assignments.isEmpty()) // First team is primary
                    .build();

            BugTeamAssignment savedAssignment = bugTeamAssignmentRepository.save(assignment);
            assignments.add(savedAssignment);

            logger.debug("Created team assignment: bug {} -> team {}", bug.getId(), team.getName());
        }

        logger.info("Successfully saved {} team assignments for bug: {}", assignments.size(), bug.getId());
        return assignments;
    }

    /**
     * Update team assignments for a bug.
     * This replaces all existing assignments with new ones.
     * 
     * @param bugId     the bug ID to update assignments for
     * @param teamIds   the new list of team IDs to assign
     * @param updatedBy the user ID who is making the update
     * @return list of updated team assignments
     */
    public List<BugTeamAssignment> updateTeamAssignments(Long bugId, Set<UUID> teamIds, UUID updatedBy) {
        logger.info("Updating team assignments for bug: {} to {} teams", bugId, teamIds.size());

        Bug bug = new Bug(); // We only need the ID for the assignment
        bug.setId(bugId);

        return saveTeamAssignments(bug, teamIds, updatedBy);
    }

    /**
     * Remove a specific team assignment for a bug.
     * 
     * @param bugId     the bug ID
     * @param teamId    the team ID to remove
     * @param removedBy the user ID who is removing the assignment
     */
    public void removeTeamAssignment(Long bugId, UUID teamId, UUID removedBy) {
        logger.info("Removing team assignment: bug {} -> team {}", bugId, teamId);

        if (bugTeamAssignmentRepository.existsByBugIdAndTeamId(bugId, teamId)) {
            bugTeamAssignmentRepository.deleteByBugIdAndTeamId(bugId, teamId);
            logger.info("Successfully removed team assignment: bug {} -> team {}", bugId, teamId);
        } else {
            logger.warn("Team assignment not found: bug {} -> team {}", bugId, teamId);
        }
    }

    /**
     * Get all team assignments for a bug.
     * 
     * @param bugId the bug ID
     * @return list of team assignments
     */
    public List<BugTeamAssignment> getBugTeamAssignments(Long bugId) {
        return bugTeamAssignmentRepository.findByBugIdOrdered(bugId);
    }

    /**
     * Get the primary team assignment for a bug.
     * 
     * @param bugId the bug ID
     * @return the primary team assignment, or null if none exists
     */
    public BugTeamAssignment getPrimaryTeamAssignment(Long bugId) {
        return bugTeamAssignmentRepository.findByBugIdAndIsPrimaryTrue(bugId).orElse(null);
    }

    /**
     * Check if a bug has any team assignments
     * 
     * @param bugId the bug ID
     * @return true if the bug has team assignments, false otherwise
     */
    public boolean hasTeamAssignments(Long bugId) {
        return bugTeamAssignmentRepository.existsByBugId(bugId);
    }
}