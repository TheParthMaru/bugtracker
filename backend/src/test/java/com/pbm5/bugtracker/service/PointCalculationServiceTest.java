package com.pbm5.bugtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pbm5.bugtracker.dto.PointTransactionRequest;
import com.pbm5.bugtracker.dto.PointTransactionResponse;
import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.BugPriority;
import com.pbm5.bugtracker.entity.PointTransaction;
import com.pbm5.bugtracker.entity.PointValue;
import com.pbm5.bugtracker.exception.PointCalculationException;
import com.pbm5.bugtracker.repository.PointTransactionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointCalculationService Tests")
class PointCalculationServiceTest {

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @InjectMocks
    private PointCalculationService pointCalculationService;

    private UUID testUserId;
    private UUID testProjectId;
    private Long testBugId;
    private Bug testBug;
    private PointTransaction testTransaction;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testProjectId = UUID.randomUUID();
        testBugId = 1L;

        testBug = new Bug();
        testBug.setId(testBugId);
        testBug.setPriority(BugPriority.HIGH);

        testTransaction = PointTransaction.builder()
                .transactionId(UUID.randomUUID())
                .userId(testUserId)
                .projectId(testProjectId)
                .pointsCredited(50)
                .pointsDeducted(0)
                .reason("Bug Resolution - HIGH")
                .bugId(testBugId)
                .earnedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Point Calculation Tests")
    class PointCalculationTests {

        @Test
        @DisplayName("Should calculate CRASH bug resolution points")
        void testCalculateBugResolutionPoints_CRASH() {
            // Given
            testBug.setPriority(BugPriority.CRASH);

            // When
            int points = pointCalculationService.calculateBugResolutionPoints(testBug);

            // Then
            assertThat(points).isEqualTo(PointValue.BUG_RESOLUTION_CRASH.getPoints());
        }

        @Test
        @DisplayName("Should calculate CRITICAL bug resolution points")
        void testCalculateBugResolutionPoints_CRITICAL() {
            // Given
            testBug.setPriority(BugPriority.CRITICAL);

            // When
            int points = pointCalculationService.calculateBugResolutionPoints(testBug);

            // Then
            assertThat(points).isEqualTo(PointValue.BUG_RESOLUTION_CRITICAL.getPoints());
        }

        @Test
        @DisplayName("Should calculate HIGH bug resolution points")
        void testCalculateBugResolutionPoints_HIGH() {
            // Given
            testBug.setPriority(BugPriority.HIGH);

            // When
            int points = pointCalculationService.calculateBugResolutionPoints(testBug);

            // Then
            assertThat(points).isEqualTo(PointValue.BUG_RESOLUTION_HIGH.getPoints());
        }

        @Test
        @DisplayName("Should calculate MEDIUM bug resolution points")
        void testCalculateBugResolutionPoints_MEDIUM() {
            // Given
            testBug.setPriority(BugPriority.MEDIUM);

            // When
            int points = pointCalculationService.calculateBugResolutionPoints(testBug);

            // Then
            assertThat(points).isEqualTo(PointValue.BUG_RESOLUTION_MEDIUM.getPoints());
        }

        @Test
        @DisplayName("Should calculate LOW bug resolution points")
        void testCalculateBugResolutionPoints_LOW() {
            // Given
            testBug.setPriority(BugPriority.LOW);

            // When
            int points = pointCalculationService.calculateBugResolutionPoints(testBug);

            // Then
            assertThat(points).isEqualTo(PointValue.BUG_RESOLUTION_LOW.getPoints());
        }

        @Test
        @DisplayName("Should throw exception for null bug")
        void testCalculateBugResolutionPoints_NullBug() {
            // When & Then
            assertThatThrownBy(() -> pointCalculationService.calculateBugResolutionPoints(null))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("Bug or bug priority cannot be null");
        }

        @Test
        @DisplayName("Should throw exception for null priority")
        void testCalculateBugResolutionPoints_NullPriority() {
            // Given
            testBug.setPriority(null);

            // When & Then
            assertThatThrownBy(() -> pointCalculationService.calculateBugResolutionPoints(testBug))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("Bug or bug priority cannot be null");
        }

        @Test
        @DisplayName("Should calculate daily login points")
        void testCalculateDailyLoginPoints() {
            // When
            int points = pointCalculationService.calculateDailyLoginPoints();

            // Then
            assertThat(points).isEqualTo(PointValue.DAILY_LOGIN.getPoints());
        }

        @Test
        @DisplayName("Should apply bug reopened penalty")
        void testApplyBugReopenedPenalty() {
            // When
            int penalty = pointCalculationService.applyBugReopenedPenalty();

            // Then
            assertThat(penalty).isEqualTo(-PointValue.BUG_REOPENED_PENALTY.getPoints());
        }
    }

    @Nested
    @DisplayName("Transaction Creation Tests")
    class TransactionCreationTests {

        @Test
        @DisplayName("Should create positive points transaction")
        void testCreatePointTransaction_PositivePoints() {
            // Given
            PointTransactionRequest request = PointTransactionRequest.builder()
                    .userId(testUserId)
                    .projectId(testProjectId)
                    .pointsCredited(50)
                    .pointsDeducted(0)
                    .reason("Bug Resolution - HIGH")
                    .bugId(testBugId)
                    .build();

            when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(testTransaction);

            // When
            PointTransactionResponse response = pointCalculationService.createPointTransaction(request, testUserId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(testUserId);
            assertThat(response.getPointsCredited()).isEqualTo(50);
            assertThat(response.getPointsDeducted()).isEqualTo(0);
            assertThat(response.getReason()).isEqualTo("Bug Resolution - HIGH");
            verify(pointTransactionRepository).save(any(PointTransaction.class));
        }

        @Test
        @DisplayName("Should create negative points transaction")
        void testCreatePointTransaction_NegativePoints() {
            // Given
            PointTransactionRequest request = PointTransactionRequest.builder()
                    .userId(testUserId)
                    .projectId(testProjectId)
                    .pointsCredited(0)
                    .pointsDeducted(10)
                    .reason("Bug Reopened")
                    .bugId(testBugId)
                    .build();

            testTransaction.setPointsCredited(0);
            testTransaction.setPointsDeducted(10);
            testTransaction.setReason("Bug Reopened");

            when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(testTransaction);

            // When
            PointTransactionResponse response = pointCalculationService.createPointTransaction(request, testUserId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getPointsCredited()).isEqualTo(0);
            assertThat(response.getPointsDeducted()).isEqualTo(10);
            assertThat(response.getReason()).isEqualTo("Bug Reopened");
        }

        @Test
        @DisplayName("Should create project-specific transaction")
        void testCreatePointTransaction_WithProject() {
            // Given
            PointTransactionRequest request = PointTransactionRequest.builder()
                    .userId(testUserId)
                    .projectId(testProjectId)
                    .pointsCredited(25)
                    .pointsDeducted(0)
                    .reason("Bug Resolution - MEDIUM")
                    .bugId(testBugId)
                    .build();

            when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(testTransaction);

            // When
            PointTransactionResponse response = pointCalculationService.createPointTransaction(request, testUserId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProjectId()).isEqualTo(testProjectId);
            assertThat(response.getBugId()).isEqualTo(testBugId);
        }

        @Test
        @DisplayName("Should create system activity transaction")
        void testCreatePointTransaction_SystemActivity() {
            // Given
            PointTransactionRequest request = PointTransactionRequest.builder()
                    .userId(testUserId)
                    .projectId(null)
                    .pointsCredited(1)
                    .pointsDeducted(0)
                    .reason("Daily Login")
                    .bugId(null)
                    .build();

            testTransaction.setProjectId(null);
            testTransaction.setPointsCredited(1);
            testTransaction.setReason("Daily Login");
            testTransaction.setBugId(null);

            when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(testTransaction);

            // When
            PointTransactionResponse response = pointCalculationService.createPointTransaction(request, testUserId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProjectId()).isNull();
            assertThat(response.getBugId()).isNull();
            assertThat(response.getPointsCredited()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should map response correctly")
        void testCreatePointTransaction_ResponseMapping() {
            // Given
            PointTransactionRequest request = PointTransactionRequest.builder()
                    .userId(testUserId)
                    .projectId(testProjectId)
                    .pointsCredited(100)
                    .pointsDeducted(0)
                    .reason("Bug Resolution - CRASH")
                    .bugId(testBugId)
                    .build();

            when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(testTransaction);

            // When
            PointTransactionResponse response = pointCalculationService.createPointTransaction(request, testUserId);

            // Then
            assertThat(response.getTransactionId()).isEqualTo(testTransaction.getTransactionId());
            assertThat(response.getUserId()).isEqualTo(testTransaction.getUserId());
            assertThat(response.getProjectId()).isEqualTo(testTransaction.getProjectId());
            assertThat(response.getPoints()).isEqualTo(testTransaction.getNetPoints());
            assertThat(response.getPointsCredited()).isEqualTo(testTransaction.getPointsCredited());
            assertThat(response.getPointsDeducted()).isEqualTo(testTransaction.getPointsDeducted());
            assertThat(response.getReason()).isEqualTo(testTransaction.getReason());
            assertThat(response.getBugId()).isEqualTo(testTransaction.getBugId());
            assertThat(response.getEarnedAt()).isEqualTo(testTransaction.getEarnedAt());
        }

        @Test
        @DisplayName("Should handle transaction creation errors")
        void testCreatePointTransaction_ErrorHandling() {
            // Given
            PointTransactionRequest request = PointTransactionRequest.builder()
                    .userId(testUserId)
                    .projectId(testProjectId)
                    .pointsCredited(50)
                    .pointsDeducted(0)
                    .reason("Bug Resolution - HIGH")
                    .bugId(testBugId)
                    .build();

            when(pointTransactionRepository.save(any(PointTransaction.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> pointCalculationService.createPointTransaction(request, testUserId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");
        }
    }

    @Nested
    @DisplayName("Transaction Queries Tests")
    class TransactionQueriesTests {

        @Test
        @DisplayName("Should find daily login transactions for date")
        void testFindDailyLoginTransactionsForDate() {
            // Given
            LocalDate testDate = LocalDate.now();
            List<PointTransaction> expectedTransactions = List.of(testTransaction);

            when(pointTransactionRepository.findDailyLoginTransactionsForDate(testUserId, testDate))
                    .thenReturn(expectedTransactions);

            // When
            List<PointTransaction> result = pointCalculationService.findDailyLoginTransactionsForDate(testUserId,
                    testDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testTransaction);
            verify(pointTransactionRepository).findDailyLoginTransactionsForDate(testUserId, testDate);
        }

        @Test
        @DisplayName("Should find transactions by reason")
        void testFindTransactionsByReason() {
            // Given
            String reason = "Bug Resolution - HIGH";
            List<PointTransaction> expectedTransactions = List.of(testTransaction);

            when(pointTransactionRepository.findByUserIdAndReason(testUserId, reason))
                    .thenReturn(expectedTransactions);

            // When
            List<PointTransaction> result = pointCalculationService.findTransactionsByReason(testUserId, reason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testTransaction);
            verify(pointTransactionRepository).findByUserIdAndReason(testUserId, reason);
        }

        @Test
        @DisplayName("Should return empty list when no transactions found")
        void testFindTransactionsByReason_EmptyResult() {
            // Given
            String reason = "Non-existent reason";

            when(pointTransactionRepository.findByUserIdAndReason(testUserId, reason))
                    .thenReturn(List.of());

            // When
            List<PointTransaction> result = pointCalculationService.findTransactionsByReason(testUserId, reason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate valid transaction")
        void testValidatePointCalculation_ValidTransaction() {
            // When & Then
            assertThat(pointCalculationService.validatePointCalculation(50, 0, "Bug Resolution - HIGH"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should throw exception for null reason")
        void testValidatePointCalculation_NullReason() {
            // When & Then
            assertThatThrownBy(() -> pointCalculationService.validatePointCalculation(50, 0, null))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("Reason cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for empty reason")
        void testValidatePointCalculation_EmptyReason() {
            // When & Then
            assertThatThrownBy(() -> pointCalculationService.validatePointCalculation(50, 0, ""))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("Reason cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for short reason")
        void testValidatePointCalculation_ShortReason() {
            // When & Then
            assertThatThrownBy(() -> pointCalculationService.validatePointCalculation(50, 0, "Hi"))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("Reason must be at least 3 characters long");
        }

        @Test
        @DisplayName("Should throw exception for zero points")
        void testValidatePointCalculation_ZeroPoints() {
            // When & Then
            assertThatThrownBy(() -> pointCalculationService.validatePointCalculation(0, 0, "Valid reason"))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("At least one of points credited or deducted must be non-zero");
        }

        @Test
        @DisplayName("Should throw exception for both positive and negative points")
        void testValidatePointCalculation_BothPositiveAndNegative() {
            // When & Then
            assertThatThrownBy(() -> pointCalculationService.validatePointCalculation(50, 10, "Valid reason"))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("Cannot have both points credited and deducted in the same transaction");
        }

        @Test
        @DisplayName("Should validate bug resolution points correctly")
        void testValidatePointCalculation_BugResolutionPoints() {
            // When & Then - Valid points
            assertThat(pointCalculationService.validatePointCalculation(PointValue.BUG_RESOLUTION_LOW.getPoints(), 0,
                    "bug-resolution-low"))
                    .isTrue();
            assertThat(pointCalculationService.validatePointCalculation(PointValue.BUG_RESOLUTION_MEDIUM.getPoints(), 0,
                    "bug-resolution-medium"))
                    .isTrue();
            assertThat(pointCalculationService.validatePointCalculation(PointValue.BUG_RESOLUTION_HIGH.getPoints(), 0,
                    "bug-resolution-high"))
                    .isTrue();
            assertThat(pointCalculationService.validatePointCalculation(PointValue.BUG_RESOLUTION_CRITICAL.getPoints(),
                    0, "bug-resolution-critical"))
                    .isTrue();
            assertThat(pointCalculationService.validatePointCalculation(PointValue.BUG_RESOLUTION_CRASH.getPoints(), 0,
                    "bug-resolution-crash"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should throw exception for invalid bug resolution points")
        void testValidatePointCalculation_InvalidBugResolutionPoints() {
            // When & Then
            assertThatThrownBy(() -> pointCalculationService.validatePointCalculation(999, 0, "bug-resolution-invalid"))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("Invalid points for bug resolution: 999");
        }

        @Test
        @DisplayName("Should validate daily login points correctly")
        void testValidatePointCalculation_DailyLoginPoints() {
            // When & Then
            assertThat(pointCalculationService.validatePointCalculation(PointValue.DAILY_LOGIN.getPoints(), 0,
                    "daily-login"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should throw exception for invalid daily login points")
        void testValidatePointCalculation_InvalidDailyLoginPoints() {
            // When & Then
            assertThatThrownBy(() -> pointCalculationService.validatePointCalculation(5, 0, "daily-login"))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("Daily login should award exactly " + PointValue.DAILY_LOGIN.getPoints()
                            + " point, got: 5");
        }

        @Test
        @DisplayName("Should validate bug reopened penalty correctly")
        void testValidatePointCalculation_BugReopenedPenalty() {
            // When & Then
            assertThat(pointCalculationService.validatePointCalculation(0, PointValue.BUG_REOPENED_PENALTY.getPoints(),
                    "bug-reopened"))
                    .isTrue();
        }

        @Test
        @DisplayName("Should throw exception for invalid bug reopened penalty")
        void testValidatePointCalculation_InvalidBugReopenedPenalty() {
            // When & Then
            assertThatThrownBy(() -> pointCalculationService.validatePointCalculation(0, 5, "bug-reopened"))
                    .isInstanceOf(PointCalculationException.class)
                    .hasMessageContaining("Bug reopened should deduct exactly "
                            + PointValue.BUG_REOPENED_PENALTY.getPoints() + " points, got: 5");
        }
    }
}
