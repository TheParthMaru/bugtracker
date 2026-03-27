# BugTracker Frontend

A modern, responsive React frontend application for the BugTracker system, built with TypeScript, Vite, and Tailwind CSS. The frontend provides a comprehensive user interface for bug tracking, team management, project organization, and advanced features including duplicate detection, auto-assignment, and gamification.

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Development](#development)
- [Features](#features)
- [API Integration](#api-integration)
- [Styling and UI](#styling-and-ui)
- [State Management](#state-management)
- [Testing](#testing)
- [Build and Deployment](#build-and-deployment)
- [Troubleshooting](#troubleshooting)

## Overview

The BugTracker frontend is a single-page application (SPA) that provides an intuitive and modern interface for managing bugs, teams, and projects. It features:

- **Modern UI/UX**: Clean, responsive design with dark/light theme support
- **Real-time Updates**: WebSocket integration for live notifications and updates
- **Advanced Features**: Duplicate detection, auto-assignment, and gamification
- **Comprehensive Analytics**: Interactive dashboards and reporting
- **Mobile Responsive**: Optimized for desktop, tablet, and mobile devices
- **Accessibility**: WCAG compliant with keyboard navigation support

## Technology Stack

### Core Technologies

- **React 19.1.0**: Modern React with concurrent features
- **TypeScript 5.8.3**: Type-safe JavaScript development
- **Vite 6.3.5**: Fast build tool and development server
- **Tailwind CSS 4.1.8**: Utility-first CSS framework
- **React Router DOM**: Client-side routing

### UI Components and Styling

- **Radix UI**: Accessible, unstyled UI primitives
- **ShadCN/UI**: Pre-built, customizable components
- **Lucide React**: Beautiful, customizable icons
- **Tailwind CSS**: Utility-first styling
- **CSS Modules**: Component-scoped styling

### State Management and Data

- **React Hooks**: Built-in state management
- **React Context**: Global state management
- **Axios**: HTTP client for API communication
- **React Hook Form**: Form handling and validation
- **Zod**: Schema validation

### Development Tools

- **ESLint**: Code linting and formatting
- **Prettier**: Code formatting
- **TypeScript**: Static type checking
- **Vite**: Development server and build tool

## Prerequisites

Before setting up the frontend, ensure you have the following installed:

### Required Software

#### Node.js 18 or higher

**Windows:**

1. Download Node.js from [nodejs.org](https://nodejs.org/)
2. Run the installer and follow the setup wizard
3. Verify installation:
   ```cmd
   node --version
   npm --version
   # Should show Node.js 18+ and npm 9+
   ```

**Linux/macOS:**

```bash
# Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# macOS (using Homebrew)
brew install node

# Verify installation
node --version
npm --version
# Should show Node.js 18+ and npm 9+
```

#### Git

**Windows:**

1. Download Git from [git-scm.com](https://git-scm.com/download/win)
2. Run the installer with default settings
3. Verify installation:
   ```cmd
   git --version
   ```

**Linux/macOS:**

```bash
# Ubuntu/Debian
sudo apt install git

# macOS (using Homebrew)
brew install git

# Verify installation
git --version
```

### Optional Software

- **VS Code** with recommended extensions:
  - ES7+ React/Redux/React-Native snippets
  - Tailwind CSS IntelliSense
  - TypeScript Importer
  - Auto Rename Tag
  - Bracket Pair Colorizer
- **Chrome DevTools** for debugging
- **Postman** for API testing

## Quick Start

### 1. Clone the Repository

```bash
# Clone the repository
git clone https://campus.cs.le.ac.uk/gitlab/pgt_project/24_25_summer/pbm5.git
cd pbm5/bugtracker/frontend
```

### 2. Install Dependencies

```bash
# Install all dependencies
npm install

# Or using yarn
yarn install
```

### 3. Environment Configuration

Create a `.env.local` file in the frontend directory:

```env
# API Configuration
VITE_API_BASE_URL=http://localhost:8080/api/bugtracker/v1
VITE_WS_URL=ws://localhost:8080/ws

# Application Configuration
VITE_APP_NAME=BugTracker
VITE_APP_VERSION=1.0.0

# Development Configuration
VITE_DEBUG=true
VITE_LOG_LEVEL=info
```

### 4. Start Development Server

```bash
# Start the development server
npm run dev

# Or using yarn
yarn dev
```

The application will start on `http://localhost:5173`

### 5. Build for Production

```bash
# Build the application
npm run build

# Preview the production build
npm run preview
```

## Project Structure

```
frontend/
├── public/                     # Static assets
│   ├── bugtracker-icon.svg    # Application icon
│   └── vite.svg               # Vite logo
├── src/                       # Source code
│   ├── components/            # Reusable UI components
│   │   ├── analytics/         # Analytics components
│   │   ├── bugs/             # Bug-related components
│   │   ├── gamification/     # Gamification components
│   │   ├── notifications/    # Notification components
│   │   ├── projects/         # Project components
│   │   ├── teams/            # Team components
│   │   ├── ui/               # Base UI components
│   │   ├── ErrorBoundary.tsx # Error handling
│   │   ├── Navbar.tsx        # Navigation bar
│   │   └── ProtectedRoute.tsx # Route protection
│   ├── pages/                # Page components
│   │   ├── AnalyticsPage.tsx # Analytics dashboard
│   │   ├── BugsPage.tsx      # Bug listing page
│   │   ├── CreateBugPage.tsx # Bug creation page
│   │   ├── GamificationPage.tsx # Gamification page
│   │   ├── LandingPage.tsx   # Dashboard page
│   │   ├── LoginPage.tsx     # Login page
│   │   ├── ProjectsPage.tsx  # Projects page
│   │   ├── TeamsPage.tsx     # Teams page
│   │   └── ...               # Other pages
│   ├── services/             # API services
│   │   ├── api.ts           # Base API client
│   │   ├── bugService.ts    # Bug API service
│   │   ├── projectService.ts # Project API service
│   │   ├── teamService.ts   # Team API service
│   │   └── ...              # Other services
│   ├── hooks/               # Custom React hooks
│   │   ├── useDebounce.ts   # Debouncing hook
│   │   ├── useDuplicateDetection.ts # Duplicate detection
│   │   ├── useWebSocketNotifications.ts # WebSocket hook
│   │   └── ...              # Other hooks
│   ├── types/               # TypeScript type definitions
│   ├── utils/               # Utility functions
│   ├── styles/              # Global styles
│   ├── config/              # Configuration files
│   ├── App.tsx              # Main application component
│   └── main.tsx             # Application entry point
├── package.json              # Dependencies and scripts
├── vite.config.ts           # Vite configuration
├── tailwind.config.js       # Tailwind CSS configuration
├── tsconfig.json            # TypeScript configuration
└── README.md                # This file
```

## Development

### Available Scripts

```bash
# Development
npm run dev          # Start development server
npm run build        # Build for production
npm run preview      # Preview production build
npm run lint         # Run ESLint
npm run lint:fix     # Fix ESLint issues
npm run type-check   # Run TypeScript type checking

# Testing (if configured)
npm run test         # Run tests
npm run test:watch   # Run tests in watch mode
npm run test:coverage # Run tests with coverage
```

### Development Workflow

1. **Feature Development**

   ```bash
   # Create a new feature branch
   git checkout -b feature/new-feature

   # Make your changes
   # Test your changes
   npm run dev

   # Commit your changes
   git add .
   git commit -m "feat: add new feature"

   # Push and create merge request
   git push origin feature/new-feature
   ```

2. **Code Quality**

   ```bash
   # Check code quality
   npm run lint
   npm run type-check

   # Fix issues
   npm run lint:fix
   ```

3. **Testing**

   ```bash
   # Run tests
   npm run test

   # Run tests with coverage
   npm run test:coverage
   ```

### Environment Variables

| Variable            | Description          | Default                                   | Required |
| ------------------- | -------------------- | ----------------------------------------- | -------- |
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8080/api/bugtracker/v1` | Yes      |
| `VITE_WS_URL`       | WebSocket URL        | `ws://localhost:8080/ws`                  | Yes      |
| `VITE_APP_NAME`     | Application name     | `BugTracker`                              | No       |
| `VITE_APP_VERSION`  | Application version  | `1.0.0`                                   | No       |
| `VITE_DEBUG`        | Debug mode           | `false`                                   | No       |
| `VITE_LOG_LEVEL`    | Logging level        | `info`                                    | No       |

## Features

### Core Features

#### Authentication & Authorization

- **User Registration**: Complete registration flow with validation
- **User Login**: Secure login with JWT token management
- **Password Reset**: Email-based password reset functionality
- **Role-based Access**: ADMIN, DEVELOPER, REPORTER role management
- **Protected Routes**: Route protection based on authentication status

#### Bug Management

- **Bug Creation**: Comprehensive bug reporting with attachments
- **Bug Listing**: Advanced filtering, sorting, and search
- **Bug Details**: Detailed bug view with comments and history
- **Bug Updates**: Status updates, priority changes, and assignments
- **File Attachments**: Support for images, documents, and videos
- **Comments System**: Threaded comments with real-time updates

#### Project Management

- **Project Creation**: Create and configure projects
- **Project Dashboard**: Overview of project statistics and activity
- **Project Members**: Member management and role assignment
- **Project Settings**: Configuration and customization options

#### Team Management

- **Team Creation**: Create and manage teams
- **Team Members**: Add/remove team members with role management
- **Team Dashboard**: Team performance and activity overview
- **Team Settings**: Team configuration and preferences

### Advanced Features

#### Duplicate Bug Detection

- **Similarity Analysis**: Real-time similarity checking during bug creation
- **Duplicate Warnings**: Visual warnings for potential duplicates
- **Similarity Configuration**: Configurable similarity thresholds
- **Duplicate Management**: Mark and manage duplicate relationships

#### Auto Assignment

- **Team Assignment**: Automatic team assignment based on bug labels
- **User Assignment**: Intelligent user assignment based on skills
- **Assignment Recommendations**: Visual recommendations with reasoning
- **Workload Balancing**: Consideration of current user workload

#### Gamification

- **Point System**: Points for bug resolution and daily logins
- **Leaderboards**: Project-specific leaderboards with multiple timeframes
- **Streaks**: Login streak tracking and visualization
- **Achievements**: Badges and milestone recognition
- **User Profile**: Comprehensive gamification profile

#### Analytics & Reporting

- **Project Analytics**: Comprehensive project statistics and metrics
- **Team Performance**: Team-based performance analysis
- **Bug Analytics**: Bug resolution rates and trends
- **Duplicate Analytics**: Duplicate detection effectiveness
- **Export Functionality**: Data export for external analysis

#### Notifications

- **Real-time Notifications**: WebSocket-based live updates
- **Multi-channel Delivery**: In-app, email, and toast notifications
- **Notification Preferences**: User-configurable notification settings
- **Notification History**: Complete notification audit trail

## API Integration

### Service Architecture

The frontend uses a service-based architecture for API communication:

```typescript
// Example service structure
export class BugService {
	private api: AxiosInstance;

	constructor() {
		this.api = createApiClient();
	}

	async getBugs(projectSlug: string, filters?: BugFilters): Promise<Bug[]> {
		const response = await this.api.get(`/projects/${projectSlug}/bugs`, {
			params: filters,
		});
		return response.data;
	}

	async createBug(
		projectSlug: string,
		bugData: CreateBugRequest
	): Promise<Bug> {
		const response = await this.api.post(
			`/projects/${projectSlug}/bugs`,
			bugData
		);
		return response.data;
	}
}
```

### API Services

- **`api.ts`**: Base API client with authentication and error handling
- **`bugService.ts`**: Bug-related API operations
- **`projectService.ts`**: Project management API operations
- **`teamService.ts`**: Team management API operations
- **`userService.ts`**: User management API operations
- **`gamificationService.ts`**: Gamification API operations
- **`notificationService.ts`**: Notification API operations
- **`analyticsService.ts`**: Analytics API operations

### Error Handling

```typescript
// Global error handling
export const errorHandler = (error: AxiosError) => {
	if (error.response?.status === 401) {
		// Handle unauthorized access
		localStorage.removeItem("bugtracker_token");
		window.location.href = "/auth/login";
	} else if (error.response?.status === 403) {
		// Handle forbidden access
		showError("You do not have permission to perform this action");
	} else {
		// Handle other errors
		showError(error.response?.data?.message || "An error occurred");
	}
};
```

## Styling and UI

### Design System

The application uses a consistent design system built on:

- **Tailwind CSS**: Utility-first CSS framework
- **ShadCN/UI**: Pre-built, accessible components
- **Radix UI**: Unstyled, accessible primitives
- **Custom Components**: Application-specific components

### Theme Support

```typescript
// Theme configuration
const theme = {
	colors: {
		primary: {
			50: "#eff6ff",
			500: "#3b82f6",
			900: "#1e3a8a",
		},
		// ... other colors
	},
	spacing: {
		// ... spacing scale
	},
	// ... other theme values
};
```

### Responsive Design

The application is fully responsive with breakpoints:

- **Mobile**: < 640px
- **Tablet**: 640px - 1024px
- **Desktop**: > 1024px

### Component Library

#### Base Components (`/src/components/ui/`)

- **Button**: Various button styles and sizes
- **Input**: Form input components
- **Card**: Content containers
- **Modal**: Dialog and modal components
- **Table**: Data table components
- **Badge**: Status and label components

#### Feature Components

- **Bug Components**: Bug-specific UI components
- **Project Components**: Project management components
- **Team Components**: Team management components
- **Analytics Components**: Charts and visualization components

## State Management

### React Hooks

The application uses React hooks for state management:

```typescript
// Custom hook example
export const useBugs = (projectSlug: string) => {
	const [bugs, setBugs] = useState<Bug[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		const fetchBugs = async () => {
			try {
				setLoading(true);
				const data = await bugService.getBugs(projectSlug);
				setBugs(data);
			} catch (err) {
				setError(err.message);
			} finally {
				setLoading(false);
			}
		};

		fetchBugs();
	}, [projectSlug]);

	return { bugs, loading, error, refetch: () => fetchBugs() };
};
```

### Context Providers

```typescript
// Authentication context
export const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
	children,
}) => {
	const [user, setUser] = useState<User | null>(null);
	const [loading, setLoading] = useState(true);

	// Authentication logic

	return (
		<AuthContext.Provider value={{ user, loading, login, logout }}>
			{children}
		</AuthContext.Provider>
	);
};
```

## Testing

### Testing Setup

```bash
# Install testing dependencies
npm install --save-dev @testing-library/react @testing-library/jest-dom vitest jsdom

# Run tests
npm run test
```

### Testing Examples

```typescript
// Component testing
import { render, screen, fireEvent } from "@testing-library/react";
import { BugCard } from "../BugCard";

describe("BugCard", () => {
	it("renders bug information correctly", () => {
		const mockBug = {
			id: 1,
			title: "Test Bug",
			description: "Test Description",
			priority: "HIGH",
			status: "OPEN",
		};

		render(<BugCard bug={mockBug} />);

		expect(screen.getByText("Test Bug")).toBeInTheDocument();
		expect(screen.getByText("HIGH")).toBeInTheDocument();
	});

	it("handles click events", () => {
		const mockBug = {
			/* ... */
		};
		const mockOnClick = jest.fn();

		render(<BugCard bug={mockBug} onClick={mockOnClick} />);

		fireEvent.click(screen.getByRole("button"));
		expect(mockOnClick).toHaveBeenCalledWith(mockBug);
	});
});
```

## Build and Deployment

### Production Build

```bash
# Build the application
npm run build

# The build output will be in the `dist/` directory
```

### Deployment Options

#### Static Hosting

```bash
# Build the application
npm run build

# Deploy to static hosting (Netlify, Vercel, etc.)
# Upload the `dist/` directory contents
```

#### Docker Deployment

```dockerfile
# Dockerfile
FROM node:18-alpine as builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### Nginx Configuration

```nginx
# nginx.conf
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Troubleshooting

### Common Issues

#### Build Issues

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear Vite cache
rm -rf node_modules/.vite
npm run dev
```

#### TypeScript Issues

```bash
# Check TypeScript configuration
npx tsc --noEmit

# Update TypeScript
npm install --save-dev typescript@latest
```

#### Dependency Issues

```bash
# Check for outdated packages
npm outdated

# Update packages
npm update

# Check for security vulnerabilities
npm audit
npm audit fix
```

#### API Connection Issues

1. **Check Backend Status**

   ```bash
   # Verify backend is running
   curl http://localhost:8080/api/bugtracker/v1/health
   ```

2. **Check Environment Variables**

   ```bash
   # Verify API URL configuration
   echo $VITE_API_BASE_URL
   ```

3. **Check CORS Configuration**
   - Ensure backend CORS is configured for frontend URL
   - Check browser console for CORS errors
