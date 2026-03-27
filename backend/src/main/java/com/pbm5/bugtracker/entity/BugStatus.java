package com.pbm5.bugtracker.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum BugStatus {
    OPEN("Open"),
    FIXED("Fixed"),
    CLOSED("Closed"),
    REOPENED("Reopened");

    private final String displayName;

    BugStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if the status transition is valid
     */
    public boolean canTransitionTo(BugStatus newStatus) {
        return getValidTransitions().contains(newStatus);
    }

    /**
     * Get valid status transitions from current status
     */
    public Set<BugStatus> getValidTransitions() {
        return switch (this) {
            case OPEN -> Set.of(FIXED, CLOSED);
            case FIXED -> Set.of(CLOSED, REOPENED);
            case CLOSED -> Set.of(REOPENED);
            case REOPENED -> Set.of(FIXED, CLOSED);
        };
    }

    /**
     * Check if the status indicates the bug is resolved
     */
    public boolean isResolved() {
        return this == FIXED || this == CLOSED;
    }

    /**
     * Check if the status indicates the bug is active
     */
    public boolean isActive() {
        return this == OPEN || this == REOPENED;
    }
}