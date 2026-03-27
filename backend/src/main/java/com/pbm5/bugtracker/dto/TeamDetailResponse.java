package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.pbm5.bugtracker.entity.TeamRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data Transfer Object (DTO) for detailed team response.
 * Extends TeamResponse and includes the complete member list.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TeamDetailResponse extends TeamResponse {

    private List<TeamMemberResponse> members;

    public TeamDetailResponse(UUID id, String name, String description, String teamSlug,
            UUID projectId, String projectSlug, UUID createdBy, String creatorName,
            LocalDateTime createdAt, LocalDateTime updatedAt, Integer memberCount,
            TeamRole currentUserRole, Boolean canManage,
            List<TeamMemberResponse> members) {
        super(id, name, description, teamSlug, projectId, projectSlug, createdBy, creatorName,
                createdAt, updatedAt, memberCount, currentUserRole, canManage);
        this.members = members;
    }
}