package com.pbm5.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new comment on a bug
 */
public class CreateCommentRequest {

    @NotBlank(message = "Comment content is required")
    @Size(min = 1, max = 5000, message = "Comment content must be between 1 and 5000 characters")
    private String content;

    private Long parentId; // For reply comments

    // Default constructor
    public CreateCommentRequest() {
    }

    // Constructor with fields
    public CreateCommentRequest(String content, Long parentId) {
        this.content = content;
        this.parentId = parentId;
    }

    // Getters and setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
} 