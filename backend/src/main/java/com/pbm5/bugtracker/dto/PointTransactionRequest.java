package com.pbm5.bugtracker.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for awarding points to a user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private UUID projectId;

    @NotNull(message = "Points credited are required")
    private Integer pointsCredited;

    @NotNull(message = "Points deducted are required")
    private Integer pointsDeducted;

    @NotNull(message = "Reason is required")
    private String reason;

    private Long bugId;
}
