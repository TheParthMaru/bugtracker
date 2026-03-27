# Teams Module Specification

## Overview

The Teams module provides collaborative team management functionality within the bug tracker application. Teams are organized as sub-modules within projects, enabling structured collaboration and role-based access control. The module supports both standalone team operations and project-scoped team management, with comprehensive membership workflows and integration with the bug assignment system.

## Business Requirements

### Core Concept

- A **Team** is a collaborative unit within a project that groups users for organized work
- Teams are project-scoped and cannot exist independently of projects
- Teams support role-based access control with ADMIN and MEMBER roles
- Teams integrate with the bug assignment system for automated task distribution
- Teams provide structured collaboration with member management and permissions

### User Stories

#### As a Project Admin

- I can create teams within my project to organize work effectively
- I can assign project members to teams for structured collaboration
- I can manage team settings and member roles
- I can view all teams in my project with member statistics
- I can delete teams when they are no longer needed

#### As a Team Admin

- I can manage team members and their roles
- I can add or remove members from my team
- I can update team information and description
- I can promote or demote team members
- I can view team activity and member engagement

#### As a Team Member

- I can view my team details and member list
- I can see my role and permissions within the team
- I can leave teams I'm a member of
- I can participate in team-assigned bug resolution
- I can view team statistics and performance metrics

#### As a Project Member

- I can view all teams in projects I'm a member of
- I can request to join teams within my projects
- I can see team assignments for bugs I'm working on
- I can collaborate with team members on bug resolution

## Data Model

### Team Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/Team.java`](backend/src/main/java/com/pbm5/bugtracker/entity/Team.java)

The Team entity represents a collaborative unit within a project:

- **Project association**: Mandatory project_id reference for project-scoped organization
- **Unique identification**: UUID primary key with project-scoped name and slug constraints
- **Team metadata**: Name, description, and URL-friendly slug generation
- **Ownership tracking**: Created by user reference and audit timestamps
- **Relationship management**: One-to-many relationships with team members
- **Data validation**: Comprehensive validation rules for all fields

### TeamMember Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/TeamMember.java`](backend/src/main/java/com/pbm5/bugtracker/entity/TeamMember.java)

Manages user membership within teams with role-based access:

- **Membership tracking**: User-team relationship with role assignment
- **Role management**: ADMIN and MEMBER roles with different capabilities
- **Audit trail**: Joined timestamp and added by user tracking
- **Relationship integrity**: Composite unique constraint on team_id and user_id
- **Utility methods**: Role checking and promotion/demotion capabilities

### TeamRole Enum

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/TeamRole.java`](backend/src/main/java/com/pbm5/bugtracker/entity/TeamRole.java)

Defines user capabilities within teams:

- **ADMIN**: Full team management, member management, team modification rights
- **MEMBER**: Basic team access, bug assignment participation, collaboration rights

## Database Schema

### Core Tables

#### Teams Table

**Migration**: [`V2__Create_teams_table.sql`](backend/src/main/resources/db/migration/V2__Create_teams_table.sql)

The teams table stores core team information with:

- **UUID primary key** for distributed system compatibility
- **Project association** with mandatory project_id foreign key
- **Unique constraints** on project-scoped name and team_slug for data integrity
- **Creator tracking** with created_by user reference
- **Audit timestamps** for creation and modification tracking
- **Data validation constraints** ensuring proper slug format and non-empty values

#### Team Members Table

**Migration**: [`V3__Create_team_members_table.sql`](backend/src/main/resources/db/migration/V3__Create_team_members_table.sql)

The team_members table manages user membership with:

- **Composite unique constraint** on team_id and user_id
- **Role-based access control** with ADMIN/MEMBER roles
- **Membership tracking** with joined_at and added_by fields
- **Cascade deletion** when teams are removed
- **Data consistency constraints** ensuring valid role values

#### Project-Teams Integration

**Migration**: [`V8__Migrate_teams_to_project_submodule.sql`](backend/src/main/resources/db/migration/V8__Migrate_teams_to_project_submodule.sql)

The integration migration establishes:

- **Project association** with project_id column and foreign key constraints
- **Project-scoped uniqueness** with unique constraints on project_id + name/slug
- **Slug format updates** to include project prefix for URL structure
- **Performance indexes** for project-based team queries
- **Data migration** for existing teams to project context

#### Bug-Team Assignments

**Migration**: [`V23__Add_team_assignments.sql`](backend/src/main/resources/db/migration/V23__Add_team_assignments.sql)

The bug_team_assignments table enables:

- **Team-bug relationships** for automated assignment system
- **Primary team designation** with is_primary flag for main assignment
- **Assignment tracking** with assigned_at and assigned_by fields
- **Performance optimization** with strategic indexes for assignment queries

## API Endpoints

### Standalone Teams

**Base URL**: `/api/bugtracker/v1/teams`

#### GET /api/bugtracker/v1/teams

- **Purpose**: List all teams with optional search and pagination
- **Auth**: Required
- **Query Parameters**:
  - `search` (optional): Search term for team names/descriptions
  - `page` (optional): Page number (default: 0)
  - `size` (optional): Page size (default: 20)
  - `sort` (optional): Sort field and direction
- **Response**: Paginated list of teams with user context

#### GET /api/bugtracker/v1/teams/{teamId}

- **Purpose**: Get team details by ID
- **Auth**: Required
- **Response**: Team details with member information and user context

#### GET /api/bugtracker/v1/teams/teams/{teamSlug}

- **Purpose**: Get team details by slug
- **Auth**: Required
- **Response**: Team details with member information and user context

### Project-Scoped Teams

**Base URL**: `/api/bugtracker/v1/projects/{projectSlug}/teams`

#### GET /api/bugtracker/v1/projects/{projectSlug}/teams

- **Purpose**: Get teams within a project
- **Auth**: Required (Project members only)
- **Query Parameters**:
  - `search` (optional): Search term for team names/descriptions
  - `page` (optional): Page number
  - `size` (optional): Page size
  - `sortBy` (optional): Sort field
  - `sortDir` (optional): Sort direction (asc/desc)
- **Response**: Paginated list of teams within the project

#### POST /api/bugtracker/v1/projects/{projectSlug}/teams

- **Purpose**: Create a team within a project
- **Auth**: Required (Project admin only)
- **Body**: `{ name, description }`
- **Business Logic**:
  - Validate project admin permissions
  - Generate project-scoped slug with conflict resolution
  - Creator becomes team admin automatically
  - Validate team name uniqueness within project
- **Response**: Created team with project context

#### GET /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}

- **Purpose**: Get specific team within project
- **Auth**: Required (Project members only)
- **Response**: Team details with project context and member information

#### PUT /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}

- **Purpose**: Update team within project
- **Auth**: Required (Project admin or team admin)
- **Body**: `{ name?, description? }`
- **Business Logic**:
  - Validate admin permissions (project or team level)
  - Handle slug regeneration when name changes
  - Check for name conflicts within project
- **Response**: Updated team

#### DELETE /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}

- **Purpose**: Delete team within project
- **Auth**: Required (Project admin or team admin)
- **Business Logic**:
  - Validate admin permissions
  - Cascade delete team members
  - Handle bug-team assignment cleanup
- **Response**: Success confirmation

### Team Membership Management

#### GET /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/members

- **Purpose**: Get team members within project
- **Auth**: Required (Project members only)
- **Response**: Paginated list of team members with roles

#### POST /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/members

- **Purpose**: Add member to team within project
- **Auth**: Required (Project admin or team admin)
- **Body**: `{ userId, role }`
- **Business Logic**:
  - Validate admin permissions
  - Ensure user is project member
  - Check for existing team membership
  - Set default role if not specified
- **Response**: Success confirmation

#### PUT /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/members/{userId}

- **Purpose**: Update team member role within project
- **Auth**: Required (Project admin or team admin)
- **Body**: `{ role }`
- **Business Logic**:
  - Validate admin permissions
  - Check for last admin protection
  - Update member role
- **Response**: Success confirmation

#### DELETE /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/members/{userId}

- **Purpose**: Remove member from team within project
- **Auth**: Required (Project admin or team admin)
- **Business Logic**:
  - Validate admin permissions
  - Check for last admin protection
  - Remove team membership
- **Response**: Success confirmation

#### POST /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/leave

- **Purpose**: Leave team within project
- **Auth**: Required
- **Business Logic**:
  - Validate user is team member
  - Check for last admin protection
  - Remove team membership
- **Response**: Success confirmation

## Integration with Projects Module

### Project-Teams Relationship

Teams are tightly integrated with projects as sub-modules:

- **Mandatory association**: Teams cannot exist without project context
- **Project-scoped uniqueness**: Team names and slugs are unique within projects
- **Permission inheritance**: Project membership required for team access
- **Admin control**: Project admins control team creation permissions

### Access Control Integration

- **Project member requirement**: Users must be project members to access teams
- **Project admin privileges**: Project admins can create and manage all teams
- **Team admin privileges**: Team admins can manage their specific teams
- **Cascading permissions**: Project deletion removes associated teams

### Slug Generation Integration

- **Project prefix**: Team slugs include project slug prefix
- **Format**: `{project-slug}-{team-name-slug}`
- **Conflict resolution**: Handles duplicate slugs within project scope
- **URL structure**: Enables project-scoped team URLs

## Frontend Implementation

### Pages/Routes

**Location**: `frontend/src/pages/`

- `/teams` - Teams listing with project selection requirement
- `/projects/{projectSlug}/teams` - Project-scoped teams management
- `/projects/{projectSlug}/teams/{teamSlug}` - Team details and member management

### Key Components

**Location**: `frontend/src/components/teams/`

- `TeamsTable` - Display teams in table/list view with actions
- `CreateTeamModal` - Team creation form with validation
- `TeamCard` - Display team information in card format
- `TeamMemberList` - Display team members with role management
- `RoleBadge` - Display user role in team
- `TeamActionMenu` - Team management actions menu
- `TeamEmptyState` - Empty state for teams without data

### Service Layer

**File**: [`frontend/src/services/teamService.ts`](frontend/src/services/teamService.ts)

The TeamService provides comprehensive API integration for:

- **Team CRUD operations**: Create, read, update, delete teams with validation
- **Membership management**: Add, remove, update roles for team members
- **Project integration**: Manage teams within project context
- **Search and filtering**: Advanced query capabilities with pagination
- **Error handling**: Comprehensive error management and user feedback

### Type Definitions

**File**: [`frontend/src/types/team.ts`](frontend/src/types/team.ts)

The team types define the data structures for:

- **Team interface**: Core team data with project context and member counts
- **TeamMember interface**: Membership details with user information and roles
- **TeamRole enum**: ADMIN, MEMBER roles with different capabilities
- **Project integration types**: Project-scoped team operations and responses
- **UI component types**: Props and interfaces for team components

### State Management

The frontend uses React hooks for state management with caching integration:

- **Team lists**: Cached team collections with search and pagination
- **Current team**: Active team context with member information
- **User teams**: Personal team memberships and roles
- **Project context**: Project-scoped team operations
- **Loading states**: UI feedback for async operations
- **Error handling**: Centralized error state management

## Security & Authorization

### Access Control Matrix

| Action              | Anonymous | User | Project Member | Project Admin | Team Admin |
| ------------------- | --------- | ---- | -------------- | ------------- | ---------- |
| View teams list     | No        | No   | Yes            | Yes           | Yes        |
| View team details   | No        | No   | Yes            | Yes           | Yes        |
| Create team         | No        | No   | No             | Yes           | No         |
| Update team         | No        | No   | No             | Yes           | Yes        |
| Delete team         | No        | No   | No             | Yes           | Yes        |
| View team members   | No        | No   | Yes            | Yes           | Yes        |
| Add team members    | No        | No   | No             | Yes           | Yes        |
| Remove team members | No        | No   | No             | Yes           | Yes        |
| Update member roles | No        | No   | No             | Yes           | Yes        |
| Leave team          | No        | No   | Yes            | Yes           | Yes        |

### Business Rules

1. **Project Association**: Teams must be associated with a project
2. **Project Admin Control**: Only project admins can create teams
3. **Team Admin Privileges**: Team admins can manage their teams
4. **Last Admin Protection**: Cannot remove the last team admin
5. **Project Membership**: Users must be project members to access teams
6. **Cascading Deletion**: Deleting project removes associated teams
7. **Role Hierarchy**: ADMIN > MEMBER (no access for non-members)
8. **Slug Uniqueness**: Team slugs must be unique within project scope

## Technical Considerations

### Slug Generation

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/Team.java`](backend/src/main/java/com/pbm5/bugtracker/entity/Team.java)

The slug generation system provides project-scoped identifiers:

- **Project prefix**: Includes project slug in team slug format
- **Name transformation**: Converts team names to lowercase, hyphenated format
- **Conflict resolution**: Handles duplicate slugs within project scope
- **Validation**: Ensures proper format with regex validation
- **URL structure**: Enables project-scoped team URLs

### Caching Strategy

**Backend**: Spring Cache with method-level caching
**Frontend**: Custom cache service with TTL

- Cache team lists with 5-minute TTL
- Cache team details with 1-minute TTL
- Invalidate on membership changes
- Bypass cache option for real-time updates

### Performance Optimizations

- Implement pagination for large team lists
- Add search and filtering capabilities
- Use database indexes for common queries
- Lazy loading for team members
- Composite indexes for project-team relationships

### Error Handling

**Location**: `backend/src/main/java/com/pbm5/bugtracker/exception/`

- `TeamNotFoundException`: Team not found
- `TeamAccessDeniedException`: Insufficient permissions
- `TeamNameConflictException`: Name already exists in project
- `DuplicateTeamMembershipException`: User already has membership
- `InvalidTeamOperationException`: General validation errors
- `LastTeamAdminException`: Cannot remove last team admin

### Business Logic Implementation

**Location**: `backend/src/main/java/com/pbm5/bugtracker/service/`

- **TeamService**: Core team management operations
- **TeamSecurityService**: Permission checking and validation
- **SlugService**: Unique slug generation with conflict resolution
- **TeamNotificationEventListener**: Event handling for team operations

## Integration with Bug Assignment System

### Team-Bug Assignments

**File**: [`backend/src/main/resources/db/migration/V23__Add_team_assignments.sql`](backend/src/main/resources/db/migration/V23__Add_team_assignments.sql)

The bug assignment system integrates with teams through:

- **Team assignment tracking**: Records which teams are assigned to bugs
- **Primary team designation**: Identifies the main team responsible for a bug
- **Assignment history**: Tracks when and by whom teams were assigned
- **Auto-assignment support**: Enables automated team assignment based on skills and workload

### Assignment Workflow

1. **Bug creation**: Teams can be assigned during bug creation
2. **Manual assignment**: Project admins can assign teams to existing bugs
3. **Auto-assignment**: System can automatically assign teams based on criteria
4. **Team notification**: Team members are notified of new assignments
5. **Workload balancing**: System considers team capacity for assignments

## Implementation Status

### Completed Features

- **Team Management**: Full CRUD operations with project integration
- **Membership Workflow**: Role-based access control with admin/member roles
- **Project Integration**: Teams as project sub-modules with proper access control
- **Frontend**: Complete UI with caching and state management
- **Database**: Optimized schema with indexes and constraints
- **API**: RESTful endpoints with comprehensive error handling
- **Bug Integration**: Team-bug assignment system for automated task distribution

### Current Capabilities

- Project-scoped team creation and management
- Role-based team membership with admin/member roles
- Team member management with add/remove/update operations
- Search and filtering with pagination
- Project integration with proper access control
- Bug assignment integration for automated task distribution
- Comprehensive audit logging and error handling
