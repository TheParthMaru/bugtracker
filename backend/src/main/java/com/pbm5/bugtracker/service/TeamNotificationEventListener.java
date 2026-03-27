package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.pbm5.bugtracker.entity.*;
import com.pbm5.bugtracker.service.NotificationService;

import java.util.HashMap;
import java.util.Map;

/**
 * Event listener service for team-related notifications.
 * 
 * Handles notification creation and delivery for team events including:
 * - Team role changes
 * - Team member joins
 * - Team member removals
 * 
 * Integration Points:
 * - Called by TeamService after successful operations
 * - Processes template variables for personalized notifications
 * - Coordinates with NotificationService for delivery
 * 
 * Business Rules:
 * - Only notify users who have opted in for specific event types
 * - Template variables include all relevant team and user information
 * - Notifications are sent asynchronously to avoid blocking team operations
 * 
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamNotificationEventListener {

        private final NotificationService notificationService;

        /**
         * Handle team role change notification.
         * 
         * Notifies the user when their role in a team changes.
         * 
         * @param team      The team where role changed
         * @param user      The user whose role changed
         * @param oldRole   The previous role
         * @param newRole   The new role
         * @param changedBy The user who performed the role change
         */
        @Async
        public void onTeamRoleChanged(Team team, User user, TeamRole oldRole, TeamRole newRole, User changedBy) {
                try {
                        log.info(
                                        "TeamNotificationEventListener.onTeamRoleChanged - Starting notification processing: teamId={}, teamName='{}', userId={}, userEmail={}, oldRole={}, newRole={}, changedByEmail={}",
                                        team.getId(), team.getName(), user.getId(), user.getEmail(),
                                        oldRole, newRole, changedBy != null ? changedBy.getEmail() : "system");

                        // Prepare template variables
                        Map<String, Object> templateVariables = createTeamTemplateVariables(team);
                        templateVariables.put("oldRole", oldRole.toString());
                        templateVariables.put("newRole", newRole.toString());
                        templateVariables.put("updatedByName",
                                        changedBy != null ? changedBy.getFirstName() + " " + changedBy.getLastName()
                                                        : "System");
                        templateVariables.put("updatedByEmail",
                                        changedBy != null ? changedBy.getEmail() : "system@bugtracker.com");

                        // Create and deliver notification
                        String title = String.format("Your role in team '%s' has been updated", team.getName());
                        String message = String.format("Your role in team '%s' has been changed from %s to %s by %s",
                                        team.getName(), oldRole, newRole,
                                        changedBy != null ? changedBy.getFirstName() + " " + changedBy.getLastName()
                                                        : "System");

                        log.info(
                                        "TeamNotificationEventListener.onTeamRoleChanged - Calling notification service: title='{}', message='{}'",
                                        title, message);

                        notificationService.createAndDeliverNotification(
                                        user.getId(),
                                        "TEAM_ROLE_CHANGED",
                                        title,
                                        message,
                                        createNotificationData("team_role_change", team, user, changedBy),
                                        templateVariables,
                                        null, // No bug context
                                        null, // No project context
                                        team,
                                        changedBy);

                        log.info(
                                        "TeamNotificationEventListener.onTeamRoleChanged - Notification processing completed: teamId={}, userId={}, oldRole={}, newRole={}, changedByEmail={}",
                                        team.getId(), user.getId(), oldRole, newRole,
                                        changedBy != null ? changedBy.getEmail() : "system");

                } catch (Exception e) {
                        log.error(
                                        "TeamNotificationEventListener.onTeamRoleChanged - Failed to send team role change notification: teamId={}, userId={}, oldRole={}, newRole={}, error={}, stackTrace={}",
                                        team.getId(), user.getId(), oldRole, newRole, e.getMessage(),
                                        e.getStackTrace());
                }
        }

        /**
         * Handle team member joined notification.
         * 
         * Notifies the user when they are added to a team.
         * 
         * @param team    The team the user joined
         * @param user    The user who joined the team
         * @param addedBy The user who added the member
         * @param role    The role assigned to the new member
         */
        @Async
        public void onTeamMemberJoined(Team team, User user, User addedBy, TeamRole role) {
                try {
                        log.info(
                                        "TeamNotificationEventListener.onTeamMemberJoined - Starting notification processing: teamId={}, teamName='{}', userId={}, userEmail={}, role={}, addedByEmail={}",
                                        team.getId(), team.getName(), user.getId(), user.getEmail(),
                                        role, addedBy != null ? addedBy.getEmail() : "system");

                        // Prepare template variables
                        Map<String, Object> templateVariables = createTeamTemplateVariables(team);
                        templateVariables.put("role", role.toString());
                        templateVariables.put("newMemberName", user.getFirstName() + " " + user.getLastName());
                        templateVariables.put("addedByName",
                                        addedBy != null ? addedBy.getFirstName() + " " + addedBy.getLastName()
                                                        : "System");
                        templateVariables.put("addedByEmail",
                                        addedBy != null ? addedBy.getEmail() : "system@bugtracker.com");

                        // Create and deliver notification
                        String title = String.format("You have been added to team '%s'", team.getName());
                        String message = String.format("You have been added to team '%s' as %s by %s",
                                        team.getName(), role,
                                        addedBy != null ? addedBy.getFirstName() + " " + addedBy.getLastName()
                                                        : "System");

                        log.info(
                                        "TeamNotificationEventListener.onTeamMemberJoined - Calling notification service: title='{}', message='{}'",
                                        title, message);

                        notificationService.createAndDeliverNotification(
                                        user.getId(),
                                        "TEAM_MEMBER_JOINED",
                                        title,
                                        message,
                                        createNotificationData("team_member_joined", team, user, addedBy),
                                        templateVariables,
                                        null, // No bug context
                                        null, // No project context
                                        team,
                                        addedBy);

                        log.info(
                                        "TeamNotificationEventListener.onTeamMemberJoined - Notification processing completed: teamId={}, userId={}, role={}, addedByEmail={}",
                                        team.getId(), user.getId(), role,
                                        addedBy != null ? addedBy.getEmail() : "system");

                } catch (Exception e) {
                        log.error(
                                        "TeamNotificationEventListener.onTeamMemberJoined - Failed to send team member joined notification: teamId={}, userId={}, role={}, error={}, stackTrace={}",
                                        team.getId(), user.getId(), role, e.getMessage(), e.getStackTrace());
                }
        }

        /**
         * Create template variables for team-related notifications.
         * 
         * @param team The team
         * @return Map of template variables
         */
        private Map<String, Object> createTeamTemplateVariables(Team team) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("teamId", team.getId().toString());
                variables.put("teamName", team.getName());
                variables.put("teamDescription", team.getDescription() != null ? team.getDescription() : "");
                variables.put("teamSlug", team.getTeamSlug());
                variables.put("projectId", team.getProjectId().toString());
                // Build team URL - teams are accessed via project context
                variables.put("teamUrl",
                                String.format("/projects/%s/teams/%s", team.getProjectId(), team.getTeamSlug()));
                return variables;
        }

        /**
         * Create notification data for team-related notifications.
         * 
         * @param actionType The type of action (e.g., "team_role_change",
         *                   "team_member_joined")
         * @param team       The team
         * @param user       The user
         * @param actor      The user who performed the action
         * @return JSON string of notification data
         */
        private String createNotificationData(String actionType, Team team, User user, User actor) {
                return String.format("""
                                {
                                    "actionType": "%s",
                                    "teamId": "%s",
                                    "teamName": "%s",
                                    "teamSlug": "%s",
                                    "projectId": "%s",
                                    "userId": "%s",
                                    "userEmail": "%s",
                                    "userName": "%s",
                                    "actorId": "%s",
                                    "actorEmail": "%s",
                                    "actorName": "%s"
                                }""",
                                actionType,
                                team.getId().toString(),
                                team.getName(),
                                team.getTeamSlug(),
                                team.getProjectId().toString(),
                                user.getId().toString(),
                                user.getEmail(),
                                user.getFirstName() + " " + user.getLastName(),
                                actor != null ? actor.getId().toString() : "",
                                actor != null ? actor.getEmail() : "",
                                actor != null ? actor.getFirstName() + " " + actor.getLastName() : "");
        }
}
