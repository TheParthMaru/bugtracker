/**
 * Teams Components Index
 *
 * Centralized exports for all team-related components.
 * This file provides a clean API for importing team components.
 */

// Team-related components
export { TeamCard, TeamCardSkeleton } from "./TeamCard";
export { CreateTeamModal, useCreateTeamModal } from "./CreateTeamModal";

export {
	TeamSearchFilters,
	FILTER_OPTIONS,
	SORT_OPTIONS,
} from "./TeamSearchFilters";
export { TeamEmptyState } from "./TeamEmptyState";
export { TeamMemberList, TeamMemberListSkeleton } from "./TeamMemberList";

// Badge and Role Components
export {
	RoleBadge,
	RoleSelector,
	getRoleDisplayName,
	getRoleDescription,
	isAdminRole,
	isMemberRole,
	hasAdminPermissions,
	canManageMembers,
	canEditTeam,
	canDeleteTeam,
} from "./RoleBadge";

// Modal Components
export {
	AddMemberModal,
	useAddMemberModal,
	AddMemberButton,
} from "./AddMemberModal";

// Re-export types for convenience
export type {
	TeamCardProps,
	TeamMemberListProps,
	TeamActionMenuProps,
	CreateTeamModalProps,
	AddMemberModalProps,
	RoleBadgeProps,
	Team,
	TeamMember,
	TeamRole,
	CreateTeamRequest,
	AddMemberRequest,
	UpdateMemberRoleRequest,
} from "@/types/team";
