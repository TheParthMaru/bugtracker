package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.BugPriority;
import com.pbm5.bugtracker.entity.BugType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public class CreateBugRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 1, message = "Description cannot be empty")
    private String description;

    @NotNull(message = "Bug type is required")
    private BugType type;

    @NotNull(message = "Priority is required")
    private BugPriority priority;

    private UUID assigneeId;

    private Set<Long> labelIds;

    private Set<String> tags;

    private Set<String> assignedTeamIds;

    // Constructors
    public CreateBugRequest() {
    }

    public CreateBugRequest(String title, String description, BugType type, BugPriority priority) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
    }

    public CreateBugRequest(String title, String description, BugType type, BugPriority priority, UUID assigneeId,
            Set<Long> labelIds, Set<String> tags) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.assigneeId = assigneeId;
        this.labelIds = labelIds;
        this.tags = tags;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BugType getType() {
        return type;
    }

    public void setType(BugType type) {
        this.type = type;
    }

    public BugPriority getPriority() {
        return priority;
    }

    public void setPriority(BugPriority priority) {
        this.priority = priority;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Set<Long> getLabelIds() {
        return labelIds;
    }

    public void setLabelIds(Set<Long> labelIds) {
        this.labelIds = labelIds;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Set<String> getAssignedTeamIds() {
        return assignedTeamIds;
    }

    public void setAssignedTeamIds(Set<String> assignedTeamIds) {
        this.assignedTeamIds = assignedTeamIds;
    }
}