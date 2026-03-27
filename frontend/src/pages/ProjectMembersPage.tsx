/**
 * ProjectMembersPage Component
 *
 * Page for managing project members with comprehensive member management capabilities.
 * Allows project admins to view, promote, demote, and remove members.
 *
 * Features:
 * - Member list with avatar, name, email, role, and joined date
 * - Role change functionality (admin only)
 * - Member removal (admin only)
 * - Loading states and error handling
 * - Responsive design and accessibility
 * - Pagination support
 * - Empty state handling
 * - Search functionality
 */

import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
	ArrowLeft,
	Users,
	MoreHorizontal,
	UserMinus,
	Shield,
	Calendar,
	Mail,
	Loader2,
	AlertCircle,
	Search,
	UserPlus,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
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
import { Separator } from "@/components/ui/separator";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { projectService } from "@/services/projectService";
import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";
import {
	ProjectRoleBadge,
	hasAdminPermissions,
} from "@/components/projects/ProjectRoleBadge";
import { cn } from "@/lib/utils";
import { toast } from "react-toastify";
import type {
	Project,
	ProjectMember,
	ProjectMembersListResponse,
} from "@/types/project";
import { ProjectRole, MemberStatus } from "@/types/project";

interface ProjectMembersPageState {
	members: ProjectMember[];
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

export function ProjectMembersPage() {
	const { projectSlug } = useParams<{ projectSlug: string }>();
	const navigate = useNavigate();

	// State management
	const [project, setProject] = useState<Project | null>(null);
	const [state, setState] = useState<ProjectMembersPageState>({
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

	const [selectedMember, setSelectedMember] = useState<ProjectMember | null>(
		null
	);
	const [actionType, setActionType] = useState<"remove" | "roleChange" | null>(
		null
	);
	const [newRole, setNewRole] = useState<ProjectRole | null>(null);
	const [actionLoading, setActionLoading] = useState(false);

	// Load project and members
	const loadProjectAndMembers = async (page = 0, search = "") => {
		if (!projectSlug) return;

		try {
			setState((prev) => ({ ...prev, loading: true, error: null }));

			// Load project details
			const projectData = await projectService.getProjectBySlug(projectSlug);
			setProject(projectData);

			// Check if user is a member of the project (any role is fine for viewing)
			if (!projectData.userRole) {
				setState((prev) => ({
					...prev,
					loading: false,
					error: "You don't have access to this project",
				}));
				return;
			}

			// Load project members
			const response = await projectService.getProjectMembers(projectSlug, {
				page,
				size: state.pagination.size,
				status: MemberStatus.ACTIVE, // Only show active members
			});

			// Filter members by search term on the frontend
			const filteredMembers = search.trim()
				? response.content.filter(
						(member) =>
							member.firstName.toLowerCase().includes(search.toLowerCase()) ||
							member.lastName.toLowerCase().includes(search.toLowerCase()) ||
							member.userEmail.toLowerCase().includes(search.toLowerCase())
				  )
				: response.content;

			// Debug: Log the first member to see what data we're getting
			if (filteredMembers.length > 0) {
				console.log("First member data:", filteredMembers[0]);
			}

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
						: "Failed to load project members",
			}));
		}
	};

	// Initial load
	useEffect(() => {
		loadProjectAndMembers();
	}, [projectSlug]);

	// Search handler with debounce
	useEffect(() => {
		const debounceTimer = setTimeout(() => {
			if (state.searchTerm !== "") {
				loadProjectAndMembers(0, state.searchTerm);
			} else {
				loadProjectAndMembers(0);
			}
		}, 300);

		return () => clearTimeout(debounceTimer);
	}, [state.searchTerm]);

	// Handle member removal
	const handleRemoveMember = async () => {
		if (!selectedMember || !projectSlug) return;

		setActionLoading(true);
		try {
			await projectService.removeMember(projectSlug, selectedMember.userId);

			// Refresh the member list
			await loadProjectAndMembers(state.pagination.page, state.searchTerm);

			toast.success(
				`Removed ${selectedMember.firstName} ${selectedMember.lastName} from the project`
			);

			setSelectedMember(null);
			setActionType(null);
		} catch (error) {
			toast.error(
				error instanceof Error ? error.message : "Failed to remove member"
			);
		} finally {
			setActionLoading(false);
		}
	};

	// Handle role change
	const handleRoleChange = async () => {
		if (!selectedMember || !newRole || !projectSlug) return;

		setActionLoading(true);
		try {
			await projectService.updateMemberRole(
				projectSlug,
				selectedMember.userId,
				{
					role: newRole,
				}
			);

			// Refresh the member list
			await loadProjectAndMembers(state.pagination.page, state.searchTerm);

			toast.success(
				`Updated ${selectedMember.firstName} ${selectedMember.lastName}'s role to ${newRole}`
			);

			setSelectedMember(null);
			setActionType(null);
			setNewRole(null);
		} catch (error) {
			toast.error(
				error instanceof Error ? error.message : "Failed to update member role"
			);
		} finally {
			setActionLoading(false);
		}
	};

	// Handle pagination
	const handlePageChange = (newPage: number) => {
		loadProjectAndMembers(newPage, state.searchTerm);
	};

	// Get member initials for avatar
	const getMemberInitials = (member: ProjectMember): string => {
		// Try firstName + lastName first
		if (member.firstName && member.lastName) {
			const firstInitial = member.firstName[0];
			const lastInitial = member.lastName[0];
			return `${firstInitial}${lastInitial}`.toUpperCase();
		}

		// Try userName as fallback
		if (member.userName) {
			const nameParts = member.userName.trim().split(" ");
			if (nameParts.length >= 2) {
				return `${nameParts[0][0]}${nameParts[1][0]}`.toUpperCase();
			} else if (nameParts.length === 1) {
				return nameParts[0][0].toUpperCase();
			}
		}

		// Try email as last resort
		if (member.userEmail) {
			const emailName = member.userEmail.split("@")[0];
			if (emailName.length >= 2) {
				return emailName.substring(0, 2).toUpperCase();
			}
		}

		// Final fallback
		return "U";
	};

	// Format join date
	const formatJoinDate = (date: string | null): string => {
		if (!date) return "Unknown";
		return new Date(date).toLocaleDateString();
	};

	// Handle back navigation
	const handleBack = () => {
		if (project) {
			navigate(`/projects/${project.projectSlug}`);
		} else {
			navigate("/projects");
		}
	};

	// Loading state
	if (state.loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-6xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="outline" size="sm" disabled>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
						</Button>
					</div>
					<Card>
						<CardContent className="p-6">
							<div className="flex items-center gap-4">
								<Loader2 className="h-8 w-8 animate-spin" />
								<div className="space-y-2">
									<div className="h-6 bg-muted rounded w-48 animate-pulse" />
									<div className="h-4 bg-muted rounded w-32 animate-pulse" />
								</div>
							</div>
						</CardContent>
					</Card>
				</main>
				<Footer />
			</div>
		);
	}

	// Error state
	if (state.error || !project) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-6xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="outline" size="sm" onClick={handleBack}>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
						</Button>
					</div>
					<Card>
						<CardHeader>
							<CardTitle className="text-red-600">
								Error Loading Members
							</CardTitle>
							<CardDescription>
								{state.error || "Failed to load project members"}
							</CardDescription>
						</CardHeader>
						<CardContent>
							<Button onClick={handleBack}>
								<ArrowLeft className="h-4 w-4 mr-2" />
								Back to Project
							</Button>
						</CardContent>
					</Card>
				</main>
				<Footer />
			</div>
		);
	}

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />

			<main className="flex-1 container mx-auto px-4 py-8 max-w-6xl">
				{/* Breadcrumb */}
				<ProjectBreadcrumb
					projectName={project.name}
					projectSlug={project.projectSlug}
					current="Members"
					onBackClick={handleBack}
				/>

				{/* Page Header */}
				<div className="mb-8">
					<h1 className="text-3xl font-bold mb-2">Project Members</h1>
					<p className="text-muted-foreground">
						{hasAdminPermissions(project.userRole)
							? `Manage members of "${project.name}". You can change roles, remove members, and view member details.`
							: `View members of "${project.name}". You can see all project members and their details.`}
					</p>
				</div>

				{/* Search and Filters */}
				<Card className="mb-6">
					<CardContent className="p-6">
						<div className="flex items-center gap-4">
							<div className="flex-1">
								<div className="relative">
									<Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
									<Input
										placeholder="Search members by name or email..."
										value={state.searchTerm}
										onChange={(e) =>
											setState((prev) => ({
												...prev,
												searchTerm: e.target.value,
											}))
										}
										className="pl-10"
									/>
								</div>
							</div>
							<div className="flex items-center gap-2 text-sm text-muted-foreground">
								<Users className="h-4 w-4" />
								<span>
									{state.pagination.totalElements} member
									{state.pagination.totalElements !== 1 ? "s" : ""}
								</span>
							</div>
						</div>
					</CardContent>
				</Card>

				{/* Members List */}
				<Card>
					<CardHeader>
						<CardTitle>Active Members</CardTitle>
						<CardDescription>
							All active members of this project. Only admins can manage member
							roles and remove members.
						</CardDescription>
					</CardHeader>
					<CardContent>
						{state.members.length === 0 ? (
							<div className="text-center py-12">
								<Users className="h-12 w-12 mx-auto mb-4 text-muted-foreground opacity-50" />
								<h3 className="text-lg font-semibold mb-2">No Members Found</h3>
								<p className="text-muted-foreground">
									{state.searchTerm
										? "No members match your search criteria"
										: "This project has no active members yet"}
								</p>
							</div>
						) : (
							<div className="space-y-4">
								{state.members.map((member) => (
									<div
										key={member.id}
										className="flex items-center justify-between p-6 border rounded-lg hover:bg-muted/50 transition-colors"
									>
										<div className="flex items-center space-x-4">
											{/* Avatar with Initials */}
											<Avatar className="h-12 w-12">
												<AvatarFallback className="text-sm font-semibold">
													{getMemberInitials(member)}
												</AvatarFallback>
											</Avatar>

											{/* Member Information */}
											<div className="flex-1 min-w-0">
												{/* Name and Role Badge */}
												<div className="flex items-center gap-3 mb-2">
													<h4 className="font-semibold text-lg truncate">
														{member.firstName && member.lastName
															? `${member.firstName} ${member.lastName}`
															: member.userName ||
															  member.userEmail ||
															  "Unknown User"}
													</h4>
													<ProjectRoleBadge role={member.role} size="md" />
												</div>

												{/* Email and Join Date */}
												<div className="flex items-center gap-6 text-sm text-muted-foreground">
													<span className="flex items-center gap-2">
														<Mail className="h-4 w-4" />
														<span className="font-medium">
															{member.userEmail}
														</span>
													</span>
													<span className="flex items-center gap-2">
														<Calendar className="h-4 w-4" />
														<span>
															Joined {formatJoinDate(member.joinedAt)}
														</span>
													</span>
												</div>

												{/* Member Status Badge */}
												<div className="mt-2">
													<Badge variant="secondary" className="text-xs">
														{member.status === "ACTIVE"
															? "Active Member"
															: member.status}
													</Badge>
												</div>
											</div>
										</div>

										{/* Member Actions */}
										{hasAdminPermissions(project.userRole) && (
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
						{state.pagination.totalPages > 1 && (
							<div className="flex items-center justify-between mt-6 pt-6 border-t">
								<div className="text-sm text-muted-foreground">
									Showing {state.pagination.page * state.pagination.size + 1} to{" "}
									{Math.min(
										(state.pagination.page + 1) * state.pagination.size,
										state.pagination.totalElements
									)}{" "}
									of {state.pagination.totalElements} members
								</div>
								<div className="flex items-center gap-2">
									<Button
										variant="outline"
										size="sm"
										onClick={() => handlePageChange(state.pagination.page - 1)}
										disabled={!state.pagination.hasPrevious}
									>
										Previous
									</Button>
									<Button
										variant="outline"
										size="sm"
										onClick={() => handlePageChange(state.pagination.page + 1)}
										disabled={!state.pagination.hasNext}
									>
										Next
									</Button>
								</div>
							</div>
						)}
					</CardContent>
				</Card>
			</main>

			<Footer />

			{/* Role Change Dialog */}
			<AlertDialog
				open={actionType === "roleChange"}
				onOpenChange={(open) => {
					if (!open) {
						setActionType(null);
						setSelectedMember(null);
						setNewRole(null);
					}
				}}
			>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Change Member Role</AlertDialogTitle>
						<AlertDialogDescription>
							Change the role for {selectedMember?.firstName}{" "}
							{selectedMember?.lastName}. This will affect their permissions
							within the project.
						</AlertDialogDescription>
					</AlertDialogHeader>
					<div className="py-4">
						<label className="text-sm font-medium">New Role</label>
						<Select
							value={newRole || ""}
							onValueChange={(value) => setNewRole(value as ProjectRole)}
						>
							<SelectTrigger className="mt-2">
								<SelectValue placeholder="Select a role" />
							</SelectTrigger>
							<SelectContent>
								<SelectItem value={ProjectRole.MEMBER}>
									<div className="flex items-center gap-2">
										<Users className="h-4 w-4" />
										Member
									</div>
								</SelectItem>
								<SelectItem value={ProjectRole.ADMIN}>
									<div className="flex items-center gap-2">
										<Shield className="h-4 w-4" />
										Admin
									</div>
								</SelectItem>
							</SelectContent>
						</Select>
					</div>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={handleRoleChange}
							disabled={
								actionLoading || !newRole || newRole === selectedMember?.role
							}
						>
							{actionLoading ? (
								<>
									<Loader2 className="h-4 w-4 mr-2 animate-spin" />
									Updating...
								</>
							) : (
								"Update Role"
							)}
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>

			{/* Remove Member Dialog */}
			<AlertDialog
				open={actionType === "remove"}
				onOpenChange={(open) => {
					if (!open) {
						setActionType(null);
						setSelectedMember(null);
					}
				}}
			>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Remove Member</AlertDialogTitle>
						<AlertDialogDescription>
							Are you sure you want to remove {selectedMember?.firstName}{" "}
							{selectedMember?.lastName} from this project? This action cannot
							be undone.
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={handleRemoveMember}
							disabled={actionLoading}
							className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
						>
							{actionLoading ? (
								<>
									<Loader2 className="h-4 w-4 mr-2 animate-spin" />
									Removing...
								</>
							) : (
								"Remove Member"
							)}
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>
		</div>
	);
}
