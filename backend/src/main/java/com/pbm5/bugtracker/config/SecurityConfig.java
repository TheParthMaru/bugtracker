package com.pbm5.bugtracker.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Skip the entire security chain (including CORS) for platform health checks.
     * {@code permitAll()} still runs filters; global CORS with credentials + wildcard
     * can fail for browser/tool {@code Origin} headers and surface as 500 before the controller runs.
     */
    @Bean
    public WebSecurityCustomizer healthEndpointIgnoring() {
        return web -> web.ignoring().requestMatchers("/healthz", "/health");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
                    corsConfiguration.setAllowedOriginPatterns(java.util.List.of("*"));
                    corsConfiguration.setAllowedMethods(java.util.List.of("*"));
                    corsConfiguration.setAllowedHeaders(java.util.List.of("*"));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                }))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers("/healthz", "/health").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/bugtracker/v1/auth/**").permitAll()

                        // WebSocket endpoints (HTTP handshake level - message level auth handled
                        // separately)
                        .requestMatchers("/ws-notifications/**").permitAll()
                        .requestMatchers("/ws-notifications-native/**").permitAll()

                        // Authenticated endpoints
                        .requestMatchers("/api/bugtracker/v1/profile").authenticated()
                        .requestMatchers("/api/bugtracker/v1/teams/**").authenticated()
                        .requestMatchers("/api/bugtracker/v1/projects/**").authenticated()
                        .requestMatchers("/api/bugtracker/v1/bugs/**").authenticated()
                        .requestMatchers("/api/bugtracker/v1/users/me/teams").authenticated()
                        .requestMatchers("/api/similarity/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
