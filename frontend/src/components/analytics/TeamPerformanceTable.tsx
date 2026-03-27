import React from "react";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import type {
	TeamPerformanceStatistics,
	AssigneeStats,
	UserStats,
} from "@/types/analytics";
import { cn } from "@/lib/utils";

interface TeamPerformanceTableProps {
	data: TeamPerformanceStatistics;
	className?: string;
}

export function TeamPerformanceTable({
	data,
	className,
}: TeamPerformanceTableProps) {
	const calculateResolutionRate = (totalBugs: number, resolvedBugs: number) => {
		return totalBugs > 0 ? (resolvedBugs / totalBugs) * 100 : 0;
	};

	const getPerformanceColor = (rate: number) => {
		if (rate >= 80) return "text-green-600";
		if (rate >= 60) return "text-yellow-600";
		return "text-red-600";
	};

	const getPerformanceBadge = (rate: number) => {
		if (rate >= 80)
			return (
				<Badge variant="secondary" className="bg-green-100 text-green-800">
					Excellent
				</Badge>
			);
		if (rate >= 60)
			return (
				<Badge variant="secondary" className="bg-yellow-100 text-yellow-800">
					Good
				</Badge>
			);
		return (
			<Badge variant="secondary" className="bg-red-100 text-red-800">
				Needs Improvement
			</Badge>
		);
	};

	return (
		<div className={cn("space-y-4", className)}>
			<div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
				{/* Assignee Performance */}
				<div>
					<h4 className="text-lg font-semibold mb-4">Assignee Performance</h4>
					<Table>
						<TableHeader>
							<TableRow>
								<TableHead>Team Member</TableHead>
								<TableHead>Total Bugs</TableHead>
								<TableHead>Resolved</TableHead>
								<TableHead>Resolution Rate</TableHead>
							</TableRow>
						</TableHeader>
						<TableBody>
							{data.assigneeStats.map((assignee) => {
								const resolutionRate = calculateResolutionRate(
									assignee.count,
									assignee.resolvedCount
								);
								return (
									<TableRow key={assignee.userId}>
										<TableCell className="font-medium">
											{assignee.fullName}
										</TableCell>
										<TableCell>{assignee.count}</TableCell>
										<TableCell>{assignee.resolvedCount}</TableCell>
										<TableCell>
											<span
												className={cn(
													"text-sm font-medium",
													getPerformanceColor(resolutionRate)
												)}
											>
												{resolutionRate.toFixed(1)}%
											</span>
										</TableCell>
										<TableCell>{getPerformanceBadge(resolutionRate)}</TableCell>
									</TableRow>
								);
							})}
						</TableBody>
					</Table>
				</div>

				{/* Team Bug Statistics */}
				<div>
					<h4 className="text-lg font-semibold mb-4">Team Bug Assignments</h4>
					<Table>
						<TableHeader>
							<TableRow>
								<TableHead>Team Name</TableHead>
								<TableHead>Total Bugs</TableHead>
								<TableHead>Open Bugs</TableHead>
								<TableHead>Resolved</TableHead>
								<TableHead>Resolution Rate</TableHead>
								<TableHead>Performance</TableHead>
							</TableRow>
						</TableHeader>
						<TableBody>
							{data.teamBugStats?.map((team) => {
								const resolutionRate = team.resolutionRate * 100;
								return (
									<TableRow key={team.teamId}>
										<TableCell className="font-medium">
											<div>
												<div>{team.teamName}</div>
												{team.teamSlug && (
													<div className="text-sm text-muted-foreground">
														{team.teamSlug}
													</div>
												)}
											</div>
										</TableCell>
										<TableCell>{team.totalBugs}</TableCell>
										<TableCell>{team.openBugs}</TableCell>
										<TableCell>{team.resolvedBugs}</TableCell>
										<TableCell>
											<span
												className={cn(
													"text-sm font-medium",
													getPerformanceColor(resolutionRate)
												)}
											>
												{resolutionRate.toFixed(1)}%
											</span>
										</TableCell>
										<TableCell>{getPerformanceBadge(resolutionRate)}</TableCell>
									</TableRow>
								);
							}) || (
								<TableRow>
									<TableCell
										colSpan={6}
										className="text-center text-muted-foreground"
									>
										No team data available
									</TableCell>
								</TableRow>
							)}
						</TableBody>
					</Table>
				</div>
			</div>

			{/* Resolution Time Analysis */}
			{data.resolutionTimeStats.length > 0 && (
				<div>
					<h4 className="text-lg font-semibold mb-4">
						Average Resolution Time
					</h4>
					<Table>
						<TableHeader>
							<TableRow>
								<TableHead>Team Member</TableHead>
								<TableHead>Avg Resolution Time</TableHead>
								<TableHead>Efficiency</TableHead>
							</TableRow>
						</TableHeader>
						<TableBody>
							{data.resolutionTimeStats.map(([userId, avgTime]) => {
								const efficiency =
									avgTime < 3
										? "Excellent"
										: avgTime < 5
										? "Good"
										: "Needs Improvement";
								const efficiencyColor =
									avgTime < 3
										? "bg-green-100 text-green-800"
										: avgTime < 5
										? "bg-yellow-100 text-yellow-800"
										: "bg-red-100 text-red-800";

								return (
									<TableRow key={userId}>
										<TableCell className="font-medium">
											{userId}{" "}
											{/* TODO: Get user name when resolution time stats are updated */}
										</TableCell>
										<TableCell>
											<span
												className={cn(
													"font-medium",
													getPerformanceColor(
														avgTime < 5 ? 80 : avgTime < 7 ? 60 : 40
													)
												)}
											>
												{avgTime.toFixed(1)} days
											</span>
										</TableCell>
										<TableCell>
											<Badge variant="secondary" className={efficiencyColor}>
												{efficiency}
											</Badge>
										</TableCell>
									</TableRow>
								);
							})}
						</TableBody>
					</Table>
				</div>
			)}

			{/* Summary Statistics */}
			<div className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-6">
				<div className="bg-muted p-4 rounded-lg">
					<div className="text-2xl font-bold">
						{data.assigneeStats.reduce(
							(sum, assignee) => sum + assignee.count,
							0
						)}
					</div>
					<div className="text-sm text-muted-foreground">
						Total Assigned Bugs
					</div>
				</div>
				<div className="bg-muted p-4 rounded-lg">
					<div className="text-2xl font-bold">
						{data.assigneeStats.reduce(
							(sum, assignee) => sum + assignee.resolvedCount,
							0
						)}
					</div>
					<div className="text-sm text-muted-foreground">
						Total Resolved Bugs
					</div>
				</div>
				<div className="bg-muted p-4 rounded-lg">
					<div className="text-2xl font-bold">
						{data.teamBugStats?.reduce(
							(sum, team) => sum + team.totalBugs,
							0
						) || 0}
					</div>
					<div className="text-sm text-muted-foreground">Total Bugs</div>
				</div>
				<div className="bg-muted p-4 rounded-lg">
					<div className="text-2xl font-bold">
						{data.teamBugStats?.length || 0}
					</div>
					<div className="text-sm text-muted-foreground">Active Teams</div>
				</div>
			</div>
		</div>
	);
}
