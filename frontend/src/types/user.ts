/**
 * User-related TypeScript types
 *
 * This file contains all TypeScript interfaces and types for user management
 * functionality, matching the backend API specification.
 */

// User role enum
export enum UserRole {
	ADMIN = "ADMIN",
	DEVELOPER = "DEVELOPER",
	REPORTER = "REPORTER",
}

// Core user interface
export interface User {
	id: string;
	firstName: string;
	lastName: string;
	email: string;
	role: UserRole;
	skills?: string[];
	createdAt?: string;
	updatedAt?: string;
}

// User profile interface (for current user)
export interface UserProfile extends User {
	// Additional profile-specific fields can be added here
}

// User search result interface
export interface UserSearchResult {
	id: string;
	firstName: string;
	lastName: string;
	email: string;
	role: UserRole;
	skills?: string[];
	isTeamMember?: boolean; // For team member search context
}

// Request DTOs
export interface UpdateUserRequest {
	firstName?: string;
	lastName?: string;
	email?: string;
	role?: UserRole;
	skills?: string[];
}

export interface UserSearchRequest {
	search?: string;
	role?: UserRole;
	page?: number;
	size?: number;
}

// API Response types
export interface UsersListResponse {
	content: User[];
	pageable: {
		pageNumber: number;
		pageSize: number;
		sort: {
			sorted: boolean;
			unsorted: boolean;
			empty: boolean;
		};
		offset: number;
		paged: boolean;
		unpaged: boolean;
	};
	totalElements: number;
	totalPages: number;
	last: boolean;
	size: number;
	number: number;
	sort: {
		sorted: boolean;
		unsorted: boolean;
		empty: boolean;
	};
	first: boolean;
	numberOfElements: number;
	empty: boolean;
}

export interface UserSearchResponse {
	content: User[];
	pageable: {
		pageNumber: number;
		pageSize: number;
		sort: {
			sorted: boolean;
			unsorted: boolean;
			empty: boolean;
		};
		offset: number;
		paged: boolean;
		unpaged: boolean;
	};
	totalElements: number;
	totalPages: number;
	last: boolean;
	size: number;
	number: number;
	sort: {
		sorted: boolean;
		unsorted: boolean;
		empty: boolean;
	};
	first: boolean;
	numberOfElements: number;
	empty: boolean;
}

// UI component prop types
export interface UserCardProps {
	user: User;
	onSelect?: (user: User) => void;
	onRemove?: (userId: string) => void;
	selected?: boolean;
	disabled?: boolean;
	showActions?: boolean;
}

export interface UserSearchProps {
	onUserSelect: (user: User) => void;
	excludeUserIds?: string[];
	placeholder?: string;
	disabled?: boolean;
	multiple?: boolean;
}

export interface UserAvatarProps {
	user: User;
	size?: "sm" | "md" | "lg";
	showName?: boolean;
	showEmail?: boolean;
}

// Error types
export interface ApiError {
	code: string;
	correlationId: string;
	error: string;
	message: string;
	timestamp: string;
	status: number;
}

export interface ValidationError {
	message: string;
	errors: Record<string, string>;
	status: string;
}

// Loading state
export interface LoadingState {
	isLoading: boolean;
	error: string | null;
}

export interface LoginResponse {
	token: string;
	dailyLoginAwarded: boolean;
}
