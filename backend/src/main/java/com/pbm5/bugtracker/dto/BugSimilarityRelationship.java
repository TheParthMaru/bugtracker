package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.BugPriority;
import com.pbm5.bugtracker.entity.BugStatus;
import com.pbm5.bugtracker.entity.SimilarityAlgorithm;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for bug similarity relationships.
 * 
 * This DTO represents a relationship between two bugs that are similar,
 * including information about both bugs and their similarity score.
 * Used in API responses for similarity analysis features.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
public class BugSimilarityRelationship {

    // Bug A information (the source bug)
    private Long bugAId;
    private Integer bugAProjectTicketNumber;
    private String bugATitle;
    private String bugADescription;
    private BugStatus bugAStatus;
    private BugPriority bugAPriority;
    private String bugAAssigneeName;
    private String bugAReporterName;
    private LocalDateTime bugACreatedAt;
    private LocalDateTime bugAUpdatedAt;

    // Bug B information (the similar bug)
    private Long bugBId;
    private Integer bugBProjectTicketNumber;
    private String bugBTitle;
    private String bugBDescription;
    private BugStatus bugBStatus;
    private BugPriority bugBPriority;
    private String bugBAssigneeName;
    private String bugBReporterName;
    private LocalDateTime bugBCreatedAt;
    private LocalDateTime bugBUpdatedAt;

    // Similarity information
    private double similarityScore;
    private Map<SimilarityAlgorithm, Double> algorithmScores;
    private String textFingerprint;
    private boolean isAlreadyMarkedDuplicate;
    private Long originalBugId; // If this relationship is already marked as duplicate

    // Constructors
    public BugSimilarityRelationship() {
    }

    // Getters and Setters for Bug A
    public Long getBugAId() {
        return bugAId;
    }

    public void setBugAId(Long bugAId) {
        this.bugAId = bugAId;
    }

    public Integer getBugAProjectTicketNumber() {
        return bugAProjectTicketNumber;
    }

    public void setBugAProjectTicketNumber(Integer bugAProjectTicketNumber) {
        this.bugAProjectTicketNumber = bugAProjectTicketNumber;
    }

    public String getBugATitle() {
        return bugATitle;
    }

    public void setBugATitle(String bugATitle) {
        this.bugATitle = bugATitle;
    }

    public String getBugADescription() {
        return bugADescription;
    }

    public void setBugADescription(String bugADescription) {
        this.bugADescription = bugADescription;
    }

    public BugStatus getBugAStatus() {
        return bugAStatus;
    }

    public void setBugAStatus(BugStatus bugAStatus) {
        this.bugAStatus = bugAStatus;
    }

    public BugPriority getBugAPriority() {
        return bugAPriority;
    }

    public void setBugAPriority(BugPriority bugAPriority) {
        this.bugAPriority = bugAPriority;
    }

    public String getBugAAssigneeName() {
        return bugAAssigneeName;
    }

    public void setBugAAssigneeName(String bugAAssigneeName) {
        this.bugAAssigneeName = bugAAssigneeName;
    }

    public String getBugAReporterName() {
        return bugAReporterName;
    }

    public void setBugAReporterName(String bugAReporterName) {
        this.bugAReporterName = bugAReporterName;
    }

    public LocalDateTime getBugACreatedAt() {
        return bugACreatedAt;
    }

    public void setBugACreatedAt(LocalDateTime bugACreatedAt) {
        this.bugACreatedAt = bugACreatedAt;
    }

    public LocalDateTime getBugAUpdatedAt() {
        return bugAUpdatedAt;
    }

    public void setBugAUpdatedAt(LocalDateTime bugAUpdatedAt) {
        this.bugAUpdatedAt = bugAUpdatedAt;
    }

    // Getters and Setters for Bug B
    public Long getBugBId() {
        return bugBId;
    }

    public void setBugBId(Long bugBId) {
        this.bugBId = bugBId;
    }

    public Integer getBugBProjectTicketNumber() {
        return bugBProjectTicketNumber;
    }

    public void setBugBProjectTicketNumber(Integer bugBProjectTicketNumber) {
        this.bugBProjectTicketNumber = bugBProjectTicketNumber;
    }

    public String getBugBTitle() {
        return bugBTitle;
    }

    public void setBugBTitle(String bugBTitle) {
        this.bugBTitle = bugBTitle;
    }

    public String getBugBDescription() {
        return bugBDescription;
    }

    public void setBugBDescription(String bugBDescription) {
        this.bugBDescription = bugBDescription;
    }

    public BugStatus getBugBStatus() {
        return bugBStatus;
    }

    public void setBugBStatus(BugStatus bugBStatus) {
        this.bugBStatus = bugBStatus;
    }

    public BugPriority getBugBPriority() {
        return bugBPriority;
    }

    public void setBugBPriority(BugPriority bugBPriority) {
        this.bugBPriority = bugBPriority;
    }

    public String getBugBAssigneeName() {
        return bugBAssigneeName;
    }

    public void setBugBAssigneeName(String bugBAssigneeName) {
        this.bugBAssigneeName = bugBAssigneeName;
    }

    public String getBugBReporterName() {
        return bugBReporterName;
    }

    public void setBugBReporterName(String bugBReporterName) {
        this.bugBReporterName = bugBReporterName;
    }

    public LocalDateTime getBugBCreatedAt() {
        return bugBCreatedAt;
    }

    public void setBugBCreatedAt(LocalDateTime bugBCreatedAt) {
        this.bugBCreatedAt = bugBCreatedAt;
    }

    public LocalDateTime getBugBUpdatedAt() {
        return bugBUpdatedAt;
    }

    public void setBugBUpdatedAt(LocalDateTime bugBUpdatedAt) {
        this.bugBUpdatedAt = bugBUpdatedAt;
    }

    // Getters and Setters for Similarity
    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public Map<SimilarityAlgorithm, Double> getAlgorithmScores() {
        return algorithmScores;
    }

    public void setAlgorithmScores(Map<SimilarityAlgorithm, Double> algorithmScores) {
        this.algorithmScores = algorithmScores;
    }

    public String getTextFingerprint() {
        return textFingerprint;
    }

    public void setTextFingerprint(String textFingerprint) {
        this.textFingerprint = textFingerprint;
    }

    public boolean isAlreadyMarkedDuplicate() {
        return isAlreadyMarkedDuplicate;
    }

    public void setAlreadyMarkedDuplicate(boolean alreadyMarkedDuplicate) {
        isAlreadyMarkedDuplicate = alreadyMarkedDuplicate;
    }

    public Long getOriginalBugId() {
        return originalBugId;
    }

    public void setOriginalBugId(Long originalBugId) {
        this.originalBugId = originalBugId;
    }
}
