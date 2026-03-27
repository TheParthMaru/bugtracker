package com.pbm5.bugtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bug_comments")
public class BugComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bug_id", nullable = false)
    @NotNull(message = "Bug is required")
    private Bug bug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BugComment parent;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Comment content is required")
    @Size(min = 1, message = "Comment content cannot be empty")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull(message = "Author is required")
    private User author;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BugComment> replies = new ArrayList<>();

    // Constructors
    public BugComment() {
    }

    public BugComment(String content, User author) {
        this.content = content;
        this.author = author;
    }

    public BugComment(String content, User author, BugComment parent) {
        this.content = content;
        this.author = author;
        this.parent = parent;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bug getBug() {
        return bug;
    }

    public void setBug(Bug bug) {
        this.bug = bug;
    }

    public BugComment getParent() {
        return parent;
    }

    public void setParent(BugComment parent) {
        this.parent = parent;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
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

    public List<BugComment> getReplies() {
        return replies;
    }

    public void setReplies(List<BugComment> replies) {
        this.replies = replies;
    }

    // Business Logic Methods

    /**
     * Check if this comment is a reply
     */
    public boolean isReply() {
        return parent != null;
    }

    /**
     * Check if this comment is a top-level comment
     */
    public boolean isTopLevel() {
        return parent == null;
    }

    /**
     * Check if this comment has replies
     */
    public boolean hasReplies() {
        return !replies.isEmpty();
    }

    /**
     * Get the number of replies
     */
    public int getReplyCount() {
        return replies.size();
    }

    /**
     * Add a reply to this comment
     */
    public void addReply(BugComment reply) {
        replies.add(reply);
        reply.setParent(this);
    }

    /**
     * Remove a reply from this comment
     */
    public void removeReply(BugComment reply) {
        replies.remove(reply);
        reply.setParent(null);
    }

    /**
     * Check if the comment has been edited
     */
    public boolean isEdited() {
        return updatedAt != null && !updatedAt.equals(createdAt);
    }

    /**
     * Get the comment depth level (0 for top-level, 1 for replies, etc.)
     */
    public int getDepth() {
        if (parent == null) {
            return 0;
        }
        return parent.getDepth() + 1;
    }

    /**
     * Check if this comment can be edited by the given user
     */
    public boolean canBeEditedBy(User user) {
        return user != null && author != null && user.getId().equals(author.getId());
    }

    /**
     * Check if this comment can be deleted by the given user
     */
    public boolean canBeDeletedBy(User user) {
        return user != null && author != null && user.getId().equals(author.getId());
    }

    /**
     * Get a preview of the comment content (first 100 characters)
     */
    public String getContentPreview() {
        if (content == null) {
            return "";
        }
        if (content.length() <= 100) {
            return content;
        }
        return content.substring(0, 100) + "...";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BugComment that = (BugComment) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BugComment{" +
                "id=" + id +
                ", content='" + getContentPreview() + '\'' +
                ", author=" + (author != null ? author.getEmail() : null) +
                ", parent=" + (parent != null ? parent.getId() : null) +
                ", bug=" + (bug != null ? bug.getId() : null) +
                '}';
    }
}