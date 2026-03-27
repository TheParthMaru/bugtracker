/**
 * ProjectTeamsPage Component
 *
 * Displays all teams in a specific project.
 * This page shows teams with their basic information and allows navigation to team details.
 *
 * Features:
 * - List all teams in the project
 * - Search and filter teams
 * - Navigate to team details
 * - Back navigation to project
 */

import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Users, Plus, FolderPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { projectService } from "@/services/projectService";
import { teamService } from "@/services/teamService";
import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";
import {
	CreateProjectTeamModal,
	useCreateProjectTeamModal,
} from "@/components/projects";
import { TeamsTable } from "@/components/teams/TeamsTable";
import type { Project } from "@/types/project";
import type { Team } from "@/types/team";
import { toast } from "react-toastify";

export function ProjectTeamsPage() {
	const { projectSlug } = useParams<{ projectSlug: string }>();
	const navigate = useNavigate();
	const [project, setProject] = useState<Project | null>(null);
	const [teams, setTeams] = useState<Team[]>([]);
	const [loading, setLoading] = useState(true);
	const [teamsLoading, setTeamsLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [searchTerm, setSearchTerm] = useState("");
	const [currentFilter, setCurrentFilter] = useState<"all-teams" | "my-teams">(
		"all-teams"
	);
	const [sortBy, setSortBy] = useState<string>("name_asc");

	// Create team modal
	const {
		isOpen: isCreateModalOpen,
		openModal: openCreateModal,
		closeModal: closeCreateModal,
	} = useCreateProjectTeamModal();
	const [isCreating, setIsCreating] = useState(false);

	useEffect(() => {
		const loadProjectAndTeams = async () => {
			if (!projectSlug) {
				setError("Project slug is required");
				setLoading(false);
				return;
			}

			try {
				setLoading(true);
				setError(null);

				// Load project details
				const projectData = await projectService.getProjectBySlug(projectSlug);
				setProject(projectData);

				// Load teams
				await loadTeams();
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

		loadProjectAndTeams();
	}, [projectSlug]);

	const loadTeams = async () => {
		if (!projectSlug) return;

		try {
			setTeamsLoading(true);

			// Use teamService instead of projectService to avoid caching issues
			const teamsResponse = await teamService.getProjectTeams(projectSlug);

			let filteredTeams = teamsResponse;

			// Apply filter
			if (currentFilter === "my-teams") {
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

			setTeams(filteredTeams);
		} catch (err) {
			console.error("Failed to load teams:", err);
			// Show the actual error message from the backend
			const errorMessage =
				err instanceof Error ? err.message : "Failed to load project teams";
			toast.error(errorMessage);
		} finally {
			setTeamsLoading(false);
		}
	};

	// Handle team creation
	const handleCreateTeam = async (data: any) => {
		if (!projectSlug) return;

		try {
			setIsCreating(true);

			// Create the team using the project service
			const newTeam = await projectService.createProjectTeam(projectSlug, data);

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

	// Debounced search effect
	useEffect(() => {
		const timeoutId = setTimeout(() => {
			loadTeams();
		}, 300);

		return () => clearTimeout(timeoutId);
	}, [searchTerm, projectSlug, currentFilter, sortBy]);

	const handleBack = () => {
		navigate(`/projects/${projectSlug}`);
	};

	const handleViewTeam = (teamSlug: string) => {
		navigate(`/projects/${projectSlug}/teams/${teamSlug}`);
	};

	// Handle team join
	const handleJoinTeam = async (teamId: string) => {
		if (!projectSlug) return;

		try {
			// Find the team to get its slug
			const team = teams.find((t) => t.id === teamId);
			if (!team) return;

			await teamService.joinTeam(projectSlug, team.teamSlug);
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
		if (!projectSlug) return;

		try {
			// Find the team to get its slug
			const team = teams.find((t) => t.id === teamId);
			if (!team) return;

			await teamService.leaveTeam(projectSlug, team.teamSlug);
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
		if (!projectSlug) return;

		const team = teams.find((t) => t.id === teamId);
		if (team) {
			navigate(`/projects/${projectSlug}/teams/${team.teamSlug}/edit`);
		}
	};

	// Handle team delete
	const handleDeleteTeam = async (teamId: string) => {
		if (!projectSlug) return;

		try {
			const team = teams.find((t) => t.id === teamId);
			if (!team) return;

			if (
				confirm(
					`Are you sure you want to delete "${team.name}"? This action cannot be undone.`
				)
			) {
				await teamService.deleteTeam(projectSlug, team.teamSlug);
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

	if (loading) {
		return (
			<div className="min-h-screen bg-background">
				<Navbar />
				<main className="container mx-auto px-4 py-8">
					<div className="text-center">
						<div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary mx-auto"></div>
						<p className="mt-4 text-muted-foreground">
							Loading project teams...
						</p>
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	if (error || !project) {
		return (
			<div className="min-h-screen bg-background">
				<Navbar />
				<main className="container mx-auto px-4 py-8">
					<div className="text-center">
						<h1 className="text-2xl font-bold text-destructive mb-4">
							{error || "Project not found"}
						</h1>
						<Button onClick={handleBack} variant="outline">
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Projects
						</Button>
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	return (
		<div className="min-h-screen bg-background">
			<Navbar />
			<main className="container mx-auto px-4 py-8">
				{/* Breadcrumb */}
				<ProjectBreadcrumb
					projectName={project.name}
					projectSlug={project.projectSlug}
					section="Teams"
					sectionHref={`/projects/${project.projectSlug}/teams`}
					onBackClick={handleBack}
				/>

				{/* Header */}
				<div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
					<div>
						<h1 className="text-3xl font-bold">Teams in {project.name}</h1>
						<p className="text-muted-foreground mt-1">
							{currentFilter === "my-teams"
								? "Teams you're a member of"
								: "Manage and view all teams in this project"}
						</p>
					</div>
					{project.isUserAdmin && (
						<Button onClick={openCreateModal} disabled={isCreating}>
							<FolderPlus className="h-4 w-4 mr-2" />
							{isCreating ? "Creating..." : "Create Team"}
						</Button>
					)}
				</div>

				{/* Teams List */}
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
			</main>

			{/* Create Team Modal */}
			<CreateProjectTeamModal
				isOpen={isCreateModalOpen}
				onClose={closeCreateModal}
				onSubmit={handleCreateTeam}
				projectSlug={projectSlug!}
				projectName={project?.name || ""}
				isLoading={isCreating}
			/>

			<Footer />
		</div>
	);
}
