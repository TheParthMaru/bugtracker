# Notifications Module Specification

## Overview

The Notifications module provides a comprehensive, multi-channel notification system for the bug tracker application. It enables real-time communication through email, in-app notifications, and toast messages, supporting event-driven notifications for bug lifecycle events, team activities, and system updates. The module is designed as an independent system that integrates with all other modules to provide timely and relevant user communications.

## Business Requirements

### Core Concept

- **Notifications** are user communications triggered by system events and user actions
- Notifications support multiple delivery channels: email, in-app, and toast messages
- Notifications are event-driven and automatically triggered by system activities
- Notifications respect user preferences and can be customized per user
- Notifications provide real-time updates and historical notification management

### User Stories

#### As a Bug Reporter

- I can receive notifications when my reported bugs are assigned, updated, or resolved
- I can receive email notifications for important bug status changes
- I can view all my notifications in a centralized notification center
- I can configure which types of notifications I want to receive
- I can mark notifications as read and manage my notification history

#### As a Bug Assignee

- I can receive immediate notifications when bugs are assigned to me
- I can get notified about comments and updates on bugs I'm working on
- I can receive priority-based notifications for urgent bug assignments
- I can configure notification preferences to avoid notification overload
- I can receive real-time toast notifications for immediate attention

#### As a Team Member

- I can receive notifications about team activities and assignments
- I can get notified when team members make updates or comments
- I can receive notifications about team performance and achievements
- I can configure team-specific notification preferences
- I can participate in team notification workflows

#### As a Project Admin

- I can receive notifications about project-wide activities and issues
- I can get notified about system events and administrative tasks
- I can configure project-level notification settings
- I can manage notification templates and delivery preferences
- I can monitor notification delivery and system health

## Data Model

### Core Notification Entities

#### UserNotification Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/UserNotification.java`](backend/src/main/java/com/pbm5/bugtracker/entity/UserNotification.java)

The UserNotification entity represents individual notifications:

- **User association**: Mandatory user reference for notification targeting
- **Event tracking**: Event type and related entity references (bug, project, team, user)
- **Content management**: Title, message, and additional JSON data
- **Status tracking**: Read/unread status and delivery timestamps
- **Relationship context**: Optional references to related bugs, projects, teams, and users

#### NotificationPreferences Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/NotificationPreferences.java`](backend/src/main/java/com/pbm5/bugtracker/entity/NotificationPreferences.java)

The NotificationPreferences entity manages user notification settings:

- **User association**: One-to-one relationship with users
- **Channel preferences**: Email, in-app, and toast notification settings
- **Event type filtering**: Granular control over which events trigger notifications
- **Frequency settings**: Notification frequency and timing preferences
- **Quiet hours**: Time-based notification suppression

#### NotificationTemplate Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/NotificationTemplate.java`](backend/src/main/java/com/pbm5/bugtracker/entity/NotificationTemplate.java)

The NotificationTemplate entity manages notification content:

- **Template types**: Email, in-app, and toast message templates
- **Event association**: Templates linked to specific event types
- **Variable substitution**: Support for dynamic content generation
- **Localization support**: Multi-language template support
- **Version management**: Template versioning and update tracking

#### NotificationDeliveryLog Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/NotificationDeliveryLog.java`](backend/src/main/java/com/pbm5/bugtracker/entity/NotificationDeliveryLog.java)

The NotificationDeliveryLog entity tracks delivery status:

- **Delivery tracking**: Success/failure status for each delivery attempt
- **Channel information**: Which channel was used for delivery
- **Error logging**: Detailed error information for failed deliveries
- **Retry tracking**: Retry attempts and backoff strategies
- **Audit trail**: Complete delivery history for compliance

### Notification Channels

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/NotificationChannel.java`](backend/src/main/java/com/pbm5/bugtracker/entity/NotificationChannel.java)

The NotificationChannel enum defines available delivery channels:

- **EMAIL**: Email notifications with HTML templates
- **IN_APP**: In-application notification center
- **TOAST**: Real-time toast messages via WebSocket

## Database Schema

### Core Tables

#### User Notifications Table

**Migration**: Database migration for user notifications

The user_notifications table stores notification data:

- **UUID primary key** for efficient querying and pagination
- **User association** with mandatory user_id foreign key
- **Event tracking** with event_type and related entity references
- **Content storage** with title, message, and JSON data fields
- **Status management** with read status and delivery timestamps
- **Relationship context** with optional foreign keys to bugs, projects, teams, and users

#### Notification Preferences Table

**Migration**: Database migration for notification preferences

The notification_preferences table manages user settings:

- **User association** with one-to-one user relationship
- **Channel settings** with boolean flags for each delivery channel
- **Event filtering** with JSON configuration for event type preferences
- **Timing preferences** with quiet hours and frequency settings
- **Audit fields** with creation and update timestamps

#### Notification Templates Table

**Migration**: Database migration for notification templates

The notification_templates table stores template content:

- **Template identification** with name and event type association
- **Channel-specific content** with separate templates for each channel
- **Variable support** with template variable definitions
- **Version management** with template versioning and active status
- **Localization** with language-specific template variants

#### Notification Delivery Log Table

**Migration**: Database migration for delivery logging

The notification_delivery_logs table tracks delivery status:

- **Notification reference** with foreign key to user notifications
- **Channel information** with delivery channel and attempt details
- **Status tracking** with success/failure status and error details
- **Retry management** with attempt count and backoff information
- **Audit trail** with complete delivery history

## API Endpoints

### Notification Management

**Base URL**: `/api/bugtracker/v1/notifications`

#### GET /api/bugtracker/v1/notifications

- **Purpose**: Get paginated notifications for the current user
- **Auth**: Required (Authenticated users only)
- **Query Parameters**:
  - `page` (optional): Page number (default: 0)
  - `size` (optional): Page size (default: 20)
  - `read` (optional): Filter by read status (true/false)
  - `type` (optional): Filter by notification type
  - `search` (optional): Search in notification content
- **Response**: Paginated list of notifications with full details
- **Business Logic**:
  - Filter notifications by current user
  - Apply read status and type filters
  - Support search across notification content
  - Return paginated results with metadata

#### POST /api/bugtracker/v1/notifications/{notificationId}/mark-read

- **Purpose**: Mark a notification as read
- **Auth**: Required (Notification owner only)
- **Response**: Success confirmation
- **Business Logic**:
  - Validate notification ownership
  - Update read status and timestamp
  - Trigger real-time updates via WebSocket

#### POST /api/bugtracker/v1/notifications/mark-all-read

- **Purpose**: Mark all notifications as read for current user
- **Auth**: Required (Authenticated users only)
- **Response**: Success confirmation with count of marked notifications
- **Business Logic**:
  - Update all unread notifications for current user
  - Batch update for performance
  - Trigger real-time updates via WebSocket

#### GET /api/bugtracker/v1/notifications/unread-count

- **Purpose**: Get unread notification count for current user
- **Auth**: Required (Authenticated users only)
- **Response**: Unread count with timestamp
- **Business Logic**:
  - Count unread notifications for current user
  - Cache result for performance
  - Support real-time updates via WebSocket

### Notification Preferences

#### GET /api/bugtracker/v1/notifications/preferences

- **Purpose**: Get notification preferences for current user
- **Auth**: Required (Authenticated users only)
- **Response**: User notification preferences
- **Business Logic**:
  - Return user preferences or create defaults
  - Include all channel and event type settings

#### PUT /api/bugtracker/v1/notifications/preferences

- **Purpose**: Update notification preferences for current user
- **Auth**: Required (Authenticated users only)
- **Body**: Updated notification preferences
- **Response**: Updated preferences
- **Business Logic**:
  - Validate preference settings
  - Update or create user preferences
  - Apply changes to future notifications

### WebSocket Endpoints

#### WebSocket Connection

- **Purpose**: Real-time notification delivery
- **Auth**: Required (JWT token in connection)
- **Protocol**: STOMP over WebSocket
- **Topics**:
  - `/user/{userId}/notifications` - Personal notifications
  - `/user/{userId}/unread-count` - Unread count updates
- **Business Logic**:
  - Authenticate WebSocket connection with JWT
  - Subscribe to user-specific notification topics
  - Deliver real-time notifications and count updates

## Integration with Other Modules

### Independent Module Design

The notifications module is designed as an independent system:

- **Event-driven architecture**: Notifications triggered by events from other modules
- **Loose coupling**: Other modules don't depend on notification implementation
- **Universal integration**: Can integrate with any module through event listeners
- **Scalable design**: Independent scaling and deployment capabilities

### Bugs Module Integration

Notifications integrate with the bugs module for bug lifecycle events:

- **Bug creation**: Notify project members of new bugs
- **Bug assignment**: Notify assignees of new bug assignments
- **Bug updates**: Notify relevant users of bug status changes
- **Bug comments**: Notify participants of new comments
- **Bug resolution**: Notify stakeholders of bug completion

### Projects Module Integration

Notifications integrate with the projects module for project events:

- **Project updates**: Notify members of project changes
- **Member additions**: Notify new members and existing members
- **Project milestones**: Notify stakeholders of project progress
- **Project invitations**: Notify users of project invitations

### Teams Module Integration

Notifications integrate with the teams module for team activities:

- **Team assignments**: Notify team members of bug assignments
- **Team updates**: Notify members of team changes
- **Member additions**: Notify team members of new additions
- **Team performance**: Notify team leads of performance updates

### Gamification Integration

Notifications integrate with the gamification system for achievements:

- **Point awards**: Notify users of point gains and achievements
- **Streak updates**: Notify users of streak milestones
- **Leaderboard changes**: Notify users of ranking updates
- **Achievement unlocks**: Notify users of new achievements

## Frontend Implementation

### Pages/Routes

**Location**: `frontend/src/pages/`

- `/notifications` - Comprehensive notification management page
- `/notifications/preferences` - Notification preferences configuration

### Key Components

**Location**: `frontend/src/components/notifications/`

- `NotificationBell` - Notification bell with unread count and dropdown
- `NotificationDropdown` - Dropdown with recent notifications and actions
- `NotificationItem` - Individual notification display with actions
- `ToastManager` - Real-time toast notification management
- `NotificationPreferences` - User preference configuration interface

### Service Layer

**File**: [`frontend/src/services/notificationService.ts`](frontend/src/services/notificationService.ts)

The NotificationService provides comprehensive API integration for:

- **Notification management**: CRUD operations for user notifications
- **Preference management**: User notification preference configuration
- **Real-time updates**: WebSocket integration for live notifications
- **Search and filtering**: Advanced notification query capabilities
- **Bulk operations**: Mark all read and batch notification management

### Type Definitions

**File**: [`frontend/src/types/notification.ts`](frontend/src/types/notification.ts)

The notification types define data structures for:

- **Notification interface**: Core notification data with relationships
- **NotificationPreferences interface**: User preference configuration
- **NotificationFilters interface**: Search and filter parameters
- **WebSocket types**: Real-time notification and count update types
- **API response types**: Pagination and response structures

### Custom Hooks

**File**: [`frontend/src/hooks/useWebSocketNotifications.ts`](frontend/src/hooks/useWebSocketNotifications.ts)

The useWebSocketNotifications hook provides:

- **WebSocket connection**: STOMP-based real-time notification delivery
- **Connection management**: Automatic connection, reconnection, and error handling
- **Event handling**: Real-time notification and count update processing
- **State management**: Connection status and notification state management
- **Error handling**: Comprehensive error handling and user feedback

### State Management

The frontend uses React hooks for notification state management:

- **Notification data**: Cached notification lists with pagination
- **Unread count**: Real-time unread notification count
- **Connection status**: WebSocket connection state and health
- **Loading states**: UI feedback for async notification operations
- **Error handling**: Centralized error state management

## Security & Authorization

### Access Control Matrix

| Action                     | Anonymous | User | Project Member | Project Admin |
| -------------------------- | --------- | ---- | -------------- | ------------- |
| View own notifications     | No        | Yes  | Yes            | Yes           |
| Mark notifications read    | No        | Yes  | Yes            | Yes           |
| Configure preferences      | No        | Yes  | Yes            | Yes           |
| View notification logs     | No        | No   | No             | No            |
| Manage notification system | No        | No   | No             | No            |

### Business Rules

1. **User Isolation**: Users can only access their own notifications
2. **Preference Control**: Users can configure their own notification preferences
3. **Event Authorization**: Notifications respect the same permissions as triggering events
4. **WebSocket Security**: WebSocket connections require valid JWT authentication
5. **Data Privacy**: Notification content respects user privacy and project confidentiality
6. **Delivery Tracking**: All notification deliveries are logged for audit purposes

## Technical Considerations

### Multi-Channel Delivery

The notification system supports multiple delivery channels:

- **Email notifications**: HTML email templates with Resend API integration
- **In-app notifications**: Persistent notifications stored in database
- **Toast notifications**: Real-time popup messages via WebSocket
- **Preference-based filtering**: Users can enable/disable specific channels
- **Template processing**: Dynamic content generation with variable substitution

### Real-Time Communication

The system provides real-time notification delivery:

- **WebSocket integration**: STOMP protocol for reliable real-time communication
- **Connection management**: Automatic connection, reconnection, and error handling
- **User-specific topics**: Secure user-specific notification channels
- **Fallback mechanisms**: Polling fallback when WebSocket is unavailable
- **Performance optimization**: Efficient message routing and delivery

### Event-Driven Architecture

The notification system uses event-driven design:

- **Event listeners**: Automatic notification creation based on system events
- **Loose coupling**: Other modules don't depend on notification implementation
- **Extensible design**: Easy addition of new notification types and events
- **Error handling**: Graceful handling of notification creation failures
- **Audit trail**: Complete event tracking and notification history

### Performance Optimization

The system is optimized for performance and scalability:

- **Database indexing**: Strategic indexes for efficient notification queries
- **Caching**: Redis caching for frequently accessed notification data
- **Batch operations**: Efficient bulk notification operations
- **Pagination**: Large notification list handling
- **Async processing**: Background notification delivery and processing

### Error Handling and Reliability

The system provides comprehensive error handling:

- **Delivery retry**: Exponential backoff retry for failed deliveries
- **Error logging**: Detailed error tracking and analysis
- **Graceful degradation**: System continues to work with partial failures
- **User feedback**: Clear error messages and status updates
- **Monitoring**: System health monitoring and alerting

## Advanced Features Integration

### Email Integration

**Brief Overview**: The notifications module integrates with Resend API for reliable email delivery, supporting HTML templates, variable substitution, and delivery tracking. Email notifications respect user preferences and include unsubscribe mechanisms.

### WebSocket Real-Time Updates

**Brief Overview**: The notifications module provides real-time notification delivery through WebSocket connections using STOMP protocol. This enables instant toast notifications, live unread count updates, and real-time notification synchronization across user sessions.

### Template System

**Brief Overview**: The notifications module includes a comprehensive template system supporting multiple channels (email, in-app, toast), variable substitution, localization, and version management. Templates are event-specific and can be customized per project or user.

### Preference Management

**Brief Overview**: The notifications module provides granular user preference management allowing users to control which events trigger notifications, which channels are used for delivery, and when notifications are sent (including quiet hours and frequency settings).

## Implementation Status

### Completed Features

- **Multi-channel delivery**: Email, in-app, and toast notification support
- **Real-time updates**: WebSocket integration for live notifications
- **User preferences**: Comprehensive notification preference management
- **Event-driven architecture**: Automatic notification creation from system events
- **Template system**: Dynamic content generation with variable substitution
- **Delivery tracking**: Complete audit trail and error handling
- **Frontend interface**: Comprehensive notification management UI
- **Security integration**: JWT authentication and user isolation

### Current Capabilities

- Real-time notification delivery across multiple channels
- Comprehensive user preference management and customization
- Event-driven notification creation from all system modules
- WebSocket-based real-time updates and synchronization
- Email integration with HTML templates and delivery tracking
- In-app notification center with search and filtering
- Toast notifications for immediate user attention
- Complete audit trail and delivery status tracking
