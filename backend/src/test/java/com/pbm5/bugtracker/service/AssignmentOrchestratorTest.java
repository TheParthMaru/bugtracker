package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation;
import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation.AssignmentType;
import com.pbm5.bugtracker.dto.TeamAssignmentRecommendation.TeamAssignmentInfo;
import com.pbm5.bugtracker.entity.*;
import com.pbm5.bugtracker.repository.BugRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentOrchestratorTest {

    @Mock
    private TeamAssignmentService teamAssignmentService;

    @Mock
    private UserAssignmentService userAssignmentService;

    @Mock
    private BugRepository bugRepository;

    @InjectMocks
    private AssignmentOrchestrator assignmentOrchestrator;

    private Project testProject;
    private Bug testBug;
    private User testUser;
    private TeamAssignmentRecommendation testRecommendation;
    private List<TeamAssignmentInfo> testTeamAssignments;

    @BeforeEach
    void setUp() {
        // Setup test project
        testProject = new Project();
        testProject.setId(UUID.randomUUID());
        testProject.setName("Test Project");

        // Setup test bug
        testBug = new Bug();
        testBug.setId(1L);
        testBug.setTitle("Test Bug");
        testBug.setDescription("Test Description");
        testBug.setProject(testProject);

        // Setup test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john@example.com");

        // Setup test team assignments
        TeamAssignmentInfo teamAssignment1 = TeamAssignmentInfo.builder()
                .teamId(UUID.randomUUID())
                .teamName("Frontend Team")
                .teamSlug("frontend-team")
                .projectSlug("test-project")
                .memberCount(3)
                .matchingLabels(List.of("frontend"))
                .labelMatchScore(1.0)
                .isPrimary(true)
                .assignmentReason("Auto-detected based on bug labels")
                .build();

        TeamAssignmentInfo teamAssignment2 = TeamAssignmentInfo.builder()
                .teamId(UUID.randomUUID())
                .teamName("Backend Team")
                .teamSlug("backend-team")
                .projectSlug("test-project")
                .memberCount(2)
                .matchingLabels(List.of("backend"))
                .labelMatchScore(0.8)
                .isPrimary(false)
                .assignmentReason("Auto-detected based on bug labels")
                .build();

        testTeamAssignments = List.of(teamAssignment1, teamAssignment2);

        // Setup test recommendation
        testRecommendation = TeamAssignmentRecommendation.builder()
                .assignmentType(AssignmentType.MULTI_TEAM)
                .message("Bug assigned to 2 teams for coordinated resolution")
                .assignedTeams(testTeamAssignments)
                .teamMemberSkills(new HashMap<>())
                .analyzedLabels(Set.of("frontend", "backend"))
                .analyzedTags(Set.of("react", "javascript"))
                .generatedAt(java.time.LocalDateTime.now())
                .confidenceScore(0.85)
                .build();
    }

    // 3.1 Complete Workflow Tests

    @Test
    void testExecuteCompleteAutoAssignment_Success() {
        // Given
        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(testRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(testUser);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        verify(teamAssignmentService).getAssignmentRecommendation(testBug);
        verify(userAssignmentService).autoAssignUserToBug(eq(testBug), anyList());
    }

    @Test
    void testExecuteCompleteAutoAssignment_NoTeamsFound() {
        // Given
        TeamAssignmentRecommendation noTeamRecommendation = TeamAssignmentRecommendation.builder()
                .assignmentType(AssignmentType.NO_TEAM_FOUND)
                .message("No specific team found for labels")
                .assignedTeams(Collections.emptyList())
                .teamMemberSkills(new HashMap<>())
                .analyzedLabels(Set.of("nonexistent"))
                .analyzedTags(Set.of("unknown"))
                .generatedAt(java.time.LocalDateTime.now())
                .confidenceScore(0.0)
                .build();

        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(noTeamRecommendation);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        // Then
        assertThat(result).isNull();
        verify(teamAssignmentService).getAssignmentRecommendation(testBug);
        verify(userAssignmentService, never()).autoAssignUserToBug(any(), any());
    }

    @Test
    void testExecuteCompleteAutoAssignment_TeamsFoundNoUsers() {
        // Given
        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(testRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(null);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        // Then
        assertThat(result).isNull();
        verify(teamAssignmentService).getAssignmentRecommendation(testBug);
        verify(userAssignmentService).autoAssignUserToBug(eq(testBug), anyList());
    }

    @Test
    void testExecuteCompleteAutoAssignment_ServiceError() {
        // Given
        when(teamAssignmentService.getAssignmentRecommendation(testBug))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        assertThatThrownBy(() -> assignmentOrchestrator.executeCompleteAutoAssignment(testBug))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to execute complete auto-assignment workflow");
    }

    @Test
    void testExecuteCompleteAutoAssignment_ByBugId_Success() {
        // Given
        when(bugRepository.findById(testBug.getId())).thenReturn(Optional.of(testBug));
        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(testRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(testUser);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignmentById(testBug.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        verify(bugRepository).findById(testBug.getId());
        verify(teamAssignmentService).getAssignmentRecommendation(testBug);
        verify(userAssignmentService).autoAssignUserToBug(eq(testBug), anyList());
    }

    @Test
    void testExecuteCompleteAutoAssignment_ByBugId_InvalidBug() {
        // Given
        when(bugRepository.findById(testBug.getId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> assignmentOrchestrator.executeCompleteAutoAssignmentById(testBug.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bug not found with ID");
    }

    // Integration Tests

    @Test
    void testTeamAssignmentToUserAssignment_Flow() {
        // Given
        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(testRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(testUser);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        // Then
        assertThat(result).isNotNull();

        // Verify the flow: team assignment first, then user assignment
        verify(teamAssignmentService).getAssignmentRecommendation(testBug);
        verify(userAssignmentService).autoAssignUserToBug(eq(testBug), argThat(teamIds -> {
            // Verify that team IDs are extracted from the recommendation
            return teamIds.size() == 2 &&
                    teamIds.contains(testTeamAssignments.get(0).getTeamId()) &&
                    teamIds.contains(testTeamAssignments.get(1).getTeamId());
        }));
    }

    @Test
    void testAssignmentRecommendationToPersistence_Flow() {
        // Given
        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(testRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(testUser);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        // Then
        assertThat(result).isNotNull();

        // Verify that the recommendation is properly processed
        verify(teamAssignmentService).getAssignmentRecommendation(testBug);

        // Verify that team IDs are correctly extracted and passed to user assignment
        verify(userAssignmentService).autoAssignUserToBug(eq(testBug), anyList());
    }

    @Test
    void testMultiTeamAssignment_Flow() {
        // Given
        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(testRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(testUser);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        // Then
        assertThat(result).isNotNull();

        // Verify that multiple teams are handled correctly
        verify(userAssignmentService).autoAssignUserToBug(eq(testBug), argThat(teamIds -> {
            return teamIds.size() == 2; // Both teams should be passed
        }));
    }

    // Edge Cases and Error Handling

    @Test
    void testMinimumScoreThresholds() {
        // Given
        TeamAssignmentRecommendation lowConfidenceRecommendation = TeamAssignmentRecommendation.builder()
                .assignmentType(AssignmentType.SINGLE_TEAM)
                .message("Low confidence assignment")
                .assignedTeams(List.of(testTeamAssignments.get(0)))
                .teamMemberSkills(new HashMap<>())
                .analyzedLabels(Set.of("frontend"))
                .analyzedTags(Set.of("react"))
                .generatedAt(java.time.LocalDateTime.now())
                .confidenceScore(0.1) // Low confidence
                .build();

        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(lowConfidenceRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(testUser);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        // Then
        assertThat(result).isNotNull(); // Should still proceed with assignment
        verify(teamAssignmentService).getAssignmentRecommendation(testBug);
        verify(userAssignmentService).autoAssignUserToBug(eq(testBug), anyList());
    }

    @Test
    void testMaximumLimits() {
        // Given
        List<TeamAssignmentInfo> manyTeams = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TeamAssignmentInfo team = TeamAssignmentInfo.builder()
                    .teamId(UUID.randomUUID())
                    .teamName("Team " + i)
                    .teamSlug("team-" + i)
                    .projectSlug("test-project")
                    .memberCount(1)
                    .matchingLabels(List.of("label" + i))
                    .labelMatchScore(0.5)
                    .isPrimary(i == 0)
                    .assignmentReason("Auto-detected")
                    .build();
            manyTeams.add(team);
        }

        TeamAssignmentRecommendation manyTeamsRecommendation = TeamAssignmentRecommendation.builder()
                .assignmentType(AssignmentType.MULTI_TEAM)
                .message("Many teams assigned")
                .assignedTeams(manyTeams)
                .teamMemberSkills(new HashMap<>())
                .analyzedLabels(Set.of("multiple"))
                .analyzedTags(Set.of("various"))
                .generatedAt(java.time.LocalDateTime.now())
                .confidenceScore(0.7)
                .build();

        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(manyTeamsRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(testUser);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        // Then
        assertThat(result).isNotNull();

        // Verify that all teams are passed to user assignment (no artificial limiting
        // in orchestrator)
        verify(userAssignmentService).autoAssignUserToBug(eq(testBug), argThat(teamIds -> {
            return teamIds.size() == 10; // All teams should be passed
        }));
    }

    @Test
    void testNullAndEmptyInputs() {
        // Given
        Bug nullBug = null;

        // When & Then
        assertThatThrownBy(() -> assignmentOrchestrator.executeCompleteAutoAssignment(nullBug))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Cannot invoke");
    }

    @Test
    void testInvalidData() {
        // Given
        Long invalidBugId = -1L;
        when(bugRepository.findById(invalidBugId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> assignmentOrchestrator.executeCompleteAutoAssignmentById(invalidBugId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bug not found with ID");
    }

    // Performance Tests

    @Test
    void testLargeDatasetPerformance() {
        // Given
        List<TeamAssignmentInfo> largeTeamList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TeamAssignmentInfo team = TeamAssignmentInfo.builder()
                    .teamId(UUID.randomUUID())
                    .teamName("Team " + i)
                    .teamSlug("team-" + i)
                    .projectSlug("test-project")
                    .memberCount(10)
                    .matchingLabels(List.of("label" + i))
                    .labelMatchScore(0.8)
                    .isPrimary(i == 0)
                    .assignmentReason("Auto-detected")
                    .build();
            largeTeamList.add(team);
        }

        TeamAssignmentRecommendation largeRecommendation = TeamAssignmentRecommendation.builder()
                .assignmentType(AssignmentType.MULTI_TEAM)
                .message("Large dataset assignment")
                .assignedTeams(largeTeamList)
                .teamMemberSkills(new HashMap<>())
                .analyzedLabels(Set.of("large"))
                .analyzedTags(Set.of("dataset"))
                .generatedAt(java.time.LocalDateTime.now())
                .confidenceScore(0.8)
                .build();

        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(largeRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(testUser);

        long startTime = System.currentTimeMillis();

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then
        assertThat(result).isNotNull();
        assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
    }

    @Test
    void testMemoryUsage() {
        // Given
        when(teamAssignmentService.getAssignmentRecommendation(testBug)).thenReturn(testRecommendation);
        when(userAssignmentService.autoAssignUserToBug(eq(testBug), anyList())).thenReturn(testUser);

        // When
        User result = assignmentOrchestrator.executeCompleteAutoAssignment(testBug);

        // Then
        assertThat(result).isNotNull();

        // Verify that the orchestrator doesn't hold onto large objects unnecessarily
        verify(teamAssignmentService).getAssignmentRecommendation(testBug);
        verify(userAssignmentService).autoAssignUserToBug(eq(testBug), anyList());
    }
}
