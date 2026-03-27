# Authentication & Security Specification

## Overview

The Bug Tracker application implements a comprehensive, stateless authentication system using JSON Web Tokens (JWT) with BCrypt password hashing. The security architecture follows industry best practices for web application security, providing secure user registration, authentication, and session management across both frontend and backend components. The system includes gamification integration during login, password reset functionality, and WebSocket authentication.

## Security Architecture

### Authentication Strategy

- **Stateless Authentication**: JWT-based authentication for scalability
- **Token-Based Sessions**: No server-side session storage
- **Role-Based Access Control (RBAC)**: User roles with specific permissions (ADMIN, DEVELOPER, REPORTER)
- **Secure Password Storage**: BCrypt hashing with automatic salt generation
- **Gamification Integration**: Daily login points awarded during authentication
- **Password Reset**: Email-based password reset functionality
- **WebSocket Authentication**: JWT tokens used for WebSocket connections

## JWT Implementation

### Token Structure

```json
{
	"header": {
		"alg": "HS256",
		"typ": "JWT"
	},
	"payload": {
		"sub": "user@example.com",
		"iat": 1609459200,
		"exp": 1609462800
	},
	"signature": "HMAC-SHA256-signature"
}
```

### JWT Service Implementation

**Location**: `backend/src/main/java/com/pbm5/bugtracker/service/JwtService.java`

#### Key Methods

- **`generateToken(String email)`**: Creates signed JWT tokens with user email as subject
- **`extractEmail(String token)`**: Safely extracts user email from token subject
- **`isTokenValid(String token)`**: Verifies token signature and expiration
- **`getSigningKey()`**: Private method that returns HMAC-SHA256 signing key

#### Security Configuration

- **Signing Algorithm**: HMAC-SHA256 (HS256)
- **Secret Key**: Configurable via `jwt.secret-key` property (default: hardcoded for development)
- **Token Expiration**: Configurable via `jwt.expiration-ms` property (default: 7 days = 604800000ms)
- **Key Management**: Uses JJWT Keys utility for secure key generation
- **Configuration**: Externalized to `application.properties` for production override

### Token Lifecycle

![Token Lifecycle](/docs/images/token-lifecycle.png)

## Authentication Flows

### User Registration Flow

![alt text](/docs/images/user-registration-flow.png)

#### Registration Validation Rules

- **Email**: Valid email format with proper domain validation, unique in database
- **Password**: Minimum 8 characters, complexity requirements via `@ValidPassword` annotation
- **Confirm Password**: Must match password via `@PasswordsMatch` annotation
- **Name Fields**: Non-empty first and last name via `@NotBlank`
- **Role**: Must be one of ADMIN, DEVELOPER, REPORTER via `@NotNull`
- **Skills**: Optional, Set<String> for multiple skills

### User Login Flow

![User Login Flow](/docs/images/user-login-flow.png)

#### Login Error Handling

- **User Not Found**: 401 Unauthorized - "User not found"
- **Wrong Password**: 401 Unauthorized - "Invalid password"
- **Validation Errors**: 400 Bad Request with field-specific errors
- **Gamification Errors**: Logged but don't fail login process
- **Server Errors**: 500 Internal Server Error with generic message

## Spring Security Configuration

### Security Filter Chain

**Location**: `backend/src/main/java/com/pbm5/bugtracker/config/SecurityConfig.java`

#### Security Features

- **CORS Enabled**: Cross-origin requests allowed for frontend communication with wildcard origins
- **CSRF Disabled**: Not needed for stateless JWT authentication
- **Path-Based Authorization**: Different rules for public and protected endpoints
- **JWT Filter Integration**: Custom `JwtAuthFilter` for token validation
- **Method Security**: `@EnableMethodSecurity(prePostEnabled = true)` for method-level authorization

#### Public Endpoints (No Authentication Required)

- `/actuator/**` - Spring Boot Actuator endpoints
- `/api/auth/**` - Legacy auth endpoints
- `/api/bugtracker/v1/auth/**` - Authentication endpoints (register, login, password reset)
- `/ws-notifications/**` - WebSocket notification endpoints
- `/ws-notifications-native/**` - Native WebSocket notification endpoints

#### Protected Endpoints (Authentication Required)

- `/api/bugtracker/v1/profile` - User profile management
- `/api/bugtracker/v1/teams/**` - Team management
- `/api/bugtracker/v1/projects/**` - Project management
- `/api/bugtracker/v1/bugs/**` - Bug management
- `/api/bugtracker/v1/users/me/teams` - User's teams
- `/api/similarity/**` - Similarity analysis
- `/api/**` - All other API endpoints

### JWT Authentication Filter

**Location**: `backend/src/main/java/com/pbm5/bugtracker/config/JwtAuthFilter.java`

#### Filter Features

- **Header Validation**: Checks for "Bearer " prefix in Authorization header
- **Token Extraction**: Safely extracts JWT from Authorization header (substring after "Bearer ")
- **User Loading**: Fetches user details from database using email from token
- **Authority Setting**: Sets Spring Security authorities based on user role (ROLE_ADMIN, ROLE_DEVELOPER, ROLE_REPORTER)
- **Context Management**: Properly sets SecurityContextHolder with UsernamePasswordAuthenticationToken
- **Request Details**: Sets WebAuthenticationDetailsSource for request tracking

#### Filter Process

1. Extract Authorization header
2. Validate "Bearer " prefix
3. Extract token (substring after "Bearer ")
4. Extract email from token using JwtService
5. Load user from database by email
6. Validate token using JwtService
7. Set Spring Security context with user and authorities
8. Continue filter chain

## Frontend Security Implementation

### Protected Route Component

**Location**: `frontend/src/components/ProtectedRoute.tsx`

#### Security Features

- **Token Verification**: Checks for token presence in localStorage using key "bugtracker_token"
- **Automatic Redirection**: Redirects to `/auth/login` if no token found
- **Route Protection**: Wraps protected components
- **Navigation Replacement**: Uses `replace` to prevent back-button issues

#### Implementation

```typescript
export default function ProtectedRoute({ children }: { children: ReactNode }) {
	const token = localStorage.getItem("bugtracker_token");

	if (!token) {
		return <Navigate to="/auth/login" replace />;
	}

	return <>{children}</>;
}
```

### API Service Configuration

**Location**: `frontend/src/services/api.ts`

#### Security Features

- **Automatic Token Attachment**: Adds JWT to all requests via request interceptor
- **Centralized Configuration**: Single Axios instance with base URL configuration
- **Error Handling**: Global 401 handling with automatic logout and redirect
- **Development Logging**: Comprehensive request/response logging in development mode

#### Request Interceptor

```typescript
API.interceptors.request.use((config) => {
	const token = localStorage.getItem("bugtracker_token");
	if (token) {
		config.headers.Authorization = `Bearer ${token}`;
	}
	return config;
});
```

#### Response Interceptor

```typescript
API.interceptors.response.use(
	(response) => response,
	(error) => {
		if (error.response?.status === 401) {
			localStorage.removeItem("bugtracker_token");
			const isLoginRequest = error.config?.url?.includes("/auth/login");
			const isOnLoginPage = window.location.pathname === "/auth/login";

			if (!isOnLoginPage && !isLoginRequest) {
				window.location.href = "/auth/login";
			}
		}
		return Promise.reject(error);
	}
);
```

## Password Security

### Password Hashing Implementation

**Backend**: BCrypt with automatic salt generation

#### Security Features

- **Adaptive Hashing**: BCrypt automatically handles salt generation
- **Cost Factor**: Configurable work factor (default: 10 rounds)
- **Rainbow Table Resistance**: Each password has unique salt
- **Future-Proof**: Algorithm can be upgraded without breaking existing passwords

### Password Validation

**Location**: `backend/src/main/java/com/pbm5/bugtracker/validation/`

#### Validation Rules

- **Minimum Length**: 8 characters
- **Complexity**: Custom rules via `@ValidPassword` annotation
- **Confirmation**: Password and confirm password must match via `@PasswordsMatch` annotation
- **Client-Side**: React Hook Form + Zod validation
- **Server-Side**: Bean validation with custom validators

#### Custom Validators

- **`ValidPassword`**: Custom password complexity validation
- **`PasswordsMatch`**: Ensures password and confirmPassword fields match
- **`PasswordValidator`**: Implements password strength requirements

## Role-Based Access Control (RBAC)

### User Roles

```java
public enum Role {
    ADMIN,      // Full system access
    DEVELOPER,  // Development team access
    REPORTER    // Bug reporting access
}
```

### Authorization Matrix

| Resource             | ADMIN | DEVELOPER | REPORTER |
| -------------------- | ----- | --------- | -------- |
| User Management      | Yes   | No        | No       |
| Profile Access       | Yes   | Yes       | Yes      |
| Bug Creation         | Yes   | Yes       | Yes      |
| Bug Assignment       | Yes   | Yes       | No       |
| Project Management   | Yes   | Yes       | No       |
| System Configuration | Yes   | No        | No       |

### Implementation Strategy

- **Database Level**: Role stored in user entity as enum (ADMIN, DEVELOPER, REPORTER)
- **JWT Level**: Role not included in token claims (loaded from database during authentication)
- **Spring Security**: Method-level authorization with @PreAuthorize and authorities (ROLE_ADMIN, ROLE_DEVELOPER, ROLE_REPORTER)
- **Frontend**: Role-based UI rendering and route access based on user profile data
- **Authentication Utils**: `AuthenticationUtils.getCurrentUserId()` for extracting user ID from SecurityContext

## Session Management

### Token Storage Strategy

#### Current Implementation: localStorage

```typescript
// Store token
localStorage.setItem("bugtracker_token", token);

// Retrieve token
const token = localStorage.getItem("bugtracker_token");

// Remove token (logout)
localStorage.removeItem("bugtracker_token");
```

#### Security Considerations

**Pros:**

- Simple implementation
- Persists across browser sessions
- Easy to access from JavaScript

**Cons:**

- Vulnerable to XSS attacks
- Accessible to all JavaScript on the page
- Not automatically sent with requests

### Token Refresh Strategy (Current Implementation)

```
JWT Token (Long-lived: 7 days)
    ↓
Client-side token validation
    ↓
Automatic logout on 401 responses
    ↓
Redirect to login page
```

**Note**: Currently using long-lived tokens (7 days) with automatic logout on expiration. Refresh token mechanism is planned for future implementation.

## Error Handling & Security Logging

### Global Exception Handler

**Location**: `backend/src/main/java/com/pbm5/bugtracker/config/GlobalExceptionHandler.java`

#### Security-Related Exception Handling

- **Authentication Failures**: Consistent error messages to prevent user enumeration
- **Authorization Failures**: Generic "Access Denied" responses
- **Validation Errors**: Detailed field-level validation feedback
- **JWT Errors**: Token expiration and invalid token handling

### Security Logging

#### Current Logging Configuration

```properties
# Security-specific logging
logging.level.com.pbm5.bugtracker=DEBUG
logging.level.org.springframework.security=DEBUG
logging.file.name=logs/bugtracker.log
```

#### Security Events to Log

- **Authentication Attempts**: Success and failure
- **Authorization Failures**: Access denied events
- **Token Operations**: Generation, validation, expiration
- **Suspicious Activity**: Multiple failed attempts, invalid tokens

## Security Compliance & Best Practices

### Current Implementation Status

**Password Hashing**: BCrypt implementation
**JWT Authentication**: Stateless token-based auth
**Input Validation**: Bean validation and custom validators
**CORS Configuration**: Proper cross-origin setup
**Error Handling**: Consistent error responses
**Role-Based Access**: User role implementation

### Security Enhancements (Roadmap)

- **Token Refresh**: Implement refresh token mechanism for better security
- **Rate Limiting**: API rate limiting to prevent abuse
- **Account Lockout**: Temporary lockout after failed attempts
- **Password Policies**: Enhanced password complexity requirements
- **Audit Logging**: Comprehensive security event logging
- **Security Headers**: Production security headers (CSP, HSTS, etc.)
- **Token Storage**: Migration to HttpOnly cookies for XSS protection
- **Multi-Factor Authentication**: 2FA implementation
- **JWT Claims**: Include user role in JWT claims for reduced database calls

### Compliance Considerations

- **OWASP Top 10**: Address web application security risks
- **Data Protection**: User data encryption and privacy
- **Access Control**: Principle of least privilege
- **Audit Trail**: Security event logging and monitoring

## WebSocket Authentication

### WebSocket Security Implementation

**Location**: `frontend/src/hooks/useWebSocketNotifications.ts`

#### Security Features

- **JWT Token Validation**: Validates JWT token before WebSocket connection
- **Authorization Header**: Includes Bearer token in WebSocket connection headers
- **Connection Validation**: Checks for valid token presence before attempting connection
- **Error Handling**: Graceful handling of authentication failures

#### WebSocket Connection Process

```typescript
// Validate JWT token before attempting connection
const token = localStorage.getItem("bugtracker_token");
if (
	!token ||
	token === "null" ||
	token === "undefined" ||
	token.trim() === ""
) {
	console.warn("No valid JWT token found, skipping WebSocket connection");
	setError("Authentication required - please log in");
	return;
}

// Create STOMP client with authentication
const client = new Client({
	brokerURL: WS_URL,
	connectHeaders: {
		Authorization: `Bearer ${token}`,
	},
	// ... other configuration
});
```

## Password Reset Functionality

### Password Reset Implementation

**Location**: `backend/src/main/java/com/pbm5/bugtracker/controller/AuthController.java`

#### Available Endpoints

- **`POST /api/bugtracker/v1/auth/forgot-password`**: Initiates password reset process
- **`POST /api/bugtracker/v1/auth/reset-password`**: Completes password reset with token

#### Password Reset Flow

![alt text](/docs/images/password-reset-flow.png)

## Configuration Security

### Environment Variables (Production)

```bash
# JWT Configuration
JWT_SECRET_KEY=your-super-secure-secret-key-here
JWT_EXPIRATION_MS=604800000

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://your-database-host:5432/your-database-name
SPRING_DATASOURCE_USERNAME=your-database-username
SPRING_DATASOURCE_PASSWORD=your-secure-database-password

# CORS Configuration (handled in SecurityConfig)
# Currently using wildcard origins for development

# Email Configuration
RESEND_API_KEY=your-resend-api-key
RESEND_FROM_EMAIL=your-verified-email@domain.com
RESEND_FROM_NAME=Your App Name

# Frontend Configuration
APP_FRONTEND_BASE_URL=https://yourdomain.com
```

### Development vs Production

#### Development

- Hardcoded secrets (for convenience)
- Debug logging enabled
- Permissive CORS
- HTTP connections allowed

#### Production

- Environment-based secrets
- Error logging only
- Strict CORS policy
- HTTPS enforcement
- Security headers enabled

This security specification provides a comprehensive foundation for secure authentication and authorization in the Bug Tracker application, with clear guidelines for both current implementation and future security enhancements.
