package com.pbm5.bugtracker.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import com.github.slugify.Slugify;

@Service
public class SlugService {
    private final Slugify slugify;

    public SlugService() {
        this.slugify = Slugify.builder()
                .customReplacement("_", "-")
                .customReplacement("&", "and")
                .customReplacement("@", "at")
                .build();
    }

    /**
     * Generate a URL-friendly slug from a team name
     * 
     * @param teamName The original team name
     * @return A URL-friendly slug
     */
    public String generateSlug(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
        }

        String slug = slugify.slugify(teamName.trim());

        // Ensure slug is not empty after slugification
        if (slug.isEmpty()) {
            // Fallback to a default pattern if slugification results in empty string
            slug = "team-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // Ensure slug doesn't exceed database constraints (increased to 200 for project
        // prefix)
        if (slug.length() > 200) {
            slug = slug.substring(0, 197) + "...";
        }

        return slug;
    }

    /**
     * Generate a project-scoped team slug with format: <project-slug>-<team-slug>
     * 
     * @param projectSlug The project slug
     * @param teamSlug    The team slug
     * @return A project-scoped team slug
     */
    public String generateProjectTeamSlug(String projectSlug, String teamSlug) {
        if (projectSlug == null || projectSlug.trim().isEmpty()) {
            throw new IllegalArgumentException("Project slug cannot be null or empty");
        }
        if (teamSlug == null || teamSlug.trim().isEmpty()) {
            throw new IllegalArgumentException("Team slug cannot be null or empty");
        }

        String combinedSlug = projectSlug.trim() + "-" + teamSlug.trim();

        // Ensure combined slug doesn't exceed database constraints
        if (combinedSlug.length() > 200) {
            // Truncate team slug if needed, keeping project slug intact
            int maxTeamSlugLength = 200 - projectSlug.length() - 1; // -1 for hyphen
            if (maxTeamSlugLength > 0) {
                combinedSlug = projectSlug.trim() + "-" + teamSlug.trim().substring(0, maxTeamSlugLength);
            } else {
                // If project slug is too long, truncate it
                combinedSlug = projectSlug.trim().substring(0, 199) + "-";
            }
        }

        return combinedSlug;
    }

    /**
     * Generate a team slug with project prefix
     * Format: <project-slug>-<team-slug>
     * 
     * @param projectSlug The project slug
     * @param teamName    The team name
     * @return A project-scoped team slug
     */
    public String generateTeamSlug(String projectSlug, String teamName) {
        if (projectSlug == null || projectSlug.trim().isEmpty()) {
            throw new IllegalArgumentException("Project slug cannot be null or empty");
        }

        if (teamName == null || teamName.trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
        }

        String baseSlug = generateSlug(teamName);
        String fullSlug = projectSlug + "-" + baseSlug;

        // Ensure the full slug doesn't exceed database constraints
        if (fullSlug.length() > 200) {
            int maxBaseLength = 200 - projectSlug.length() - 1; // -1 for the hyphen
            fullSlug = projectSlug + "-" + baseSlug.substring(0, maxBaseLength);
        }

        return fullSlug;
    }

    /**
     * Generate a unique slug by appending a counter if the base slug already exists
     * This method should be used with a repository check to ensure uniqueness
     * 
     * @param baseSlug The base slug to make unique
     * @param counter  The counter to append
     * @return A unique slug with counter suffix
     */
    public String generateUniqueSlug(String baseSlug, int counter) {
        if (counter <= 1) {
            return baseSlug;
        }

        String suffix = "-" + counter;
        String uniqueSlug = baseSlug + suffix;

        // Ensure the unique slug doesn't exceed database constraints
        if (uniqueSlug.length() > 100) {
            int maxBaseLength = 100 - suffix.length();
            uniqueSlug = baseSlug.substring(0, maxBaseLength) + suffix;
        }

        return uniqueSlug;
    }

    /**
     * Validate if a slug meets the required format
     * 
     * @param slug The slug to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return false;
        }

        // Check against the regex pattern defined in the Team entity (increased to 200)
        return slug.matches("^[a-z0-9-]+$") && slug.length() <= 200;
    }

    /**
     * Clean and validate a manually provided slug
     * 
     * @param slug The slug to clean
     * @return A cleaned and validated slug
     */
    public String cleanSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            throw new IllegalArgumentException("Slug cannot be null or empty");
        }

        // Convert to lowercase and replace invalid characters
        String cleaned = slug.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Slug cannot be empty after cleaning");
        }

        // Ensure slug doesn't exceed database constraints (increased to 200)
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200);
        }

        return cleaned;
    }
}