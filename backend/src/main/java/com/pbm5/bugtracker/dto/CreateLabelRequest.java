package com.pbm5.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateLabelRequest {

    @NotBlank(message = "Label name is required")
    @Size(min = 1, max = 50, message = "Label name must be between 1 and 50 characters")
    private String name;

    @NotBlank(message = "Label color is required")
    @Size(min = 1, max = 7, message = "Label color must be between 1 and 7 characters")
    private String color;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    public CreateLabelRequest() {}

    public CreateLabelRequest(String name, String color, String description) {
        this.name = name;
        this.color = color;
        this.description = description;
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
} 