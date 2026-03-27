package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;

/**
 * DTO containing information about a duplicate relationship.
 * 
 * This DTO provides details about how and when a bug was marked as duplicate,
 * including the user who marked it and the timestamp.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public class DuplicateRelationshipInfo {

    private String markedByUserName;
    private LocalDateTime markedAt;

    // Constructors
    public DuplicateRelationshipInfo() {
    }

    public DuplicateRelationshipInfo(String markedByUserName, LocalDateTime markedAt) {
        this.markedByUserName = markedByUserName;
        this.markedAt = markedAt;
    }

    // Getters and Setters
    public String getMarkedByUserName() {
        return markedByUserName;
    }

    public void setMarkedByUserName(String markedByUserName) {
        this.markedByUserName = markedByUserName;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    // Utility Methods
    public boolean hasMarkedByUser() {
        return markedByUserName != null && !markedByUserName.trim().isEmpty();
    }

    public boolean hasMarkedAt() {
        return markedAt != null;
    }

    @Override
    public String toString() {
        return "DuplicateRelationshipInfo{" +
                "markedByUserName='" + markedByUserName + '\'' +
                ", markedAt=" + markedAt +
                '}';
    }
}
