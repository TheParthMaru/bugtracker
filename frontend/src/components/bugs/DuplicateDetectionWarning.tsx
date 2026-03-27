import React, { memo, useCallback } from "react";
import { BugSimilarityResult } from "@/types/similarity";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { AlertTriangle, Info, XCircle, CheckCircle } from "lucide-react";
import { formatSimilarityPercentage } from "@/types/similarity";

interface DuplicateDetectionWarningProps {
	similarBugs: BugSimilarityResult[];
	isChecking: boolean;
	projectSlug: string;
	onViewBug: (bugId: number) => void;
	onMarkAsDuplicate: (bugId: number) => void;
	onProceedAnyway: () => void;
	onDismiss: () => void;
	className?: string;
}

// Memoized component to prevent unnecessary re-renders
export const DuplicateDetectionWarning = memo<DuplicateDetectionWarningProps>(
	({
		similarBugs,
		isChecking,
		projectSlug,
		onViewBug,
		onMarkAsDuplicate,
		onProceedAnyway,
		onDismiss,
		className = "",
	}) => {
		// Early return if no similar bugs
		if (similarBugs.length === 0) {
			return null;
		}

		// Sort bugs by similarity score (highest first)
		const sortedBugs = [...similarBugs].sort(
			(a, b) => b.similarityScore - a.similarityScore
		);

		// Determine warning level based on highest similarity
		const highestSimilarity = sortedBugs[0]?.similarityScore || 0;

		let warningLevel: "info" | "warning" | "error" = "info";
		let warningIcon = <Info className="h-4 w-4" />;
		let warningTitle = "Similar bugs found";
		let warningVariant: "default" | "destructive" = "default";

		if (highestSimilarity >= 0.9) {
			warningLevel = "error";
			warningIcon = <XCircle className="h-4 w-4" />;
			warningTitle = "Potential duplicate detected";
			warningVariant = "destructive";
		} else if (highestSimilarity >= 0.7) {
			warningLevel = "warning";
			warningIcon = <AlertTriangle className="h-4 w-4" />;
			warningTitle = "Very similar bugs found";
			warningVariant = "destructive";
		} else if (highestSimilarity >= 0.4) {
			warningLevel = "info";
			warningIcon = <Info className="h-4 w-4" />;
			warningTitle = "Similar bugs found";
			warningVariant = "default";
		}

		// Memoized callback to prevent unnecessary re-renders
		const handleViewBug = useCallback(
			(bugId: number) => {
				onViewBug(bugId);
			},
			[onViewBug]
		);

		const handleMarkAsDuplicate = useCallback(
			(bugId: number) => {
				onMarkAsDuplicate(bugId);
			},
			[onMarkAsDuplicate]
		);

		const handleProceedAnyway = useCallback(() => {
			onProceedAnyway();
		}, [onProceedAnyway]);

		const handleDismiss = useCallback(() => {
			onDismiss();
		}, [onDismiss]);

		const handleShowAll = useCallback(() => {
			const url = `/api/bugtracker/v1/projects/${projectSlug}/similarity-analysis`;
			window.open(url, "_blank");
		}, [projectSlug]);

		return (
			<Alert variant={warningVariant} className={`mb-4 ${className}`}>
				<div className="flex items-start justify-between w-full">
					<div className="flex items-start space-x-3 flex-1">
						{warningIcon}
						<div className="flex-1">
							<h4 className="font-medium mb-2">{warningTitle}</h4>
							<AlertDescription className="mb-3">
								We found {similarBugs.length} bug
								{similarBugs.length !== 1 ? "s" : ""} that appear
								{similarBugs.length !== 1 ? "" : "s"} similar to what you're
								creating.
								{highestSimilarity >= 0.7 && " This might be a duplicate."}
							</AlertDescription>

							{/* Similar bugs table */}
							<div className="mb-3">
								<Table>
									<TableHeader>
										<TableRow>
											<TableHead className="w-20">Similarity</TableHead>
											<TableHead>Bug Details</TableHead>
											<TableHead className="w-20">Actions</TableHead>
										</TableRow>
									</TableHeader>
									<TableBody>
										{sortedBugs.slice(0, 3).map((bug) => (
											<TableRow key={bug.bugId}>
												<TableCell className="font-medium text-center">
													{formatSimilarityPercentage(bug.similarityScore)}
												</TableCell>
												<TableCell>
													<div className="space-y-1">
														<div className="font-medium text-gray-900">
															{bug.title}
														</div>
														<p className="text-sm text-gray-600 line-clamp-2">
															{bug.description}
														</p>
														<div className="text-xs text-gray-500">
															#{bug.projectTicketNumber} • {bug.status}
														</div>
													</div>
												</TableCell>
												<TableCell>
													<div className="flex items-center space-x-2">
														<Button
															variant="outline"
															size="sm"
															onClick={() => handleViewBug(bug.bugId)}
															className="flex-1"
														>
															View
														</Button>
														<Button
															variant="outline"
															size="sm"
															onClick={() => handleMarkAsDuplicate(bug.bugId)}
															className="flex-1"
														>
															Mark as Duplicate
														</Button>
													</div>
												</TableCell>
											</TableRow>
										))}
									</TableBody>
								</Table>

								{similarBugs.length > 3 && (
									<div className="text-sm text-gray-500 text-center py-2 mt-2">
										... and {similarBugs.length - 3} more similar bugs
										<Button
											variant="ghost"
											size="sm"
											onClick={handleShowAll}
											className="ml-2 text-blue-600 hover:text-blue-800 hover:bg-blue-50"
										>
											Show All
										</Button>
									</div>
								)}
							</div>

							{/* Action buttons */}
							<div className="flex items-center space-x-3">
								<Button
									variant="outline"
									size="sm"
									onClick={handleProceedAnyway}
									className="flex items-center space-x-2"
								>
									<CheckCircle className="h-4 w-4" />
									<span>
										{highestSimilarity >= 0.9
											? "Proceed Despite Similarity"
											: highestSimilarity >= 0.7
											? "Proceed Anyway"
											: "Proceed"}
									</span>
								</Button>

								<Button
									variant="ghost"
									size="sm"
									onClick={handleDismiss}
									className="text-gray-500 hover:text-gray-700"
								>
									Dismiss
								</Button>
							</div>

							{/* Additional info for high similarity */}
							{highestSimilarity >= 0.7 && (
								<div className="mt-3 p-2 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-800">
									<strong>Note:</strong> If this is indeed a duplicate, please
									consider marking it as such after creation to help maintain
									bug quality.
								</div>
							)}
						</div>
					</div>

					{/* Loading indicator */}
					{isChecking && (
						<div className="ml-4 flex-shrink-0">
							<div className="animate-spin rounded-full h-4 w-4 border-b-2 border-current"></div>
						</div>
					)}
				</div>
			</Alert>
		);
	}
);

// Set display name for debugging
DuplicateDetectionWarning.displayName = "DuplicateDetectionWarning";
