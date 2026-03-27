# Auto Assignment System Specification

## Overview

The Auto Assignment System is an advanced feature that automatically assigns bugs to teams and users based on intelligent matching algorithms. The system uses a two-phase approach: first assigning bugs to appropriate teams based on label matching, then assigning specific users within those teams based on skill matching and workload balancing. The system employs sophisticated algorithms for team-label matching, skill-based user selection, and workload distribution to ensure optimal bug assignment.

## Business Requirements

### Core Concept

- **Auto Assignment** is the process of automatically assigning bugs to teams and users without manual intervention
- The system uses a two-phase approach: team assignment followed by user assignment
- Team assignment is based on bug labels and team specialization matching
- User assignment is based on skill matching, workload balancing, and availability
- The system provides intelligent recommendations with confidence scores and detailed reasoning

### User Stories

#### As a Bug Reporter

- I can create bugs and have them automatically assigned to appropriate teams
- I can see which teams and users are recommended for my bug
- I can understand why specific teams and users were selected
- I can override automatic assignments if needed
- I can see the confidence score for assignment recommendations

#### As a Project Manager

- I can configure auto assignment settings for my project
- I can view assignment analytics and effectiveness metrics
- I can monitor workload distribution across team members
- I can adjust assignment algorithms and thresholds
- I can track assignment success rates and user satisfaction

#### As a Team Lead

- I can see which bugs are automatically assigned to my team
- I can view team member workload and availability
- I can understand the skill matching criteria used for assignments
- I can provide feedback on assignment quality
- I can manage team capacity and skill development

#### As a Developer

- I can receive bugs that match my skills and current workload
- I can see why I was selected for specific bug assignments
- I can view my current assignment load and availability
- I can provide feedback on assignment relevance
- I can update my skills to improve future assignments

## Data Model

### Core Services

#### AssignmentOrchestrator Service

**File**: [`backend/src/main/java/com/pbm5/bugtracker/service/AssignmentOrchestrator.java`](backend/src/main/java/com/pbm5/bugtracker/service/AssignmentOrchestrator.java)

The AssignmentOrchestrator coordinates the complete auto assignment workflow:

- **Workflow coordination**: Manages the sequence of team assignment followed by user assignment
- **Error handling**: Provides comprehensive error handling and rollback capabilities
- **Single entry point**: Offers a unified interface for the complete assignment process
- **Logging and monitoring**: Tracks assignment success rates and performance metrics

#### TeamAssignmentService Service

**File**: [`backend/src/main/java/com/pbm5/bugtracker/service/TeamAssignmentService.java`](backend/src/main/java/com/pbm5/bugtracker/service/TeamAssignmentService.java)

The TeamAssignmentService handles team assignment logic:

- **Label matching**: Intelligent matching of bug labels to team specializations
- **Team scoring**: Calculates match scores based on team names, descriptions, and specializations
- **Multi-team assignment**: Supports assigning bugs to multiple teams for complex issues
- **Recommendation generation**: Provides detailed assignment recommendations with confidence scores

#### UserAssignmentService Service

**File**: [`backend/src/main/java/com/pbm5/bugtracker/service/UserAssignmentService.java`](backend/src/main/java/com/pbm5/bugtracker/service/UserAssignmentService.java)

The UserAssignmentService handles user assignment logic:

- **Skill matching**: Matches user skills to bug tags using exact and partial matching
- **Workload balancing**: Considers current user workload for optimal distribution
- **Availability scoring**: Calculates user availability based on current assignments
- **Cross-team selection**: Finds the best user across all assigned teams

### Supporting Entities

#### BugTeamAssignment Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/BugTeamAssignment.java`](backend/src/main/java/com/pbm5/bugtracker/entity/BugTeamAssignment.java)

The BugTeamAssignment entity manages bug-team relationships:

- **Bug-team mapping**: Links bugs to assigned teams with assignment metadata
- **Primary team designation**: Identifies the primary responsible team
- **Assignment tracking**: Records who made the assignment and when
- **Audit trail**: Maintains complete assignment history

#### TeamAssignmentRecommendation DTO

**File**: [`backend/src/main/java/com/pbm5/bugtracker/dto/TeamAssignmentRecommendation.java`](backend/src/main/java/com/pbm5/bugtracker/dto/TeamAssignmentRecommendation.java)

The TeamAssignmentRecommendation provides comprehensive assignment information:

- **Assignment details**: Team information, member skills, and match scores
- **Confidence scoring**: Overall confidence in the assignment recommendation
- **Assignment type**: Single team, multi-team, or no team found
- **Detailed reasoning**: Explanation of why teams were selected

## Database Schema

### Core Tables

#### Bug Team Assignments Table

**Migration**: [`V23__Add_team_assignments.sql`](backend/src/main/resources/db/migration/V23__Add_team_assignments.sql)

The bug_team_assignments table manages bug-team relationships:

- **UUID primary key** for efficient relationship management
- **Bug and team references** with foreign key constraints
- **Primary team designation** with is_primary boolean flag
- **Assignment tracking** with assigned_by user reference and assigned_at timestamp
- **Unique constraints** preventing duplicate assignments and ensuring one primary team per bug

#### Performance Indexes

**Migration**: [`V23__Add_team_assignments.sql`](backend/src/main/resources/db/migration/V23__Add_team_assignments.sql)

Strategic indexes optimize assignment queries:

- **Bug-based indexes**: bug_id for efficient bug assignment lookups
- **Team-based indexes**: team_id for team assignment analysis
- **User-based indexes**: assigned_by for assignment audit trails
- **Primary team indexes**: Combined bug_id and is_primary for primary team queries
- **Timestamp indexes**: assigned_at for assignment history analysis

## Assignment Algorithms

### Team Assignment Algorithm

The team assignment algorithm uses intelligent label matching to find appropriate teams:

#### Label Matching Process

1. **Label extraction**: Extract bug labels from the bug entity
2. **Team retrieval**: Get all teams within the project scope
3. **Score calculation**: Calculate match scores for each team
4. **Threshold filtering**: Filter teams above minimum match threshold (0.3)
5. **Ranking and selection**: Rank teams by score and select top matches

#### Team Scoring Algorithm

**Formula**: `team_score = Σ(label_match_score_i) / total_labels`

**Label Match Scoring**:

- **Exact name match**: 1.0 (team name contains label name)
- **Partial name match**: 0.8 (bidirectional partial matching)
- **Description match**: 0.6 (team description contains label name)
- **Fuzzy match**: 0.7 (common variations and synonyms)

#### Fuzzy Matching Rules

The system includes intelligent fuzzy matching for common variations:

- **Frontend variations**: frontend, front-end, front end, ui, ux, client
- **Backend variations**: backend, back-end, back end, server, api
- **Database variations**: database, db, data, sql
- **DevOps variations**: devops, dev-ops, deployment, infrastructure
- **Testing variations**: testing, test, qa, quality
- **Security variations**: security, sec, infosec

#### Multi-Team Assignment

- **Maximum teams**: Limited to 5 teams per bug to prevent overwhelming assignments
- **Primary team**: First team (highest score) designated as primary
- **Confidence calculation**: Weighted combination of team match and skill match scores

### User Assignment Algorithm

The user assignment algorithm finds the best user within assigned teams:

#### User Scoring Formula

**Formula**: `user_score = (skill_score × 0.7) + (availability_score × 0.3)`

#### Skill Matching Algorithm

**Exact Match Scoring**:

- **Exact skill match**: 1.0 point per matching tag
- **Partial skill match**: 0.5 points per matching tag
- **Final score**: `(exact_matches + partial_matches × 0.5) / total_tags`

**Skill Matching Process**:

1. **Normalization**: Convert skills and tags to lowercase for comparison
2. **Exact matching**: Check for identical skill-tag pairs
3. **Partial matching**: Check for substring matches in either direction
4. **Score calculation**: Apply weighted scoring for exact vs partial matches

#### Workload Balancing Algorithm

**Availability Score Formula**: `availability = max(0, 1 - (current_assignments / max_assignments))`

**Configuration**:

- **Maximum assignments**: 5 bugs per user for availability calculation
- **Project scope**: Availability calculated within project context
- **Score range**: 0.0 (overloaded) to 1.0 (completely available)

#### Cross-Team User Selection

1. **Team iteration**: Evaluate users across all assigned teams
2. **Best user selection**: Select user with highest combined score
3. **Score comparison**: Compare users from different teams
4. **Final assignment**: Assign bug to user with highest overall score

### Assignment Orchestration

The AssignmentOrchestrator manages the complete workflow:

#### Workflow Steps

1. **Team Assignment**: Auto-assign teams based on bug labels
2. **Team Validation**: Ensure at least one team is assigned
3. **User Assignment**: Find best user within assigned teams
4. **Result Logging**: Log assignment results and confidence scores
5. **Error Handling**: Handle failures gracefully with rollback

#### Error Handling Strategy

- **Graceful degradation**: Continue with partial assignments if possible
- **Rollback capability**: Undo assignments if critical failures occur
- **Comprehensive logging**: Track all assignment decisions and failures
- **User notification**: Inform users of assignment results and issues

## API Endpoints

### Team Assignment

**Base URL**: `/api/bugtracker/v1/projects/{projectSlug}/bugs`

#### POST /api/bugtracker/v1/projects/{projectSlug}/bugs/team-assignment-recommendation

- **Purpose**: Get team assignment recommendation for a bug
- **Auth**: Required (Project members only)
- **Body**: `{ title, description, type, priority, labelIds, tags }`
- **Response**: TeamAssignmentRecommendation with detailed team suggestions
- **Business Logic**:
  - Analyze bug labels and tags for team matching
  - Calculate team match scores using intelligent algorithms
  - Find skilled team members for each recommended team
  - Generate confidence scores and assignment reasoning
  - Return comprehensive recommendation with assignment details

#### POST /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}/teams

- **Purpose**: Assign teams to a bug
- **Auth**: Required (Project admin or team admin)
- **Body**: `{ teamIds, isPrimaryTeamId? }`
- **Response**: Success confirmation with assignment details
- **Business Logic**:
  - Validate team existence and project membership
  - Remove existing team assignments for the bug
  - Create new team assignments with primary team designation
  - Log assignment actions for audit trail

### User Assignment

#### POST /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}/assign

- **Purpose**: Auto-assign user to a bug
- **Auth**: Required (Project members only)
- **Response**: Assigned user information with assignment score
- **Business Logic**:
  - Find best user across assigned teams
  - Calculate skill matching and availability scores
  - Select user with highest combined score
  - Update bug assignment and log assignment decision

### Assignment Analytics

#### GET /api/bugtracker/v1/projects/{projectSlug}/assignments/analytics

- **Purpose**: Get assignment analytics and effectiveness metrics
- **Auth**: Required (Project members only)
- **Response**: Assignment statistics and performance metrics
- **Business Logic**:
  - Calculate assignment success rates
  - Analyze workload distribution across users
  - Generate skill matching effectiveness metrics
  - Provide team assignment performance data

## Integration with Other Modules

### Bugs Module Integration

The auto assignment system integrates with the bugs module:

- **Bug creation**: Automatic assignment during bug creation process
- **Bug updates**: Re-evaluation of assignments when bug content changes
- **Bug lifecycle**: Assignment updates based on bug status changes
- **Assignment tracking**: Complete assignment history in bug data

### Teams Module Integration

The system integrates with teams for assignment logic:

- **Team discovery**: Find teams based on project membership
- **Team specialization**: Match teams based on names and descriptions
- **Member analysis**: Analyze team member skills and availability
- **Team capacity**: Consider team capacity for assignment decisions

### Projects Module Integration

The system integrates with projects for scope and configuration:

- **Project scope**: All assignments are project-scoped
- **Access control**: Project membership required for assignment access
- **Configuration**: Project-specific assignment settings and thresholds
- **Analytics**: Project-level assignment performance metrics

### Gamification Integration

The system integrates with gamification for user engagement:

- **Assignment points**: Award points for successful bug assignments
- **Skill development**: Track skill improvement through assignments
- **Workload achievements**: Recognize balanced workload management
- **Team collaboration**: Reward effective team-based assignments

## Frontend Implementation

### Pages/Routes

**Location**: `frontend/src/pages/`

- `/projects/{projectSlug}/bugs/create` - Bug creation with auto assignment
- `/projects/{projectSlug}/bugs/{bugId}` - Bug details with assignment management

### Key Components

**Location**: `frontend/src/components/bugs/`

- `TeamAssignmentSection` - Team assignment interface with recommendations
- `UserAssignmentSection` - User assignment interface with skill matching
- `AssignmentRecommendation` - Display assignment recommendations and reasoning
- `WorkloadIndicator` - Show user workload and availability status
- `SkillMatchDisplay` - Display skill matching details and scores

### Service Integration

**File**: [`frontend/src/services/bugService.ts`](frontend/src/services/bugService.ts)

The BugService provides comprehensive auto assignment API integration:

- **Team recommendations**: Get team assignment recommendations
- **User assignment**: Auto-assign users to bugs
- **Assignment management**: Manage team and user assignments
- **Analytics integration**: Retrieve assignment performance data

### Type Definitions

**File**: [`frontend/src/types/bug.ts`](frontend/src/types/bug.ts)

The bug types define data structures for:

- **TeamAssignmentRecommendation**: Comprehensive team assignment suggestions
- **TeamAssignmentInfo**: Individual team assignment details
- **TeamMemberSkillMatch**: User skill matching information
- **AssignmentType**: Enum for assignment types (single, multi, none)

### State Management

The frontend uses React hooks for assignment state management:

- **Assignment recommendations**: Cached team and user recommendations
- **Loading states**: UI feedback for async assignment operations
- **Error handling**: Centralized error state management
- **Real-time updates**: Live assignment updates and notifications

## Security & Authorization

### Access Control Matrix

| Action                          | Anonymous | User | Project Member | Project Admin | Team Admin |
| ------------------------------- | --------- | ---- | -------------- | ------------- | ---------- |
| View assignment recommendations | No        | No   | Yes            | Yes           | Yes        |
| Auto-assign teams               | No        | No   | No             | Yes           | Yes        |
| Auto-assign users               | No        | No   | Yes            | Yes           | Yes        |
| Override assignments            | No        | No   | No             | Yes           | Yes        |
| View assignment analytics       | No        | No   | Yes            | Yes           | Yes        |

### Business Rules

1. **Project Association**: All assignments are project-scoped
2. **Team Membership**: Users can only be assigned to teams they belong to
3. **Assignment Override**: Only project admins and team admins can override assignments
4. **Workload Limits**: System respects maximum assignment limits for workload balancing
5. **Skill Validation**: Assignment recommendations based on verified user skills
6. **Audit Trail**: All assignment decisions are logged with user and timestamp information

## Technical Considerations

### Performance Optimizations

- **Caching**: Redis caching for team and user recommendations
- **Database indexes**: Strategic indexes for efficient assignment queries
- **Batch processing**: Efficient assignment processing for multiple bugs
- **Lazy loading**: Progressive loading of assignment recommendations
- **Query optimization**: Optimized database queries for assignment algorithms

### Algorithm Configuration

- **Configurable thresholds**: Adjustable minimum match scores for teams and users
- **Weight customization**: Configurable weights for skill vs availability scoring
- **Team limits**: Configurable maximum teams per bug assignment
- **Workload limits**: Configurable maximum assignments per user
- **Fuzzy matching**: Configurable fuzzy matching rules and variations

### Error Handling

- **Graceful degradation**: System continues to work with partial assignment failures
- **Fallback mechanisms**: Fallback to manual assignment when auto assignment fails
- **Comprehensive logging**: Detailed logging of assignment decisions and failures
- **User feedback**: Clear error messages and assignment status updates
- **Rollback capability**: Ability to undo assignments if critical failures occur

### Scalability Considerations

- **Horizontal scaling**: Stateless design supports multiple server instances
- **Database optimization**: Efficient queries and indexes for large-scale assignments
- **Cache distribution**: Redis clustering for high availability
- **Load balancing**: Even distribution of assignment calculation workload
- **Resource management**: Memory and CPU optimization for large-scale operations

## Implementation Status

### Completed Features

- **Two-phase assignment**: Team assignment followed by user assignment
- **Intelligent team matching**: Label-based team matching with fuzzy logic
- **Skill-based user selection**: Exact and partial skill matching algorithms
- **Workload balancing**: Availability scoring based on current assignments
- **Assignment orchestration**: Complete workflow coordination and error handling
- **Frontend integration**: Complete UI for assignment recommendations and management
- **Analytics integration**: Assignment performance metrics and effectiveness tracking
- **Configuration management**: Project-specific assignment settings and thresholds

### Current Capabilities

- Automatic team assignment based on bug labels with intelligent matching
- Automatic user assignment based on skill matching and workload balancing
- Comprehensive assignment recommendations with confidence scores
- Multi-team assignment support for complex bugs
- Real-time assignment updates and notifications
- Complete assignment audit trail and history tracking
- Assignment analytics and performance monitoring
- Configurable assignment algorithms and thresholds

## Future Enhancements

### Advanced Algorithms

- Machine learning-based assignment optimization
- Predictive assignment based on historical data
- Dynamic threshold adjustment based on assignment success
- Advanced workload prediction and capacity planning

### Enhanced User Experience

- Visual assignment flow and reasoning display
- Interactive assignment configuration interface
- Advanced assignment filtering and search
- Bulk assignment management capabilities

### System Improvements

- Real-time assignment updates via WebSocket
- Advanced caching strategies with intelligent invalidation
- Performance monitoring and optimization dashboards
- Integration with external assignment services

### Analytics Enhancements

- Predictive assignment analytics
- User behavior analysis and assignment preferences
- Advanced reporting and visualization
- A/B testing for assignment algorithm optimization
