package com.pbm5.bugtracker.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pbm5.bugtracker.entity.UserNotification;
import com.pbm5.bugtracker.service.NotificationService;
import com.pbm5.bugtracker.service.NotificationTemplateService;
import com.pbm5.bugtracker.service.ResendEmailNotificationService;

import java.util.Map;
import java.util.UUID;

/**
 * Test controller for notification system verification.
 * 
 * Provides simple endpoints to test:
 * - Notification creation and delivery
 * - Email service functionality
 * - Template processing
 * - Multi-channel delivery
 * 
 * These endpoints are for development and testing purposes only.
 * Should be disabled or secured in production environments.
 */
@RestController
@RequestMapping("/api/bugtracker/v1/test/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationTestController {

        private final NotificationService notificationService;
        private final NotificationTemplateService notificationTemplateService;
        private final ResendEmailNotificationService resendEmailService;

        /**
         * Test bug assignment notification.
         */
        @PostMapping("/bug-assigned")
        public ResponseEntity<Map<String, Object>> testBugAssignedNotification(
                        @RequestBody Map<String, Object> request) {

                UUID userId = UUID.fromString((String) request.get("userId"));
                String bugTitle = (String) request.getOrDefault("bugTitle", "Test Bug");
                String assignerName = (String) request.getOrDefault("assignerName", "Test Assigner");
                Integer bugId = (Integer) request.getOrDefault("bugId", 123);

                // Template variables
                Map<String, Object> templateVariables = Map.of(
                                "bugId", bugId,
                                "bugTitle", bugTitle,
                                "assignerName", assignerName,
                                "bugUrl", "http://localhost:3000/bugs/" + bugId);

                // Create and deliver notification
                UserNotification notification = notificationService.createAndDeliverNotification(
                                userId,
                                "BUG_ASSIGNED",
                                "Bug Assigned",
                                String.format("Bug #%d \"%s\" has been assigned to you by %s", bugId, bugTitle,
                                                assignerName),
                                String.format("{\"bugId\": %d, \"bugTitle\": \"%s\", \"assignerName\": \"%s\"}",
                                                bugId, bugTitle, assignerName),
                                templateVariables,
                                null, null, null, null);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "notificationId", notification != null ? notification.getNotificationId() : null,
                                "message", "Bug assignment notification sent",
                                "templateVariables", templateVariables));
        }

        /**
         * Test email service directly.
         */
        @PostMapping("/test-email")
        public ResponseEntity<Map<String, Object>> testEmail(@RequestBody Map<String, Object> request) {
                String toEmail = (String) request.get("toEmail");
                String subject = (String) request.getOrDefault("subject", "Test Email from BugTracker");
                String htmlContent = (String) request.getOrDefault("htmlContent",
                                "<h1>Test Email</h1><p>This is a test email from the BugTracker notification system.</p>");
                String textContent = (String) request.getOrDefault("textContent",
                                "Test Email - This is a test email from the BugTracker notification system.");

                try {
                        String messageId = resendEmailService.sendNotificationEmail(toEmail, subject, htmlContent,
                                        textContent);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "messageId", messageId,
                                        "serviceMode", resendEmailService.getServiceMode(),
                                        "message", "Email sent successfully"));
                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of(
                                        "success", false,
                                        "error", e.getMessage(),
                                        "serviceMode", resendEmailService.getServiceMode(),
                                        "message", "Email sending failed"));
                }
        }

        /**
         * Test template processing.
         */
        @PostMapping("/test-template")
        public ResponseEntity<Map<String, Object>> testTemplate(@RequestBody Map<String, Object> request) {
                String templateKey = (String) request.getOrDefault("templateKey", "BUG_ASSIGNED");
                String templateType = (String) request.getOrDefault("templateType", "html");
                @SuppressWarnings("unchecked")
                Map<String, Object> variables = (Map<String, Object>) request.getOrDefault("variables", Map.of(
                                "bugId", 123,
                                "bugTitle", "Test Bug",
                                "assignerName", "Test User",
                                "bugUrl", "http://localhost:3000/bugs/123"));

                try {
                        String processedContent = notificationTemplateService.processTemplate(templateKey, templateType,
                                        variables);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "templateKey", templateKey,
                                        "templateType", templateType,
                                        "variables", variables,
                                        "processedContent", processedContent));
                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of(
                                        "success", false,
                                        "error", e.getMessage(),
                                        "templateKey", templateKey,
                                        "templateType", templateType));
                }
        }

        /**
         * Initialize default templates.
         */
        @PostMapping("/init-templates")
        public ResponseEntity<Map<String, Object>> initializeTemplates() {
                try {
                        notificationTemplateService.initializeDefaultTemplates();

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Default templates initialized successfully"));
                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of(
                                        "success", false,
                                        "error", e.getMessage(),
                                        "message", "Failed to initialize templates"));
                }
        }

        /**
         * Get service status.
         */
        @GetMapping("/status")
        public ResponseEntity<Map<String, Object>> getServiceStatus() {
                return ResponseEntity.ok(Map.of(
                                "emailServiceReady", resendEmailService.isEmailServiceReady(),
                                "emailServiceMode", resendEmailService.getServiceMode(),
                                "templatesAvailable", notificationTemplateService.getAllActiveTemplates().size()));
        }
}
