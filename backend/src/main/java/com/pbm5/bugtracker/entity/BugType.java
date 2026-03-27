package com.pbm5.bugtracker.entity;

public enum BugType {
    ISSUE("Issue"),
    TASK("Task"),
    SPEC("Specification");

    private final String displayName;

    BugType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}