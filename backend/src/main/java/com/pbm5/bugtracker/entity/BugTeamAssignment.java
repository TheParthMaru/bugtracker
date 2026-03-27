package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity representing the assignment of a bug to a team.
 * This allows tracking which teams are responsible for which bugs,
 * supporting the auto-assignment feature.
 */
@Entity
@Table(name = "bug_team_assignments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BugTeamAssignment {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bug_id", nullable = false)
    @JsonIgnore
    private Bug bug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @JsonIgnore
    private Team team;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    @JsonIgnore
    private User assignedBy;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    // Utility methods

    /**
     * Check if this assignment is for a specific bug
     */
    public boolean isForBug(Long bugId) {
        return bug != null && Objects.equals(bug.getId(), bugId);
    }

    /**
     * Check if this assignment is for a specific team
     */
    public boolean isForTeam(UUID teamId) {
        return team != null && Objects.equals(team.getId(), teamId);
    }

    /**
     * Check if this assignment was made by a specific user
     */
    public boolean wasAssignedBy(UUID userId) {
        return assignedBy != null && Objects.equals(assignedBy.getId(), userId);
    }

    /**
     * Get the bug ID for this assignment
     */
    public Long getBugId() {
        return bug != null ? bug.getId() : null;
    }

    /**
     * Get the team ID for this assignment
     */
    public UUID getTeamId() {
        return team != null ? team.getId() : null;
    }

    /**
     * Get the assigned by user ID
     */
    public UUID getAssignedById() {
        return assignedBy != null ? assignedBy.getId() : null;
    }

    /**
     * Get a human-readable description of this assignment
     */
    public String getAssignmentDescription() {
        if (bug == null || team == null) {
            return "Invalid assignment";
        }
        
        String teamName = team.getName();
        String bugTitle = bug.getTitle();
        
        if (isPrimary) {
            return String.format("Primary assignment: %s → %s", bugTitle, teamName);
        } else {
            return String.format("Secondary assignment: %s → %s", bugTitle, teamName);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BugTeamAssignment)) return false;
        
        BugTeamAssignment that = (BugTeamAssignment) o;
        
        if (bug != null && that.bug != null) {
            return Objects.equals(bug.getId(), that.bug.getId()) &&
                   Objects.equals(team.getId(), that.team.getId());
        }
        
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        if (bug != null && team != null) {
            return Objects.hash(bug.getId(), team.getId());
        }
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BugTeamAssignment{" +
                "id=" + id +
                ", bugId=" + getBugId() +
                ", teamId=" + getTeamId() +
                ", assignedAt=" + assignedAt +
                ", assignedById=" + getAssignedById() +
                ", isPrimary=" + isPrimary +
                '}';
    }
} 