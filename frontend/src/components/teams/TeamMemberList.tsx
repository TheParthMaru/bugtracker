/**
 * TeamMemberList Component
 *
 * Displays a list of team members with management capabilities.
 * Supports role changes, member removal, and proper loading states.
 *
 * Features:
 * - Member list with avatar, name, email, role, and joined date
 * - Role change functionality (admin only)
 * - Member removal (admin only)
 * - Loading states and error handling
 * - Responsive design and accessibility
 * - Pagination support
 * - Empty state handling
 */

import React, { useState, useEffect } from "react";
import {
	User,
	MoreHorizontal,
	UserMinus,
	Shield,
	Calendar,
	Mail,
	Loader2,
	AlertCircle,
	Search,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger,
	DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
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
import { RoleBadge, canManageMembers } from "./RoleBadge";
import { teamService } from "@/services/teamService";
import { projectService } from "@/services/projectService";
import { cn } from "@/lib/utils";
import type {
	TeamMember,
	TeamRole,
	TeamMembersListResponse,
} from "@/types/team";

interface TeamMemberListProps {
	teamSlug: string;
	projectSlug?: string;
	currentUserRole?: TeamRole | null;
	onMemberUpdate?: () => void;
	className?: string;
}

interface TeamMemberListState {
	members: TeamMember[];
	loading: boolean;
	error: string | null;
	searchTerm: string;
	pagination: {
		page: number;
		size: number;
		totalElements: number;
		totalPages: number;
		hasNext: boolean;
		hasPrevious: boolean;
	};
}

export function TeamMemberList({
	teamSlug,
	projectSlug,
	currentUserRole,
	onMemberUpdate,
	className,
}: TeamMemberListProps) {
	const [state, setState] = useState<TeamMemberListState>({
		members: [],
		loading: true,
		error: null,
		searchTerm: "",
		pagination: {
			page: 0,
			size: 10,
			totalElements: 0,
			totalPages: 0,
			hasNext: false,
			hasPrevious: false,
		},
	});

	const [selectedMember, setSelectedMember] = useState<TeamMember | null>(null);
	const [actionType, setActionType] = useState<"remove" | "roleChange" | null>(
		null
	);
	const [newRole, setNewRole] = useState<TeamRole | null>(null);
	const [actionLoading, setActionLoading] = useState(false);

	const canManage = canManageMembers(currentUserRole ?? null);

	// Load team members
	const loadMembers = async (page = 0, search = "") => {
		try {
			setState((prev) => ({ ...prev, loading: true, error: null }));

			// Use project-scoped service if projectSlug is provided, otherwise use standalone service
			let response;
			if (projectSlug) {
				response = await projectService.getProjectTeamMembers(
					projectSlug,
					teamSlug,
					{
						page,
						size: state.pagination.size,
					}
				);
			} else {
				// This should not happen in project-scoped context
				// For now, throw an error to indicate this needs to be fixed
				throw new Error("Team members must be loaded in project context");
			}

			// Filter members by search term on the frontend
			const filteredMembers = search.trim()
				? response.content.filter(
						(member) =>
							member.firstName.toLowerCase().includes(search.toLowerCase()) ||
							member.lastName.toLowerCase().includes(search.toLowerCase()) ||
							member.email.toLowerCase().includes(search.toLowerCase())
				  )
				: response.content;

			setState((prev) => ({
				...prev,
				members: filteredMembers,
				pagination: {
					page: response.number,
					size: response.size,
					totalElements: response.totalElements,
					totalPages: response.totalPages,
					hasNext: !response.last,
					hasPrevious: !response.first,
				},
				loading: false,
			}));
		} catch (error) {
			setState((prev) => ({
				...prev,
				loading: false,
				error:
					error instanceof Error
						? error.message
						: "Failed to load team members",
			}));
		}
	};

	// Initial load
	useEffect(() => {
		loadMembers();
	}, [teamSlug, projectSlug]);

	// Search handler with debounce
	useEffect(() => {
		const debounceTimer = setTimeout(() => {
			if (state.searchTerm !== "") {
				loadMembers(0, state.searchTerm);
			} else {
				loadMembers(0);
			}
		}, 300);

		return () => clearTimeout(debounceTimer);
	}, [state.searchTerm]);

	// Handle member removal
	const handleRemoveMember = async () => {
		if (!selectedMember) return;

		setActionLoading(true);
		try {
			// Use project-scoped service if projectSlug is provided, otherwise use standalone service
			if (projectSlug) {
				await projectService.removeProjectTeamMember(
					projectSlug,
					teamSlug,
					selectedMember.userId
				);
			} else {
				// This should not happen in project-scoped context
				// For now, throw an error to indicate this needs to be fixed
				throw new Error("Team members must be managed in project context");
			}

			// Refresh the member list
			await loadMembers(state.pagination.page, state.searchTerm);

			// Notify parent component
			onMemberUpdate?.();

			setSelectedMember(null);
			setActionType(null);
		} catch (error) {
			setState((prev) => ({
				...prev,
				error:
					error instanceof Error ? error.message : "Failed to remove member",
			}));
		} finally {
			setActionLoading(false);
		}
	};

	// Handle role change
	const handleRoleChange = async () => {
		if (!selectedMember || !newRole) return;

		setActionLoading(true);
		try {
			// Use project-scoped service if projectSlug is provided, otherwise use standalone service
			if (projectSlug) {
				await projectService.updateProjectTeamMemberRole(
					projectSlug,
					teamSlug,
					selectedMember.userId,
					{
						role: newRole as any, // Cast to any since ProjectRole and TeamRole have same values
					}
				);
			} else {
				// This should not happen in project-scoped context
				// For now, throw an error to indicate this needs to be fixed
				throw new Error("Team members must be managed in project context");
			}

			// Refresh the member list
			await loadMembers(state.pagination.page, state.searchTerm);

			// Notify parent component
			onMemberUpdate?.();

			setSelectedMember(null);
			setActionType(null);
			setNewRole(null);
		} catch (error) {
			setState((prev) => ({
				...prev,
				error:
					error instanceof Error
						? error.message
						: "Failed to update member role",
			}));
		} finally {
			setActionLoading(false);
		}
	};

	// Handle pagination
	const handlePageChange = (newPage: number) => {
		loadMembers(newPage, state.searchTerm);
	};

	// Get member initials for avatar
	const getMemberInitials = (member: TeamMember): string => {
		return `${member.firstName[0]}${member.lastName[0]}`.toUpperCase();
	};

	// Format join date
	const formatJoinDate = (date: string | null): string => {
		if (!date) return "Unknown";
		return new Date(date).toLocaleDateString();
	};

	// Loading skeleton
	const MemberSkeleton = () => (
		<div className="flex items-center space-x-4 p-4">
			<div className="w-10 h-10 bg-muted rounded-full animate-pulse" />
			<div className="flex-1 space-y-2">
				<div className="h-4 bg-muted rounded w-1/4 animate-pulse" />
				<div className="h-3 bg-muted rounded w-1/3 animate-pulse" />
			</div>
			<div className="w-16 h-6 bg-muted rounded animate-pulse" />
		</div>
	);

	// Empty state
	const EmptyState = ({ isSearch = false }: { isSearch?: boolean }) => (
		<div className="text-center py-8">
			<User className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
			<h3 className="text-lg font-semibold mb-2">
				{isSearch ? "No members found" : "No team members"}
			</h3>
			<p className="text-muted-foreground">
				{isSearch
					? "Try adjusting your search terms"
					: "This team doesn't have any members yet."}
			</p>
		</div>
	);

	return (
		<Card className={cn("w-full", className)}>
			<CardHeader>
				<div className="flex items-center justify-between">
					<CardTitle className="flex items-center gap-2">
						<User className="h-5 w-5" />
						Team Members
						{state.pagination.totalElements > 0 && (
							<Badge variant="secondary" className="ml-2">
								{state.pagination.totalElements}
							</Badge>
						)}
					</CardTitle>
				</div>

				{/* Search */}
				<div className="relative">
					<Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
					<Input
						placeholder="Search members..."
						value={state.searchTerm}
						onChange={(e) =>
							setState((prev) => ({ ...prev, searchTerm: e.target.value }))
						}
						className="pl-10"
					/>
				</div>
			</CardHeader>

			<CardContent>
				{/* Error State */}
				{state.error && (
					<div className="flex items-center gap-2 p-4 mb-4 border border-destructive/20 rounded-lg bg-destructive/10">
						<AlertCircle className="h-4 w-4 text-destructive" />
						<span className="text-sm text-destructive">{state.error}</span>
						<Button
							variant="outline"
							size="sm"
							onClick={() =>
								loadMembers(state.pagination.page, state.searchTerm)
							}
							className="ml-auto"
						>
							Retry
						</Button>
					</div>
				)}

				{/* Loading State */}
				{state.loading && (
					<div className="space-y-4">
						{Array.from({ length: 3 }).map((_, i) => (
							<MemberSkeleton key={i} />
						))}
					</div>
				)}

				{/* Empty State */}
				{!state.loading && state.members.length === 0 && (
					<EmptyState isSearch={!!state.searchTerm} />
				)}

				{/* Member List */}
				{!state.loading && state.members.length > 0 && (
					<div className="space-y-4">
						{state.members.map((member) => (
							<div
								key={member.id}
								className="flex items-center justify-between p-4 border rounded-lg hover:bg-muted/50 transition-colors"
							>
								<div className="flex items-center space-x-4">
									<Avatar>
										<div className="w-10 h-10 bg-primary/10 rounded-full flex items-center justify-center">
											<span className="text-sm font-medium">
												{getMemberInitials(member)}
											</span>
										</div>
									</Avatar>

									<div className="flex-1 min-w-0">
										<div className="flex items-center gap-2 mb-1">
											<h4 className="font-medium truncate">
												{member.firstName} {member.lastName}
											</h4>
											<RoleBadge role={member.role} size="sm" />
										</div>

										<div className="flex items-center gap-4 text-sm text-muted-foreground">
											<span className="flex items-center gap-1">
												<Mail className="h-3 w-3" />
												{member.email}
											</span>
											<span className="flex items-center gap-1">
												<Calendar className="h-3 w-3" />
												Joined {formatJoinDate(member.joinedAt)}
											</span>
										</div>
									</div>
								</div>

								{/* Member Actions */}
								{canManage && (
									<DropdownMenu>
										<DropdownMenuTrigger asChild>
											<Button variant="ghost" size="sm">
												<MoreHorizontal className="h-4 w-4" />
											</Button>
										</DropdownMenuTrigger>
										<DropdownMenuContent align="end">
											<DropdownMenuItem
												onClick={() => {
													setSelectedMember(member);
													setActionType("roleChange");
													setNewRole(member.role);
												}}
											>
												<Shield className="h-4 w-4 mr-2" />
												Change Role
											</DropdownMenuItem>
											<DropdownMenuSeparator />
											<DropdownMenuItem
												onClick={() => {
													setSelectedMember(member);
													setActionType("remove");
												}}
												className="text-destructive focus:text-destructive"
											>
												<UserMinus className="h-4 w-4 mr-2" />
												Remove Member
											</DropdownMenuItem>
										</DropdownMenuContent>
									</DropdownMenu>
								)}
							</div>
						))}
					</div>
				)}

				{/* Pagination */}
				{!state.loading && state.pagination.totalPages > 1 && (
					<div className="flex items-center justify-between mt-6">
						<div className="text-sm text-muted-foreground">
							Showing {state.pagination.page * state.pagination.size + 1} to{" "}
							{Math.min(
								(state.pagination.page + 1) * state.pagination.size,
								state.pagination.totalElements
							)}{" "}
							of {state.pagination.totalElements} members
						</div>

						<div className="flex items-center space-x-2">
							<Button
								variant="outline"
								size="sm"
								disabled={!state.pagination.hasPrevious}
								onClick={() => handlePageChange(state.pagination.page - 1)}
							>
								Previous
							</Button>
							<Button
								variant="outline"
								size="sm"
								disabled={!state.pagination.hasNext}
								onClick={() => handlePageChange(state.pagination.page + 1)}
							>
								Next
							</Button>
						</div>
					</div>
				)}
			</CardContent>

			{/* Role Change Dialog */}
			<AlertDialog
				open={actionType === "roleChange"}
				onOpenChange={() => setActionType(null)}
			>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Change Member Role</AlertDialogTitle>
						<AlertDialogDescription>
							Update the role for {selectedMember?.firstName}{" "}
							{selectedMember?.lastName}
						</AlertDialogDescription>
					</AlertDialogHeader>

					<div className="py-4">
						<Select
							value={newRole || ""}
							onValueChange={(value) => setNewRole(value as TeamRole)}
						>
							<SelectTrigger>
								<SelectValue placeholder="Select new role" />
							</SelectTrigger>
							<SelectContent>
								<SelectItem value="ADMIN">Admin</SelectItem>
								<SelectItem value="MEMBER">Member</SelectItem>
							</SelectContent>
						</Select>
					</div>

					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={handleRoleChange}
							disabled={actionLoading || !newRole}
						>
							{actionLoading && (
								<Loader2 className="h-4 w-4 mr-2 animate-spin" />
							)}
							Update Role
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>

			{/* Remove Member Dialog */}
			<AlertDialog
				open={actionType === "remove"}
				onOpenChange={() => setActionType(null)}
			>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Remove Team Member</AlertDialogTitle>
						<AlertDialogDescription>
							Are you sure you want to remove {selectedMember?.firstName}{" "}
							{selectedMember?.lastName} from this team? This action cannot be
							undone.
						</AlertDialogDescription>
					</AlertDialogHeader>

					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={handleRemoveMember}
							disabled={actionLoading}
							className="bg-destructive hover:bg-destructive/90"
						>
							{actionLoading && (
								<Loader2 className="h-4 w-4 mr-2 animate-spin" />
							)}
							Remove Member
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>
		</Card>
	);
}

// Loading skeleton component for external use
export function TeamMemberListSkeleton({ className }: { className?: string }) {
	return (
		<Card className={cn("w-full", className)}>
			<CardHeader>
				<div className="flex items-center justify-between">
					<div className="h-6 w-32 bg-muted rounded animate-pulse" />
					<div className="h-6 w-16 bg-muted rounded animate-pulse" />
				</div>
				<div className="h-10 w-full bg-muted rounded animate-pulse" />
			</CardHeader>
			<CardContent>
				<div className="space-y-4">
					{Array.from({ length: 3 }).map((_, i) => (
						<div key={i} className="flex items-center space-x-4 p-4">
							<div className="w-10 h-10 bg-muted rounded-full animate-pulse" />
							<div className="flex-1 space-y-2">
								<div className="h-4 bg-muted rounded w-1/4 animate-pulse" />
								<div className="h-3 bg-muted rounded w-1/3 animate-pulse" />
							</div>
							<div className="w-16 h-6 bg-muted rounded animate-pulse" />
						</div>
					))}
				</div>
			</CardContent>
		</Card>
	);
}
