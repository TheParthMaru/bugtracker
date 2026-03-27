package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for bug duplicate summary information.
 * 
 * This DTO provides essential information about a bug that is a duplicate,
 * including basic bug details and duplicate relationship metadata.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public class BugDuplicateSummaryResponse {

    private Long id;
    private Integer projectTicketNumber;
    private String title;
    private String status;
    private String priority;
    private String assigneeName;
    private String reporterName;
    private LocalDateTime createdAt;
    private LocalDateTime markedAsDuplicateAt;
    private String markedByUserName;

    // Constructors
    public BugDuplicateSummaryResponse() {
    }

    public BugDuplicateSummaryResponse(Long id, Integer projectTicketNumber, String title,
            String status, String priority) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
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

    public LocalDateTime getMarkedAsDuplicateAt() {
        return markedAsDuplicateAt;
    }

    public void setMarkedAsDuplicateAt(LocalDateTime markedAsDuplicateAt) {
        this.markedAsDuplicateAt = markedAsDuplicateAt;
    }

    public String getMarkedByUserName() {
        return markedByUserName;
    }

    public void setMarkedByUserName(String markedByUserName) {
        this.markedByUserName = markedByUserName;
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

    public boolean hasDuplicateInfo() {
        return markedAsDuplicateAt != null && markedByUserName != null;
    }

    @Override
    public String toString() {
        return "BugDuplicateSummaryResponse{" +
                "id=" + id +
                ", projectTicketNumber=" + projectTicketNumber +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", markedAsDuplicateAt=" + markedAsDuplicateAt +
                ", markedByUserName='" + markedByUserName + '\'' +
                '}';
    }
}
