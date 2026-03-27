/**
 * LeaderboardComponent
 *
 * Project-specific leaderboard with timeframe selection.
 * Follows the specification requirements without over-engineering.
 */

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { Trophy, Medal, Award, Hash, TrendingUp, Users } from "lucide-react";
import { gamificationService } from "@/services/gamificationService";
import type {
	LeaderboardEntryResponse,
	LeaderboardTimeframe,
	PageResponse,
} from "@/types/gamification";

interface LeaderboardComponentProps {
	projectId: string;
	initialTimeframe?: LeaderboardTimeframe;
}

export default function LeaderboardComponent({
	projectId,
	initialTimeframe = "all-time",
}: LeaderboardComponentProps) {
	const [timeframe, setTimeframe] =
		useState<LeaderboardTimeframe>(initialTimeframe);
	const [leaderboardData, setLeaderboardData] =
		useState<PageResponse<LeaderboardEntryResponse> | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [currentPage, setCurrentPage] = useState(0);
	const [pageSize] = useState(20);

	useEffect(() => {
		fetchLeaderboard();
	}, [projectId, timeframe, currentPage, pageSize]);

	const fetchLeaderboard = async () => {
		try {
			setLoading(true);
			const data = await gamificationService.getProjectLeaderboard(
				projectId,
				timeframe,
				currentPage,
				pageSize
			);
			setLeaderboardData(data);
		} catch (err) {
			console.error("Failed to fetch leaderboard:", err);
			setError(
				err instanceof Error ? err.message : "Failed to load leaderboard"
			);
		} finally {
			setLoading(false);
		}
	};

	const handleTimeframeChange = (newTimeframe: string) => {
		setTimeframe(newTimeframe as LeaderboardTimeframe);
		setCurrentPage(0); // Reset to first page when changing timeframe
	};

	const getPointsForTimeframe = (entry: LeaderboardEntryResponse) => {
		switch (timeframe) {
			case "weekly":
				return entry.weeklyPoints;
			case "monthly":
				return entry.monthlyPoints;
			default:
				return entry.allTimePoints;
		}
	};

	const getTimeframeLabel = () => {
		switch (timeframe) {
			case "weekly":
				return "This Week";
			case "monthly":
				return "This Month";
			default:
				return "All Time";
		}
	};

	const getRankIcon = (rank: number) => {
		switch (rank) {
			case 1:
				return <Trophy className="h-5 w-5 text-yellow-500" />;
			case 2:
				return <Medal className="h-5 w-5 text-gray-400" />;
			case 3:
				return <Award className="h-5 w-5 text-amber-600" />;
			default:
				return <Hash className="h-4 w-4 text-muted-foreground" />;
		}
	};

	const getRankColor = (rank: number) => {
		switch (rank) {
			case 1:
				return "bg-yellow-100 text-yellow-800 border-yellow-200";
			case 2:
				return "bg-gray-100 text-gray-800 border-gray-200";
			case 3:
				return "bg-amber-100 text-amber-800 border-amber-200";
			default:
				return "bg-muted text-muted-foreground border-border";
		}
	};

	if (loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<div className="flex-1 p-8">
					<div className="container mx-auto max-w-7xl">
						<div className="text-center">Loading leaderboard...</div>
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

	return (
		<div className="min-h-screen flex flex-col">
			<main className="flex-1">
				<div className="container mx-auto px-4 py-4">
					{/* Page Header */}
					<div className="mb-8">
						<h1 className="text-3xl font-bold mb-2">Project Leaderboard</h1>
						<p className="text-muted-foreground">
							{getTimeframeLabel()} rankings for project members
						</p>
					</div>

					{/* Timeframe Selector */}
					<Card className="mb-6">
						<CardContent className="p-6">
							<div className="flex items-center justify-between">
								<div className="flex items-center gap-4">
									<label className="text-sm font-medium">Timeframe:</label>
									<Select
										value={timeframe}
										onValueChange={handleTimeframeChange}
									>
										<SelectTrigger className="w-48">
											<SelectValue />
										</SelectTrigger>
										<SelectContent>
											<SelectItem value="weekly">Weekly</SelectItem>
											<SelectItem value="monthly">Monthly</SelectItem>
											<SelectItem value="all-time">All Time</SelectItem>
										</SelectContent>
									</Select>
								</div>
								<div className="flex items-center gap-2 text-sm text-muted-foreground">
									<Users className="h-4 w-4" />
									{leaderboardData?.totalElements || 0} participants
								</div>
							</div>
						</CardContent>
					</Card>

					{/* Leaderboard Table */}
					<Card>
						<CardHeader>
							<CardTitle className="flex items-center gap-2">
								<TrendingUp className="h-5 w-5" />
								{getTimeframeLabel()} Rankings
							</CardTitle>
						</CardHeader>
						<CardContent>
							{leaderboardData && leaderboardData.content.length > 0 ? (
								<>
									<Table>
										<TableHeader>
											<TableRow>
												<TableHead className="w-20">Rank</TableHead>
												<TableHead>User</TableHead>
												<TableHead className="text-right">Points</TableHead>
												<TableHead className="text-right">
													Bugs Resolved
												</TableHead>
												<TableHead className="text-right">
													Current Streak
												</TableHead>
											</TableRow>
										</TableHeader>
										<TableBody>
											{leaderboardData.content.map((entry) => (
												<TableRow key={entry.leaderboardEntryId}>
													<TableCell>
														<div className="flex items-center gap-2">
															{getRankIcon(entry.rank)}
															<Badge
																variant="outline"
																className={`${getRankColor(
																	entry.rank
																)} font-semibold`}
															>
																{entry.rank}
															</Badge>
														</div>
													</TableCell>
													<TableCell>
														<div className="font-medium">
															{entry.userDisplayName || "Unknown User"}
														</div>
													</TableCell>
													<TableCell className="text-right font-semibold">
														{getPointsForTimeframe(entry).toLocaleString()}
													</TableCell>
													<TableCell className="text-right">
														{entry.bugsResolved}
													</TableCell>
													<TableCell className="text-right">
														<div className="flex items-center justify-end gap-1">
															<Badge variant="outline" className="text-xs">
																{entry.currentStreak} days
															</Badge>
														</div>
													</TableCell>
												</TableRow>
											))}
										</TableBody>
									</Table>

									{/* Pagination */}
									{leaderboardData.totalPages > 1 && (
										<div className="flex items-center justify-between mt-6">
											<div className="text-sm text-muted-foreground">
												Page {leaderboardData.number + 1} of{" "}
												{leaderboardData.totalPages}
											</div>
											<div className="flex gap-2">
												<Button
													variant="outline"
													size="sm"
													onClick={() =>
														setCurrentPage(Math.max(0, currentPage - 1))
													}
													disabled={leaderboardData.first}
												>
													Previous
												</Button>
												<Button
													variant="outline"
													size="sm"
													onClick={() => setCurrentPage(currentPage + 1)}
													disabled={leaderboardData.last}
												>
													Next
												</Button>
											</div>
										</div>
									)}
								</>
							) : (
								<div className="text-center py-12 text-muted-foreground">
									<Users className="h-12 w-12 mx-auto mb-4 text-muted-foreground/50" />
									<p className="text-lg font-medium">
										No leaderboard data available
									</p>
									<p className="text-sm">
										Start earning points to appear on the leaderboard!
									</p>
								</div>
							)}
						</CardContent>
					</Card>
				</div>
			</main>
		</div>
	);
}
