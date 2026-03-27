# Bugs Module Specification

## Overview

The Bugs module is the core functionality of the bug tracker application, providing comprehensive bug management capabilities with advanced features including duplicate detection, automatic assignment, gamification integration, and team collaboration. The module is tightly integrated with projects, teams, and other advanced modules to provide a complete bug tracking solution.

## Business Requirements

### Core Concept

- A **Bug** is a reported issue, task, or specification within a project that requires attention and resolution
- Bugs are project-scoped and cannot exist independently of projects
- Bugs support comprehensive lifecycle management from creation to resolution
- Bugs integrate with teams for collaborative resolution and automatic assignment
- Bugs support advanced features like duplicate detection, gamification, and analytics

### User Stories

#### As a Bug Reporter

- I can create bugs with detailed information including title, description, type, and priority
- I can attach files and images to provide additional context
- I can add labels and tags for better categorization
- I can track the status of my reported bugs
- I can receive notifications about bug updates and assignments

#### As a Bug Assignee

- I can view bugs assigned to me with priority and deadline information
- I can update bug status and add comments to communicate progress
- I can attach files to document my work and solutions
- I can collaborate with team members on bug resolution
- I can earn points and achievements for resolving bugs

#### As a Project Admin

- I can manage all bugs within my project
- I can assign bugs to team members or teams
- I can configure automatic assignment rules
- I can view project-wide bug analytics and reports
- I can manage bug labels and categories

#### As a Team Member

- I can view bugs assigned to my team
- I can collaborate on bug resolution with team members
- I can participate in team-based bug assignments
- I can contribute to team performance metrics

## Data Model

### Bug Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/Bug.java`](backend/src/main/java/com/pbm5/bugtracker/entity/Bug.java)

The Bug entity represents the core bug tracking functionality:

- **Project association**: Mandatory project_id reference for project-scoped organization
- **Unique identification**: Long primary key with project-scoped ticket numbers
- **Bug metadata**: Title, description, type, status, and priority
- **Assignment tracking**: Reporter and assignee user references
- **Lifecycle management**: Creation, update, and closure timestamps
- **Relationship management**: One-to-many relationships with attachments, comments, and labels
- **Tagging system**: Custom tags array for flexible categorization

### Bug Enums

**Files**:

- [`backend/src/main/java/com/pbm5/bugtracker/entity/BugStatus.java`](backend/src/main/java/com/pbm5/bugtracker/entity/BugStatus.java)
- [`backend/src/main/java/com/pbm5/bugtracker/entity/BugType.java`](backend/src/main/java/com/pbm5/bugtracker/entity/BugType.java)
- [`backend/src/main/java/com/pbm5/bugtracker/entity/BugPriority.java`](backend/src/main/java/com/pbm5/bugtracker/entity/BugPriority.java)

**BugStatus** manages the bug lifecycle:

- **OPEN**: Initial status for newly created bugs
- **FIXED**: Bug has been resolved but not yet closed
- **CLOSED**: Bug is completely resolved and closed
- **REOPENED**: Previously closed bug that needs attention again

**BugType** categorizes bugs by nature:

- **ISSUE**: Defect or problem requiring fixing
- **TASK**: Work item or feature request
- **SPEC**: Specification or documentation item

**BugPriority** defines urgency levels with SLA requirements:

- **CRASH**: System crash requiring immediate response
- **CRITICAL**: Critical issue requiring immediate response
- **HIGH**: High priority issue
- **MEDIUM**: Medium priority issue
- **LOW**: Low priority issue

### Supporting Entities

**BugAttachment**: File attachments with metadata and access control
**BugComment**: Threaded comments with replies and attachments
**BugLabel**: Categorized labels for bug organization
**BugDuplicate**: Duplicate relationship tracking with confidence scores

## Database Schema

### Core Tables

#### Bugs Table

**Migration**: [`V9__Create_bugs_table.sql`](backend/src/main/resources/db/migration/V9__Create_bugs_table.sql)

The bugs table stores core bug information with:

- **Long primary key** for efficient querying and pagination
- **Project association** with mandatory project_id foreign key
- **Project-scoped ticket numbers** with unique constraints within projects
- **Comprehensive bug metadata** including title, description, type, status, and priority
- **Assignment tracking** with reporter and assignee user references
- **Lifecycle timestamps** for creation, updates, and closure
- **Data validation constraints** ensuring proper enum values and required fields

#### Bug Attachments Table

**Migration**: [`V12__Create_bug_attachments_table.sql`](backend/src/main/resources/db/migration/V12__Create_bug_attachments_table.sql)

The bug_attachments table manages file attachments with:

- **File metadata** including filename, size, and MIME type
- **Access control** with uploaded_by user reference
- **Storage management** with file path and original filename tracking
- **Audit trail** with creation timestamps

#### Bug Comments Table

**Migration**: [`V13__Create_bug_comments_table.sql`](backend/src/main/resources/db/migration/V13__Create_bug_comments_table.sql)

The bug_comments table supports threaded discussions with:

- **Threaded structure** with parent-child relationships for replies
- **Rich content** with text content and attachment support
- **Author tracking** with user references and timestamps
- **Cascade deletion** when bugs are removed

#### Bug Labels System

**Migrations**:

- [`V10__Create_bug_labels_table.sql`](backend/src/main/resources/db/migration/V10__Create_bug_labels_table.sql)
- [`V11__Create_bug_label_mapping.sql`](backend/src/main/resources/db/migration/V11__Create_bug_label_mapping.sql)

The bug labels system provides:

- **Categorized labels** with colors and descriptions
- **Many-to-many relationships** between bugs and labels
- **System vs custom labels** for predefined and user-created categories
- **Project-scoped labels** for organization-specific categorization

#### Project Ticket Numbers

**Migration**: [`V18__Add_project_ticket_number.sql`](backend/src/main/resources/db/migration/V18__Add_project_ticket_number.sql)

The project ticket number system provides:

- **Sequential numbering** within each project
- **Unique constraints** ensuring no duplicate ticket numbers per project
- **Automatic generation** with database functions
- **User-friendly identifiers** for bug references

#### Bug Tags System

**Migration**: [`V22__Add_bug_tags.sql`](backend/src/main/resources/db/migration/V22__Add_bug_tags.sql)

The bug tags system enables:

- **Flexible tagging** with PostgreSQL array support
- **Efficient searching** with GIN indexes
- **Custom categorization** beyond predefined labels
- **Performance optimization** for tag-based queries

#### Team Assignments

**Migration**: [`V23__Add_team_assignments.sql`](backend/src/main/resources/db/migration/V23__Add_team_assignments.sql)

The bug-team assignment system supports:

- **Team-bug relationships** for collaborative resolution
- **Primary team designation** with is_primary flag
- **Assignment tracking** with assigned_by and assigned_at fields
- **Performance optimization** with strategic indexes

#### Duplicate Detection

**Migration**: [`V19__Create_bug_similarity_tables.sql`](backend/src/main/resources/db/migration/V19__Create_bug_similarity_tables.sql)

The duplicate detection system provides:

- **Similarity tracking** with confidence scores and algorithms
- **Duplicate relationships** with original and duplicate bug references
- **Detection methods** tracking manual, automatic, and hybrid detection
- **Analytics support** for duplicate analysis and reporting

## API Endpoints

### Bug Management

**Base URL**: `/api/bugtracker/v1/projects/{projectSlug}/bugs`

#### GET /api/bugtracker/v1/projects/{projectSlug}/bugs

- **Purpose**: List bugs within a project with filtering and pagination
- **Auth**: Required (Project members only)
- **Query Parameters**:
  - `search` (optional): Search term for bug title/description
  - `status` (optional): Filter by bug status
  - `priority` (optional): Filter by bug priority
  - `type` (optional): Filter by bug type
  - `assignee` (optional): Filter by assignee (user ID, "UNASSIGNED", "ASSIGNED")
  - `page` (optional): Page number (default: 0)
  - `size` (optional): Page size (default: 20)
  - `sort` (optional): Sort field and direction
- **Response**: Paginated list of bugs with full details

#### POST /api/bugtracker/v1/projects/{projectSlug}/bugs

- **Purpose**: Create a new bug
- **Auth**: Required (Project members only)
- **Body**: `{ title, description, type, priority, assigneeId?, labelIds?, tags?, assignedTeamIds? }`
- **Business Logic**:
  - Validate project membership
  - Generate project-specific ticket number
  - Handle team assignments if provided
  - Execute automatic assignment if no manual assignment
  - Trigger gamification events for bug creation
- **Response**: Created bug with full details

#### GET /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}

- **Purpose**: Get bug details by ID
- **Auth**: Required (Project members only)
- **Response**: Bug details with attachments, comments, and team assignments

#### PUT /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}

- **Purpose**: Update bug details
- **Auth**: Required (Project members only)
- **Body**: `{ title?, description?, type?, priority?, assigneeId?, status?, labelIds?, tags? }`
- **Business Logic**:
  - Validate update permissions
  - Handle status transitions with validation
  - Trigger gamification events for status changes
  - Update team assignments if changed
- **Response**: Updated bug details

#### DELETE /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}

- **Purpose**: Delete bug
- **Auth**: Required (Project admin or bug reporter)
- **Business Logic**:
  - Validate deletion permissions
  - Cascade delete attachments and comments
  - Clean up team assignments and duplicate relationships
- **Response**: Success confirmation

### Bug Comments

#### GET /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}/comments

- **Purpose**: Get bug comments with threading
- **Auth**: Required (Project members only)
- **Response**: Threaded list of comments with replies

#### POST /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}/comments

- **Purpose**: Add comment to bug
- **Auth**: Required (Project members only)
- **Body**: `{ content, parentId? }`
- **Response**: Created comment with full details

### Bug Attachments

#### GET /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}/attachments

- **Purpose**: Get bug attachments
- **Auth**: Required (Project members only)
- **Response**: List of attachments with metadata

#### POST /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}/attachments

- **Purpose**: Upload attachment to bug
- **Auth**: Required (Project members only)
- **Body**: Multipart file upload
- **Response**: Created attachment with metadata

### Team Assignments

#### GET /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}/teams

- **Purpose**: Get teams assigned to bug
- **Auth**: Required (Project members only)
- **Response**: List of assigned teams with assignment details

#### POST /api/bugtracker/v1/projects/{projectSlug}/bugs/{bugId}/teams

- **Purpose**: Assign teams to bug
- **Auth**: Required (Project admin or team admin)
- **Body**: `{ teamIds, isPrimaryTeamId? }`
- **Response**: Success confirmation

## Integration with Other Modules

### Projects Module Integration

Bugs are tightly integrated with projects:

- **Mandatory association**: Bugs cannot exist without project context
- **Project-scoped ticket numbers**: Sequential numbering within each project
- **Access control**: Project membership required for bug access
- **Project admin privileges**: Project admins can manage all bugs

### Teams Module Integration

Bugs integrate with teams for collaborative resolution:

- **Team assignments**: Bugs can be assigned to multiple teams
- **Primary team designation**: One team designated as primary responsible
- **Collaborative resolution**: Team members can work together on bugs
- **Team-based auto-assignment**: Automatic assignment based on team skills

### Duplicate Detection Integration

Bugs support advanced duplicate detection:

- **Similarity analysis**: Automatic detection of similar bugs
- **Confidence scoring**: Machine learning-based similarity scores
- **Manual marking**: Users can manually mark duplicates
- **Relationship tracking**: Maintains duplicate relationships with metadata

### Gamification Integration

Bugs integrate with the gamification system:

- **Point awards**: Points for bug creation, resolution, and collaboration
- **Achievement tracking**: Badges for bug-related accomplishments
- **Leaderboards**: Rankings based on bug resolution performance
- **Streak tracking**: Daily login and activity streaks

### Automatic Assignment Integration

Bugs support intelligent automatic assignment:

- **Skill-based matching**: Assignment based on user skills and bug requirements
- **Workload balancing**: Consideration of current user workload
- **Team recommendations**: Automatic team assignment based on capabilities
- **Priority-based assignment**: Immediate assignment for high-priority bugs

## Frontend Implementation

### Pages/Routes

**Location**: `frontend/src/pages/`

- `/bugs` - Bugs listing with project selection and filtering
- `/projects/{projectSlug}/bugs` - Project-scoped bugs management
- `/projects/{projectSlug}/bugs/{bugId}` - Bug details and management
- `/projects/{projectSlug}/bugs/create` - Bug creation form
- `/projects/{projectSlug}/bugs/{bugId}/edit` - Bug editing form

### Key Components

**Location**: `frontend/src/components/bugs/`

- `BugsTable` - Display bugs in table/list view with actions
- `BugCard` - Display bug information in card format
- `BugDetailPage` - Comprehensive bug details with comments and attachments
- `BugFilters` - Advanced filtering interface for bugs
- `BugStatusBadge` - Display bug status with color coding
- `BugCommentForm` - Comment creation and editing
- `BugCommentThread` - Threaded comment display
- `BugAttachmentUpload` - File upload interface
- `BugAttachmentViewer` - File preview and download
- `DuplicateManagementInterface` - Duplicate bug management
- `TeamAssignmentSection` - Team assignment interface

### Service Layer

**File**: [`frontend/src/services/bugService.ts`](frontend/src/services/bugService.ts)

The BugService provides comprehensive API integration for:

- **Bug CRUD operations**: Create, read, update, delete bugs with validation
- **Comment management**: Add, edit, delete comments with threading
- **Attachment management**: Upload, download, and manage file attachments
- **Team assignments**: Manage team assignments and collaboration
- **Duplicate detection**: Similarity analysis and duplicate management
- **Search and filtering**: Advanced query capabilities with pagination
- **Analytics integration**: Bug statistics and reporting

### Type Definitions

**File**: [`frontend/src/types/bug.ts`](frontend/src/types/bug.ts)

The bug types define the data structures for:

- **Bug interface**: Core bug data with project context and relationships
- **BugComment interface**: Comment data with threading and attachments
- **BugAttachment interface**: File attachment metadata and access
- **BugLabel interface**: Label data with colors and descriptions
- **Enums**: BugStatus, BugType, BugPriority with display names and utilities
- **Search and filter types**: Parameters for advanced bug queries
- **Integration types**: Team assignments and duplicate relationships

### State Management

The frontend uses React hooks for state management with caching integration:

- **Bug lists**: Cached bug collections with search and pagination
- **Current bug**: Active bug context with comments and attachments
- **User bugs**: Personal bug assignments and reports
- **Project context**: Project-scoped bug operations
- **Loading states**: UI feedback for async operations
- **Error handling**: Centralized error state management

## Security & Authorization

### Access Control Matrix

| Action             | Anonymous | User | Project Member | Project Admin | Bug Reporter | Bug Assignee |
| ------------------ | --------- | ---- | -------------- | ------------- | ------------ | ------------ |
| View bugs list     | No        | No   | Yes            | Yes           | Yes          | Yes          |
| View bug details   | No        | No   | Yes            | Yes           | Yes          | Yes          |
| Create bug         | No        | No   | Yes            | Yes           | Yes          | Yes          |
| Update bug         | No        | No   | No             | Yes           | Yes          | Yes          |
| Delete bug         | No        | No   | No             | Yes           | Yes          | No           |
| Add comments       | No        | No   | Yes            | Yes           | Yes          | Yes          |
| Upload attachments | No        | No   | Yes            | Yes           | Yes          | Yes          |
| Assign teams       | No        | No   | No             | Yes           | No           | No           |
| Mark duplicates    | No        | No   | Yes            | Yes           | Yes          | Yes          |

### Business Rules

1. **Project Association**: Bugs must be associated with a project
2. **Project Membership**: Users must be project members to access bugs
3. **Bug Reporter Rights**: Bug reporters can view and comment on their bugs
4. **Bug Assignee Rights**: Bug assignees can update status and add comments
5. **Project Admin Control**: Project admins can manage all bugs in their project
6. **Status Transitions**: Bug status changes follow defined workflow rules
7. **Team Assignment**: Only project admins and team admins can assign teams
8. **Duplicate Management**: Project members can mark and manage duplicates

## Technical Considerations

### Performance Optimizations

- **Database indexes**: Strategic indexes for common query patterns
- **Pagination**: Efficient pagination for large bug lists
- **Caching**: Redis caching for frequently accessed bug data
- **Full-text search**: PostgreSQL full-text search for bug content
- **Lazy loading**: Lazy loading for comments and attachments

### Search and Filtering

- **Full-text search**: Search across bug titles and descriptions
- **Advanced filters**: Filter by status, priority, type, assignee, and labels
- **Tag-based search**: Efficient tag-based filtering with GIN indexes
- **Date range queries**: Filter bugs by creation and update dates
- **Combined queries**: Support for complex multi-criteria searches

### File Management

- **Secure storage**: Files stored with access control and validation
- **File type validation**: Restrict file types for security
- **Size limits**: Enforce file size limits to prevent abuse
- **Virus scanning**: Integration with antivirus scanning services
- **CDN integration**: Content delivery network for file serving

### Notification System

- **Real-time notifications**: WebSocket-based real-time updates
- **Email notifications**: Email alerts for important bug events
- **Notification preferences**: User-configurable notification settings
- **Event-driven architecture**: Event-based notification triggers

## Advanced Features Integration

### Duplicate Detection

**Brief Overview**: The bugs module integrates with an advanced duplicate detection system that uses machine learning algorithms to identify similar bugs automatically. Users can also manually mark duplicates, and the system maintains confidence scores and detection methods for analytics.

### Automatic Assignment

**Brief Overview**: The bugs module supports intelligent automatic assignment that considers user skills, current workload, and bug requirements. The system can assign bugs to both individual users and teams based on configured rules and machine learning models.

### Gamification

**Brief Overview**: The bugs module integrates with a comprehensive gamification system that awards points for bug-related activities, tracks achievements and streaks, and maintains leaderboards. Users earn points for creating bugs, resolving bugs, and collaborating with team members.

### Analytics and Reporting

**Brief Overview**: The bugs module provides comprehensive analytics including bug resolution times, team performance metrics, duplicate detection statistics, and project health indicators. These analytics support data-driven decision making and process improvement.

## Implementation Status

### Completed Features

- **Bug Management**: Full CRUD operations with project integration
- **Comment System**: Threaded comments with replies and attachments
- **Attachment System**: File upload, storage, and access control
- **Label System**: Categorized labels with colors and descriptions
- **Tag System**: Flexible tagging with efficient search capabilities
- **Team Integration**: Team assignments and collaborative resolution
- **Project Integration**: Project-scoped bugs with ticket numbering
- **Frontend**: Complete UI with advanced filtering and search
- **Database**: Optimized schema with comprehensive indexes
- **API**: RESTful endpoints with comprehensive error handling

### Current Capabilities

- Project-scoped bug creation and management
- Comprehensive bug lifecycle with status transitions
- Advanced filtering and search with full-text capabilities
- Team-based collaboration and assignment
- File attachment and comment management
- Label and tag-based organization
- Integration with duplicate detection system
- Integration with automatic assignment system
- Integration with gamification system
- Comprehensive audit logging and error handling
