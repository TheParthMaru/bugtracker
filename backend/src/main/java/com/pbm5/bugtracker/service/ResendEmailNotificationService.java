package com.pbm5.bugtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.pbm5.bugtracker.exception.EmailDeliveryException;

import java.util.List;
import java.util.Map;

/**
 * Service for sending email notifications via Resend API.
 * 
 * Supports both mock and real implementations:
 * - Mock mode: Logs email content for testing (default)
 * - Real mode: Sends emails via Resend API when configured
 * 
 * Features:
 * - Smart mock/real mode switching via configuration
 * - Comprehensive error handling and logging
 * - Success/failure simulation for testing
 * - Production-ready Resend API integration
 * - Delivery tracking and external ID correlation
 * 
 * Configuration:
 * - resend.enabled=false (mock mode)
 * - resend.enabled=true (real mode, requires API key)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailNotificationService {

    @Value("${resend.api.key:MOCK_API_KEY}")
    private String resendApiKey;

    @Value("${resend.from.email:noreply@bugtracker.com}")
    private String fromEmail;

    @Value("${resend.from.name:BugTracker Notifications}")
    private String fromName;

    @Value("${resend.api.url:https://api.resend.com}")
    private String apiUrl;

    @Value("${resend.enabled:false}")
    private boolean resendEnabled;

    private final RestTemplate restTemplate;

    /**
     * Send a single notification email.
     * 
     * @param toEmail     Recipient email address
     * @param subject     Email subject line
     * @param htmlContent HTML email content
     * @param textContent Plain text email content (fallback)
     * @return External message ID from Resend (or mock ID)
     */
    public String sendNotificationEmail(String toEmail, String subject, String htmlContent, String textContent) {
        log.debug("Sending notification email to: {} with subject: {}", toEmail, subject);

        if (resendEnabled && StringUtils.hasText(resendApiKey) && !"MOCK_API_KEY".equals(resendApiKey)) {
            return sendViaResendAPI(toEmail, subject, htmlContent, textContent);
        } else {
            return sendMockEmail(toEmail, subject, htmlContent, textContent);
        }
    }

    /**
     * Send notification email via real Resend API.
     */
    private String sendViaResendAPI(String toEmail, String subject, String htmlContent, String textContent) {
        try {
            // Create email request payload
            Map<String, Object> emailRequest = Map.of(
                    "from", fromName + " <" + fromEmail + ">",
                    "to", List.of(toEmail),
                    "subject", subject,
                    "html", htmlContent,
                    "text", textContent);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            // Create HTTP entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emailRequest, headers);

            // Send email via Resend API
            @SuppressWarnings("rawtypes")

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/emails",
                    entity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                assert responseBody != null; // Already checked above
                String messageId = (String) responseBody.get("id");
                log.info("Email sent successfully via Resend to: {} with ID: {}", toEmail, messageId);
                return messageId;
            } else {
                log.error("Failed to send email via Resend: {}", response.getBody());
                throw new EmailDeliveryException("Failed to send email via Resend: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error sending email via Resend to: {}", toEmail, e);
            throw new EmailDeliveryException("Resend email service error: " + e.getMessage(), e);
        }
    }

    /**
     * Send mock email for testing purposes.
     */
    private String sendMockEmail(String toEmail, String subject, String htmlContent, String textContent) {
        log.info("=== MOCK EMAIL NOTIFICATION ===");
        log.info("From: {} <{}>", fromName, fromEmail);
        log.info("To: {}", toEmail);
        log.info("Subject: {}", subject);
        log.info("HTML Content: {}", htmlContent);
        log.info("Text Content: {}", textContent);
        log.info("===============================");

        // Simulate delivery success/failure for testing
        return simulateEmailDelivery(toEmail, subject);
    }

    /**
     * Simulate email delivery with realistic success/failure rates.
     */
    private String simulateEmailDelivery(String toEmail, String subject) {
        // Simulate different scenarios for testing
        double random = Math.random();

        if (random < 0.90) {
            // 90% success rate
            String mockId = "mock_" + System.currentTimeMillis();
            log.info("MOCK: Email delivery simulated as SUCCESS with ID: {}", mockId);
            return mockId;
        } else if (random < 0.95) {
            // 5% temporary failure (will retry)
            log.warn("MOCK: Email delivery simulated as TEMPORARY FAILURE (will retry)");
            throw new EmailDeliveryException("Mock temporary failure - network timeout");
        } else {
            // 5% permanent failure (bounced)
            log.error("MOCK: Email delivery simulated as PERMANENT FAILURE (bounced)");
            throw new EmailDeliveryException("Mock permanent failure - invalid email address");
        }
    }

    /**
     * Check if email service is configured and ready.
     */
    public boolean isEmailServiceReady() {
        if (resendEnabled) {
            return !"MOCK_API_KEY".equals(resendApiKey) && resendApiKey != null && !resendApiKey.trim().isEmpty();
        }
        return true; // Mock mode is always ready
    }

    /**
     * Get current service mode for debugging.
     */
    public String getServiceMode() {
        if (resendEnabled && !"MOCK_API_KEY".equals(resendApiKey)) {
            return "REAL_RESEND";
        }
        return "MOCK";
    }
}
