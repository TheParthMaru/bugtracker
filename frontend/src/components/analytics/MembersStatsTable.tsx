import React from "react";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";

import type {
	TeamPerformanceStatistics,
	AssigneeStats,
} from "@/types/analytics";
import { cn } from "@/lib/utils";

interface MembersStatsTableProps {
	data: TeamPerformanceStatistics;
	className?: string;
}

export function MembersStatsTable({ data, className }: MembersStatsTableProps) {
	const calculateResolutionRate = (totalBugs: number, resolvedBugs: number) => {
		return totalBugs > 0 ? (resolvedBugs / totalBugs) * 100 : 0;
	};

	const getPerformanceColor = (rate: number) => {
		if (rate >= 80) return "text-green-600";
		if (rate >= 60) return "text-yellow-600";
		return "text-red-600";
	};

	return (
		<div className={cn("space-y-4", className)}>
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
								</TableRow>
							);
						})}
					</TableBody>
				</Table>
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
											<span
												className={cn(
													"text-sm font-medium px-2 py-1 rounded-full",
													efficiencyColor
												)}
											>
												{efficiency}
											</span>
										</TableCell>
									</TableRow>
								);
							})}
						</TableBody>
					</Table>
				</div>
			)}

			{/* Summary Statistics */}
			<div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
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
			</div>
		</div>
	);
}
