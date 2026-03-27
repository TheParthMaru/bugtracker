package com.pbm5.bugtracker.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String corsAllowedOriginsRaw;

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
                    CorsConfiguration cfg = new CorsConfiguration();
                    List<String> origins = Arrays.stream(corsAllowedOriginsRaw.split(","))
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .toList();
                    cfg.setAllowedOrigins(origins);
                    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    cfg.setAllowedHeaders(List.of("*"));
                    cfg.setAllowCredentials(true);
                    cfg.setMaxAge(3600L);
                    return cfg;
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
