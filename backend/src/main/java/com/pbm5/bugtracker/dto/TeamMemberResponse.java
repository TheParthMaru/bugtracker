package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.pbm5.bugtracker.entity.TeamRole;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for team member response.
 * Contains member information with user details.
 */
@Data
public class TeamMemberResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private TeamRole role;
    private LocalDateTime joinedAt;
    private UUID addedBy;
    private String addedByName;

    public TeamMemberResponse(UUID id, UUID userId, String firstName, String lastName,
            String email, TeamRole role, LocalDateTime joinedAt,
            UUID addedBy, String addedByName) {
        this.id = id;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.joinedAt = joinedAt;
        this.addedBy = addedBy;
        this.addedByName = addedByName;
    }
}