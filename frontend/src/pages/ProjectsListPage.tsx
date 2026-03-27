/**
 * ProjectsListPage Component
 *
 * Main projects listing page with comprehensive search and filtering functionality.
 * Features include real-time search, sorting, URL state management, and optimized
 * performance for large project lists.
 *
 * Key Features:
 * - Real-time search with 300ms debouncing
 * - Sorting options (name, date, member count)
 * - URL state management for bookmarkable searches
 * - Project creation and management
 * - Empty states and loading states
 * - Performance optimization
 * - Accessibility support
 */

import { useState, useEffect, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { FolderPlus, SortAsc, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

import { Card, CardContent } from "@/components/ui/card";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { ProjectPicker } from "@/components/ui/project-picker";
import { Badge } from "@/components/ui/badge";
import { toast } from "react-toastify";
import { ProjectsListBreadcrumb } from "@/components/ui/breadcrumb";

import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { projectService } from "@/services/projectService";
import {
	ProjectsTable,
	CreateProjectModal,
	useCreateProjectModal,
} from "@/components/projects";

import { useSearchParams } from "@/hooks/useSearchParams";
import type {
	Project,
	ProjectSearchParams,
	CreateProjectRequest,
} from "@/types/project";

// Sorting options
const SORT_OPTIONS = {
	name_asc: "Name (A-Z)",
	name_desc: "Name (Z-A)",
	created_desc: "Newest First",
	created_asc: "Oldest First",
} as const;

export function ProjectsListPage() {
	const navigate = useNavigate();

	// URL state management
	const {
		params: urlParams,
		updateParam,
		clearParams,
	} = useSearchParams({
		search: "",
		sortBy: "name_asc",
		page: "0",
		create: undefined,
		filter: undefined,
	});

	// Local state
	const [projects, setProjects] = useState<Project[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	// Extract values from URL params
	const sortBy = (urlParams.sortBy as keyof typeof SORT_OPTIONS) || "name_asc";
	const currentPage = parseInt(urlParams.page || "0", 10);
	const createParam = urlParams.create;
	const filterParam = urlParams.filter;

	// Local filter state for immediate updates
	const [currentFilter, setCurrentFilter] = useState<string | undefined>(
		filterParam || "my-projects" // Default to "my-projects" if no filter specified
	);

	// Pagination state
	const [pagination, setPagination] = useState({
		page: currentPage,
		size: 10,
		totalElements: 0,
		totalPages: 0,
		hasNext: false,
		hasPrevious: false,
	});

	// Quick jump to project state
	const [selectedProjectSlug, setSelectedProjectSlug] = useState<string>("");

	// Create project modal
	const {
		isOpen: isCreateModalOpen,
		openModal: openCreateModal,
		closeModal: closeCreateModal,
	} = useCreateProjectModal();
	const [isCreating, setIsCreating] = useState(false);

	// Check if there are active filters
	const hasActiveFilters = useMemo(() => {
		return sortBy !== "name_asc";
	}, [sortBy]);

	// Load projects
	const loadProjects = useCallback(
		async (
			page?: number,
			sort?: string,
			bypassCache: boolean = false,
			filterOverride?: string | undefined
		) => {
			try {
				setLoading(true);
				setError(null);

				// Use provided parameters or current URL values
				const currentPageValue = page ?? currentPage;
				const currentSortValue = sort ?? sortBy;
				// Use filter override if provided, otherwise use current state
				const currentFilterValue =
					filterOverride !== undefined ? filterOverride : currentFilter;

				// Parse sort parameter and convert to Spring Data format
				const [sortField, sortDirection] = currentSortValue.split("_");

				// Map frontend sort fields to backend field names
				const fieldMapping: Record<string, string> = {
					name: "name",
					created: "createdAt",
				};

				let response;

				// Handle different data sources based on filter

				if (currentFilterValue === "my-projects") {
					const userProjects = await projectService.getUserProjects(
						bypassCache
					);

					// No search filtering needed
					let filteredProjects = userProjects;

					// Apply sorting locally
					filteredProjects.sort((a, b) => {
						const aValue = sortField === "name" ? a.name : a.createdAt || "";
						const bValue = sortField === "name" ? b.name : b.createdAt || "";

						if (sortDirection === "asc") {
							return aValue.localeCompare(bValue);
						} else {
							return bValue.localeCompare(aValue);
						}
					});

					// Apply pagination locally
					const pageSize = pagination.size || 10; // Ensure we have a valid page size
					const startIndex = currentPageValue * pageSize;
					const endIndex = startIndex + pageSize;
					const paginatedProjects = filteredProjects.slice(
						startIndex,
						endIndex
					);

					response = {
						content: paginatedProjects,
						number: currentPageValue,
						size: pageSize,
						totalElements: filteredProjects.length,
						totalPages: Math.ceil(filteredProjects.length / pageSize),
						last: endIndex >= filteredProjects.length,
						first: currentPageValue === 0,
						numberOfElements: paginatedProjects.length,
						empty: paginatedProjects.length === 0,
					};
				} else {
					// Load all projects (existing behavior)
					const params: ProjectSearchParams = {
						page: currentPageValue,
						size: pagination.size,

						sort: `${fieldMapping[sortField] || sortField},${sortDirection}`,
					};

					response = await projectService.getProjects(params, bypassCache);
				}

				setProjects(response.content);

				const newPagination = {
					page: response.page?.number || 0,
					size: response.page?.size || pagination.size, // Fallback to current size if undefined
					totalElements:
						response.page?.totalElements || response.content.length,
					totalPages: response.page?.totalPages || 1,
					hasNext: response.page ? !response.page.last : false,
					hasPrevious: response.page ? response.page.number > 0 : false,
				};

				setPagination(newPagination);
			} catch (error) {
				console.error("Failed to load projects:", error);
				const errorMessage =
					error instanceof Error ? error.message : "Failed to load projects";
				setError(errorMessage);
				toast.error(errorMessage);
			} finally {
				setLoading(false);
			}
		},
		[currentPage, sortBy, pagination.size, currentFilter]
	);

	// Sync local filter state with URL params
	useEffect(() => {
		setCurrentFilter(filterParam);
	}, [filterParam]);

	// Debug: Monitor projects state changes
	useEffect(() => {
		console.log("Projects state changed:", projects);
		console.log("Projects length:", projects.length);
	}, [projects]);

	// Set default filter to "my-projects" if none specified
	useEffect(() => {
		if (!filterParam) {
			console.log(
				"No filter parameter found, setting default to 'my-projects'"
			);
			updateParam("filter", "my-projects");
			setCurrentFilter("my-projects");
		}
	}, [filterParam, updateParam]);

	// Reload projects when URL parameters change
	useEffect(() => {
		console.log("useEffect triggered - filter:", currentFilter);
		loadProjects(currentPage, sortBy, true); // Force bypass cache
	}, [currentPage, sortBy, currentFilter]);

	// Refresh projects when user returns to the page (focus event)
	useEffect(() => {
		const handleFocus = () => {
			// Refresh with cache bypass when user returns to the page
			loadProjects(currentPage, sortBy, true);
		};

		window.addEventListener("focus", handleFocus);
		return () => {
			window.removeEventListener("focus", handleFocus);
		};
	}, [currentPage, sortBy, currentFilter, loadProjects]);

	// Auto-open create modal if query param present
	useEffect(() => {
		if (createParam === "1" || createParam === "true") {
			openCreateModal();
			updateParam("create", undefined);
		}
	}, [createParam, openCreateModal, updateParam]);

	// Handle sort change
	const handleSortChange = useCallback(
		(value: keyof typeof SORT_OPTIONS) => {
			updateParam("sortBy", value !== "name_asc" ? value : undefined);
		},
		[updateParam]
	);

	// Handle clear all filters
	const handleClearAll = useCallback(() => {
		clearParams();
	}, [clearParams]);

	// Handle show all projects (set filter to "all")
	const handleShowAllProjects = useCallback(() => {
		updateParam("filter", "all");
		// Reset to first page and clear search when switching to all projects
		updateParam("page", "0");

		// Update local state immediately
		console.log("Setting currentFilter state to 'all'");
		setCurrentFilter("all");

		// Reset pagination state to match URL params
		console.log("Resetting pagination state");
		setPagination((prev) => ({
			...prev,
			page: 0,
			totalElements: 0,
			totalPages: 0,
			hasNext: false,
			hasPrevious: false,
		}));

		loadProjects(0, sortBy, true, "all");
	}, [updateParam, loadProjects, sortBy]);

	// Handle pagination
	const handlePageChange = useCallback(
		(newPage: number) => {
			updateParam("page", newPage.toString());
			window.scrollTo({ top: 0, behavior: "smooth" });
		},
		[updateParam, currentPage]
	);

	// Handle project creation
	const handleCreateProject = async (data: CreateProjectRequest) => {
		setIsCreating(true);
		try {
			const newProject = await projectService.createProject(data);

			// Force refresh the projects list with bypass cache to show the new project
			await loadProjects(currentPage, sortBy, true);
			closeCreateModal();

			// Show success message (don't navigate to non-existent project detail page)
			toast.success("Project created successfully!");
		} catch (error) {
			console.error("Failed to create project:", error);
			toast.error(
				error instanceof Error ? error.message : "Failed to create project"
			);
		} finally {
			setIsCreating(false);
		}
	};

	// Handle project actions
	const handleJoinProject = useCallback(
		async (projectId: string) => {
			try {
				const project = projects.find((p) => p.id === projectId);
				if (!project) return;

				await projectService.joinProject(project.projectSlug);

				// Force refresh projects with bypass cache to show updated status
				await loadProjects(currentPage, sortBy, true);

				toast.success("Join request submitted successfully!");
			} catch (error) {
				console.error("Failed to join project:", error);
				toast.error(
					error instanceof Error ? error.message : "Failed to join project"
				);
			}
		},
		[projects, currentPage, sortBy, loadProjects]
	);

	const handleLeaveProject = useCallback(
		async (projectId: string) => {
			try {
				const project = projects.find((p) => p.id === projectId);
				if (!project) return;

				await projectService.leaveProject(project.projectSlug);

				// Force refresh projects with bypass cache to show updated status
				await loadProjects(currentPage, sortBy, true);

				toast.success("Left project successfully!");
			} catch (error) {
				console.error("Failed to leave project:", error);
				toast.error(
					error instanceof Error ? error.message : "Failed to leave project"
				);
			}
		},
		[projects, currentPage, sortBy, loadProjects]
	);

	const handleViewProject = useCallback(
		(projectId: string) => {
			const project = projects.find((p) => p.id === projectId);
			if (project) {
				navigate(`/projects/${project.projectSlug}`);
			}
		},
		[projects, navigate]
	);

	const handleEditProject = useCallback(
		(projectId: string) => {
			const project = projects.find((p) => p.id === projectId);
			if (project) {
				navigate(`/projects/${project.projectSlug}/edit`);
			}
		},
		[projects, navigate]
	);

	const handleQuickJumpToProject = useCallback(
		(projectSlug: string) => {
			navigate(`/projects/${projectSlug}`);
		},
		[navigate]
	);

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />

			<main className="flex-1 container mx-auto px-4 py-4">
				{/* Breadcrumb */}
				<ProjectsListBreadcrumb />

				{/* Header */}
				<div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
					<div>
						<h1 className="text-3xl font-bold">Projects</h1>
						<p className="text-muted-foreground mt-1">
							{currentFilter === "my-projects"
								? "Your projects and memberships"
								: "Discover and join projects to collaborate on bug tracking"}
						</p>
					</div>
					<Button onClick={openCreateModal}>
						<FolderPlus className="h-4 w-4 mr-2" />
						Create Project
					</Button>
				</div>

				{/* Filter Toggle */}
				<div className="mb-4 flex items-center gap-2">
					<Button
						variant={currentFilter === "my-projects" ? "default" : "outline"}
						size="sm"
						onClick={() => {
							updateParam("filter", "my-projects");
							updateParam("page", "0");
							setCurrentFilter("my-projects");
							// Reset pagination state to match URL params
							setPagination((prev) => ({
								...prev,
								page: 0,
								totalElements: 0,
								totalPages: 0,
								hasNext: false,
								hasPrevious: false,
							}));
							// Force reload projects with the new filter
							loadProjects(0, sortBy, true, "my-projects");
						}}
						className={
							currentFilter === "my-projects"
								? "bg-primary text-primary-foreground"
								: ""
						}
					>
						My Projects
					</Button>
					<Button
						variant={currentFilter === "all" ? "default" : "outline"}
						size="sm"
						onClick={handleShowAllProjects}
						className={
							currentFilter === "all"
								? "bg-primary text-primary-foreground"
								: ""
						}
					>
						Show All
					</Button>
				</div>

				{/* Search and Filters */}
				<Card className="mb-4">
					<CardContent className="pt-4">
						<form
							onSubmit={(e) => {
								e.preventDefault();
								// Prevent form submission
							}}
							className="flex flex-col sm:flex-row gap-3"
						>
							{/* Project Selection */}
							<div className="flex-1">
								<div className="mb-2">
									<h3 className="text-sm font-medium text-foreground">
										Select Project
									</h3>
								</div>
								<ProjectPicker
									projects={projects}
									selectedProjectSlug={selectedProjectSlug}
									onProjectSelect={handleQuickJumpToProject}
									placeholder="Select a project to view..."
									disabled={loading}
									className="w-full"
								/>
							</div>

							{/* Sort */}
							<div className="sm:w-48">
								<div className="mb-2">
									<h3 className="text-sm font-medium text-foreground">
										Sort By
									</h3>
								</div>
								<Select
									value={sortBy}
									onValueChange={(value) =>
										handleSortChange(value as keyof typeof SORT_OPTIONS)
									}
								>
									<SelectTrigger>
										<SortAsc className="h-4 w-4 mr-2" />
										<SelectValue />
									</SelectTrigger>
									<SelectContent>
										{Object.entries(SORT_OPTIONS).map(([key, label]) => (
											<SelectItem key={key} value={key}>
												{label}
											</SelectItem>
										))}
									</SelectContent>
								</Select>
							</div>

							{/* View Mode Toggle */}
						</form>

						{/* Active Filters */}
						{hasActiveFilters && (
							<div className="flex items-center gap-2 mt-4">
								<span className="text-sm text-muted-foreground">Filters:</span>

								{sortBy !== "name_asc" && (
									<Badge variant="secondary">
										Sort: {SORT_OPTIONS[sortBy]}
									</Badge>
								)}
								<Button
									type="button"
									variant="ghost"
									size="sm"
									onClick={handleClearAll}
									className="text-xs"
								>
									Clear All
								</Button>
							</div>
						)}
					</CardContent>
				</Card>

				{/* Projects Display */}
				{error ? (
					<Card className="p-8 text-center">
						<AlertCircle className="h-12 w-12 mx-auto mb-4 text-destructive" />
						<h3 className="text-lg font-semibold mb-2">
							Failed to Load Projects
						</h3>
						<p className="text-muted-foreground mb-4">{error}</p>
						<Button onClick={() => loadProjects(currentPage, sortBy, true)}>
							Try Again
						</Button>
					</Card>
				) : (
					<ProjectsTable
						projects={projects}
						onJoin={handleJoinProject}
						onViewDetails={handleViewProject}
						disabled={loading}
						hasActiveFilters={hasActiveFilters}
						onClearFilters={handleClearAll}
						onCreateProject={openCreateModal}
					/>
				)}

				{/* Pagination - Always show for consistent UI */}
				{!loading && projects.length > 0 && (
					<div className="flex items-center justify-center gap-2 mt-6">
						<Button
							variant="outline"
							size="sm"
							disabled={!pagination.hasPrevious}
							onClick={() => handlePageChange(pagination.page - 1)}
						>
							Previous
						</Button>
						<div className="flex items-center gap-2">
							{Array.from({ length: Math.max(1, pagination.totalPages) }).map(
								(_, i) => {
									const pageNumber = i;
									return (
										<Button
											key={pageNumber}
											variant={
												pageNumber === pagination.page ? "default" : "outline"
											}
											size="sm"
											onClick={() => handlePageChange(pageNumber)}
										>
											{pageNumber + 1}
										</Button>
									);
								}
							)}
						</div>
						<Button
							variant="outline"
							size="sm"
							disabled={!pagination.hasNext}
							onClick={() => handlePageChange(pagination.page + 1)}
						>
							Next
						</Button>
					</div>
				)}

				{/* Results Summary */}
				{!loading && (
					<div className="text-center text-sm text-muted-foreground mt-3">
						Showing {projects.length} of{" "}
						{pagination.totalElements || projects.length} projects
						{pagination.totalPages > 1 && (
							<span className="block text-xs mt-1">
								Page {pagination.page + 1} of {pagination.totalPages}
							</span>
						)}
					</div>
				)}
			</main>

			{/* Create Project Modal */}
			<CreateProjectModal
				isOpen={isCreateModalOpen}
				onClose={closeCreateModal}
				onSubmit={handleCreateProject}
				isLoading={isCreating}
			/>
			<Footer />
		</div>
	);
}
