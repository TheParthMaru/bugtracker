package com.pbm5.bugtracker.dto;

import java.util.UUID;

import com.pbm5.bugtracker.entity.TeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for adding a member to a team.
 * Contains user ID and optional role specification.
 */
@Data
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private TeamRole role = TeamRole.MEMBER;
}