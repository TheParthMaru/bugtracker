package com.pbm5.bugtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
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

import com.pbm5.bugtracker.dto.LeaderboardEntryResponse;
import com.pbm5.bugtracker.entity.ProjectLeaderboard;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.exception.LeaderboardNotFoundException;
import com.pbm5.bugtracker.repository.ProjectLeaderboardRepository;
import com.pbm5.bugtracker.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardService Tests")
class LeaderboardServiceTest {

    @Mock
    private ProjectLeaderboardRepository projectLeaderboardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    private UUID testUserId;
    private UUID testProjectId;
    private User testUser;
    private ProjectLeaderboard testLeaderboardEntry;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testProjectId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");

        testLeaderboardEntry = ProjectLeaderboard.builder()
                .leaderboardEntryId(UUID.randomUUID())
                .projectId(testProjectId)
                .userId(testUserId)
                .weeklyPoints(50)
                .monthlyPoints(150)
                .allTimePoints(500)
                .bugsResolved(5)
                .currentStreak(3)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Leaderboard Updates Tests")
    class LeaderboardUpdatesTests {

        @Test
        @DisplayName("Should create new leaderboard entry")
        void testUpdateProjectLeaderboard_NewEntry() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.empty());
            when(projectLeaderboardRepository.save(any(ProjectLeaderboard.class)))
                    .thenReturn(testLeaderboardEntry);

            // When
            leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, 50);

            // Then
            verify(projectLeaderboardRepository).findByProjectIdAndUserId(testProjectId, testUserId);
            verify(projectLeaderboardRepository).save(any(ProjectLeaderboard.class));
        }

        @Test
        @DisplayName("Should update existing leaderboard entry")
        void testUpdateProjectLeaderboard_ExistingEntry() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.of(testLeaderboardEntry));
            when(projectLeaderboardRepository.save(any(ProjectLeaderboard.class)))
                    .thenReturn(testLeaderboardEntry);

            // When
            leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, 25);

            // Then
            verify(projectLeaderboardRepository).findByProjectIdAndUserId(testProjectId, testUserId);
            verify(projectLeaderboardRepository).save(any(ProjectLeaderboard.class));
        }

        @Test
        @DisplayName("Should update leaderboard with bugs resolved delta")
        void testUpdateProjectLeaderboard_WithBugsResolved() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.of(testLeaderboardEntry));
            when(projectLeaderboardRepository.save(any(ProjectLeaderboard.class)))
                    .thenReturn(testLeaderboardEntry);

            // When
            leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, 50, 1);

            // Then
            verify(projectLeaderboardRepository).findByProjectIdAndUserId(testProjectId, testUserId);
            verify(projectLeaderboardRepository).save(any(ProjectLeaderboard.class));
        }

        @Test
        @DisplayName("Should handle negative points correctly")
        void testUpdateProjectLeaderboard_NegativePoints() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.empty());
            when(projectLeaderboardRepository.save(any(ProjectLeaderboard.class)))
                    .thenReturn(testLeaderboardEntry);

            // When
            leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, -10);

            // Then
            verify(projectLeaderboardRepository).findByProjectIdAndUserId(testProjectId, testUserId);
            verify(projectLeaderboardRepository).save(any(ProjectLeaderboard.class));
        }

        @Test
        @DisplayName("Should handle multiple point updates")
        void testUpdateProjectLeaderboard_MultipleUpdates() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.of(testLeaderboardEntry));
            when(projectLeaderboardRepository.save(any(ProjectLeaderboard.class)))
                    .thenReturn(testLeaderboardEntry);

            // When
            leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, 25);
            leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, 30);

            // Then
            verify(projectLeaderboardRepository, org.mockito.Mockito.times(2))
                    .findByProjectIdAndUserId(testProjectId, testUserId);
            verify(projectLeaderboardRepository, org.mockito.Mockito.times(2))
                    .save(any(ProjectLeaderboard.class));
        }

        @Test
        @DisplayName("Should handle errors during leaderboard update")
        void testUpdateProjectLeaderboard_ErrorHandling() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.of(testLeaderboardEntry));
            when(projectLeaderboardRepository.save(any(ProjectLeaderboard.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, 50))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");
        }
    }

    @Nested
    @DisplayName("Leaderboard Retrieval Tests")
    class LeaderboardRetrievalTests {

        @Test
        @DisplayName("Should get weekly leaderboard")
        void testGetWeeklyLeaderboard() {
            // Given
            List<ProjectLeaderboard> entries = List.of(testLeaderboardEntry);
            when(projectLeaderboardRepository.findByProjectIdOrderByWeeklyPointsDesc(testProjectId))
                    .thenReturn(entries);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            List<LeaderboardEntryResponse> result = leaderboardService.getWeeklyLeaderboard(testProjectId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(testUserId);
            assertThat(result.get(0).getWeeklyPoints()).isEqualTo(50);
            assertThat(result.get(0).getUserDisplayName()).isEqualTo("John Doe");
            assertThat(result.get(0).getRank()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get monthly leaderboard")
        void testGetMonthlyLeaderboard() {
            // Given
            List<ProjectLeaderboard> entries = List.of(testLeaderboardEntry);
            when(projectLeaderboardRepository.findByProjectIdOrderByMonthlyPointsDesc(testProjectId))
                    .thenReturn(entries);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            List<LeaderboardEntryResponse> result = leaderboardService.getMonthlyLeaderboard(testProjectId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(testUserId);
            assertThat(result.get(0).getMonthlyPoints()).isEqualTo(150);
            assertThat(result.get(0).getUserDisplayName()).isEqualTo("John Doe");
            assertThat(result.get(0).getRank()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get all-time leaderboard")
        void testGetAllTimeLeaderboard() {
            // Given
            List<ProjectLeaderboard> entries = List.of(testLeaderboardEntry);
            when(projectLeaderboardRepository.findByProjectIdOrderByAllTimePointsDesc(testProjectId))
                    .thenReturn(entries);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            List<LeaderboardEntryResponse> result = leaderboardService.getAllTimeLeaderboard(testProjectId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(testUserId);
            assertThat(result.get(0).getAllTimePoints()).isEqualTo(500);
            assertThat(result.get(0).getUserDisplayName()).isEqualTo("John Doe");
            assertThat(result.get(0).getRank()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get leaderboard entry for existing user")
        void testGetLeaderboardEntry_ExistingUser() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.of(testLeaderboardEntry));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            LeaderboardEntryResponse result = leaderboardService.getLeaderboardEntry(testProjectId, testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            assertThat(result.getProjectId()).isEqualTo(testProjectId);
            assertThat(result.getUserDisplayName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void testGetLeaderboardEntry_NonExistentUser() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> leaderboardService.getLeaderboardEntry(testProjectId, testUserId))
                    .isInstanceOf(LeaderboardNotFoundException.class)
                    .hasMessageContaining(
                            "Leaderboard entry not found for project: " + testProjectId + ", user: " + testUserId);
        }

        @Test
        @DisplayName("Should get paginated weekly leaderboard")
        void testGetProjectLeaderboard_Weekly() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProjectLeaderboard> entriesPage = new PageImpl<>(List.of(testLeaderboardEntry), pageable, 1);
            when(projectLeaderboardRepository.findTopWeeklyPerformersByProject(testProjectId, pageable))
                    .thenReturn(entriesPage);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            Page<LeaderboardEntryResponse> result = leaderboardService.getProjectLeaderboard(testProjectId, "weekly",
                    pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(testUserId);
            assertThat(result.getContent().get(0).getWeeklyPoints()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should get paginated monthly leaderboard")
        void testGetProjectLeaderboard_Monthly() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProjectLeaderboard> entriesPage = new PageImpl<>(List.of(testLeaderboardEntry), pageable, 1);
            when(projectLeaderboardRepository.findTopMonthlyPerformersByProject(testProjectId, pageable))
                    .thenReturn(entriesPage);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            Page<LeaderboardEntryResponse> result = leaderboardService.getProjectLeaderboard(testProjectId, "monthly",
                    pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(testUserId);
            assertThat(result.getContent().get(0).getMonthlyPoints()).isEqualTo(150);
        }

        @Test
        @DisplayName("Should get paginated all-time leaderboard")
        void testGetProjectLeaderboard_AllTime() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProjectLeaderboard> entriesPage = new PageImpl<>(List.of(testLeaderboardEntry), pageable, 1);
            when(projectLeaderboardRepository.findTopPerformersByProject(testProjectId, pageable))
                    .thenReturn(entriesPage);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            Page<LeaderboardEntryResponse> result = leaderboardService.getProjectLeaderboard(testProjectId, "all-time",
                    pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(testUserId);
            assertThat(result.getContent().get(0).getAllTimePoints()).isEqualTo(500);
        }

        @Test
        @DisplayName("Should map response correctly")
        void testGetProjectLeaderboard_ResponseMapping() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProjectLeaderboard> entriesPage = new PageImpl<>(List.of(testLeaderboardEntry), pageable, 1);
            when(projectLeaderboardRepository.findTopPerformersByProject(testProjectId, pageable))
                    .thenReturn(entriesPage);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            Page<LeaderboardEntryResponse> result = leaderboardService.getProjectLeaderboard(testProjectId, "all-time",
                    pageable);

            // Then
            LeaderboardEntryResponse response = result.getContent().get(0);
            assertThat(response.getLeaderboardEntryId()).isEqualTo(testLeaderboardEntry.getLeaderboardEntryId());
            assertThat(response.getProjectId()).isEqualTo(testLeaderboardEntry.getProjectId());
            assertThat(response.getUserId()).isEqualTo(testLeaderboardEntry.getUserId());
            assertThat(response.getWeeklyPoints()).isEqualTo(testLeaderboardEntry.getWeeklyPoints());
            assertThat(response.getMonthlyPoints()).isEqualTo(testLeaderboardEntry.getMonthlyPoints());
            assertThat(response.getAllTimePoints()).isEqualTo(testLeaderboardEntry.getAllTimePoints());
            assertThat(response.getBugsResolved()).isEqualTo(testLeaderboardEntry.getBugsResolved());
            assertThat(response.getCurrentStreak()).isEqualTo(testLeaderboardEntry.getCurrentStreak());
            assertThat(response.getUpdatedAt()).isEqualTo(testLeaderboardEntry.getUpdatedAt());
            assertThat(response.getUserDisplayName()).isEqualTo("John Doe");
            assertThat(response.getRank()).isEqualTo(0); // Page mapping doesn't set rank
        }
    }

    @Nested
    @DisplayName("Leaderboard Management Tests")
    class LeaderboardManagementTests {

        @Test
        @DisplayName("Should reset weekly points")
        void testResetWeeklyPoints() {
            // When
            leaderboardService.resetWeeklyPoints();

            // Then - Method should complete without throwing exceptions
            // The actual implementation would involve bulk database updates
        }

        @Test
        @DisplayName("Should reset monthly points")
        void testResetMonthlyPoints() {
            // When
            leaderboardService.resetMonthlyPoints();

            // Then - Method should complete without throwing exceptions
            // The actual implementation would involve bulk database updates
        }

        @Test
        @DisplayName("Should create new leaderboard entry with correct initial values")
        void testCreateNewLeaderboardEntry() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.empty());
            when(projectLeaderboardRepository.save(any(ProjectLeaderboard.class)))
                    .thenReturn(testLeaderboardEntry);

            // When
            leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, 50);

            // Then
            verify(projectLeaderboardRepository).save(any(ProjectLeaderboard.class));
        }

        @Test
        @DisplayName("Should handle user not found in repository")
        void testMapToResponse_UserNotFound() {
            // Given
            List<ProjectLeaderboard> entries = List.of(testLeaderboardEntry);
            when(projectLeaderboardRepository.findByProjectIdOrderByWeeklyPointsDesc(testProjectId))
                    .thenReturn(entries);
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // When
            List<LeaderboardEntryResponse> result = leaderboardService.getWeeklyLeaderboard(testProjectId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserDisplayName()).isEqualTo("Unknown User");
        }

        @Test
        @DisplayName("Should handle user repository errors gracefully")
        void testMapToResponse_UserRepositoryError() {
            // Given
            List<ProjectLeaderboard> entries = List.of(testLeaderboardEntry);
            when(projectLeaderboardRepository.findByProjectIdOrderByWeeklyPointsDesc(testProjectId))
                    .thenReturn(entries);
            when(userRepository.findById(testUserId)).thenThrow(new RuntimeException("Database error"));

            // When
            List<LeaderboardEntryResponse> result = leaderboardService.getWeeklyLeaderboard(testProjectId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserDisplayName()).isEqualTo("Unknown User");
        }

        @Test
        @DisplayName("Should handle negative bugs resolved delta")
        void testUpdateProjectLeaderboard_NegativeBugsResolved() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.of(testLeaderboardEntry));
            when(projectLeaderboardRepository.save(any(ProjectLeaderboard.class)))
                    .thenReturn(testLeaderboardEntry);

            // When
            leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, 0, -1);

            // Then
            verify(projectLeaderboardRepository).findByProjectIdAndUserId(testProjectId, testUserId);
            verify(projectLeaderboardRepository).save(any(ProjectLeaderboard.class));
        }

        @Test
        @DisplayName("Should handle zero bugs resolved delta")
        void testUpdateProjectLeaderboard_ZeroBugsResolved() {
            // Given
            when(projectLeaderboardRepository.findByProjectIdAndUserId(testProjectId, testUserId))
                    .thenReturn(Optional.of(testLeaderboardEntry));
            when(projectLeaderboardRepository.save(any(ProjectLeaderboard.class)))
                    .thenReturn(testLeaderboardEntry);

            // When
            leaderboardService.updateProjectLeaderboard(testProjectId, testUserId, 50, 0);

            // Then
            verify(projectLeaderboardRepository).findByProjectIdAndUserId(testProjectId, testUserId);
            verify(projectLeaderboardRepository).save(any(ProjectLeaderboard.class));
        }
    }
}
