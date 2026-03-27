# BugTracker Backend

A comprehensive Spring Boot backend application for the BugTracker system, providing RESTful APIs for bug tracking, team management, project organization, and advanced features including duplicate detection, auto-assignment, and gamification.

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Architecture](#architecture)
- [Testing](#testing)
- [Development](#development)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

## Overview

The BugTracker backend is a Spring Boot application that provides a comprehensive REST API for managing bugs, teams, projects, and users. It includes advanced features such as:

- **Authentication & Authorization**: JWT-based authentication with role-based access control
- **Bug Management**: Complete CRUD operations with attachments, comments, and status tracking
- **Team & Project Management**: Hierarchical organization with proper access controls
- **Duplicate Detection**: ML-based similarity algorithms for identifying duplicate bugs
- **Auto Assignment**: Intelligent assignment of bugs to teams and users based on skills and workload
- **Gamification**: Point-based reward system with leaderboards and streaks
- **Analytics**: Comprehensive reporting and statistics
- **Notifications**: Multi-channel notification system with real-time updates

## Technology Stack

### Core Technologies

- **Java 17**: Modern Java features and performance improvements
- **Spring Boot 3.2.0**: Rapid application development framework
- **Spring Security**: Authentication and authorization
- **Spring Data JPA**: Data persistence and repository pattern
- **Spring WebSocket**: Real-time communication
- **PostgreSQL**: Primary database
- **Gradle**: Build automation and dependency management

### Key Dependencies

- **JWT (jjwt)**: JSON Web Token implementation
- **Apache Commons Text**: Text similarity algorithms
- **Lombok**: Code generation and boilerplate reduction
- **Flyway**: Database migration management
- **Resend API**: Email notification service
- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking framework for testing

## Prerequisites

Before setting up the backend, ensure you have the following installed:

### Required Software

#### Java 17 or higher

**Windows:**

1. Download OpenJDK 17 from [Adoptium](https://adoptium.net/)
2. Run the installer and follow the setup wizard
3. Verify installation:
   ```cmd
   java -version
   # Should show version 17 or higher
   ```

**Linux/macOS:**

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# macOS (using Homebrew)
brew install openjdk@17

# Verify installation
java -version
# Should show version 17 or higher
```

#### PostgreSQL 13 or higher

**Windows:**

1. Download PostgreSQL from [postgresql.org](https://www.postgresql.org/download/windows/)
2. Run the installer and follow the setup wizard
3. Remember the password for the `postgres` user
4. Verify installation:
   ```cmd
   psql --version
   # Should show version 13 or higher
   ```

**Linux/macOS:**

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install postgresql postgresql-contrib

# macOS (using Homebrew)
brew install postgresql

# Verify installation
psql --version
# Should show version 13 or higher
```

#### Gradle 8.0 or higher

**Windows:**

1. Download Gradle from [gradle.org](https://gradle.org/releases/)
2. Extract to a directory (e.g., `C:\gradle`)
3. Add `C:\gradle\bin` to your PATH environment variable
4. Verify installation:
   ```cmd
   gradle --version
   # Should show version 8.0 or higher
   ```

**Linux/macOS:**

```bash
# Ubuntu/Debian
sudo apt install gradle

# macOS (using Homebrew)
brew install gradle

# Verify installation
gradle --version
# Should show version 8.0 or higher
```

### Optional Software

- **VS Code** with Java Extension Pack (for development)
- **Postman** (for API testing)

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd bugtracker/backend
```

### 2. Database Setup

See [Database Setup](#database-setup) section for detailed instructions.

### 3. Configuration

Copy the example configuration and update with your settings:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

### 4. Build and Run

```bash
# Build the application
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## Database Setup

### PostgreSQL Installation

#### Ubuntu/Debian

```bash
# Update package list
sudo apt update

# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Start PostgreSQL service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Switch to postgres user
sudo -u postgres psql
```

#### macOS (using Homebrew)

```bash
# Install PostgreSQL
brew install postgresql

# Start PostgreSQL service
brew services start postgresql
```

#### Windows

1. Download PostgreSQL from [postgresql.org](https://www.postgresql.org/download/windows/)
2. Run the installer and follow the setup wizard
3. Remember the password for the `postgres` user

### Database Configuration

#### 1. Create Database and User

```sql
-- Connect to PostgreSQL as superuser
sudo -u postgres psql

-- Create database
CREATE DATABASE bugtracker;

-- Create user
CREATE USER bugtracker_user WITH PASSWORD 'your_secure_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE bugtracker TO bugtracker_user;

-- Grant schema privileges
\c bugtracker
GRANT ALL ON SCHEMA public TO bugtracker_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO bugtracker_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO bugtracker_user;

-- Exit PostgreSQL
\q
```

#### 2. Update Application Configuration

Update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/bugtracker
spring.datasource.username=bugtracker_user
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

## Configuration

### Application Properties

Create `src/main/resources/application.properties` with the following configuration:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/api/bugtracker/v1

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/bugtracker
spring.datasource.username=bugtracker_user
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true


# JWT Configuration
app.jwt.secret=your-super-secret-jwt-key-make-it-long-and-secure
app.jwt.expiration=604800000

# Email Configuration (Resend API)
app.email.api-key=your-resend-api-key
app.email.from-email=noreply@yourdomain.com

# File Upload Configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging Configuration
logging.level.com.pbm5.bugtracker=INFO
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# CORS Configuration
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.cors.allowed-headers=*
app.cors.allow-credentials=true
```

### Environment Variables

For production deployment, use environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/bugtracker
export DB_USERNAME=bugtracker_user
export DB_PASSWORD=your_secure_password
export JWT_SECRET=your-super-secret-jwt-key
export EMAIL_API_KEY=your-resend-api-key
```

## API Documentation

### Base URL

```
http://localhost:8080/api/bugtracker/v1
```

### Authentication Endpoints

- `POST /auth/register` - User registration
- `POST /auth/login` - User login
- `POST /auth/forgot-password` - Password reset request
- `POST /auth/reset-password` - Password reset confirmation

### User Management

- `GET /users/profile` - Get current user profile
- `PUT /users/profile` - Update user profile
- `GET /users` - List users (admin only)

### Project Management

- `GET /projects` - List projects
- `POST /projects` - Create project
- `GET /projects/{projectSlug}` - Get project details
- `PUT /projects/{projectSlug}` - Update project
- `DELETE /projects/{projectSlug}` - Delete project
- `GET /projects/{projectSlug}/members` - Get project members
- `POST /projects/{projectSlug}/members` - Add project member

### Team Management

- `GET /teams` - List teams
- `POST /teams` - Create team
- `GET /teams/{teamSlug}` - Get team details
- `PUT /teams/{teamSlug}` - Update team
- `DELETE /teams/{teamSlug}` - Delete team
- `GET /teams/{teamSlug}/members` - Get team members
- `POST /teams/{teamSlug}/members` - Add team member

### Bug Management

- `GET /projects/{projectSlug}/bugs` - List bugs
- `POST /projects/{projectSlug}/bugs` - Create bug
- `GET /projects/{projectSlug}/bugs/{bugId}` - Get bug details
- `PUT /projects/{projectSlug}/bugs/{bugId}` - Update bug
- `DELETE /projects/{projectSlug}/bugs/{bugId}` - Delete bug
- `POST /projects/{projectSlug}/bugs/{bugId}/comments` - Add comment
- `POST /projects/{projectSlug}/bugs/{bugId}/attachments` - Upload attachment

### Analytics

- `GET /projects/{projectSlug}/analytics` - Get project analytics
- `GET /projects/{projectSlug}/analytics/teams` - Get team performance
- `GET /projects/{projectSlug}/analytics/duplicates` - Get duplicate analytics

### Gamification

- `GET /gamification/users/{userId}/points` - Get user points
- `GET /gamification/users/{userId}/streak` - Get user streak
- `GET /gamification/users/{userId}/transactions` - Get point history
- `GET /gamification/projects/{projectId}/leaderboard` - Get project leaderboard

### Notifications

- `GET /notifications` - Get user notifications
- `PUT /notifications/{notificationId}/read` - Mark notification as read
- `GET /notifications/preferences` - Get notification preferences
- `PUT /notifications/preferences` - Update notification preferences

## Architecture

### Package Structure

```
com.pbm5.bugtracker/
├── BugTrackerApplication.java          # Main application class
├── config/                             # Configuration classes
│   ├── SecurityConfig.java            # Security configuration
│   ├── WebSocketConfig.java           # WebSocket configuration
│   └── CacheConfig.java               # Cache configuration
├── controller/                         # REST controllers
│   ├── AuthController.java            # Authentication endpoints
│   ├── BugController.java             # Bug management endpoints
│   ├── ProjectController.java         # Project management endpoints
│   ├── TeamController.java            # Team management endpoints
│   └── GamificationController.java    # Gamification endpoints
├── service/                           # Business logic services
│   ├── AuthService.java               # Authentication logic
│   ├── BugService.java                # Bug management logic
│   ├── ProjectService.java            # Project management logic
│   ├── TeamService.java               # Team management logic
│   └── GamificationService.java       # Gamification logic
├── repository/                        # Data access layer
│   ├── UserRepository.java            # User data access
│   ├── BugRepository.java             # Bug data access
│   ├── ProjectRepository.java         # Project data access
│   └── TeamRepository.java            # Team data access
├── entity/                            # JPA entities
│   ├── User.java                      # User entity
│   ├── Bug.java                       # Bug entity
│   ├── Project.java                   # Project entity
│   └── Team.java                      # Team entity
├── dto/                               # Data transfer objects
│   ├── LoginRequest.java              # Login request DTO
│   ├── BugResponse.java               # Bug response DTO
│   └── ProjectResponse.java           # Project response DTO
├── exception/                         # Custom exceptions
│   ├── BugNotFoundException.java      # Bug not found exception
│   └── ProjectNotFoundException.java  # Project not found exception
└── util/                              # Utility classes
    ├── JwtUtil.java                   # JWT utility
    └── AuthenticationUtils.java       # Authentication utilities
```

### Database Schema

The application uses Flyway for database migrations. Key tables include:

- **users**: User accounts and profiles
- **projects**: Project information and metadata
- **teams**: Team information and project associations
- **bugs**: Bug reports and tracking information
- **bug_comments**: Comments on bugs
- **bug_attachments**: File attachments for bugs
- **user_points**: Gamification points and statistics
- **project_leaderboard**: Project-specific leaderboards
- **point_transactions**: Audit trail for point awards
- **notifications**: User notifications and preferences

### Security Architecture

- **JWT Authentication**: Stateless authentication with configurable expiration
- **Role-Based Access Control**: ADMIN, DEVELOPER, REPORTER roles
- **Project-Scoped Permissions**: Access control based on project membership
- **CORS Configuration**: Configurable cross-origin resource sharing
- **Input Validation**: Comprehensive validation for all inputs
- **SQL Injection Prevention**: Parameterized queries and JPA

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.pbm5.bugtracker.service.BugServiceTest"

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Test Structure

```
src/test/java/
├── com.pbm5.bugtracker/
│   ├── service/                       # Service layer tests
│   │   ├── BugServiceTest.java        # Bug service tests
│   │   ├── ProjectServiceTest.java    # Project service tests
│   │   └── GamificationServiceTest.java # Gamification tests
│   ├── controller/                    # Controller tests
│   │   ├── BugControllerTest.java     # Bug controller tests
│   │   └── AuthControllerTest.java    # Auth controller tests
│   └── repository/                    # Repository tests
│       ├── BugRepositoryTest.java     # Bug repository tests
│       └── UserRepositoryTest.java    # User repository tests
└── resources/
    └── application-test.properties    # Test configuration
```

### Test Configuration

Create `src/test/resources/application-test.properties`:

```properties
# Test Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration for Tests
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Disable Flyway for Tests
spring.flyway.enabled=false

# Test JWT Secret
app.jwt.secret=test-secret-key-for-testing-only

# Disable Email for Tests
app.email.api-key=test-key
```

## Development

### Development Setup

1. **IDE Configuration**

   - Install Lombok plugin
   - Configure code formatting
   - Set up debug configurations

2. **Code Style**

   - Follow Java naming conventions
   - Use meaningful variable names
   - Add comprehensive JavaDoc comments
   - Follow Spring Boot best practices

3. **Git Workflow**

   ```bash
   # Create feature branch
   git checkout -b feature/new-feature

   # Make changes and commit
   git add .
   git commit -m "feat: add new feature"

   # Push and create pull request
   git push origin feature/new-feature
   ```

### Adding New Features

1. **Create Entity** (if needed)

   ```java
   @Entity
   @Table(name = "new_entity")
   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public class NewEntity {
       @Id
       @GeneratedValue(strategy = GenerationType.UUID)
       private UUID id;

       // Add fields
   }
   ```

2. **Create Repository**

   ```java
   @Repository
   public interface NewEntityRepository extends JpaRepository<NewEntity, UUID> {
       // Add custom queries
   }
   ```

3. **Create Service**

   ```java
   @Service
   @Transactional
   @RequiredArgsConstructor
   public class NewEntityService {
       private final NewEntityRepository repository;

       // Add business logic
   }
   ```

4. **Create Controller**

   ```java
   @RestController
   @RequestMapping("/api/bugtracker/v1/new-entities")
   @RequiredArgsConstructor
   public class NewEntityController {
       private final NewEntityService service;

       // Add endpoints
   }
   ```

5. **Add Tests**

   ```java
   @ExtendWith(MockitoExtension.class)
   class NewEntityServiceTest {
       @Mock
       private NewEntityRepository repository;

       @InjectMocks
       private NewEntityService service;

       // Add test methods
   }
   ```

### Database Migrations

1. **Create Migration**

   ```bash
   # Create new migration file
   touch src/main/resources/db/migration/V{version}__{description}.sql
   ```

2. **Migration Example**

   ```sql
   -- V30__Add_new_table.sql
   CREATE TABLE new_table (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       name VARCHAR(255) NOT NULL,
       created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
       updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
   );

   CREATE INDEX idx_new_table_name ON new_table(name);
   ```

## Deployment

### Production Deployment

1. **Environment Variables**

   ```bash
   export SPRING_PROFILES_ACTIVE=production
   export DB_URL=jdbc:postgresql://prod-db:5432/bugtracker
   export DB_USERNAME=bugtracker_user
   export DB_PASSWORD=secure_production_password
   export JWT_SECRET=super-secure-production-jwt-secret
   export REDIS_HOST=prod-redis
   export EMAIL_API_KEY=production-email-api-key
   ```

2. **Build for Production**

   ```bash
   # Build with production profile
   ./gradlew build -Pprofile=production

   # Run with production configuration
   java -jar build/libs/bugtracker-backend-1.0.0.jar
   ```

## Troubleshooting

### Common Issues

#### Database Connection Issues

```bash
# Check PostgreSQL status
sudo systemctl status postgresql

# Check if database exists
psql -U bugtracker_user -d bugtracker -c "\l"

# Check database permissions
psql -U bugtracker_user -d bugtracker -c "\dp"
```

#### Port Already in Use

```bash
# Find process using port 8080
sudo lsof -i :8080

# Kill process
sudo kill -9 <PID>

# Or change port in application.properties
server.port=8081
```

#### JWT Token Issues

```bash
# Check JWT secret configuration
grep -r "jwt.secret" src/main/resources/

# Verify token format
echo "your-jwt-token" | base64 -d
```

### Logs and Debugging

#### Enable Debug Logging

```properties
# Add to application.properties
logging.level.com.pbm5.bugtracker=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

#### View Application Logs

```bash
# View logs in real-time
tail -f logs/application.log

# Search for specific errors
grep -i "error" logs/application.log

# View recent logs
tail -n 100 logs/application.log
```

#### Database Query Debugging

```properties
# Enable SQL logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Performance Issues

#### Database Performance

```sql
-- Check slow queries
SELECT query, mean_time, calls
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Check table sizes
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

#### Memory Issues

```bash
# Check JVM memory usage
jstat -gc <pid>

# Check application memory
jmap -histo <pid>

# Generate heap dump
jmap -dump:format=b,file=heap.hprof <pid>
```

### Getting Help

1. **Check Logs**: Always check application logs first
2. **Verify Configuration**: Ensure all configuration values are correct
3. **Test Dependencies**: Verify database connections
4. **Check Documentation**: Refer to Spring Boot and PostgreSQL documentation
5. **Community Support**: Use Stack Overflow for specific issues

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:

- Create an issue in the repository
- Contact: pbm5@student.le.ac.uk
- Documentation: See `/docs` folder for detailed specifications
