package com.pbm5.bugtracker.entity;

/**
 * Enumeration representing different methods of duplicate bug detection.
 * 
 * This enum defines how duplicate relationships between bugs are identified:
 * - MANUAL: User manually marked bugs as duplicates
 * - AUTOMATIC: System automatically detected duplicates using similarity
 * algorithms
 * - HYBRID: Combination of automatic detection with manual verification
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public enum DuplicateDetectionMethod {

    /**
     * Manual detection - user explicitly marked bugs as duplicates
     */
    MANUAL("Manual", "User manually identified and marked duplicate bugs"),

    /**
     * Automatic detection - system detected duplicates using similarity algorithms
     */
    AUTOMATIC("Automatic", "System automatically detected duplicates using similarity analysis"),

    /**
     * Hybrid detection - automatic detection with manual verification
     */
    HYBRID("Hybrid", "System suggested duplicates, confirmed by user");

    private final String displayName;
    private final String description;

    DuplicateDetectionMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get human-readable display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get description of the detection method
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this method involves automatic detection
     */
    public boolean isAutomatic() {
        return this == AUTOMATIC || this == HYBRID;
    }

    /**
     * Check if this method involves manual verification
     */
    public boolean isManual() {
        return this == MANUAL || this == HYBRID;
    }

    /**
     * Get enum from string value (case-insensitive)
     */
    public static DuplicateDetectionMethod fromString(String value) {
        if (value == null) {
            return MANUAL; // Default to manual
        }

        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MANUAL; // Default to manual if invalid value
        }
    }
}