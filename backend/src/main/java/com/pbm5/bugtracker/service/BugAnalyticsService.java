package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.BugPriority;
import com.pbm5.bugtracker.entity.BugStatus;
import com.pbm5.bugtracker.entity.BugType;
import com.pbm5.bugtracker.entity.TeamMember;
import com.pbm5.bugtracker.repository.BugRepository;
import com.pbm5.bugtracker.repository.TeamMemberRepository;
import com.pbm5.bugtracker.repository.UserRepository;
import com.pbm5.bugtracker.repository.TeamRepository;
import com.pbm5.bugtracker.dto.ProjectResponse;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.entity.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
public class BugAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(BugAnalyticsService.class);

    @Autowired
    private BugRepository bugRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    // Helper method to convert project slug to UUID with proper user authentication
    private UUID getProjectIdFromSlug(String projectSlug) {
        // Get the authenticated user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("User not authenticated");
            throw new SecurityException("User not authenticated");
        }

        // Extract user email from the User object in the principal
        String userEmail;
        if (authentication.getPrincipal() instanceof com.pbm5.bugtracker.entity.User) {
            com.pbm5.bugtracker.entity.User user = (com.pbm5.bugtracker.entity.User) authentication.getPrincipal();
            userEmail = user.getEmail();
            log.debug("Extracted email from User principal: {}", userEmail);
        } else {
            // Fallback to getName() if principal is not a User object
            userEmail = authentication.getName();
            log.debug("Using authentication.getName() as fallback: {}", userEmail);
        }

        if (userEmail == null || userEmail.isEmpty()) {
            log.error("User email not found in authentication");
            throw new SecurityException("User email not found in authentication");
        }

        log.debug("Processing analytics request for user email: {} and project slug: {}", userEmail, projectSlug);

        try {
            // SOLUTION 2: Use security context directly instead of database lookup
            // This avoids the database connection/transaction issues

            // Get user ID from email (keeping this for debugging, but with better error
            // handling)
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            log.debug("User lookup result: {}", userOpt.isPresent() ? userOpt.get() : "NOT_FOUND");

            UUID userId;
            if (userOpt.isPresent()) {
                userId = userOpt.get().getId();
                log.debug("Retrieved user ID from database: {} for email: {}", userId, userEmail);
            } else {
                // FALLBACK: If database lookup fails, we need to handle this gracefully
                // Since ProjectService.getProjectBySlug requires a UUID, we'll throw a more
                // helpful error
                log.error(
                        "Database lookup failed for user: {}. This indicates a database connection or transaction issue.",
                        userEmail);
                throw new SecurityException("Database authentication failed for user: " + userEmail +
                        ". Please check database connection and try again. If the issue persists, contact system administrator.");
            }

            // Get project by slug for the authenticated user
            ProjectResponse project = projectService.getProjectBySlug(projectSlug, userId);
            if (project == null) {
                log.error("Project not found or access denied: {} for user: {}", projectSlug, userEmail);
                throw new IllegalArgumentException("Project not found or access denied: " + projectSlug);
            }

            log.debug("Successfully retrieved project: {} with ID: {}", project.getName(), project.getId());
            return project.getId();

        } catch (Exception e) {
            log.error("Failed to get project by slug: {} for user: {}", projectSlug, userEmail, e);
            throw new IllegalArgumentException("Failed to get project by slug: " + projectSlug, e);
        }
    }

    // Basic Statistics

    /**
     * Get comprehensive bug statistics for a project
     */
    public ProjectBugStatistics getProjectStatistics(String projectSlug, LocalDateTime startDate,
            LocalDateTime endDate) {
        log.debug("Getting project statistics for project slug: {} with date range: {} to {}",
                projectSlug, startDate, endDate);
        try {
            UUID projectId = getProjectIdFromSlug(projectSlug);
            log.debug("Retrieved project ID: {} for slug: {}", projectId, projectSlug);

            long totalBugs, openBugs, fixedBugs, closedBugs, reopenedBugs;

            // If no date range provided, get all-time statistics
            if (startDate == null || endDate == null) {
                log.debug("No date range provided, getting all-time statistics");
                totalBugs = bugRepository.countByProjectId(projectId);
                openBugs = bugRepository.countByProjectIdAndStatus(projectId, BugStatus.OPEN);
                fixedBugs = bugRepository.countByProjectIdAndStatus(projectId, BugStatus.FIXED);
                closedBugs = bugRepository.countByProjectIdAndStatus(projectId, BugStatus.CLOSED);
                reopenedBugs = bugRepository.countByProjectIdAndStatus(projectId, BugStatus.REOPENED);
            } else {
                log.debug("Date range provided, getting filtered statistics from {} to {}", startDate, endDate);
                totalBugs = bugRepository.countBugsInPeriod(projectId, startDate, endDate);
                openBugs = bugRepository.countBugsInPeriodByStatus(projectId, BugStatus.OPEN, startDate, endDate);
                fixedBugs = bugRepository.countBugsInPeriodByStatus(projectId, BugStatus.FIXED, startDate, endDate);
                closedBugs = bugRepository.countBugsInPeriodByStatus(projectId, BugStatus.CLOSED, startDate, endDate);
                reopenedBugs = bugRepository.countBugsInPeriodByStatus(projectId, BugStatus.REOPENED, startDate,
                        endDate);
            }

            log.debug(
                    "Bug counts for project {}: total={}, open={}, fixed={}, closed={}, reopened={} (date range: {} to {})",
                    projectSlug, totalBugs, openBugs, fixedBugs, closedBugs, reopenedBugs, startDate, endDate);

            // Priority distribution
            Map<BugPriority, Long> priorityDistribution;
            if (startDate == null || endDate == null) {
                log.debug("No date range provided, getting all-time priority distribution");
                priorityDistribution = convertToPriorityDistribution(
                        bugRepository.getPriorityDistributionByProjectId(projectId));
            } else {
                log.debug("Date range provided, getting filtered priority distribution from {} to {}", startDate,
                        endDate);
                priorityDistribution = convertToPriorityDistribution(
                        bugRepository.getPriorityDistributionByProjectIdInPeriod(projectId, startDate, endDate));
            }

            // Type distribution
            Map<BugType, Long> typeDistribution;
            if (startDate == null || endDate == null) {
                log.debug("No date range provided, getting all-time type distribution");
                typeDistribution = convertToTypeDistribution(
                        bugRepository.getTypeDistributionByProjectId(projectId));
            } else {
                log.debug("Date range provided, getting filtered type distribution from {} to {}", startDate, endDate);
                typeDistribution = convertToTypeDistribution(
                        bugRepository.getTypeDistributionByProjectIdInPeriod(projectId, startDate, endDate));
            }

            // Status distribution
            Map<BugStatus, Long> statusDistribution;
            if (startDate == null || endDate == null) {
                log.debug("No date range provided, getting all-time status distribution");
                statusDistribution = convertToStatusDistribution(
                        bugRepository.getStatusDistributionByProjectId(projectId));
            } else {
                log.debug("Date range provided, getting filtered status distribution from {} to {}", startDate,
                        endDate);
                statusDistribution = convertToStatusDistribution(
                        bugRepository.getStatusDistributionByProjectIdInPeriod(projectId, startDate, endDate));
            }

            ProjectBugStatistics stats = new ProjectBugStatistics(
                    totalBugs, openBugs, fixedBugs, closedBugs, reopenedBugs,
                    priorityDistribution, typeDistribution, statusDistribution);

            log.debug(
                    "Successfully generated statistics for project: {} with distributions - Priority: {} entries, Type: {} entries, Status: {} entries",
                    projectSlug, priorityDistribution.size(), typeDistribution.size(), statusDistribution.size());
            return stats;

        } catch (Exception e) {
            log.error("Failed to get project statistics for project: {}", projectSlug, e);
            throw e;
        }
    }

    // Helper methods to convert Object[] to Map<Enum, Long>
    private Map<BugPriority, Long> convertToPriorityDistribution(List<Object[]> results) {
        Map<BugPriority, Long> distribution = new HashMap<>();
        for (Object[] result : results) {
            if (result[0] != null && result[1] != null) {
                distribution.put((BugPriority) result[0], (Long) result[1]);
            }
        }
        return distribution;
    }

    private Map<BugType, Long> convertToTypeDistribution(List<Object[]> results) {
        Map<BugType, Long> distribution = new HashMap<>();
        for (Object[] result : results) {
            if (result[0] != null && result[1] != null) {
                distribution.put((BugType) result[0], (Long) result[1]);
            }
        }
        return distribution;
    }

    private Map<BugStatus, Long> convertToStatusDistribution(List<Object[]> results) {
        Map<BugStatus, Long> distribution = new HashMap<>();
        for (Object[] result : results) {
            if (result[0] != null && result[1] != null) {
                distribution.put((BugStatus) result[0], (Long) result[1]);
            }
        }
        return distribution;
    }

    /**
     * Get team performance statistics
     */
    public TeamPerformanceStatistics getTeamPerformanceStatistics(String projectSlug, LocalDateTime startDate,
            LocalDateTime endDate) {
        UUID projectId = getProjectIdFromSlug(projectSlug);

        // If no date range provided, get all-time statistics
        if (startDate == null || endDate == null) {
            log.debug("No date range provided for team performance, getting all-time statistics");
            List<Object[]> assigneeStatsRaw = bugRepository.getAssigneeStatisticsByProjectId(projectId);
            List<Object[]> reporterStatsRaw = bugRepository.getReporterStatisticsByProjectId(projectId);
            // Get resolution time statistics
            List<Object[]> resolutionTimeStats = bugRepository.getAverageResolutionTimeByAssignee(projectId);

            // Convert raw data to DTOs
            List<AssigneeStats> assigneeStats = assigneeStatsRaw.stream()
                    .map(row -> new AssigneeStats(
                            row[0].toString(), // userId
                            (String) row[1], // firstName
                            (String) row[2], // lastName
                            (Long) row[3], // totalCount
                            (Long) row[4] // resolvedCount
                    ))
                    .toList();

            List<UserStats> reporterStats = reporterStatsRaw.stream()
                    .map(row -> new UserStats(
                            row[0].toString(), // userId
                            (String) row[1], // firstName
                            (String) row[2], // lastName
                            (Long) row[3] // count
                    ))
                    .toList();

            // Get team bug statistics
            List<TeamBugStats> teamBugStats = getTeamBugStatistics(projectSlug, startDate, endDate);

            return new TeamPerformanceStatistics(assigneeStats, reporterStats, resolutionTimeStats, teamBugStats);
        }

        // Date range provided, get filtered statistics
        log.debug("Date range provided for team performance, getting filtered statistics from {} to {}", startDate,
                endDate);
        List<Object[]> assigneeStatsRaw = bugRepository.getAssigneeStatisticsByProjectIdInPeriod(projectId, startDate,
                endDate);
        List<Object[]> reporterStatsRaw = bugRepository.getReporterStatisticsByProjectIdInPeriod(projectId, startDate,
                endDate);
        // Get resolution time statistics for the period
        List<Object[]> resolutionTimeStats = bugRepository.getAverageResolutionTimeByAssigneeInPeriod(projectId,
                startDate, endDate);

        // Convert raw data to DTOs
        List<AssigneeStats> assigneeStats = assigneeStatsRaw.stream()
                .map(row -> new AssigneeStats(
                        row[0].toString(), // userId
                        (String) row[1], // firstName
                        (String) row[2], // lastName
                        (Long) row[3], // totalCount
                        (Long) row[4] // resolvedCount
                ))
                .toList();

        List<UserStats> reporterStats = reporterStatsRaw.stream()
                .map(row -> new UserStats(
                        row[0].toString(), // userId
                        (String) row[1], // firstName
                        (String) row[2], // lastName
                        (Long) row[3] // count
                ))
                .toList();

        // Get team bug statistics
        List<TeamBugStats> teamBugStats = getTeamBugStatistics(projectSlug, startDate, endDate);

        return new TeamPerformanceStatistics(assigneeStats, reporterStats, resolutionTimeStats, teamBugStats);
    }

    /**
     * Get team bug statistics for a project
     */
    public List<TeamBugStats> getTeamBugStatistics(String projectSlug, LocalDateTime startDate, LocalDateTime endDate) {
        UUID projectId = getProjectIdFromSlug(projectSlug);

        // Get all teams in the project using the repository directly
        List<Team> teams = teamRepository.findByProjectId(projectId);
        log.debug("Found {} teams for project {}", teams.size(), projectSlug);

        List<TeamBugStats> teamStats = new ArrayList<>();

        for (Team team : teams) {
            log.debug("Processing team: {} (ID: {})", team.getName(), team.getId());

            // Get team members to debug
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(team.getId());
            log.debug("Team {} has {} members: {}", team.getName(), teamMembers.size(),
                    teamMembers.stream().map(tm -> tm.getUserId()).toList());

            // Get bugs assigned to team members
            List<Bug> teamBugs = bugRepository.findByProjectIdAndTeamId(projectId, team.getId());

            // Debug: Log the actual bugs found
            log.debug("Team {} bugs before filtering: {}", team.getName(),
                    teamBugs.stream().map(bug -> String.format("Bug#%d(assignee:%s)",
                            bug.getProjectTicketNumber(),
                            bug.getAssignee() != null ? bug.getAssignee().getId() : "null")).toList());

            // Apply date filtering if dates are provided
            if (startDate != null && endDate != null) {
                teamBugs = teamBugs.stream()
                        .filter(bug -> bug.getCreatedAt() != null &&
                                !bug.getCreatedAt().isBefore(startDate) &&
                                !bug.getCreatedAt().isAfter(endDate))
                        .toList();
                log.debug("Team {} has {} bugs after date filtering ({} to {})",
                        team.getName(), teamBugs.size(), startDate, endDate);
            }

            log.debug("Team {} has {} bugs assigned", team.getName(), teamBugs.size());

            long totalBugs = teamBugs.size();
            long openBugs = teamBugs.stream().filter(bug -> bug.getStatus() == BugStatus.OPEN).count();
            long resolvedBugs = teamBugs.stream()
                    .filter(bug -> bug.getStatus() == BugStatus.FIXED || bug.getStatus() == BugStatus.CLOSED).count();

            double resolutionRate = totalBugs > 0 ? (double) resolvedBugs / totalBugs : 0.0;

            log.debug("Team {} stats - Total: {}, Open: {}, Resolved: {}, Rate: {:.1f}%",
                    team.getName(), totalBugs, openBugs, resolvedBugs, resolutionRate * 100);

            teamStats.add(new TeamBugStats(
                    team.getId(),
                    team.getName(),
                    team.getDescription(),
                    totalBugs,
                    openBugs,
                    resolvedBugs,
                    resolutionRate));
        }

        return teamStats;
    }

    // Reporting

    /**
     * Generate comprehensive project report
     */
    public ProjectReport generateProjectReport(String projectSlug, LocalDateTime startDate, LocalDateTime endDate) {
        ProjectBugStatistics statistics = getProjectStatistics(projectSlug, startDate, endDate);
        TeamPerformanceStatistics teamStats = getTeamPerformanceStatistics(projectSlug, startDate, endDate);

        return new ProjectReport(
                statistics, teamStats,
                startDate, endDate);
    }

    // Data Transfer Objects

    public static class ProjectBugStatistics {
        private final long totalBugs;
        private final long openBugs;
        private final long fixedBugs;
        private final long closedBugs;
        private final long reopenedBugs;
        private final Map<BugPriority, Long> priorityDistribution;
        private final Map<BugType, Long> typeDistribution;
        private final Map<BugStatus, Long> statusDistribution;

        public ProjectBugStatistics(long totalBugs, long openBugs, long fixedBugs,
                long closedBugs, long reopenedBugs, Map<BugPriority, Long> priorityDistribution,
                Map<BugType, Long> typeDistribution, Map<BugStatus, Long> statusDistribution) {
            this.totalBugs = totalBugs;
            this.openBugs = openBugs;
            this.fixedBugs = fixedBugs;
            this.closedBugs = closedBugs;
            this.reopenedBugs = reopenedBugs;
            this.priorityDistribution = priorityDistribution;
            this.typeDistribution = typeDistribution;
            this.statusDistribution = statusDistribution;
        }

        // Getters
        public long getTotalBugs() {
            return totalBugs;
        }

        public long getOpenBugs() {
            return openBugs;
        }

        public long getFixedBugs() {
            return fixedBugs;
        }

        public long getClosedBugs() {
            return closedBugs;
        }

        public long getReopenedBugs() {
            return reopenedBugs;
        }

        public Map<BugPriority, Long> getPriorityDistribution() {
            return priorityDistribution;
        }

        public Map<BugType, Long> getTypeDistribution() {
            return typeDistribution;
        }

        public Map<BugStatus, Long> getStatusDistribution() {
            return statusDistribution;
        }

        public double getResolutionRate() {
            return totalBugs > 0 ? (double) (fixedBugs + closedBugs) / totalBugs : 0.0;
        }
    }

    public static class TeamBugStats {
        private final UUID teamId;
        private final String teamName;
        private final String teamDescription;
        private final long totalBugs;
        private final long openBugs;
        private final long resolvedBugs;
        private final double resolutionRate;

        public TeamBugStats(UUID teamId, String teamName, String teamDescription,
                long totalBugs, long openBugs, long resolvedBugs, double resolutionRate) {
            this.teamId = teamId;
            this.teamName = teamName;
            this.teamDescription = teamDescription;
            this.totalBugs = totalBugs;
            this.openBugs = openBugs;
            this.resolvedBugs = resolvedBugs;
            this.resolutionRate = resolutionRate;
        }

        // Getters
        public UUID getTeamId() {
            return teamId;
        }

        public String getTeamName() {
            return teamName;
        }

        public String getTeamDescription() {
            return teamDescription;
        }

        public long getTotalBugs() {
            return totalBugs;
        }

        public long getOpenBugs() {
            return openBugs;
        }

        public long getResolvedBugs() {
            return resolvedBugs;
        }

        public double getResolutionRate() {
            return resolutionRate;
        }
    }

    public static class UserStats {
        private final String userId;
        private final String firstName;
        private final String lastName;
        private final long count;

        public UserStats(String userId, String firstName, String lastName, long count) {
            this.userId = userId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.count = count;
        }

        // Getters
        public String getUserId() {
            return userId;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public long getCount() {
            return count;
        }

        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    public static class AssigneeStats extends UserStats {
        private final long resolvedCount;

        public AssigneeStats(String userId, String firstName, String lastName, long totalCount, long resolvedCount) {
            super(userId, firstName, lastName, totalCount);
            this.resolvedCount = resolvedCount;
        }

        public long getResolvedCount() {
            return resolvedCount;
        }
    }

    public static class TeamPerformanceStatistics {
        private final List<AssigneeStats> assigneeStats;
        private final List<UserStats> reporterStats;
        private final List<Object[]> resolutionTimeStats;
        private final List<TeamBugStats> teamBugStats;

        public TeamPerformanceStatistics(List<AssigneeStats> assigneeStats, List<UserStats> reporterStats,
                List<Object[]> resolutionTimeStats, List<TeamBugStats> teamBugStats) {
            this.assigneeStats = assigneeStats;
            this.reporterStats = reporterStats;
            this.resolutionTimeStats = resolutionTimeStats;
            this.teamBugStats = teamBugStats;
        }

        // Getters
        public List<AssigneeStats> getAssigneeStats() {
            return assigneeStats;
        }

        public List<UserStats> getReporterStats() {
            return reporterStats;
        }

        public List<Object[]> getResolutionTimeStats() {
            return resolutionTimeStats;
        }

        public List<TeamBugStats> getTeamBugStats() {
            return teamBugStats;
        }
    }

    public static class ProjectReport {
        private final ProjectBugStatistics statistics;
        private final TeamPerformanceStatistics teamStats;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

        public ProjectReport(ProjectBugStatistics statistics, TeamPerformanceStatistics teamStats,
                LocalDateTime startDate, LocalDateTime endDate) {
            this.statistics = statistics;
            this.teamStats = teamStats;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        // Getters
        public ProjectBugStatistics getStatistics() {
            return statistics;
        }

        public TeamPerformanceStatistics getTeamStats() {
            return teamStats;
        }

        public LocalDateTime getStartDate() {
            return startDate;
        }

        public LocalDateTime getEndDate() {
            return endDate;
        }
    }
}