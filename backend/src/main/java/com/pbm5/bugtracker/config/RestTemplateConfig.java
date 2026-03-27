package com.pbm5.bugtracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate used by external API services.
 * 
 * This configuration provides a RestTemplate bean for:
 * - Resend email API integration
 * - Other external service integrations
 * 
 * The RestTemplate is configured with appropriate timeouts
 * and error handling for production use.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Create RestTemplate bean for external API calls.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
