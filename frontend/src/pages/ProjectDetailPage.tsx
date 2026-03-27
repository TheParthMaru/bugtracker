/**
 * ProjectDetailPage Component
 *
 * Displays detailed information about a specific project.
 * This is a placeholder implementation that will be expanded later.
 *
 * Features:
 * - Project information display
 * - Member management
 * - Team integration (future)
 * - Navigation with back button
 * - Proper routing with slug parameter
 */

import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
	ArrowLeft,
	Settings,
	Users,
	FolderOpen,
	UserPlus,
	// Plus,
	Search,
	LogOut,
	Trash2,
	Copy,
	Bug,
	BarChart3,
	User,
	Trophy,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
// import { Input } from "@/components/ui/input";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { PendingRequestsModal } from "@/components/projects/PendingRequestsModal";

// import {
// 	ProjectTeamCard,
// 	ProjectTeamCardSkeleton,
// } from "@/components/projects/ProjectTeamCard";
import { projectService } from "@/services/projectService";
import { bugService } from "@/services/bugService";
import { userService } from "@/services/userService";

import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";
import {
	ProjectRoleBadge,
	MemberStatusBadge,
	canAccessProject,
	isPendingRole,
	isRejectedMember,
} from "@/components/projects/ProjectRoleBadge";
// import { CreateTeamModal, useCreateTeamModal } from "@/components/teams";
import type { Project, ProjectDetailResponse } from "@/types/project";
// import type { Team } from "@/types/team";
import type { BugStatistics } from "@/types/bug";
// import type { CreateTeamRequest } from "@/types/team";
import { toast } from "react-toastify";
import SimilarityConfigPanel from "@/components/bugs/SimilarityConfigPanel";

export function ProjectDetailPage() {
	const { projectSlug } = useParams<{ projectSlug: string }>();
	const navigate = useNavigate();
	const [project, setProject] = useState<ProjectDetailResponse | null>(null);
	const [bugStatistics, setBugStatistics] = useState<BugStatistics | null>(
		null
	);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [isPendingRequestsOpen, setIsPendingRequestsOpen] = useState(false);

	const [currentUserId, setCurrentUserId] = useState<string>("");

	// Helper function to determine if user can access project features
	const canAccessProjectFeatures = () => {
		if (!project) return false;

		// Check if user has active membership with proper role
		const hasActiveRole = canAccessProject(project.userRole);
		const isActiveStatus = project.userMembershipStatus === "ACTIVE";

		return hasActiveRole && isActiveStatus;
	};

	// Helper function to get tooltip text for disabled buttons
	const getDisabledButtonTooltip = () => {
		if (!project) return "Loading...";

		if (!project.userRole) {
			return "Join project to access features";
		}

		if (isPendingRole(project.userRole)) {
			return "Awaiting admin approval";
		}

		if (isRejectedMember(project.userMembershipStatus)) {
			return "Request rejected - contact admin";
		}

		if (project.userMembershipStatus === "PENDING") {
			return "Awaiting admin approval";
		}

		return "Access restricted";
	};

	// const [teams, setTeams] = useState<Team[]>([]);
	// const [teamsLoading, setTeamsLoading] = useState(false);
	// const [teamsSearch, setTeamsSearch] = useState("");
	// const [canCreateTeams, setCanCreateTeams] = useState(false);

	// Create team modal
	// const {
	// 	isOpen: isCreateModalOpen,
	// 	openModal: openCreateModal,
	// 	closeModal: closeCreateModal,
	// } = useCreateTeamModal();
	// const [isCreating, setIsCreating] = useState(false);

	useEffect(() => {
		const loadProject = async () => {
			if (!projectSlug) {
				setError("Project slug is required");
				setLoading(false);
				return;
			}

			try {
				setLoading(true);
				setError(null);

				// Load current user first
				try {
					const userData = await userService.getCurrentUser();
					setCurrentUserId(userData.id);
				} catch (error) {
					console.error("Failed to load current user:", error);
					// Don't fail the entire page load for this
				}

				// Fetch project data and members in parallel
				const [projectData, membersResponse] = await Promise.all([
					projectService.getProjectBySlug(projectSlug),
					projectService.getProjectMembers(projectSlug),
				]);

				// Combine project data with members to create ProjectDetailResponse
				const projectWithMembers: ProjectDetailResponse = {
					...projectData,
					members: membersResponse.content,
				};

				setProject(projectWithMembers);

				// Load bug statistics only if user can access project features
				if (canAccessProjectFeatures()) {
					await loadBugStatistics();
				}
			} catch (err) {
				console.error("Failed to load project:", err);
				const errorMessage =
					err instanceof Error ? err.message : "Failed to load project";
				setError(errorMessage);
				toast.error(errorMessage);
			} finally {
				setLoading(false);
			}
		};

		loadProject();
	}, [projectSlug]);

	// const loadTeams = async () => {
	// 	if (!slug) return;

	// 	try {
	// 		setTeamsLoading(true);
	// 		const teamsResponse = await projectService.getProjectTeams(slug, {
	// 			search: teamsSearch || undefined,
	// 		// 	page: 0,
	// 		// 	size: 10,
	// 		// });
	// 		setTeams(teamsResponse.content);
	// 	} catch (err) {
	// 		console.error("Failed to load teams:", err);
	// 		toast.error("Failed to load project teams");
	// 	} finally {
	// 		setTeamsLoading(false);
	// 	}
	// };

	const loadBugStatistics = async () => {
		if (!projectSlug) return;

		// Only load bug statistics if user can access project features
		if (!canAccessProjectFeatures()) {
			console.log(
				"Skipping bug statistics - user cannot access project features"
			);
			return;
		}

		try {
			const statistics = await bugService.getBugStatistics(projectSlug);
			setBugStatistics(statistics);
		} catch (err) {
			console.error("Failed to load bug statistics:", err);
			// Don't show toast error for statistics as it's not critical
		}
	};

	// Debounced search effect
	// useEffect(() => {
	// 	const timeoutId = setTimeout(() => {
	// 		loadTeams();
	// 	}, 300);

	// 	return () => clearTimeout(timeoutId);
	// }, [teamsSearch, slug]);

	const handleBack = () => {
		navigate("/projects");
	};

	const handleEdit = () => {
		if (project) {
			navigate(`/projects/${project.projectSlug}/edit`);
		}
	};

	const handleManageMembers = () => {
		if (project) {
			navigate(`/projects/${project.projectSlug}/members`);
		}
	};

	// const handleCreateTeam = () => {
	// 	openCreateModal();
	// };

	// const handleCreateTeamSubmit = async (
	// 	data: CreateTeamRequest & { projectSlug: string }
	// ) => {
	// 	try {
	// 	// 	setIsCreating(true);
	// 	// 	await projectService.createProjectTeam(data.projectSlug, {
	// 	// 		name: data.name,
	// 	// 		description: data.description || "",
	// 	// 	});
	// 	// 	toast.success("Team created successfully");
	// 	// 	closeCreateModal();
	// 	// 	await loadTeams(); // Refresh teams list
	// 	// } catch (error) {
	// 	// 	console.error("Failed to create team:", error);
	// 	// 	toast.error("Failed to create team");
	// 	// } finally {
	// 	// 	setIsCreating(false);
	// 	// }
	// };

	// const handleViewTeam = (projectSlug: string, teamSlug: string) => {
	// 	navigate(`/projects/${projectSlug}/teams/${teamSlug}`);
	// };

	// const handleEditTeam = (projectSlug: string, teamSlug: string) => {
	// 	navigate(`/projects/${projectSlug}/teams/${teamSlug}/edit`);
	// };

	// const handleDeleteTeam = async (projectSlug: string, teamSlug: string) => {
	// 	if (
	// 	// 	!confirm(
	// 	// 		"Are you sure you want to delete this team? This action cannot be undone."
	// 	// 	)
	// 	// ) {
	// 	// 	return;
	// 	// }

	// 	try {
	// 	// 	await projectService.deleteProjectTeam(projectSlug, teamSlug);
	// 	// 	toast.success("Team deleted successfully");
	// 	// 	await loadTeams(); // Refresh teams list
	// 	// } catch (err) {
	// 	// 	console.error("Failed to delete team:", err);
	// 	// 	toast.error("Failed to delete team");
	// 	// }
	// };

	const handlePendingRequestsUpdated = async () => {
		// Refresh project data to update pending request count
		if (project) {
			try {
				const [updatedProject, membersResponse] = await Promise.all([
					projectService.getProjectBySlug(project.projectSlug),
					projectService.getProjectMembers(project.projectSlug),
				]);

				// Combine project data with members
				const projectWithMembers: ProjectDetailResponse = {
					...updatedProject,
					members: membersResponse.content,
				};

				setProject(projectWithMembers);
			} catch (err) {
				console.error("Failed to refresh project data:", err);
			}
		}
	};

	const handleLeaveProject = async () => {
		if (!project) return;

		if (
			!confirm(
				`Are you sure you want to leave "${project.name}"? You will lose access to this project and all its content.`
			)
		) {
			return;
		}

		try {
			await projectService.leaveProject(project.projectSlug);
			toast.success("Successfully left the project");
			navigate("/projects"); // Redirect to projects list
		} catch (err) {
			console.error("Failed to leave project:", err);
			// Show the actual error message from the backend
			const errorMessage =
				err instanceof Error ? err.message : "Failed to leave project";
			toast.error(errorMessage);
		}
	};

	const handleDeleteProject = async () => {
		if (!project) return;

		if (
			!confirm(
				`Are you sure you want to delete "${project.name}"? This action cannot be undone. All project data, including members, teams, and associated content, will be permanently deleted.`
			)
		) {
			return;
		}

		try {
			await projectService.deleteProject(project.projectSlug);
			toast.success("Project deleted successfully");
			navigate("/projects"); // Redirect to projects list
		} catch (err) {
			console.error("Failed to delete project:", err);
			// Show the actual error message from the backend
			const errorMessage =
				err instanceof Error ? err.message : "Failed to delete project";
			toast.error(errorMessage);
		}
	};

	const handleCopyProjectLink = async () => {
		if (!project) return;

		const projectUrl = `${window.location.origin}/api/bugtracker/v1/projects/${project.projectSlug}`;
		try {
			await navigator.clipboard.writeText(projectUrl);
			toast.success("Project API link copied to clipboard");
		} catch (error) {
			console.error("Failed to copy link:", error);
			toast.error("Failed to copy link");
		}
	};

	if (loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-7xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="outline" size="sm" disabled>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Projects
						</Button>
					</div>
					<div className="space-y-4">
						<div className="h-8 bg-muted rounded animate-pulse" />
						<div className="h-4 bg-muted rounded animate-pulse w-1/3" />
						<div className="h-32 bg-muted rounded animate-pulse" />
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	if (error || !project) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-7xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="outline" size="sm" onClick={handleBack}>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Projects
						</Button>
					</div>
					<Card>
						<CardHeader>
							<CardTitle className="text-red-600">Project Not Found</CardTitle>
							<CardDescription>
								{error || "The requested project could not be found."}
							</CardDescription>
						</CardHeader>
						<CardContent>
							<Button onClick={handleBack}>
								<ArrowLeft className="h-4 w-4 mr-2" />
								Back to Projects
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

			<main className="flex-1 container mx-auto px-4 py-4 transition-all duration-300">
				{/* Breadcrumb */}
				<ProjectBreadcrumb
					projectName={project.name}
					projectSlug={project.projectSlug}
					onBackClick={handleBack}
				/>

				{/* Page Title Section */}
				<div className="mb-8">
					<h1 className="text-4xl font-bold mb-2">{project.name}</h1>
					<p className="text-lg text-muted-foreground">
						{project.description
							? project.description
							: "Project details and management"}
					</p>
				</div>

				{/* Navigation Header */}
				<div className="flex items-center justify-between gap-4 mb-8"></div>

				{/* Quick Stats */}
				<div className="grid gap-4 md:grid-cols-3 mb-6">
					<Card>
						<CardContent className="p-4">
							<div className="flex items-center justify-between">
								<div>
									<p className="font-medium text-muted-foreground">
										Total Bugs
									</p>
									{canAccessProjectFeatures() ? (
										<p className="text-2xl font-bold">
											{bugStatistics?.totalBugs || 0}
										</p>
									) : (
										<p className="text-sm text-muted-foreground">
											{getDisabledButtonTooltip()}
										</p>
									)}
								</div>
								<Bug className="h-8 w-8 text-muted-foreground" />
							</div>
						</CardContent>
					</Card>
					<Card>
						<CardContent className="p-4">
							<div className="flex items-center justify-between">
								<div>
									<p className="font-medium text-muted-foreground">Open Bugs</p>
									{canAccessProjectFeatures() ? (
										<p className="text-2xl font-bold text-orange-600">
											{bugStatistics?.openBugs || 0}
										</p>
									) : (
										<p className="text-sm text-muted-foreground">
											{getDisabledButtonTooltip()}
										</p>
									)}
								</div>
								<Bug className="h-8 w-8 text-orange-600" />
							</div>
						</CardContent>
					</Card>
					<Card>
						<CardContent className="p-4">
							<div className="flex items-center justify-between">
								<div>
									<p className="font-medium text-muted-foreground">
										Project Admin
									</p>
									<div className="flex items-center gap-2 mt-1">
										<Avatar className="h-6 w-6">
											<AvatarFallback className="text-xs">
												{project.adminFirstName && project.adminLastName
													? `${project.adminFirstName[0]}${project.adminLastName[0]}`
													: "AD"}
											</AvatarFallback>
										</Avatar>
										<span className="text-sm font-medium">
											{project.adminFirstName && project.adminLastName
												? `${project.adminFirstName} ${project.adminLastName}`
												: "Project Admin"}
										</span>
									</div>
								</div>
								<User className="h-8 w-8 text-muted-foreground" />
							</div>
						</CardContent>
					</Card>
				</div>

				{/* Project Information */}
				<div className="grid gap-6 lg:grid-cols-3">
					{/* Main Content */}
					<div className="lg:col-span-2 space-y-6">
						<Card>
							<CardHeader>
								<CardTitle className="text-lg">Project Details</CardTitle>
							</CardHeader>
							<CardContent>
								<div className="grid gap-4 md:grid-cols-2">
									<div>
										<h4 className="font-medium mb-2">Project Information</h4>
										<div className="space-y-2 text-sm">
											<div className="flex justify-between">
												<span className="text-muted-foreground">Created:</span>
												<span>
													{new Date(
														project.createdAt || ""
													).toLocaleDateString()}
												</span>
											</div>
											<div className="flex justify-between">
												<span className="text-muted-foreground">Members:</span>
												<span>{project.memberCount}</span>
											</div>
											{project.isUserAdmin && (
												<div className="flex justify-between">
													<span className="text-muted-foreground">
														Pending Members:
													</span>
													<span>{project.pendingRequestCount || 0}</span>
												</div>
											)}
											<div className="flex justify-between items-center">
												<span className="text-muted-foreground">
													Project URL:
												</span>
												<div className="flex items-center gap-2">
													<code className="text-xs bg-muted px-2 py-1 rounded">
														/projects/{project.projectSlug}
													</code>
													<Button
														variant="ghost"
														size="sm"
														onClick={handleCopyProjectLink}
														disabled={!canAccessProjectFeatures()}
														aria-label={
															canAccessProjectFeatures()
																? "Copy project link"
																: getDisabledButtonTooltip()
														}
													>
														<Copy className="h-3 w-3" />
													</Button>
												</div>
											</div>
										</div>
									</div>
									<div>
										<h4 className="font-medium mb-2">Your Role</h4>
										<div className="space-y-2 text-sm">
											{project.userRole ? (
												<>
													<div className="flex justify-between">
														<span className="text-muted-foreground">Role:</span>
														<ProjectRoleBadge
															role={project.userRole}
															size="sm"
														/>
													</div>
													{project.userMembershipStatus && (
														<div className="flex justify-between">
															<span className="text-muted-foreground">
																Status:
															</span>
															<MemberStatusBadge
																status={project.userMembershipStatus}
																size="sm"
															/>
														</div>
													)}
												</>
											) : (
												<div className="text-muted-foreground">
													Not a member of this project
												</div>
											)}
										</div>
									</div>
								</div>
							</CardContent>
						</Card>

						{/* Similarity Configuration Panel - Admin Only */}
						{project.isUserAdmin && (
							<Card id="similarity-config-panel">
								<CardHeader>
									<CardTitle className="text-lg">
										Duplicate Detection Settings
									</CardTitle>
									<CardDescription>
										Configure similarity algorithms for automatic duplicate bug
										detection
									</CardDescription>
								</CardHeader>
								<CardContent>
									<SimilarityConfigPanel
										projectSlug={project.projectSlug}
										onConfigurationChange={() => {
											toast.success(
												"Similarity configuration updated successfully!"
											);
										}}
									/>
								</CardContent>
							</Card>
						)}
					</div>

					{/* Sidebar */}
					<div className="space-y-6">
						{/* Quick Actions */}
						<Card>
							<CardHeader>
								<CardTitle className="text-lg">Quick Actions</CardTitle>
							</CardHeader>
							<CardContent className="space-y-2">
								<Button
									variant="outline"
									className="w-full justify-start"
									onClick={() =>
										navigate(`/projects/${project.projectSlug}/bugs`)
									}
									disabled={!canAccessProjectFeatures()}
									aria-label={
										canAccessProjectFeatures()
											? "View project bugs"
											: getDisabledButtonTooltip()
									}
								>
									<Bug className="h-4 w-4 mr-2" />
									View Bugs
								</Button>
								<Button
									variant="outline"
									className="w-full justify-start"
									onClick={() =>
										navigate(`/projects/${project.projectSlug}/analytics`)
									}
									disabled={!canAccessProjectFeatures()}
									aria-label={
										canAccessProjectFeatures()
											? "View project analytics"
											: getDisabledButtonTooltip()
									}
								>
									<BarChart3 className="h-4 w-4 mr-2" />
									Analytics
								</Button>
								<Button
									variant="outline"
									className="w-full justify-start"
									onClick={handleManageMembers}
									disabled={!canAccessProjectFeatures()}
									aria-label={
										canAccessProjectFeatures()
											? "View project members"
											: getDisabledButtonTooltip()
									}
								>
									<Users className="h-4 w-4 mr-2" />
									View Members
								</Button>

								<Button
									variant="outline"
									className="w-full justify-start"
									onClick={() =>
										navigate(`/projects/${project.projectSlug}/teams`)
									}
									disabled={!canAccessProjectFeatures()}
									aria-label={
										canAccessProjectFeatures()
											? "View project teams"
											: getDisabledButtonTooltip()
									}
								>
									<FolderOpen className="h-4 w-4 mr-2" />
									View Teams
								</Button>
								<Button
									variant="outline"
									className="w-full justify-start"
									onClick={() => navigate("/leaderboard")}
									disabled={!canAccessProjectFeatures()}
									aria-label={
										canAccessProjectFeatures()
											? "View leaderboard"
											: getDisabledButtonTooltip()
									}
								>
									<BarChart3 className="h-4 w-4 mr-2" />
									View Leaderboard
								</Button>
								{project.isUserAdmin && (
									<Button
										variant="outline"
										className="w-full justify-start"
										onClick={() => setIsPendingRequestsOpen(true)}
									>
										<UserPlus className="h-4 w-4 mr-2" />
										View Pending Requests ({project.pendingRequestCount || 0})
									</Button>
								)}

								{/* Configure Duplicate Detection - Admin Only */}
								{project.isUserAdmin && (
									<Button
										variant="outline"
										className="w-full justify-start"
										onClick={() => {
											document
												.getElementById("similarity-config-panel")
												?.scrollIntoView({
													behavior: "smooth",
												});
										}}
									>
										<Search className="h-4 w-4 mr-2" />
										Configure Duplicate Detection
									</Button>
								)}

								{/* Edit Project - Admin Only */}
								{project.isUserAdmin && (
									<Button
										variant="outline"
										className="w-full justify-start"
										onClick={handleEdit}
									>
										<Settings className="h-4 w-4 mr-2" />
										Edit Project
									</Button>
								)}

								{/* Leave Project - For all members except admin */}
								{project.userRole && !project.isUserAdmin && (
									<Button
										variant="outline"
										className="w-full justify-start text-orange-600 hover:text-orange-700"
										onClick={handleLeaveProject}
									>
										<LogOut className="h-4 w-4 mr-2" />
										Leave Project
									</Button>
								)}
								{/* Delete Project - For admin only */}
								{project.isUserAdmin && (
									<Button
										variant="outline"
										className="w-full justify-start text-destructive hover:text-destructive"
										onClick={handleDeleteProject}
									>
										<Trash2 className="h-4 w-4 mr-2" />
										Delete Project
									</Button>
								)}
							</CardContent>
						</Card>
					</div>
				</div>
			</main>

			<Footer />

			{/* Pending Requests Modal */}
			{project && isPendingRequestsOpen && (
				<PendingRequestsModal
					isOpen={isPendingRequestsOpen}
					onClose={() => setIsPendingRequestsOpen(false)}
					projectSlug={project.projectSlug}
					projectName={project.name}
					onRequestsUpdated={handlePendingRequestsUpdated}
				/>
			)}

			{/* Create Team Modal - Removed for now */}
			{/* <CreateTeamModal
				isOpen={isCreateModalOpen}
				onClose={closeCreateModal}
				onSubmit={handleCreateTeamSubmit}
				isLoading={isCreating}
			/> */}
		</div>
	);
}
