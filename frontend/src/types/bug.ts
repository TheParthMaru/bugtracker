/**
 * Bug-related TypeScript types
 *
 * This file contains all TypeScript interfaces and types for bug management
 * functionality, matching the backend API specification and following the same
 * patterns established in the projects and teams modules.
 */

// Enums
export enum BugType {
	ISSUE = "ISSUE",
	TASK = "TASK",
	SPEC = "SPEC",
}

export enum BugStatus {
	OPEN = "OPEN",
	FIXED = "FIXED",
	CLOSED = "CLOSED",
	REOPENED = "REOPENED",
}

export enum BugPriority {
	CRASH = "CRASH",
	CRITICAL = "CRITICAL",
	HIGH = "HIGH",
	MEDIUM = "MEDIUM",
	LOW = "LOW",
}

export enum CloseReason {
	FIXED = "FIXED",
	INVALID = "INVALID",
	DUPLICATE = "DUPLICATE",
	WONT_FIX = "WONT_FIX",
	WORKS_FOR_ME = "WORKS_FOR_ME",
}

// Core bug interfaces
export interface Bug {
	id: number;
	projectTicketNumber?: number;
	title: string;
	description: string;
	type: BugType;
	status: BugStatus;
	priority: BugPriority;
	projectId: string;
	projectName: string;
	projectSlug: string;
	reporter: User;
	assignee: User | null;
	closeReason?: CloseReason | null;
	labels: BugLabel[];
	tags: string[];
	assignedTeamIds: string[]; // New field for team assignments
	assignedTeams?: TeamAssignmentInfo[]; // Team assignment details from backend
	attachments: BugAttachment[];
	comments: BugComment[];
	createdAt: string;
	updatedAt: string;
	closedAt: string | null;
	commentCount: number;
	attachmentCount: number;
}

export interface BugLabel {
	id: number;
	name: string;
	color: string;
	description: string | null;
	isSystem: boolean;
	createdAt: string;
}

export interface BugAttachment {
	id: number;
	filename: string;
	originalFilename: string;
	filePath: string;
	fileSize: number;
	mimeType: string;
	uploadedBy: User;
	createdAt: string;
}

export interface BugComment {
	id: number;
	content: string;
	author: User;
	parent: BugComment | null;
	replies: BugComment[];
	attachments: BugAttachment[];
	createdAt: string;
	updatedAt: string;
}

export interface User {
	id: string;
	firstName: string;
	lastName: string;
	email: string;
}

// Request DTOs
export interface CreateBugRequest {
	title: string;
	description: string;
	type: BugType;
	priority: BugPriority;
	assigneeId?: string;
	labelIds?: number[];
	tags?: string[];
	assignedTeamIds?: string[]; // New field for team assignments
}

export interface UpdateBugRequest {
	title?: string;
	description?: string;
	type?: BugType;
	priority?: BugPriority;
	assigneeId?: string;
	labelIds?: number[];
	tags?: string[];
	assignedTeamIds?: string[]; // New field for team assignments
}

export interface UpdateBugStatusRequest {
	status: BugStatus;
}

export interface AssignBugRequest {
	assigneeId: string;
}

export interface CreateCommentRequest {
	content: string;
	parentId?: number;
}

export interface UpdateCommentRequest {
	content: string;
}

export interface CreateLabelRequest {
	name: string;
	color: string;
	description?: string;
}

export interface UpdateLabelRequest {
	name?: string;
	color?: string;
	description?: string;
}

// API Response types
export interface BugsListResponse {
	content: Bug[];
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

export interface BugLabelsListResponse {
	content: BugLabel[];
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

export interface BugCommentsListResponse {
	content: BugComment[];
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

export interface BugAttachmentsListResponse {
	content: BugAttachment[];
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

// Search and filter parameters
export interface BugSearchParams {
	search?: string;
	status?: BugStatus | "ALL";
	priority?: BugPriority | "ALL";
	type?: BugType | "ALL";
	assignee?: string;
	reporter?: string;
	labels?: string[];
	page?: number;
	size?: number;
	sort?: string;
}

export interface BugLabelSearchParams {
	search?: string;
	isSystem?: boolean;
	page?: number;
	size?: number;
}

export interface BugCommentSearchParams {
	page?: number;
	size?: number;
	sort?: string;
}

export interface BugAttachmentSearchParams {
	page?: number;
	size?: number;
}

// Analytics types
export interface BugAnalytics {
	totalBugs: number;
	openBugs: number;
	fixedBugs: number;
	closedBugs: number;
	reopenedBugs: number;
	priorityDistribution: Record<BugPriority, number>;
	statusDistribution: Record<BugStatus, number>;
	typeDistribution: Record<BugType, number>;
	topLabels: Array<{ name: string; count: number }>;
	monthlyTrend: Array<{
		month: string;
		created: number;
		resolved: number;
	}>;
}

export interface BugStatistics {
	totalBugs: number;
	openBugs: number;
	fixedBugs: number;
	closedBugs: number;
	reopenedBugs: number;
	highPriorityUnassigned: number;
	attentionRequired: number;
}

// Component props interfaces
export interface BugCardProps {
	bug: Bug;
	onEdit?: (bugId: number) => void;
	onDelete?: (bugId: number) => void;
	onViewDetails?: (bugId: number) => void;
	onAssign?: (bugId: number) => void;
	onStatusChange?: (bugId: number) => void;
	isLoading?: boolean;
	disabled?: boolean;
}

export interface BugFiltersProps {
	searchTerm: string;
	onSearchChange: (value: string) => void;
	status: BugStatus | "ALL";
	onStatusChange: (value: BugStatus | "ALL") => void;
	priority: BugPriority | "ALL";
	onPriorityChange: (value: BugPriority | "ALL") => void;
	type: BugType | "ALL";
	onTypeChange: (value: BugType | "ALL") => void;
	assignee: string | "ALL";
	onAssigneeChange: (value: string | "ALL") => void;
	labels: string[];
	onLabelsChange: (value: string[]) => void;
	onClearAll: () => void;
	hasActiveFilters: boolean;
	isLoading?: boolean;
}

export interface BugFormProps {
	bug?: Bug;
	onSubmit: (data: CreateBugRequest | UpdateBugRequest) => void;
	onCancel: () => void;
	isLoading?: boolean;
	projectId: string;
}

export interface BugStatusBadgeProps {
	status: BugStatus;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
}

export interface BugPriorityBadgeProps {
	priority: BugPriority;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
}

export interface BugTypeBadgeProps {
	type: BugType;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
}

export interface BugLabelBadgeProps {
	label: BugLabel;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
	onRemove?: () => void;
}

export interface CreateBugModalProps {
	isOpen: boolean;
	onClose: () => void;
	onSubmit: (data: CreateBugRequest) => void;
	projectId: string;
	isLoading?: boolean;
}

export interface EditBugModalProps {
	isOpen: boolean;
	onClose: () => void;
	onSubmit: (data: UpdateBugRequest) => void;
	bug: Bug;
	isLoading?: boolean;
}

export interface BugDetailProps {
	bug: Bug;
	onEdit?: () => void;
	onDelete?: () => void;
	onStatusChange?: (status: BugStatus) => void;
	onAssign?: (assigneeId: string) => void;
	onUnassign?: () => void;
	isLoading?: boolean;
}

// Utility functions
export function getBugTypeDisplayName(type: BugType): string {
	switch (type) {
		case BugType.ISSUE:
			return "Issue";
		case BugType.TASK:
			return "Task";
		case BugType.SPEC:
			return "Specification";
		default:
			return type;
	}
}

export function getBugStatusDisplayName(status: BugStatus): string {
	switch (status) {
		case BugStatus.OPEN:
			return "Open";
		case BugStatus.FIXED:
			return "Fixed";
		case BugStatus.CLOSED:
			return "Closed";
		case BugStatus.REOPENED:
			return "Reopened";
		default:
			return status;
	}
}

export function getBugPriorityDisplayName(priority: BugPriority): string {
	switch (priority) {
		case BugPriority.CRASH:
			return "Crash";
		case BugPriority.CRITICAL:
			return "Critical";
		case BugPriority.HIGH:
			return "High";
		case BugPriority.MEDIUM:
			return "Medium";
		case BugPriority.LOW:
			return "Low";
		default:
			return priority;
	}
}

export function getBugPriorityColor(priority: BugPriority): string {
	switch (priority) {
		case BugPriority.CRASH:
			return "bg-red-600";
		case BugPriority.CRITICAL:
			return "bg-orange-600";
		case BugPriority.HIGH:
			return "bg-yellow-600";
		case BugPriority.MEDIUM:
			return "bg-blue-600";
		case BugPriority.LOW:
			return "bg-green-600";
		default:
			return "bg-gray-600";
	}
}

export function getBugStatusColor(status: BugStatus): string {
	switch (status) {
		case BugStatus.OPEN:
			return "bg-blue-600";
		case BugStatus.FIXED:
			return "bg-green-600";
		case BugStatus.CLOSED:
			return "bg-gray-600";
		case BugStatus.REOPENED:
			return "bg-red-600";
		default:
			return "bg-gray-600";
	}
}

export function getBugTypeColor(type: BugType): string {
	switch (type) {
		case BugType.ISSUE:
			return "bg-red-600";
		case BugType.TASK:
			return "bg-blue-600";
		case BugType.SPEC:
			return "bg-purple-600";
		default:
			return "bg-gray-600";
	}
}

// Error types
export interface BugApiError {
	code: string;
	correlationId: string;
	error: string;
	message: string;
	timestamp: string;
	status: number;
}

export interface BugValidationError {
	message: string;
	errors: Record<string, string>;
	status: string;
}

export interface BugLoadingState {
	isLoading: boolean;
	error: string | null;
}

// Team Assignment Types
export interface TeamAssignmentRecommendation {
	assignmentType: AssignmentType;
	message: string;
	assignedTeams: TeamAssignmentInfo[];
	teamMemberSkills: Record<string, TeamMemberSkillMatch[]>;
	analyzedLabels: string[];
	analyzedTags: string[];
	generatedAt: string;
	confidenceScore: number;

	// Utility methods
	hasTeams(): boolean;
	getTeamCount(): number;
	isMultiTeam(): boolean;
	isNoTeamFound(): boolean;
	getPrimaryTeam(): TeamAssignmentInfo | null;
	getDisplayMessage(): string;
	getAssignmentSummary(): string;
}

export enum AssignmentType {
	SINGLE_TEAM = "SINGLE_TEAM",
	MULTI_TEAM = "MULTI_TEAM",
	NO_TEAM_FOUND = "NO_TEAM_FOUND",
	MANUAL_OVERRIDE = "MANUAL_OVERRIDE",
	PARTIAL_MATCH = "PARTIAL_MATCH",
}

export interface TeamAssignmentInfo {
	teamId: string;
	teamName: string;
	teamSlug: string;
	projectSlug: string;
	memberCount: number;
	matchingLabels: string[];
	labelMatchScore: number;
	isPrimary: boolean;
	assignmentReason: string;
}

export interface TeamMemberSkillMatch {
	userId: string;
	firstName: string;
	lastName: string;
	email: string;
	relevantSkills: string[];
	matchingTags: string[];
	skillRelevanceScore: number;
	primarySkill: string;
	isAvailable: boolean;
}
