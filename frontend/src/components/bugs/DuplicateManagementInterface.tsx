import React, { useState, useEffect } from "react";
import {
	BugDuplicateSummary,
	DuplicateAnalyticsResponse,
	formatDate,
	getBugDisplayName,
} from "@/types/similarity";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { Download, Eye, FileText, Users, BarChart3 } from "lucide-react";
import { bugService } from "@/services/bugService";

interface DuplicateManagementInterfaceProps {
	projectSlug: string;
	className?: string;
}

/**
 * Component for managing duplicate bugs in a project.
 *
 * This component provides:
 * - List of all duplicate bugs
 * - Duplicate analytics
 * - Export functionality
 */
export const DuplicateManagementInterface: React.FC<
	DuplicateManagementInterfaceProps
> = ({ projectSlug, className = "" }) => {
	const [duplicates, setDuplicates] = useState<BugDuplicateSummary[]>([]);
	const [analytics, setAnalytics] = useState<DuplicateAnalyticsResponse | null>(
		null
	);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		loadDuplicateData();
	}, [projectSlug]);

	const loadDuplicateData = async () => {
		try {
			setLoading(true);
			setError(null);

			// Load analytics first
			const analyticsData = await bugService.getDuplicateAnalytics(projectSlug);
			setAnalytics(analyticsData);

			// Load duplicates if any exist
			if (analyticsData.hasDuplicates()) {
				// For now, we'll load duplicates from the first bug that has duplicates
				// In a real implementation, you might want a dedicated endpoint for all duplicates
				const duplicatesData = await bugService.getDuplicatesOfBug(
					projectSlug,
					1
				); // Placeholder
				setDuplicates(duplicatesData);
			}
		} catch (err) {
			setError(
				err instanceof Error ? err.message : "Failed to load duplicate data"
			);
		} finally {
			setLoading(false);
		}
	};

	const handleExportCSV = () => {
		if (!duplicates.length) return;

		const headers = [
			"Bug ID",
			"Project Ticket #",
			"Title",
			"Status",
			"Priority",
			"Assignee",
			"Reporter",
			"Created At",
			"Marked as Duplicate At",
			"Marked By User",
		];

		const csvContent = [
			headers.join(","),
			...duplicates.map((duplicate) =>
				[
					duplicate.id,
					duplicate.projectTicketNumber,
					`"${duplicate.title.replace(/"/g, '""')}"`,
					duplicate.status,
					duplicate.priority,
					duplicate.assigneeName || "",
					duplicate.reporterName || "",
					duplicate.createdAt,
					duplicate.markedAsDuplicateAt,
					duplicate.markedByUserName,
				].join(",")
			),
		].join("\n");

		const blob = new Blob([csvContent], { type: "text/csv" });
		const url = window.URL.createObjectURL(blob);
		const a = document.createElement("a");
		a.href = url;
		a.download = `duplicate-bugs-${projectSlug}-${
			new Date().toISOString().split("T")[0]
		}.csv`;
		a.click();
		window.URL.revokeObjectURL(url);
	};

	const handleViewBug = (bugId: number) => {
		const bugUrl = `/api/bugtracker/v1/projects/${projectSlug}/bugs/${bugId}`;
		window.open(bugUrl, "_blank");
	};

	if (loading) {
		return (
			<Card className={className}>
				<CardContent className="p-6 text-center">
					<div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
					<p className="mt-2 text-muted-foreground">
						Loading duplicate data...
					</p>
				</CardContent>
			</Card>
		);
	}

	if (error) {
		return (
			<Card className={className}>
				<CardContent className="p-6 text-center text-destructive">
					<p>Error: {error}</p>
					<Button
						onClick={loadDuplicateData}
						variant="outline"
						className="mt-2"
					>
						Retry
					</Button>
				</CardContent>
			</Card>
		);
	}

	if (!analytics?.hasDuplicates()) {
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
		<div className={`space-y-6 ${className}`}>
			{/* Header with Export */}
			<div className="flex items-center justify-between">
				<div>
					<h2 className="text-2xl font-bold flex items-center gap-2">
						<Users className="w-6 h-6" />
						Duplicate Management
					</h2>
					<p className="text-muted-foreground">
						Manage and analyze duplicate bugs in this project
					</p>
				</div>

				<Button onClick={handleExportCSV} disabled={!duplicates.length}>
					<Download className="w-4 h-4 mr-2" />
					Export CSV
				</Button>
			</div>

			{/* Analytics Summary */}
			{analytics && (
				<Card>
					<CardHeader>
						<CardTitle className="flex items-center gap-2">
							<BarChart3 className="w-5 h-5" />
							Quick Statistics
						</CardTitle>
					</CardHeader>
					<CardContent>
						<div className="grid grid-cols-1 md:grid-cols-4 gap-4">
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

							<div className="text-center">
								<div className="text-2xl font-bold text-purple-600">
									{Object.keys(analytics.duplicatesByUser).length}
								</div>
								<div className="text-sm text-muted-foreground">Users</div>
							</div>
						</div>
					</CardContent>
				</Card>
			)}

			{/* Duplicates Table */}
			{duplicates.length > 0 && (
				<Card>
					<CardHeader>
						<CardTitle className="flex items-center gap-2">
							<FileText className="w-5 h-5" />
							Duplicate Bugs ({duplicates.length})
						</CardTitle>
					</CardHeader>
					<CardContent>
						<Table>
							<TableHeader>
								<TableRow>
									<TableHead>Bug</TableHead>
									<TableHead>Status</TableHead>
									<TableHead>Priority</TableHead>
									<TableHead>Assignee</TableHead>
									<TableHead>Marked By</TableHead>
									<TableHead>Marked At</TableHead>
									<TableHead>Actions</TableHead>
								</TableRow>
							</TableHeader>
							<TableBody>
								{duplicates.map((duplicate) => (
									<TableRow key={duplicate.id}>
										<TableCell>
											<div>
												<div className="font-medium">
													{getBugDisplayName(duplicate)}
												</div>
												<div className="text-sm text-muted-foreground">
													Created {formatDate(duplicate.createdAt)}
												</div>
											</div>
										</TableCell>
										<TableCell>
											<Badge variant="outline">{duplicate.status}</Badge>
										</TableCell>
										<TableCell>
											<Badge variant="secondary">{duplicate.priority}</Badge>
										</TableCell>
										<TableCell>
											{duplicate.assigneeName || "Unassigned"}
										</TableCell>
										<TableCell>
											<Badge variant="outline">
												{duplicate.markedByUserName}
											</Badge>
										</TableCell>
										<TableCell>
											{formatDate(duplicate.markedAsDuplicateAt)}
										</TableCell>
										<TableCell>
											<Button
												variant="ghost"
												size="sm"
												onClick={() => handleViewBug(duplicate.id)}
											>
												<Eye className="w-4 h-4 mr-1" />
												View
											</Button>
										</TableCell>
									</TableRow>
								))}
							</TableBody>
						</Table>
					</CardContent>
				</Card>
			)}
		</div>
	);
};
