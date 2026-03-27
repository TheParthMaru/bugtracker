package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.BugLabel;

import java.time.LocalDateTime;

public class BugLabelResponse {

    private Long id;
    private String name;
    private String color;
    private String description;
    private boolean isSystem;
    private LocalDateTime createdAt;

    // Constructors
    public BugLabelResponse() {
    }

    public BugLabelResponse(BugLabel label) {
        this.id = label.getId();
        this.name = label.getName();
        this.color = label.getColor();
        this.description = label.getDescription();
        this.isSystem = label.isSystem();
        this.createdAt = label.getCreatedAt();
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
}