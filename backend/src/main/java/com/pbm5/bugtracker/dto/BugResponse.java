package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.BugPriority;
import com.pbm5.bugtracker.entity.BugStatus;
import com.pbm5.bugtracker.entity.BugType;
import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation.TeamAssignmentInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BugResponse {

    private static final Logger logger = LoggerFactory.getLogger(BugResponse.class);

    private Long id;
    private Integer projectTicketNumber;
    private String title;
    private String description;
    private BugType type;
    private BugStatus status;
    private BugPriority priority;
    private UUID projectId;
    private String projectName;
    private String projectSlug;
    private UserResponse reporter;
    private UserResponse assignee;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private List<BugAttachmentResponse> attachments;
    private List<BugCommentResponse> comments;
    private Set<BugLabelResponse> labels;
    private Set<String> tags;
    private List<TeamAssignmentInfo> assignedTeams;
    private long commentCount;
    private long attachmentCount;

    // Constructors
    public BugResponse() {
    }

    public BugResponse(Bug bug) {
        this.id = bug.getId();
        this.projectTicketNumber = bug.getProjectTicketNumber();
        this.title = bug.getTitle();
        this.description = bug.getDescription();
        this.type = bug.getType();
        this.status = bug.getStatus();
        this.priority = bug.getPriority();
        this.projectId = bug.getProject().getId();
        this.projectName = bug.getProject().getName();
        this.projectSlug = bug.getProject().getProjectSlug();
        this.reporter = new UserResponse(bug.getReporter());
        this.assignee = bug.getAssignee() != null ? new UserResponse(bug.getAssignee()) : null;

        this.createdAt = bug.getCreatedAt();
        this.updatedAt = bug.getUpdatedAt();
        this.closedAt = bug.getClosedAt();

        // Convert attachments
        this.attachments = bug.getAttachments().stream()
                .map(BugAttachmentResponse::new)
                .collect(Collectors.toList());

        // Convert comments
        this.comments = bug.getComments().stream()
                .map(BugCommentResponse::new)
                .collect(Collectors.toList());

        // Convert labels
        logger.info("BugResponse -> Constructor -> Converting labels from bug entity. Size: {}, Type: {}",
                bug.getLabels() != null ? bug.getLabels().size() : 0,
                bug.getLabels() != null ? bug.getLabels().getClass().getSimpleName() : "null");

        this.labels = bug.getLabels().stream()
                .map(BugLabelResponse::new)
                .collect(Collectors.toSet());

        logger.info("BugResponse -> Constructor -> Labels conversion completed. Size: {}",
                this.labels != null ? this.labels.size() : 0);

        // Convert tags
        this.tags = bug.getTags();

        // Team assignments will be populated separately since they require service
        // calls
        this.assignedTeams = new ArrayList<>();

        this.commentCount = bug.getComments().size();
        this.attachmentCount = bug.getAttachments().size();
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

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectSlug() {
        return projectSlug;
    }

    public void setProjectSlug(String projectSlug) {
        this.projectSlug = projectSlug;
    }

    public UserResponse getReporter() {
        return reporter;
    }

    public void setReporter(UserResponse reporter) {
        this.reporter = reporter;
    }

    public UserResponse getAssignee() {
        return assignee;
    }

    public void setAssignee(UserResponse assignee) {
        this.assignee = assignee;
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

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public List<BugAttachmentResponse> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<BugAttachmentResponse> attachments) {
        this.attachments = attachments;
    }

    public List<BugCommentResponse> getComments() {
        return comments;
    }

    public void setComments(List<BugCommentResponse> comments) {
        this.comments = comments;
    }

    public Set<BugLabelResponse> getLabels() {
        return labels;
    }

    public void setLabels(Set<BugLabelResponse> labels) {
        this.labels = labels;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }

    public long getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(long attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public List<TeamAssignmentInfo> getAssignedTeams() {
        return assignedTeams;
    }

    public void setAssignedTeams(List<TeamAssignmentInfo> assignedTeams) {
        this.assignedTeams = assignedTeams;
    }
}