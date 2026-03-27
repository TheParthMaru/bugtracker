import React from "react";
import { DuplicateAnalyticsResponse } from "@/types/similarity";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { BarChart3, Users, FileText, TrendingUp } from "lucide-react";

interface DuplicateAnalyticsDashboardProps {
	analytics: DuplicateAnalyticsResponse;
	className?: string;
}

/**
 * Component for displaying duplicate analytics dashboard.
 *
 * This component shows:
 * - Total duplicate count
 * - Duplicates by detection method
 * - Duplicates by user who marked them
 */
export const DuplicateAnalyticsDashboard: React.FC<
	DuplicateAnalyticsDashboardProps
> = ({ analytics, className = "" }) => {
	if (!analytics.hasDuplicates()) {
		return (
			<Card className={className}>
				<CardContent className="p-6 text-center text-muted-foreground">
					<FileText className="w-8 h-8 mx-auto mb-2 text-muted-foreground/50" />
					<p>No duplicates found in this project</p>
				</CardContent>
			</Card>
		);
	}

	return (
		<div className={`space-y-4 ${className}`}>
			{/* Summary Card */}
			<Card>
				<CardHeader className="pb-3">
					<CardTitle className="text-lg flex items-center gap-2">
						<BarChart3 className="w-5 h-5" />
						Duplicate Analytics Overview
					</CardTitle>
				</CardHeader>
				<CardContent>
					<div className="grid grid-cols-1 md:grid-cols-3 gap-4">
						<div className="text-center">
							<div className="text-2xl font-bold text-destructive">
								{analytics.totalDuplicates}
							</div>
							<div className="text-sm text-muted-foreground">
								Total Duplicates
							</div>
						</div>

						<div className="text-center">
							<div className="text-2xl font-bold text-blue-600">
								{analytics.getManualDuplicates()}
							</div>
							<div className="text-sm text-muted-foreground">Manual</div>
						</div>

						<div className="text-center">
							<div className="text-2xl font-bold text-green-600">
								{analytics.getAutomaticDuplicates()}
							</div>
							<div className="text-sm text-muted-foreground">Automatic</div>
						</div>
					</div>
				</CardContent>
			</Card>

			{/* Detection Method Breakdown */}
			{analytics.hasDetectionMethodBreakdown() && (
				<Card>
					<CardHeader className="pb-3">
						<CardTitle className="text-base flex items-center gap-2">
							<TrendingUp className="w-4 h-4" />
							By Detection Method
						</CardTitle>
					</CardHeader>
					<CardContent>
						<div className="space-y-2">
							{Object.entries(analytics.duplicatesByDetectionMethod).map(
								([method, count]) => (
									<div
										key={method}
										className="flex items-center justify-between"
									>
										<Badge variant="outline" className="capitalize">
											{method.toLowerCase()}
										</Badge>
										<span className="font-medium">{count}</span>
									</div>
								)
							)}
						</div>
					</CardContent>
				</Card>
			)}

			{/* User Breakdown */}
			{analytics.hasUserBreakdown() && (
				<Card>
					<CardHeader className="pb-3">
						<CardTitle className="text-base flex items-center gap-2">
							<Users className="w-4 h-4" />
							By User
						</CardTitle>
					</CardHeader>
					<CardContent>
						<div className="space-y-2">
							{Object.entries(analytics.duplicatesByUser)
								.sort(([, a], [, b]) => b - a) // Sort by count descending
								.map(([userName, count]) => (
									<div
										key={userName}
										className="flex items-center justify-between"
									>
										<span className="text-sm font-medium">{userName}</span>
										<Badge variant="secondary">{count}</Badge>
									</div>
								))}
						</div>
					</CardContent>
				</Card>
			)}
		</div>
	);
};
