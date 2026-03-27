package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing individual point transactions for audit trail.
 * Records all points earned, lost, and the reasons for each transaction.
 */
@Entity
@Table(name = "point_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransaction {

    @Id
    @UuidGenerator
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "points_credited", nullable = false)
    private Integer pointsCredited;

    @Column(name = "points_deducted", nullable = false)
    private Integer pointsDeducted;

    @Column(name = "reason", nullable = false, length = 50)
    private String reason;

    @Column(name = "bug_id")
    private Long bugId;

    @CreationTimestamp
    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt;

    public boolean isBugRelated() {
        return bugId != null;
    }

    public boolean isPointsAwarded() {
        return pointsCredited > 0;
    }

    public boolean isPointsDeducted() {
        return pointsDeducted > 0;
    }

    public int getNetPoints() {
        return pointsCredited - pointsDeducted;
    }

    public TransactionReason getReasonAsEnum() {
        return TransactionReason.fromValue(this.reason);
    }

    public void setReasonFromEnum(TransactionReason reason) {
        this.reason = reason.getValue();
    }
}