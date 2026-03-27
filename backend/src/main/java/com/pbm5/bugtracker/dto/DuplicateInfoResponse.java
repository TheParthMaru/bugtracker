package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.DuplicateDetectionMethod;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for duplicate information of a bug.
 * 
 * This DTO contains comprehensive information about a bug's duplicate status,
 * including the original bug, relationship details, and other duplicates
 * of the same original bug.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public class DuplicateInfoResponse {

    private boolean isDuplicate;
    private BugSummaryResponse originalBug;
    private DuplicateRelationshipInfo relationshipInfo;
    private List<BugSummaryResponse> otherDuplicates;

    // Constructors
    public DuplicateInfoResponse() {
    }

    public DuplicateInfoResponse(boolean isDuplicate) {
        this.isDuplicate = isDuplicate;
    }

    public DuplicateInfoResponse(boolean isDuplicate, BugSummaryResponse originalBug,
            DuplicateRelationshipInfo relationshipInfo,
            List<BugSummaryResponse> otherDuplicates) {
        this.isDuplicate = isDuplicate;
        this.originalBug = originalBug;
        this.relationshipInfo = relationshipInfo;
        this.otherDuplicates = otherDuplicates;
    }

    // Getters and Setters
    public boolean isDuplicate() {
        return isDuplicate;
    }

    public void setDuplicate(boolean duplicate) {
        isDuplicate = duplicate;
    }

    public BugSummaryResponse getOriginalBug() {
        return originalBug;
    }

    public void setOriginalBug(BugSummaryResponse originalBug) {
        this.originalBug = originalBug;
    }

    public DuplicateRelationshipInfo getRelationshipInfo() {
        return relationshipInfo;
    }

    public void setRelationshipInfo(DuplicateRelationshipInfo relationshipInfo) {
        this.relationshipInfo = relationshipInfo;
    }

    public List<BugSummaryResponse> getOtherDuplicates() {
        return otherDuplicates;
    }

    public void setOtherDuplicates(List<BugSummaryResponse> otherDuplicates) {
        this.otherDuplicates = otherDuplicates;
    }

    // Utility Methods
    public boolean hasOriginalBug() {
        return originalBug != null;
    }

    public boolean hasOtherDuplicates() {
        return otherDuplicates != null && !otherDuplicates.isEmpty();
    }

    public int getOtherDuplicatesCount() {
        return otherDuplicates != null ? otherDuplicates.size() : 0;
    }

    @Override
    public String toString() {
        return "DuplicateInfoResponse{" +
                "isDuplicate=" + isDuplicate +
                ", originalBug=" + (originalBug != null ? originalBug.getId() : null) +
                ", hasRelationshipInfo=" + (relationshipInfo != null) +
                ", otherDuplicatesCount=" + getOtherDuplicatesCount() +
                '}';
    }
}
