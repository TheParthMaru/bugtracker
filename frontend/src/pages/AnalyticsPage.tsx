import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, BarChart3, Bug, Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import { AnalyticsDashboard } from "@/components/analytics";
import { projectService } from "@/services/projectService";
import { analyticsService } from "@/services/analyticsService";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";
import type { Project } from "@/types/project";
import type {
	ProjectBugStatistics,
	TeamPerformanceStatistics,
} from "@/types/analytics";
import { format } from "date-fns";

export function AnalyticsPage() {
	const { projectSlug } = useParams<{ projectSlug: string }>();
	const navigate = useNavigate();
	const [project, setProject] = useState<Project | null>(null);
	const [loading, setLoading] = useState(true);
	const [exporting, setExporting] = useState(false);

	useEffect(() => {
		if (projectSlug) {
			loadProject();
		}
	}, [projectSlug]);

	const loadProject = async () => {
		try {
			setLoading(true);
			const projectData = await projectService.getProjectBySlug(projectSlug!);
			setProject(projectData);
		} catch (error) {
			console.error("Failed to load project:", error);
		} finally {
			setLoading(false);
		}
	};

	const exportReport = async () => {
		if (!project) return;

		try {
			setExporting(true);

			// Get date range for the last 30 days
			const endDate = new Date();
			const startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);

			console.log("Starting export for project:", project.projectSlug);
			console.log(
				"Date range:",
				startDate.toISOString(),
				"to",
				endDate.toISOString()
			);

			// Fetch all analytics data for the report
			const [statistics, teamPerformance] = await Promise.all([
				analyticsService.getProjectStatistics(project.projectSlug),
				analyticsService.getTeamPerformanceStatistics(project.projectSlug),
			]);

			console.log("Analytics data received:", {
				statistics: !!statistics,
				teamPerformance: !!teamPerformance,
			});

			// Generate CSV content
			const csvContent = generateCSVReport(
				project,
				statistics,
				teamPerformance
			);

			// Create and download the file
			const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
			const link = document.createElement("a");
			const url = URL.createObjectURL(blob);
			link.setAttribute("href", url);
			link.setAttribute(
				"download",
				`${project.name}-analytics-report-${format(
					new Date(),
					"yyyy-MM-dd"
				)}.csv`
			);
			link.style.visibility = "hidden";
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
		} catch (err) {
			console.error("Failed to export report:", err);
			alert("Failed to export report. Please try again.");
		} finally {
			setExporting(false);
		}
	};

	const generateCSVReport = (
		project: Project,
		statistics: ProjectBugStatistics | undefined,
		teamPerformance: TeamPerformanceStatistics | undefined
	): string => {
		// Provide fallback values if any data is undefined
		const safeStats = statistics || {
			totalBugs: 0,
			openBugs: 0,
			assignedBugs: 0,
			fixedBugs: 0,
			closedBugs: 0,
			reopenedBugs: 0,
			resolutionRate: 0,
			priorityDistribution: {},
			typeDistribution: {},
			statusDistribution: {},
		};

		const safeTeam = teamPerformance || {
			assigneeStats: [],
			reporterStats: [],
			resolutionTimeStats: [],
		};

		const rows = [
			["Project Analytics Report"],
			[""],
			["Project Information"],
			["Name", project.name || "N/A"],
			["Slug", project.projectSlug || "N/A"],
			["Description", project.description || "N/A"],
			[
				"Created",
				project.createdAt
					? format(new Date(project.createdAt), "dd/MM/yyyy")
					: "N/A",
			],
			[""],
			["Bug Statistics"],
			["Total Bugs", safeStats.totalBugs.toString()],
			["Open Bugs", safeStats.openBugs.toString()],
			["Assigned Bugs", safeStats.assignedBugs.toString()],
			["Fixed Bugs", safeStats.fixedBugs.toString()],
			["Closed Bugs", safeStats.closedBugs.toString()],
			["Reopened Bugs", safeStats.reopenedBugs.toString()],
			["Resolution Rate", `${(safeStats.resolutionRate * 100).toFixed(2)}%`],
			[""],
			["Priority Distribution"],
			...Object.entries(safeStats.priorityDistribution || {}).map(
				([priority, count]) => [priority, count.toString()]
			),
			[""],
			["Type Distribution"],
			...Object.entries(safeStats.typeDistribution || {}).map(
				([type, count]) => [type, count.toString()]
			),
			[""],
			["Status Distribution"],
			...Object.entries(safeStats.statusDistribution || {}).map(
				([status, count]) => [status, count.toString()]
			),

			["Team Performance"],
			[
				"User Name",
				"Total Bugs",
				"Resolved Bugs",
				"Avg Resolution Time (days)",
			],
			...(safeTeam.assigneeStats || []).map((assignee) => [
				assignee.fullName || "Unknown",
				(assignee.count || 0).toString(),
				(assignee.resolvedCount || 0).toString(),
				"0", // Placeholder for resolution time
			]),
			[""],
			["Report Generated", format(new Date(), "dd/MM/yyyy HH:mm:ss")],
		];

		return rows
			.map((row) => row.map((cell) => `"${cell}"`).join(","))
			.join("\n");
	};

	const handleBack = () => {
		navigate(`/projects/${projectSlug}`);
	};

	if (loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-7xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="outline" size="sm" disabled>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
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

	if (!project) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-7xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="outline" size="sm" onClick={handleBack}>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
						</Button>
					</div>
					<div className="text-center">
						<h1 className="text-2xl font-bold text-gray-900">
							Project not found
						</h1>
						<p className="text-gray-600 mt-2">
							Please select a valid project to view analytics.
						</p>
						<Button onClick={handleBack} className="mt-4">
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
						</Button>
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />

			<main className="flex-1 container mx-auto px-4 py-4">
				{/* Breadcrumb */}
				<ProjectBreadcrumb
					projectName={project.name}
					projectSlug={project.projectSlug}
					onBackClick={handleBack}
				/>

				{/* Page Title Section */}
				<div className="mb-8">
					<h1 className="text-4xl font-bold mb-2">{project.name} Analytics</h1>
					<p className="text-lg text-muted-foreground">
						Comprehensive bug tracking analytics and insights
					</p>
				</div>

				{/* Analytics Dashboard */}
				<AnalyticsDashboard projectSlug={projectSlug} />
			</main>

			<Footer />
		</div>
	);
}
