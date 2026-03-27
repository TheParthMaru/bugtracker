/**
 * UserGamificationProfile Component
 *
 * User statistics display with point history and streak information.
 * Follows the specification requirements without over-engineering.
 */

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { LegacyBreadcrumb } from "@/components/ui/breadcrumb";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import {
	Trophy,
	Flame,
	Bug,
	Calendar,
	TrendingUp,
	Zap,
	ArrowUp,
	ArrowDown,
	Clock,
} from "lucide-react";
import { gamificationService } from "@/services/gamificationService";
import { PointNotificationService } from "@/services/pointNotificationService";
import StreakVisualizationComponent from "./StreakVisualizationComponent";
import {
	isWelcomeBonus,
	isBugResolution,
	isDailyLogin,
	isBugReopened,
	TransactionReason,
} from "@/types/gamification-enums";
import type {
	UserPointsResponse,
	StreakInfoResponse,
	PointTransactionResponse,
	PageResponse,
} from "@/types/gamification";

interface UserGamificationProfileProps {
	userId: string;
}

export default function UserGamificationProfile({
	userId,
}: UserGamificationProfileProps) {
	const [userPoints, setUserPoints] = useState<UserPointsResponse | null>(null);
	const [streakInfo, setStreakInfo] = useState<StreakInfoResponse | null>(null);
	const [pointHistory, setPointHistory] =
		useState<PageResponse<PointTransactionResponse> | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [currentPage, setCurrentPage] = useState(0);
	const [pageSize] = useState(20);

	useEffect(() => {
		fetchUserProfile();
	}, [userId, currentPage, pageSize]);

	const fetchUserProfile = async () => {
		try {
			setLoading(true);
			const [points, streak, history] = await Promise.all([
				gamificationService.getUserPoints(userId),
				gamificationService.getUserStreak(userId),
				gamificationService.getPointHistory(userId, currentPage, pageSize),
			]);

			setUserPoints(points);
			setStreakInfo(streak);
			setPointHistory(history);

			// Check for welcome bonus notification (only show once per session)
			console.log(
				"🔍 UserGamificationProfile: Checking for welcome bonus in history:",
				history.content
			);

			const hasWelcomeBonus = history.content.some((transaction) => {
				const includesWelcomeBonus = isWelcomeBonus(transaction.reason);
				console.log(
					"🔍 UserGamificationProfile: Transaction reason:",
					transaction.reason,
					"includes welcome bonus:",
					includesWelcomeBonus
				);
				return includesWelcomeBonus;
			});

			console.log(
				"🔍 UserGamificationProfile: Has welcome bonus:",
				hasWelcomeBonus
			);
			console.log(
				"🔍 UserGamificationProfile: Session storage check:",
				sessionStorage.getItem(`welcomeBonusShown_${userId}`)
			);

			if (
				hasWelcomeBonus &&
				!sessionStorage.getItem(`welcomeBonusShown_${userId}`)
			) {
				console.log(
					"UserGamificationProfile: Showing welcome bonus notification"
				);
				// Mark as shown for this session
				sessionStorage.setItem(`welcomeBonusShown_${userId}`, "true");

				// Show welcome bonus notification
				setTimeout(() => {
					PointNotificationService.showWelcomeBonus();
				}, 500);
			} else {
				console.log(
					"🔍 UserGamificationProfile: Not showing welcome bonus notification. Reason:",
					!hasWelcomeBonus
						? "No welcome bonus transaction found"
						: "Already shown in this session"
				);
			}
		} catch (err) {
			console.error("Failed to fetch user profile:", err);
			setError(
				err instanceof Error ? err.message : "Failed to load user profile"
			);
		} finally {
			setLoading(false);
		}
	};

	const getPointReasonIcon = (reason: string) => {
		if (isBugResolution(reason))
			return <Bug className="h-4 w-4 text-green-500" />;
		if (isDailyLogin(reason))
			return <Calendar className="h-4 w-4 text-blue-500" />;
		if (isBugReopened(reason))
			return <ArrowDown className="h-4 w-4 text-red-500" />;
		return <Zap className="h-4 w-4 text-yellow-500" />;
	};

	const getPointReasonColor = (points: number) => {
		if (points > 0) return "text-green-600";
		if (points < 0) return "text-red-600";
		return "text-muted-foreground";
	};

	if (loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<div className="flex-1 p-8">
					<div className="container mx-auto max-w-7xl">
						<div className="text-center">Loading user profile...</div>
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
						<h1 className="text-3xl font-bold mb-2">My Points & Stats</h1>
						<p className="text-muted-foreground">
							Your progress, points, and activity history
						</p>
					</div>

					{/* Breadcrumb Navigation */}
					<div className="mb-6">
						<LegacyBreadcrumb
							items={[
								{ label: "Home", href: "/" },
								{ label: "Leaderboard", href: "/leaderboard" },
								{ label: "My Profile", current: true },
							]}
						/>
					</div>

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
									<TrendingUp className="h-8 w-8 text-blue-500" />
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
					<div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
						{/* Left Column - Point History */}
						<div className="space-y-6">
							<Card>
								<CardHeader>
									<CardTitle className="flex items-center gap-2">
										<Zap className="h-5 w-5 text-yellow-500" />
										Point History
									</CardTitle>
								</CardHeader>
								<CardContent>
									{pointHistory && pointHistory.content.length > 0 ? (
										<>
											<Table>
												<TableHeader>
													<TableRow>
														<TableHead>Reason</TableHead>
														<TableHead className="text-right">Points</TableHead>
														<TableHead>Type</TableHead>
														<TableHead className="text-right">Date</TableHead>
													</TableRow>
												</TableHeader>
												<TableBody>
													{pointHistory.content.map((transaction) => (
														<TableRow key={transaction.transactionId}>
															<TableCell>
																<div className="flex items-center gap-2">
																	{getPointReasonIcon(transaction.reason)}
																	<span className="text-sm">
																		{transaction.reason}
																	</span>
																</div>
															</TableCell>
															<TableCell className="text-right">
																<Badge
																	variant={
																		transaction.points > 0
																			? "default"
																			: "destructive"
																	}
																	className="font-mono"
																>
																	{transaction.points > 0 ? "+" : ""}
																	{transaction.points}
																</Badge>
															</TableCell>
															<TableCell>
																{transaction.projectId ? (
																	<Badge
																		variant="outline"
																		className="bg-blue-50 text-blue-700 border-blue-200"
																	>
																		Project
																	</Badge>
																) : (
																	<Badge
																		variant="outline"
																		className="bg-green-50 text-green-700 border-green-200"
																	>
																		System
																	</Badge>
																)}
															</TableCell>
															<TableCell className="text-right text-sm text-muted-foreground">
																{new Date(
																	transaction.earnedAt
																).toLocaleDateString()}
															</TableCell>
														</TableRow>
													))}
												</TableBody>
											</Table>

											{/* Pagination */}
											{pointHistory.totalPages > 1 && (
												<div className="flex items-center justify-between mt-6">
													<div className="text-sm text-muted-foreground">
														Page {pointHistory.number + 1} of{" "}
														{pointHistory.totalPages}
													</div>
													<div className="flex gap-2">
														<Button
															variant="outline"
															size="sm"
															onClick={() =>
																setCurrentPage(Math.max(0, currentPage - 1))
															}
															disabled={pointHistory.first}
														>
															Previous
														</Button>
														<Button
															variant="outline"
															size="sm"
															onClick={() => setCurrentPage(currentPage + 1)}
															disabled={pointHistory.last}
														>
															Next
														</Button>
													</div>
												</div>
											)}
										</>
									) : (
										<div className="text-center py-8 text-muted-foreground">
											<Zap className="h-12 w-12 mx-auto mb-4 text-muted-foreground/50" />
											<p className="text-lg font-medium">
												No point history available
											</p>
											<p className="text-sm">
												Start earning points to see your activity!
											</p>
										</div>
									)}
								</CardContent>
							</Card>

							{/* Activity Summary */}
							<Card>
								<CardHeader>
									<CardTitle>Activity Summary</CardTitle>
								</CardHeader>
								<CardContent>
									<div className="space-y-4">
										<div className="flex items-center justify-between">
											<span className="text-sm text-muted-foreground">
												Last Activity
											</span>
											<span className="text-sm font-medium">
												{userPoints.lastActivity
													? new Date(
															userPoints.lastActivity
													  ).toLocaleDateString()
													: "Never"}
											</span>
										</div>
										<div className="flex items-center justify-between">
											<span className="text-sm text-muted-foreground">
												Total Transactions
											</span>
											<span className="text-sm font-medium">
												{pointHistory?.totalElements || 0}
											</span>
										</div>
									</div>
								</CardContent>
							</Card>
						</div>

						{/* Right Column - Streak Visualization */}
						<div className="space-y-6">
							<StreakVisualizationComponent
								userId={userId}
								currentStreak={streakInfo.currentStreak}
								maxStreak={streakInfo.maxStreak}
								lastLoginDate={streakInfo.lastLoginDate}
							/>

							{/* Streak Stats */}
							<Card>
								<CardHeader>
									<CardTitle className="text-sm">Streak Details</CardTitle>
								</CardHeader>
								<CardContent>
									<div className="space-y-3">
										<div className="flex items-center justify-between">
											<span className="text-sm text-muted-foreground">
												Current Streak
											</span>
											<Badge
												variant="outline"
												className="bg-orange-50 text-orange-700 border-orange-200"
											>
												{streakInfo.currentStreak} days
											</Badge>
										</div>
										<div className="flex items-center justify-between">
											<span className="text-sm text-muted-foreground">
												Best Streak
											</span>
											<Badge
												variant="outline"
												className="bg-blue-50 text-blue-700 border-blue-200"
											>
												{streakInfo.maxStreak} days
											</Badge>
										</div>
										<div className="flex items-center justify-between">
											<span className="text-sm text-muted-foreground">
												Last Login
											</span>
											<span className="text-sm font-medium">
												{new Date(
													streakInfo.lastLoginDate
												).toLocaleDateString()}
											</span>
										</div>
										{streakInfo.streakStartDate && (
											<div className="flex items-center justify-between">
												<span className="text-sm text-muted-foreground">
													Streak Started
												</span>
												<span className="text-sm font-medium">
													{new Date(
														streakInfo.streakStartDate
													).toLocaleDateString()}
												</span>
											</div>
										)}
									</div>
								</CardContent>
							</Card>
						</div>
					</div>
				</div>
			</main>
			<Footer />
		</div>
	);
}
