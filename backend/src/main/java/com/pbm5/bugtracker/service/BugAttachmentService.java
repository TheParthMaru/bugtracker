package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.BugAttachment;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.exception.AttachmentNotFoundException;
import com.pbm5.bugtracker.exception.FileUploadException;
import com.pbm5.bugtracker.repository.BugAttachmentRepository;
import com.pbm5.bugtracker.repository.BugRepository;
import com.pbm5.bugtracker.repository.UserRepository;
import com.pbm5.bugtracker.service.BugNotificationEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BugAttachmentService {

    @Autowired
    private BugAttachmentRepository bugAttachmentRepository;

    @Autowired
    private BugRepository bugRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BugNotificationEventListener bugNotificationEventListener;

    @Value("${app.file.upload.path:/tmp/bugtracker/uploads}")
    private String uploadPath;

    @Value("${app.file.max-size:10485760}") // 10MB default
    private long maxFileSize;

    // File Upload Operations

    /**
     * Upload file attachment to bug
     */
    public BugAttachment uploadAttachment(UUID projectId, Long bugId, MultipartFile file, UUID uploadedById) {
        // Validate file
        validateFile(file);

        // Get bug and validate access
        Bug bug = bugRepository.findByProjectIdAndIdWithUsers(projectId, bugId)
                .orElseThrow(() -> new RuntimeException("Bug not found"));

        User uploadedBy = userRepository.findById(uploadedById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(fileExtension);

            // Create directory structure
            Path uploadDir = Paths.get(uploadPath, projectId.toString(), bugId.toString());
            Files.createDirectories(uploadDir);

            // Save file
            Path filePath = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            // Create attachment record
            BugAttachment attachment = new BugAttachment(
                    uniqueFilename,
                    originalFilename,
                    filePath.toString(),
                    file.getSize(),
                    file.getContentType(),
                    uploadedBy);
            attachment.setBug(bug);

            BugAttachment savedAttachment = bugAttachmentRepository.save(attachment);

            // Trigger notification for attachment addition (async - won't affect
            // transaction)
            try {
                bugNotificationEventListener.onBugAttachmentAdded(bug, savedAttachment, uploadedBy);
            } catch (Exception e) {
                // Log error but don't fail the upload if notification fails
                System.err.println("Failed to send attachment notification: " + e.getMessage());
            }

            return savedAttachment;

        } catch (IOException e) {
            throw new FileUploadException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Get attachment by ID
     */
    public BugAttachment getAttachmentById(UUID projectId, Long bugId, Long attachmentId) {
        BugAttachment attachment = bugAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found with id: " + attachmentId));

        // Verify the attachment belongs to the specified bug
        if (!attachment.getBug().getId().equals(bugId)) {
            throw new AttachmentNotFoundException("Attachment not found with id: " + attachmentId);
        }

        return attachment;
    }

    /**
     * Get all attachments for a bug
     */
    public List<BugAttachment> getAttachmentsByBugId(Long bugId) {
        return bugAttachmentRepository.findByBugIdOrderByCreatedAtDesc(bugId);
    }

    /**
     * Download attachment
     */
    public org.springframework.core.io.Resource downloadAttachment(UUID projectId, Long bugId, Long attachmentId) {
        BugAttachment attachment = getAttachmentById(projectId, bugId, attachmentId);

        try {
            Path filePath = Paths.get(attachment.getFilePath());
            System.out.println("DEBUG: Attempting to access file at path: " + filePath);
            System.out.println("DEBUG: File exists: " + Files.exists(filePath));
            System.out.println("DEBUG: File is readable: " + Files.isReadable(filePath));

            if (!Files.exists(filePath)) {
                System.out.println("DEBUG: File does not exist at path: " + filePath);
                throw new AttachmentNotFoundException("Attachment file not found on disk: " + attachment.getFilePath());
            }

            if (!Files.isReadable(filePath)) {
                System.out.println("DEBUG: File is not readable at path: " + filePath);
                throw new RuntimeException("Attachment file is not readable: " + attachment.getFilePath());
            }

            return new org.springframework.core.io.FileSystemResource(filePath.toFile());
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in downloadAttachment: " + e.getMessage());
            throw new RuntimeException("Failed to download attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Delete attachment
     */
    public void deleteAttachment(UUID projectId, Long bugId, Long attachmentId) {
        BugAttachment attachment = getAttachmentById(projectId, bugId, attachmentId);

        try {
            // Delete physical file
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);

            // Delete database record
            bugAttachmentRepository.delete(attachment);

        } catch (IOException e) {
            throw new RuntimeException("Failed to delete attachment file: " + e.getMessage(), e);
        }
    }

    // File Validation

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or null");
        }

        if (file.getSize() > maxFileSize) {
            throw new FileUploadException(
                    "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new FileUploadException("File content type is null");
        }

        // Validate file type
        if (!isAllowedFileType(contentType)) {
            throw new FileUploadException("File type not allowed: " + contentType);
        }
    }

    /**
     * Check if file type is allowed
     */
    private boolean isAllowedFileType(String contentType) {
        // Images
        if (contentType.startsWith("image/")) {
            return true;
        }

        // Documents
        if (contentType.equals("application/pdf") ||
                contentType.equals("application/msword") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equals("application/vnd.ms-excel") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                contentType.equals("text/plain") ||
                contentType.equals("text/csv")) {
            return true;
        }

        // Archives
        if (contentType.equals("application/zip") ||
                contentType.equals("application/x-rar-compressed") ||
                contentType.equals("application/x-7z-compressed") ||
                contentType.equals("application/gzip") ||
                contentType.equals("application/x-tar")) {
            return true;
        }

        // Logs
        if (contentType.equals("text/plain") &&
                (contentType.contains("log") || contentType.contains("txt"))) {
            return true;
        }

        return false;
    }

    // Utility Methods

    /**
     * Generate unique filename
     */
    private String generateUniqueFilename(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + "." + extension;
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    // Analytics

    /**
     * Get attachment statistics for project
     */
    public AttachmentStatistics getAttachmentStatistics(Long projectId) {
        long totalAttachments = bugAttachmentRepository.countByProjectId(projectId);
        long totalFileSize = bugAttachmentRepository.getTotalFileSizeByProjectId(projectId);
        List<Object[]> fileTypeDistribution = bugAttachmentRepository.getFileTypeDistributionByProjectId(projectId);

        return new AttachmentStatistics(totalAttachments, totalFileSize, fileTypeDistribution);
    }

    /**
     * Get attachment statistics for bug
     */
    public AttachmentStatistics getAttachmentStatisticsByBug(Long bugId) {
        long totalAttachments = bugAttachmentRepository.countByBugId(bugId);
        long totalFileSize = bugAttachmentRepository.getTotalFileSizeByBugId(bugId);
        List<Object[]> fileTypeDistribution = bugAttachmentRepository.getFileTypeDistributionByBugId(bugId);

        return new AttachmentStatistics(totalAttachments, totalFileSize, fileTypeDistribution);
    }

    /**
     * Get large files in project
     */
    public List<BugAttachment> getLargeFiles(Long projectId, long minSize) {
        return bugAttachmentRepository.findLargeFilesByProjectId(projectId, minSize);
    }

    /**
     * Get recent uploads
     */
    public List<BugAttachment> getRecentUploads(Long projectId) {
        return bugAttachmentRepository.findRecentUploadsByProjectId(projectId);
    }

    // Validation Methods

    /**
     * Check if attachment exists
     */
    public boolean attachmentExists(Long bugId, Long attachmentId) {
        return bugAttachmentRepository.existsByBugIdAndId(bugId, attachmentId);
    }

    /**
     * Check if filename already exists for bug
     */
    public boolean filenameExistsForBug(Long bugId, String originalFilename) {
        return bugAttachmentRepository.existsByBugIdAndOriginalFilename(bugId, originalFilename);
    }

    // Statistics DTO
    public static class AttachmentStatistics {
        private final long totalAttachments;
        private final long totalFileSize;
        private final List<Object[]> fileTypeDistribution;

        public AttachmentStatistics(long totalAttachments, long totalFileSize, List<Object[]> fileTypeDistribution) {
            this.totalAttachments = totalAttachments;
            this.totalFileSize = totalFileSize;
            this.fileTypeDistribution = fileTypeDistribution;
        }

        // Getters
        public long getTotalAttachments() {
            return totalAttachments;
        }

        public long getTotalFileSize() {
            return totalFileSize;
        }

        public List<Object[]> getFileTypeDistribution() {
            return fileTypeDistribution;
        }

        public String getTotalFileSizeFormatted() {
            if (totalFileSize < 1024) {
                return totalFileSize + " B";
            } else if (totalFileSize < 1024 * 1024) {
                return String.format("%.1f KB", totalFileSize / 1024.0);
            } else if (totalFileSize < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", totalFileSize / (1024.0 * 1024.0));
            } else {
                return String.format("%.1f GB", totalFileSize / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
}