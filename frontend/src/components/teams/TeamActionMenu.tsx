/**
 * TeamActionMenu Component
 *
 * A dropdown menu component for team-level actions with role-based permissions.
 * Provides actions like edit, delete, view members, leave team, etc.
 *
 * Features:
 * - Role-based action visibility (admin vs member)
 * - Confirmation dialogs for destructive actions
 * - Proper accessibility with keyboard navigation
 * - Loading states during actions
 * - Responsive design
 * - Team privacy handling
 */

import React, { useState } from "react";
import {
	MoreVertical,
	Edit,
	Trash2,
	Users,
	LogOut,
	Eye,
	Link,
	Settings,
	Shield,
	Globe,
	Lock,
	UserPlus,
	Copy,
	ExternalLink,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger,
	DropdownMenuSeparator,
	DropdownMenuLabel,
} from "@/components/ui/dropdown-menu";
import {
	AlertDialog,
	AlertDialogAction,
	AlertDialogCancel,
	AlertDialogContent,
	AlertDialogDescription,
	AlertDialogFooter,
	AlertDialogHeader,
	AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
	RoleBadge,
	canEditTeam,
	canDeleteTeam,
	hasAdminPermissions,
} from "./RoleBadge";
import { cn } from "@/lib/utils";
import type { TeamActionMenuProps } from "@/types/team";

type ActionType = "delete" | "leave" | null;

export function TeamActionMenu({
	team,
	onEdit,
	onDelete,
	onViewMembers,
	onLeave,
	currentUserRole,
	disabled = false,
}: TeamActionMenuProps) {
	const [confirmAction, setConfirmAction] = useState<ActionType>(null);
	const [isOpen, setIsOpen] = useState(false);

	const canEdit = canEditTeam(currentUserRole ?? null);
	const canDelete = canDeleteTeam(currentUserRole ?? null);
	const isAdmin = hasAdminPermissions(currentUserRole ?? null);
	const isMember = !!currentUserRole; // User has a role = is a member

	// Handle menu item actions
	const handleEdit = () => {
		setIsOpen(false);
		onEdit?.();
	};

	const handleViewMembers = () => {
		setIsOpen(false);
		onViewMembers?.();
	};

	const handleAddMember = () => {
		setIsOpen(false);
		// This would be handled by parent component
	};

	const handleCopyLink = async () => {
		try {
			const url = `${window.location.origin}/teams/${team.teamSlug}`;
			await navigator.clipboard.writeText(url);
			// You might want to show a toast notification here
		} catch (error) {
			console.error("Failed to copy link:", error);
		}
		setIsOpen(false);
	};

	const handleDelete = () => {
		setConfirmAction("delete");
		setIsOpen(false);
	};

	const handleLeave = () => {
		setConfirmAction("leave");
		setIsOpen(false);
	};

	const confirmDelete = () => {
		onDelete?.();
		setConfirmAction(null);
	};

	const confirmLeave = () => {
		onLeave?.();
		setConfirmAction(null);
	};

	// Don't render if no actions available
	if (!isMember && !team.isPublic) {
		return null;
	}

	return (
		<>
			<DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
				<DropdownMenuTrigger asChild>
					<Button
						variant="ghost"
						size="sm"
						disabled={disabled}
						className="h-8 w-8 p-0"
						aria-label="Team actions"
					>
						<MoreVertical className="h-4 w-4" />
					</Button>
				</DropdownMenuTrigger>

				<DropdownMenuContent align="end" className="w-48">
					<DropdownMenuLabel className="flex items-center gap-2">
						<span className="truncate">{team.name}</span>
						{currentUserRole && <RoleBadge role={currentUserRole} size="sm" />}
					</DropdownMenuLabel>

					<DropdownMenuSeparator />

					{/* View Team Details */}
					<DropdownMenuItem onClick={handleViewMembers}>
						<Eye className="h-4 w-4 mr-2" />
						View Details
					</DropdownMenuItem>

					{/* View Members */}
					{(isMember || team.isPublic) && (
						<DropdownMenuItem onClick={handleViewMembers}>
							<Users className="h-4 w-4 mr-2" />
							View Members
							<span className="ml-auto text-xs text-muted-foreground">
								{team.memberCount}
							</span>
						</DropdownMenuItem>
					)}

					{/* Copy Team Link */}
					{team.isPublic && (
						<DropdownMenuItem onClick={handleCopyLink}>
							<Link className="h-4 w-4 mr-2" />
							Copy Link
						</DropdownMenuItem>
					)}

					{/* Admin Actions */}
					{isAdmin && (
						<>
							<DropdownMenuSeparator />

							<DropdownMenuItem onClick={handleEdit}>
								<Edit className="h-4 w-4 mr-2" />
								Edit Team
							</DropdownMenuItem>

							<DropdownMenuItem onClick={handleAddMember}>
								<UserPlus className="h-4 w-4 mr-2" />
								Add Member
							</DropdownMenuItem>

							<DropdownMenuItem onClick={handleViewMembers}>
								<Settings className="h-4 w-4 mr-2" />
								Manage Members
							</DropdownMenuItem>
						</>
					)}

					{/* Member Actions */}
					{isMember && (
						<>
							<DropdownMenuSeparator />

							<DropdownMenuItem
								onClick={handleLeave}
								className="text-orange-600 focus:text-orange-600"
							>
								<LogOut className="h-4 w-4 mr-2" />
								Leave Team
							</DropdownMenuItem>
						</>
					)}

					{/* Delete Action (Admin Only) */}
					{canDelete && (
						<>
							<DropdownMenuSeparator />

							<DropdownMenuItem
								onClick={handleDelete}
								className="text-destructive focus:text-destructive"
							>
								<Trash2 className="h-4 w-4 mr-2" />
								Delete Team
							</DropdownMenuItem>
						</>
					)}

					{/* Team Privacy Indicator */}
					<DropdownMenuSeparator />
					<DropdownMenuLabel className="flex items-center gap-2 text-xs text-muted-foreground">
						{team.isPublic ? (
							<>
								<Globe className="h-3 w-3" />
								Public Team
							</>
						) : (
							<>
								<Lock className="h-3 w-3" />
								Private Team
							</>
						)}
					</DropdownMenuLabel>
				</DropdownMenuContent>
			</DropdownMenu>

			{/* Delete Confirmation Dialog */}
			<AlertDialog
				open={confirmAction === "delete"}
				onOpenChange={() => setConfirmAction(null)}
			>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Delete Team</AlertDialogTitle>
						<AlertDialogDescription>
							Are you sure you want to delete "{team.name}"? This action cannot
							be undone. All team data, including members and associated
							content, will be permanently deleted.
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={confirmDelete}
							className="bg-destructive hover:bg-destructive/90"
						>
							Delete Team
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>

			{/* Leave Confirmation Dialog */}
			<AlertDialog
				open={confirmAction === "leave"}
				onOpenChange={() => setConfirmAction(null)}
			>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Leave Team</AlertDialogTitle>
						<AlertDialogDescription>
							Are you sure you want to leave "{team.name}"?
							{isAdmin && team.memberCount === 1 && (
								<span className="block mt-2 text-orange-600 font-medium">
									Warning: You are the only admin. Leaving will make this team
									inaccessible.
								</span>
							)}
							{isAdmin && team.memberCount > 1 && (
								<span className="block mt-2 text-orange-600">
									You are an admin. Make sure another admin is available to
									manage the team.
								</span>
							)}
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={confirmLeave}
							className="bg-orange-600 hover:bg-orange-700"
						>
							Leave Team
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>
		</>
	);
}

// Simplified version for use in cards where space is limited
export function TeamActionMenuCompact({
	team,
	onEdit,
	onDelete,
	onViewMembers,
	onLeave,
	currentUserRole,
	disabled = false,
}: TeamActionMenuProps) {
	const [confirmAction, setConfirmAction] = useState<ActionType>(null);
	const [isOpen, setIsOpen] = useState(false);

	const canEdit = canEditTeam(currentUserRole ?? null);
	const canDelete = canDeleteTeam(currentUserRole ?? null);
	const isAdmin = hasAdminPermissions(currentUserRole ?? null);
	const isMember = !!currentUserRole;

	if (!isMember && !team.isPublic) {
		return null;
	}

	return (
		<>
			<DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
				<DropdownMenuTrigger asChild>
					<Button
						variant="ghost"
						size="sm"
						disabled={disabled}
						className="h-8 w-8 p-0"
						aria-label="Team actions"
					>
						<MoreVertical className="h-4 w-4" />
					</Button>
				</DropdownMenuTrigger>

				<DropdownMenuContent align="end" className="w-40">
					<DropdownMenuItem
						onClick={() => {
							setIsOpen(false);
							onViewMembers?.();
						}}
					>
						<Eye className="h-4 w-4 mr-2" />
						View
					</DropdownMenuItem>

					{canEdit && (
						<DropdownMenuItem
							onClick={() => {
								setIsOpen(false);
								onEdit?.();
							}}
						>
							<Edit className="h-4 w-4 mr-2" />
							Edit
						</DropdownMenuItem>
					)}

					{isMember && (
						<DropdownMenuItem
							onClick={() => {
								setConfirmAction("leave");
								setIsOpen(false);
							}}
							className="text-orange-600"
						>
							<LogOut className="h-4 w-4 mr-2" />
							Leave
						</DropdownMenuItem>
					)}

					{canDelete && (
						<DropdownMenuItem
							onClick={() => {
								setConfirmAction("delete");
								setIsOpen(false);
							}}
							className="text-destructive"
						>
							<Trash2 className="h-4 w-4 mr-2" />
							Delete
						</DropdownMenuItem>
					)}
				</DropdownMenuContent>
			</DropdownMenu>

			{/* Confirmation Dialogs */}
			<AlertDialog
				open={confirmAction === "delete"}
				onOpenChange={() => setConfirmAction(null)}
			>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Delete Team</AlertDialogTitle>
						<AlertDialogDescription>
							Are you sure you want to delete "{team.name}"?
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={() => {
								onDelete?.();
								setConfirmAction(null);
							}}
							className="bg-destructive hover:bg-destructive/90"
						>
							Delete
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>

			<AlertDialog
				open={confirmAction === "leave"}
				onOpenChange={() => setConfirmAction(null)}
			>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Leave Team</AlertDialogTitle>
						<AlertDialogDescription>
							Are you sure you want to leave "{team.name}"?
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={() => {
								onLeave?.();
								setConfirmAction(null);
							}}
							className="bg-orange-600 hover:bg-orange-700"
						>
							Leave
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>
		</>
	);
}
