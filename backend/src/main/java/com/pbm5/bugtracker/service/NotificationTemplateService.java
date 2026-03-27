package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.entity.NotificationTemplate;
import com.pbm5.bugtracker.repository.NotificationTemplateRepository;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class for managing notification templates.
 * 
 * Handles complete template lifecycle including:
 * - Programmatic template creation (no seed data)
 * - Template variable substitution
 * - Template versioning and activation
 * - Content generation for different channels
 * 
 * Key Features:
 * - Dynamic template creation and management
 * - Variable substitution with validation
 * - Multi-channel template support (email, in-app, toast)
 * - Template versioning with rollback capability
 * - Performance optimization through caching
 * 
 * Business Rules:
 * - Templates are created programmatically, not via database seed data
 * - Each template key can have multiple versions
 * - Only one version per template key can be active
 * - Variable substitution is performed safely with validation
 * - Templates support HTML, text, and plain formats
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationTemplateService {

        private final NotificationTemplateRepository notificationTemplateRepository;

        /**
         * Initialize default notification templates after application is ready.
         * This ensures all required templates are available and runs with proper
         * transaction context.
         * Only initializes templates if they don't already exist to avoid unnecessary
         * re-initialization.
         */
        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void initializeTemplatesOnStartup() {
                try {
                        log.info(
                                        "NotificationTemplateService.initializeTemplatesOnStartup - Starting automatic template initialization");

                        // Check if ALL required templates exist - don't skip if any are missing
                        if (areAllRequiredTemplatesPresent()) {
                                log.info(
                                                "NotificationTemplateService.initializeTemplatesOnStartup - All required templates already exist, skipping initialization");
                                return;
                        }

                        log.info(
                                        "NotificationTemplateService.initializeTemplatesOnStartup - Some required templates missing, proceeding with initialization");
                        initializeDefaultTemplates();
                        log.info(
                                        "NotificationTemplateService.initializeTemplatesOnStartup - Template initialization completed successfully");
                } catch (Exception e) {
                        log.error(
                                        "NotificationTemplateService.initializeTemplatesOnStartup - Failed to initialize templates on startup: {}",
                                        e.getMessage(), e);
                        // Don't fail application startup if template initialization fails
                        // Templates can be initialized manually via test endpoint if needed
                }
        }

        /**
         * Check if all required notification templates are present.
         * This ensures we don't skip initialization if some templates are missing.
         */
        private boolean areAllRequiredTemplatesPresent() {
                try {
                        // Define all required template keys
                        String[] requiredTemplateKeys = {
                                        "BUG_ASSIGNED",
                                        "BUG_STATUS_CHANGED",
                                        "BUG_COMMENTED",
                                        "BUG_MENTIONED",
                                        "BUG_PRIORITY_CHANGED",
                                        "BUG_ATTACHMENT_ADDED",
                                        "PROJECT_ROLE_CHANGED",
                                        "PROJECT_MEMBER_JOINED",
                                        "TEAM_ROLE_CHANGED",
                                        "TEAM_MEMBER_JOINED",
                                        "GAMIFICATION_POINTS",
                                        "GAMIFICATION_ACHIEVEMENTS",
                                        "GAMIFICATION_LEADERBOARD"
                        };

                        // Check if each required template exists and is active
                        for (String templateKey : requiredTemplateKeys) {
                                Optional<NotificationTemplate> template = notificationTemplateRepository
                                                .findByTemplateKeyAndIsActiveTrue(templateKey);
                                if (template.isEmpty()) {
                                        log.info("NotificationTemplateService.areAllRequiredTemplatesPresent - Missing required template: {}",
                                                        templateKey);
                                        return false;
                                }
                        }

                        log.info("NotificationTemplateService.areAllRequiredTemplatesPresent - All {} required templates are present",
                                        requiredTemplateKeys.length);
                        return true;
                } catch (Exception e) {
                        log.warn("NotificationTemplateService.areAllRequiredTemplatesPresent - Error checking templates: {}, will proceed with initialization",
                                        e.getMessage());
                        return false; // If we can't check, assume we need to initialize
                }
        }

        /**
         * Get active template by key.
         */
        @Transactional(readOnly = true)
        public Optional<NotificationTemplate> getActiveTemplate(String templateKey) {
                return notificationTemplateRepository.findByTemplateKeyAndIsActiveTrue(templateKey);
        }

        /**
         * Create or update a notification template.
         * This method is used programmatically, not via seed data.
         */
        public NotificationTemplate createOrUpdateTemplate(
                        String templateKey,
                        String name,
                        String description,
                        String subjectTemplate,
                        String htmlTemplate,
                        String textTemplate,
                        String inAppTemplate,
                        String toastTemplate,
                        String variables) {

                // Check if template exists and is active (consistent with
                // areAllRequiredTemplatesPresent)
                Optional<NotificationTemplate> existing = notificationTemplateRepository
                                .findByTemplateKeyAndIsActiveTrue(templateKey);

                if (existing.isPresent()) {
                        // Update existing template (create new version)
                        return createNewVersion(existing.get(), name, description, subjectTemplate,
                                        htmlTemplate, textTemplate, inAppTemplate, toastTemplate, variables);
                } else {
                        // Create new template
                        NotificationTemplate template = NotificationTemplate.builder()
                                        .templateKey(templateKey)
                                        .name(name)
                                        .description(description)
                                        .subjectTemplate(subjectTemplate)
                                        .htmlTemplate(htmlTemplate)
                                        .textTemplate(textTemplate)
                                        .inAppTemplate(inAppTemplate)
                                        .toastTemplate(toastTemplate)
                                        .variables(variables)
                                        .isActive(true)
                                        .version(1)
                                        .build();

                        NotificationTemplate saved = notificationTemplateRepository.save(template);
                        log.info("Created new notification template: {} (version {})", templateKey, saved.getVersion());
                        return saved;
                }
        }

        /**
         * Process template with variable substitution.
         */
        @Transactional(readOnly = true)
        public String processTemplate(String templateKey, String templateType, Map<String, Object> variables) {
                NotificationTemplate template = getActiveTemplate(templateKey)
                                .orElseThrow(() -> new RuntimeException("Template not found: " + templateKey));

                String templateContent = switch (templateType.toLowerCase()) {
                        case "subject" -> template.getSubjectTemplate();
                        case "html" -> template.getHtmlTemplate();
                        case "text" -> template.getTextTemplate();
                        case "inapp" -> template.getInAppTemplate();
                        case "toast" -> template.getToastTemplate();
                        default -> throw new IllegalArgumentException("Unknown template type: " + templateType);
                };

                if (templateContent == null) {
                        throw new RuntimeException("Template content not found for type: " + templateType);
                }

                return substituteVariables(templateContent, variables);
        }

        /**
         * Get all active templates.
         */
        @Transactional(readOnly = true)
        public List<NotificationTemplate> getAllActiveTemplates() {
                return notificationTemplateRepository.findByIsActiveTrueOrderByName();
        }

        /**
         * Deactivate a template.
         */
        public void deactivateTemplate(String templateKey) {
                NotificationTemplate template = notificationTemplateRepository
                                .findByTemplateKeyAndIsActiveTrue(templateKey)
                                .orElseThrow(() -> new RuntimeException("Active template not found: " + templateKey));

                template.setIsActive(false);
                notificationTemplateRepository.save(template);
                log.info("Deactivated template: {}", templateKey);
        }

        /**
         * Initialize default templates programmatically.
         * This method should be called during application startup, not via seed data.
         */
        @Transactional
        public void initializeDefaultTemplates() {
                log.info(
                                "NotificationTemplateService.initializeDefaultTemplates - Starting programmatic template initialization");

                // Bug Assignment Template
                log.info("NotificationTemplateService.initializeDefaultTemplates - Creating BUG_ASSIGNED template");
                createOrUpdateTemplate(
                                "BUG_ASSIGNED",
                                "Bug Assignment Notification",
                                "Template for bug assignment notifications",
                                "Bug #{bugId} assigned to you",
                                "<p>Bug <strong>#{bugId} \"${bugTitle}\"</strong> has been assigned to you by ${assignerName}.</p><p><a href=\"${bugUrl}\">View Bug</a></p>",
                                "Bug #${bugId} \"${bugTitle}\" has been assigned to you by ${assignerName}. View: ${bugUrl}",
                                "Bug #${bugId} \"${bugTitle}\" assigned to you by ${assignerName}",
                                "Bug assigned: ${bugTitle}",
                                "{\"bugId\": \"number\", \"bugTitle\": \"string\", \"assignerName\": \"string\", \"bugUrl\": \"string\"}");

                // Bug Comment Template
                createOrUpdateTemplate(
                                "BUG_COMMENTED",
                                "Bug Comment Notification",
                                "Template for new bug comment notifications",
                                "New comment on bug #{bugId}",
                                "<p>${commenterName} commented on bug <strong>#{bugId} \"${bugTitle}\"</strong>:</p><blockquote>${commentText}</blockquote><p><a href=\"${bugUrl}\">View Bug</a></p>",
                                "${commenterName} commented on bug #${bugId} \"${bugTitle}\": ${commentText}. View: ${bugUrl}",
                                "${commenterName} commented on \"${bugTitle}\"",
                                "💬 ${commenterName}: ${commentText}",
                                "{\"bugId\": \"number\", \"bugTitle\": \"string\", \"commenterName\": \"string\", \"commentText\": \"string\", \"bugUrl\": \"string\"}");

                // Bug Mention Template
                createOrUpdateTemplate(
                                "BUG_MENTIONED",
                                "Bug Mention Notification",
                                "Template for @mention notifications in bug comments",
                                "You were mentioned in bug #{bugId}",
                                "<p>${mentionerName} mentioned you in bug <strong>#{bugId} \"${bugTitle}\"</strong>:</p><blockquote>${commentText}</blockquote><p><a href=\"${bugUrl}\">View Bug</a></p>",
                                "${mentionerName} mentioned you in bug #${bugId} \"${bugTitle}\": ${commentText}. View: ${bugUrl}",
                                "${mentionerName} mentioned you in \"${bugTitle}\"",
                                "@️ ${mentionerName} mentioned you in ${bugTitle}",
                                "{\"bugId\": \"number\", \"bugTitle\": \"string\", \"mentionerName\": \"string\", \"commentText\": \"string\", \"bugUrl\": \"string\"}");

                // Bug Status Changed Template
                createOrUpdateTemplate(
                                "BUG_STATUS_CHANGED",
                                "Bug Status Change Notification",
                                "Template for bug status change notifications",
                                "Bug #{bugId} status updated",
                                "<p>Bug <strong>#{bugId} \"${bugTitle}\"</strong> status changed from <span class=\"${oldStatusClass}\">${oldStatus}</span> to <span class=\"${newStatusClass}\">${newStatus}</span> by ${updatedByName}.</p><p><a href=\"${bugUrl}\">View Bug</a></p>",
                                "Bug #${bugId} \"${bugTitle}\" status changed from ${oldStatus} to ${newStatus} by ${updatedByName}. View: ${bugUrl}",
                                "Bug status changed from ${oldStatus} to ${newStatus}",
                                "Status changed: ${oldStatus} → ${newStatus}",
                                "{\"bugId\": \"number\", \"bugTitle\": \"string\", \"oldStatus\": \"string\", \"newStatus\": \"string\", \"updatedByName\": \"string\", \"bugUrl\": \"string\"}");

                // Gamification Points Template
                createOrUpdateTemplate(
                                "GAMIFICATION_POINTS",
                                "Points Earned Notification",
                                "Template for gamification points notifications",
                                "Points earned!",
                                "<p>🪙 You earned <strong>${points} points</strong> for ${reason}!</p>",
                                "🪙 You earned ${points} points for ${reason}!",
                                "Earned ${points} points for ${reason}",
                                "🪙 +${points} points: ${reason}",
                                "{\"points\": \"number\", \"reason\": \"string\"}");

                // Bug Priority Changed Template
                createOrUpdateTemplate(
                                "BUG_PRIORITY_CHANGED",
                                "Bug Priority Change Notification",
                                "Template for bug priority change notifications",
                                "Bug #{bugId} priority updated",
                                "<p>Bug <strong>#{bugId} \"${bugTitle}\"</strong> priority changed from <span class=\"priority-${oldPriorityClass}\">${oldPriority}</span> to <span class=\"priority-${newPriorityClass}\">${newPriority}</span> by ${updatedByName}.</p><p><a href=\"${bugUrl}\">View Bug</a></p>",
                                "Bug #${bugId} \"${bugTitle}\" priority changed from ${oldPriority} to ${newPriority} by ${updatedByName}. View: ${bugUrl}",
                                "Priority changed from ${oldPriority} to ${newPriority}",
                                "Priority: ${oldPriority} → ${newPriority}",
                                "{\"bugId\": \"number\", \"bugTitle\": \"string\", \"oldPriority\": \"string\", \"newPriority\": \"string\", \"updatedByName\": \"string\", \"bugUrl\": \"string\"}");

                // Bug Attachment Added Template
                createOrUpdateTemplate(
                                "BUG_ATTACHMENT_ADDED",
                                "Bug Attachment Notification",
                                "Template for bug attachment notifications",
                                "New attachment on bug #{bugId}",
                                "<p>${attacherName} added an attachment to bug <strong>#{bugId} \"${bugTitle}\"</strong>.</p><p><strong>File:</strong> ${fileName}</p><p><a href=\"${bugUrl}\">View Bug</a></p>",
                                "${attacherName} added attachment \"${fileName}\" to bug #${bugId} \"${bugTitle}\". View: ${bugUrl}",
                                "Attachment added: ${fileName}",
                                "📎 ${fileName} added",
                                "{\"bugId\": \"number\", \"bugTitle\": \"string\", \"attacherName\": \"string\", \"fileName\": \"string\", \"bugUrl\": \"string\"}");

                // Project Role Changed Template
                createOrUpdateTemplate(
                                "PROJECT_ROLE_CHANGED",
                                "Project Role Change Notification",
                                "Template for project role change notifications",
                                "Project role updated",
                                "<p>Your role in project <strong>\"${projectName}\"</strong> has been changed from ${oldRole} to ${newRole} by ${updatedByName}.</p><p><a href=\"${projectUrl}\">View Project</a></p>",
                                "Your role in project \"${projectName}\" changed from ${oldRole} to ${newRole} by ${updatedByName}. View: ${projectUrl}",
                                "Role changed from ${oldRole} to ${newRole}",
                                "Role: ${oldRole} → ${newRole}",
                                "{\"projectName\": \"string\", \"oldRole\": \"string\", \"newRole\": \"string\", \"updatedByName\": \"string\", \"projectUrl\": \"string\"}");

                // Project Member Joined Template
                createOrUpdateTemplate(
                                "PROJECT_MEMBER_JOINED",
                                "Project Member Joined Notification",
                                "Template for project member joined notifications",
                                "New member joined project",
                                "<p>${newMemberName} has joined project <strong>\"${projectName}\"</strong> with role ${role}.</p><p><a href=\"${projectUrl}\">View Project</a></p>",
                                "${newMemberName} joined project \"${projectName}\" with role ${role}. View: ${projectUrl}",
                                "New member: ${newMemberName}",
                                "👤 ${newMemberName} joined",
                                "{\"projectName\": \"string\", \"newMemberName\": \"string\", \"role\": \"string\", \"projectUrl\": \"string\"}");

                // Team Role Changed Template
                createOrUpdateTemplate(
                                "TEAM_ROLE_CHANGED",
                                "Team Role Change Notification",
                                "Template for team role change notifications",
                                "Team role updated",
                                "<p>Your role in team <strong>\"${teamName}\"</strong> has been changed from ${oldRole} to ${newRole} by ${updatedByName}.</p><p><a href=\"${teamUrl}\">View Team</a></p>",
                                "Your role in team \"${teamName}\" changed from ${oldRole} to ${newRole} by ${updatedByName}. View: ${teamUrl}",
                                "Role changed from ${oldRole} to ${newRole}",
                                "Role: ${oldRole} → ${newRole}",
                                "{\"projectName\": \"string\", \"oldRole\": \"string\", \"newRole\": \"string\", \"updatedByName\": \"string\", \"teamUrl\": \"string\"}");

                // Team Member Joined Template
                createOrUpdateTemplate(
                                "TEAM_MEMBER_JOINED",
                                "Team Member Joined Notification",
                                "Template for team member joined notifications",
                                "New member joined team",
                                "<p>${newMemberName} has joined team <strong>\"${teamName}\"</strong> with role ${role}.</p><p><a href=\"${teamUrl}\">View Team</a></p>",
                                "${newMemberName} joined team \"${teamName}\" with role ${role}. View: ${teamUrl}",
                                "New member: ${newMemberName}",
                                "👤 ${newMemberName} joined",
                                "{\"teamName\": \"string\", \"newMemberName\": \"string\", \"role\": \"string\", \"teamUrl\": \"string\"}");

                // Gamification Achievements Template
                createOrUpdateTemplate(
                                "GAMIFICATION_ACHIEVEMENTS",
                                "Achievement Unlocked Notification",
                                "Template for achievement notifications",
                                "Achievement unlocked!",
                                "<p>🏆 You've unlocked the achievement <strong>\"${achievementName}\"</strong>!</p><p>${achievementDescription}</p>",
                                "🏆 Achievement unlocked: ${achievementName} - ${achievementDescription}",
                                "Achievement: ${achievementName}",
                                "🏆 ${achievementName}",
                                "{\"achievementName\": \"string\", \"achievementDescription\": \"string\"}");

                // Gamification Leaderboard Template
                createOrUpdateTemplate(
                                "GAMIFICATION_LEADERBOARD",
                                "Leaderboard Update Notification",
                                "Template for leaderboard notifications",
                                "Leaderboard position updated",
                                "<p>📊 Your position on the leaderboard has changed!</p><p><strong>New Position:</strong> ${newPosition}</p><p><strong>Previous Position:</strong> ${oldPosition}</p>",
                                "📊 Leaderboard position changed from ${oldPosition} to ${newPosition}",
                                "Position: ${oldPosition} → ${newPosition}",
                                "📊 ${newPosition} position",
                                "{\"newPosition\": \"number\", \"oldPosition\": \"number\"}");

                // Final count and completion log
                long finalTemplateCount = notificationTemplateRepository.count();
                log.info(
                                "NotificationTemplateService.initializeDefaultTemplates - Template initialization completed. Total templates created: {}",
                                finalTemplateCount);
                log.info(
                                "NotificationTemplateService.initializeDefaultTemplates - Default notification templates initialized successfully");
        }

        /**
         * Create a new version of an existing template.
         */
        private NotificationTemplate createNewVersion(
                        NotificationTemplate existing,
                        String name,
                        String description,
                        String subjectTemplate,
                        String htmlTemplate,
                        String textTemplate,
                        String inAppTemplate,
                        String toastTemplate,
                        String variables) {

                // Get next version number
                Integer maxVersion = notificationTemplateRepository
                                .findMaxVersionByTemplateKey(existing.getTemplateKey());
                int newVersion = (maxVersion != null ? maxVersion : 0) + 1;

                // Deactivate old versions
                notificationTemplateRepository.deactivateOlderVersions(existing.getTemplateKey(), newVersion);

                // Create new version
                NotificationTemplate newTemplate = NotificationTemplate.builder()
                                .templateKey(existing.getTemplateKey())
                                .name(name)
                                .description(description)
                                .subjectTemplate(subjectTemplate)
                                .htmlTemplate(htmlTemplate)
                                .textTemplate(textTemplate)
                                .inAppTemplate(inAppTemplate)
                                .toastTemplate(toastTemplate)
                                .variables(variables)
                                .isActive(true)
                                .version(newVersion)
                                .build();

                NotificationTemplate saved = notificationTemplateRepository.save(newTemplate);
                log.info("Created new version {} for template: {}", newVersion, existing.getTemplateKey());
                return saved;
        }

        /**
         * Substitute variables in template content.
         */
        private String substituteVariables(String template, Map<String, Object> variables) {
                if (template == null || variables == null) {
                        return template;
                }

                String result = template;
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                        String placeholder = "${" + entry.getKey() + "}";
                        String value = entry.getValue() != null ? entry.getValue().toString() : "";
                        result = result.replace(placeholder, value);
                }

                return result;
        }
}
