package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.TeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for updating a team member's role.
 * Contains the new role to assign to the member.
 */
@Data
public class UpdateMemberRoleRequest {

    @NotNull(message = "Role is required")
    private TeamRole role;
}