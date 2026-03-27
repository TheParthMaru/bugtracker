package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.BugStatus;
import com.pbm5.bugtracker.entity.BugPriority;
import java.time.LocalDateTime;

/**
 * DTO containing summary information about a bug.
 * 
 * This DTO provides essential bug details for display in duplicate
 * relationships,
 * bug lists, and other summary views.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public class BugSummaryResponse {

    private Long id;
    private Integer projectTicketNumber;
    private String title;
    private BugStatus status;
    private BugPriority priority;
    private String assigneeName;
    private String reporterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public BugSummaryResponse() {
    }

    public BugSummaryResponse(Long id, Integer projectTicketNumber, String title,
            BugStatus status, BugPriority priority) {
        this.id = id;
        this.projectTicketNumber = projectTicketNumber;
        this.title = title;
        this.status = status;
        this.priority = priority;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getProjectTicketNumber() {
        return projectTicketNumber;
    }

    public void setProjectTicketNumber(Integer projectTicketNumber) {
        this.projectTicketNumber = projectTicketNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BugStatus getStatus() {
        return status;
    }

    public void setStatus(BugStatus status) {
        this.status = status;
    }

    public BugPriority getPriority() {
        return priority;
    }

    public void setPriority(BugPriority priority) {
        this.priority = priority;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility Methods
    public String getDisplayName() {
        return String.format("#%d: %s",
                projectTicketNumber != null ? projectTicketNumber : id,
                title != null ? title : "Untitled");
    }

    public boolean isAssigned() {
        return assigneeName != null && !assigneeName.trim().isEmpty();
    }

    public boolean isActive() {
        return status != null && status.isActive();
    }

    @Override
    public String toString() {
        return "BugSummaryResponse{" +
                "id=" + id +
                ", projectTicketNumber=" + projectTicketNumber +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                '}';
    }
}
