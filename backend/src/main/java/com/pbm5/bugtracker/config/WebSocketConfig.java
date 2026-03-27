package com.pbm5.bugtracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.lang.NonNull;

/**
 * WebSocket configuration for real-time notifications.
 * 
 * Configures STOMP messaging over WebSocket to enable:
 * - Real-time notification delivery
 * - Live notification count updates
 * - Toast notification broadcasting
 * - User-specific notification channels
 * 
 * Architecture:
 * - STOMP protocol over WebSocket for reliable messaging
 * - User-specific channels: /user/{userId}/notifications
 * - Broadcast channels: /topic/notifications (if needed for global updates)
 * - SockJS fallback for browsers without WebSocket support
 * 
 * Security:
 * - JWT-based authentication for WebSocket connections
 * - User isolation through individual channels
 * - Message filtering based on user permissions
 * 
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for handling subscriptions and broadcasting.
     * 
     * Sets up:
     * - Simple broker for /topic and /user destinations
     * - Application destination prefix for client messages
     * - User destination prefix for user-specific channels
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Enable simple broker for /topic and /user destinations
        // /topic/* - for broadcast messages (optional, for global notifications)
        // /user/* - for user-specific messages (main use case)
        config.enableSimpleBroker("/topic", "/user");

        // Prefix for messages FROM client TO server
        // e.g., client sends to /app/notifications/mark-read
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destinations
        // e.g., server sends to /user/{userId}/notifications
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints for WebSocket connections.
     * 
     * Configures:
     * - Main WebSocket endpoint at /ws-notifications
     * - SockJS fallback support for older browsers
     * - CORS configuration for frontend integration
     */
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Main WebSocket endpoint
        registry.addEndpoint("/ws-notifications")
                .setAllowedOriginPatterns("*") // Configure properly for production
                .withSockJS(); // Enable SockJS fallback

        // Alternative endpoint without SockJS (for modern browsers)
        registry.addEndpoint("/ws-notifications-native")
                .setAllowedOriginPatterns("*"); // Configure properly for production
    }
}
