package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.pbm5.bugtracker.entity.*;
import com.pbm5.bugtracker.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Event listener service for bug-related notifications.
 * 
 * Handles notification creation and delivery for bug events including:
 * - Bug assignment/unassignment
 * - Bug status changes
 * - Bug priority changes
 * - New bug creation
 * - Bug comments and mentions
 * 
 * Integration Points:
 * - Called by BugService after successful operations
 * - Processes template variables for personalized notifications
 * - Coordinates with NotificationService for delivery
 * 
 * Business Rules:
 * - Only notify users who have opted in for specific event types
 * - Template variables include all relevant bug and user information
 * - Notifications are sent asynchronously to avoid blocking bug operations
 * 
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BugNotificationEventListener {

        private final NotificationService notificationService;
        private final UserRepository userRepository;

        /**
         * Handle bug assignment notification.
         * 
         * Notifies the assigned user when a bug is assigned to them.
         * 
         * @param bug        The assigned bug
         * @param assignee   The user who was assigned the bug
         * @param assignedBy The user who performed the assignment (optional)
         */
        @Async
        public void onBugAssigned(Bug bug, User assignee, User assignedBy) {
                try {
                        log.info("BugNotificationEventListener.onBugAssigned - Starting notification processing: bugId={}, bugTitle='{}', assigneeId={}, assigneeEmail={}, assignedByEmail={}",
                                        bug.getId(), bug.getTitle(), assignee.getId(), assignee.getEmail(),
                                        assignedBy != null ? assignedBy.getEmail() : "system");

                        // Prepare template variables
                        Map<String, Object> templateVariables = createBugTemplateVariables(bug);
                        templateVariables.put("assigneeName", assignee.getFirstName() + " " + assignee.getLastName());
                        templateVariables.put("assigneeEmail", assignee.getEmail());

                        if (assignedBy != null) {
                                templateVariables.put("assignedByName",
                                                assignedBy.getFirstName() + " " + assignedBy.getLastName());
                                templateVariables.put("assignedByEmail", assignedBy.getEmail());
                                log.info("BugNotificationEventListener.onBugAssigned - Assignment by user: assignedBy={}({})",
                                                assignedBy.getEmail(), assignedBy.getId());
                        } else {
                                templateVariables.put("assignedByName", "System");
                                templateVariables.put("assignedByEmail", "system@bugtracker.com");
                                log.info("BugNotificationEventListener.onBugAssigned - Assignment by system");
                        }

                        // Create and deliver notification
                        String title = String.format("Bug #%s assigned to you", bug.getProjectTicketNumber());
                        String message = String.format("You have been assigned bug '%s' in project '%s'",
                                        bug.getTitle(), bug.getProject().getName());

                        log.info("BugNotificationEventListener.onBugAssigned - Calling notification service: title='{}', message='{}'",
                                        title, message);

                        notificationService.createAndDeliverNotification(
                                        assignee.getId(),
                                        "BUG_ASSIGNED",
                                        title,
                                        message,
                                        createNotificationData("bug_assignment", bug, assignee, assignedBy),
                                        templateVariables,
                                        bug,
                                        bug.getProject(),
                                        null, // No team context for bug assignment
                                        assignedBy);

                        log.info("BugNotificationEventListener.onBugAssigned - Notification processing completed: bugId={}, assigneeEmail={}, assignedByEmail={}",
                                        bug.getId(), assignee.getEmail(),
                                        assignedBy != null ? assignedBy.getEmail() : "system");

                } catch (Exception e) {
                        log.error("BugNotificationEventListener.onBugAssigned - Failed to send bug assignment notification: bugId={}, assigneeEmail={}, assignedByEmail={}, error={}",
                                        bug.getId(), assignee.getEmail(),
                                        assignedBy != null ? assignedBy.getEmail() : "system", e.getMessage(), e);
                }
        }

        /**
         * Handle bug status change notification.
         * 
         * Notifies relevant users when a bug's status changes.
         * 
         * @param bug       The bug whose status changed
         * @param oldStatus The previous status
         * @param newStatus The new status
         * @param changedBy The user who changed the status
         */
        @Async
        public void onBugStatusChanged(Bug bug, BugStatus oldStatus, BugStatus newStatus, User changedBy) {
                try {
                        log.debug("Processing bug status change notification: bug={}, oldStatus={}, newStatus={}, changedBy={}",
                                        bug.getId(), oldStatus, newStatus, changedBy.getId());

                        // Prepare template variables
                        Map<String, Object> templateVariables = createBugTemplateVariables(bug);
                        templateVariables.put("oldStatus", oldStatus.toString());
                        templateVariables.put("newStatus", newStatus.toString());
                        templateVariables.put("oldStatusClass", oldStatus.name().toLowerCase().replace("_", "-"));
                        templateVariables.put("newStatusClass", newStatus.name().toLowerCase().replace("_", "-"));
                        templateVariables.put("updatedByName",
                                        changedBy.getFirstName() + " " + changedBy.getLastName());
                        templateVariables.put("updatedByEmail", changedBy.getEmail());

                        // Create notification title and message
                        String title = String.format("Bug #%s status updated", bug.getProjectTicketNumber());
                        String message = String.format("Bug '%s' status changed from %s to %s",
                                        bug.getTitle(), oldStatus, newStatus);

                        // Notify assignee if different from the person who made the change
                        if (bug.getAssignee() != null && !bug.getAssignee().getId().equals(changedBy.getId())) {
                                templateVariables.put("recipientName",
                                                bug.getAssignee().getFirstName() + " "
                                                                + bug.getAssignee().getLastName());

                                notificationService.createAndDeliverNotification(
                                                bug.getAssignee().getId(),
                                                "BUG_STATUS_CHANGED",
                                                title,
                                                message,
                                                createNotificationData("bug_status_change", bug, bug.getAssignee(),
                                                                changedBy),
                                                templateVariables,
                                                bug,
                                                bug.getProject(),
                                                null,
                                                changedBy);
                        }

                        // Notify reporter if different from assignee and the person who made the change
                        if (bug.getReporter() != null &&
                                        !bug.getReporter().getId().equals(changedBy.getId()) &&
                                        (bug.getAssignee() == null || !bug.getReporter().getId()
                                                        .equals(bug.getAssignee().getId()))) {

                                templateVariables.put("recipientName",
                                                bug.getReporter().getFirstName() + " "
                                                                + bug.getReporter().getLastName());

                                notificationService.createAndDeliverNotification(
                                                bug.getReporter().getId(),
                                                "BUG_STATUS_CHANGED",
                                                title,
                                                message,
                                                createNotificationData("bug_status_change", bug, bug.getReporter(),
                                                                changedBy),
                                                templateVariables,
                                                bug,
                                                bug.getProject(),
                                                null,
                                                changedBy);
                        }

                        log.info("Bug status change notifications sent: bug={}, oldStatus={}, newStatus={}",
                                        bug.getId(), oldStatus, newStatus);

                } catch (Exception e) {
                        log.error("Failed to send bug status change notification: bug={}, error={}",
                                        bug.getId(), e.getMessage(), e);
                }
        }

        /**
         * Handle bug comment notification.
         * 
         * Notifies relevant users when a comment is added to a bug.
         * 
         * @param bug       The bug that was commented on
         * @param comment   The comment that was added
         * @param commenter The user who added the comment
         */
        @Async
        public void onBugCommented(Bug bug, BugComment comment, User commenter) {
                try {
                        log.debug("Processing bug comment notification: bug={}, comment={}, commenter={}",
                                        bug.getId(), comment.getId(), commenter.getId());

                        // Prepare template variables
                        Map<String, Object> templateVariables = createBugTemplateVariables(bug);
                        templateVariables.put("commenterName",
                                        commenter.getFirstName() + " " + commenter.getLastName());
                        templateVariables.put("commenterEmail", commenter.getEmail());
                        templateVariables.put("commentText", comment.getContent()); // Fixed: template expects
                                                                                    // 'commentText'
                        templateVariables.put("commentContent", comment.getContent()); // Keep for backward
                                                                                       // compatibility
                        templateVariables.put("commentTime", comment.getCreatedAt().toString());

                        // Create notification title and message
                        String title = String.format("New comment on bug #%s", bug.getProjectTicketNumber());
                        String message = String.format("%s commented on bug '%s'",
                                        commenter.getFirstName() + " " + commenter.getLastName(), bug.getTitle());

                        // Notify assignee if different from commenter
                        if (bug.getAssignee() != null && !bug.getAssignee().getId().equals(commenter.getId())) {
                                log.debug("Sending comment notification to assignee: assigneeId={}, commenterId={}",
                                                bug.getAssignee().getId(), commenter.getId());

                                templateVariables.put("recipientName",
                                                bug.getAssignee().getFirstName() + " "
                                                                + bug.getAssignee().getLastName());

                                notificationService.createAndDeliverNotification(
                                                bug.getAssignee().getId(),
                                                "BUG_COMMENTED",
                                                title,
                                                message,
                                                createNotificationData("bug_comment", bug, bug.getAssignee(),
                                                                commenter),
                                                templateVariables,
                                                bug,
                                                bug.getProject(),
                                                null,
                                                commenter);

                                log.debug("Comment notification sent to assignee successfully");
                        } else {
                                log.debug("Skipping assignee notification: assignee={}, commenter={}",
                                                bug.getAssignee() != null ? bug.getAssignee().getId() : "null",
                                                commenter.getId());
                        }

                        // Notify reporter if different from assignee and commenter
                        if (bug.getReporter() != null &&
                                        !bug.getReporter().getId().equals(commenter.getId()) &&
                                        (bug.getAssignee() == null || !bug.getReporter().getId()
                                                        .equals(bug.getAssignee().getId()))) {

                                log.debug("Sending comment notification to reporter: reporterId={}, commenterId={}, assigneeId={}",
                                                bug.getReporter().getId(), commenter.getId(),
                                                bug.getAssignee() != null ? bug.getAssignee().getId() : "null");

                                templateVariables.put("recipientName",
                                                bug.getReporter().getFirstName() + " "
                                                                + bug.getReporter().getLastName());

                                notificationService.createAndDeliverNotification(
                                                bug.getReporter().getId(),
                                                "BUG_COMMENTED",
                                                title,
                                                message,
                                                createNotificationData("bug_comment", bug, bug.getReporter(),
                                                                commenter),
                                                templateVariables,
                                                bug,
                                                bug.getProject(),
                                                null,
                                                commenter);

                                log.debug("Comment notification sent to reporter successfully");
                        } else {
                                log.debug("Skipping reporter notification: reporter={}, commenter={}, assignee={}",
                                                bug.getReporter() != null ? bug.getReporter().getId() : "null",
                                                commenter.getId(),
                                                bug.getAssignee() != null ? bug.getAssignee().getId() : "null");
                        }

                        log.info("Bug comment notifications sent: bug={}, comment={}", bug.getId(), comment.getId());

                        // Now handle mentions in the same async context
                        handleMentionsInComment(bug, comment, commenter);

                } catch (Exception e) {
                        log.error("Failed to send bug comment notification: bug={}, comment={}, error={}, stackTrace={}",
                                        bug.getId(), comment.getId(), e.getMessage(), e);

                        // Also log to console for immediate visibility during testing
                        System.err.println("Failed to send bug comment notification:");
                        System.err.println("  Bug ID: " + bug.getId());
                        System.err.println("  Comment ID: " + comment.getId());
                        System.err.println("  Error: " + e.getMessage());
                        e.printStackTrace();
                }
        }

        /**
         * Handle bug priority change notification.
         * 
         * Notifies relevant users when a bug's priority changes.
         * 
         * @param bug         The bug whose priority changed
         * @param oldPriority The previous priority
         * @param newPriority The new priority
         * @param changedBy   The user who changed the priority
         */
        @Async
        public void onBugPriorityChanged(Bug bug, BugPriority oldPriority, BugPriority newPriority, User changedBy) {
                try {
                        log.info("BugNotificationEventListener.onBugPriorityChanged - Starting notification processing: bugId={}, oldPriority={}, newPriority={}, changedBy={}({})",
                                        bug.getId(), oldPriority, newPriority, changedBy.getId(), changedBy.getEmail());

                        log.debug("Processing bug priority change notification: bug={}, oldPriority={}, newPriority={}, changedBy={}",
                                        bug.getId(), oldPriority, newPriority, changedBy.getId());

                        // Prepare template variables
                        Map<String, Object> templateVariables = createBugTemplateVariables(bug);
                        templateVariables.put("oldPriority", oldPriority.toString());
                        templateVariables.put("newPriority", newPriority.toString());
                        templateVariables.put("oldPriorityClass", oldPriority.name().toLowerCase().replace("_", "-"));
                        templateVariables.put("newPriorityClass", newPriority.name().toLowerCase().replace("_", "-"));
                        templateVariables.put("updatedByName",
                                        changedBy.getFirstName() + " " + changedBy.getLastName());
                        templateVariables.put("updatedByEmail", changedBy.getEmail());

                        // Create notification title and message
                        String title = String.format("Bug #%s priority updated", bug.getProjectTicketNumber());
                        String message = String.format("Bug '%s' priority changed from %s to %s",
                                        bug.getTitle(), oldPriority, newPriority);

                        // Notify assignee if different from the person who made the change
                        if (bug.getAssignee() != null && !bug.getAssignee().getId().equals(changedBy.getId())) {
                                templateVariables.put("recipientName",
                                                bug.getAssignee().getFirstName() + " "
                                                                + bug.getAssignee().getLastName());

                                notificationService.createAndDeliverNotification(
                                                bug.getAssignee().getId(),
                                                "BUG_PRIORITY_CHANGED",
                                                title,
                                                message,
                                                createNotificationData("bug_priority_change", bug, bug.getAssignee(),
                                                                changedBy),
                                                templateVariables,
                                                bug,
                                                bug.getProject(),
                                                null,
                                                changedBy);
                        }

                        // Notify reporter if different from assignee and the person who made the change
                        if (bug.getReporter() != null &&
                                        !bug.getReporter().getId().equals(changedBy.getId()) &&
                                        (bug.getAssignee() == null || !bug.getReporter().getId()
                                                        .equals(bug.getAssignee().getId()))) {

                                templateVariables.put("recipientName",
                                                bug.getReporter().getFirstName() + " "
                                                                + bug.getReporter().getLastName());

                                notificationService.createAndDeliverNotification(
                                                bug.getReporter().getId(),
                                                "BUG_PRIORITY_CHANGED",
                                                title,
                                                message,
                                                createNotificationData("bug_priority_change", bug, bug.getReporter(),
                                                                changedBy),
                                                templateVariables,
                                                bug,
                                                bug.getProject(),
                                                null,
                                                changedBy);
                        }

                        log.info("Bug priority change notifications sent: bug={}, oldPriority={}, newPriority={}",
                                        bug.getId(), oldPriority, newPriority);

                } catch (Exception e) {
                        log.error("Failed to send bug priority change notification: bug={}, error={}",
                                        bug.getId(), e.getMessage(), e);
                }
        }

        /**
         * Handle bug attachment notification.
         * 
         * Notifies relevant users when a file is attached to a bug.
         * 
         * @param bug        The bug that received the attachment
         * @param attachment The attachment that was added
         * @param attachedBy The user who added the attachment
         */
        @Async
        public void onBugAttachmentAdded(Bug bug, BugAttachment attachment, User attachedBy) {
                try {
                        log.info("BugNotificationEventListener.onBugAttachmentAdded - Starting notification processing: bugId={}, attachmentId={}, attachedBy={}({})",
                                        bug.getId(), attachment.getId(), attachedBy.getId(), attachedBy.getEmail());

                        log.debug("Processing bug attachment notification: bug={}, attachment={}, attachedBy={}",
                                        bug.getId(), attachment.getId(), attachedBy.getId());

                        // Debug logging for assignee and reporter
                        log.debug("Bug attachment notification - Bug assignee: {}, Bug reporter: {}, Attached by: {}",
                                        bug.getAssignee() != null
                                                        ? bug.getAssignee().getId() + "(" + bug.getAssignee().getEmail()
                                                                        + ")"
                                                        : "null",
                                        bug.getReporter() != null
                                                        ? bug.getReporter().getId() + "(" + bug.getReporter().getEmail()
                                                                        + ")"
                                                        : "null",
                                        attachedBy.getId() + "(" + attachedBy.getEmail() + ")");

                        // Prepare template variables
                        Map<String, Object> templateVariables = createBugTemplateVariables(bug);
                        templateVariables.put("attacherName",
                                        attachedBy.getFirstName() + " " + attachedBy.getLastName());
                        templateVariables.put("attacherEmail", attachedBy.getEmail());
                        templateVariables.put("fileName", attachment.getOriginalFilename());
                        templateVariables.put("fileSize", attachment.getFileSize());
                        templateVariables.put("mimeType", attachment.getMimeType());
                        templateVariables.put("attachmentTime", attachment.getCreatedAt().toString());

                        // Create notification title and message
                        String title = String.format("New attachment on bug #%s", bug.getProjectTicketNumber());
                        String message = String.format("%s added attachment \"%s\" to bug '%s'",
                                        attachedBy.getFirstName() + " " + attachedBy.getLastName(),
                                        attachment.getOriginalFilename(), bug.getTitle());

                        // Notify assignee if different from the person who added the attachment
                        if (bug.getAssignee() != null && !bug.getAssignee().getId().equals(attachedBy.getId())) {
                                log.debug("Sending attachment notification to assignee: assigneeId={}, attacherId={}",
                                                bug.getAssignee().getId(), attachedBy.getId());

                                templateVariables.put("recipientName",
                                                bug.getAssignee().getFirstName() + " "
                                                                + bug.getAssignee().getLastName());

                                notificationService.createAndDeliverNotification(
                                                bug.getAssignee().getId(),
                                                "BUG_ATTACHMENT_ADDED",
                                                title,
                                                message,
                                                createNotificationData("bug_attachment", bug, bug.getAssignee(),
                                                                attachedBy),
                                                templateVariables,
                                                bug,
                                                bug.getProject(),
                                                null,
                                                attachedBy);

                                log.debug("Attachment notification sent to assignee successfully");
                        } else {
                                log.debug("Skipping assignee notification: assignee={}, attacher={}",
                                                bug.getAssignee() != null ? bug.getAssignee().getId() : "null",
                                                attachedBy.getId());
                        }

                        // Notify reporter if different from assignee and the person who added the
                        // attachment
                        if (bug.getReporter() != null &&
                                        !bug.getReporter().getId().equals(attachedBy.getId()) &&
                                        (bug.getAssignee() == null || !bug.getReporter().getId()
                                                        .equals(bug.getAssignee().getId()))) {

                                log.debug("Sending attachment notification to reporter: reporterId={}, attacherId={}, assigneeId={}",
                                                bug.getReporter().getId(), attachedBy.getId(),
                                                bug.getAssignee() != null ? bug.getAssignee().getId() : "null");

                                templateVariables.put("recipientName",
                                                bug.getReporter().getFirstName() + " "
                                                                + bug.getReporter().getLastName());

                                notificationService.createAndDeliverNotification(
                                                bug.getReporter().getId(),
                                                "BUG_ATTACHMENT_ADDED",
                                                title,
                                                message,
                                                createNotificationData("bug_attachment", bug, bug.getReporter(),
                                                                attachedBy),
                                                templateVariables,
                                                bug,
                                                bug.getProject(),
                                                null,
                                                attachedBy);

                                log.debug("Attachment notification sent to reporter successfully");
                        } else {
                                log.debug("Skipping reporter notification: reporter={}, attacher={}, assignee={}",
                                                bug.getReporter() != null ? bug.getReporter().getId() : "null",
                                                attachedBy.getId(),
                                                bug.getAssignee() != null ? bug.getAssignee().getId() : "null");
                        }

                        log.info("Bug attachment notifications sent: bug={}, attachment={}", bug.getId(),
                                        attachment.getId());

                } catch (Exception e) {
                        log.error("Failed to send bug attachment notification: bug={}, attachment={}, error={}",
                                        bug.getId(), attachment.getId(), e.getMessage(), e);
                }
        }

        /**
         * Handle bug mention notification.
         * 
         * Notifies users when they are mentioned in bug comments.
         * 
         * @param bug           The bug where the mention occurred
         * @param comment       The comment containing the mention
         * @param mentioner     The user who mentioned someone
         * @param mentionedUser The user who was mentioned
         */
        @Async
        public void onBugMentioned(Bug bug, BugComment comment, User mentioner, User mentionedUser) {
                try {
                        log.info("BugNotificationEventListener.onBugMentioned - Starting notification processing: bugId={}, commentId={}, mentioner={}({}), mentionedUser={}({})",
                                        bug.getId(), comment.getId(), mentioner.getId(), mentioner.getEmail(),
                                        mentionedUser.getId(), mentionedUser.getEmail());

                        log.debug("Processing bug mention notification: bug={}, comment={}, mentioner={}, mentionedUser={}",
                                        bug.getId(), comment.getId(), mentioner.getId(), mentionedUser.getId());

                        // Add debug logging
                        System.out.println("=== BUG MENTION NOTIFICATION DEBUG ===");
                        System.out.println("Bug ID: " + bug.getId());
                        System.out.println("Bug Title: " + bug.getTitle());
                        System.out.println("Comment ID: " + comment.getId());
                        System.out.println("Comment Content: " + comment.getContent());
                        System.out.println("Mentioner: " + mentioner.getEmail() + " (ID: " + mentioner.getId() + ")");
                        System.out.println("Mentioned User: " + mentionedUser.getEmail() + " (ID: "
                                        + mentionedUser.getId() + ")");

                        // Prepare template variables
                        Map<String, Object> templateVariables = createBugTemplateVariables(bug);
                        templateVariables.put("mentionerName",
                                        mentioner.getFirstName() + " " + mentioner.getLastName());
                        templateVariables.put("mentionerEmail", mentioner.getEmail());
                        templateVariables.put("commentText", comment.getContent());
                        templateVariables.put("commentTime", comment.getCreatedAt().toString());

                        System.out.println("Template variables prepared: " + templateVariables);

                        // Create notification title and message
                        String title = String.format("You were mentioned in bug #%s", bug.getProjectTicketNumber());
                        String message = String.format("%s mentioned you in bug '%s'",
                                        mentioner.getFirstName() + " " + mentioner.getLastName(), bug.getTitle());

                        System.out.println("Notification title: " + title);
                        System.out.println("Notification message: " + message);

                        // Send notification to the mentioned user
                        System.out.println("Calling notificationService.createAndDeliverNotification...");
                        notificationService.createAndDeliverNotification(
                                        mentionedUser.getId(),
                                        "BUG_MENTIONED",
                                        title,
                                        message,
                                        createNotificationData("bug_mention", bug, mentionedUser, mentioner),
                                        templateVariables,
                                        bug,
                                        bug.getProject(),
                                        null,
                                        mentioner);
                        System.out.println("Notification service call completed successfully");

                        log.info("Bug mention notification sent: bug={}, comment={}, mentionedUser={}",
                                        bug.getId(), comment.getId(), mentionedUser.getId());

                } catch (Exception e) {
                        log.error("Failed to send bug mention notification: bug={}, comment={}, mentionedUser={}, error={}",
                                        bug.getId(), comment.getId(), mentionedUser.getId(), e.getMessage(), e);

                        // Add debug logging for errors
                        System.err.println("=== BUG MENTION NOTIFICATION ERROR ===");
                        System.err.println("Error: " + e.getMessage());
                        e.printStackTrace();
                        System.err.println("=== END ERROR DEBUG ===");
                }
        }

        /**
         * Handle mentions in a bug comment.
         * 
         * @param bug       The bug where the comment was added
         * @param comment   The comment containing potential mentions
         * @param commenter The user who wrote the comment
         */
        private void handleMentionsInComment(Bug bug, BugComment comment, User commenter) {
                try {
                        log.debug("Processing mentions in comment: bug={}, comment={}, commenter={}",
                                        bug.getId(), comment.getId(), commenter.getId());

                        // Mention detection regex pattern - handles various mention formats:
                        // @FirstName, @FirstName LastName, @user@example.com, @username
                        // Uses word boundaries to avoid capturing extra text
                        String mentionPattern = "@([a-zA-Z]+(?:\\s+[a-zA-Z]+)?)";

                        // Also detect email mentions separately
                        String emailPattern = "@([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})";

                        String content = comment.getContent();
                        if (content == null || content.trim().isEmpty()) {
                                log.debug("Comment content is empty, no mentions to process");
                                return;
                        }

                        log.debug("Analyzing comment content for mentions: '{}'", content);
                        log.debug("Using name regex pattern: {}", mentionPattern);
                        log.debug("Using email regex pattern: {}", emailPattern);

                        // Find name mentions
                        java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile(mentionPattern);
                        java.util.regex.Matcher nameMatcher = namePattern.matcher(content);

                        List<String> mentions = new java.util.ArrayList<>();
                        while (nameMatcher.find()) {
                                String mention = nameMatcher.group(1); // Extract the username without @
                                log.debug("Found name mention: '{}'", mention);
                                mentions.add(mention);
                        }

                        // Find email mentions
                        java.util.regex.Pattern emailRegexPattern = java.util.regex.Pattern.compile(emailPattern);
                        java.util.regex.Matcher emailMatcher = emailRegexPattern.matcher(content);

                        while (emailMatcher.find()) {
                                String mention = emailMatcher.group(1); // Extract the email without @
                                log.debug("Found email mention: '{}'", mention);
                                mentions.add(mention);
                        }

                        log.debug("Total mentions found: {}", mentions.size());
                        log.debug("Mentions list: {}", mentions);

                        if (!mentions.isEmpty()) {
                                List<User> mentionedUsers = findUsersByMentions(mentions);
                                log.debug("Found mentioned users: {}",
                                                mentionedUsers.stream().map(User::getEmail).toList());

                                for (User mentionedUser : mentionedUsers) {
                                        log.debug("Processing mention for user: {} (ID: {})",
                                                        mentionedUser.getEmail(), mentionedUser.getId());
                                        log.debug("Commenter ID: {}, Mentioned User ID: {}",
                                                        commenter.getId(), mentionedUser.getId());

                                        // Skip if mentioned user is the commenter
                                        if (!mentionedUser.getId().equals(commenter.getId())) {
                                                log.debug("Sending mention notification to: {}",
                                                                mentionedUser.getEmail());
                                                try {
                                                        onBugMentioned(bug, comment, commenter, mentionedUser);
                                                        log.debug("Mention notification sent successfully to: {}",
                                                                        mentionedUser.getEmail());
                                                } catch (Exception e) {
                                                        log.error("Failed to send mention notification to {}: {}",
                                                                        mentionedUser.getEmail(), e.getMessage(), e);
                                                }
                                        } else {
                                                log.debug("Skipping self-mention for: {}", mentionedUser.getEmail());
                                        }
                                }
                        } else {
                                log.debug("No mentions detected in comment");
                        }

                } catch (Exception e) {
                        log.error("Failed to process mentions in comment: bug={}, comment={}, error={}",
                                        bug.getId(), comment.getId(), e.getMessage(), e);
                }
        }

        /**
         * Find users by username mentions using smart search
         * 
         * @param usernames List of usernames to find
         * @return List of found users
         */
        private List<User> findUsersByMentions(List<String> usernames) {
                if (usernames.isEmpty()) {
                        return List.of();
                }

                log.debug("Looking up users for usernames: {}", usernames);

                List<User> mentionedUsers = new java.util.ArrayList<>();
                for (String username : usernames) {
                        log.debug("Looking up user with username: '{}'", username);

                        User foundUser = findUserByMention(username);
                        if (foundUser != null) {
                                mentionedUsers.add(foundUser);
                        }
                }

                log.debug("Total users found: {}", mentionedUsers.size());
                log.debug("Users found: {}", mentionedUsers.stream().map(User::getEmail).toList());

                return mentionedUsers;
        }

        /**
         * Smart user lookup that searches by first name, last name, and email
         * 
         * @param mention The mention text (e.g., "Alice", "Alice Newton",
         *                "alice@test.com")
         * @return The found user or null if not found
         */
        private User findUserByMention(String mention) {
                if (mention == null || mention.trim().isEmpty()) {
                        return null;
                }

                String cleanMention = mention.trim();
                log.debug("Searching for user with mention: '{}'", cleanMention);

                // Strategy 1: Try exact email match first (most reliable)
                if (cleanMention.contains("@")) {
                        log.debug("Mention contains @, trying exact email match");
                        var userOpt = userRepository.findByEmail(cleanMention);
                        if (userOpt.isPresent()) {
                                User user = userOpt.get();
                                log.debug("Found user by exact email: {} (ID: {})", user.getEmail(), user.getId());
                                return user;
                        }
                        log.debug("No user found by exact email: {}", cleanMention);
                }

                // Strategy 2: For name mentions, suggest using email format instead
                log.debug("Name-based mention detected: '{}'", cleanMention);
                log.info("MENTION USAGE TIP: For reliable mentions, use email format like @alice@test.com instead of @Alice Newton");

                // Try a simple first name exact match as fallback (case insensitive)
                log.debug("Trying simple first name match for: '{}'", cleanMention);
                try {
                        List<User> allUsers = userRepository.findAll();
                        for (User user : allUsers) {
                                if (user.getFirstName().equalsIgnoreCase(cleanMention)) {
                                        log.debug("Found user by exact first name match: {} (ID: {})", user.getEmail(),
                                                        user.getId());
                                        log.info("SUCCESS: Found user {} by first name '{}'. For better reliability, use @{} next time.",
                                                        user.getEmail(), cleanMention, user.getEmail());
                                        return user;
                                }
                        }

                        // Try full name match
                        for (User user : allUsers) {
                                String fullName = user.getFirstName() + " " + user.getLastName();
                                if (fullName.equalsIgnoreCase(cleanMention)) {
                                        log.debug("Found user by exact full name match: {} (ID: {})", user.getEmail(),
                                                        user.getId());
                                        log.info("SUCCESS: Found user {} by full name '{}'. For better reliability, use @{} next time.",
                                                        user.getEmail(), cleanMention, user.getEmail());
                                        return user;
                                }
                        }
                } catch (Exception e) {
                        log.debug("Error in simple name search: {}", e.getMessage());
                }

                log.debug("No user found for mention: '{}'", cleanMention);
                return null;
        }

        /**
         * Create common template variables for bug notifications.
         */
        private Map<String, Object> createBugTemplateVariables(Bug bug) {
                Map<String, Object> variables = new HashMap<>();

                // Bug information
                variables.put("bugId", bug.getProjectTicketNumber());
                variables.put("bugTitle", bug.getTitle());
                variables.put("bugDescription", bug.getDescription());
                variables.put("bugStatus", bug.getStatus().toString());
                variables.put("bugPriority", bug.getPriority().toString());
                variables.put("priorityClass", bug.getPriority().name().toLowerCase());

                // Project information
                variables.put("projectName", bug.getProject().getName());
                variables.put("projectSlug", bug.getProject().getProjectSlug());

                // URLs (these would be configured based on frontend routes)
                variables.put("bugUrl", "/projects/" + bug.getProject().getProjectSlug() + "/bugs/"
                                + bug.getProjectTicketNumber());
                variables.put("unsubscribeUrl", "/notification-preferences");

                // Reporter information
                if (bug.getReporter() != null) {
                        variables.put("reporterName",
                                        bug.getReporter().getFirstName() + " " + bug.getReporter().getLastName());
                        variables.put("reporterEmail", bug.getReporter().getEmail());
                }

                // Assignee information
                if (bug.getAssignee() != null) {
                        variables.put("assigneeName",
                                        bug.getAssignee().getFirstName() + " " + bug.getAssignee().getLastName());
                        variables.put("assigneeEmail", bug.getAssignee().getEmail());
                }

                return variables;
        }

        /**
         * Create notification data JSON for bug events.
         */
        private String createNotificationData(String eventType, Bug bug, User recipient, User actor) {
                return String.format("""
                                {
                                    "eventType": "%s",
                                    "bugId": %d,
                                    "projectId": "%s",
                                    "recipientId": "%s",
                                    "actorId": "%s",
                                    "timestamp": "%s"
                                }
                                """,
                                eventType,
                                bug.getId(),
                                bug.getProject().getId(),
                                recipient.getId(),
                                actor != null ? actor.getId() : "system",
                                java.time.LocalDateTime.now().toString());
        }
}
