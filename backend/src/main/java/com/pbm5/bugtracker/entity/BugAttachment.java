package com.pbm5.bugtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bug_attachments")
public class BugAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bug_id", nullable = false)
    @NotNull(message = "Bug is required")
    private Bug bug;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename must not exceed 255 characters")
    private String filename;

    @Column(name = "original_filename", nullable = false, length = 255)
    @NotBlank(message = "Original filename is required")
    @Size(max = 255, message = "Original filename must not exceed 255 characters")
    private String originalFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    @NotBlank(message = "File path is required")
    @Size(max = 500, message = "File path must not exceed 500 characters")
    private String filePath;

    @Column(name = "file_size", nullable = false)
    @NotNull(message = "File size is required")
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    @NotBlank(message = "MIME type is required")
    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    private String mimeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    @NotNull(message = "Uploader is required")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public BugAttachment() {
    }

    public BugAttachment(String filename, String originalFilename, String filePath, Long fileSize, String mimeType,
            User uploadedBy) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.uploadedBy = uploadedBy;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Business Logic Methods

    /**
     * Check if this is an image file
     */
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Check if this is a document file
     */
    public boolean isDocument() {
        if (mimeType == null)
            return false;
        return mimeType.startsWith("application/pdf") ||
                mimeType.startsWith("application/msword") ||
                mimeType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                mimeType.startsWith("application/vnd.ms-excel") ||
                mimeType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                mimeType.startsWith("text/");
    }

    /**
     * Check if this is a video file
     */
    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * Check if this is an archive file
     */
    public boolean isArchive() {
        if (mimeType == null)
            return false;
        return mimeType.equals("application/zip") ||
                mimeType.equals("application/x-rar-compressed") ||
                mimeType.equals("application/x-7z-compressed") ||
                mimeType.equals("application/gzip") ||
                mimeType.equals("application/x-tar");
    }

    /**
     * Check if this is a log file
     */
    public boolean isLogFile() {
        if (mimeType == null)
            return false;
        return mimeType.equals("text/plain") &&
                (originalFilename.endsWith(".log") ||
                        originalFilename.endsWith(".txt") ||
                        originalFilename.contains("log"));
    }

    /**
     * Get file size in human readable format
     */
    public String getFileSizeFormatted() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Check if file size is within acceptable limits
     */
    public boolean isSizeAcceptable() {
        // 10MB limit
        return fileSize <= 10 * 1024 * 1024;
    }

    /**
     * Get file extension
     */
    public String getFileExtension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BugAttachment that = (BugAttachment) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BugAttachment{" +
                "id=" + id +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", mimeType='" + mimeType + '\'' +
                ", bug=" + (bug != null ? bug.getId() : null) +
                '}';
    }
}