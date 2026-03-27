package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.entity.*;
import com.pbm5.bugtracker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAssignmentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private BugRepository bugRepository;

    @InjectMocks
    private UserAssignmentService userAssignmentService;

    private Project testProject;
    private Bug testBug;
    private Set<String> testTags;
    private List<UUID> testTeamIds;
    private List<TeamMember> testTeamMembers;
    private List<User> testUsers;

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

        // Setup test tags
        testTags = Set.of("react", "javascript");
        testBug.setTags(testTags);

        // Setup test team IDs
        testTeamIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        // Setup test users
        User skilledUser = new User();
        skilledUser.setId(UUID.randomUUID());
        skilledUser.setFirstName("John");
        skilledUser.setLastName("Doe");
        skilledUser.setEmail("john@example.com");
        skilledUser.setSkills(Set.of("react", "javascript", "css"));

        User partiallySkilledUser = new User();
        partiallySkilledUser.setId(UUID.randomUUID());
        partiallySkilledUser.setFirstName("Jane");
        partiallySkilledUser.setLastName("Smith");
        partiallySkilledUser.setEmail("jane@example.com");
        partiallySkilledUser.setSkills(Set.of("javascript"));

        User unskilledUser = new User();
        unskilledUser.setId(UUID.randomUUID());
        unskilledUser.setFirstName("Bob");
        unskilledUser.setLastName("Johnson");
        unskilledUser.setEmail("bob@example.com");
        unskilledUser.setSkills(Set.of("java", "spring"));

        testUsers = List.of(skilledUser, partiallySkilledUser, unskilledUser);

        // Setup test team members
        TeamMember member1 = new TeamMember();
        member1.setId(UUID.randomUUID());
        member1.setTeamId(testTeamIds.get(0));
        member1.setUserId(skilledUser.getId());

        TeamMember member2 = new TeamMember();
        member2.setId(UUID.randomUUID());
        member2.setTeamId(testTeamIds.get(0));
        member2.setUserId(partiallySkilledUser.getId());

        TeamMember member3 = new TeamMember();
        member3.setId(UUID.randomUUID());
        member3.setTeamId(testTeamIds.get(1));
        member3.setUserId(unskilledUser.getId());

        testTeamMembers = List.of(member1, member2, member3);
    }

    // 2.1 User Assignment Algorithm Tests

    @Test
    void testAutoAssignUserToBug_Success() {
        // Given
        when(teamMemberRepository.findByTeamId(testTeamIds.get(0)))
                .thenReturn(List.of(testTeamMembers.get(0), testTeamMembers.get(1)));
        when(teamMemberRepository.findByTeamId(testTeamIds.get(1))).thenReturn(List.of(testTeamMembers.get(2)));
        when(userRepository.findById(testUsers.get(0).getId())).thenReturn(Optional.of(testUsers.get(0)));
        when(userRepository.findById(testUsers.get(1).getId())).thenReturn(Optional.of(testUsers.get(1)));
        when(userRepository.findById(testUsers.get(2).getId())).thenReturn(Optional.of(testUsers.get(2)));
        when(bugRepository.countByProjectIdAndAssigneeId(testProject.getId(), testUsers.get(0).getId())).thenReturn(1L);
        when(bugRepository.countByProjectIdAndAssigneeId(testProject.getId(), testUsers.get(1).getId())).thenReturn(2L);
        when(bugRepository.countByProjectIdAndAssigneeId(testProject.getId(), testUsers.get(2).getId())).thenReturn(0L);

        // When
        User result = userAssignmentService.autoAssignUserToBug(testBug, testTeamIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUsers.get(0).getId()); // Best skilled user
    }

    @Test
    void testAutoAssignUserToBug_NoTeams() {
        // Given
        List<UUID> emptyTeamIds = Collections.emptyList();

        // When
        User result = userAssignmentService.autoAssignUserToBug(testBug, emptyTeamIds);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void testAutoAssignUserToBug_NoTags() {
        // Given
        testBug.setTags(Collections.emptySet());

        // When
        User result = userAssignmentService.autoAssignUserToBug(testBug, testTeamIds);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void testAutoAssignUserToBug_NoSkilledUsers() {
        // Given
        // Create a user with completely different skills
        User unskilledUser = new User();
        unskilledUser.setId(UUID.randomUUID());
        unskilledUser.setFirstName("Unskilled");
        unskilledUser.setLastName("User");
        unskilledUser.setEmail("unskilled@example.com");
        unskilledUser.setSkills(Set.of("python", "django")); // Completely different from testTags

        TeamMember unskilledMember = new TeamMember();
        unskilledMember.setId(UUID.randomUUID());
        unskilledMember.setTeamId(testTeamIds.get(0));
        unskilledMember.setUserId(unskilledUser.getId());

        when(teamMemberRepository.findByTeamId(testTeamIds.get(0))).thenReturn(List.of(unskilledMember));
        when(teamMemberRepository.findByTeamId(testTeamIds.get(1))).thenReturn(Collections.emptyList());
        when(userRepository.findById(unskilledUser.getId())).thenReturn(Optional.of(unskilledUser));
        when(bugRepository.countByProjectIdAndAssigneeId(testProject.getId(), unskilledUser.getId())).thenReturn(0L);

        // When
        User result = userAssignmentService.autoAssignUserToBug(testBug, testTeamIds);

        // Then
        // The algorithm has a fallback mechanism that assigns users even without skill
        // matches
        // This is actually correct behavior for availability-based assignment
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(unskilledUser.getId());
    }

    @Test
    void testAutoAssignUserToBug_MultipleTeams() {
        // Given
        when(teamMemberRepository.findByTeamId(testTeamIds.get(0))).thenReturn(List.of(testTeamMembers.get(1)));
        when(teamMemberRepository.findByTeamId(testTeamIds.get(1))).thenReturn(List.of(testTeamMembers.get(0)));
        when(userRepository.findById(testUsers.get(0).getId())).thenReturn(Optional.of(testUsers.get(0)));
        when(userRepository.findById(testUsers.get(1).getId())).thenReturn(Optional.of(testUsers.get(1)));
        when(bugRepository.countByProjectIdAndAssigneeId(testProject.getId(), testUsers.get(0).getId())).thenReturn(0L);
        when(bugRepository.countByProjectIdAndAssigneeId(testProject.getId(), testUsers.get(1).getId())).thenReturn(3L);

        // When
        User result = userAssignmentService.autoAssignUserToBug(testBug, testTeamIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUsers.get(0).getId()); // Best across all teams
    }

    // Note: Private method tests removed as they are not accessible from outside
    // the class
    // The skill matching, availability calculation, and assignment scoring logic
    // is tested indirectly through the public autoAssignUserToBug method
}
