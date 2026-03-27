package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for team assignment recommendations when creating or updating bugs.
 * Contains information about which teams should be assigned based on bug labels
 * and which team members have relevant skills for the bug tags.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAssignmentRecommendation {

    /**
     * Type of assignment recommendation
     */
    private AssignmentType assignmentType;

    /**
     * Human-readable message explaining the recommendation
     */
    private String message;

    /**
     * List of teams that should be assigned to the bug
     */
    private List<TeamAssignmentInfo> assignedTeams;

    /**
     * Map of team ID to list of skilled team members
     */
    private Map<UUID, List<TeamMemberSkillMatch>> teamMemberSkills;

    /**
     * Labels that were analyzed for team matching
     */
    private Set<String> analyzedLabels;

    /**
     * Tags that were analyzed for skill matching
     */
    private Set<String> analyzedTags;

    /**
     * Timestamp when this recommendation was generated
     */
    private LocalDateTime generatedAt;

    /**
     * Confidence score for the overall recommendation (0.0 to 1.0)
     */
    private Double confidenceScore;

    /**
     * Enum for different types of assignment recommendations
     */
    public enum AssignmentType {
        SINGLE_TEAM, // One team matches the labels
        MULTI_TEAM, // Multiple teams match the labels
        NO_TEAM_FOUND, // No teams match the labels
        MANUAL_OVERRIDE, // User manually specified teams
        PARTIAL_MATCH // Some labels matched, some didn't
    }

    /**
     * Information about a team assignment
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamAssignmentInfo {
        private UUID teamId;
        private String teamName;
        private String teamSlug;
        private String projectSlug;
        private Integer memberCount;
        private List<String> matchingLabels;
        private Double labelMatchScore;
        private Boolean isPrimary; // Primary team gets priority notifications
        private String assignmentReason; // Why this team was selected
    }

    /**
     * Team member with skill relevance information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberSkillMatch {
        private UUID userId;
        private String firstName;
        private String lastName;
        private String email;
        private Set<String> relevantSkills;
        private Set<String> matchingTags;
        private Double skillRelevanceScore; // 0.0 to 1.0
        private String primarySkill; // Most relevant skill for this bug
        private Boolean isAvailable; // Whether member can take on new work
    }

    // Utility methods

    /**
     * Check if any teams were found
     */
    public boolean hasTeams() {
        return assignedTeams != null && !assignedTeams.isEmpty();
    }

    /**
     * Get the number of assigned teams
     */
    public int getTeamCount() {
        return assignedTeams != null ? assignedTeams.size() : 0;
    }

    /**
     * Check if this is a multi-team assignment
     */
    public boolean isMultiTeam() {
        return assignmentType == AssignmentType.MULTI_TEAM;
    }

    /**
     * Check if no teams were found
     */
    public boolean isNoTeamFound() {
        return assignmentType == AssignmentType.NO_TEAM_FOUND;
    }

    /**
     * Get primary team (first team marked as primary, or first team if none marked)
     */
    public TeamAssignmentInfo getPrimaryTeam() {
        if (assignedTeams == null || assignedTeams.isEmpty()) {
            return null;
        }

        return assignedTeams.stream()
                .filter(team -> Boolean.TRUE.equals(team.getIsPrimary()))
                .findFirst()
                .orElse(assignedTeams.get(0));
    }

    /**
     * Get formatted message for UI display
     */
    public String getDisplayMessage() {
        if (message != null) {
            return message;
        }

        switch (assignmentType) {
            case SINGLE_TEAM:
                return "Bug assigned to 1 team";
            case MULTI_TEAM:
                return String.format("Bug assigned to %d teams", getTeamCount());
            case NO_TEAM_FOUND:
                return String.format("No specific team found for labels: %s",
                        String.join(", ", analyzedLabels));
            case PARTIAL_MATCH:
                return String.format("Partial team match - %d teams found for some labels",
                        getTeamCount());
            case MANUAL_OVERRIDE:
                return "Teams manually assigned by user";
            default:
                return "Team assignment recommendation generated";
        }
    }

    /**
     * Get summary of team assignments for logging
     */
    public String getAssignmentSummary() {
        if (!hasTeams()) {
            return "No teams assigned";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Assigned to teams: ");

        for (int i = 0; i < assignedTeams.size(); i++) {
            TeamAssignmentInfo team = assignedTeams.get(i);
            if (i > 0)
                summary.append(", ");
            summary.append(team.getTeamName());

            if (Boolean.TRUE.equals(team.getIsPrimary())) {
                summary.append(" (Primary)");
            }
        }

        return summary.toString();
    }
}