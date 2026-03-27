package com.pbm5.bugtracker.entity;

/**
 * Enum for point values in the gamification system
 * Centralizes all point calculations and prevents magic numbers
 */
public enum PointValue {
    WELCOME_BONUS(1),
    DAILY_LOGIN(1),
    BUG_RESOLUTION_CRASH(100),
    BUG_RESOLUTION_CRITICAL(75),
    BUG_RESOLUTION_HIGH(50),
    BUG_RESOLUTION_MEDIUM(25),
    BUG_RESOLUTION_LOW(10),
    BUG_REOPENED_PENALTY(10);

    private final int points;

    PointValue(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }

    public static int getBugResolutionPoints(String priority) {
        return switch (priority.toUpperCase()) {
            case "CRASH" -> BUG_RESOLUTION_CRASH.points;
            case "CRITICAL" -> BUG_RESOLUTION_CRITICAL.points;
            case "HIGH" -> BUG_RESOLUTION_HIGH.points;
            case "MEDIUM" -> BUG_RESOLUTION_MEDIUM.points;
            case "LOW" -> BUG_RESOLUTION_LOW.points;
            default -> 0;
        };
    }
}