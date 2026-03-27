# Gamification System Specification

## Overview

The Gamification System is an advanced feature that enhances user engagement and motivation through a comprehensive point-based reward system. The system tracks user activities, awards points for various achievements, maintains leaderboards, and tracks login streaks to create a competitive and engaging environment. The gamification system integrates seamlessly with the bug tracking workflow, automatically awarding points for bug resolution, daily logins, and other activities while providing real-time feedback and social recognition through leaderboards and achievements.

## Business Requirements

### Core Concept

- **Gamification** is the application of game design elements and principles to non-game contexts to increase user engagement and motivation
- The system uses a point-based reward system with multiple earning opportunities and social recognition features
- Points are awarded automatically for various activities including bug resolution, daily logins, and system interactions
- The system maintains project-specific leaderboards and global user statistics
- Login streaks encourage consistent platform usage and user retention

### User Stories

#### As a Developer

- I can earn points for resolving bugs based on their priority level
- I can see my current point total and ranking in project leaderboards
- I can track my login streak and see my progress toward streak goals
- I can view my point transaction history to understand how I earned points
- I can see notifications when I earn points or achieve milestones

#### As a Project Manager

- I can view project leaderboards to see top performers
- I can track team engagement through gamification metrics
- I can see which team members are most active in bug resolution
- I can use gamification data to identify high-performing contributors
- I can monitor project-specific engagement and participation rates

#### As a Team Lead

- I can see my team's performance in project leaderboards
- I can track individual team member contributions and engagement
- I can use gamification metrics to motivate team members
- I can identify team members who need encouragement or recognition
- I can celebrate team achievements and milestones

#### As a System Administrator

- I can configure point values for different activities
- I can monitor system-wide gamification engagement metrics
- I can manage leaderboard resets and maintenance
- I can track user retention through streak analytics
- I can adjust gamification parameters to optimize engagement

## Data Model

### Core Services

#### GamificationService

**File**: [`backend/src/main/java/com/pbm5/bugtracker/service/GamificationService.java`](backend/src/main/java/com/pbm5/bugtracker/service/GamificationService.java)

The GamificationService is the main orchestrator for all gamification operations:

- **User initialization**: Sets up gamification data for new users with welcome bonus
- **Point awarding**: Coordinates point transactions and updates across all related systems
- **Daily login handling**: Processes daily login rewards and streak updates
- **Integration coordination**: Manages integration with leaderboards, streaks, and notifications
- **Data aggregation**: Provides comprehensive user gamification statistics

#### PointCalculationService

**File**: [`backend/src/main/java/com/pbm5/bugtracker/service/PointCalculationService.java`](backend/src/main/java/com/pbm5/bugtracker/service/PointCalculationService.java)

The PointCalculationService handles all point-related calculations and validations:

- **Bug resolution points**: Calculates points based on bug priority levels
- **Daily login points**: Awards consistent daily login rewards
- **Penalty calculations**: Applies penalties for bug reopenings
- **Transaction validation**: Ensures point transactions are valid and consistent
- **Point value management**: Centralizes all point value calculations

#### LeaderboardService

**File**: [`backend/src/main/java/com/pbm5/bugtracker/service/LeaderboardService.java`](backend/src/main/java/com/pbm5/bugtracker/service/LeaderboardService.java)

The LeaderboardService manages project-specific leaderboards and rankings:

- **Project leaderboards**: Maintains weekly, monthly, and all-time project rankings
- **User ranking**: Calculates and updates user positions in leaderboards
- **Performance tracking**: Tracks bugs resolved and points earned per project
- **Leaderboard updates**: Updates rankings when users earn points
- **Ranking calculations**: Provides ranked lists with user display names

#### StreakService

**File**: [`backend/src/main/java/com/pbm5/bugtracker/service/StreakService.java`](backend/src/main/java/com/pbm5/bugtracker/service/StreakService.java)

The StreakService manages user login streaks and streak-related achievements:

- **Streak calculation**: Tracks consecutive daily logins
- **Streak validation**: Ensures streak updates are valid and consistent
- **Max streak tracking**: Records the highest streak achieved by each user
- **Streak reset logic**: Handles streak breaks and resets appropriately
- **Streak information**: Provides comprehensive streak statistics

### Supporting Entities

#### PointValue Enum

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/PointValue.java`](backend/src/main/java/com/pbm5/bugtracker/entity/PointValue.java)

The PointValue enum centralizes all point values in the system:

- **Welcome bonus**: 1 point for new user registration
- **Daily login**: 1 point for each daily login
- **Bug resolution points**: Variable points based on bug priority (10-100 points)
- **Bug reopened penalty**: -10 points for bug reopenings
- **Point calculation methods**: Static methods for point value retrieval

#### UserPoints Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/UserPoints.java`](backend/src/main/java/com/pbm5/bugtracker/entity/UserPoints.java)

The UserPoints entity manages global user point statistics:

- **Total points**: Cumulative points earned across all projects
- **Bugs resolved**: Global count of bugs resolved by the user
- **Point management**: Methods for adding points and updating statistics
- **User association**: Links to user entity for display purposes

#### ProjectLeaderboard Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/ProjectLeaderboard.java`](backend/src/main/java/com/pbm5/bugtracker/entity/ProjectLeaderboard.java)

The ProjectLeaderboard entity manages project-specific user rankings:

- **Project association**: Links to specific project for scoped rankings
- **Time-based points**: Separate tracking for weekly, monthly, and all-time points
- **Bugs resolved**: Project-specific bug resolution count
- **Current streak**: Project-specific login streak tracking
- **Ranking data**: All data necessary for leaderboard calculations

#### UserStreak Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/UserStreak.java`](backend/src/main/java/com/pbm5/bugtracker/entity/UserStreak.java)

The UserStreak entity tracks user login streaks:

- **Current streak**: Number of consecutive daily logins
- **Max streak**: Highest streak ever achieved by the user
- **Last login date**: Date of the most recent login for streak calculation
- **Streak management**: Methods for incrementing and resetting streaks

#### PointTransaction Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/PointTransaction.java`](backend/src/main/java/com/pbm5/bugtracker/entity/PointTransaction.java)

The PointTransaction entity provides a complete audit trail of all point activities:

- **Transaction details**: Points credited, deducted, and net points
- **Activity tracking**: Reason for point award and associated bug/project
- **Timestamp**: When the transaction occurred
- **User association**: Which user received the points
- **Audit trail**: Complete history of all point-related activities

## Database Schema

### Core Tables

#### User Points Table

**Migration**: [`V25__Create_user_points_table.sql`](backend/src/main/resources/db/migration/V25__Create_user_points_table.sql)

The user_points table manages global user point statistics:

- **UUID primary key** for efficient user association
- **User reference** with foreign key constraint to users table
- **Total points** with default value of 0 and non-null constraint
- **Bugs resolved count** for global bug resolution tracking
- **Timestamps** for creation and update tracking
- **Unique constraint** ensuring one record per user

#### Project Leaderboard Table

**Migration**: [`V26__Create_project_leaderboard_table.sql`](backend/src/main/resources/db/migration/V26__Create_project_leaderboard_table.sql)

The project_leaderboard table manages project-specific user rankings:

- **UUID primary key** for efficient relationship management
- **Project and user references** with foreign key constraints
- **Time-based points** (weekly, monthly, all-time) with default values
- **Bugs resolved count** for project-specific tracking
- **Current streak** for project-specific streak tracking
- **Unique constraint** preventing duplicate project-user combinations
- **Check constraints** ensuring non-negative point values

#### User Streaks Table

**Migration**: [`V27__Create_user_streaks_table.sql`](backend/src/main/resources/db/migration/V27__Create_user_streaks_table.sql)

The user_streaks table tracks user login streaks:

- **UUID primary key** for efficient user association
- **User reference** with foreign key constraint to users table
- **Current and max streak** with default values and non-null constraints
- **Last login date** for streak calculation and validation
- **Timestamps** for creation and update tracking
- **Unique constraint** ensuring one streak record per user

#### Point Transactions Table

**Migration**: [`V28__Create_point_transactions_table.sql`](backend/src/main/resources/db/migration/V28__Create_point_transactions_table.sql)

The point_transactions table provides complete audit trail of point activities:

- **UUID primary key** for efficient transaction management
- **User reference** with foreign key constraint to users table
- **Project reference** with optional foreign key for project-specific activities
- **Point details** (credited, deducted, net) with appropriate constraints
- **Activity tracking** with reason, bug reference, and timestamp
- **Indexes** for efficient querying by user, project, and date

### Performance Indexes

Strategic indexes optimize gamification queries:

- **User-based indexes**: user_id for efficient user point lookups
- **Project-based indexes**: project_id for project leaderboard queries
- **Date-based indexes**: earned_at for transaction history analysis
- **Composite indexes**: Combined user_id and project_id for project-specific queries
- **Reason-based indexes**: reason for activity analysis and reporting

## Point System

### Point Values

The gamification system uses a centralized point value system defined in the PointValue enum:

#### Bug Resolution Points

**Priority-based point awards**:

- **Crash**: 100 points (highest priority, most critical bugs)
- **Critical**: 75 points (high priority, significant impact)
- **High**: 50 points (medium-high priority, important bugs)
- **Medium**: 25 points (standard priority, moderate impact)
- **Low**: 10 points (lowest priority, minor issues)

#### System Activity Points

**Consistent point awards**:

- **Welcome bonus**: 1 point (new user registration incentive)
- **Daily login**: 1 point (encourages consistent platform usage)
- **Bug reopened penalty**: -10 points (discourages premature bug closures)

### Point Calculation Algorithm

#### Bug Resolution Point Calculation

**Formula**: `points = PointValue.getBugResolutionPoints(bug.priority)`

**Process**:

1. **Priority validation**: Ensure bug priority is valid and not null
2. **Point lookup**: Retrieve point value from PointValue enum
3. **Transaction creation**: Create point transaction record
4. **User update**: Update user's total points and bugs resolved count
5. **Leaderboard update**: Update project-specific leaderboard rankings

#### Daily Login Point Calculation

**Formula**: `points = PointValue.DAILY_LOGIN.getPoints()` (always 1 point)

**Process**:

1. **Eligibility check**: Verify user hasn't already received daily login points
2. **Streak update**: Update user's login streak
3. **Point award**: Award 1 point for daily login
4. **Transaction recording**: Create transaction record for audit trail
5. **Notification**: Send notification about point award

#### Penalty Point Calculation

**Formula**: `penalty = -PointValue.BUG_REOPENED_PENALTY.getPoints()` (always -10 points)

**Process**:

1. **Penalty application**: Deduct 10 points from user's total
2. **Bug count adjustment**: Decrement bugs resolved count
3. **Transaction recording**: Create penalty transaction record
4. **Leaderboard update**: Update project leaderboard rankings
5. **Notification**: Inform user about penalty application

### Point Validation

The system includes comprehensive validation for all point transactions:

#### Transaction Validation Rules

1. **Reason validation**: Reason must be at least 3 characters long
2. **Point validation**: At least one of credited or deducted points must be non-zero
3. **Exclusivity validation**: Cannot have both credited and deducted points in same transaction
4. **Value validation**: Point values must match expected values for specific reasons
5. **User validation**: User must exist and be active

#### Specific Validations

- **Bug resolution**: Points must match priority-based values (10, 25, 50, 75, 100)
- **Daily login**: Must award exactly 1 point
- **Bug reopened**: Must deduct exactly 10 points
- **Welcome bonus**: Must award exactly 1 point

## Leaderboard System

### Leaderboard Types

The system maintains three types of leaderboards for each project:

#### Weekly Leaderboard

**Purpose**: Encourages short-term engagement and weekly participation
**Reset frequency**: Every Monday at midnight
**Ranking criteria**: Weekly points earned in the current week
**Use case**: Weekly team competitions and short-term motivation

#### Monthly Leaderboard

**Purpose**: Balances short-term and long-term engagement
**Reset frequency**: First day of each month at midnight
**Ranking criteria**: Monthly points earned in the current month
**Use case**: Monthly team recognition and medium-term goals

#### All-Time Leaderboard

**Purpose**: Recognizes long-term contributors and sustained engagement
**Reset frequency**: Never (permanent historical record)
**Ranking criteria**: Total points earned since joining the project
**Use case**: Long-term recognition and career achievement tracking

### Ranking Algorithm

#### Leaderboard Update Process

**Formula**: `new_rank = calculate_rank(user_points, all_users_points)`

**Process**:

1. **Point update**: Add new points to user's project-specific totals
2. **Rank calculation**: Calculate new rank based on updated points
3. **Leaderboard refresh**: Update leaderboard with new rankings
4. **Notification**: Notify user of rank changes if significant

#### Ranking Calculation

**Algorithm**:

1. **Sort by points**: Sort all users by their points in descending order
2. **Assign ranks**: Assign ranks based on position in sorted list
3. **Handle ties**: Users with same points get same rank
4. **Rank display**: Display ranks with appropriate formatting

### Leaderboard Features

#### Project-Scoped Rankings

- **Project isolation**: Each project maintains separate leaderboards
- **Cross-project points**: Users can participate in multiple project leaderboards
- **Project-specific metrics**: Bugs resolved and points tracked per project
- **Project context**: All leaderboard data is scoped to specific projects

#### Real-Time Updates

- **Immediate updates**: Leaderboards update immediately when points are awarded
- **Live rankings**: Users see current rankings in real-time
- **Change notifications**: Users receive notifications for significant rank changes
- **Performance optimization**: Efficient updates without full leaderboard recalculation

## Streak System

### Streak Calculation

#### Login Streak Algorithm

**Formula**: `current_streak = consecutive_days_since_last_gap`

**Process**:

1. **Date comparison**: Compare current login date with last login date
2. **Consecutive check**: If login is on consecutive day, increment streak
3. **Gap detection**: If gap detected, reset streak to 1
4. **Max streak update**: Update max streak if current exceeds previous max
5. **Streak persistence**: Save updated streak information

#### Streak Validation Rules

1. **Date validation**: Login date cannot be in the future
2. **Consecutive validation**: Streak only increments for consecutive days
3. **Gap handling**: Any gap in login resets streak to 1
4. **Max streak tracking**: Always maintain highest streak achieved
5. **First login**: First login starts streak at 1

### Streak Features

#### Streak Tracking

- **Current streak**: Number of consecutive days logged in
- **Max streak**: Highest streak ever achieved by the user
- **Streak start date**: When the current streak began
- **Last login date**: Most recent login for streak calculation
- **Streak milestones**: Recognition for achieving streak goals

#### Streak Notifications

- **Streak continuation**: Notifications for maintaining streaks
- **Streak milestones**: Special notifications for streak achievements
- **Streak breaks**: Notifications when streaks are broken
- **Streak recovery**: Encouragement to start new streaks

## API Endpoints

### Gamification Management

**Base URL**: `/api/bugtracker/v1/gamification`

#### GET /api/bugtracker/v1/gamification/users/{userId}/points

- **Purpose**: Get user's gamification points and statistics
- **Auth**: Required (User can access own data, admins can access any)
- **Response**: UserPointsResponse with total points, bugs resolved, and streak info
- **Business Logic**:
  - Retrieve user's global point statistics
  - Calculate current streak and max streak
  - Include bugs resolved count and last activity
  - Provide user display name for UI rendering

#### GET /api/bugtracker/v1/gamification/users/{userId}/streak

- **Purpose**: Get user's login streak information
- **Auth**: Required (User can access own data, admins can access any)
- **Response**: StreakInfoResponse with streak details
- **Business Logic**:
  - Retrieve user's current and max streak
  - Calculate streak start date
  - Include last login date for streak validation
  - Provide comprehensive streak statistics

#### GET /api/bugtracker/v1/gamification/users/{userId}/transactions

- **Purpose**: Get user's point transaction history
- **Auth**: Required (User can access own data, admins can access any)
- **Response**: PageResponse<PointTransactionResponse> with transaction history
- **Business Logic**:
  - Retrieve paginated transaction history
  - Include points earned, reasons, and timestamps
  - Filter by date range if specified
  - Provide complete audit trail of point activities

### Leaderboard Management

#### GET /api/bugtracker/v1/gamification/projects/{projectId}/leaderboard

- **Purpose**: Get project leaderboard with specified timeframe
- **Auth**: Required (Project members only)
- **Response**: PageResponse<LeaderboardEntryResponse> with ranked users
- **Business Logic**:
  - Retrieve project-specific leaderboard data
  - Sort users by points for specified timeframe
  - Calculate ranks and include user display names
  - Provide paginated results for large teams

#### GET /api/bugtracker/v1/gamification/projects/{projectId}/leaderboard/{userId}

- **Purpose**: Get specific user's leaderboard entry for a project
- **Auth**: Required (Project members only)
- **Response**: LeaderboardEntryResponse with user's ranking
- **Business Logic**:
  - Retrieve user's project-specific statistics
  - Calculate user's rank in the project
  - Include points, bugs resolved, and streak info
  - Provide comprehensive user performance data

### Point Management

#### POST /api/bugtracker/v1/gamification/users/{userId}/points

- **Purpose**: Award points to a user (internal use)
- **Auth**: Required (System operations only)
- **Body**: `{ projectId?, points, reason, bugId? }`
- **Response**: PointTransactionResponse with transaction details
- **Business Logic**:
  - Validate point transaction request
  - Create point transaction record
  - Update user's total points and statistics
  - Update project leaderboard if applicable
  - Send notification about point award

## Integration with Other Modules

### Bugs Module Integration

The gamification system integrates with the bugs module for automatic point awards:

- **Bug resolution**: Automatic point awards when bugs are resolved
- **Bug reopening**: Automatic penalty application when bugs are reopened
- **Priority-based rewards**: Points awarded based on bug priority levels
- **Assignment tracking**: Points awarded to assigned users for bug resolution

### Authentication Module Integration

The system integrates with authentication for daily login tracking:

- **Login detection**: Automatic daily login point awards
- **Streak tracking**: Login streaks maintained through authentication events
- **User initialization**: Gamification data setup for new users
- **Session management**: Integration with user session tracking

### Projects Module Integration

The system integrates with projects for scoped leaderboards:

- **Project leaderboards**: Separate leaderboards for each project
- **Project membership**: Leaderboard access based on project membership
- **Project-specific metrics**: Points and bugs resolved tracked per project
- **Project context**: All gamification data scoped to specific projects

### Notifications Module Integration

The system integrates with notifications for user engagement:

- **Point notifications**: Notifications when users earn points
- **Streak notifications**: Notifications for streak milestones and breaks
- **Leaderboard notifications**: Notifications for rank changes
- **Achievement notifications**: Notifications for special achievements

## Frontend Implementation

### Pages/Routes

**Location**: `frontend/src/pages/`

- `/gamification` - Main gamification dashboard
- `/gamification/profile/{userId}` - User gamification profile
- `/projects/{projectSlug}/leaderboard` - Project leaderboard

### Key Components

**Location**: `frontend/src/components/gamification/`

- `GamificationDashboard` - Main dashboard with points, streaks, and leaderboards
- `UserGamificationProfile` - Detailed user profile with transaction history
- `LeaderboardComponent` - Project leaderboard with timeframe selection
- `StreakVisualizationComponent` - Visual streak display and progress
- `ToastTestComponent` - Testing component for gamification notifications

### Service Integration

**File**: [`frontend/src/services/gamificationService.ts`](frontend/src/services/gamificationService.ts)

The GamificationService provides comprehensive API integration:

- **User points**: Retrieve user point statistics and history
- **Leaderboards**: Access project leaderboards with different timeframes
- **Streaks**: Get user streak information and progress
- **Transactions**: Retrieve point transaction history with pagination

### Type Definitions

**File**: [`frontend/src/types/gamification.ts`](frontend/src/types/gamification.ts)

The gamification types define data structures for:

- **UserPointsResponse**: User point statistics and global metrics
- **LeaderboardEntryResponse**: Leaderboard entry with ranking information
- **StreakInfoResponse**: User streak details and progress
- **PointTransactionResponse**: Individual point transaction records
- **PageResponse**: Pagination wrapper for list responses

### State Management

The frontend uses React hooks for gamification state management:

- **User points**: Cached user point statistics and updates
- **Leaderboard data**: Cached leaderboard data with refresh capabilities
- **Streak information**: Real-time streak updates and progress tracking
- **Transaction history**: Paginated transaction history with loading states
- **Error handling**: Centralized error state management for gamification operations

## Security & Authorization

### Access Control Matrix

| Action                   | Anonymous | User | Project Member | Project Admin | System Admin |
| ------------------------ | --------- | ---- | -------------- | ------------- | ------------ |
| View own points          | No        | Yes  | Yes            | Yes           | Yes          |
| View own streak          | No        | Yes  | Yes            | Yes           | Yes          |
| View own transactions    | No        | Yes  | Yes            | Yes           | Yes          |
| View project leaderboard | No        | No   | Yes            | Yes           | Yes          |
| Award points             | No        | No   | No             | No            | Yes          |
| View all user data       | No        | No   | No             | No            | Yes          |

### Business Rules

1. **User data privacy**: Users can only view their own gamification data
2. **Project membership**: Leaderboard access requires project membership
3. **Point integrity**: Only system operations can award points
4. **Data consistency**: All point transactions must be validated and recorded
5. **Streak accuracy**: Streak calculations must be accurate and consistent
6. **Leaderboard fairness**: Leaderboard rankings must be calculated fairly and consistently

## Technical Considerations

### Performance Optimizations

- **Caching**: Redis caching for frequently accessed leaderboard data
- **Database indexes**: Strategic indexes for efficient gamification queries
- **Batch updates**: Efficient batch updates for leaderboard maintenance
- **Lazy loading**: Progressive loading of gamification data
- **Query optimization**: Optimized database queries for large-scale operations

### Data Consistency

- **Transaction management**: All point operations wrapped in database transactions
- **Constraint validation**: Database constraints ensure data integrity
- **Audit trail**: Complete audit trail of all point-related activities
- **Error handling**: Comprehensive error handling with rollback capabilities
- **Data validation**: Extensive validation of all gamification data

### Scalability Considerations

- **Horizontal scaling**: Stateless design supports multiple server instances
- **Database optimization**: Efficient queries and indexes for large-scale operations
- **Cache distribution**: Redis clustering for high availability
- **Load balancing**: Even distribution of gamification calculation workload
- **Resource management**: Memory and CPU optimization for large-scale operations

## Implementation Status

### Completed Features

- **Point system**: Complete point calculation and award system
- **Leaderboard system**: Project-specific leaderboards with multiple timeframes
- **Streak tracking**: Login streak calculation and management
- **User statistics**: Comprehensive user gamification statistics
- **Transaction history**: Complete audit trail of all point activities
- **Frontend integration**: Complete UI for gamification features
- **API endpoints**: Comprehensive REST API for gamification operations
- **Database schema**: Complete database schema with proper constraints and indexes

### Current Capabilities

- Automatic point awards for bug resolution based on priority levels
- Daily login point awards with streak tracking
- Project-specific leaderboards with weekly, monthly, and all-time rankings
- Comprehensive user statistics including points, streaks, and bugs resolved
- Complete transaction history with pagination and filtering
- Real-time leaderboard updates and notifications
- User gamification profiles with detailed statistics
- Integration with bug tracking, authentication, and notification systems

## Future Enhancements

### Advanced Features

- Achievement system with badges and milestones
- Team-based competitions and challenges
- Seasonal events and special point multipliers
- Advanced analytics and reporting dashboards
- Integration with external gamification platforms

### User Experience Improvements

- Enhanced visualizations for streaks and progress
- Interactive leaderboard animations and transitions
- Personalized gamification recommendations
- Social features for team collaboration
- Mobile-optimized gamification interface

### System Improvements

- Machine learning-based point optimization
- Advanced caching strategies with intelligent invalidation
- Performance monitoring and optimization dashboards
- Integration with external analytics services
- Advanced notification and engagement features
