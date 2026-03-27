/**
 * Projects Components Index
 *
 * Centralized exports for all project-related components and utilities.
 * Provides clean imports for the rest of the application.
 */

// Core Components
export { ProjectsTable } from "./ProjectsTable";

// Project Teams Components
export { ProjectTeamCard, ProjectTeamCardSkeleton } from "./ProjectTeamCard";
export {
	CreateProjectTeamModal,
	useCreateProjectTeamModal,
	CreateProjectTeamButton,
} from "./CreateProjectTeamModal";

// Badge Components
export {
	ProjectRoleBadge,
	MemberStatusBadge,
	ProjectRoleSelector,

	// Utility functions
	getRoleDisplayName,
	getRoleDescription,
	getStatusDisplayName,
	getStatusDescription,
	isAdminRole,
	isMemberRole,
	hasAdminPermissions,
	canManageMembers,
	canEditProject,
	canDeleteProject,
	canApproveMembers,
	isActiveMember,
	isPendingMember,
	isRejectedMember,
} from "./ProjectRoleBadge";

// Modal Components
export {
	CreateProjectModal,
	useCreateProjectModal,
	CreateProjectButton,
} from "./CreateProjectModal";

// Re-export types for convenience
export type {
	ProjectRoleBadgeProps,
	MemberStatusBadgeProps,
	CreateProjectModalProps,
	CreateProjectTeamModalProps,
	JoinProjectButtonProps,
	ProjectActionMenuProps,
	ProjectMemberListProps,
	ProjectMemberRequestsProps,
	Project,
	ProjectMember,
	ProjectRole,
	MemberStatus,
	CreateProjectRequest,
	UpdateProjectRequest,
	JoinProjectRequest,
	UpdateMemberRoleRequest,
	ProjectSearchParams,
	ProjectDetailPageData,
	UserProjectMembership,
	ProjectStats,
} from "@/types/project";
