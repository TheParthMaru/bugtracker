package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.BugComment;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BugCommentResponse {

    private Long id;
    private String content;
    private UserResponse author;
    private Long parentId;
    private List<BugCommentResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int depth;
    private long replyCount;

    // Constructors
    public BugCommentResponse() {
    }

    public BugCommentResponse(BugComment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.author = new UserResponse(comment.getAuthor());
        this.parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();

        // Convert replies
        this.replies = comment.getReplies().stream()
                .map(BugCommentResponse::new)
                .collect(Collectors.toList());

        this.replyCount = comment.getReplies().size();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UserResponse getAuthor() {
        return author;
    }

    public void setAuthor(UserResponse author) {
        this.author = author;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<BugCommentResponse> getReplies() {
        return replies;
    }

    public void setReplies(List<BugCommentResponse> replies) {
        this.replies = replies;
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

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public long getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(long replyCount) {
        this.replyCount = replyCount;
    }
}