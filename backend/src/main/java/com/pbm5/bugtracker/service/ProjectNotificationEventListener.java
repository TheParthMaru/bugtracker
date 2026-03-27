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
 * Event listener service for project-related notifications.
 * 
 * Handles notification creation and delivery for project events including:
 * - Project role changes
 * - Project member joins
 * - Project member removals
 * 
 * Integration Points:
 * - Called by ProjectMemberService after successful operations
 * - Processes template variables for personalized notifications
 * - Coordinates with NotificationService for delivery
 * 
 * Business Rules:
 * - Only notify users who have opted in for specific event types
 * - Template variables include all relevant project and user information
 * - Notifications are sent asynchronously to avoid blocking project operations
 * 
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectNotificationEventListener {

        private final NotificationService notificationService;

        /**
         * Handle project role change notification.
         * 
         * Notifies the user when their role in a project changes.
         * 
         * @param project   The project where role changed
         * @param user      The user whose role changed
         * @param oldRole   The previous role
         * @param newRole   The new role
         * @param changedBy The user who performed the role change
         */
        @Async
        public void onProjectRoleChanged(Project project, User user, ProjectRole oldRole, ProjectRole newRole,
                        User changedBy) {
                try {
                        log.info("ProjectNotificationEventListener.onProjectRoleChanged - Starting notification processing: projectId={}, projectName='{}', userId={}, userEmail={}, oldRole={}, newRole={}, changedByEmail={}",
                                        project.getId(), project.getName(), user.getId(), user.getEmail(),
                                        oldRole, newRole, changedBy != null ? changedBy.getEmail() : "system");

                        // Prepare template variables
                        Map<String, Object> templateVariables = createProjectTemplateVariables(project);
                        templateVariables.put("oldRole", oldRole.toString());
                        templateVariables.put("newRole", newRole.toString());
                        templateVariables.put("updatedByName",
                                        changedBy != null ? changedBy.getFirstName() + " " + changedBy.getLastName()
                                                        : "System");
                        templateVariables.put("updatedByEmail",
                                        changedBy != null ? changedBy.getEmail() : "system@bugtracker.com");

                        // Create and deliver notification
                        String title = String.format("Project role updated in '%s'", project.getName());
                        String message = String.format("Your role in project '%s' has been changed from %s to %s by %s",
                                        project.getName(), oldRole, newRole,
                                        changedBy != null ? changedBy.getFirstName() + " " + changedBy.getLastName()
                                                        : "System");

                        log.info("ProjectNotificationEventListener.onProjectRoleChanged - Calling notification service: title='{}', message='{}'",
                                        title, message);

                        notificationService.createAndDeliverNotification(
                                        user.getId(),
                                        "PROJECT_ROLE_CHANGED",
                                        title,
                                        message,
                                        createNotificationData("project_role_change", project, user, changedBy),
                                        templateVariables,
                                        null, // No bug context
                                        project,
                                        null, // No team context
                                        changedBy);

                        log.info("ProjectNotificationEventListener.onProjectRoleChanged - Notification processing completed: projectId={}, userId={}, oldRole={}, newRole={}, changedByEmail={}",
                                        project.getId(), user.getId(), oldRole, newRole,
                                        changedBy != null ? changedBy.getEmail() : "system");

                } catch (Exception e) {
                        log.error("ProjectNotificationEventListener.onProjectRoleChanged - Failed to send project role change notification: projectId={}, userId={}, oldRole={}, newRole={}, error={}, stackTrace={}",
                                        project.getId(), user.getId(), oldRole, newRole, e.getMessage(), e);

                        // Also log to console for immediate visibility during testing
                        System.err.println("Failed to send project role change notification:");
                        System.err.println("  Project ID: " + project.getId());
                        System.err.println("  User ID: " + user.getId());
                        System.err.println("  Old Role: " + oldRole);
                        System.err.println("  New Role: " + newRole);
                        System.err.println("  Error: " + e.getMessage());
                        e.printStackTrace();
                }
        }

        /**
         * Handle project member joined notification.
         * 
         * Notifies the new member when they successfully join a project.
         * 
         * @param project   The project joined
         * @param newMember The new member
         * @param addedBy   The user who added the member
         */
        @Async
        public void onProjectMemberJoined(Project project, User newMember, User addedBy) {
                try {
                        log.info("ProjectNotificationEventListener.onProjectMemberJoined - Starting notification processing: projectId={}, projectName='{}', newMemberId={}, newMemberEmail={}, addedByEmail={}",
                                        project.getId(), project.getName(), newMember.getId(), newMember.getEmail(),
                                        addedBy != null ? addedBy.getEmail() : "system");

                        Map<String, Object> templateVariables = createProjectTemplateVariables(project);
                        templateVariables.put("newMemberName",
                                        newMember.getFirstName() + " " + newMember.getLastName());
                        templateVariables.put("newMemberEmail", newMember.getEmail());
                        templateVariables.put("addedByName",
                                        addedBy != null ? addedBy.getFirstName() + " " + addedBy.getLastName()
                                                        : "System");
                        templateVariables.put("addedByEmail",
                                        addedBy != null ? addedBy.getEmail() : "system@bugtracker.com");

                        String title = String.format("New member joined project '%s'", project.getName());
                        String message = String.format("%s has joined project '%s'",
                                        newMember.getFirstName() + " " + newMember.getLastName(), project.getName());

                        log.info("ProjectNotificationEventListener.onProjectMemberJoined - Calling notification service: title='{}', message='{}'",
                                        title, message);

                        notificationService.createAndDeliverNotification(
                                        newMember.getId(),
                                        "PROJECT_MEMBER_JOINED",
                                        title,
                                        message,
                                        createNotificationData("project_member_joined", project, newMember, addedBy),
                                        templateVariables,
                                        null, // No bug context
                                        project,
                                        null, // No team context
                                        addedBy);

                        log.info("ProjectNotificationEventListener.onProjectMemberJoined - Notification processing completed: projectId={}, newMemberId={}, addedByEmail={}",
                                        project.getId(), newMember.getId(),
                                        addedBy != null ? addedBy.getEmail() : "system");

                } catch (Exception e) {
                        log.error("ProjectNotificationEventListener.onProjectMemberJoined - Failed to send project member joined notification: projectId={}, newMemberId={}, error={}, stackTrace={}",
                                        project.getId(), newMember.getId(), e.getMessage(), e);
                        System.err.println("Failed to send project member joined notification:");
                        System.err.println("  Project ID: " + project.getId());
                        System.err.println("  New Member ID: " + newMember.getId());
                        System.err.println("  Error: " + e.getMessage());
                        e.printStackTrace();
                }
        }

        /**
         * Handle project join request notification.
         * 
         * Notifies project admins when someone requests to join their project.
         * 
         * @param project The project where join request was made
         * @param user    The user requesting to join
         * @param admin   The admin to notify
         */
        @Async
        public void onProjectJoinRequestCreated(Project project, User user, User admin) {
                try {
                        log.info("ProjectNotificationEventListener.onProjectJoinRequestCreated - Starting notification processing: projectId={}, projectName='{}', requesterId={}, requesterEmail={}, adminId={}, adminEmail={}",
                                        project.getId(), project.getName(), user.getId(), user.getEmail(),
                                        admin.getId(), admin.getEmail());

                        Map<String, Object> templateVariables = createProjectTemplateVariables(project);
                        templateVariables.put("requesterName", user.getFirstName() + " " + user.getLastName());
                        templateVariables.put("requesterEmail", user.getEmail());
                        templateVariables.put("adminName", admin.getFirstName() + " " + admin.getLastName());
                        templateVariables.put("adminEmail", admin.getEmail());

                        String title = String.format("Join request for project '%s'", project.getName());
                        String message = String.format("%s has requested to join project '%s'",
                                        user.getFirstName() + " " + user.getLastName(), project.getName());

                        log.info("ProjectNotificationEventListener.onProjectJoinRequestCreated - Calling notification service: title='{}', message='{}'",
                                        title, message);

                        log.info("🔔 About to call notificationService.createAndDeliverNotification for admin {} with eventType PROJECT_MEMBER_JOINED",
                                        admin.getEmail());

                        notificationService.createAndDeliverNotification(
                                        admin.getId(),
                                        "PROJECT_MEMBER_JOINED", // Using existing template - could be improved with
                                                                 // dedicated template
                                        title,
                                        message,
                                        createNotificationData("project_join_request", project, user, admin),
                                        templateVariables,
                                        null, // No bug context
                                        project,
                                        null, // No team context
                                        user);

                        log.info("ProjectNotificationEventListener.onProjectJoinRequestCreated - Notification processing completed: projectId={}, requesterId={}, adminId={}",
                                        project.getId(), user.getId(), admin.getId());

                } catch (Exception e) {
                        log.error("ProjectNotificationEventListener.onProjectJoinRequestCreated - Failed to send join request notification: projectId={}, requesterId={}, adminId={}, error={}, stackTrace={}",
                                        project.getId(), user.getId(), admin.getId(), e.getMessage(), e);
                        System.err.println("Failed to send join request notification:");
                        System.err.println("  Project ID: " + project.getId());
                        System.err.println("  Requester ID: " + user.getId());
                        System.err.println("  Admin ID: " + admin.getId());
                        System.err.println("  Error: " + e.getMessage());
                        e.printStackTrace();
                }
        }

        /**
         * Handle project join request rejection notification.
         * 
         * Notifies the user when their join request is rejected by an admin.
         * 
         * @param project The project where join request was rejected
         * @param user    The user whose request was rejected
         * @param admin   The admin who rejected the request
         */
        @Async
        public void onProjectJoinRequestRejected(Project project, User user, User admin) {
                try {
                        log.info("ProjectNotificationEventListener.onProjectJoinRequestRejected - Starting notification processing: projectId={}, projectName='{}', requesterId={}, requesterEmail={}, adminId={}, adminEmail={}",
                                        project.getId(), project.getName(), user.getId(), user.getEmail(),
                                        admin.getId(), admin.getEmail());

                        Map<String, Object> templateVariables = createProjectTemplateVariables(project);
                        templateVariables.put("requesterName", user.getFirstName() + " " + user.getLastName());
                        templateVariables.put("requesterEmail", user.getEmail());
                        templateVariables.put("adminName", admin.getFirstName() + " " + admin.getLastName());
                        templateVariables.put("adminEmail", admin.getEmail());

                        String title = String.format("Join request rejected for project '%s'", project.getName());
                        String message = String.format("Your request to join project '%s' has been rejected by %s",
                                        project.getName(), admin.getFirstName() + " " + admin.getLastName());

                        log.info("ProjectNotificationEventListener.onProjectJoinRequestRejected - Calling notification service: title='{}', message='{}'",
                                        title, message);

                        notificationService.createAndDeliverNotification(
                                        user.getId(),
                                        "PROJECT_MEMBER_JOINED", // Using existing template for now
                                        title,
                                        message,
                                        createNotificationData("project_join_request_rejected", project, user, admin),
                                        templateVariables,
                                        null, // No bug context
                                        project,
                                        null, // No team context
                                        admin);

                        log.info("ProjectNotificationEventListener.onProjectJoinRequestRejected - Notification processing completed: projectId={}, requesterId={}, adminId={}",
                                        project.getId(), user.getId(), admin.getId());

                } catch (Exception e) {
                        log.error("ProjectNotificationEventListener.onProjectJoinRequestRejected - Failed to send join request rejection notification: projectId={}, requesterId={}, adminId={}, error={}, stackTrace={}",
                                        project.getId(), user.getId(), admin.getId(), e.getMessage(), e);
                        System.err.println("Failed to send join request rejection notification:");
                        System.err.println("  Project ID: " + project.getId());
                        System.err.println("  Requester ID: " + user.getId());
                        System.err.println("  Admin ID: " + admin.getId());
                        System.err.println("  Error: " + e.getMessage());
                        e.printStackTrace();
                }
        }

        /**
         * Handle project join request approval notification.
         * 
         * Notifies the user when their join request is approved by an admin.
         * 
         * @param project The project where join request was approved
         * @param user    The user whose request was approved
         * @param admin   The admin who approved the request
         */
        @Async
        public void onProjectJoinRequestApproved(Project project, User user, User admin) {
                try {
                        log.info("ProjectNotificationEventListener.onProjectJoinRequestApproved - Starting notification processing: projectId={}, projectName='{}', requesterId={}, requesterEmail={}, adminId={}, adminEmail={}",
                                        project.getId(), project.getName(), user.getId(), user.getEmail(),
                                        admin.getId(), admin.getEmail());

                        Map<String, Object> templateVariables = createProjectTemplateVariables(project);
                        templateVariables.put("requesterName", user.getFirstName() + " " + user.getLastName());
                        templateVariables.put("requesterEmail", user.getEmail());
                        templateVariables.put("adminName", admin.getFirstName() + " " + admin.getLastName());
                        templateVariables.put("adminEmail", admin.getEmail());

                        String title = String.format("Join request approved for project '%s'", project.getName());
                        String message = String.format(
                                        "Your request to join project '%s' has been approved by %s. Welcome to the project!",
                                        project.getName(), admin.getFirstName() + " " + admin.getLastName());

                        log.info("ProjectNotificationEventListener.onProjectJoinRequestApproved - Calling notification service: title='{}', message='{}'",
                                        title, message);

                        notificationService.createAndDeliverNotification(
                                        user.getId(),
                                        "PROJECT_MEMBER_JOINED", // Using existing template for now
                                        title,
                                        message,
                                        createNotificationData("project_join_request_approved", project, user, admin),
                                        templateVariables,
                                        null, // No bug context
                                        project,
                                        null, // No team context
                                        admin);

                        log.info("ProjectNotificationEventListener.onProjectJoinRequestApproved - Notification processing completed: projectId={}, requesterId={}, adminId={}",
                                        project.getId(), user.getId(), admin.getId());

                } catch (Exception e) {
                        log.error("ProjectNotificationEventListener.onProjectJoinRequestApproved - Failed to send join request approval notification: projectId={}, requesterId={}, adminId={}, error={}, stackTrace={}",
                                        project.getId(), user.getId(), admin.getId(), e.getMessage(), e);
                        System.err.println("Failed to send join request approval notification:");
                        System.err.println("  Project ID: " + project.getId());
                        System.err.println("  Requester ID: " + user.getId());
                        System.err.println("  Admin ID: " + admin.getId());
                        System.err.println("  Error: " + e.getMessage());
                        e.printStackTrace();
                }
        }

        /**
         * Create common template variables for project notifications.
         */
        private Map<String, Object> createProjectTemplateVariables(Project project) {
                Map<String, Object> variables = new HashMap<>();

                // Project information
                variables.put("projectName", project.getName());
                variables.put("projectSlug", project.getProjectSlug());
                variables.put("projectDescription", project.getDescription());

                // URLs (these would be configured based on frontend routes)
                variables.put("projectUrl", "/projects/" + project.getProjectSlug());
                variables.put("unsubscribeUrl", "/notification-preferences");

                return variables;
        }

        /**
         * Create notification data JSON for project events.
         */
        private String createNotificationData(String eventType, Project project, User recipient, User actor) {
                return String.format("""
                                {
                                    "eventType": "%s",
                                    "projectId": "%s",
                                    "projectSlug": "%s",
                                    "recipientId": "%s",
                                    "actorId": "%s",
                                    "timestamp": "%s"
                                }
                                """,
                                eventType,
                                project.getId(),
                                project.getProjectSlug(),
                                recipient.getId(),
                                actor != null ? actor.getId() : "system",
                                java.time.LocalDateTime.now().toString());
        }
}
