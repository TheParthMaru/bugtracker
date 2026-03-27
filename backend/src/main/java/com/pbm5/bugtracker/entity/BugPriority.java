package com.pbm5.bugtracker.entity;

public enum BugPriority {
    CRASH("Crash", 1),
    CRITICAL("Critical", 2),
    HIGH("High", 3),
    MEDIUM("Medium", 4),
    LOW("Low", 5);

    private final String displayName;
    private final int weight;

    BugPriority(String displayName, int weight) {
        this.displayName = displayName;
        this.weight = weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * Check if this priority is higher than the given priority
     */
    public boolean isHigherThan(BugPriority other) {
        return this.weight < other.weight;
    }

    /**
     * Check if this priority is lower than the given priority
     */
    public boolean isLowerThan(BugPriority other) {
        return this.weight > other.weight;
    }

    /**
     * Get the SLA response time in hours for this priority
     */
    public int getSlaResponseHours() {
        return switch (this) {
            case CRASH, CRITICAL -> 1; // Immediate response
            case HIGH -> 24; // 24 hours
            case MEDIUM -> 168; // 1 week
            case LOW -> -1; // No strict SLA
        };
    }

    /**
     * Check if this priority requires immediate assignment
     */
    public boolean requiresImmediateAssignment() {
        return this == CRASH || this == CRITICAL;
    }

    /**
     * Get the color code for UI display
     */
    public String getColor() {
        return switch (this) {
            case CRASH -> "#DC2626"; // Red
            case CRITICAL -> "#EA580C"; // Orange
            case HIGH -> "#D97706"; // Amber
            case MEDIUM -> "#059669"; // Green
            case LOW -> "#6B7280"; // Gray
        };
    }
}