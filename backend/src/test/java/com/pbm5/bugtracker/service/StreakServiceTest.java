package com.pbm5.bugtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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

import com.pbm5.bugtracker.dto.StreakInfoResponse;
import com.pbm5.bugtracker.entity.UserStreak;
import com.pbm5.bugtracker.exception.StreakValidationException;
import com.pbm5.bugtracker.repository.UserStreakRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreakService Tests")
class StreakServiceTest {

    @Mock
    private UserStreakRepository userStreakRepository;

    @InjectMocks
    private StreakService streakService;

    private UUID testUserId;
    private UserStreak testUserStreak;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testDate = LocalDate.now();

        testUserStreak = UserStreak.builder()
                .userId(testUserId)
                .currentStreak(5)
                .maxStreak(10)
                .lastLoginDate(testDate.minusDays(1))
                .build();
    }

    @Nested
    @DisplayName("Streak Updates Tests")
    class StreakUpdatesTests {

        @Test
        @DisplayName("Should create new user streak")
        void testUpdateUserStreak_NewUser() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
            when(userStreakRepository.save(any(UserStreak.class))).thenReturn(testUserStreak);

            // When
            StreakInfoResponse result = streakService.updateUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            assertThat(result.getCurrentStreak()).isEqualTo(5);
            assertThat(result.getMaxStreak()).isEqualTo(10);
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should update consecutive day streak")
        void testUpdateUserStreak_ConsecutiveDay() {
            // Given
            UserStreak existingStreak = UserStreak.builder()
                    .userId(testUserId)
                    .currentStreak(3)
                    .maxStreak(5)
                    .lastLoginDate(testDate.minusDays(1))
                    .build();

            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(existingStreak));
            when(userStreakRepository.save(any(UserStreak.class))).thenReturn(existingStreak);

            // When
            StreakInfoResponse result = streakService.updateUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should handle same day login without changing streak")
        void testUpdateUserStreak_SameDay() {
            // Given
            UserStreak existingStreak = UserStreak.builder()
                    .userId(testUserId)
                    .currentStreak(3)
                    .maxStreak(5)
                    .lastLoginDate(testDate) // Same day
                    .build();

            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(existingStreak));
            when(userStreakRepository.save(any(UserStreak.class))).thenReturn(existingStreak);

            // When
            StreakInfoResponse result = streakService.updateUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should reset streak after gap in login")
        void testUpdateUserStreak_GapInLogin() {
            // Given
            UserStreak existingStreak = UserStreak.builder()
                    .userId(testUserId)
                    .currentStreak(5)
                    .maxStreak(10)
                    .lastLoginDate(testDate.minusDays(3)) // Gap of 2 days
                    .build();

            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(existingStreak));
            when(userStreakRepository.save(any(UserStreak.class))).thenReturn(existingStreak);

            // When
            StreakInfoResponse result = streakService.updateUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should update max streak when current exceeds it")
        void testUpdateUserStreak_MaxStreakUpdate() {
            // Given
            UserStreak existingStreak = UserStreak.builder()
                    .userId(testUserId)
                    .currentStreak(8)
                    .maxStreak(7) // Current streak will exceed max
                    .lastLoginDate(testDate.minusDays(1))
                    .build();

            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(existingStreak));
            when(userStreakRepository.save(any(UserStreak.class))).thenReturn(existingStreak);

            // When
            StreakInfoResponse result = streakService.updateUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should handle errors during streak update")
        void testUpdateUserStreak_ErrorHandling() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
            when(userStreakRepository.save(any(UserStreak.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> streakService.updateUserStreak(testUserId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");
        }
    }

    @Nested
    @DisplayName("Streak Calculations Tests")
    class StreakCalculationsTests {

        @Test
        @DisplayName("Should calculate current streak for existing user")
        void testCalculateStreak_ExistingUser() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserStreak));

            // When
            int streak = streakService.calculateStreak(testUserId);

            // Then
            assertThat(streak).isEqualTo(5);
            verify(userStreakRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("Should return zero streak for new user")
        void testCalculateStreak_NewUser() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

            // When
            int streak = streakService.calculateStreak(testUserId);

            // Then
            assertThat(streak).isEqualTo(0);
            verify(userStreakRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("Should return zero streak for user with zero streak")
        void testCalculateStreak_ZeroStreak() {
            // Given
            UserStreak zeroStreak = UserStreak.builder()
                    .userId(testUserId)
                    .currentStreak(0)
                    .maxStreak(0)
                    .lastLoginDate(null)
                    .build();

            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(zeroStreak));

            // When
            int streak = streakService.calculateStreak(testUserId);

            // Then
            assertThat(streak).isEqualTo(0);
        }

        @Test
        @DisplayName("Should map response correctly")
        void testCalculateStreak_ResponseMapping() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserStreak));

            // When
            int streak = streakService.calculateStreak(testUserId);

            // Then
            assertThat(streak).isEqualTo(testUserStreak.getCurrentStreak());
        }
    }

    @Nested
    @DisplayName("Streak Information Tests")
    class StreakInformationTests {

        @Test
        @DisplayName("Should get existing user streak")
        void testGetUserStreak_ExistingUser() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserStreak));

            // When
            StreakInfoResponse result = streakService.getUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            assertThat(result.getCurrentStreak()).isEqualTo(5);
            assertThat(result.getMaxStreak()).isEqualTo(10);
            assertThat(result.getLastLoginDate()).isEqualTo(testDate.minusDays(1));
            verify(userStreakRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("Should create new user streak when not found")
        void testGetUserStreak_NewUser() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
            when(userStreakRepository.save(any(UserStreak.class))).thenReturn(testUserStreak);

            // When
            StreakInfoResponse result = streakService.getUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should calculate streak start date correctly")
        void testGetUserStreak_StreakStartDate() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserStreak));

            // When
            StreakInfoResponse result = streakService.getUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStreakStartDate()).isNotNull();
            // Streak start date should be lastLoginDate minus (currentStreak - 1) days
            LocalDate expectedStartDate = testDate.minusDays(1).minusDays(5 - 1);
            assertThat(result.getStreakStartDate()).isEqualTo(expectedStartDate);
        }

        @Test
        @DisplayName("Should handle null streak start date for zero streak")
        void testGetUserStreak_ZeroStreakStartDate() {
            // Given
            UserStreak zeroStreak = UserStreak.builder()
                    .userId(testUserId)
                    .currentStreak(0)
                    .maxStreak(0)
                    .lastLoginDate(null)
                    .build();

            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(zeroStreak));

            // When
            StreakInfoResponse result = streakService.getUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStreakStartDate()).isNull();
        }
    }

    @Nested
    @DisplayName("Streak Validation Tests")
    class StreakValidationTests {

        @Test
        @DisplayName("Should validate valid date")
        void testValidateStreakUpdate_ValidDate() {
            // When & Then
            assertThat(streakService.validateStreakUpdate(testUserId, testDate)).isTrue();
        }

        @Test
        @DisplayName("Should validate past date")
        void testValidateStreakUpdate_PastDate() {
            // Given
            LocalDate pastDate = testDate.minusDays(1);

            // When & Then
            assertThat(streakService.validateStreakUpdate(testUserId, pastDate)).isTrue();
        }

        @Test
        @DisplayName("Should throw exception for null date")
        void testValidateStreakUpdate_NullDate() {
            // When & Then
            assertThatThrownBy(() -> streakService.validateStreakUpdate(testUserId, null))
                    .isInstanceOf(StreakValidationException.class)
                    .hasMessageContaining("Login date cannot be null");
        }

        @Test
        @DisplayName("Should throw exception for future date")
        void testValidateStreakUpdate_FutureDate() {
            // Given
            LocalDate futureDate = testDate.plusDays(1);

            // When & Then
            assertThatThrownBy(() -> streakService.validateStreakUpdate(testUserId, futureDate))
                    .isInstanceOf(StreakValidationException.class)
                    .hasMessageContaining("Login date cannot be in the future");
        }
    }

    @Nested
    @DisplayName("Max Streak Update Tests")
    class MaxStreakUpdateTests {

        @Test
        @DisplayName("Should update max streak when current exceeds it")
        void testUpdateMaxStreak_CurrentExceedsMax() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserStreak));
            when(userStreakRepository.save(any(UserStreak.class))).thenReturn(testUserStreak);

            // When
            streakService.updateMaxStreak(testUserId, 15);

            // Then
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should not update max streak when current is less than max")
        void testUpdateMaxStreak_CurrentLessThanMax() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserStreak));

            // When
            streakService.updateMaxStreak(testUserId, 3);

            // Then
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository, org.mockito.Mockito.never()).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should not update max streak when current equals max")
        void testUpdateMaxStreak_CurrentEqualsMax() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserStreak));

            // When
            streakService.updateMaxStreak(testUserId, 10);

            // Then
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository, org.mockito.Mockito.never()).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should handle user not found for max streak update")
        void testUpdateMaxStreak_UserNotFound() {
            // Given
            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

            // When
            streakService.updateMaxStreak(testUserId, 15);

            // Then
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository, org.mockito.Mockito.never()).save(any(UserStreak.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle first login ever")
        void testUpdateUserStreak_FirstLoginEver() {
            // Given
            UserStreak existingStreak = UserStreak.builder()
                    .userId(testUserId)
                    .currentStreak(0)
                    .maxStreak(0)
                    .lastLoginDate(null) // First login ever
                    .build();

            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(existingStreak));
            when(userStreakRepository.save(any(UserStreak.class))).thenReturn(existingStreak);

            // When
            StreakInfoResponse result = streakService.updateUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            verify(userStreakRepository).findByUserId(testUserId);
            verify(userStreakRepository).save(any(UserStreak.class));
        }

        @Test
        @DisplayName("Should handle streak start date calculation for single day streak")
        void testGetUserStreak_SingleDayStreak() {
            // Given
            UserStreak singleDayStreak = UserStreak.builder()
                    .userId(testUserId)
                    .currentStreak(1)
                    .maxStreak(1)
                    .lastLoginDate(testDate)
                    .build();

            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(singleDayStreak));

            // When
            StreakInfoResponse result = streakService.getUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStreakStartDate()).isEqualTo(testDate);
        }

        @Test
        @DisplayName("Should handle negative current streak")
        void testGetUserStreak_NegativeStreak() {
            // Given
            UserStreak negativeStreak = UserStreak.builder()
                    .userId(testUserId)
                    .currentStreak(-1)
                    .maxStreak(0)
                    .lastLoginDate(testDate)
                    .build();

            when(userStreakRepository.findByUserId(testUserId)).thenReturn(Optional.of(negativeStreak));

            // When
            StreakInfoResponse result = streakService.getUserStreak(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStreakStartDate()).isNull();
        }
    }
}
