package com.pbm5.bugtracker.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Configuration class for caching in the application.
 * Enables Spring caching and configures Caffeine as the cache provider.
 * 
 * This configuration is optimized for team and project permission caching
 * to improve performance of security checks and membership validation.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure the cache manager with Caffeine cache provider.
     * 
     * @return configured cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Configure cache settings for optimal performance
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // Maximum number of entries in cache
                .maximumSize(1000)
                // Expire entries after 15 minutes of write
                .expireAfterWrite(Duration.ofMinutes(15))
                // Expire entries after 5 minutes of last access
                .expireAfterAccess(Duration.ofMinutes(5))
                // Enable statistics for monitoring
                .recordStats());

        // Pre-create caches for better performance
        cacheManager.setCacheNames(java.util.List.of(
                "teamPermissions",
                "teamMembership",
                "projectPermissions",
                "projectMembership",
                "projectStats"));

        return cacheManager;
    }
}