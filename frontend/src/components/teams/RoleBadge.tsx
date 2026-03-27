/**
 * RoleBadge Component
 *
 * A reusable badge component for displaying team roles with different variants.
 * Follows the existing design patterns and provides consistent role visualization.
 *
 * Features:
 * - Different variants (default, outline, secondary)
 * - Multiple sizes (sm, md, lg)
 * - Role-specific styling
 * - Accessibility features
 */

import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { TeamRole } from "@/types/team";
import type { RoleBadgeProps } from "@/types/team";

export function RoleBadge({
	role,
	variant = "default",
	size = "md",
}: RoleBadgeProps) {
	const isAdmin = role === TeamRole.ADMIN;

	// Size classes matching project module
	const sizeClasses = {
		sm: "text-xs px-2 py-0.5",
		md: "text-xs px-2.5 py-1",
		lg: "text-sm px-3 py-1.5",
	};

	// Role-specific styling
	const getRoleVariant = () => {
		if (variant !== "default") return variant;
		return "default"; // Always use default variant for custom colors
	};

	// Role-specific colors matching project module
	const getRoleColor = () => {
		if (variant === "outline") {
			return isAdmin
				? "border-primary text-primary"
				: "border-muted-foreground text-muted-foreground";
		}

		// Match project module colors
		switch (role) {
			case TeamRole.ADMIN:
				return "text-red-700 bg-red-50 border-red-200";
			case TeamRole.MEMBER:
				return "text-blue-700 bg-blue-50 border-blue-200";
			default:
				return "text-gray-700 bg-gray-50 border-gray-200";
		}
	};

	return (
		<Badge
			variant={getRoleVariant()}
			className={cn("font-medium border", sizeClasses[size], getRoleColor())}
		>
			{getRoleDisplayName(role)}
		</Badge>
	);
}

// Utility function to get role display name
export function getRoleDisplayName(role: TeamRole): string {
	switch (role) {
		case TeamRole.ADMIN:
			return "Admin";
		case TeamRole.MEMBER:
			return "Member";
		default:
			return role;
	}
}

// Utility function to get role description
export function getRoleDescription(role: TeamRole): string {
	switch (role) {
		case TeamRole.ADMIN:
			return "Can manage team settings, members, and permissions";
		case TeamRole.MEMBER:
			return "Can view team details and participate in team activities";
		default:
			return "Team member";
	}
}

// Role comparison utilities
export function isAdminRole(role: TeamRole | null): boolean {
	return role === TeamRole.ADMIN;
}

export function isMemberRole(role: TeamRole | null): boolean {
	return role === TeamRole.MEMBER;
}

export function hasAdminPermissions(role: TeamRole | null): boolean {
	return role === TeamRole.ADMIN;
}

export function canManageMembers(role: TeamRole | null): boolean {
	return role === TeamRole.ADMIN;
}

export function canEditTeam(role: TeamRole | null): boolean {
	return role === TeamRole.ADMIN;
}

export function canDeleteTeam(role: TeamRole | null): boolean {
	return role === TeamRole.ADMIN;
}

// Role selection component for forms
export function RoleSelector({
	value,
	onChange,
	disabled = false,
	includeDescriptions = true,
}: {
	value: TeamRole;
	onChange: (role: TeamRole) => void;
	disabled?: boolean;
	includeDescriptions?: boolean;
}) {
	return (
		<div className="space-y-2">
			{Object.values(TeamRole).map((role) => (
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
						name="role"
						value={role}
						checked={value === role}
						onChange={(e) => onChange(e.target.value as TeamRole)}
						disabled={disabled}
						className="mt-1"
					/>
					<div className="flex-1 min-w-0">
						<div className="flex items-center gap-2 mb-1">
							<RoleBadge role={role} size="sm" />
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
