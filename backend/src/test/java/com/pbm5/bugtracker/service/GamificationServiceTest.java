package com.pbm5.bugtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.pbm5.bugtracker.dto.PointTransactionRequest;
import com.pbm5.bugtracker.dto.PointTransactionResponse;
import com.pbm5.bugtracker.dto.UserPointsResponse;
import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.PointTransaction;
import com.pbm5.bugtracker.entity.PointValue;
import com.pbm5.bugtracker.entity.Project;
import com.pbm5.bugtracker.entity.TransactionReason;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.entity.UserPoints;
import com.pbm5.bugtracker.repository.BugRepository;
import com.pbm5.bugtracker.repository.PointTransactionRepository;
import com.pbm5.bugtracker.repository.ProjectRepository;
import com.pbm5.bugtracker.repository.UserPointsRepository;
import com.pbm5.bugtracker.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GamificationService Tests")
class GamificationServiceTest {

        @Mock
        private UserPointsRepository userPointsRepository;

        @Mock
        private PointCalculationService pointCalculationService;

        @Mock
        private LeaderboardService leaderboardService;

        @Mock
        private StreakService streakService;

        @Mock
        private ProjectRepository projectRepository;

        @Mock
        private BugRepository bugRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private PointTransactionRepository pointTransactionRepository;

        @Mock
        private NotificationService notificationService;

        @InjectMocks
        private GamificationService gamificationService;

        private UUID testUserId;
        private UUID testProjectId;
        private Long testBugId;
        private User testUser;
        private Project testProject;
        private Bug testBug;
        private UserPoints testUserPoints;

        @BeforeEach
        void setUp() {
                testUserId = UUID.randomUUID();
                testProjectId = UUID.randomUUID();
                testBugId = 1L;

                testUser = new User();
                testUser.setId(testUserId);
                testUser.setFirstName("John");
                testUser.setLastName("Doe");
                testUser.setEmail("john.doe@example.com");

                testProject = new Project();
                testProject.setId(testProjectId);
                testProject.setName("Test Project");

                testBug = new Bug();
                testBug.setId(testBugId);
                testBug.setProjectTicketNumber(123);

                testUserPoints = UserPoints.builder()
                                .userId(testUserId)
                                .totalPoints(100)
                                .currentStreak(5)
                                .maxStreak(10)
                                .bugsResolved(3)
                                .lastActivity(LocalDateTime.now())
                                .build();
        }

        @Nested
        @DisplayName("User Initialization Tests")
        class UserInitializationTests {

                @Test
                @DisplayName("Should initialize gamification data for new user")
                void testInitializeUserGamificationData_NewUser() {
                        // Given
                        when(userPointsRepository.existsByUserId(testUserId)).thenReturn(false);
                        when(pointCalculationService.findTransactionsByReason(testUserId,
                                        TransactionReason.WELCOME_BONUS.getValue()))
                                        .thenReturn(List.of());
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);

                        // When
                        gamificationService.initializeUserGamificationData(testUserId);

                        // Then
                        verify(userPointsRepository).existsByUserId(testUserId);
                        verify(pointCalculationService).createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId));
                        verify(userPointsRepository).save(any(UserPoints.class));
                }

                @Test
                @DisplayName("Should skip initialization for existing user")
                void testInitializeUserGamificationData_ExistingUser() {
                        // Given
                        when(userPointsRepository.existsByUserId(testUserId)).thenReturn(true);

                        // When
                        gamificationService.initializeUserGamificationData(testUserId);

                        // Then
                        verify(userPointsRepository).existsByUserId(testUserId);
                        verify(pointCalculationService, never()).createPointTransaction(any(), any());
                        verify(userPointsRepository, never()).save(any(UserPoints.class));
                }

                @Test
                @DisplayName("Should handle welcome bonus creation")
                void testInitializeUserGamificationData_WelcomeBonus() {
                        // Given
                        when(userPointsRepository.existsByUserId(testUserId)).thenReturn(false);
                        when(pointCalculationService.findTransactionsByReason(testUserId,
                                        TransactionReason.WELCOME_BONUS.getValue()))
                                        .thenReturn(List.of());
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);

                        // When
                        gamificationService.initializeUserGamificationData(testUserId);

                        // Then
                        verify(pointCalculationService).createPointTransaction(
                                        argThat(request -> request.getUserId().equals(testUserId) &&
                                                        request.getProjectId() == null &&
                                                        request.getPointsCredited() == PointValue.WELCOME_BONUS
                                                                        .getPoints()
                                                        &&
                                                        request.getPointsDeducted() == 0 &&
                                                        request.getReason().equals(
                                                                        TransactionReason.WELCOME_BONUS.getValue())),
                                        eq(testUserId));
                }

                @Test
                @DisplayName("Should handle error during initialization")
                void testInitializeUserGamificationData_ErrorHandling() {
                        // Given
                        when(userPointsRepository.existsByUserId(testUserId)).thenReturn(false);
                        when(pointCalculationService.findTransactionsByReason(testUserId,
                                        TransactionReason.WELCOME_BONUS.getValue()))
                                        .thenReturn(List.of());
                        when(userPointsRepository.save(any(UserPoints.class)))
                                        .thenThrow(new RuntimeException("Database error"));

                        // When & Then
                        assertThatThrownBy(() -> gamificationService.initializeUserGamificationData(testUserId))
                                        .isInstanceOf(RuntimeException.class)
                                        .hasMessageContaining("Database error");
                }

                @Test
                @DisplayName("Should skip welcome bonus if already exists")
                void testInitializeUserGamificationData_ExistingWelcomeBonus() {
                        // Given
                        PointTransaction existingWelcomeBonus = new PointTransaction();
                        when(userPointsRepository.existsByUserId(testUserId)).thenReturn(false);
                        when(pointCalculationService.findTransactionsByReason(testUserId,
                                        TransactionReason.WELCOME_BONUS.getValue()))
                                        .thenReturn(List.of(existingWelcomeBonus));
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));

                        // When
                        gamificationService.initializeUserGamificationData(testUserId);

                        // Then
                        verify(pointCalculationService, never()).createPointTransaction(any(), any());
                        verify(userPointsRepository, never()).save(any(UserPoints.class)); // No save when welcome bonus
                                                                                           // already
                                                                                           // exists
                }
        }

        @Nested
        @DisplayName("User Points Retrieval Tests")
        class UserPointsRetrievalTests {

                @Test
                @DisplayName("Should retrieve existing user points")
                void testGetUserPoints_ExistingUser() {
                        // Given
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

                        // When
                        UserPointsResponse response = gamificationService.getUserPoints(testUserId);

                        // Then
                        assertThat(response).isNotNull();
                        assertThat(response.getUserId()).isEqualTo(testUserId);
                        assertThat(response.getTotalPoints()).isEqualTo(100);
                        assertThat(response.getCurrentStreak()).isEqualTo(5);
                        assertThat(response.getMaxStreak()).isEqualTo(10);
                        assertThat(response.getBugsResolved()).isEqualTo(3);
                        assertThat(response.getUserDisplayName()).isEqualTo("John Doe");
                }

                @Test
                @DisplayName("Should perform fallback initialization for new user")
                void testGetUserPoints_NewUser_FallbackInit() {
                        // Given
                        when(userPointsRepository.findByUserId(testUserId))
                                        .thenReturn(Optional.empty()) // First call returns empty
                                        .thenReturn(Optional.of(testUserPoints)); // Second call returns user points
                        when(userPointsRepository.existsByUserId(testUserId)).thenReturn(false);
                        when(pointCalculationService.findTransactionsByReason(testUserId,
                                        TransactionReason.WELCOME_BONUS.getValue()))
                                        .thenReturn(List.of());
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

                        // When
                        UserPointsResponse response = gamificationService.getUserPoints(testUserId);

                        // Then
                        assertThat(response).isNotNull();
                        assertThat(response.getUserId()).isEqualTo(testUserId);
                        verify(pointCalculationService).createPointTransaction(any(), any());
                }

                @Test
                @DisplayName("Should handle user not found in repository")
                void testGetUserPoints_UserNotFound() {
                        // Given
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

                        // When
                        UserPointsResponse response = gamificationService.getUserPoints(testUserId);

                        // Then
                        assertThat(response).isNotNull();
                        assertThat(response.getUserDisplayName()).isEqualTo("User " + testUserId);
                }

                @Test
                @DisplayName("Should handle fallback initialization failure")
                void testGetUserPoints_FallbackInitFailure() {
                        // Given
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
                        when(userPointsRepository.existsByUserId(testUserId)).thenReturn(false);
                        when(pointCalculationService.findTransactionsByReason(testUserId,
                                        TransactionReason.WELCOME_BONUS.getValue()))
                                        .thenReturn(List.of());
                        when(userPointsRepository.save(any(UserPoints.class)))
                                        .thenThrow(new RuntimeException("Database error"));

                        // When & Then
                        assertThatThrownBy(() -> gamificationService.getUserPoints(testUserId))
                                        .isInstanceOf(RuntimeException.class)
                                        .hasMessageContaining("Failed to initialize gamification data");
                }
        }

        @Nested
        @DisplayName("Point History Tests")
        class PointHistoryTests {

                @Test
                @DisplayName("Should retrieve paginated point history")
                void testGetPointHistory_WithPagination() {
                        // Given
                        Pageable pageable = PageRequest.of(0, 10);
                        PointTransaction transaction = new PointTransaction();
                        transaction.setTransactionId(UUID.randomUUID());
                        transaction.setUserId(testUserId);
                        transaction.setProjectId(testProjectId);
                        transaction.setPointsCredited(50);
                        transaction.setPointsDeducted(0);
                        transaction.setReason("Bug Resolution - HIGH");
                        transaction.setBugId(testBugId);
                        transaction.setEarnedAt(LocalDateTime.now());

                        Page<PointTransaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);
                        when(pointTransactionRepository.findByUserIdOrderByEarnedAtDesc(testUserId, pageable))
                                        .thenReturn(transactionPage);

                        // When
                        Page<PointTransactionResponse> response = gamificationService.getPointHistory(testUserId,
                                        pageable);

                        // Then
                        assertThat(response).isNotNull();
                        assertThat(response.getContent()).hasSize(1);
                        assertThat(response.getContent().get(0).getUserId()).isEqualTo(testUserId);
                        assertThat(response.getContent().get(0).getPoints()).isEqualTo(50);
                }

                @Test
                @DisplayName("Should handle empty point history")
                void testGetPointHistory_EmptyHistory() {
                        // Given
                        Pageable pageable = PageRequest.of(0, 10);
                        Page<PointTransaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);
                        when(pointTransactionRepository.findByUserIdOrderByEarnedAtDesc(testUserId, pageable))
                                        .thenReturn(emptyPage);

                        // When
                        Page<PointTransactionResponse> response = gamificationService.getPointHistory(testUserId,
                                        pageable);

                        // Then
                        assertThat(response).isNotNull();
                        assertThat(response.getContent()).isEmpty();
                        assertThat(response.getTotalElements()).isEqualTo(0);
                }

                @Test
                @DisplayName("Should map transaction response correctly")
                void testGetPointHistory_ResponseMapping() {
                        // Given
                        Pageable pageable = PageRequest.of(0, 10);
                        PointTransaction transaction = new PointTransaction();
                        transaction.setTransactionId(UUID.randomUUID());
                        transaction.setUserId(testUserId);
                        transaction.setProjectId(testProjectId);
                        transaction.setPointsCredited(25);
                        transaction.setPointsDeducted(0);
                        transaction.setReason("Daily Login");
                        transaction.setBugId(null);
                        transaction.setEarnedAt(LocalDateTime.now());

                        Page<PointTransaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);
                        when(pointTransactionRepository.findByUserIdOrderByEarnedAtDesc(testUserId, pageable))
                                        .thenReturn(transactionPage);

                        // When
                        Page<PointTransactionResponse> response = gamificationService.getPointHistory(testUserId,
                                        pageable);

                        // Then
                        PointTransactionResponse responseTransaction = response.getContent().get(0);
                        assertThat(responseTransaction.getTransactionId()).isEqualTo(transaction.getTransactionId());
                        assertThat(responseTransaction.getUserId()).isEqualTo(testUserId);
                        assertThat(responseTransaction.getProjectId()).isEqualTo(testProjectId);
                        assertThat(responseTransaction.getPointsCredited()).isEqualTo(25);
                        assertThat(responseTransaction.getPointsDeducted()).isEqualTo(0);
                        assertThat(responseTransaction.getReason()).isEqualTo("Daily Login");
                        assertThat(responseTransaction.getBugId()).isNull();
                }
        }

        @Nested
        @DisplayName("Point Awarding Tests")
        class PointAwardingTests {

                @Test
                @DisplayName("Should award positive points")
                void testAwardPoints_PositivePoints() {
                        // Given
                        int points = 50;
                        String reason = "Bug Resolution - HIGH";
                        PointTransactionResponse mockTransaction = PointTransactionResponse.builder()
                                        .transactionId(UUID.randomUUID())
                                        .userId(testUserId)
                                        .projectId(testProjectId)
                                        .points(points)
                                        .reason(reason)
                                        .build();

                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(mockTransaction);
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        doNothing().when(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId,
                                        points);

                        // When
                        PointTransactionResponse response = gamificationService.awardPoints(testUserId, testProjectId,
                                        points,
                                        reason, testBugId);

                        // Then
                        assertThat(response).isNotNull();
                        assertThat(response.getPoints()).isEqualTo(points);
                        verify(pointCalculationService).createPointTransaction(any(), any());
                        verify(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId, points);
                }

                @Test
                @DisplayName("Should award negative points (penalties)")
                void testAwardPoints_NegativePoints() {
                        // Given
                        int points = -10;
                        String reason = "Bug Reopened";
                        PointTransactionResponse mockTransaction = PointTransactionResponse.builder()
                                        .transactionId(UUID.randomUUID())
                                        .userId(testUserId)
                                        .projectId(testProjectId)
                                        .points(points)
                                        .reason(reason)
                                        .build();

                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(mockTransaction);
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        doNothing().when(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId,
                                        points);

                        // When
                        PointTransactionResponse response = gamificationService.awardPoints(testUserId, testProjectId,
                                        points,
                                        reason, testBugId);

                        // Then
                        assertThat(response).isNotNull();
                        assertThat(response.getPoints()).isEqualTo(points);
                        verify(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId, points);
                }

                @Test
                @DisplayName("Should handle system activities (null project)")
                void testAwardPoints_SystemActivity() {
                        // Given
                        int points = 1;
                        String reason = "Daily Login";
                        PointTransactionResponse mockTransaction = PointTransactionResponse.builder()
                                        .transactionId(UUID.randomUUID())
                                        .userId(testUserId)
                                        .projectId(null)
                                        .points(points)
                                        .reason(reason)
                                        .build();

                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(mockTransaction);
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);

                        // When
                        PointTransactionResponse response = gamificationService.awardPoints(testUserId, null, points,
                                        reason, null);

                        // Then
                        assertThat(response).isNotNull();
                        assertThat(response.getPoints()).isEqualTo(points);
                        verify(leaderboardService, never()).updateProjectLeaderboard(any(), any(), anyInt());
                }

                @Test
                @DisplayName("Should create new user points if not exists")
                void testAwardPoints_NewUserPoints() {
                        // Given
                        int points = 25;
                        String reason = "Bug Resolution - MEDIUM";
                        PointTransactionResponse mockTransaction = PointTransactionResponse.builder()
                                        .transactionId(UUID.randomUUID())
                                        .userId(testUserId)
                                        .projectId(testProjectId)
                                        .points(points)
                                        .reason(reason)
                                        .build();

                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(mockTransaction);
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
                        lenient().when(userPointsRepository.existsByUserId(testUserId)).thenReturn(false);
                        lenient()
                                        .when(pointCalculationService.findTransactionsByReason(testUserId,
                                                        TransactionReason.WELCOME_BONUS.getValue()))
                                        .thenReturn(List.of());
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        doNothing().when(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId,
                                        points);

                        // When
                        PointTransactionResponse response = gamificationService.awardPoints(testUserId, testProjectId,
                                        points,
                                        reason, testBugId);

                        // Then
                        assertThat(response).isNotNull();
                        verify(userPointsRepository, times(2)).save(any(UserPoints.class)); // Once for initialization,
                                                                                            // once for
                                                                                            // update
                }

                @Test
                @DisplayName("Should validate transaction creation")
                void testAwardPoints_TransactionCreation() {
                        // Given
                        int points = 100;
                        String reason = "Bug Resolution - CRASH";
                        PointTransactionResponse mockTransaction = PointTransactionResponse.builder()
                                        .transactionId(UUID.randomUUID())
                                        .userId(testUserId)
                                        .projectId(testProjectId)
                                        .points(points)
                                        .reason(reason)
                                        .build();

                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(mockTransaction);
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        doNothing().when(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId,
                                        points);

                        // When
                        gamificationService.awardPoints(testUserId, testProjectId, points, reason, testBugId);

                        // Then
                        verify(pointCalculationService).createPointTransaction(
                                        argThat(request -> request.getUserId().equals(testUserId) &&
                                                        request.getProjectId().equals(testProjectId) &&
                                                        request.getPointsCredited() == points &&
                                                        request.getPointsDeducted() == 0 &&
                                                        request.getReason().equals(reason) &&
                                                        request.getBugId().equals(testBugId)),
                                        eq(testUserId));
                }
        }

        @Nested
        @DisplayName("Daily Login Tests")
        class DailyLoginTests {

                @Test
                @DisplayName("Should handle daily login for eligible user")
                void testHandleDailyLogin_EligibleUser() {
                        // Given
                        LocalDate today = LocalDate.now();
                        when(pointCalculationService.findDailyLoginTransactionsForDate(testUserId, today))
                                        .thenReturn(List.of()); // Empty list means eligible
                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(PointTransactionResponse.builder().build());
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        when(streakService.updateUserStreak(testUserId)).thenReturn(null);

                        // When
                        gamificationService.handleDailyLogin(testUserId);

                        // Then
                        verify(pointCalculationService).createPointTransaction(any(), any());
                        verify(streakService).updateUserStreak(testUserId);
                }

                @Test
                @DisplayName("Should skip daily login for ineligible user")
                void testHandleDailyLogin_IneligibleUser() {
                        // Given
                        LocalDate today = LocalDate.now();
                        PointTransaction existingTransaction = new PointTransaction();
                        when(pointCalculationService.findDailyLoginTransactionsForDate(testUserId, today))
                                        .thenReturn(List.of(existingTransaction)); // Non-empty list means ineligible

                        // When
                        gamificationService.handleDailyLogin(testUserId);

                        // Then
                        verify(pointCalculationService, never()).createPointTransaction(any(), any());
                        verify(streakService, never()).updateUserStreak(testUserId);
                }

                @Test
                @DisplayName("Should update streak during daily login")
                void testHandleDailyLogin_StreakUpdate() {
                        // Given
                        LocalDate today = LocalDate.now();
                        when(pointCalculationService.findDailyLoginTransactionsForDate(testUserId, today))
                                        .thenReturn(List.of()); // Empty list means eligible
                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(PointTransactionResponse.builder().build());
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);

                        // When
                        gamificationService.handleDailyLogin(testUserId);

                        // Then
                        verify(streakService).updateUserStreak(testUserId);
                }

                @Test
                @DisplayName("Should handle daily login errors gracefully")
                void testHandleDailyLogin_ErrorHandling() {
                        // Given
                        LocalDate today = LocalDate.now();
                        when(pointCalculationService.findDailyLoginTransactionsForDate(testUserId, today))
                                        .thenReturn(List.of()); // Empty list means eligible
                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenThrow(new RuntimeException("Transaction error"));

                        // When & Then
                        assertThatThrownBy(() -> gamificationService.handleDailyLogin(testUserId))
                                        .isInstanceOf(RuntimeException.class)
                                        .hasMessageContaining("Transaction error");
                }
        }

        @Nested
        @DisplayName("Bug Resolution Tests")
        class BugResolutionTests {

                @Test
                @DisplayName("Should handle CRASH bug resolution")
                void testHandleBugResolution_CRASH() {
                        // Given
                        String priority = "CRASH";
                        when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(testProject));
                        when(bugRepository.findById(testBugId)).thenReturn(Optional.of(testBug));
                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(PointTransactionResponse.builder().build());
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt());
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt(), anyInt());
                        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
                        when(notificationService.createAndDeliverNotification(any(), any(), any(), any(), any(), any(),
                                        any(),
                                        any(), any(), any()))
                                        .thenReturn(null);

                        // When
                        gamificationService.handleBugResolution(testBugId, testUserId, testProjectId, priority);

                        // Then
                        verify(pointCalculationService).createPointTransaction(any(), any());
                        verify(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId,
                                        PointValue.BUG_RESOLUTION_CRASH.getPoints(), 1);
                        verify(notificationService).createAndDeliverNotification(any(), any(), any(), any(), any(),
                                        any(), any(),
                                        any(), any(), any());
                }

                @Test
                @DisplayName("Should handle CRITICAL bug resolution")
                void testHandleBugResolution_CRITICAL() {
                        // Given
                        String priority = "CRITICAL";
                        when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(testProject));
                        when(bugRepository.findById(testBugId)).thenReturn(Optional.of(testBug));
                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(PointTransactionResponse.builder().build());
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt());
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt(), anyInt());
                        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
                        when(notificationService.createAndDeliverNotification(any(), any(), any(), any(), any(), any(),
                                        any(),
                                        any(), any(), any()))
                                        .thenReturn(null);

                        // When
                        gamificationService.handleBugResolution(testBugId, testUserId, testProjectId, priority);

                        // Then
                        verify(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId,
                                        PointValue.BUG_RESOLUTION_CRITICAL.getPoints(), 1);
                }

                @Test
                @DisplayName("Should handle HIGH bug resolution")
                void testHandleBugResolution_HIGH() {
                        // Given
                        String priority = "HIGH";
                        when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(testProject));
                        when(bugRepository.findById(testBugId)).thenReturn(Optional.of(testBug));
                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(PointTransactionResponse.builder().build());
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt());
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt(), anyInt());
                        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
                        when(notificationService.createAndDeliverNotification(any(), any(), any(), any(), any(), any(),
                                        any(),
                                        any(), any(), any()))
                                        .thenReturn(null);

                        // When
                        gamificationService.handleBugResolution(testBugId, testUserId, testProjectId, priority);

                        // Then
                        verify(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId,
                                        PointValue.BUG_RESOLUTION_HIGH.getPoints(), 1);
                }

                @Test
                @DisplayName("Should send notification for bug resolution")
                void testHandleBugResolution_Notification() {
                        // Given
                        String priority = "MEDIUM";
                        when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(testProject));
                        when(bugRepository.findById(testBugId)).thenReturn(Optional.of(testBug));
                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(PointTransactionResponse.builder().build());
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt());
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt(), anyInt());
                        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

                        // When
                        gamificationService.handleBugResolution(testBugId, testUserId, testProjectId, priority);

                        // Then
                        verify(notificationService).createAndDeliverNotification(
                                        eq(testUserId),
                                        eq("GAMIFICATION_POINTS"),
                                        eq("Points earned for bug resolution!"),
                                        anyString(),
                                        eq(null),
                                        any(Map.class),
                                        eq(null),
                                        eq(null),
                                        eq(null),
                                        eq(null));
                }
        }

        @Nested
        @DisplayName("Daily Login Eligibility Tests")
        class DailyLoginEligibilityTests {

                @Test
                @DisplayName("Should return true for eligible user")
                void testIsUserEligibleForDailyLogin_Eligible() {
                        // Given
                        LocalDate today = LocalDate.now();
                        when(pointCalculationService.findDailyLoginTransactionsForDate(testUserId, today))
                                        .thenReturn(List.of());

                        // When
                        boolean eligible = gamificationService.isUserEligibleForDailyLogin(testUserId);

                        // Then
                        assertThat(eligible).isTrue();
                }

                @Test
                @DisplayName("Should return false for ineligible user")
                void testIsUserEligibleForDailyLogin_Ineligible() {
                        // Given
                        LocalDate today = LocalDate.now();
                        PointTransaction existingTransaction = new PointTransaction();
                        when(pointCalculationService.findDailyLoginTransactionsForDate(testUserId, today))
                                        .thenReturn(List.of(existingTransaction));

                        // When
                        boolean eligible = gamificationService.isUserEligibleForDailyLogin(testUserId);

                        // Then
                        assertThat(eligible).isFalse();
                }
        }

        @Nested
        @DisplayName("Bug Reopening Tests")
        class BugReopeningTests {

                @Test
                @DisplayName("Should handle bug reopening penalty")
                void testHandleBugReopening_Penalty() {
                        // Given
                        when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(testProject));
                        when(bugRepository.findById(testBugId)).thenReturn(Optional.of(testBug));
                        when(pointCalculationService.createPointTransaction(any(PointTransactionRequest.class),
                                        eq(testUserId)))
                                        .thenReturn(PointTransactionResponse.builder().build());
                        when(userPointsRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserPoints));
                        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(testUserPoints);
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt());
                        lenient().doNothing().when(leaderboardService).updateProjectLeaderboard(eq(testProjectId),
                                        eq(testUserId),
                                        anyInt(), anyInt());
                        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
                        when(notificationService.createAndDeliverNotification(any(), any(), any(), any(), any(), any(),
                                        any(),
                                        any(), any(), any()))
                                        .thenReturn(null);

                        // When
                        gamificationService.handleBugReopening(testBugId, testUserId, testProjectId);

                        // Then
                        verify(leaderboardService).updateProjectLeaderboard(testProjectId, testUserId,
                                        -PointValue.BUG_REOPENED_PENALTY.getPoints(), -1);
                        verify(notificationService).createAndDeliverNotification(
                                        eq(testUserId),
                                        eq("GAMIFICATION_POINTS"),
                                        eq("Points penalty for bug reopening"),
                                        anyString(),
                                        eq(null),
                                        any(Map.class),
                                        eq(null),
                                        eq(null),
                                        eq(null),
                                        eq(null));
                }
        }
}
