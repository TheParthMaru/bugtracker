/**
 * StreakVisualizationComponent
 *
 * Simplified streak visualization showing current streak, max streak, and weekly activity.
 * Follows the specification requirements without over-engineering.
 */

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Flame, TrendingUp, Calendar } from "lucide-react";
import type { StreakVisualizationComponentProps } from "@/types/gamification";

export default function StreakVisualizationComponent({
	userId,
	currentStreak,
	maxStreak,
	lastLoginDate,
}: StreakVisualizationComponentProps) {
	// Generate simple weekly activity data (last 4 weeks)
	const generateWeeklyActivity = () => {
		const weeks = [];
		const today = new Date();

		for (let i = 3; i >= 0; i--) {
			const weekStart = new Date(today);
			weekStart.setDate(today.getDate() - i * 7);

			// Simple logic: if user had activity in this week, show as active
			const weekEnd = new Date(weekStart);
			weekEnd.setDate(weekStart.getDate() + 6);

			const lastLogin = new Date(lastLoginDate);
			const isActive = lastLogin >= weekStart && lastLogin <= weekEnd;

			weeks.push({
				week: `Week ${4 - i}`,
				active: isActive,
				date: weekStart.toLocaleDateString("en-US", {
					month: "short",
					day: "numeric",
				}),
			});
		}

		return weeks;
	};

	const weeklyActivity = generateWeeklyActivity();

	return (
		<Card className="w-full">
			<CardHeader>
				<CardTitle className="flex items-center gap-2">
					<Flame className="h-5 w-5 text-orange-500" />
					Login Streak
				</CardTitle>
			</CardHeader>
			<CardContent className="space-y-6">
				{/* Streak Summary */}
				<div className="grid grid-cols-1 md:grid-cols-3 gap-4">
					<div className="text-center">
						<div className="text-2xl font-bold text-orange-600">
							{currentStreak}
						</div>
						<div className="text-sm text-muted-foreground">Current Streak</div>
					</div>
					<div className="text-center">
						<div className="text-2xl font-bold text-blue-600">{maxStreak}</div>
						<div className="text-sm text-muted-foreground">Best Streak</div>
					</div>
					<div className="text-center">
						<div className="text-2xl font-bold text-green-600">
							{weeklyActivity.filter((w) => w.active).length}
						</div>
						<div className="text-sm text-muted-foreground">Active Weeks</div>
					</div>
				</div>

				{/* Simple Weekly Activity */}
				<div className="space-y-3">
					<h4 className="text-sm font-medium flex items-center gap-2">
						<Calendar className="h-4 w-4" />
						Recent Activity
					</h4>
					<div className="grid grid-cols-4 gap-2">
						{weeklyActivity.map((week, index) => (
							<div key={index} className="text-center">
								<div
									className={`w-8 h-8 mx-auto rounded-lg border-2 flex items-center justify-center text-xs font-medium ${
										week.active
											? "bg-green-100 text-green-800 border-green-300"
											: "bg-muted text-muted-foreground border-border"
									}`}
								>
									{week.active ? "✓" : "○"}
								</div>
								<div className="text-xs text-muted-foreground mt-1">
									{week.week}
								</div>
							</div>
						))}
					</div>
				</div>

				{/* Streak Stats */}
				<div className="grid grid-cols-1 md:grid-cols-2 gap-4">
					<div className="flex items-center gap-3 p-3 bg-orange-50 rounded-lg border border-orange-200">
						<Flame className="h-5 w-5 text-orange-500" />
						<div>
							<div className="text-sm font-medium text-orange-800">
								{currentStreak > 0 ? "Keep it up!" : "Start your streak!"}
							</div>
							<div className="text-xs text-orange-600">
								{currentStreak > 0
									? `You're on a ${currentStreak}-day streak!`
									: "Log in tomorrow to start building your streak!"}
							</div>
						</div>
					</div>

					<div className="flex items-center gap-3 p-3 bg-blue-50 rounded-lg border border-blue-200">
						<TrendingUp className="h-5 w-5 text-blue-500" />
						<div>
							<div className="text-sm font-medium text-blue-800">
								{currentStreak === maxStreak
									? "New record!"
									: "Beat your best!"}
							</div>
							<div className="text-xs text-blue-600">
								{currentStreak === maxStreak
									? `You've matched your best streak of ${maxStreak} days!`
									: `Your best streak is ${maxStreak} days`}
							</div>
						</div>
					</div>
				</div>
			</CardContent>
		</Card>
	);
}
