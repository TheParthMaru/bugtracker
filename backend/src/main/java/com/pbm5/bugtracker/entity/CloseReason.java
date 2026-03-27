package com.pbm5.bugtracker.entity;

public enum CloseReason {
    FIXED("Fixed"),
    INVALID("Invalid"),
    DUPLICATE("Duplicate"),
    WONT_FIX("Won't Fix"),
    WORKS_FOR_ME("Works for Me");

    private final String displayName;

    CloseReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this close reason indicates the issue was resolved
     */
    public boolean isResolved() {
        return this == FIXED;
    }

    /**
     * Check if this close reason indicates the issue was not a real problem
     */
    public boolean isNotRealIssue() {
        return this == INVALID || this == WORKS_FOR_ME;
    }

    /**
     * Check if this close reason indicates the issue was already reported
     */
    public boolean isDuplicate() {
        return this == DUPLICATE;
    }

    /**
     * Check if this close reason indicates the issue won't be addressed
     */
    public boolean isWontFix() {
        return this == WONT_FIX;
    }
}