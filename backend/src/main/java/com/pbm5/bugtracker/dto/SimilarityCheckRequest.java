package com.pbm5.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for checking bug similarity.
 * 
 * This DTO is used when checking for potential duplicate bugs
 * before creating a new bug report. Contains the title and description
 * that will be compared against existing bugs in the project.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public class SimilarityCheckRequest {

    @NotBlank(message = "Title is required for similarity checking")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @NotBlank(message = "Description is required for similarity checking")
    @Size(min = 10, message = "Description must be at least 10 characters for meaningful similarity analysis")
    private String description;

    // Project ID is set by the controller after validation
    private UUID projectId;

    // Optional parameters for fine-tuning similarity detection
    private Double similarityThreshold; // Override default threshold
    private Integer maxResults = 10; // Maximum number of similar bugs to return
    private Boolean includeClosedBugs = false; // Whether to include closed bugs in search

    // Constructors
    public SimilarityCheckRequest() {
    }

    public SimilarityCheckRequest(String title, String description, UUID projectId) {
        this.title = title;
        this.description = description;
        this.projectId = projectId;
    }

    public SimilarityCheckRequest(String title, String description, UUID projectId, Double similarityThreshold) {
        this(title, description, projectId);
        this.similarityThreshold = similarityThreshold;
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

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public Boolean getIncludeClosedBugs() {
        return includeClosedBugs;
    }

    public void setIncludeClosedBugs(Boolean includeClosedBugs) {
        this.includeClosedBugs = includeClosedBugs;
    }

    // Utility Methods

    /**
     * Get combined text for similarity analysis
     */
    public String getCombinedText() {
        return (title != null ? title : "") + " " + (description != null ? description : "");
    }

    /**
     * Check if request has minimum required content for similarity analysis
     */
    public boolean hasMinimumContent() {
        return title != null && !title.trim().isEmpty() &&
                description != null && description.length() >= 10;
    }

    /**
     * Get effective similarity threshold (use provided or default)
     */
    public double getEffectiveSimilarityThreshold() {
        return similarityThreshold != null ? similarityThreshold : 0.75;
    }

    /**
     * Get effective max results (with bounds checking)
     */
    public int getEffectiveMaxResults() {
        if (maxResults == null || maxResults <= 0) {
            return 10; // default
        }
        return Math.min(maxResults, 50); // cap at 50 to prevent performance issues
    }

    @Override
    public String toString() {
        return "SimilarityCheckRequest{" +
                "title='" + (title != null ? title.substring(0, Math.min(title.length(), 50)) + "..." : null) + '\'' +
                ", description='"
                + (description != null ? description.substring(0, Math.min(description.length(), 100)) + "..." : null)
                + '\'' +
                ", projectId=" + projectId +
                ", similarityThreshold=" + similarityThreshold +
                ", maxResults=" + maxResults +
                ", includeClosedBugs=" + includeClosedBugs +
                '}';
    }
}