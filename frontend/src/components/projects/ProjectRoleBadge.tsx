/**
 * ProjectRoleBadge Component
 *
 * Displays project role badges with consistent styling and colors.
 * Includes utility functions for role permissions and a role selector component.
 *
 * Features:
 * - Consistent role badge styling
 * - Permission checking utilities
 * - Role selector for forms
 * - Member status badges
 * - Responsive design
 */

import React from "react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
	ProjectRole,
	MemberStatus,
	type ProjectRoleBadgeProps,
	type MemberStatusBadgeProps,
} from "@/types/project";

/**
 * Project Role Badge Component
 */
export function ProjectRoleBadge({
	role,
	variant = "default",
	size = "md",
}: ProjectRoleBadgeProps) {
	const getRoleVariant = () => {
		if (variant !== "default") return variant;
		return role === ProjectRole.ADMIN ? "default" : "secondary";
	};

	const getRoleColor = () => {
		switch (role) {
			case ProjectRole.ADMIN:
				return "text-red-700 bg-red-50 border-red-200";
			case ProjectRole.MEMBER:
				return "text-blue-700 bg-blue-50 border-blue-200";
			case ProjectRole.PENDING:
				return "text-orange-700 bg-orange-50 border-orange-200";
			default:
				return "text-gray-700 bg-gray-50 border-gray-200";
		}
	};

	const sizeClasses = {
		sm: "text-xs px-2 py-0.5",
		md: "text-xs px-2.5 py-1",
		lg: "text-sm px-3 py-1.5",
	};

	return (
		<Badge
			variant={getRoleVariant()}
			className={cn("font-medium border", getRoleColor(), sizeClasses[size])}
		>
			{getRoleDisplayName(role)}
		</Badge>
	);
}

/**
 * Member Status Badge Component
 */
export function MemberStatusBadge({
	status,
	variant = "default",
	size = "md",
}: MemberStatusBadgeProps) {
	const getStatusVariant = () => {
		if (variant !== "default") return variant;

		switch (status) {
			case MemberStatus.ACTIVE:
				return "default";
			case MemberStatus.PENDING:
				return "secondary";
			case MemberStatus.REJECTED:
				return "destructive";
			default:
				return "secondary";
		}
	};

	const getStatusColor = () => {
		switch (status) {
			case MemberStatus.ACTIVE:
				return "text-green-700 bg-green-50 border-green-200";
			case MemberStatus.PENDING:
				return "text-orange-700 bg-orange-50 border-orange-200";
			case MemberStatus.REJECTED:
				return "text-red-700 bg-red-50 border-red-200";
			default:
				return "text-gray-700 bg-gray-50 border-gray-200";
		}
	};

	const sizeClasses = {
		sm: "text-xs px-2 py-0.5",
		md: "text-xs px-2.5 py-1",
		lg: "text-sm px-3 py-1.5",
	};

	return (
		<Badge
			variant={getStatusVariant()}
			className={cn("font-medium border", getStatusColor(), sizeClasses[size])}
		>
			{getStatusDisplayName(status)}
		</Badge>
	);
}

/**
 * Utility Functions
 */

export function getRoleDisplayName(role: ProjectRole): string {
	switch (role) {
		case ProjectRole.ADMIN:
			return "Admin";
		case ProjectRole.MEMBER:
			return "Member";
		case ProjectRole.PENDING:
			return "Pending";
		default:
			return "Unknown";
	}
}

export function getRoleDescription(role: ProjectRole): string {
	switch (role) {
		case ProjectRole.ADMIN:
			return "Full project access, can manage members and settings";
		case ProjectRole.MEMBER:
			return "Can access project resources and participate in activities";
		case ProjectRole.PENDING:
			return "Membership request awaiting admin approval";
		default:
			return "Unknown role";
	}
}

export function getStatusDisplayName(status: MemberStatus): string {
	switch (status) {
		case MemberStatus.ACTIVE:
			return "Active";
		case MemberStatus.PENDING:
			return "Pending";
		case MemberStatus.REJECTED:
			return "Rejected";
		default:
			return "Unknown";
	}
}

export function getStatusDescription(status: MemberStatus): string {
	switch (status) {
		case MemberStatus.ACTIVE:
			return "Active project member with full access";
		case MemberStatus.PENDING:
			return "Membership request awaiting approval";
		case MemberStatus.REJECTED:
			return "Membership request was rejected";
		default:
			return "Unknown status";
	}
}

/**
 * Permission Checking Functions
 */

export function isAdminRole(role: ProjectRole | null): boolean {
	return role === ProjectRole.ADMIN;
}

export function isMemberRole(role: ProjectRole | null): boolean {
	return role === ProjectRole.MEMBER;
}

export function isPendingRole(role: ProjectRole | null): boolean {
	return role === ProjectRole.PENDING;
}

export function hasAdminPermissions(role: ProjectRole | null): boolean {
	return role === ProjectRole.ADMIN;
}

export function canManageMembers(role: ProjectRole | null): boolean {
	return role === ProjectRole.ADMIN;
}

export function canEditProject(role: ProjectRole | null): boolean {
	return role === ProjectRole.ADMIN;
}

export function canDeleteProject(role: ProjectRole | null): boolean {
	return role === ProjectRole.ADMIN;
}

export function canApproveMembers(role: ProjectRole | null): boolean {
	return role === ProjectRole.ADMIN;
}

export function canAccessProject(role: ProjectRole | null): boolean {
	return role === ProjectRole.ADMIN || role === ProjectRole.MEMBER;
}

export function isActiveMember(status: MemberStatus | null): boolean {
	return status === MemberStatus.ACTIVE;
}

export function isPendingMember(status: MemberStatus | null): boolean {
	return status === MemberStatus.PENDING;
}

export function isRejectedMember(status: MemberStatus | null): boolean {
	return status === MemberStatus.REJECTED;
}

/**
 * Role Selector Component for Forms
 */
export function ProjectRoleSelector({
	value,
	onChange,
	disabled = false,
	includeDescriptions = true,
}: {
	value: ProjectRole;
	onChange: (role: ProjectRole) => void;
	disabled?: boolean;
	includeDescriptions?: boolean;
}) {
	// Filter out PENDING role as it's not manually assignable
	const selectableRoles = Object.values(ProjectRole).filter(
		(role) => role !== ProjectRole.PENDING
	);

	return (
		<div className="space-y-2">
			{selectableRoles.map((role) => (
				<label
					key={role}
					className={cn(
						"flex items-start gap-3 p-3 rounded-lg border cursor-pointer transition-colors",
						value === role
							? "border-primary bg-primary/5"
							: "border-muted hover:border-muted-foreground/50",
						disabled && "opacity-50 cursor-not-allowed"
					)}
				>
					<input
						type="radio"
						name="projectRole"
						value={role}
						checked={value === role}
						onChange={(e) => onChange(e.target.value as ProjectRole)}
						disabled={disabled}
						className="mt-1"
					/>
					<div className="flex-1 min-w-0">
						<div className="flex items-center gap-2 mb-1">
							<ProjectRoleBadge role={role} size="sm" />
							<span className="font-medium">{getRoleDisplayName(role)}</span>
						</div>
						{includeDescriptions && (
							<p className="text-sm text-muted-foreground">
								{getRoleDescription(role)}
							</p>
						)}
					</div>
				</label>
			))}
		</div>
	);
}
