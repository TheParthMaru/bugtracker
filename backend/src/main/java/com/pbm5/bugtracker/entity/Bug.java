package com.pbm5.bugtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "bugs", uniqueConstraints = @UniqueConstraint(columnNames = { "project_id",
        "project_ticket_number" }, name = "idx_bugs_project_ticket_number"))
public class Bug {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_ticket_number", nullable = false)
    private Integer projectTicketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @NotNull(message = "Project is required")
    private Project project;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Description is required")
    @Size(min = 1, message = "Description cannot be empty")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Bug type is required")
    private BugType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Status is required")
    private BugStatus status = BugStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Priority is required")
    private BugPriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    @NotNull(message = "Reporter is required")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Relationships
    @OneToMany(mappedBy = "bug", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BugAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "bug", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BugComment> comments = new ArrayList<>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "bug_label_mapping", joinColumns = @JoinColumn(name = "bug_id"), inverseJoinColumns = @JoinColumn(name = "label_id"))
    @Size(max = 10, message = "Maximum 10 labels allowed per bug")
    private Set<BugLabel> labels = new HashSet<>();

    @Column(columnDefinition = "TEXT[] DEFAULT '{}'")
    @Size(max = 20, message = "Maximum 20 tags allowed per bug")
    private Set<String> tags = new HashSet<>();

    // Constructors
    public Bug() {
    }

    public Bug(String title, String description, BugType type, BugPriority priority, Project project, User reporter) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.project = project;
        this.reporter = reporter;
        this.status = BugStatus.OPEN;
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
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

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
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

    public List<BugAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<BugAttachment> attachments) {
        this.attachments = attachments;
    }

    public List<BugComment> getComments() {
        return comments;
    }

    public void setComments(List<BugComment> comments) {
        this.comments = comments;
    }

    public Set<BugLabel> getLabels() {
        return labels;
    }

    public void setLabels(Set<BugLabel> labels) {
        // Log the change for debugging
        System.out.println("Bug.setLabels() called - Current labels count: " +
                (this.labels != null ? this.labels.size() : 0) +
                ", New labels count: " + (labels != null ? labels.size() : 0));

        if (labels != null) {
            System.out.println("Bug.setLabels() - New labels: " +
                    labels.stream().map(label -> "ID:" + label.getId() + ",Name:" + label.getName())
                            .collect(java.util.stream.Collectors.joining(", ")));
        }

        // ✅ SIMPLE: Just set the labels, let JPA handle the relationship automatically
        // Remove the problematic bidirectional management that was corrupting the
        // relationship
        this.labels = labels;

        // Log the result
        System.out.println("Bug.setLabels() completed - Final labels count: " +
                (this.labels != null ? this.labels.size() : 0));
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    // Business Logic Methods

    /**
     * Check if the bug can transition to the given status
     */
    public boolean canTransitionTo(BugStatus newStatus) {
        return status.canTransitionTo(newStatus);
    }

    /**
     * Transition the bug to a new status
     */
    public void transitionTo(BugStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("Invalid status transition from " + status + " to " + newStatus);
        }

        this.status = newStatus;

        // Set closed_at timestamp when closing
        if (newStatus == BugStatus.CLOSED && this.closedAt == null) {
            this.closedAt = LocalDateTime.now();
        }

        // Clear closed_at timestamp when reopening
        if (newStatus == BugStatus.REOPENED) {
            this.closedAt = null;
        }
    }

    /**
     * Assign the bug to a user
     */
    public void assignTo(User assignee) {
        this.assignee = assignee;
        // Status remains unchanged when assigning - assignment is separate from status
    }

    /**
     * Unassign the bug
     */
    public void unassign() {
        this.assignee = null;
        // Status remains unchanged when unassigning - assignment is separate from
        // status
    }

    /**
     * Reopen the bug
     */
    public void reopen() {
        this.status = BugStatus.REOPENED;
        this.closedAt = null;
    }

    /**
     * Check if the bug is assigned
     */
    public boolean isAssigned() {
        return assignee != null;
    }

    /**
     * Check if the bug is resolved
     */
    public boolean isResolved() {
        return status.isResolved();
    }

    /**
     * Check if the bug is active
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Check if the bug requires immediate attention
     */
    public boolean requiresImmediateAttention() {
        return priority.requiresImmediateAssignment() && !isAssigned();
    }

    /**
     * Add a label to the bug
     */
    public void addLabel(BugLabel label) {
        if (labels.size() >= 10) {
            throw new IllegalArgumentException("Maximum 10 labels allowed per bug");
        }
        labels.add(label);
    }

    /**
     * Remove a label from the bug
     */
    public void removeLabel(BugLabel label) {
        labels.remove(label);
    }

    /**
     * Add a tag to the bug
     */
    public void addTag(String tag) {
        if (tags.size() >= 20) {
            throw new IllegalArgumentException("Maximum 20 tags allowed per bug");
        }
        if (tag != null && !tag.trim().isEmpty()) {
            tags.add(tag.trim());
        }
    }

    /**
     * Remove a tag from the bug
     */
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    /**
     * Check if the bug has a specific tag
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    /**
     * Add an attachment to the bug
     */
    public void addAttachment(BugAttachment attachment) {
        attachments.add(attachment);
        attachment.setBug(this);
    }

    /**
     * Remove an attachment from the bug
     */
    public void removeAttachment(BugAttachment attachment) {
        attachments.remove(attachment);
        attachment.setBug(null);
    }

    /**
     * Add a comment to the bug
     */
    public void addComment(BugComment comment) {
        comments.add(comment);
        comment.setBug(this);
    }

    /**
     * Remove a comment from the bug
     */
    public void removeComment(BugComment comment) {
        comments.remove(comment);
        comment.setBug(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Bug bug = (Bug) o;
        return id != null && id.equals(bug.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Bug{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", priority=" + priority +
                ", project=" + (project != null ? project.getId() : null) +
                '}';
    }
}