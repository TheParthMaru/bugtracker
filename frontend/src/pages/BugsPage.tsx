/**
 * BugsPage Component
 *
 * Main bugs listing page with project filtering and comprehensive search functionality.
 * Users can view all bugs across their projects or filter by specific projects.
 *
 * Key Features:
 * - Project-based filtering
 * - Real-time search with debouncing
 * - Advanced filtering (status, priority, type, assignee, labels)
 * - Bug creation and management
 * - Empty states and loading states
 */

import { useState, useEffect, useMemo, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Bug, Plus, Search, SortAsc, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
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
import { useDebounce } from "@/hooks/useDebounce";
import { useSearchParams } from "@/hooks/useSearchParams";

import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import {
	ProjectBreadcrumb,
	SimpleBreadcrumb,
} from "@/components/ui/breadcrumb";
import { bugService } from "@/services/bugService";
import { projectService } from "@/services/projectService";
import { userService } from "@/services/userService";
import { BugsTable } from "@/components/bugs";
import type { Bug as BugType, BugSearchParams } from "@/types/bug";
import type { Project } from "@/types/project";
import { BugStatus, BugPriority, BugType as BugTypeEnum } from "@/types/bug";

// Sorting options
const SORT_OPTIONS = {
	ticket_asc: "Ticket # (Low to High)",
	ticket_desc: "Ticket # (High to Low)",
	title_asc: "Title (A-Z)",
	title_desc: "Title (Z-A)",
	created_desc: "Newest First",
	created_asc: "Oldest First",
	priority_asc: "Priority (Low to High)",
	priority_desc: "Priority (High to Low)",
	status_asc: "Status (A-Z)",
	status_desc: "Status (Z-A)",
} as const;

export function BugsPage() {
	const navigate = useNavigate();
	const { projectSlug } = useParams<{ projectSlug?: string }>();

	// URL state management
	const {
		params: urlParams,
		updateParam,
		clearParams,
	} = useSearchParams({
		search: "",
		status: "ALL",
		priority: "ALL",
		type: "ALL",
		assignee: "ALL",
		sortBy: "ticket_asc",
		page: "0",
		projectSlug: "",
	});

	// State
	const [bugs, setBugs] = useState<BugType[]>([]);
	const [projects, setProjects] = useState<Project[]>([]);
	const [currentUserId, setCurrentUserId] = useState<string>("");
	const [loading, setLoading] = useState(true);
	const [projectsLoading, setProjectsLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	// Extract values from URL params
	const searchTerm = urlParams.search || "";
	const status = (urlParams.status as BugStatus | "ALL") || "ALL";
	const priority = (urlParams.priority as BugPriority | "ALL") || "ALL";
	const type = (urlParams.type as BugTypeEnum | "ALL") || "ALL";
	const assignee = urlParams.assignee || "ALL";
	const sortBy =
		(urlParams.sortBy as keyof typeof SORT_OPTIONS) || "ticket_asc";
	const currentPage = parseInt(urlParams.page || "0", 10);
	const selectedProjectSlug = urlParams.projectSlug || "";

	// Get selected project ID from the slug
	const selectedProject = projects.find(
		(p) => p.projectSlug === selectedProjectSlug
	);
	const selectedProjectId = selectedProject?.id || "";

	// Debounced search term for API calls
	const debouncedSearchTerm = useDebounce(searchTerm, 300);

	// Load projects and current user
	useEffect(() => {
		loadProjects();
		loadCurrentUser();
	}, []);

	const loadCurrentUser = async () => {
		try {
			const userData = await userService.getCurrentUser();
			setCurrentUserId(userData.id);
		} catch (error) {
			console.error("Failed to load current user:", error);
		}
	};

	// Auto-select project if accessed via project-specific route
	useEffect(() => {
		if (projectSlug && projects.length > 0 && !selectedProjectSlug) {
			updateParam("projectSlug", projectSlug);
		}
	}, [projectSlug, projects, selectedProjectSlug, updateParam]);

	// Load bugs when project is selected or filters change
	useEffect(() => {
		if (selectedProjectId) {
			loadBugs();
		} else {
			setBugs([]);
		}
	}, [
		selectedProjectId,
		debouncedSearchTerm,
		status,
		priority,
		type,
		assignee,
		sortBy,
		currentPage,
	]);

	// Update URL when filters change
	useEffect(() => {
		updateParam("search", searchTerm || undefined);
		updateParam("status", status !== "ALL" ? status : undefined);
		updateParam("priority", priority !== "ALL" ? priority : undefined);
		updateParam("type", type !== "ALL" ? type : undefined);
		updateParam("assignee", assignee !== "ALL" ? assignee : undefined);
		updateParam("sortBy", sortBy !== "ticket_asc" ? sortBy : undefined);
	}, [searchTerm, status, priority, type, assignee, sortBy, updateParam]);

	const loadProjects = async () => {
		try {
			setProjectsLoading(true);
			const userProjects = await projectService.getUserProjects();
			setProjects(userProjects);
			// Don't auto-select any project - let user choose
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

	const loadBugs = async () => {
		if (!selectedProjectId) {
			setBugs([]);
			return;
		}

		setLoading(true);
		setError(null);

		try {
			// Build search parameters
			const apiParams: BugSearchParams = {
				page: currentPage,
				size: 20,
				sort: getSortParam(sortBy),
			};

			if (debouncedSearchTerm) {
				apiParams.search = debouncedSearchTerm;
			}

			if (status !== "ALL") {
				apiParams.status = status;
			}

			if (priority !== "ALL") {
				apiParams.priority = priority;
			}

			if (type !== "ALL") {
				apiParams.type = type;
			}

			if (assignee !== "ALL") {
				if (assignee === "UNASSIGNED") {
					apiParams.assignee = "null";
				} else if (assignee === "ASSIGNED") {
					apiParams.assignee = "not-null";
				} else if (assignee === "ASSIGNED_TO_ME") {
					apiParams.assignee = currentUserId;
				} else {
					apiParams.assignee = assignee;
				}
			}

			const response = await bugService.getBugs(selectedProjectId, apiParams);
			console.log("Bugs loaded:", response.content);
			console.log("First bug projectSlug:", response.content[0]?.projectSlug);
			console.log("First bug projectId:", response.content[0]?.projectId);
			setBugs(response.content);
		} catch (error) {
			console.error("Failed to load bugs:", error);
			setError("Failed to load bugs. Please try again.");
			toast.error("Failed to load bugs");
		} finally {
			setLoading(false);
		}
	};

	const getSortParam = (sortBy: string): string => {
		switch (sortBy) {
			case "ticket_asc":
				return "projectTicketNumber,asc";
			case "ticket_desc":
				return "projectTicketNumber,desc";
			case "title_asc":
				return "title,asc";
			case "title_desc":
				return "title,desc";
			case "created_desc":
				return "createdAt,desc";
			case "created_asc":
				return "createdAt,asc";
			case "priority_asc":
				return "priority,asc";
			case "priority_desc":
				return "priority,desc";
			case "status_asc":
				return "status,asc";
			case "status_desc":
				return "status,desc";
			default:
				return "projectTicketNumber,asc";
		}
	};

	// Filter handlers
	const handleSearchChange = useCallback(
		(value: string) => {
			updateParam("search", value || undefined);
		},
		[updateParam]
	);

	const handleStatusChange = useCallback(
		(value: BugStatus | "ALL") => {
			updateParam("status", value !== "ALL" ? value : undefined);
		},
		[updateParam]
	);

	const handlePriorityChange = useCallback(
		(value: BugPriority | "ALL") => {
			updateParam("priority", value !== "ALL" ? value : undefined);
		},
		[updateParam]
	);

	const handleTypeChange = useCallback(
		(value: BugTypeEnum | "ALL") => {
			updateParam("type", value !== "ALL" ? value : undefined);
		},
		[updateParam]
	);

	const handleAssigneeChange = useCallback(
		(value: string | "ALL") => {
			updateParam("assignee", value !== "ALL" ? value : undefined);
		},
		[updateParam]
	);

	const handleSortChange = useCallback(
		(value: keyof typeof SORT_OPTIONS) => {
			updateParam("sortBy", value !== "ticket_asc" ? value : undefined);
		},
		[updateParam]
	);

	const handleClearAll = useCallback(() => {
		clearParams();
	}, [clearParams]);

	const handleViewBugDetails = (projectTicketNumber: number) => {
		// Find the project this bug belongs to
		const bug = bugs.find((b) => b.projectTicketNumber === projectTicketNumber);
		console.log("Bug found for navigation:", bug);
		console.log("Bug projectSlug:", bug?.projectSlug);
		console.log("Bug projectId:", bug?.projectId);
		console.log("Bug projectName:", bug?.projectName);
		console.log("All bug properties:", Object.keys(bug || {}));
		console.log("Full bug object:", JSON.stringify(bug, null, 2));

		if (bug && bug.projectSlug) {
			// Navigate to project-scoped bug detail page using project slug
			console.log(
				"Navigating to:",
				`/projects/${bug.projectSlug}/bugs/${projectTicketNumber}`
			);
			navigate(`/projects/${bug.projectSlug}/bugs/${projectTicketNumber}`);
		} else {
			// Fallback to global bug detail page (though this shouldn't happen)
			console.log("Fallback navigation to:", `/bugs/${projectTicketNumber}`);
			navigate(`/bugs/${projectTicketNumber}`);
		}
	};

	const handleNavigateToCreateBug = () => {
		if (selectedProjectSlug) {
			navigate(`/projects/${selectedProjectSlug}/bugs/create`);
		}
	};

	// Check if there are active filters
	const hasActiveFilters = useMemo(() => {
		return Boolean(
			searchTerm ||
				status !== "ALL" ||
				priority !== "ALL" ||
				type !== "ALL" ||
				assignee !== "ALL"
		);
	}, [searchTerm, status, priority, type, assignee]);

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />

			<main className="flex-1 container mx-auto px-4 py-8">
				{/* Breadcrumb */}
				{projectSlug && selectedProjectId ? (
					<ProjectBreadcrumb
						projectName={
							projects.find((p) => p.id === selectedProjectId)?.name ||
							"Loading..."
						}
						projectSlug={projectSlug}
						section="Bugs"
						sectionHref={`/projects/${projectSlug}/bugs`}
						current="All Bugs"
					/>
				) : (
					<SimpleBreadcrumb section="Bugs" />
				)}

				{/* Header */}
				<div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
					<div>
						<h1 className="text-3xl font-bold text-gray-900">
							{selectedProjectId
								? `Bugs - ${
										projects.find((p) => p.id === selectedProjectId)?.name
								  }`
								: "Bugs"}
						</h1>
						<p className="text-gray-600 mt-1">
							{selectedProjectId
								? `Manage and track bugs for ${
										projects.find((p) => p.id === selectedProjectId)?.name
								  }`
								: "Select a project to view and manage its bugs"}
						</p>
					</div>
					<div className="flex items-center gap-3">
						<Button
							onClick={() =>
								navigate(`/projects/${selectedProjectSlug}/bugs/create`)
							}
							disabled={loading || !selectedProjectId}
							title={
								!selectedProjectId
									? "Select a project first"
									: "Create a new bug"
							}
						>
							<Plus className="mr-2 h-4 w-4" />
							Create Bug
						</Button>
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
								onProjectSelect={(slug) => {
									updateParam("projectSlug", slug);
								}}
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
										placeholder="Search bugs by title or description..."
										value={searchTerm}
										onChange={(e) => handleSearchChange(e.target.value)}
										className="pl-10"
									/>
								</div>

								{/* Filter Controls */}
								<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
									{/* Status Filter */}
									<div>
										<label className="text-sm font-medium text-gray-700 mb-1 block">
											Status
										</label>
										<Select
											value={status}
											onValueChange={(value) =>
												handleStatusChange(value as any)
											}
										>
											<SelectTrigger>
												<SelectValue />
											</SelectTrigger>
											<SelectContent>
												<SelectItem value="ALL">All Statuses</SelectItem>
												<SelectItem value="OPEN">Open</SelectItem>
												<SelectItem value="FIXED">Fixed</SelectItem>
												<SelectItem value="CLOSED">Closed</SelectItem>
												<SelectItem value="REOPENED">Reopened</SelectItem>
											</SelectContent>
										</Select>
									</div>

									{/* Priority Filter */}
									<div>
										<label className="text-sm font-medium text-gray-700 mb-1 block">
											Priority
										</label>
										<Select
											value={priority}
											onValueChange={(value) =>
												handlePriorityChange(value as any)
											}
										>
											<SelectTrigger>
												<SelectValue />
											</SelectTrigger>
											<SelectContent>
												<SelectItem value="ALL">All Priorities</SelectItem>
												<SelectItem value="CRASH">Crash</SelectItem>
												<SelectItem value="CRITICAL">Critical</SelectItem>
												<SelectItem value="HIGH">High</SelectItem>
												<SelectItem value="MEDIUM">Medium</SelectItem>
												<SelectItem value="LOW">Low</SelectItem>
											</SelectContent>
										</Select>
									</div>

									{/* Type Filter */}
									<div>
										<label className="text-sm font-medium text-gray-700 mb-1 block">
											Type
										</label>
										<Select
											value={type}
											onValueChange={(value) => handleTypeChange(value as any)}
										>
											<SelectTrigger>
												<SelectValue />
											</SelectTrigger>
											<SelectContent>
												<SelectItem value="ALL">All Types</SelectItem>
												<SelectItem value="ISSUE">Issue</SelectItem>
												<SelectItem value="TASK">Task</SelectItem>
												<SelectItem value="SPEC">Specification</SelectItem>
											</SelectContent>
										</Select>
									</div>

									{/* Assignee Filter */}
									<div>
										<label className="text-sm font-medium text-gray-700 mb-1 block">
											Assignee
										</label>
										<Select
											value={assignee}
											onValueChange={(value) => handleAssigneeChange(value)}
										>
											<SelectTrigger>
												<SelectValue />
											</SelectTrigger>
											<SelectContent>
												<SelectItem value="ALL">All Assignees</SelectItem>
												<SelectItem value="UNASSIGNED">Unassigned</SelectItem>
												<SelectItem value="ASSIGNED">Assigned</SelectItem>
												<SelectItem value="ASSIGNED_TO_ME">
													Assigned to Me
												</SelectItem>
											</SelectContent>
										</Select>
									</div>

									{/* Sort */}
									<div>
										<label className="text-sm font-medium text-gray-700 mb-1 block">
											Sort By
										</label>
										<Select
											value={sortBy}
											onValueChange={(value) => handleSortChange(value as any)}
										>
											<SelectTrigger>
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
								</div>

								{/* Active Filters */}
								{hasActiveFilters && (
									<div className="flex items-center gap-2 pt-2 border-t">
										<span className="text-sm text-gray-500">
											Active filters:
										</span>
										{searchTerm && (
											<Badge variant="secondary">Search: {searchTerm}</Badge>
										)}
										{status !== "ALL" && (
											<Badge variant="secondary">Status: {status}</Badge>
										)}
										{priority !== "ALL" && (
											<Badge variant="secondary">Priority: {priority}</Badge>
										)}
										{type !== "ALL" && (
											<Badge variant="secondary">Type: {type}</Badge>
										)}
										{assignee !== "ALL" && (
											<Badge variant="secondary">
												{assignee === "UNASSIGNED"
													? "Unassigned"
													: assignee === "ASSIGNED"
													? "Assigned"
													: assignee === "ASSIGNED_TO_ME"
													? "My Bugs"
													: assignee}
											</Badge>
										)}
										<Button
											variant="ghost"
											size="sm"
											onClick={handleClearAll}
											className="text-xs"
										>
											Clear All
										</Button>
									</div>
								)}
							</div>
						)}
					</CardContent>
				</Card>

				{/* Bugs List */}
				{!selectedProjectId ? (
					<Card>
						<CardContent className="p-8 text-center">
							<Bug className="h-12 w-12 text-gray-400 mx-auto mb-4" />
							<h3 className="text-lg font-semibold text-gray-900 mb-2">
								Select a project to view bugs
							</h3>
							<p className="text-gray-600 mb-4">
								Choose a project from the dropdown above to get started.
							</p>
						</CardContent>
					</Card>
				) : (
					<BugsTable
						bugs={bugs}
						onViewDetails={handleViewBugDetails}
						disabled={loading}
						hasActiveFilters={hasActiveFilters}
						onClearFilters={handleClearAll}
						onCreateBug={handleNavigateToCreateBug}
					/>
				)}
			</main>

			<Footer />
		</div>
	);
}
