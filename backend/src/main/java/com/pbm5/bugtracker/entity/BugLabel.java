package com.pbm5.bugtracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bug_labels")
public class BugLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    @NotBlank(message = "Label name is required")
    @Size(min = 1, max = 100, message = "Label name must be between 1 and 100 characters")
    private String name;

    @Column(length = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color code")
    private String color = "#3B82F6";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToMany(mappedBy = "labels", cascade = CascadeType.PERSIST)
    private Set<Bug> bugs = new HashSet<>();

    // Constructors
    public BugLabel() {
    }

    public BugLabel(String name, String color, String description) {
        this.name = name;
        this.color = color != null ? color : "#3B82F6";
        this.description = description;
        this.isSystem = false;
    }

    public BugLabel(String name, String color, String description, boolean isSystem) {
        this.name = name;
        this.color = color != null ? color : "#3B82F6";
        this.description = description;
        this.isSystem = isSystem;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<Bug> getBugs() {
        return bugs;
    }

    public void setBugs(Set<Bug> bugs) {
        this.bugs = bugs;
    }

    // Business Logic Methods

    /**
     * Check if this is a system label
     */
    public boolean isSystemLabel() {
        return isSystem;
    }

    /**
     * Check if this label can be deleted
     */
    public boolean canBeDeleted() {
        return !isSystem;
    }

    /**
     * Check if this label can be modified
     */
    public boolean canBeModified() {
        return !isSystem;
    }

    /**
     * Get the number of bugs using this label
     */
    public int getBugCount() {
        return bugs.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BugLabel bugLabel = (BugLabel) o;
        return id != null && id.equals(bugLabel.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BugLabel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", isSystem=" + isSystem +
                '}';
    }
}