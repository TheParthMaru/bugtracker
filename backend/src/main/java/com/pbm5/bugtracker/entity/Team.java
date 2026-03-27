package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "teams")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Team {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Team name cannot be blank")
    @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    private String name;

    @Column(columnDefinition = "TEXT")
    @Size(max = 500, message = "Team description cannot exceed 500 characters")
    private String description;

    @Column(name = "team_slug", nullable = false, length = 200) // Increased for project-slug prefix
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Team slug must contain only lowercase letters, numbers, and hyphens")
    private String teamSlug;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private Set<TeamMember> members = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    @JsonIgnore
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @JsonIgnore
    private User creator;

    // Utility methods
    public void addMember(TeamMember member) {
        members.add(member);
        member.setTeam(this);
    }

    public void removeMember(TeamMember member) {
        members.remove(member);
        member.setTeam(null);
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean hasMember(UUID userId) {
        return members.stream()
                .anyMatch(member -> member.getUserId().equals(userId));
    }

    public TeamRole getUserRole(UUID userId) {
        return members.stream()
                .filter(member -> member.getUserId().equals(userId))
                .findFirst()
                .map(TeamMember::getRole)
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Team))
            return false;
        Team team = (Team) o;
        return Objects.equals(id, team.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Business logic methods
    public static String generateTeamSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot generate team slug from empty name");
        }

        String teamSlug = name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s\\-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (teamSlug.length() < 2) {
            teamSlug = teamSlug + "-team";
        }

        return teamSlug;
    }

    public static boolean isValidTeamSlugFormat(String teamSlug) {
        if (teamSlug == null || teamSlug.trim().isEmpty()) {
            return false;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[a-z0-9-]+$");
        return pattern.matcher(teamSlug.trim()).matches();
    }

    @PrePersist
    protected void prePersist() {
        if (teamSlug == null || teamSlug.trim().isEmpty()) {
            this.teamSlug = generateTeamSlug(this.name);
        }
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", teamSlug='" + teamSlug + '\'' +
                ", memberCount=" + getMemberCount() +
                '}';
    }
}