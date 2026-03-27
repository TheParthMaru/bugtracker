/**
 * Project-related TypeScript types
 *
 * This file contains all TypeScript interfaces and types for project management
 * functionality, matching the backend API specification and following the same
 * patterns established in the teams module.
 */

// Enums
export enum ProjectRole {
	ADMIN = "ADMIN",
	MEMBER = "MEMBER",
	PENDING = "PENDING", // Pending approval - no access until approved
}

export enum MemberStatus {
	PENDING = "PENDING", // Waiting for approval
	ACTIVE = "ACTIVE", // Approved and active
	REJECTED = "REJECTED", // Request was rejected
}

// Core project interfaces
export interface Project {
	id: string;
	name: string;
	description: string | null;
	projectSlug: string;
	adminId: string;
	adminFirstName: string | null; // From backend API
	adminLastName: string | null; // From backend API
	createdAt: string | null;
	updatedAt: string | null;
	isActive: boolean;
	memberCount: number;
	pendingRequestCount?: number; // From API response
	// Use actual API response field names:
	userRole: ProjectRole | null; // null if not a member
	userMembershipStatus: MemberStatus | null; // null if not a member
	isUserAdmin?: boolean; // From API response
}

export interface ProjectMember {
	id: string;
	projectId: string;
	userId: string;
	userName: string;
	firstName: string;
	lastName: string;
	userEmail: string;
	email?: string; // Backward compatibility alias for userEmail
	role: ProjectRole;
	status: MemberStatus;
	joinedAt: string | null;
	requestedAt: string | null;
	approvedBy: string | null;
	approvedByName: string | null;
	approvedAt: string | null;
}

export interface ProjectDetailResponse extends Project {
	members: ProjectMember[];
	teams?: any[]; // Will be defined later when teams integration is complete
}

// Request DTOs
export interface CreateProjectRequest {
	name: string;
	description?: string;
}

export interface UpdateProjectRequest {
	name?: string;
	description?: string;
}

export interface JoinProjectRequest {
	// Empty for now - just the API call
}

export interface UpdateMemberRoleRequest {
	role: ProjectRole;
}

// API Response types
export interface ProjectsListResponse {
	content: Project[];
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

export interface ProjectMembersListResponse {
	content: ProjectMember[];
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
export interface ProjectSearchParams {
	search?: string;
	page?: number;
	size?: number;
	sort?: string; // Spring Data format: "field,direction" (e.g., "name,asc", "createdAt,desc")
}

export interface ProjectMemberSearchParams {
	status?: MemberStatus;
	role?: ProjectRole;
	page?: number;
	size?: number;
}

// Component prop interfaces

export interface ProjectMemberListProps {
	members: ProjectMember[];
	userRole?: ProjectRole | null; // Updated field name
	onUpdateRole?: (userId: string, role: ProjectRole) => void;
	onRemoveMember?: (userId: string) => void;
	onApproveMember?: (userId: string) => void;
	onRejectMember?: (userId: string) => void;
	isLoading?: boolean;
	disabled?: boolean;
}

export interface ProjectRoleBadgeProps {
	role: ProjectRole;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
}

export interface MemberStatusBadgeProps {
	status: MemberStatus;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
}

export interface ProjectActionMenuProps {
	project: Project;
	onEdit?: () => void;
	onDelete?: () => void;
	onViewMembers?: () => void;
	onLeave?: () => void;
	userRole?: ProjectRole | null; // Updated field name
	disabled?: boolean;
}

// Modal component props
export interface CreateProjectModalProps {
	isOpen: boolean;
	onClose: () => void;
	onSubmit: (data: CreateProjectRequest) => void;
	isLoading?: boolean;
}

export interface JoinProjectButtonProps {
	project: Project;
	onJoin?: (projectId: string) => void;
	isLoading?: boolean;
	disabled?: boolean;
}

export interface ProjectMemberRequestsProps {
	projectId: string;
	userRole?: ProjectRole | null; // Updated field name
	onMemberUpdate?: () => void;
}

// Error types (consistent with teams module)
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

// Loading states
export interface LoadingState {
	isLoading: boolean;
	error: string | null;
}

// Project statistics (for dashboard/profile views)
export interface ProjectStats {
	totalProjects: number;
	adminProjects: number;
	memberProjects: number;
	pendingRequests: number;
}

// User project relationship (for user profile views)
export interface UserProjectMembership {
	project: Project;
	role: ProjectRole;
	status: MemberStatus;
	joinedAt: string | null;
}

// Extended project response for detailed views
export interface ProjectDetailPageData {
	project: Project;
	members: ProjectMember[];
	pendingRequests: ProjectMember[];
	userRole: ProjectRole | null;
	userStatus: MemberStatus | null;
	canEdit: boolean;
	canDelete: boolean;
	canManageMembers: boolean;
}

// Bulk operations (for future use)
export interface BulkMemberAction {
	userIds: string[];
	action: "approve" | "reject" | "remove" | "updateRole";
	role?: ProjectRole;
}

// Project activity (for future activity feeds)
export interface ProjectActivity {
	id: string;
	projectId: string;
	userId: string;
	userFirstName: string;
	userLastName: string;
	action: string;
	details: Record<string, any>;
	createdAt: string;
}

// ============================================================================
// PROJECT-TEAMS INTEGRATION TYPES
// ============================================================================

// Import team types for integration
import type {
	Team,
	TeamMember,
	TeamRole,
	CreateTeamRequest,
	AddMemberRequest,
} from "@/types/team";

// Project with teams context
export interface ProjectWithTeams extends Project {
	teams?: Team[];
	teamCount?: number;
	canCreateTeams?: boolean;
}

// Project detail with teams
export interface ProjectDetailWithTeams extends ProjectDetailResponse {
	teams: Team[];
	teamCount: number;
	canCreateTeams: boolean;
}

// Project teams list response
export interface ProjectTeamsListResponse {
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

// Project team search parameters
export interface ProjectTeamSearchParams {
	search?: string;
	page?: number;
	size?: number;
	sortBy?: string;
	sortDir?: "asc" | "desc";
}

// Project team member search parameters
export interface ProjectTeamMemberSearchParams {
	page?: number;
	size?: number;
}

// Project team card props
export interface ProjectTeamCardProps {
	team: Team;
	projectSlug: string;
	onEdit?: (projectSlug: string, teamSlug: string) => void;
	onDelete?: (projectSlug: string, teamSlug: string) => void;
	onViewDetails?: (projectSlug: string, teamSlug: string) => void;
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

// Project teams URL state
export interface ProjectTeamsURLState {
	search?: string;
	page?: number;
	size?: number;
	sortBy?: string;
	sortDir?: "asc" | "desc";
}

// Team success response for project operations
export interface TeamSuccessResponse {
	message: string;
	timestamp: string;
	operation: string;
	metadata: Record<string, any>;
}
