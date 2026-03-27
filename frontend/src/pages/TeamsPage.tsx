/**
 * TeamsPage Component
 *
 * Teams management page that requires project selection first, following the same pattern as BugsPage.
 * Teams are project-scoped and cannot be viewed without selecting a project.
 *
 * Key Features:
 * - Project selection dropdown (required before any team operations)
 * - Teams list with search and filtering (only after project selection)
 * - Create team functionality (enabled after project selection)
 * - URL state management for selected project
 * - Consistent UX with bugs module
 */

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
	Users,
	Plus,
	Search,
	SortAsc,
	AlertCircle,
	FolderOpen,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { ProjectPicker } from "@/components/ui/project-picker";
import { Label } from "@/components/ui/label";
import { toast } from "react-toastify";

import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { LegacyBreadcrumb } from "@/components/ui/breadcrumb";
import { teamService } from "@/services/teamService";
import { projectService } from "@/services/projectService";
import { TeamsTable } from "@/components/teams/TeamsTable";
import { CreateTeamModal, useCreateTeamModal } from "@/components/teams";
import { TeamEmptyState } from "@/components/teams/TeamEmptyState";
import { useDebounce } from "@/hooks/useDebounce";
import { useSearchParams } from "@/hooks/useSearchParams";
import type { Team } from "@/types/team";
import type { Project } from "@/types/project";
import { ProjectRole } from "@/types/project";

// Sorting options (copied from ProjectTeamsPage)
const SORT_OPTIONS = {
	name_asc: "Name (A-Z)",
	name_desc: "Name (Z-A)",
	created_asc: "Created (Oldest)",
	created_desc: "Created (Newest)",
	members_asc: "Members (Low-High)",
	members_desc: "Members (High-Low)",
} as const;

export function TeamsPage() {
	const navigate = useNavigate();

	// URL state management
	const {
		params: urlParams,
		updateParam,
		clearParams,
	} = useSearchParams({
		search: "",
		sortBy: "name_asc",
		projectSlug: undefined,
		viewMode: "all", // "all" or "my"
	});

	// Local state
	const [teams, setTeams] = useState<Team[]>([]);
	const [projects, setProjects] = useState<Project[]>([]);
	const [selectedProjectSlug, setSelectedProjectSlug] = useState<string>("");
	const [selectedProjectId, setSelectedProjectId] = useState<string>("");
	const [loading, setLoading] = useState(true);
	const [projectsLoading, setProjectsLoading] = useState(true);
	const [teamsLoading, setTeamsLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	// Extract values from URL params
	const searchTerm = urlParams.search || "";
	const sortBy = (urlParams.sortBy as keyof typeof SORT_OPTIONS) || "name_asc";
	const projectSlug = urlParams.projectSlug;
	const viewMode = urlParams.viewMode || "all";

	// Debounced search term for API calls
	const debouncedSearchTerm = useDebounce(searchTerm, 300);

	// Create team modal
	const {
		isOpen: isCreateModalOpen,
		openModal: openCreateModal,
		closeModal: closeCreateModal,
	} = useCreateTeamModal();
	const [isCreating, setIsCreating] = useState(false);

	// Load projects first
	useEffect(() => {
		loadProjects();
	}, []);

	// Auto-select project if accessed via project-specific route
	useEffect(() => {
		if (projectSlug && projects.length > 0) {
			const project = projects.find((p) => p.projectSlug === projectSlug);
			if (project) {
				setSelectedProjectSlug(projectSlug);
				setSelectedProjectId(project.id);
			}
		}
	}, [projectSlug, projects]);

	// Load teams when project is selected or filters change
	useEffect(() => {
		if (selectedProjectId) {
			loadTeams();
		} else {
			setTeams([]);
		}
	}, [selectedProjectId, debouncedSearchTerm, sortBy, viewMode]);

	// Update URL when filters change
	useEffect(() => {
		updateParam("search", searchTerm || undefined);
		updateParam("sortBy", sortBy !== "name_asc" ? sortBy : undefined);
		updateParam("projectSlug", selectedProjectSlug || undefined);
		updateParam("viewMode", viewMode !== "all" ? viewMode : undefined);
	}, [searchTerm, sortBy, selectedProjectSlug, viewMode, updateParam]);

	const loadProjects = async () => {
		try {
			setProjectsLoading(true);
			const userProjects = await projectService.getUserProjects();
			// Filter to only show projects where user is member or admin
			const accessibleProjects = userProjects.filter(
				(project) =>
					project.userRole === ProjectRole.ADMIN ||
					project.userRole === ProjectRole.MEMBER
			);
			setProjects(accessibleProjects);
		} catch (error) {
			console.error("Failed to load projects:", error);
			// Show the actual error message from the backend
			const errorMessage =
				error instanceof Error ? error.message : "Failed to load projects";
			toast.error(errorMessage);
		} finally {
			setProjectsLoading(false);
		}
	};

	const loadTeams = async () => {
		if (!selectedProjectId || !selectedProjectSlug) {
			setTeams([]);
			return;
		}

		setTeamsLoading(true);
		setError(null);

		try {
			// Get teams for the selected project
			const teamsResponse = await teamService.getProjectTeams(
				selectedProjectSlug
			);

			// Defensive programming: ensure we have an array
			if (!teamsResponse || !Array.isArray(teamsResponse)) {
				console.warn("getProjectTeams returned non-array:", teamsResponse);
				setTeams([]);
				return;
			}

			let filteredTeams = [...teamsResponse]; // Create a copy to avoid mutating original

			// Apply filter for "my teams" vs "all teams"
			if (viewMode === "my") {
				// Filter to show only teams where current user is a member
				filteredTeams = filteredTeams.filter((team) => team.currentUserRole);
			}

			// Apply sorting
			filteredTeams.sort((a, b) => {
				const [sortField, sortDirection] = sortBy.split("_");

				let aValue: any, bValue: any;

				switch (sortField) {
					case "name":
						aValue = a.name.toLowerCase();
						bValue = b.name.toLowerCase();
						break;
					case "created":
						aValue = new Date(a.createdAt || "").getTime();
						bValue = new Date(b.createdAt || "").getTime();
						break;
					case "members":
						aValue = a.memberCount || 0;
						bValue = b.memberCount || 0;
						break;
					default:
						aValue = a.name.toLowerCase();
						bValue = b.name.toLowerCase();
				}

				if (sortDirection === "asc") {
					return aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
				} else {
					return aValue > bValue ? -1 : aValue < bValue ? 1 : 0;
				}
			});

			// Apply search filter
			if (debouncedSearchTerm) {
				filteredTeams = filteredTeams.filter(
					(team) =>
						team.name
							.toLowerCase()
							.includes(debouncedSearchTerm.toLowerCase()) ||
						(team.description &&
							team.description
								.toLowerCase()
								.includes(debouncedSearchTerm.toLowerCase()))
				);
			}

			setTeams(filteredTeams);
		} catch (error) {
			console.error("Failed to load teams:", error);
			setError(error instanceof Error ? error.message : "Failed to load teams");
		} finally {
			setTeamsLoading(false);
		}
	};

	// Handle project selection change
	const handleProjectChange = (slug: string) => {
		setSelectedProjectSlug(slug);
		const project = projects.find((p) => p.projectSlug === slug);
		setSelectedProjectId(project?.id || "");
	};

	// Handle search change
	const handleSearchChange = (value: string) => {
		// Update the search term which will trigger the debounced effect
		updateParam("search", value || undefined);
	};

	// Handle team creation
	const handleCreateTeam = async (data: any) => {
		if (!selectedProjectSlug) return;

		try {
			setIsCreating(true);

			// Create the team using the project service
			const newTeam = await projectService.createProjectTeam(
				selectedProjectSlug,
				data
			);

			// Show success message
			toast.success(`Team "${newTeam.name}" created successfully!`);

			// Close the modal
			closeCreateModal();

			// Refresh the teams list
			await loadTeams();
		} catch (error) {
			console.error("Failed to create team:", error);
			// Show the actual error message from the backend
			const errorMessage =
				error instanceof Error
					? error.message
					: "Failed to create team. Please try again.";
			toast.error(errorMessage);
		} finally {
			setIsCreating(false);
		}
	};

	// Handle team view
	const handleViewTeam = (teamSlug: string) => {
		if (selectedProjectSlug) {
			navigate(`/projects/${selectedProjectSlug}/teams/${teamSlug}`);
		}
	};

	// Handle team join
	const handleJoinTeam = async (teamId: string) => {
		if (!selectedProjectSlug) return;

		try {
			// Find the team to get its slug
			const team = teams.find((t) => t.id === teamId);
			if (!team) return;

			await teamService.joinTeam(selectedProjectSlug, team.teamSlug);
			toast.success(`Successfully joined ${team.name}`);

			// Refresh teams list to update membership status
			await loadTeams();
		} catch (error) {
			console.error("Failed to join team:", error);
			const errorMessage =
				error instanceof Error ? error.message : "Failed to join team";
			toast.error(errorMessage);
		}
	};

	// Handle team leave
	const handleLeaveTeam = async (teamId: string) => {
		if (!selectedProjectSlug) return;

		try {
			// Find the team to get its slug
			const team = teams.find((t) => t.id === teamId);
			if (!team) return;

			await teamService.leaveTeam(selectedProjectSlug, team.teamSlug);
			toast.success(`Successfully left ${team.name}`);

			// Refresh teams list to update membership status
			await loadTeams();
		} catch (error) {
			console.error("Failed to leave team:", error);
			const errorMessage =
				error instanceof Error ? error.message : "Failed to leave team";
			toast.error(errorMessage);
		}
	};

	// Handle team edit
	const handleEditTeam = (teamId: string) => {
		if (!selectedProjectSlug) return;

		const team = teams.find((t) => t.id === teamId);
		if (team) {
			navigate(`/projects/${selectedProjectSlug}/teams/${team.teamSlug}/edit`);
		}
	};

	// Handle team delete
	const handleDeleteTeam = async (teamId: string) => {
		if (!selectedProjectSlug) return;

		try {
			const team = teams.find((t) => t.id === teamId);
			if (!team) return;

			if (
				confirm(
					`Are you sure you want to delete "${team.name}"? This action cannot be undone.`
				)
			) {
				await teamService.deleteTeam(selectedProjectSlug, team.teamSlug);
				toast.success(`Team "${team.name}" deleted successfully`);

				// Refresh teams list
				await loadTeams();
			}
		} catch (error) {
			console.error("Failed to delete team:", error);
			const errorMessage =
				error instanceof Error ? error.message : "Failed to delete team";
			toast.error(errorMessage);
		}
	};

	// Check if there are active filters
	const hasActiveFilters = useMemo(() => {
		return Boolean(searchTerm || sortBy !== "name_asc" || viewMode !== "all");
	}, [searchTerm, sortBy, viewMode]);

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />

			<main className="flex-1 container mx-auto px-4 py-4">
				{/* Breadcrumb */}
				<LegacyBreadcrumb
					items={[
						{
							label: "Home",
							href: "/",
						},
						...(selectedProjectId
							? [
									{
										label: "Projects",
										href: "/projects",
									},
									{
										label:
											projects.find((p) => p.id === selectedProjectId)?.name ||
											"Project",
										href: `/projects/${selectedProjectSlug}`,
									},
							  ]
							: []),
						{
							label: "Teams",
							current: true,
						},
					]}
					showBackButton={true}
					backButtonText={
						selectedProjectId ? "Back to Project" : "Back to Home"
					}
					backButtonHref={
						selectedProjectId ? `/projects/${selectedProjectSlug}` : "/"
					}
				/>
				{/* Header */}
				<div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
					<div>
						<h1 className="text-3xl font-bold text-gray-900">
							{selectedProjectId
								? `Teams - ${
										projects.find((p) => p.id === selectedProjectId)?.name
								  }`
								: "Teams"}
						</h1>
						<p className="text-gray-600 mt-1">
							{selectedProjectId
								? `Manage teams for ${
										projects.find((p) => p.id === selectedProjectId)?.name
								  }`
								: "Select a project to view and manage its teams"}
						</p>
					</div>
					<div className="flex items-center gap-3">
						{selectedProjectId &&
							(() => {
								const selectedProject = projects.find(
									(p) => p.id === selectedProjectId
								);
								const canCreateTeam =
									selectedProject?.userRole === ProjectRole.ADMIN;

								return canCreateTeam ? (
									<Button
										onClick={openCreateModal}
										disabled={projectsLoading}
										title="Create a new team"
									>
										<Plus className="mr-2 h-4 w-4" />
										Create Team
									</Button>
								) : (
									<div className="text-sm text-muted-foreground px-3 py-2 bg-muted rounded-md">
										Only project admins can create teams
									</div>
								);
							})()}
					</div>
				</div>

				{/* Project Selection and Filters */}
				<Card className="mb-6">
					<CardContent className="p-6">
						{/* Project Selection */}
						<div className="mb-6">
							<label className="text-sm font-medium text-gray-700 mb-2 block">
								Project
							</label>
							<ProjectPicker
								projects={projects}
								selectedProjectSlug={selectedProjectSlug}
								onProjectSelect={handleProjectChange}
								placeholder="Select a project"
								disabled={projectsLoading}
								className="w-full max-w-md"
							/>
						</div>

						{/* Search and Filters */}
						{selectedProjectId && (
							<div className="space-y-4">
								{/* Search */}
								<div className="relative">
									<Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
									<Input
										placeholder="Search teams by name or description..."
										value={searchTerm}
										onChange={(e) => handleSearchChange(e.target.value)}
										className="pl-10"
									/>
								</div>

								{/* Filter Controls */}
								<div className="grid grid-cols-1 md:grid-cols-2 gap-4">
									{/* Sort By */}
									<div>
										<label className="text-sm font-medium text-gray-700 mb-1 block">
											Sort By
										</label>
										<Select
											value={sortBy}
											onValueChange={(value) => updateParam("sortBy", value)}
										>
											<SelectTrigger>
												<SelectValue />
											</SelectTrigger>
											<SelectContent>
												{Object.entries(SORT_OPTIONS).map(([value, label]) => (
													<SelectItem key={value} value={value}>
														{label}
													</SelectItem>
												))}
											</SelectContent>
										</Select>
									</div>

									{/* View Mode */}
									<div>
										<label className="text-sm font-medium text-gray-700 mb-1 block">
											View
										</label>
										<Select
											value={viewMode}
											onValueChange={(value) => updateParam("viewMode", value)}
										>
											<SelectTrigger>
												<SelectValue />
											</SelectTrigger>
											<SelectContent>
												<SelectItem value="all">All Teams</SelectItem>
												<SelectItem value="my">My Teams</SelectItem>
											</SelectContent>
										</Select>
									</div>
								</div>

								{/* Clear Filters */}
								{hasActiveFilters && (
									<div className="flex justify-end">
										<Button
											variant="outline"
											onClick={() => {
												clearParams();
												setSelectedProjectSlug("");
												setSelectedProjectId("");
											}}
										>
											Clear All Filters
										</Button>
									</div>
								)}
							</div>
						)}
					</CardContent>
				</Card>

				{/* Teams List */}
				{!selectedProjectId ? (
					<Card>
						<CardContent className="text-center py-12">
							<Users className="h-16 w-16 mx-auto mb-4 text-muted-foreground opacity-50" />
							<h3 className="text-lg font-semibold mb-2">
								No Project Selected
							</h3>
							<p className="text-muted-foreground mb-4">
								Please select a project above to view and manage its teams.
							</p>
						</CardContent>
					</Card>
				) : (
					<TeamsTable
						teams={teams}
						onJoin={handleJoinTeam}
						onLeave={handleLeaveTeam}
						onEdit={handleEditTeam}
						onDelete={handleDeleteTeam}
						onViewDetails={handleViewTeam}
						loading={teamsLoading}
						disabled={false}
					/>
				)}

				{/* Error Display */}
				{error && (
					<Card className="mt-6">
						<CardContent className="pt-6">
							<div className="text-center text-muted-foreground">
								<AlertCircle className="h-12 w-12 mx-auto mb-4" />
								<p>{error}</p>
								<Button onClick={loadTeams} className="mt-4">
									Try Again
								</Button>
							</div>
						</CardContent>
					</Card>
				)}
			</main>

			{/* Create Team Modal */}
			<CreateTeamModal
				isOpen={isCreateModalOpen}
				onClose={closeCreateModal}
				onSubmit={handleCreateTeam}
				isLoading={isCreating}
			/>

			<Footer />
		</div>
	);
}
