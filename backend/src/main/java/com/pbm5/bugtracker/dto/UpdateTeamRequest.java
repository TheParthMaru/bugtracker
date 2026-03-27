package com.pbm5.bugtracker.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for updating an existing team.
 * All fields are optional to support partial updates.
 * Only non-null fields will be updated.
 */
@Data
public class UpdateTeamRequest {

    @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Team description cannot exceed 500 characters")
    private String description;

}