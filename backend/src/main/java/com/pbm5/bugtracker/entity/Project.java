package com.pbm5.bugtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA Entity representing a Project in the bug tracker system.
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "projectMembers" })
@Slf4j
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonProperty("id")
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    @JsonProperty("name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Project description cannot exceed 2000 characters")
    @JsonProperty("description")
    private String description;

    @Column(name = "project_slug", nullable = false, unique = true, length = 120)
    @NotBlank(message = "Project slug is required")
    @Size(min = 3, max = 120, message = "Project slug must be between 3 and 120 characters")
    @JsonProperty("projectSlug")
    private String projectSlug;

    @Column(name = "admin_id", nullable = false)
    @NotNull(message = "Project admin is required")
    @JsonProperty("adminId")
    private UUID adminId;

    @Column(name = "is_active", nullable = false)
    @JsonProperty("isActive")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ProjectMember> projectMembers = new HashSet<>();

    // Business logic methods
    public static String generateProjectSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot generate slug from empty name");
        }

        log.debug("Generating project slug from name: {}", name);

        String projectSlug = name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s\\-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (projectSlug.length() < 3) {
            projectSlug = projectSlug + "-project";
        }

        log.debug("Generated projectSlug: {}", projectSlug);
        return projectSlug;
    }

    @PrePersist
    protected void prePersist() {
        log.debug("Pre-persist callback for project: {}", name);

        if (projectSlug == null || projectSlug.trim().isEmpty()) {
            this.projectSlug = generateProjectSlug(this.name);
            log.info("Auto-generated slug for project '{}': {}", name, projectSlug);
        }

        if (isActive == null) {
            isActive = true;
        }

        log.debug("Project pre-persist completed. ProjectSlug: {}, Active: {}", projectSlug, isActive);
    }

    @PreUpdate
    protected void preUpdate() {
        log.debug("Pre-update callback for project: {}", name);
        log.debug("Project pre-update completed");
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    public void softDelete() {
        log.info("Soft deleting project: {} ({})", name, id);
        this.isActive = false;
    }

    public void restore() {
        log.info("Restoring project: {} ({})", name, id);
        this.isActive = true;
    }

    public void addMember(ProjectMember member) {
        if (member != null) {
            log.debug("Adding member to project {}: user {}", id, member.getUserId());
            projectMembers.add(member);
            member.setProject(this);
        }
    }

    public void removeMember(ProjectMember member) {
        if (member != null) {
            log.debug("Removing member from project {}: user {}", id, member.getUserId());
            projectMembers.remove(member);
            member.setProject(null);
        }
    }

    public long getActiveMemberCount() {
        return projectMembers.stream()
                .filter(member -> MemberStatus.ACTIVE.equals(member.getStatus()))
                .count();
    }

    public static boolean isValidProjectSlugFormat(String projectSlug) {
        if (projectSlug == null || projectSlug.trim().isEmpty()) {
            return false;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
        return pattern.matcher(projectSlug.trim()).matches();
    }
}