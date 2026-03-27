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
	TeamBugStats,
} from "@/types/analytics";
import { cn } from "@/lib/utils";

interface TeamStatsTableProps {
	data: TeamPerformanceStatistics;
	className?: string;
}

export function TeamStatsTable({ data, className }: TeamStatsTableProps) {
	const getPerformanceColor = (rate: number) => {
		if (rate >= 80) return "text-green-600";
		if (rate >= 60) return "text-yellow-600";
		return "text-red-600";
	};

	return (
		<div className={cn("space-y-4", className)}>
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
						</TableRow>
					</TableHeader>
					<TableBody>
						{data.teamBugStats?.map((team) => {
							const resolutionRate = team.resolutionRate * 100;
							return (
								<TableRow key={team.teamId}>
									<TableCell className="font-medium">{team.teamName}</TableCell>
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

			{/* Summary Statistics */}
			<div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
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
