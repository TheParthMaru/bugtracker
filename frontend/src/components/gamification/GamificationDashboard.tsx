/**
 * GamificationDashboard Component
 *
 * Main dashboard showing user's gamification overview.
 * Follows the specification requirements without over-engineering.
 */

import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { LegacyBreadcrumb } from "@/components/ui/breadcrumb";
import {
	Trophy,
	TrendingUp,
	Calendar,
	Target,
	ArrowRight,
	Zap,
	Bug,
	Flame,
} from "lucide-react";
import { gamificationService } from "@/services/gamificationService";
import { projectService } from "@/services/projectService";
import { PointNotificationService } from "@/services/pointNotificationService";
import ToastTestComponent from "./ToastTestComponent";
import {
	isBugResolution,
	isBugReopened,
	isWelcomeBonus,
	extractBugPriority,
	extractProjectName,
	extractTicketNumber,
} from "@/types/gamification-enums";
import type {
	UserPointsResponse,
	StreakInfoResponse,
	PointTransactionResponse,
	LeaderboardEntryResponse,
} from "@/types/gamification";

interface GamificationDashboardProps {
	userId: string;
}

export default function GamificationDashboard({
	userId,
}: GamificationDashboardProps) {
	console.log("GamificationDashboard: Component rendered with userId:", userId);

	const [userPoints, setUserPoints] = useState<UserPointsResponse | null>(null);
	const [streakInfo, setStreakInfo] = useState<StreakInfoResponse | null>(null);
	const [recentTransactions, setRecentTransactions] = useState<
		PointTransactionResponse[]
	>([]);
	const [projectRankings, setProjectRankings] = useState<
		LeaderboardEntryResponse[]
	>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	// Leaderboard toggle state
	const [leaderboardType, setLeaderboardType] = useState<
		"personal" | "project"
	>("personal");
	const [selectedProjectSlug, setSelectedProjectSlug] = useState("");
	const [projects, setProjects] = useState<
		Array<{ id: string; name: string; projectSlug: string }>
	>([]);
	const [projectsLoading, setProjectsLoading] = useState(false);

	// Project leaderboard data state
	const [projectLeaderboardData, setProjectLeaderboardData] = useState<
		LeaderboardEntryResponse[]
	>([]);
	const [projectLeaderboardLoading, setProjectLeaderboardLoading] =
		useState(false);

	// Load projects for project leaderboard
	useEffect(() => {
		const loadProjects = async () => {
			try {
				setProjectsLoading(true);
				const projectsData = await projectService.getProjects();
				setProjects(
					projectsData.content.map((p) => ({
						id: p.id,
						name: p.name,
						projectSlug: p.projectSlug,
					}))
				);
				console.log(
					"GamificationDashboard: Loaded projects for leaderboard toggle",
					projectsData.content.length
				);
			} catch (err) {
				console.error("GamificationDashboard: Failed to load projects:", err);
			} finally {
				setProjectsLoading(false);
			}
		};

		loadProjects();
	}, []);

	// Load project leaderboard data when project is selected
	useEffect(() => {
		if (leaderboardType === "project" && selectedProjectSlug) {
			loadProjectLeaderboard();
		}
	}, [leaderboardType, selectedProjectSlug]);

	const loadProjectLeaderboard = async () => {
		if (!selectedProjectSlug) return;

		try {
			setProjectLeaderboardLoading(true);
			console.log(
				"GamificationDashboard: Loading project leaderboard for project:",
				selectedProjectSlug
			);

			// Get project ID from slug
			const project = projects.find(
				(p) => p.projectSlug === selectedProjectSlug
			);
			if (!project) {
				console.error(
					"GamificationDashboard: Project not found for slug:",
					selectedProjectSlug
				);
				return;
			}

			// Fetch project leaderboard data
			const response = await gamificationService.getProjectLeaderboard(
				project.id,
				"all-time",
				0,
				50
			);
			setProjectLeaderboardData(response.content);
			console.log(
				"GamificationDashboard: Loaded project leaderboard data:",
				response.content.length,
				"entries"
			);
		} catch (err) {
			console.error(
				"GamificationDashboard: Failed to load project leaderboard:",
				err
			);
			setProjectLeaderboardData([]);
		} finally {
			setProjectLeaderboardLoading(false);
		}
	};

	useEffect(() => {
		console.log(
			"GamificationDashboard: useEffect triggered for userId:",
			userId
		);
		const fetchDashboardData = async () => {
			try {
				console.log("GamificationDashboard: Starting to fetch data...");
				setLoading(true);
				const [points, streak, transactions] = await Promise.all([
					gamificationService.getUserPoints(userId),
					gamificationService.getUserStreak(userId),
					gamificationService.getPointHistory(userId, 0, 5), // Last 5 transactions
				]);

				console.log("GamificationDashboard: Data fetched successfully:", {
					points,
					streak,
					transactions: transactions.content,
				});

				setUserPoints(points);
				setStreakInfo(streak);
				setRecentTransactions(transactions.content);

				// For now, we'll show a placeholder for project rankings
				// This would be enhanced when we have project data
				setProjectRankings([]);
			} catch (err) {
				console.error(
					"GamificationDashboard: Failed to fetch gamification data:",
					err
				);
				console.error("GamificationDashboard: Error details:", {
					message: err instanceof Error ? err.message : "Unknown error",
					stack: err instanceof Error ? err.stack : "No stack trace",
					userId: userId,
				});
				setError(
					err instanceof Error
						? err.message
						: "Failed to load gamification data"
				);
			} finally {
				setLoading(false);
			}
		};

		fetchDashboardData();
	}, [userId]);

	// Separate useEffect for welcome bonus notification to avoid race conditions
	// NOTE: This is the ONLY notification that should appear when navigating to Leaderboard
	// All other notifications (bug resolution, penalties) are shown immediately on the Bug Detail Page
	useEffect(() => {
		if (!userPoints) {
			console.log(
				"GamificationDashboard: No user points data yet, skipping welcome bonus check"
			);
			return;
		}

		console.log(
			"GamificationDashboard: Checking welcome bonus from transaction history:",
			{
				userPoints: userPoints,
				totalPoints: userPoints.totalPoints,
				recentTransactions: recentTransactions.length,
				hasWelcomeBonusTransaction: recentTransactions.some((transaction) =>
					isWelcomeBonus(transaction.reason)
				),
				sessionStorage: sessionStorage.getItem(`welcomeBonusShown_${userId}`),
			}
		);

		// Check welcome bonus from transaction history to determine if user is actually new
		// Fixed: Check for welcome bonus transaction instead of totalPoints >= 0 (which is always true)
		const welcomeBonusShown = sessionStorage.getItem(
			`welcomeBonusShown_${userId}`
		);

		// Check if user has a welcome bonus transaction (indicates they are a new user)
		const hasWelcomeBonusTransaction = recentTransactions.some((transaction) =>
			isWelcomeBonus(transaction.reason)
		);

		const hasWelcomeBonus = hasWelcomeBonusTransaction && !welcomeBonusShown;

		if (hasWelcomeBonus) {
			console.log(
				"GamificationDashboard: Showing welcome bonus notification for new user with welcome bonus transaction"
			);
			sessionStorage.setItem(`welcomeBonusShown_${userId}`, "true");
			setTimeout(() => {
				PointNotificationService.showWelcomeBonus();
			}, 500);
		} else {
			console.log(
				"GamificationDashboard: Not showing welcome bonus notification. Reason:",
				!hasWelcomeBonusTransaction
					? "No welcome bonus transaction found (user is not new)"
					: `Already shown in this session (sessionStorage: ${welcomeBonusShown})`
			);
		}
	}, [userPoints, userId]);

	// Check for recent penalty notifications that the user should see
	// This handles cases where the user was offline when penalized
	useEffect(() => {
		if (!recentTransactions.length) return;

		// Look for recent penalty transactions (within last 24 hours)
		const recentPenalties = recentTransactions.filter((transaction) => {
			const isPenalty = isBugReopened(transaction.reason);
			const isRecent =
				new Date(transaction.earnedAt).getTime() >
				Date.now() - 24 * 60 * 60 * 1000; // 24 hours
			return isPenalty && isRecent;
		});

		// Show penalty notifications for recent penalties
		recentPenalties.forEach((penalty) => {
			const penaltyKey = `penaltyShown_${penalty.transactionId}`;
			const alreadyShown = sessionStorage.getItem(penaltyKey);

			if (!alreadyShown) {
				console.log(
					"GamificationDashboard: Showing penalty notification for recent bug reopening",
					{
						transactionId: penalty.transactionId,
						reason: penalty.reason,
						points: penalty.points,
					}
				);

				// Mark as shown
				sessionStorage.setItem(penaltyKey, "true");

				// Show penalty notification
				setTimeout(() => {
					PointNotificationService.showBugPenalty(
						penalty.points,
						extractProjectName(penalty.reason),
						extractTicketNumber(penalty.reason)
					);
				}, 1000); // Small delay to ensure welcome bonus shows first
			}
		});
	}, [recentTransactions]);

	if (loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<div className="flex-1 p-8">
					<div className="container mx-auto max-w-7xl">
						<div className="text-center">Loading gamification dashboard...</div>
					</div>
				</div>
			</div>
		);
	}

	if (error) {
		return (
			<div className="min-h-screen flex flex-col">
				<div className="flex-1 p-8">
					<div className="container mx-auto max-w-7xl">
						<div className="text-center text-red-600">Error: {error}</div>
					</div>
				</div>
			</div>
		);
	}

	if (!userPoints || !streakInfo) {
		return (
			<div className="min-h-screen flex flex-col">
				<div className="flex-1 p-8">
					<div className="container mx-auto max-w-7xl">
						<div className="text-center">No gamification data available</div>
					</div>
				</div>
			</div>
		);
	}

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />
			<main className="flex-1">
				<div className="container mx-auto px-4 py-4">
					{/* Page Header */}
					<div className="mb-8">
						<h1 className="text-3xl font-bold mb-2">Leaderboard & Points</h1>
						<p className="text-muted-foreground">
							Track your progress, points, and rankings
						</p>
					</div>

					{/* Breadcrumb Navigation */}
					<div className="mb-6">
						<LegacyBreadcrumb
							items={[
								{ label: "Home", href: "/" },
								{ label: "Leaderboard", href: "/leaderboard" },
							]}
						/>
					</div>

					{/* Leaderboard Type Toggle */}
					<div className="mb-6">
						<div className="flex items-center gap-4">
							<label className="text-sm font-medium">Leaderboard Type:</label>
							<div className="flex items-center space-x-2">
								<Button
									variant={
										leaderboardType === "personal" ? "default" : "outline"
									}
									size="sm"
									onClick={() => setLeaderboardType("personal")}
									className="min-w-[120px]"
								>
									<Trophy className="h-4 w-4 mr-2" />
									Personal
								</Button>
								<Button
									variant={
										leaderboardType === "project" ? "default" : "outline"
									}
									size="sm"
									onClick={() => setLeaderboardType("project")}
									className="min-w-[120px]"
								>
									<Bug className="h-4 w-4 mr-2" />
									Project
								</Button>
							</div>

							{/* Project Dropdown (only show when project leaderboard is selected) */}
							{leaderboardType === "project" && (
								<div className="flex items-center gap-2">
									<label className="text-sm font-medium">Select Project:</label>
									<select
										value={selectedProjectSlug}
										onChange={(e) => setSelectedProjectSlug(e.target.value)}
										className="px-3 py-2 border border-input rounded-md bg-background text-sm"
										disabled={projectsLoading}
									>
										<option value="">Choose a project</option>
										{projects.map((project) => (
											<option key={project.id} value={project.projectSlug}>
												{project.name}
											</option>
										))}
									</select>
								</div>
							)}
						</div>
					</div>

					{/* Conditional Content Based on Leaderboard Type */}
					{leaderboardType === "personal" ? (
						<>
							{/* Stats Overview */}
							<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
								<Card>
									<CardContent className="p-6">
										<div className="flex items-center gap-3">
											<Trophy className="h-8 w-8 text-yellow-500" />
											<div>
												<div className="text-2xl font-bold">
													{userPoints.totalPoints}
												</div>
												<div className="text-sm text-muted-foreground">
													Total Points
												</div>
											</div>
										</div>
									</CardContent>
								</Card>

								<Card>
									<CardContent className="p-6">
										<div className="flex items-center gap-3">
											<Flame className="h-8 w-8 text-orange-500" />
											<div>
												<div className="text-2xl font-bold">
													{streakInfo.currentStreak}
												</div>
												<div className="text-sm text-muted-foreground">
													Current Streak
												</div>
											</div>
										</div>
									</CardContent>
								</Card>

								<Card>
									<CardContent className="p-6">
										<div className="flex items-center gap-3">
											<Bug className="h-8 w-8 text-green-500" />
											<div>
												<div className="text-2xl font-bold">
													{userPoints.bugsResolved}
												</div>
												<div className="text-sm text-muted-foreground">
													Bugs Resolved
												</div>
											</div>
										</div>
									</CardContent>
								</Card>

								<Card>
									<CardContent className="p-6">
										<div className="flex items-center gap-3">
											<Target className="h-8 w-8 text-blue-500" />
											<div>
												<div className="text-2xl font-bold">
													{streakInfo.maxStreak}
												</div>
												<div className="text-sm text-muted-foreground">
													Best Streak
												</div>
											</div>
										</div>
									</CardContent>
								</Card>
							</div>

							{/* Main Content Grid */}
							<div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
								{/* Left Column - Recent Activity */}
								<div className="lg:col-span-2 space-y-6">
									{/* Recent Points Activity */}
									<Card>
										<CardHeader>
											<CardTitle className="flex items-center gap-2">
												<Zap className="h-5 w-5 text-yellow-500" />
												Recent Activity
											</CardTitle>
										</CardHeader>
										<CardContent>
											{recentTransactions.length > 0 ? (
												<div className="space-y-3">
													{recentTransactions.map((transaction) => (
														<div
															key={transaction.transactionId}
															className="flex items-center justify-between p-3 bg-muted/50 rounded-lg"
														>
															<div className="flex items-center gap-3">
																<Badge
																	variant={
																		transaction.points > 0
																			? "default"
																			: "destructive"
																	}
																>
																	{transaction.points > 0 ? "+" : ""}
																	{transaction.points}
																</Badge>
																<div>
																	<div className="font-medium">
																		{transaction.reason}
																	</div>
																	<div className="text-sm text-muted-foreground">
																		{new Date(
																			transaction.earnedAt
																		).toLocaleDateString()}
																		{transaction.projectId && (
																			<span className="ml-2 text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
																				Project Activity
																			</span>
																		)}
																		{!transaction.projectId && (
																			<span className="ml-2 text-xs bg-green-100 text-green-800 px-2 py-1 rounded">
																				System Activity
																			</span>
																		)}
																	</div>
																</div>
															</div>
														</div>
													))}

													{/* View Full History Button */}
													<div className="pt-4 border-t border-muted">
														<Button
															asChild
															variant="outline"
															className="w-full"
														>
															<Link to={`/leaderboard/profile/${userId}`}>
																View Full History
																<ArrowRight className="ml-2 h-4 w-4" />
															</Link>
														</Button>
													</div>
												</div>
											) : (
												<div className="text-center py-8 text-muted-foreground">
													No recent activity
												</div>
											)}
										</CardContent>
									</Card>

									{/* Quick Actions */}
									{/* <Card>
										<CardHeader>
											<CardTitle>Quick Actions</CardTitle>
										</CardHeader>
										<CardContent>
											<div className="grid grid-cols-1 md:grid-cols-2 gap-4">
												<Button asChild className="w-full">
													<Link to="/leaderboard/profile">
														View Full Profile
														<ArrowRight className="ml-2 h-4 w-4" />
													</Link>
												</Button>
												<Button asChild variant="outline" className="w-full">
													<Link to="/projects">
														View Projects
														<ArrowRight className="ml-2 h-4 w-4" />
													</Link>
												</Button>
											</div>
										</CardContent>
									</Card> */}

									{/* Toast Test Component - Development Only */}
									{/* <Card>
										<CardHeader>
											<CardTitle>Toast Notification Tests</CardTitle>
										</CardHeader>
										<CardContent>
											<ToastTestComponent />
										</CardContent>
									</Card> */}
								</div>

								{/* Right Column - Streak Info */}
								<div className="space-y-6">
									{/* Streak Visualization */}
									<Card>
										<CardHeader>
											<CardTitle className="flex items-center gap-2">
												<Flame className="h-5 w-5 text-orange-500" />
												Current Streak
											</CardTitle>
										</CardHeader>
										<CardContent>
											<div className="text-center space-y-4">
												<div className="text-4xl font-bold text-orange-600">
													{streakInfo.currentStreak}
												</div>
												<div className="text-sm text-muted-foreground">
													{streakInfo.currentStreak === 1 ? "day" : "days"}
												</div>
												<div className="text-xs text-muted-foreground">
													Last login:{" "}
													{new Date(
														streakInfo.lastLoginDate
													).toLocaleDateString()}
												</div>
											</div>
										</CardContent>
									</Card>

									{/* Streak Goal */}
									<Card>
										<CardHeader>
											<CardTitle className="text-sm">Streak Goal</CardTitle>
										</CardHeader>
										<CardContent>
											<div className="space-y-2">
												<div className="flex justify-between text-sm">
													<span>Current</span>
													<span>{streakInfo.currentStreak}</span>
												</div>
												<div className="flex justify-between text-sm">
													<span>Best</span>
													<span>{streakInfo.maxStreak}</span>
												</div>
												<div className="w-full bg-muted rounded-full h-2">
													<div
														className="bg-orange-500 h-2 rounded-full transition-all duration-300"
														style={{
															width: `${Math.min(
																(streakInfo.currentStreak /
																	Math.max(streakInfo.maxStreak, 1)) *
																	100,
																100
															)}%`,
														}}
													/>
												</div>
											</div>
										</CardContent>
									</Card>
								</div>
							</div>
						</>
					) : (
						/* Project Leaderboard Content */
						<div className="space-y-6">
							{/* Project Leaderboard Header */}
							<div className="text-center mb-8">
								<h2 className="text-2xl font-bold mb-2">
									{selectedProjectSlug
										? `${
												projects.find(
													(p) => p.projectSlug === selectedProjectSlug
												)?.name || "Unknown Project"
										  } Leaderboard`
										: "Project Leaderboard"}
								</h2>
								<p className="text-muted-foreground">
									{selectedProjectSlug
										? "Rankings based on all-time points and bugs resolved"
										: "Select a project to view its leaderboard"}
								</p>
							</div>

							{/* Project Leaderboard Table */}
							{selectedProjectSlug ? (
								<Card>
									<CardHeader>
										<CardTitle className="flex items-center gap-2">
											<Trophy className="h-5 w-5 text-yellow-500" />
											Project Rankings
										</CardTitle>
									</CardHeader>
									<CardContent>
										{projectLeaderboardLoading ? (
											<div className="text-center py-8">
												<div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
												<p className="text-muted-foreground">
													Loading project leaderboard...
												</p>
											</div>
										) : projectLeaderboardData.length > 0 ? (
											<div className="overflow-x-auto">
												<table className="w-full">
													<thead>
														<tr className="border-b">
															<th className="text-left py-3 px-4 font-medium">
																Rank
															</th>
															<th className="text-left py-3 px-4 font-medium">
																User
															</th>
															<th className="text-left py-3 px-4 font-medium">
																All-Time Points
															</th>
															<th className="text-left py-3 px-4 font-medium">
																Bugs Resolved
															</th>
														</tr>
													</thead>
													<tbody>
														{projectLeaderboardData.map((entry, index) => (
															<tr
																key={entry.leaderboardEntryId}
																className="border-b hover:bg-muted/50"
															>
																<td className="py-3 px-4">
																	<div className="flex items-center gap-2">
																		{index === 0 && (
																			<Trophy className="h-4 w-4 text-yellow-500" />
																		)}
																		{index === 1 && (
																			<span className="text-gray-400">🥈</span>
																		)}
																		{index === 2 && (
																			<span className="text-orange-400">
																				🥉
																			</span>
																		)}
																		<span className="font-medium">
																			{index + 1}
																		</span>
																	</div>
																</td>
																<td className="py-3 px-4 font-medium">
																	{entry.userDisplayName}
																</td>
																<td className="py-3 px-4">
																	<Badge
																		variant="default"
																		className="bg-green-100 text-green-800"
																	>
																		{entry.allTimePoints} points
																	</Badge>
																</td>
																<td className="py-3 px-4">
																	<Badge variant="outline">
																		{entry.bugsResolved} bugs
																	</Badge>
																</td>
															</tr>
														))}
													</tbody>
												</table>
											</div>
										) : (
											<div className="text-center py-8 text-muted-foreground">
												<Bug className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
												<h3 className="text-lg font-medium mb-2">
													No Leaderboard Data
												</h3>
												<p>
													This project doesn't have any leaderboard entries yet.
												</p>
											</div>
										)}
									</CardContent>
								</Card>
							) : (
								<Card>
									<CardContent className="text-center py-12">
										<Bug className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
										<h3 className="text-lg font-medium mb-2">
											No Project Selected
										</h3>
										<p className="text-muted-foreground">
											Choose a project from the dropdown above to view its
											leaderboard
										</p>
									</CardContent>
								</Card>
							)}
						</div>
					)}
				</div>
			</main>
			<Footer />
		</div>
	);
}
