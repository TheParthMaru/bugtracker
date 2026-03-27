package com.pbm5.bugtracker.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for point transaction details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionResponse {

    private UUID transactionId;
    private UUID userId;
    private UUID projectId;
    private Integer points;
    private Integer pointsCredited;
    private Integer pointsDeducted;
    private String reason;
    private Long bugId;
    private LocalDateTime earnedAt;
}
