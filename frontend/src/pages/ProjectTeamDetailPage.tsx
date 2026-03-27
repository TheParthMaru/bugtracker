/**
 * ProjectTeamDetailPage Component
 *
 * Detailed view of a team within a project context with member management,
 * team information, and actions. Provides comprehensive team management
 * capabilities for team admins and project admins.
 *
 * Features:
 * - Team information display with project context
 * - Member management interface
 * - Team actions (edit, delete, leave)
 * - Member addition and role management
 * - Project-scoped navigation
 * - Responsive design
 * - Loading states and error handling
 * - Breadcrumb navigation
 */

import React, { useState, useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import {
	Users,
	Settings,
	Edit,
	Trash2,
	LogOut,
	ArrowLeft,
	Calendar,
	UserPlus,
	Loader2,
	AlertCircle,
	Copy,
	FolderOpen,
} from "lucide-react";
import { toast } from "react-toastify";
import { Badge } from "@/components/ui/badge";
import { Footer } from "@/components/ui/footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import Navbar from "@/components/Navbar";

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
import { projectService } from "@/services/projectService";
import { TeamMemberList } from "@/components/teams";
import { AddMemberModal, useAddMemberModal } from "@/components/teams";
import { RoleBadge, hasAdminPermissions } from "@/components/teams";
import { cn } from "@/lib/utils";
import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";
import type { Team, TeamMember, TeamRole } from "@/types/team";
import type { Project } from "@/types/project";

export function ProjectTeamDetailPage() {
	const { projectSlug, teamSlug } = useParams<{
		projectSlug: string;
		teamSlug: string;
	}>();
	const navigate = useNavigate();

	console.log("ProjectTeamDetailPage -> Component mounted -> URL params:", {
		projectSlug,
		teamSlug,
	});
	console.log(
		"ProjectTeamDetailPage -> Component mounted -> Current URL:",
		window.location.href
	);

	// State management
	const [team, setTeam] = useState<Team | null>(null);
	const [project, setProject] = useState<Project | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [confirmAction, setConfirmAction] = useState<"delete" | "leave" | null>(
		null
	);
	const [actionLoading, setActionLoading] = useState(false);

	// Add member modal
	const {
		isOpen: isAddMemberOpen,
		openModal: openAddMember,
		closeModal: closeAddMember,
	} = useAddMemberModal();
	const [isAddingMember, setIsAddingMember] = useState(false);
	const [existingMembers, setExistingMembers] = useState<TeamMember[]>([]);

	// Load team and project data
	const loadTeamData = async () => {
		console.log(
			"ProjectTeamDetailPage -> loadTeamData -> Starting with params:",
			{ projectSlug, teamSlug }
		);
		if (!projectSlug || !teamSlug) {
			console.log(
				"ProjectTeamDetailPage -> loadTeamData -> Missing params, returning early"
			);
			return;
		}

		try {
			setLoading(true);
			setError(null);

			console.log(
				"ProjectTeamDetailPage -> loadTeamData -> About to fetch project and team data"
			);
			// Load both project and team data
			const [projectData, teamData] = await Promise.all([
				projectService.getProjectBySlug(projectSlug),
				projectService.getProjectTeam(projectSlug, teamSlug),
			]);
			console.log("ProjectTeamDetailPage -> loadTeamData -> Received data:", {
				projectData: {
					id: projectData.id,
					name: projectData.name,
					projectSlug: projectData.projectSlug,
				},
				teamData: {
					id: teamData.id,
					name: teamData.name,
					teamSlug: teamData.teamSlug,
				},
			});

			setProject(projectData);
			setTeam(teamData);

			// Load team members for the add member modal
			try {
				const membersResponse = await projectService.getProjectTeamMembers(
					projectSlug,
					teamData.teamSlug
				);
				setExistingMembers(membersResponse.content);
			} catch (memberError) {
				console.error("Failed to load team members:", memberError);
				// Don't fail the whole page if members fail to load
			}
		} catch (error) {
			console.error("Failed to load team data:", error);
			const errorMessage =
				error instanceof Error ? error.message : "Failed to load team data";
			setError(errorMessage);
			toast.error(errorMessage);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		console.log("ProjectTeamDetailPage -> useEffect -> Dependencies changed:", {
			projectSlug,
			teamSlug,
		});
		loadTeamData();
	}, [projectSlug, teamSlug]);

	// Handle team actions
	const handleEditTeam = () => {
		console.log("ProjectTeamDetailPage -> handleEditTeam -> Called with:", {
			projectSlug,
			teamSlug,
			teamTeamSlug: team?.teamSlug,
		});
		if (projectSlug && team?.teamSlug) {
			const editUrl = `/projects/${projectSlug}/teams/${team.teamSlug}/edit`;
			console.log(
				"ProjectTeamDetailPage -> handleEditTeam -> Navigating to:",
				editUrl
			);
			navigate(editUrl);
		} else {
			console.log(
				"ProjectTeamDetailPage -> handleEditTeam -> Cannot navigate, missing data"
			);
		}
	};

	const handleDeleteTeam = async () => {
		if (!projectSlug || !team?.teamSlug) return;

		try {
			setActionLoading(true);
			await projectService.deleteProjectTeam(projectSlug, team.teamSlug);
			toast.success("Team deleted successfully");
			navigate(`/projects/${projectSlug}`);
		} catch (error) {
			console.error("Failed to delete team:", error);
			// Show the actual error message from the backend
			const errorMessage =
				error instanceof Error ? error.message : "Failed to delete team";
			toast.error(errorMessage);
		} finally {
			setActionLoading(false);
			setConfirmAction(null);
		}
	};

	const handleLeaveTeam = async () => {
		if (!projectSlug || !team?.teamSlug) return;

		try {
			setActionLoading(true);
			await projectService.leaveProjectTeam(projectSlug, team.teamSlug);
			toast.success("Left team successfully");
			navigate(`/projects/${projectSlug}`);
		} catch (error) {
			console.error("Failed to leave team:", error);
			// Show the actual error message from the backend
			const errorMessage =
				error instanceof Error ? error.message : "Failed to leave team";
			toast.error(errorMessage);
		} finally {
			setActionLoading(false);
			setConfirmAction(null);
		}
	};

	const handleAddMember = async (data: any) => {
		if (!projectSlug || !team?.teamSlug) return;

		try {
			setIsAddingMember(true);
			await projectService.addProjectTeamMember(
				projectSlug,
				team.teamSlug,
				data
			);
			toast.success("Member added successfully");
			closeAddMember();
			await loadTeamData(); // Refresh team data
		} catch (error) {
			console.error("Failed to add member:", error);
			// Show the exact backend error message if available
			const errorMessage =
				error instanceof Error ? error.message : "Failed to add member";
			toast.error(errorMessage);
		} finally {
			setIsAddingMember(false);
		}
	};

	const handleMemberUpdate = async () => {
		await loadTeamData(); // Refresh team data
	};

	const handleCopyTeamLink = async () => {
		if (!projectSlug || !team?.teamSlug) return;

		const teamUrl = `${window.location.origin}/api/bugtracker/v1/projects/${projectSlug}/teams/${team.teamSlug}`;
		try {
			await navigator.clipboard.writeText(teamUrl);
			toast.success("Team API link copied to clipboard");
		} catch (error) {
			console.error("Failed to copy link:", error);
			toast.error("Failed to copy link");
		}
	};

	// Utility functions
	const getUserInitials = (name: string): string => {
		return name
			.split(" ")
			.map((n) => n[0])
			.join("")
			.toUpperCase()
			.slice(0, 2);
	};

	const formatDate = (dateString: string | null): string => {
		if (!dateString) return "Unknown";
		return new Date(dateString).toLocaleDateString();
	};

	// Loading state
	if (loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-6xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="ghost" size="sm" disabled>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
						</Button>
					</div>
					<div className="space-y-6">
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
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	// Error state
	if (error || !team || !project) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-6xl">
					<div className="flex items-center gap-4 mb-8">
						<Button
							variant="ghost"
							size="sm"
							onClick={() => navigate(`/projects/${projectSlug}`)}
						>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
						</Button>
					</div>
					<Card>
						<CardContent className="p-8 text-center">
							<AlertCircle className="h-12 w-12 mx-auto text-destructive mb-4" />
							<h3 className="text-lg font-semibold mb-2">Team Not Found</h3>
							<p className="text-muted-foreground mb-4">
								{error ||
									"The team you're looking for doesn't exist or you don't have access to it."}
							</p>
							<Button onClick={() => navigate(`/projects/${projectSlug}`)}>
								Back to Project
							</Button>
						</CardContent>
					</Card>
				</main>
				<Footer />
			</div>
		);
	}

	const isAdmin = hasAdminPermissions(team.currentUserRole ?? null);
	const isMember = !!team.currentUserRole;
	const isProjectAdmin = project.isUserAdmin;

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />
			<main className="flex-1 container mx-auto px-4 py-4">
				{/* Breadcrumb Navigation */}
				<ProjectBreadcrumb
					projectName={project.name}
					projectSlug={projectSlug!}
					section="Teams"
					sectionHref={`/projects/${projectSlug}/teams`}
					current={team.name}
					onBackClick={() => navigate(`/projects/${projectSlug}`)}
				/>

				{/* Team Header */}
				<Card className="mb-8">
					<CardHeader>
						<div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
							<div className="flex-1">
								<div className="flex items-center gap-3 mb-2">
									<h1 className="text-3xl font-bold">{team.name}</h1>
									{team.currentUserRole && (
										<RoleBadge role={team.currentUserRole} />
									)}
								</div>
								{team.description && (
									<p className="text-muted-foreground mb-4">
										{team.description}
									</p>
								)}
								<div className="flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
									<div className="flex items-center gap-1">
										<Users className="h-4 w-4" />
										<span>
											{team.memberCount} member
											{team.memberCount !== 1 ? "s" : ""}
										</span>
									</div>
									<div className="flex items-center gap-1">
										<Calendar className="h-4 w-4" />
										<span>Created {formatDate(team.createdAt)}</span>
									</div>
									<div className="flex items-center gap-1">
										<Avatar className="h-4 w-4">
											<AvatarFallback className="text-xs">
												{getUserInitials(team.creatorName)}
											</AvatarFallback>
										</Avatar>
										<span>by {team.creatorName}</span>
									</div>
								</div>
							</div>

							{/* Team Actions */}
							<div className="flex items-center gap-2">
								{/* Copy Link */}
								<Button
									variant="outline"
									size="sm"
									onClick={handleCopyTeamLink}
								>
									<Copy className="h-4 w-4 mr-2" />
									Copy Link
								</Button>

								{/* Edit Team */}
								{(isAdmin || isProjectAdmin) && (
									<Button variant="outline" size="sm" onClick={handleEditTeam}>
										<Edit className="h-4 w-4 mr-2" />
										Edit
									</Button>
								)}

								{/* Add Member */}
								{(isAdmin || isProjectAdmin) && (
									<Button variant="outline" size="sm" onClick={openAddMember}>
										<UserPlus className="h-4 w-4 mr-2" />
										Add Member
									</Button>
								)}

								{/* Leave Team */}
								{isMember && !isProjectAdmin && (
									<Button
										variant="outline"
										size="sm"
										onClick={() => setConfirmAction("leave")}
										className="text-orange-600 hover:text-orange-700"
									>
										<LogOut className="h-4 w-4 mr-2" />
										Leave
									</Button>
								)}

								{/* Delete Team */}
								{(isAdmin || isProjectAdmin) && (
									<Button
										variant="outline"
										size="sm"
										onClick={() => setConfirmAction("delete")}
										className="text-destructive hover:text-destructive"
									>
										<Trash2 className="h-4 w-4 mr-2" />
										Delete
									</Button>
								)}
							</div>
						</div>
					</CardHeader>
				</Card>

				{/* Team Information */}
				<div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
					{/* Main Content - Member Management */}
					<div className="lg:col-span-2">
						<TeamMemberList
							teamSlug={team.teamSlug}
							projectSlug={projectSlug}
							currentUserRole={team.currentUserRole}
							onMemberUpdate={handleMemberUpdate}
						/>
					</div>

					{/* Sidebar - Team Details */}
					<div className="space-y-6">
						{/* Team Details */}
						<Card>
							<CardHeader>
								<CardTitle className="flex items-center gap-2">
									<Settings className="h-5 w-5" />
									Team Details
								</CardTitle>
							</CardHeader>
							<CardContent className="space-y-4">
								<div>
									<label className="text-sm font-medium text-muted-foreground">
										Team URL
									</label>
									<div className="flex items-center gap-2 mt-1">
										<code className="text-xs bg-muted px-2 py-1 rounded">
											/projects/{projectSlug}/teams/{team.teamSlug}
										</code>
										<Button
											variant="ghost"
											size="sm"
											onClick={handleCopyTeamLink}
										>
											<Copy className="h-3 w-3" />
										</Button>
									</div>
								</div>
								<div>
									<label className="text-sm font-medium text-muted-foreground">
										Project
									</label>
									<div className="mt-1">
										<Link
											to={`/projects/${projectSlug}`}
											className="text-sm text-primary hover:underline"
										>
											{project.name}
										</Link>
									</div>
								</div>
								<div>
									<label className="text-sm font-medium text-muted-foreground">
										Created
									</label>
									<div className="mt-1 text-sm">
										{formatDate(team.createdAt)}
									</div>
								</div>
								<div>
									<label className="text-sm font-medium text-muted-foreground">
										Last Updated
									</label>
									<div className="mt-1 text-sm">
										{formatDate(team.updatedAt)}
									</div>
								</div>
							</CardContent>
						</Card>

						{/* Quick Actions */}
						{(isAdmin || isProjectAdmin) && (
							<Card>
								<CardHeader>
									<CardTitle className="text-lg">Quick Actions</CardTitle>
								</CardHeader>
								<CardContent className="space-y-2">
									<Button
										variant="outline"
										size="sm"
										onClick={openAddMember}
										className="w-full justify-start"
									>
										<UserPlus className="h-4 w-4 mr-2" />
										Add Member
									</Button>
									<Button
										variant="outline"
										size="sm"
										onClick={handleEditTeam}
										className="w-full justify-start"
									>
										<Edit className="h-4 w-4 mr-2" />
										Edit Team
									</Button>
								</CardContent>
							</Card>
						)}
					</div>
				</div>

				{/* Add Member Modal */}
				<AddMemberModal
					isOpen={isAddMemberOpen}
					onClose={closeAddMember}
					onSubmit={handleAddMember}
					teamId={team.id}
					existingMembers={existingMembers}
					isLoading={isAddingMember}
					projectSlug={projectSlug}
				/>

				{/* Delete Team Confirmation */}
				<AlertDialog
					open={confirmAction === "delete"}
					onOpenChange={() => setConfirmAction(null)}
				>
					<AlertDialogContent>
						<AlertDialogHeader>
							<AlertDialogTitle>Delete Team</AlertDialogTitle>
							<AlertDialogDescription>
								Are you sure you want to delete "{team.name}"? This action
								cannot be undone. All team data, including members and
								associated content, will be permanently deleted.
							</AlertDialogDescription>
						</AlertDialogHeader>
						<AlertDialogFooter>
							<AlertDialogCancel>Cancel</AlertDialogCancel>
							<AlertDialogAction
								onClick={handleDeleteTeam}
								disabled={actionLoading}
								className="bg-destructive hover:bg-destructive/90"
							>
								{actionLoading && (
									<Loader2 className="h-4 w-4 mr-2 animate-spin" />
								)}
								Delete Team
							</AlertDialogAction>
						</AlertDialogFooter>
					</AlertDialogContent>
				</AlertDialog>

				{/* Leave Team Confirmation */}
				<AlertDialog
					open={confirmAction === "leave"}
					onOpenChange={() => setConfirmAction(null)}
				>
					<AlertDialogContent>
						<AlertDialogHeader>
							<AlertDialogTitle>Leave Team</AlertDialogTitle>
							<AlertDialogDescription>
								Are you sure you want to leave "{team.name}"? You will lose
								access to this team and all its content. You can rejoin if
								you're invited again.
							</AlertDialogDescription>
						</AlertDialogHeader>
						<AlertDialogFooter>
							<AlertDialogCancel>Cancel</AlertDialogCancel>
							<AlertDialogAction
								onClick={handleLeaveTeam}
								disabled={actionLoading}
								className="bg-orange-600 hover:bg-orange-700"
							>
								{actionLoading && (
									<Loader2 className="h-4 w-4 mr-2 animate-spin" />
								)}
								Leave Team
							</AlertDialogAction>
						</AlertDialogFooter>
					</AlertDialogContent>
				</AlertDialog>
			</main>
			<Footer />
		</div>
	);
}
