package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.DuplicateDetectionMethod;
import java.util.Map;

/**
 * Response DTO for duplicate analytics of a project.
 * 
 * This DTO contains comprehensive analytics about duplicate bugs in a project,
 * including counts by detection method and by user who marked them.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public class DuplicateAnalyticsResponse {

    private long totalDuplicates;
    private Map<DuplicateDetectionMethod, Long> duplicatesByDetectionMethod;
    private Map<String, Long> duplicatesByUser;

    // Constructors
    public DuplicateAnalyticsResponse() {
    }

    public DuplicateAnalyticsResponse(long totalDuplicates,
            Map<DuplicateDetectionMethod, Long> duplicatesByDetectionMethod,
            Map<String, Long> duplicatesByUser) {
        this.totalDuplicates = totalDuplicates;
        this.duplicatesByDetectionMethod = duplicatesByDetectionMethod;
        this.duplicatesByUser = duplicatesByUser;
    }

    // Getters and Setters
    public long getTotalDuplicates() {
        return totalDuplicates;
    }

    public void setTotalDuplicates(long totalDuplicates) {
        this.totalDuplicates = totalDuplicates;
    }

    public Map<DuplicateDetectionMethod, Long> getDuplicatesByDetectionMethod() {
        return duplicatesByDetectionMethod;
    }

    public void setDuplicatesByDetectionMethod(Map<DuplicateDetectionMethod, Long> duplicatesByDetectionMethod) {
        this.duplicatesByDetectionMethod = duplicatesByDetectionMethod;
    }

    public Map<String, Long> getDuplicatesByUser() {
        return duplicatesByUser;
    }

    public void setDuplicatesByUser(Map<String, Long> duplicatesByUser) {
        this.duplicatesByUser = duplicatesByUser;
    }

    // Utility Methods
    public boolean hasDuplicates() {
        return totalDuplicates > 0;
    }

    public long getManualDuplicates() {
        return duplicatesByDetectionMethod != null
                ? duplicatesByDetectionMethod.getOrDefault(DuplicateDetectionMethod.MANUAL, 0L)
                : 0L;
    }

    public long getAutomaticDuplicates() {
        return duplicatesByDetectionMethod != null
                ? duplicatesByDetectionMethod.getOrDefault(DuplicateDetectionMethod.AUTOMATIC, 0L)
                : 0L;
    }

    public long getHybridDuplicates() {
        return duplicatesByDetectionMethod != null
                ? duplicatesByDetectionMethod.getOrDefault(DuplicateDetectionMethod.HYBRID, 0L)
                : 0L;
    }

    public boolean hasDetectionMethodBreakdown() {
        return duplicatesByDetectionMethod != null && !duplicatesByDetectionMethod.isEmpty();
    }

    public boolean hasUserBreakdown() {
        return duplicatesByUser != null && !duplicatesByUser.isEmpty();
    }

    @Override
    public String toString() {
        return "DuplicateAnalyticsResponse{" +
                "totalDuplicates=" + totalDuplicates +
                ", duplicatesByDetectionMethod=" + duplicatesByDetectionMethod +
                ", duplicatesByUser=" + duplicatesByUser +
                '}';
    }
}
