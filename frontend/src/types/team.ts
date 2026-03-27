/**
 * Team-related TypeScript types
 *
 * This file contains all TypeScript interfaces and types for team management
 * functionality, matching the backend API specification.
 */

// Enums
export enum TeamRole {
	ADMIN = "ADMIN",
	MEMBER = "MEMBER",
}

// Core team interfaces
export interface Team {
	id: string;
	name: string;
	description: string | null;
	teamSlug: string;
	projectId: string;
	projectSlug: string | null; // Can be null for legacy teams
	createdBy: string;
	creatorName: string;
	createdAt: string | null;
	updatedAt: string | null;
	memberCount: number;
	isPublic: boolean;
	currentUserRole: TeamRole | null; // null if not a member
	canManage?: boolean;
}

export interface TeamMember {
	id: string;
	userId: string;
	firstName: string;
	lastName: string;
	email: string;
	role: TeamRole;
	joinedAt: string | null;
	addedBy: string;
	addedByName: string;
}

export interface TeamDetailResponse extends Team {
	members: TeamMember[];
}

// Request DTOs
export interface CreateTeamRequest {
	name: string;
	description?: string;
}

export interface UpdateTeamRequest {
	name?: string;
	description?: string;
}

export interface AddMemberRequest {
	userId: string;
	role?: TeamRole;
}

export interface UpdateMemberRoleRequest {
	role: TeamRole;
}

// API Response types
export interface TeamsListResponse {
	content: Team[];
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

export interface TeamMembersListResponse {
	content: TeamMember[];
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

// Search and filtering types
export interface TeamSearchParams {
	search?: string;
	page?: number;
	size?: number;
	sortBy?: string;
	sortDir?: "asc" | "desc";
}

export interface TeamMemberSearchParams {
	page?: number;
	size?: number;
}

// UI component prop types
export interface TeamCardProps {
	team: Team;
	onJoin?: (teamId: string) => void;
	onLeave?: (teamId: string) => void;
	onEdit?: (teamId: string) => void;
	onDelete?: (teamId: string) => void;
	onViewDetails?: (teamId: string) => void;
	isLoading?: boolean;
	disabled?: boolean;
}

export interface TeamMemberListProps {
	members: TeamMember[];
	currentUserRole?: TeamRole | null;
	onUpdateRole?: (userId: string, role: TeamRole) => void;
	onRemoveMember?: (userId: string) => void;
	isLoading?: boolean;
	disabled?: boolean;
}

export interface RoleBadgeProps {
	role: TeamRole;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
}

export interface TeamActionMenuProps {
	team: Team;
	onEdit?: () => void;
	onDelete?: () => void;
	onViewMembers?: () => void;
	onLeave?: () => void;
	currentUserRole?: TeamRole | null;
	disabled?: boolean;
}

export interface CreateTeamModalProps {
	isOpen: boolean;
	onClose: () => void;
	onSubmit: (data: CreateTeamRequest) => void;
	isLoading?: boolean;
}

export interface AddMemberModalProps {
	isOpen: boolean;
	onClose: () => void;
	onSubmit: (data: AddMemberRequest) => void;
	teamId: string;
	existingMembers: TeamMember[];
	isLoading?: boolean;
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

// User type (for consistency with existing app)
export interface User {
	id: string;
	firstName: string;
	lastName: string;
	email: string;
	role: string;
	skills?: string[];
}

// Loading states
export interface LoadingState {
	isLoading: boolean;
	error: string | null;
}

// Form validation schemas will be defined in the component files using zod

// ============================================================================
// PROJECT-TEAMS INTEGRATION TYPES
// ============================================================================

// Team with project context
export interface TeamWithProject extends Team {
	projectId: string;
	projectSlug: string;
	projectName?: string;
}

// Project-scoped team search params
export interface ProjectTeamSearchParams extends TeamSearchParams {
	projectSlug: string;
}

// Project-scoped team member search params
export interface ProjectTeamMemberSearchParams extends TeamMemberSearchParams {
	projectSlug: string;
	teamSlug: string;
}

// Success response for team operations
export interface TeamSuccessResponse {
	message: string;
	timestamp: string;
	operation: string;
	metadata: Record<string, any>;
}

// Project team card props
export interface ProjectTeamCardProps {
	team: TeamWithProject;
	projectSlug: string;
	onEdit?: (projectSlug: string, teamSlug: string) => void;
	onDelete?: (projectSlug: string, teamSlug: string) => void;
	onViewDetails?: (projectSlug: string, teamSlug: string) => void;
	onManageMembers?: (projectSlug: string, teamSlug: string) => void;
	isLoading?: boolean;
	disabled?: boolean;
}

// Create project team modal props
export interface CreateProjectTeamModalProps {
	isOpen: boolean;
	onClose: () => void;
	onSubmit: (data: CreateTeamRequest) => void;
	projectSlug: string;
	projectName: string;
	isLoading?: boolean;
}

// Project team member list props
export interface ProjectTeamMemberListProps {
	projectSlug: string;
	teamSlug: string;
	members: TeamMember[];
	currentUserRole?: TeamRole | null;
	onUpdateRole?: (userId: string, role: TeamRole) => void;
	onRemoveMember?: (userId: string) => void;
	onAddMember?: (data: AddMemberRequest) => void;
	isLoading?: boolean;
	disabled?: boolean;
}

// Team migration notice props
export interface TeamMigrationNoticeProps {
	team: Team;
	onMigrate?: (teamId: string) => void;
}

// Project teams URL state
export interface ProjectTeamsURLState {
	search?: string;
	page?: number;
	size?: number;
	sortBy?: string;
	sortDir?: "asc" | "desc";
}
