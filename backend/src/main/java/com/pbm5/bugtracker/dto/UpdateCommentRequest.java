package com.pbm5.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating an existing comment on a bug
 */
public class UpdateCommentRequest {

    @NotBlank(message = "Comment content is required")
    @Size(min = 1, max = 5000, message = "Comment content must be between 1 and 5000 characters")
    private String content;

    // Default constructor
    public UpdateCommentRequest() {
    }

    // Constructor with fields
    public UpdateCommentRequest(String content) {
        this.content = content;
    }

    // Getters and setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
} 