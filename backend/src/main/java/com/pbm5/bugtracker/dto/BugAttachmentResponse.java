package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.BugAttachment;
import com.pbm5.bugtracker.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public class BugAttachmentResponse {

    private Long id;
    private String filename;
    private String originalFilename;
    private long fileSize;
    private String mimeType;
    private UserResponse uploadedBy;
    private LocalDateTime createdAt;

    // Constructors
    public BugAttachmentResponse() {
    }

    public BugAttachmentResponse(BugAttachment attachment) {
        this.id = attachment.getId();
        this.filename = attachment.getFilename();
        this.originalFilename = attachment.getOriginalFilename();
        this.fileSize = attachment.getFileSize();
        this.mimeType = attachment.getMimeType();
        this.uploadedBy = new UserResponse(attachment.getUploadedBy());
        this.createdAt = attachment.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public UserResponse getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UserResponse uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}