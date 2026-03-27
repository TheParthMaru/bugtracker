# Projects Module Specification

## Overview

The Projects module serves as the foundational layer of the bug tracker application, similar to JIRA's project structure. All bug tracking activities are organized within projects, making this module critical for the application's core functionality. The module implements a comprehensive project management system with membership workflows, role-based access control, and team integration.

## Business Requirements

### Core Concept

- A **Project** is a logical container that groups teams, users, and bugs under a single organizational unit
- Projects are required for any bug tracking activities - no bugs can exist without an associated project
- Projects enable multi-tenancy while maintaining data isolation and proper access control
- Projects support approval-based membership workflows with role management
- Projects integrate with teams as sub-modules for organized collaboration

### User Stories

#### As a User

- I can view all available projects in the system (open-source nature)
- I can request to join any project with approval workflow
- I can see my project membership status and role
- I can leave projects I'm a member of (except if I'm the only admin)
- I can view project details and member information
- I can access teams within projects I'm a member of

#### As a Project Admin

- I can create new projects with name and description
- I can approve/reject user join requests
- I can manage project members and their roles
- I can update project details and settings
- I can delete projects (with proper safeguards)
- I can transfer admin rights to other members
- I can create and manage teams within my project
- I can view project statistics and member analytics

#### As a Project Member

- I can access project details and team information
- I can participate in teams I'm assigned to
- I can view project statistics and member lists
- I can leave the project voluntarily

## Data Model

### Project Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/Project.java`](backend/src/main/java/com/pbm5/bugtracker/entity/Project.java)

The Project entity serves as the central container for all project-related activities. It includes:

- **Unique identification**: UUID primary key with unique name and slug constraints
- **Project metadata**: Name, description, and URL-friendly slug generation
- **Ownership tracking**: Admin ID reference and soft delete capability
- **Relationship management**: One-to-many relationships with members, teams, and bugs
- **Audit trail**: Automatic creation and update timestamps
- **Data validation**: Comprehensive validation rules for all fields

### ProjectMember Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/ProjectMember.java`](backend/src/main/java/com/pbm5/bugtracker/entity/ProjectMember.java)

Manages user membership within projects with a complete approval workflow:

- **Membership lifecycle**: Tracks the entire approval process from request to final status
- **Role-based access**: ADMIN and MEMBER roles with different capabilities
- **Status management**: PENDING → ACTIVE/REJECTED workflow with admin oversight
- **Approval tracking**: Records who approved the membership and when
- **Audit capabilities**: Complete timestamp tracking for all membership actions

### Enums

**Files**:

- [`backend/src/main/java/com/pbm5/bugtracker/entity/ProjectRole.java`](backend/src/main/java/com/pbm5/bugtracker/entity/ProjectRole.java)
- [`backend/src/main/java/com/pbm5/bugtracker/entity/MemberStatus.java`](backend/src/main/java/com/pbm5/bugtracker/entity/MemberStatus.java)

**ProjectRole** defines user capabilities within projects:

- **ADMIN**: Full project management, member approval, project modification rights
- **MEMBER**: Basic project access, bug reporting, team participation
- **PENDING**: Temporary status during the approval process

**MemberStatus** manages the approval workflow:

- **PENDING**: Initial status requiring admin approval
- **ACTIVE**: Approved membership with full project access
- **REJECTED**: Denied membership with no project access

## Database Schema

### Core Tables

#### Projects Table

**Migration**: [`V4__Create_projects_table.sql`](backend/src/main/resources/db/migration/V4__Create_projects_table.sql)

The projects table stores core project information with:

- **UUID primary key** for distributed system compatibility
- **Unique constraints** on name and project_slug for data integrity
- **Foreign key reference** to users table for admin ownership
- **Soft delete capability** with is_active flag
- **Audit timestamps** for creation and modification tracking
- **Data validation constraints** ensuring proper slug format and non-empty values

#### Project Members Table

**Migration**: [`V5__Create_project_members_table.sql`](backend/src/main/resources/db/migration/V5__Create_project_members_table.sql)

The project_members table manages user membership with:

- **Composite unique constraint** on project_id and user_id
- **Role-based access control** with ADMIN/MEMBER roles
- **Approval workflow** with PENDING/ACTIVE/REJECTED statuses
- **Approval tracking** with approved_by and approved_at fields
- **Cascade deletion** when projects are removed
- **Data consistency constraints** ensuring approval data integrity

#### Performance Indexes

**Migration**: [`V6__Add_project_indexes.sql`](backend/src/main/resources/db/migration/V6__Add_project_indexes.sql)

Optimized database performance with strategic indexes on:

- **Project lookups**: slug, admin_id, name, and active status
- **Membership queries**: project_id, user_id, status, and role combinations
- **Composite indexes** for common query patterns like user-project relationships

## API Endpoints

### Project Management

**Base URL**: `/api/bugtracker/v1/projects`

#### GET /api/bugtracker/v1/projects

- **Purpose**: List all active projects with optional search and pagination
- **Auth**: Required
- **Query Parameters**:
  - `search` (optional): Search term for project names
  - `page` (optional): Page number (default: 0)
  - `size` (optional): Page size (default: 20)
  - `sort` (optional): Sort field and direction (e.g., "name,asc", "createdAt,desc")
- **Response**: Paginated list of projects with user context and member counts

#### POST /api/bugtracker/v1/projects

- **Purpose**: Create a new project
- **Auth**: Required
- **Body**: `{ name, description }`
- **Business Logic**:
  - Auto-generate slug from name with conflict resolution
  - Creator becomes admin automatically
  - Validate name uniqueness
  - Initialize default similarity configurations
- **Response**: Created project with user context

#### GET /api/bugtracker/v1/projects/{projectSlug}

- **Purpose**: Get project details by slug
- **Auth**: Required
- **Response**: Full project info with user's membership status and permissions

#### PUT /api/bugtracker/v1/projects/{projectSlug}

- **Purpose**: Update project details
- **Auth**: Required (Admin only)
- **Body**: `{ name?, description?, projectSlug? }`
- **Business Logic**:
  - Validate admin permissions
  - Handle slug regeneration when name changes
  - Check for name/slug conflicts

#### DELETE /api/bugtracker/v1/projects/{projectSlug}

- **Purpose**: Soft delete project
- **Auth**: Required (Admin only)
- **Business Logic**:
  - Validate admin permissions
  - Perform soft delete (set isActive = false)
  - Preserve data for audit purposes

#### GET /api/bugtracker/v1/projects/users/me/projects

- **Purpose**: Get current user's projects
- **Auth**: Required
- **Response**: Paginated list of projects where user is a member

#### GET /api/bugtracker/v1/projects/users/{userId}/projects

- **Purpose**: Get projects for a specific user
- **Auth**: Required
- **Response**: Paginated list of projects where specified user is a member

#### GET /api/bugtracker/v1/projects/admin/{userId}

- **Purpose**: Get projects where user is admin
- **Auth**: Required
- **Response**: Paginated list of projects where specified user has admin role

### Project Membership

#### POST /api/bugtracker/v1/projects/{projectSlug}/join

- **Purpose**: Request to join project
- **Auth**: Required
- **Body**: `{ message? }` (optional join request message)
- **Business Logic**:
  - Create pending membership request
  - Check for existing membership
  - Set status to PENDING, role to PENDING
- **Response**: Created membership with PENDING status

#### POST /api/bugtracker/v1/projects/{projectSlug}/leave

- **Purpose**: Leave project
- **Auth**: Required
- **Business Logic**:
  - Validate user is not the last admin
  - Remove membership record
- **Response**: 204 No Content

#### GET /api/bugtracker/v1/projects/{projectSlug}/members

- **Purpose**: List project members with filtering
- **Auth**: Required (Project members only)
- **Query Parameters**:
  - `status` (optional): Filter by membership status (ACTIVE, PENDING, REJECTED)
  - `page` (optional): Page number
  - `size` (optional): Page size
- **Response**: Paginated list of members with roles and statuses

#### GET /api/bugtracker/v1/projects/{projectSlug}/requests

- **Purpose**: List pending join requests
- **Auth**: Required (Admin only)
- **Response**: Paginated list of pending membership requests

#### POST /api/bugtracker/v1/projects/{projectSlug}/members/{userId}/approve

- **Purpose**: Approve join request
- **Auth**: Required (Admin only)
- **Business Logic**:
  - Validate admin permissions
  - Change status from PENDING to ACTIVE
  - Change role from PENDING to MEMBER
  - Set approval metadata and joined timestamp
- **Response**: Updated membership with ACTIVE status

#### POST /api/bugtracker/v1/projects/{projectSlug}/members/{userId}/reject

- **Purpose**: Reject join request
- **Auth**: Required (Admin only)
- **Business Logic**:
  - Validate admin permissions
  - Change status from PENDING to REJECTED
  - Reset role to PENDING (no access)
  - Set approval metadata
- **Response**: Updated membership with REJECTED status

#### DELETE /api/bugtracker/v1/projects/{projectSlug}/members/{userId}

- **Purpose**: Remove member from project
- **Auth**: Required (Admin only)
- **Business Logic**:
  - Validate admin permissions
  - Check for last admin protection
  - Remove membership record
- **Response**: 204 No Content

#### PUT /api/bugtracker/v1/projects/{projectSlug}/members/{userId}/role

- **Purpose**: Update member role
- **Auth**: Required (Admin only)
- **Body**: `{ role }`
- **Business Logic**:
  - Validate admin permissions
  - Check for last admin protection when demoting
  - Update member role
- **Response**: Updated membership

#### POST /api/bugtracker/v1/projects/{projectSlug}/members/{userId}/promote

- **Purpose**: Promote member to admin
- **Auth**: Required (Admin only)
- **Response**: Updated membership with ADMIN role

#### POST /api/bugtracker/v1/projects/{projectSlug}/members/{userId}/demote

- **Purpose**: Demote admin to member
- **Auth**: Required (Admin only)
- **Business Logic**: Check for last admin protection
- **Response**: Updated membership with MEMBER role

#### GET /api/bugtracker/v1/projects/{projectSlug}/membership

- **Purpose**: Get current user's membership status in project
- **Auth**: Required
- **Response**: User's membership details or 404 if not a member

#### GET /api/bugtracker/v1/projects/{projectSlug}/members/search

- **Purpose**: Search project members
- **Auth**: Required
- **Query Parameters**:
  - `search` (optional): Search term for user name or email
  - `page` (optional): Page number
  - `size` (optional): Page size
- **Response**: Paginated list of matching project members

## Integration with Teams Module

### Project-Teams Integration

The projects module integrates with teams as sub-modules, where teams exist within projects and inherit project-level permissions.

### Team Access Control

- Only project members can view project teams
- Only project admins can create teams within the project
- Team admins must be project members
- Team slug generation: `{project-slug}-{team-name-slug}`

### Project-Teams API Endpoints

#### GET /api/bugtracker/v1/projects/{projectSlug}/teams

- **Purpose**: Get teams within a project
- **Auth**: Required (Project members only)
- **Query Parameters**:
  - `search` (optional): Search term for team names
  - `page` (optional): Page number
  - `size` (optional): Page size
  - `sortBy` (optional): Sort field
  - `sortDir` (optional): Sort direction (asc/desc)
- **Response**: Paginated list of teams within the project

#### POST /api/bugtracker/v1/projects/{projectSlug}/teams

- **Purpose**: Create a team within a project
- **Auth**: Required (Project admin only)
- **Body**: `{ name, description }`
- **Response**: Created team with project context

#### GET /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}

- **Purpose**: Get specific team within project
- **Auth**: Required (Project members only)
- **Response**: Team details with project context

#### PUT /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}

- **Purpose**: Update team within project
- **Auth**: Required (Project admin or team admin)
- **Body**: `{ name?, description? }`
- **Response**: Updated team

#### DELETE /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}

- **Purpose**: Delete team within project
- **Auth**: Required (Project admin or team admin)
- **Response**: Success confirmation

### Team Member Management within Projects

#### GET /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/members

- **Purpose**: Get team members within project
- **Auth**: Required (Project members only)
- **Response**: Paginated list of team members

#### POST /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/members

- **Purpose**: Add member to team within project
- **Auth**: Required (Project admin or team admin)
- **Body**: `{ userId, role }`
- **Response**: Success confirmation

#### PUT /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/members/{userId}

- **Purpose**: Update team member role within project
- **Auth**: Required (Project admin or team admin)
- **Body**: `{ role }`
- **Response**: Success confirmation

#### DELETE /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/members/{userId}

- **Purpose**: Remove member from team within project
- **Auth**: Required (Project admin or team admin)
- **Response**: Success confirmation

#### POST /api/bugtracker/v1/projects/{projectSlug}/teams/{teamSlug}/leave

- **Purpose**: Leave team within project
- **Auth**: Required
- **Response**: Success confirmation

### Migration Strategy

**Migration**: `V8__Migrate_teams_to_project_submodule.sql`

1. Add `project_id` column to teams table (nullable initially)
2. Create a "Default Project" for existing teams
3. Update team slug generation logic
4. Make `project_id` non-nullable after migration
5. Add foreign key constraints and indexes
6. Update team slugs to include project prefix

## Security & Authorization

### Access Control Matrix

| Action               | Anonymous | User | Project Member | Project Admin |
| -------------------- | --------- | ---- | -------------- | ------------- |
| View projects list   | No        | Yes  | Yes            | Yes           |
| View project details | No        | Yes  | Yes            | Yes           |
| Join project         | No        | Yes  | No             | No            |
| Create project       | No        | Yes  | Yes            | Yes           |
| Update project       | No        | No   | No             | Yes           |
| Delete project       | No        | No   | No             | Yes           |
| View members         | No        | No   | Yes            | Yes           |
| Approve requests     | No        | No   | No             | Yes           |
| Remove members       | No        | No   | No             | Yes           |
| Manage teams         | No        | No   | No             | Yes           |
| View teams           | No        | No   | Yes            | Yes           |

### Business Rules

1. **Project Creation**: Any authenticated user can create projects
2. **Auto-Admin**: Project creator automatically becomes admin
3. **Last Admin Protection**: Cannot remove/demote the last admin
4. **Member Requirement**: Users must be project members to access project teams
5. **Cascading Deletion**: Deleting project handles associated teams and members
6. **Approval Workflow**: All join requests require admin approval
7. **Role Hierarchy**: ADMIN > MEMBER > PENDING (no access)
8. **Soft Delete**: Projects are soft-deleted to preserve data integrity

## Frontend Implementation

### Pages/Routes

**Location**: `frontend/src/pages/`

- `/projects` - Projects listing with filtering and search
- `/projects/{projectSlug}` - Project details and teams
- `/projects/{projectSlug}/edit` - Edit project (admin only)
- `/projects/{projectSlug}/members` - Manage members (admin only)
- `/projects/{projectSlug}/teams` - Project teams management

### Key Components

**Location**: `frontend/src/components/projects/`

- `ProjectsTable` - Display projects in table/list view with actions
- `CreateProjectModal` - Project creation form with validation
- `ProjectRoleBadge` - Display user role in project
- `ProjectTeamCard` - Display team within project context
- `CreateProjectTeamModal` - Create team within project
- `PendingRequestsModal` - Admin interface for join requests

### Service Layer

**File**: [`frontend/src/services/projectService.ts`](frontend/src/services/projectService.ts)

The ProjectService provides comprehensive API integration for:

- **Project CRUD operations**: Create, read, update, delete projects with validation
- **Membership management**: Join, leave, approve, reject, and role updates
- **User project operations**: Get user's projects and membership status
- **Project-teams integration**: Manage teams within project context
- **Search and filtering**: Advanced query capabilities with pagination

### Type Definitions

**File**: [`frontend/src/types/project.ts`](frontend/src/types/project.ts)

The project types define the data structures for:

- **Project interface**: Core project data with admin info, member counts, and user context
- **ProjectMember interface**: Membership details with user information and approval tracking
- **ProjectRole enum**: ADMIN, MEMBER, PENDING roles with different capabilities
- **MemberStatus enum**: PENDING, ACTIVE, REJECTED statuses for approval workflow

### State Management

The frontend uses React hooks for state management with caching integration:

- **Project lists**: Cached project collections with search and pagination
- **Current project**: Active project context with membership status
- **User projects**: Personal project memberships and roles
- **Project members**: Member lists with approval workflow
- **Project teams**: Teams within project context
- **Loading states**: UI feedback for async operations
- **Error handling**: Centralized error state management

### Caching Strategy

**Location**: `frontend/src/services/cacheService.ts`

- Project lists cached with TTL
- Project details cached by slug
- User projects cached separately
- Cache invalidation on mutations
- Bypass cache option for real-time updates

## Technical Considerations

### Slug Generation

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/Project.java`](backend/src/main/java/com/pbm5/bugtracker/entity/Project.java)

The slug generation system provides URL-friendly identifiers:

- **Name transformation**: Converts project names to lowercase, hyphenated format
- **Conflict resolution**: Handles duplicate slugs with incremental suffixes
- **Validation**: Ensures proper format with regex validation
- **Team integration**: Updates team slug format to include project prefix
- **Minimum length**: Ensures slugs meet minimum length requirements

### Caching Strategy

**Backend**: Spring Cache with method-level caching
**Frontend**: Custom cache service with TTL

- Cache project lists with 5-minute TTL
- Cache project details with 1-minute TTL
- Invalidate on membership changes
- Bypass cache option for real-time updates

### Performance Optimizations

- Implement pagination for large project lists
- Add search and filtering capabilities
- Use database indexes for common queries
- Lazy loading for project members
- Composite indexes for complex queries

### Error Handling

**Location**: `backend/src/main/java/com/pbm5/bugtracker/exception/`

- `ProjectNotFoundException`: Project not found
- `ProjectAccessDeniedException`: Insufficient permissions
- `ProjectNameConflictException`: Name already exists
- `DuplicateMembershipException`: User already has membership
- `InvalidProjectOperationException`: General validation errors
- `LastAdminException`: Cannot remove last admin

### Business Logic Implementation

**Location**: `backend/src/main/java/com/pbm5/bugtracker/service/`

- **ProjectService**: Core project management operations
- **ProjectMemberService**: Membership workflow management
- **ProjectSecurityService**: Permission checking and validation
- **SlugService**: Unique slug generation with conflict resolution
