import React, { useState, useEffect } from "react";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import {
	BarChart3,
	PieChart,
	TrendingUp,
	AlertTriangle,
	Users,
	Clock,
	FileText,
	Download,
	Calendar,
	Activity,
	Search,
	RefreshCw,
	Eye,
} from "lucide-react";
import { analyticsService } from "@/services/analyticsService";
import { bugService } from "@/services/bugService";
import {
	ProjectBugStatistics,
	TeamPerformanceStatistics,
	CHART_COLORS,
	PRIORITY_COLORS,
	STATUS_COLORS,
	TYPE_COLORS,
} from "@/types/analytics";
import {
	BugSimilarityResult,
	BugSimilarityRelationship,
	formatSimilarityPercentage,
} from "@/types/similarity";
import { StatisticsCard } from "./StatisticsCard";
import { AnalyticsChart } from "./AnalyticsChart";

import { TeamPerformanceTable } from "./TeamPerformanceTable";
import { MembersStatsTable } from "./MembersStatsTable";
import { TeamStatsTable } from "./TeamStatsTable";

interface AnalyticsDashboardProps {
	projectSlug: string;
	className?: string;
}

export function AnalyticsDashboard({
	projectSlug,
	className,
}: AnalyticsDashboardProps) {
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [customDateRange, setCustomDateRange] = useState<{
		start: Date;
		end: Date;
	} | null>(null);

	// Data states
	const [statistics, setStatistics] = useState<ProjectBugStatistics | null>(
		null
	);
	const [teamStats, setTeamStats] = useState<TeamPerformanceStatistics | null>(
		null
	);

	// Similarity analysis state
	const [similarBugs, setSimilarBugs] = useState<BugSimilarityRelationship[]>(
		[]
	);
	const [similarityLoading, setSimilarityLoading] = useState(false);
	const [similarityError, setSimilarityError] = useState<string | null>(null);
	const [searchTerm, setSearchTerm] = useState("");
	const [similarityThreshold, setSimilarityThreshold] = useState(0.4);
	const [currentPage, setCurrentPage] = useState(0);
	const [totalPages, setTotalPages] = useState(0);
	const [totalElements, setTotalElements] = useState(0);
	const [sortBy, setSortBy] = useState("similarityScore");
	const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");
	const [lastSimilarityRefresh, setLastSimilarityRefresh] =
		useState<Date | null>(null);

	useEffect(() => {
		loadAnalyticsData();
	}, [projectSlug, customDateRange]);

	// Load similarity data when component mounts or search/threshold/sorting/date range changes
	useEffect(() => {
		loadSimilarityData();
	}, [
		projectSlug,
		searchTerm,
		similarityThreshold,
		currentPage,
		sortBy,
		sortDirection,
		customDateRange,
	]);

	const loadSimilarityData = async () => {
		setSimilarityLoading(true);
		setSimilarityError(null);

		try {
			// Get current date range for similarity analysis
			const startDate = customDateRange?.start?.toISOString();
			const endDate = customDateRange?.end?.toISOString();

			const response = await bugService.getProjectSimilarityAnalysis(
				projectSlug,
				similarityThreshold,
				searchTerm || undefined,
				sortBy,
				sortDirection,
				currentPage,
				10, // page size
				startDate,
				endDate
			);

			console.log("Similarity data received:", response);
			console.log("Page info:", {
				currentPage: response.number,
				totalPages: response.totalPages,
				totalElements: response.totalElements,
				contentSize: response.content.length,
			});
			console.log(
				"Pagination debug - totalPages:",
				response.totalPages,
				"totalElements:",
				response.totalElements
			);

			setSimilarBugs(response.content);
			setTotalPages(response.totalPages);
			setTotalElements(response.totalElements);
			setLastSimilarityRefresh(new Date());
		} catch (error) {
			const errorMessage =
				error instanceof Error
					? error.message
					: "Failed to load similarity data";
			setSimilarityError(errorMessage);
			console.error("Failed to load similarity data:", error);
		} finally {
			setSimilarityLoading(false);
		}
	};

	const loadAnalyticsData = async () => {
		setLoading(true);
		setError(null);

		try {
			// If custom date range is selected, use it; otherwise, fetch all data (no date filtering)
			const startDate = customDateRange?.start?.toISOString();
			const endDate = customDateRange?.end?.toISOString();

			console.log("Loading analytics data with date range:", {
				startDate,
				endDate,
				hasStartDate: !!startDate,
				hasEndDate: !!endDate,
				startDateType: startDate ? typeof startDate : "undefined",
				endDateType: endDate ? typeof endDate : "undefined",
			});

			// Load all analytics data
			const [statsData, teamData] = await Promise.all([
				analyticsService.getProjectStatistics(projectSlug, startDate, endDate),
				analyticsService.getTeamPerformanceStatistics(
					projectSlug,
					startDate,
					endDate
				),
			]);

			console.log("📊 Analytics data loaded successfully:", {
				statsData: {
					totalBugs: statsData?.totalBugs,
					openBugs: statsData?.openBugs,
					resolutionRate: statsData?.resolutionRate,
				},
				teamData: {
					assigneeCount: teamData?.assigneeStats?.length,
					reporterCount: teamData?.reporterStats?.length,
				},
				dateRange: { startDate, endDate },
			});

			setStatistics(statsData);
			setTeamStats(teamData);
		} catch (err) {
			console.error("Failed to load analytics data:", err);
			setError("Failed to load analytics data. Please try again.");
		} finally {
			setLoading(false);
		}
	};

	if (loading) {
		return (
			<div className={`space-y-6 ${className}`}>
				<div className="text-center">
					<h2 className="text-2xl font-bold">Loading analytics...</h2>
				</div>
				<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
					{[...Array(4)].map((_, i) => (
						<Card key={i} className="animate-pulse">
							<CardHeader className="pb-2">
								<div className="h-4 bg-muted rounded w-3/4"></div>
							</CardHeader>
							<CardContent>
								<div className="h-8 bg-muted rounded w-1/2"></div>
							</CardContent>
						</Card>
					))}
				</div>
			</div>
		);
	}

	if (error && !statistics) {
		return (
			<div className={`space-y-6 ${className}`}>
				<div className="flex items-center justify-between">
					<div className="space-y-2">
						<h1 className="text-3xl font-bold">Analytics Dashboard</h1>
						<p className="text-muted-foreground">
							Comprehensive bug tracking analytics and insights
						</p>
					</div>
				</div>
				<Card>
					<CardContent className="pt-6">
						<div className="text-center text-muted-foreground">
							<AlertTriangle className="h-12 w-12 mx-auto mb-4" />
							<p>{error}</p>
							<Button onClick={loadAnalyticsData} className="mt-4">
								Try Again
							</Button>
						</div>
					</CardContent>
				</Card>
			</div>
		);
	}

	return (
		<div className={`space-y-4 ${className}`}>
			{/* Compact Date Range Filter */}
			<div className="flex items-center justify-between gap-4 p-3 bg-muted/30 rounded-md border">
				<div className="flex items-center gap-3">
					<span className="text-sm font-medium">Date Range</span>
					<div className="flex items-center gap-2">
						<input
							type="date"
							value={
								customDateRange?.start
									? customDateRange.start.toISOString().split("T")[0]
									: ""
							}
							onChange={(e) => {
								const date = e.target.value
									? new Date(e.target.value)
									: undefined;
								setCustomDateRange((prev) => ({
									...prev,
									start: date,
								}));
							}}
							className="border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
							max={
								customDateRange?.end
									? customDateRange.end.toISOString().split("T")[0]
									: undefined
							}
						/>
						<span className="text-xs text-muted-foreground">to</span>
						<input
							type="date"
							value={
								customDateRange?.end
									? customDateRange.end.toISOString().split("T")[0]
									: ""
							}
							onChange={(e) => {
								const date = e.target.value
									? new Date(e.target.value)
									: undefined;
								if (
									date &&
									customDateRange?.start &&
									date < customDateRange.start
								) {
									// End date cannot be before start date
									return;
								}
								setCustomDateRange((prev) => ({
									...prev,
									end: date,
								}));
							}}
							className="border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
							min={
								customDateRange?.start
									? customDateRange.start.toISOString().split("T")[0]
									: undefined
							}
						/>
						{customDateRange?.start && customDateRange?.end && (
							<>
								<Button
									variant="outline"
									size="sm"
									onClick={() => setCustomDateRange(null)}
									className="h-8 px-2 text-xs"
								>
									Clear
								</Button>
								<Button
									variant="outline"
									size="sm"
									onClick={() => loadAnalyticsData()}
									className="h-8 px-2 text-xs"
								>
									<RefreshCw className="h-3 w-3 mr-1" />
									Refresh
								</Button>
							</>
						)}
					</div>
				</div>
			</div>

			{/* Overview Statistics */}
			{statistics && (
				<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
					<StatisticsCard
						title="Total Bugs"
						value={statistics.totalBugs}
						icon={<FileText className="h-4 w-4" />}
						// Only show trend if there's historical data
						change={statistics.totalBugs > 0 ? undefined : undefined}
						changeType={statistics.totalBugs > 0 ? "neutral" : "neutral"}
					/>
					<StatisticsCard
						title="Open Bugs"
						value={statistics.openBugs}
						icon={<AlertTriangle className="h-4 w-4" />}
						// Only show trend if there's historical data
						change={statistics.openBugs > 0 ? undefined : undefined}
						changeType={statistics.openBugs > 0 ? "neutral" : "neutral"}
					/>
					<StatisticsCard
						title="Resolution Rate"
						value={`${(statistics.resolutionRate * 100).toFixed(1)}%`}
						icon={<TrendingUp className="h-4 w-4" />}
						// Only show trend if there's historical data
						change={statistics.resolutionRate > 0 ? undefined : undefined}
						changeType={statistics.resolutionRate > 0 ? "neutral" : "neutral"}
					/>
				</div>
			)}

			{/* Main Content Tabs */}
			<Tabs defaultValue="overview" className="space-y-4">
				<TabsList>
					<TabsTrigger value="overview">Overview</TabsTrigger>
					<TabsTrigger value="similarity">Similarity Analysis</TabsTrigger>
					<TabsTrigger value="team">Team</TabsTrigger>
				</TabsList>

				<TabsContent value="overview" className="space-y-4">
					<div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
						{/* Distribution Charts */}
						<Card>
							<CardHeader>
								<CardTitle>Priority Distribution</CardTitle>
								<CardDescription>Bugs by priority level</CardDescription>
							</CardHeader>
							<CardContent>
								<AnalyticsChart
									data={Object.entries(
										statistics?.priorityDistribution || {}
									).map(([key, value]) => ({
										label: key,
										value,
										color: PRIORITY_COLORS[key as keyof typeof PRIORITY_COLORS],
									}))}
									type="doughnut"
									title="Priority Distribution"
								/>
							</CardContent>
						</Card>

						<Card>
							<CardHeader>
								<CardTitle>Status Distribution</CardTitle>
								<CardDescription>Current bug status breakdown</CardDescription>
							</CardHeader>
							<CardContent>
								<AnalyticsChart
									data={Object.entries(
										statistics?.statusDistribution || {}
									).map(([key, value]) => ({
										label: key,
										value,
										color: STATUS_COLORS[key as keyof typeof STATUS_COLORS],
									}))}
									type="doughnut"
									title="Status Distribution"
								/>
							</CardContent>
						</Card>
					</div>
				</TabsContent>

				<TabsContent value="similarity" className="space-y-4">
					<Card>
						<CardHeader>
							<CardTitle className="text-lg">Similarity Analysis</CardTitle>
							<CardDescription>
								Analyze all bugs for potential duplication
							</CardDescription>
						</CardHeader>
						<CardContent>
							<div className="space-y-4">
								{/* Search and Filter Bar */}
								<div className="flex items-center justify-evenly">
									<div className="flex items-center justify-center gap-2">
										<div className="relative">
											<Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
											<Input
												placeholder="Search bugs..."
												className="pl-10 w-64"
												value={searchTerm}
												onChange={(e) => setSearchTerm(e.target.value)}
											/>
										</div>
										<div className="flex gap-2">
											<Label className="text-sm font-medium">
												Similarity Threshold
											</Label>
											<Select
												value={similarityThreshold.toString()}
												onValueChange={(value) =>
													setSimilarityThreshold(parseFloat(value))
												}
											>
												<SelectTrigger className="w-32">
													<SelectValue placeholder="Threshold" />
												</SelectTrigger>
												<SelectContent>
													<SelectItem value="0.1">10%</SelectItem>
													<SelectItem value="0.2">20%</SelectItem>
													<SelectItem value="0.3">30%</SelectItem>
													<SelectItem value="0.4">40%</SelectItem>
													<SelectItem value="0.5">50%</SelectItem>
													<SelectItem value="0.6">60%</SelectItem>
													<SelectItem value="0.7">70%</SelectItem>
													<SelectItem value="0.8">80%</SelectItem>
													<SelectItem value="0.9">90%</SelectItem>
												</SelectContent>
											</Select>
										</div>

										<div className="flex gap-2">
											<Label className="text-sm font-medium">Sort By</Label>
											<Select value={sortBy} onValueChange={setSortBy}>
												<SelectTrigger className="w-40">
													<SelectValue placeholder="Sort by" />
												</SelectTrigger>
												<SelectContent>
													<SelectItem value="similarityScore">
														Similarity Score
													</SelectItem>
													<SelectItem value="title">Bug Title</SelectItem>
													<SelectItem value="createdAt">
														Created Date
													</SelectItem>
													<SelectItem value="status">Status</SelectItem>
													<SelectItem value="priority">Priority</SelectItem>
												</SelectContent>
											</Select>
										</div>

										<Button
											variant="outline"
											size="sm"
											onClick={() =>
												setSortDirection(
													sortDirection === "desc" ? "asc" : "desc"
												)
											}
											className="w-12"
										>
											{sortDirection === "desc" ? "↓" : "↑"}
										</Button>
									</div>
									<div className="flex items-center gap-2 text-sm text-muted-foreground">
										<span>
											Last updated:{" "}
											{lastSimilarityRefresh
												? lastSimilarityRefresh.toLocaleDateString()
												: "Never"}
										</span>
										<Button
											variant="outline"
											size="sm"
											onClick={loadSimilarityData}
											disabled={similarityLoading}
										>
											<RefreshCw
												className={`h-4 w-4 mr-2 ${
													similarityLoading ? "animate-spin" : ""
												}`}
											/>
											Refresh
										</Button>
									</div>
								</div>

								{/* Similar Bugs Table */}
								<div>
									<h4 className="text-lg font-semibold mb-4">
										Similar Bugs ({similarBugs.length})
									</h4>
									<div className="border rounded-lg">
										{similarityLoading ? (
											<div className="p-8 text-center">
												<div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
												<p className="text-muted-foreground">
													Loading similarity analysis...
												</p>
											</div>
										) : similarityError ? (
											<div className="p-8 text-center text-destructive">
												<p>Error: {similarityError}</p>
												<Button
													onClick={loadSimilarityData}
													variant="outline"
													className="mt-2"
												>
													Retry
												</Button>
											</div>
										) : similarBugs.length === 0 ? (
											<div className="p-8 text-center text-muted-foreground">
												<Search className="h-8 w-8 mx-auto mb-2 text-muted-foreground/50" />
												<p>No similar bugs found</p>
												<p className="text-sm">
													{searchTerm
														? `No bugs match "${searchTerm}"`
														: "Similarity analysis will appear here"}
												</p>
											</div>
										) : (
											<>
												<Table>
													<TableHeader>
														<TableRow>
															<TableHead>Bug A (# & Title)</TableHead>
															<TableHead>Bug B (# & Title)</TableHead>
															<TableHead>Similarity %</TableHead>
														</TableRow>
													</TableHeader>
													<TableBody>
														{similarBugs.map((bug, index) => (
															<TableRow
																key={`similarity-${bug.bugAId}-${bug.bugBId}-${index}`}
															>
																<TableCell>
																	<div className="space-y-1">
																		<div className="font-medium">
																			<a
																				href={`/projects/${projectSlug}/bugs/${bug.bugAProjectTicketNumber}`}
																				className="text-blue-600 hover:text-blue-800 hover:underline"
																			>
																				Bug #{bug.bugAProjectTicketNumber}
																			</a>
																		</div>
																		<div
																			className="text-sm text-muted-foreground max-w-xs truncate"
																			title={bug.bugATitle}
																		>
																			{bug.bugATitle}
																		</div>
																		<div className="text-xs text-muted-foreground">
																			{bug.bugAStatus} • {bug.bugAPriority}
																		</div>
																	</div>
																</TableCell>
																<TableCell>
																	<div className="space-y-1">
																		<div className="font-medium">
																			<a
																				href={`/projects/${projectSlug}/bugs/${bug.bugBProjectTicketNumber}`}
																				className="text-blue-600 hover:text-blue-800 hover:underline"
																			>
																				Bug #{bug.bugBProjectTicketNumber}
																			</a>
																		</div>
																		<div className="text-sm text-muted-foreground max-w-xs truncate">
																			{bug.bugBTitle}
																		</div>
																		<div className="text-xs text-muted-foreground">
																			{bug.bugBStatus} • {bug.bugBPriority}
																		</div>
																	</div>
																</TableCell>
																<TableCell>
																	<Badge
																		variant={
																			bug.similarityScore >= 0.7
																				? "destructive"
																				: bug.similarityScore >= 0.5
																				? "default"
																				: "secondary"
																		}
																		className="font-mono min-w-[60px] text-center"
																	>
																		{formatSimilarityPercentage(
																			bug.similarityScore
																		)}
																	</Badge>
																	<div className="text-xs text-muted-foreground mt-1">
																		{bug.similarityScore >= 0.7
																			? "Likely Duplicate"
																			: bug.similarityScore >= 0.5
																			? "Probably Related"
																			: "Possibly Related"}
																	</div>
																</TableCell>
															</TableRow>
														))}
													</TableBody>
												</Table>

												{/* Pagination Controls */}
												<div className="flex items-center justify-center mt-4 px-4 py-2 border-t">
													{/* <div className="text-sm text-muted-foreground">
														{totalElements > 0 ? (
															<>
																Showing {currentPage * 10 + 1} to{" "}
																{Math.min(
																	(currentPage + 1) * 10,
																	totalElements
																)}{" "}
																of {totalElements} relationships
															</>
														) : (
															<>No relationships found</>
														)}
													</div> */}

													{/* Pagination info moved below the pagination buttons */}
													{/* Always show pagination info, but only show controls when multiple pages */}
													{totalPages > 0 && (
														<div className="flex items-center gap-2">
															<Button
																variant="outline"
																size="sm"
																onClick={() => {
																	setCurrentPage(Math.max(0, currentPage - 1));
																	window.scrollTo({
																		top: 0,
																		behavior: "smooth",
																	});
																}}
																disabled={currentPage === 0}
															>
																Previous
															</Button>
															<div className="flex items-center gap-1">
																{Array.from({ length: totalPages }, (_, i) => {
																	const pageNum = i; // Simple sequential numbering
																	return (
																		<Button
																			key={pageNum}
																			variant={
																				currentPage === pageNum
																					? "default"
																					: "outline"
																			}
																			size="sm"
																			onClick={() => {
																				setCurrentPage(pageNum);
																				window.scrollTo({
																					top: 0,
																					behavior: "smooth",
																				});
																			}}
																			className="w-8 h-8 p-0"
																		>
																			{pageNum + 1}
																		</Button>
																	);
																})}
															</div>
															<Button
																variant="outline"
																size="sm"
																onClick={() => {
																	setCurrentPage(
																		Math.min(totalPages - 1, currentPage + 1)
																	);
																	window.scrollTo({
																		top: 0,
																		behavior: "smooth",
																	});
																}}
																disabled={currentPage === totalPages - 1}
															>
																Next
															</Button>
														</div>
													)}
												</div>

												<div className="text-muted-foreground text-center mt-3">
													{totalElements > 0 ? (
														<>
															Showing {currentPage * 10 + 1} of {totalElements}{" "}
															relationships
														</>
													) : (
														<>No relationships found</>
													)}
												</div>

												{/* Pagination Summary - Removed duplicate text, keeping only page info */}
												{!similarityLoading && totalPages > 1 && (
													<div className="text-center text-muted-foreground mt-3">
														<span>
															Page {currentPage + 1} of {totalPages}
														</span>
													</div>
												)}
											</>
										)}
									</div>
								</div>
							</div>
						</CardContent>
					</Card>
				</TabsContent>

				<TabsContent value="team" className="space-y-4">
					{teamStats && (
						<Card>
							<CardHeader>
								<CardTitle>Team Performance</CardTitle>
								<CardDescription>
									Individual and team performance metrics
								</CardDescription>
							</CardHeader>
							<CardContent>
								<Tabs defaultValue="members" className="w-full">
									<TabsList className="grid w-full grid-cols-2">
										<TabsTrigger value="members">Members Stats</TabsTrigger>
										<TabsTrigger value="teams">Team Stats</TabsTrigger>
									</TabsList>

									<TabsContent value="members" className="space-y-4">
										<MembersStatsTable data={teamStats} />
									</TabsContent>

									<TabsContent value="teams" className="space-y-4">
										<TeamStatsTable data={teamStats} />
									</TabsContent>
								</Tabs>
							</CardContent>
						</Card>
					)}
				</TabsContent>
			</Tabs>
		</div>
	);
}
