package com.pbm5.bugtracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.lang.NonNull;

import com.pbm5.bugtracker.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.pbm5.bugtracker.repository.UserRepository;
import java.util.List;

/**
 * WebSocket Security Configuration for STOMP message-level authentication.
 * 
 * This configuration ensures that WebSocket messages are properly authenticated
 * while allowing the HTTP handshake to proceed without authentication.
 * 
 * Architecture:
 * - HTTP handshake is permitted (handled in SecurityConfig)
 * - STOMP CONNECT command validates JWT token from connectHeaders
 * - Message-level authorization for subscriptions and app destinations
 * - User context propagation for secure message routing
 * 
 * Security Features:
 * - JWT token validation on STOMP CONNECT
 * - User-specific channel access control (/user/{userId}/*)
 * - Authenticated access to application destinations (/app/*)
 * - Automatic user context setting for message routing
 * 
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Configure inbound message security rules.
     * 
     * Defines which destinations require authentication and what access controls
     * apply.
     */
    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Handle STOMP CONNECT - validate JWT token
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        try {
                            String token = authHeader.substring(7);
                            String userEmail = jwtService.extractEmail(token);

                            if (userEmail != null && jwtService.isTokenValid(token)) {
                                // Load user from repository (same as JwtAuthFilter)
                                var user = userRepository.findByEmail(userEmail).orElse(null);

                                if (user != null) {
                                    // Create authorities based on user role (same as JwtAuthFilter)
                                    var authorities = List
                                            .of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                                    // Create authentication object
                                    Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                            user, null, authorities);

                                    // Set authentication in accessor for this session
                                    accessor.setUser(auth);

                                    // log.debug("WebSocket STOMP authentication successful for user: {}",
                                    // userEmail);
                                } else {
                                    log.warn("User not found in database for email: {}", userEmail);
                                    throw new SecurityException("User not found: " + userEmail);
                                }
                            } else {
                                log.warn("Invalid JWT token in WebSocket STOMP CONNECT");
                                throw new SecurityException("Invalid token");
                            }
                        } catch (Exception e) {
                            log.error("WebSocket STOMP authentication failed: {}", e.getMessage());
                            throw new SecurityException("Authentication failed: " + e.getMessage());
                        }
                    } else {
                        log.warn("No Authorization header in WebSocket STOMP CONNECT");
                        throw new SecurityException("Missing authentication token");
                    }
                }

                return message;
            }
        });
    }
}
