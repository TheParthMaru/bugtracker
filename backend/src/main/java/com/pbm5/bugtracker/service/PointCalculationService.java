package com.pbm5.bugtracker.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.dto.PointTransactionRequest;
import com.pbm5.bugtracker.dto.PointTransactionResponse;
import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.PointTransaction;
import com.pbm5.bugtracker.entity.PointValue;
import com.pbm5.bugtracker.exception.PointCalculationException;
import com.pbm5.bugtracker.repository.PointTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating and awarding points based on various activities
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PointCalculationService {

    private final PointTransactionRepository pointTransactionRepository;

    /**
     * Calculate points based on bug priority
     */
    public int calculateBugResolutionPoints(Bug bug) {
        if (bug == null || bug.getPriority() == null) {
            throw new PointCalculationException("Bug or bug priority cannot be null");
        }

        return switch (bug.getPriority()) {
            case CRASH -> PointValue.BUG_RESOLUTION_CRASH.getPoints();
            case CRITICAL -> PointValue.BUG_RESOLUTION_CRITICAL.getPoints();
            case HIGH -> PointValue.BUG_RESOLUTION_HIGH.getPoints();
            case MEDIUM -> PointValue.BUG_RESOLUTION_MEDIUM.getPoints();
            case LOW -> PointValue.BUG_RESOLUTION_LOW.getPoints();
        };
    }

    /**
     * Calculate daily login points (always +1)
     */
    public int calculateDailyLoginPoints() {
        return PointValue.DAILY_LOGIN.getPoints();
    }

    /**
     * Apply penalty points for bug reopened (always -10)
     */
    public int applyBugReopenedPenalty() {
        return -PointValue.BUG_REOPENED_PENALTY.getPoints();
    }

    /**
     * Create a point transaction record
     */
    public PointTransactionResponse createPointTransaction(PointTransactionRequest request, UUID awardedBy) {
        validatePointCalculation(request.getPointsCredited(), request.getPointsDeducted(), request.getReason());

        PointTransaction transaction = PointTransaction.builder()
                .userId(request.getUserId())
                .projectId(request.getProjectId())
                .pointsCredited(request.getPointsCredited())
                .pointsDeducted(request.getPointsDeducted())
                .reason(request.getReason())
                .bugId(request.getBugId())
                .build();

        PointTransaction savedTransaction = pointTransactionRepository.save(transaction);

        log.info("Created point transaction: {} credited, {} deducted for user {} for reason: {}",
                request.getPointsCredited(), request.getPointsDeducted(), request.getUserId(), request.getReason());

        return mapToResponse(savedTransaction);
    }

    /**
     * Find daily login transactions for a specific user and date
     */
    public List<PointTransaction> findDailyLoginTransactionsForDate(UUID userId, LocalDate date) {
        return pointTransactionRepository.findDailyLoginTransactionsForDate(userId, date);
    }

    /**
     * Find transactions by reason for a specific user
     */
    public List<PointTransaction> findTransactionsByReason(UUID userId, String reason) {
        return pointTransactionRepository.findByUserIdAndReason(userId, reason);
    }

    /**
     * Validate point calculation
     */
    public boolean validatePointCalculation(int pointsCredited, int pointsDeducted, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new PointCalculationException("Reason cannot be null or empty");
        }

        if (reason.trim().length() < 3) {
            throw new PointCalculationException("Reason must be at least 3 characters long");
        }

        if (pointsCredited == 0 && pointsDeducted == 0) {
            throw new PointCalculationException("At least one of points credited or deducted must be non-zero");
        }

        if (pointsCredited > 0 && pointsDeducted > 0) {
            throw new PointCalculationException(
                    "Cannot have both points credited and deducted in the same transaction");
        }

        // Validate specific point values based on reason
        if (reason.contains("bug-resolution")) {
            if (!isValidBugResolutionPoints(pointsCredited)) {
                throw new PointCalculationException("Invalid points for bug resolution: " + pointsCredited);
            }
        } else if (reason.contains("daily-login")) {
            if (pointsCredited != PointValue.DAILY_LOGIN.getPoints()) {
                throw new PointCalculationException("Daily login should award exactly "
                        + PointValue.DAILY_LOGIN.getPoints() + " point, got: " + pointsCredited);
            }
        } else if (reason.contains("bug-reopened")) {
            if (pointsDeducted != PointValue.BUG_REOPENED_PENALTY.getPoints()) {
                throw new PointCalculationException(
                        "Bug reopened should deduct exactly " + PointValue.BUG_REOPENED_PENALTY.getPoints()
                                + " points, got: " + pointsDeducted);
            }
        }

        return true;
    }

    /**
     * Check if points are valid for bug resolution
     */
    private boolean isValidBugResolutionPoints(int points) {
        return points == PointValue.BUG_RESOLUTION_LOW.getPoints() ||
                points == PointValue.BUG_RESOLUTION_MEDIUM.getPoints() ||
                points == PointValue.BUG_RESOLUTION_HIGH.getPoints() ||
                points == PointValue.BUG_RESOLUTION_CRITICAL.getPoints() ||
                points == PointValue.BUG_RESOLUTION_CRASH.getPoints();
    }

    /**
     * Map entity to response DTO
     */
    private PointTransactionResponse mapToResponse(PointTransaction transaction) {
        return PointTransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .projectId(transaction.getProjectId())
                .points(transaction.getNetPoints())
                .pointsCredited(transaction.getPointsCredited())
                .pointsDeducted(transaction.getPointsDeducted())
                .reason(transaction.getReason())
                .bugId(transaction.getBugId())
                .earnedAt(transaction.getEarnedAt())
                .build();
    }
}
