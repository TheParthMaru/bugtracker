/**
 * Similarity Analysis Page
 *
 * This page provides comprehensive similarity analysis between all bugs in a project,
 * helping users identify potential duplicates and analyze bug patterns.
 *
 * Features:
 * - Real-time similarity analysis using backend algorithms
 * - Configurable similarity threshold
 * - Search and filtering capabilities
 * - Sorting by various criteria
 * - Pagination support
 * - Duplicate marking functionality
 */

import React, { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import {
	Search,
	RefreshCw,
	ExternalLink,
	Filter,
	ArrowUpDown,
	Eye,
	Link,
} from "lucide-react";
import { bugService } from "@/services/bugService";
import { projectService } from "@/services/projectService";
import { logger } from "@/utils/logger";
import {
	BugSimilarityResult,
	BugSimilarityRelationship,
} from "@/types/similarity";
import { BugStatus, BugPriority, BugType } from "@/types/bug";
import {
	formatSimilarityPercentage,
	getSimilarityColor,
} from "@/types/similarity";
import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";

interface SimilarityAnalysisPageState {
	similarBugs: BugSimilarityRelationship[];
	loading: boolean;
	error: string | null;
	searchTerm: string;
	similarityThreshold: number;
	sortBy: string;
	sortDirection: "asc" | "desc";
	lastRefresh: Date | null;
	currentPage: number;
	totalPages: number;
	totalElements: number;
}

export function SimilarityAnalysisPage() {
	const { projectSlug } = useParams<{ projectSlug: string }>();
	const navigate = useNavigate();

	const [state, setState] = useState<SimilarityAnalysisPageState>({
		similarBugs: [],
		loading: true,
		error: null,
		searchTerm: "",
		similarityThreshold: 0.4, // 40% default
		sortBy: "similarityScore",
		sortDirection: "desc",
		lastRefresh: null,
		currentPage: 0,
		totalPages: 0,
		totalElements: 0,
	});

	const [projectName, setProjectName] = useState<string>("");

	// Load project details
	useEffect(() => {
		if (!projectSlug) return;

		const loadProjectDetails = async () => {
			try {
				// Check if user is authenticated
				const token = localStorage.getItem("bugtracker_token");

				if (!token) {
					setState((prev) => ({
						...prev,
						error: "Authentication required. Please log in to view this page.",
						loading: false,
					}));
					return;
				}

				const project = await projectService.getProjectBySlug(projectSlug);
				setProjectName(project.name);
			} catch (error) {
				console.error("DEBUG: Project service call failed", { error });
				logger.error(
					"SimilarityAnalysisPage",
					"Failed to load project details",
					{ error }
				);

				// Handle specific error cases
				if (error instanceof Error) {
					if (
						error.message.includes("401") ||
						error.message.includes("Unauthorized")
					) {
						setState((prev) => ({
							...prev,
							error:
								"Authentication required. Please log in to view this page.",
							loading: false,
						}));
					} else if (
						error.message.includes("404") ||
						error.message.includes("Not found")
					) {
						setState((prev) => ({
							...prev,
							error: `Project "${projectSlug}" not found. Please check the project slug.`,
							loading: false,
						}));
					} else {
						setState((prev) => ({
							...prev,
							error: `Failed to load project: ${error.message}`,
							loading: false,
						}));
					}
				} else {
					setState((prev) => ({
						...prev,
						error: "Failed to load project details. Please try again.",
						loading: false,
					}));
				}
			}
		};

		loadProjectDetails();
	}, [projectSlug]);

	// Load similar bugs
	const loadSimilarBugs = useCallback(async () => {
		if (!projectSlug) return;

		try {
			setState((prev) => ({ ...prev, loading: true, error: null }));

			// Use the new backend API endpoint for similarity analysis
			const similarBugs = await bugService.getProjectSimilarityAnalysis(
				projectSlug,
				state.similarityThreshold,
				state.searchTerm,
				state.sortBy,
				state.sortDirection,
				0, // page
				100 // size - get more results for better analysis
			);

			setState((prev) => ({
				...prev,
				similarBugs: similarBugs.content,
				currentPage: similarBugs.number,
				totalPages: similarBugs.totalPages,
				totalElements: similarBugs.totalElements,
				loading: false,
				lastRefresh: new Date(),
			}));
		} catch (error) {
			const errorMessage =
				error instanceof Error ? error.message : "Failed to load similar bugs";
			setState((prev) => ({
				...prev,
				loading: false,
				error: errorMessage,
			}));
			logger.error("SimilarityAnalysisPage", "Failed to load similar bugs", {
				error,
			});
		}
	}, [
		projectSlug,
		state.searchTerm,
		state.similarityThreshold,
		state.sortBy,
		state.sortDirection,
	]);

	// Load bugs on mount and when dependencies change
	useEffect(() => {
		loadSimilarBugs();
	}, [loadSimilarBugs]);

	// Auto-refresh every 3 minutes
	useEffect(() => {
		const interval = setInterval(() => {
			loadSimilarBugs();
		}, 3 * 60 * 1000); // 3 minutes

		return () => clearInterval(interval);
	}, [loadSimilarBugs]);

	// Handle search
	const handleSearchChange = (value: string) => {
		setState((prev) => ({ ...prev, searchTerm: value }));
	};

	// Handle similarity threshold change
	const handleThresholdChange = (value: string) => {
		setState((prev) => ({ ...prev, similarityThreshold: parseFloat(value) }));
	};

	// Handle sorting
	const handleSort = (field: string) => {
		setState((prev) => ({
			...prev,
			sortBy: field,
			sortDirection:
				prev.sortBy === field && prev.sortDirection === "desc" ? "asc" : "desc",
		}));
	};

	// Handle manual refresh
	const handleRefresh = () => {
		loadSimilarBugs();
	};

	// Handle view bug
	const handleViewBug = (bugId: number) => {
		if (projectSlug) {
			// Use projectTicketNumber to match the route definition: /projects/:slug/bugs/:projectTicketNumber
			const bug = state.similarBugs.find(
				(b) => b.bugAId === bugId || b.bugBId === bugId
			);
			if (bug) {
				const bugUrl = `/projects/${projectSlug}/bugs/${bug.bugAProjectTicketNumber}`;
				window.open(bugUrl, "_blank");
			}
		}
	};

	// Handle mark as duplicate
	const handleMarkAsDuplicate = async (
		bugId: number,
		originalBugId: number
	) => {
		if (!projectSlug) return;

		try {
			setState((prev) => ({ ...prev, loading: true, error: null }));

			// Call the real backend API to mark bug as duplicate
			await bugService.markAsDuplicate(projectSlug, bugId, {
				originalBugId,
				isAutomaticDetection: false,
				confidenceScore: 1.0,
				additionalContext: "",
			});

			// Update local state to reflect the change
			setState((prev) => ({
				...prev,
				similarBugs: prev.similarBugs.map((bug) =>
					bug.bugAId === bugId || bug.bugBId === bugId
						? { ...bug, isAlreadyMarkedDuplicate: true, originalBugId }
						: bug
				),
				loading: false,
			}));

			// Show success message (you can add a toast library if needed)
			console.log("Bug marked as duplicate successfully");

			// Refresh the data to get updated duplicate status
			loadSimilarBugs();
		} catch (error) {
			console.error("Failed to mark bug as duplicate:", error);
			setState((prev) => ({
				...prev,
				loading: false,
				error: "Failed to mark bug as duplicate. Please try again.",
			}));
		}
	};

	// Early return if no project slug
	if (!projectSlug) {
		return (
			<div className="min-h-screen bg-gray-50 flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8">
					<div className="text-center">
						<h1 className="text-2xl font-bold text-gray-900">
							Project not found
						</h1>
						<p className="text-gray-600 mt-2">
							Debug: projectSlug = {JSON.stringify(projectSlug)}
						</p>
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	// Show error state with login option
	if (state.error && state.error.includes("Authentication required")) {
		return (
			<div className="min-h-screen bg-gray-50 flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8">
					<div className="text-center max-w-md mx-auto">
						<div className="bg-red-50 border border-red-200 rounded-lg p-6 mb-6">
							<h1 className="text-xl font-semibold text-red-800 mb-2">
								Authentication Required
							</h1>
							<p className="text-red-700 mb-4">{state.error}</p>
							<Button
								onClick={() => (window.location.href = "/auth/login")}
								variant="default"
							>
								Go to Login
							</Button>
						</div>
						<p className="text-gray-600">
							After logging in, you can return to this page to view the
							similarity analysis.
						</p>
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	return (
		<div className="min-h-screen bg-gray-50 flex flex-col">
			<Navbar />

			<main className="flex-1 container mx-auto px-4 py-4">
				{/* Breadcrumbs */}
				<ProjectBreadcrumb
					projectSlug={projectSlug}
					projectName={projectName}
					section="Create Bug"
					sectionHref={`/projects/${projectSlug}/bugs/create`}
					current="Similarity Analysis"
				/>

				{/* Header */}
				<div className="mb-6">
					<h1 className="text-3xl font-bold text-gray-900 mb-2">
						Similarity Analysis
					</h1>
					<p className="text-gray-600">
						Analyze all bugs in {projectName || "this project"} for potential
						duplicates
					</p>
				</div>

				{/* Controls */}
				<Card className="mb-6">
					<CardContent className="p-4">
						<div className="flex flex-col md:flex-row gap-4 items-center justify-between">
							<div className="flex flex-col md:flex-row gap-4 flex-1">
								{/* Search */}
								<div className="relative flex-1 max-w-md">
									<Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
									<Input
										placeholder="Search bugs..."
										value={state.searchTerm}
										onChange={(e) => handleSearchChange(e.target.value)}
										className="pl-10"
									/>
								</div>

								{/* Similarity Threshold */}
								<div className="flex flex-col gap-1">
									<Label
										htmlFor="similarity-threshold"
										className="text-sm font-medium text-gray-700"
									>
										Similarity Threshold
									</Label>
									<div className="flex items-center gap-2">
										<Filter className="h-4 w-4 text-gray-400" />
										<Select
											value={state.similarityThreshold.toString()}
											onValueChange={handleThresholdChange}
										>
											<SelectTrigger className="w-32" id="similarity-threshold">
												<SelectValue />
											</SelectTrigger>
											<SelectContent>
												<SelectItem value="0.3">30%+</SelectItem>
												<SelectItem value="0.4">40%+</SelectItem>
												<SelectItem value="0.5">50%+</SelectItem>
												<SelectItem value="0.6">60%+</SelectItem>
												<SelectItem value="0.7">70%+</SelectItem>
												<SelectItem value="0.8">80%+</SelectItem>
												<SelectItem value="0.9">90%+</SelectItem>
											</SelectContent>
										</Select>
									</div>
								</div>

								{/* Sort Order */}
								<div className="flex flex-col gap-1">
									<Label
										htmlFor="sort-order"
										className="text-sm font-medium text-gray-700"
									>
										Sort Order
									</Label>
									<div className="flex items-center gap-2">
										<ArrowUpDown className="h-4 w-4 text-gray-400" />
										<Select
											value={`${state.sortBy}-${state.sortDirection}`}
											onValueChange={(value) => {
												const [sortBy, sortDirection] = value.split("-");
												setState((prev) => ({
													...prev,
													sortBy,
													sortDirection: sortDirection as "asc" | "desc",
												}));
											}}
										>
											<SelectTrigger className="w-32" id="sort-order">
												<SelectValue />
											</SelectTrigger>
											<SelectContent>
												<SelectItem value="similarityScore-desc">
													Similarity Score ↓
												</SelectItem>
												<SelectItem value="similarityScore-asc">
													Similarity Score ↑
												</SelectItem>
												<SelectItem value="title-asc">Title A-Z</SelectItem>
												<SelectItem value="title-desc">Title Z-A</SelectItem>
											</SelectContent>
										</Select>
									</div>
								</div>

								{/* Download Button */}
								<div className="flex flex-col gap-1">
									<Label className="text-sm font-medium text-gray-700">
										Export
									</Label>
									<Button
										variant="outline"
										size="sm"
										onClick={() => {
											// TODO: Implement download functionality
											console.log("Download functionality to be implemented");
										}}
										className="w-32"
									>
										<ExternalLink className="h-4 w-4 mr-2" />
										Download
									</Button>
								</div>
							</div>

							{/* Refresh */}
							<div className="flex items-center gap-2">
								{state.lastRefresh && (
									<span className="text-sm text-gray-500">
										Last updated: {state.lastRefresh.toLocaleTimeString()}
									</span>
								)}
								<Button
									variant="outline"
									size="sm"
									onClick={handleRefresh}
									disabled={state.loading}
								>
									<RefreshCw
										className={`h-4 w-4 mr-2 ${
											state.loading ? "animate-spin" : ""
										}`}
									/>
									Refresh
								</Button>
							</div>
						</div>
					</CardContent>
				</Card>

				{/* Results */}
				<Card>
					<CardHeader>
						<CardTitle className="flex items-center justify-between">
							<span>Similar Bugs ({state.similarBugs.length})</span>
							{state.loading && <LoadingSpinner size="sm" />}
						</CardTitle>
					</CardHeader>
					<CardContent>
						{state.error ? (
							<div className="text-center py-8">
								<div className="text-red-600 mb-2">{state.error}</div>
								<Button onClick={loadSimilarBugs} variant="outline">
									Try Again
								</Button>
							</div>
						) : state.similarBugs.length === 0 ? (
							<div className="text-center py-8 text-gray-500">
								No bugs found with {Math.round(state.similarityThreshold * 100)}
								%+ similarity
							</div>
						) : (
							<div className="overflow-x-auto">
								<table className="w-full">
									<thead>
										<tr className="border-b">
											<th className="text-left p-3 font-medium">
												<button
													onClick={() => handleSort("similarityScore")}
													className="flex items-center gap-1 hover:text-gray-700"
												>
													Similarity
													<ArrowUpDown className="h-3 w-3" />
												</button>
											</th>
											<th className="text-left p-3 font-medium">
												<button
													onClick={() => handleSort("title")}
													className="flex items-center gap-1 hover:text-gray-700"
												>
													Bug A
													<ArrowUpDown className="h-3 w-3" />
												</button>
											</th>
											<th className="text-left p-3 font-medium">
												<button
													onClick={() => handleSort("title")}
													className="flex items-center gap-1 hover:text-gray-700"
												>
													Bug B
													<ArrowUpDown className="h-3 w-3" />
												</button>
											</th>
											<th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
												Actions
											</th>
										</tr>
									</thead>
									<tbody>
										{state.similarBugs.map((bug) => (
											<tr
												key={`${bug.bugAId}-${bug.bugBId}-${bug.similarityScore}`}
												className="border-b hover:bg-gray-50"
											>
												<td className="p-3">
													<Badge
														variant={getSimilarityColor(bug.similarityScore)}
													>
														{formatSimilarityPercentage(bug.similarityScore)}
													</Badge>
												</td>
												<td className="p-3">
													<div className="max-w-xs">
														<div className="font-medium truncate">
															#{bug.bugAProjectTicketNumber}: {bug.bugATitle}
														</div>
														<div className="text-sm text-gray-500 truncate">
															{bug.bugAStatus} • {bug.bugAPriority}
														</div>
													</div>
												</td>
												<td className="p-3">
													<div className="max-w-xs">
														<div className="font-medium truncate">
															#{bug.bugBProjectTicketNumber}: {bug.bugBTitle}
														</div>
														<div className="text-sm text-gray-500 truncate">
															{bug.bugBStatus} • {bug.bugBPriority}
														</div>
													</div>
												</td>
												{/* Actions */}
												<td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
													<div className="flex items-center gap-2">
														<Button
															variant="outline"
															size="sm"
															onClick={() => handleViewBug(bug.bugAId)}
															className="flex items-center gap-1"
														>
															<Eye className="h-4 w-4" />
															View Bug A
														</Button>
														<Button
															variant="outline"
															size="sm"
															onClick={() => handleViewBug(bug.bugBId)}
															className="flex items-center gap-1"
														>
															<Eye className="h-4 w-4" />
															View Bug B
														</Button>
														<Button
															variant="outline"
															size="sm"
															onClick={() =>
																handleMarkAsDuplicate(bug.bugAId, bug.bugBId)
															}
															className="flex items-center gap-1 text-orange-600 hover:text-orange-700 hover:bg-orange-50"
														>
															<Link className="h-4 w-4" />
															Mark Duplicate
														</Button>
													</div>
												</td>
											</tr>
										))}
									</tbody>
								</table>
							</div>
						)}
					</CardContent>
				</Card>
			</main>

			<Footer />
		</div>
	);
}
