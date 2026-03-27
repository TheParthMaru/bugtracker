# BugTracker - Software Bug Tracking and Reporting Tool

[![React](https://img.shields.io/badge/React-19.1.0-blue.svg)](https://reactjs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg)](https://spring.io/projects/spring-boot)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.8.3-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13+-blue.svg)](https://www.postgresql.org/)

A comprehensive, modern bug tracking and reporting system built with React, Spring Boot, and PostgreSQL. The system provides advanced features including duplicate detection, auto-assignment, gamification, and real-time analytics.

## Table of Contents

- [Project Overview](#project-overview)
- [Student Information](#student-information)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Documentation](#documentation)
- [Requirements Status](#requirements-status)
- [Advanced Features](#advanced-features)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

## Project Overview

The BugTracker system is a full-stack web application designed to streamline bug reporting, tracking, and resolution processes. It provides a comprehensive platform for software development teams to manage bugs efficiently with advanced features like intelligent duplicate detection, automatic assignment, and gamification elements.

### Key Highlights

- **Modern Architecture**: Built with React 19, Spring Boot 3, and PostgreSQL
- **Advanced Features**: Duplicate detection, auto-assignment, and gamification
- **Real-time Updates**: WebSocket integration for live notifications
- **Comprehensive Analytics**: Interactive dashboards and reporting
- **Mobile Responsive**: Optimized for all device types
- **Secure**: JWT authentication with role-based access control

## Student Information

- **Student Name**: Parth Maru
- **Student Email**: pbm5@student.le.ac.uk
- **University**: University of Leicester

## Features

### Core MVP Features

#### Authentication & Security

- User registration and login with JWT tokens
- Password reset functionality
- Role-based access control (ADMIN, DEVELOPER, REPORTER)
- Secure API endpoints with authentication

#### Bug Management

- Comprehensive bug reporting with attachments
- Advanced filtering, sorting, and search capabilities
- Bug status tracking and updates
- Comment system with real-time updates
- File attachment support (images, documents, videos)

#### Project Management

- Project creation and configuration
- Project member management
- Project-scoped bug tracking
- Project analytics and reporting

#### Team Management

- Team creation and management
- Team member roles and permissions
- Team-based bug assignments
- Team performance analytics

#### Analytics & Reporting

- Project-specific analytics dashboards
- Team performance metrics
- Bug resolution statistics
- Export functionality for external analysis

#### Notifications

- Real-time notifications via WebSocket
- Multi-channel delivery (in-app, email, toast)
- User-configurable notification preferences
- Notification history and audit trail

### Advanced Features

#### Duplicate Bug Detection

- Real-time similarity analysis using multiple algorithms
- Configurable similarity thresholds
- Visual warnings for potential duplicates
- Duplicate relationship management

#### Auto Assignment

- Intelligent team assignment based on bug labels
- Skill-based user assignment
- Workload balancing considerations
- Assignment recommendations with reasoning

#### Gamification

- Point system for bug resolution and daily logins
- Project-specific leaderboards (weekly, monthly, all-time)
- Login streak tracking and visualization
- Achievement badges and milestones

## Technology Stack

### Frontend

- **React 19.1.0**: Modern React with concurrent features
- **TypeScript 5.8.3**: Type-safe development
- **Vite 6.3.5**: Fast build tool and development server
- **Tailwind CSS 4.1.8**: Utility-first CSS framework
- **ShadCN/UI**: Pre-built, accessible components
- **Radix UI**: Unstyled UI primitives
- **React Router DOM**: Client-side routing
- **Axios**: HTTP client for API communication
- **React Hook Form**: Form handling and validation
- **Zod**: Schema validation

### Backend

- **Spring Boot 3.2.0**: Java framework for microservices
- **Spring Data JPA**: Data persistence layer
- **Spring Security**: Authentication and authorization
- **Spring WebSocket**: Real-time communication
- **PostgreSQL 13+**: Primary database
- **JWT**: Token-based authentication
- **BCrypt**: Password hashing
- **Apache Commons Text**: Text similarity algorithms
- **Resend API**: Email service integration

### Development Tools

- **Gradle**: Build automation
- **ESLint**: Code linting
- **Prettier**: Code formatting
- **JUnit 5**: Unit testing
- **Mockito**: Mocking framework
- **AssertJ**: Assertion library

## Project Structure

```
pbm5/
├── bugtracker/                    # Main application directory
│   ├── backend/                   # Spring Boot backend
│   │   ├── src/                   # Source code
│   │   │   ├── main/java/         # Java source files
│   │   │   │   └── com/pbm5/bugtracker/
│   │   │   │       ├── controller/    # REST controllers
│   │   │   │       ├── service/       # Business logic
│   │   │   │       ├── repository/    # Data access layer
│   │   │   │       ├── entity/        # JPA entities
│   │   │   │       ├── dto/           # Data transfer objects
│   │   │   │       ├── config/        # Configuration classes
│   │   │   │       ├── exception/     # Custom exceptions
│   │   │   │       └── validation/    # Validation logic
│   │   │   └── resources/         # Configuration files
│   │   │       ├── application.properties
│   │   │       ├── db/migration/  # Database migrations
│   │   │       └── templates/     # Email templates
│   │   ├── build.gradle           # Build configuration
│   │   ├── README.md              # Backend documentation
│   │   └── gradlew                # Gradle wrapper
│   ├── frontend/                  # React frontend
│   │   ├── src/                   # Source code
│   │   │   ├── components/        # React components
│   │   │   │   ├── analytics/     # Analytics components
│   │   │   │   ├── bugs/          # Bug-related components
│   │   │   │   ├── gamification/  # Gamification components
│   │   │   │   ├── notifications/ # Notification components
│   │   │   │   ├── projects/      # Project components
│   │   │   │   ├── teams/         # Team components
│   │   │   │   └── ui/            # Base UI components
│   │   │   ├── pages/             # Page components
│   │   │   ├── services/          # API services
│   │   │   ├── hooks/             # Custom React hooks
│   │   │   ├── types/             # TypeScript type definitions
│   │   │   └── utils/             # Utility functions
│   │   ├── package.json           # Dependencies and scripts
│   │   ├── README.md              # Frontend documentation
│   │   └── vite.config.ts         # Vite configuration
│   └── docs/                      # Project documentation
│       ├── specs/                 # Technical specifications
│       │   ├── 01-system-architecture.md
│       │   ├── 02-authentication-security.md
│       │   ├── 03-projects-module.md
│       │   ├── 04-teams-module.md
│       │   ├── 05-bugs-module.md
│       │   ├── 06-analytics-module.md
│       │   ├── 07-notifications-module.md
│       │   ├── 08-duplicate-bug-detection.md
│       │   ├── 09-auto-assignment.md
│       │   └── 10-gamification.md
│       ├── planner.md             # Project timeline
│       └── initial-project-specification.md
├── docs/                          # University documentation
│   ├── 1_prelim_report/           # Preliminary report
│   ├── 2_interim_report/          # Interim report
│   ├── 3_final_report_template/   # Final report template
│   ├── 4_final_report/            # Final report
│   └── 5_presentation/            # Presentation materials
└── README.md                      # This file
```

## Quick Start

### Prerequisites

- **Java 17+**: For backend development
- **Node.js 18+**: For frontend development
- **PostgreSQL 13+**: Database server
- **Git**: Version control

### Backend Setup

```bash
# Navigate to backend directory
cd bugtracker/backend

# Install dependencies and build
./gradlew build

# Run the application
./gradlew bootRun
```

The backend will start on `http://localhost:8080`

### Frontend Setup

```bash
# Navigate to frontend directory
cd bugtracker/frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:5173`

### Database Setup

1. Install PostgreSQL 13+
2. Create a database named `bugtracker`
3. Update `backend/src/main/resources/application.properties` with your database credentials
4. Run the application - database migrations will be applied automatically

For detailed setup instructions, see:

- [Backend README](bugtracker/backend/README.md)
- [Frontend README](bugtracker/frontend/README.md)

## Documentation

### Technical Specifications

Comprehensive technical documentation is available in the `/docs/specs/` directory:

1. **[System Architecture](bugtracker/docs/specs/01-system-architecture.md)** - Overall system design and architecture
2. **[Authentication & Security](bugtracker/docs/specs/02-authentication-security.md)** - Security implementation and authentication flows
3. **[Projects Module](bugtracker/docs/specs/03-projects-module.md)** - Project management functionality
4. **[Teams Module](bugtracker/docs/specs/04-teams-module.md)** - Team management and collaboration
5. **[Bugs Module](bugtracker/docs/specs/05-bugs-module.md)** - Core bug tracking functionality
6. **[Analytics Module](bugtracker/docs/specs/06-analytics-module.md)** - Analytics and reporting features
7. **[Notifications Module](bugtracker/docs/specs/07-notifications-module.md)** - Notification system implementation
8. **[Duplicate Bug Detection](bugtracker/docs/specs/08-duplicate-bug-detection.md)** - Advanced duplicate detection algorithms
9. **[Auto Assignment](bugtracker/docs/specs/09-auto-assignment.md)** - Intelligent assignment system
10. **[Gamification](bugtracker/docs/specs/10-gamification.md)** - Gamification features and implementation

### Project Planning

- **[Project Planner](bugtracker/docs/planner.md)** - Development timeline and milestones
- **[Initial Project Specification](bugtracker/docs/initial-project-specification.md)** - Original requirements and scope

## Requirements Status

### Essential Requirements ✅ (100% Complete)

- ✅ Users can submit new bug reports with detailed descriptions and attachments
- ✅ Developers can comment on and update bug status as they work on fixes
- ✅ Users can track the status of the bugs they report
- ✅ Basic role-based access control (users and developers)
- ✅ Bug database can be searched and filtered
- ✅ New bug reports are automatically classified into priority levels
- ✅ Duplicate detection system to identify already reported bugs
- ✅ Basic analytics and statistics on bug resolution rates

### Desirable Requirements ✅ (80% Complete)

- ✅ Developer assignment based on skill tags
- ✅ Extended analytics (rate of fixes per developer, per priority, weekly reports, etc.)
- ❌ Integration with Git platforms (GitHub, GitLab) to track repository issues
- ✅ Screen capture or video upload feature for bug reproduction evidence
- ✅ Admin dashboard for project-wide monitoring

### Optional Requirements ✅ (50% Complete)

- ❌ Integration with external datasets to enhance bug classification
- ✅ Automatic email or notification system for bug status updates
- ✅ Mobile-friendly interface or mobile app support
- ❌ User feedback rating system for resolved bugs

## Advanced Features

### Duplicate Bug Detection

The system implements sophisticated duplicate detection using multiple similarity algorithms:

- **Cosine Similarity**: Measures semantic similarity between bug descriptions
- **Jaccard Similarity**: Compares word sets for overlap
- **Levenshtein Distance**: Measures edit distance between texts
- **Weighted Combination**: Combines multiple algorithms for optimal results

**Key Files:**

- `backend/src/main/java/com/pbm5/bugtracker/service/BugSimilarityService.java`
- `frontend/src/hooks/useDuplicateDetection.ts`

### Auto Assignment

Intelligent assignment system that considers multiple factors:

- **Team Assignment**: Based on bug labels and team expertise
- **User Assignment**: Based on skills, workload, and availability
- **Workload Balancing**: Ensures fair distribution of work
- **Recommendation Engine**: Provides reasoning for assignments

**Key Files:**

- `backend/src/main/java/com/pbm5/bugtracker/service/AssignmentOrchestrator.java`
- `frontend/src/components/bugs/TeamAssignmentSection.tsx`

### Gamification

Comprehensive gamification system to enhance user engagement:

- **Point System**: Points for bug resolution, daily logins, and achievements
- **Leaderboards**: Project-specific rankings with multiple timeframes
- **Streaks**: Login streak tracking and visualization
- **Achievements**: Badges and milestone recognition

**Key Files:**

- `backend/src/main/java/com/pbm5/bugtracker/service/GamificationService.java`
- `frontend/src/components/gamification/GamificationDashboard.tsx`

## Development

### Development Workflow

1. **Feature Development**

   ```bash
   # Create a new feature branch
   git checkout -b feature/new-feature

   # Make your changes
   # Test your changes

   # Commit your changes
   git add .
   git commit -m "feat: add new feature"

   # Push and create merge request
   git push origin feature/new-feature
   ```

2. **Code Quality**

   ```bash
   # Backend
   cd bugtracker/backend
   ./gradlew test
   ./gradlew check

   # Frontend
   cd bugtracker/frontend
   npm run lint
   npm run type-check
   ```

### Environment Configuration

#### Backend Environment Variables

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/bugtracker
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT Configuration
jwt.secret=your_jwt_secret
jwt.expiration=604800000

# Email Configuration
resend.api.key=your_resend_api_key
```

#### Frontend Environment Variables

```env
# API Configuration
VITE_API_BASE_URL=http://localhost:8080/api/bugtracker/v1
VITE_WS_URL=ws://localhost:8080/ws

# Application Configuration
VITE_APP_NAME=BugTracker
VITE_APP_VERSION=1.0.0
```

## Testing

### Backend Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "BugServiceTest"

# Generate test coverage report
./gradlew jacocoTestReport
```

### Frontend Testing

```bash
# Run tests
npm run test

# Run tests with coverage
npm run test:coverage

# Run tests in watch mode
npm run test:watch
```

### Test Coverage

- **Backend**: Comprehensive unit tests for all services and controllers
- **Frontend**: Component tests and integration tests
- **API**: End-to-end testing with Postman collections

## Deployment

### Production Deployment

#### Backend Deployment

```bash
# Build the application
./gradlew build

# Run the JAR file
java -jar build/libs/bugtracker-0.0.1-SNAPSHOT.jar
```

#### Frontend Deployment

```bash
# Build the application
npm run build

# Deploy the dist/ directory to your web server
```

### Docker Deployment

```bash
# Build and run with Docker Compose
docker-compose up -d
```

### Environment Setup

1. **Production Database**: Set up PostgreSQL with proper security
2. **Environment Variables**: Configure production environment variables
3. **SSL Certificates**: Set up HTTPS for secure communication
4. **Monitoring**: Implement logging and monitoring solutions

## Contributing

This is an individual academic project. However, if you have suggestions or find issues:

1. Create an issue in the repository
2. Provide detailed description of the problem or suggestion
3. Include steps to reproduce (for bugs)
4. Contact: pbm5@student.le.ac.uk

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Repository Information

- **Repository URL**: [https://campus.cs.le.ac.uk/gitlab/pgt_project/24_25_summer/pbm5/](https://campus.cs.le.ac.uk/gitlab/pgt_project/24_25_summer/pbm5/)
- **Project URL**: [https://campus.cs.le.ac.uk/gitlab/pgt_project/24_25_summer/pbm5/-/tree/main/bugtracker](https://campus.cs.le.ac.uk/gitlab/pgt_project/24_25_summer/pbm5/-/tree/main/bugtracker)

## Support

For support and questions:

- **Email**: pbm5@student.le.ac.uk
- **Documentation**: See `/docs` folder for detailed specifications
- **Issues**: Create an issue in the repository

---

**Note**: This is an individual academic project developed as part of the University of Leicester's Software Engineering program. The project demonstrates comprehensive full-stack development skills, advanced software engineering concepts, and modern development practices.
