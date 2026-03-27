package com.pbm5.bugtracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

/**
 * Core utility class to generate, parse, and validate JWT tokens.
 * Centralizes JWT logic so our app doesn't repeat signing key logic or token
 * parsing across multiple services.
 * 
 * Configuration is externalized to application.properties for security and
 * flexibility:
 * - jwt.secret-key: Secret key for signing JWT tokens (override in production)
 * - jwt.expiration-ms: Token expiration time in milliseconds
 */
@Service
public class JwtService {

    /**
     * JWT secret key injected from application.properties.
     * In production, override with environment variable: JWT_SECRET_KEY
     */
    @Value("${jwt.secret-key}")
    private String secretKey;

    /**
     * JWT expiration time in milliseconds injected from application.properties.
     * In production, override with environment variable: JWT_EXPIRATION_MS
     */
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Get the signing key for JWT token operations.
     * Uses HMAC-SHA256 algorithm with the configured secret key.
     * 
     * @return Key object for signing/verifying JWT tokens
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Generate a JWT token for the given email.
     * 
     * @param email user's email address to embed in token
     * @return signed JWT token string
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract email from JWT token.
     * 
     * @param token JWT token string
     * @return email address from token subject
     * @throws JwtException if token is invalid or expired
     */
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validate JWT token signature and expiration.
     * 
     * @param token JWT token string to validate
     * @return true if token is valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
