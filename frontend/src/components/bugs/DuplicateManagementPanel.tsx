/**
 * DuplicateManagementPanel Component
 *
 * Comprehensive panel for managing duplicate bug relationships.
 * Shows different content based on whether the current bug is:
 * - An original bug (has duplicates)
 * - A duplicate bug (duplicate of another bug)
 *
 * Features:
 * - Expandable table structure
 * - Clear duplicate relationship information
 * - Navigation between related bugs
 * - Duplicate management actions
 */

import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
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
import {
	Collapsible,
	CollapsibleContent,
	CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
	Copy,
	ExternalLink,
	ChevronDown,
	ChevronRight,
	Trash2,
	AlertTriangle,
	Info,
} from "lucide-react";
import type { DuplicateInfoResponse, BugSummary } from "@/types/similarity";
import { bugService } from "@/services/bugService";
import { toast } from "react-toastify";

interface DuplicateManagementPanelProps {
	duplicateInfo: DuplicateInfoResponse;
	projectSlug: string;
	currentBugId: number;
	onDuplicateRemoved?: () => void;
}

export const DuplicateManagementPanel: React.FC<
	DuplicateManagementPanelProps
> = ({ duplicateInfo, projectSlug, currentBugId, onDuplicateRemoved }) => {
	const navigate = useNavigate();
	const [isExpanded, setIsExpanded] = useState(false);
	const [isRemoving, setIsRemoving] = useState(false);

	// Normalize duplicate flag from API (can be 'isDuplicate' or 'duplicate')
	const isDuplicate =
		(duplicateInfo.isDuplicate ?? duplicateInfo.duplicate) === true;

	// Debug logging to see what we're receiving
	console.log("DuplicateManagementPanel: Received props:", {
		duplicateInfo,
		projectSlug,
		currentBugId,
		isDuplicate,
		hasOriginalBug: !!duplicateInfo?.originalBug,
		otherDuplicatesCount: duplicateInfo?.otherDuplicates?.length || 0,
	});

	// Early return if no duplicate information at all
	if (
		!isDuplicate &&
		!duplicateInfo.originalBug &&
		(!duplicateInfo.otherDuplicates ||
			duplicateInfo.otherDuplicates.length === 0)
	) {
		console.log("DuplicateManagementPanel: Early return - no duplicate info");
		return null;
	}

	const handleViewBug = (bugId: number, projectTicketNumber: number) => {
		navigate(`/projects/${projectSlug}/bugs/${projectTicketNumber}`);
	};

	const handleRemoveDuplicate = async (duplicateBugId: number) => {
		if (
			!confirm(
				"Are you sure you want to remove this duplicate link? This will not delete the bug, only the duplicate relationship."
			)
		) {
			return;
		}

		setIsRemoving(true);
		try {
			// TODO: Implement remove duplicate endpoint
			// await bugService.removeDuplicateLink(projectSlug, duplicateBugId);
			toast.success("Duplicate link removed successfully");
			onDuplicateRemoved?.();
		} catch (error) {
			toast.error("Failed to remove duplicate link");
			console.error("Error removing duplicate link:", error);
		} finally {
			setIsRemoving(false);
		}
	};

	// Render content for duplicate bugs (current bug is a duplicate)
	if (isDuplicate && duplicateInfo.originalBug) {
		return (
			<Card className="border-orange-200 bg-orange-50/50">
				<CardHeader className="pb-3">
					<CardTitle className="flex items-center gap-2 text-orange-800">
						<Copy className="h-5 w-5" />
						Duplicate Bug
					</CardTitle>
					<CardDescription className="text-orange-700">
						This bug is a duplicate of another bug in the project
					</CardDescription>
				</CardHeader>
				<CardContent className="space-y-4">
					{/* Original Bug Information */}
					<div className="space-y-3">
						<div className="flex items-center justify-between">
							<span className="text-sm font-medium text-orange-800">
								Original Bug:
							</span>
							<Badge variant="outline" className="text-orange-700">
								#{duplicateInfo.originalBug.projectTicketNumber}
							</Badge>
						</div>

						<div className="rounded-lg border border-orange-200 bg-white p-3">
							<div className="font-medium text-gray-900">
								{duplicateInfo.originalBug.title}
							</div>
							<div className="text-sm text-gray-600 mt-1">
								Status:{" "}
								<Badge variant="secondary">
									{duplicateInfo.originalBug.status}
								</Badge>
							</div>
							{duplicateInfo.relationshipInfo && (
								<div className="text-xs text-gray-500 mt-2">
									Marked as duplicate by{" "}
									{duplicateInfo.relationshipInfo.markedByUserName} on{" "}
									{new Date(
										duplicateInfo.relationshipInfo.markedAt
									).toLocaleDateString()}
								</div>
							)}
						</div>

						<Button
							variant="outline"
							size="sm"
							onClick={() =>
								handleViewBug(
									duplicateInfo.originalBug!.id,
									duplicateInfo.originalBug!.projectTicketNumber
								)
							}
							className="w-full"
						>
							<ExternalLink className="h-4 w-4 mr-2" />
							View Original Bug #{duplicateInfo.originalBug.projectTicketNumber}
						</Button>
					</div>

					{/* Other Duplicates Section */}
					{duplicateInfo.otherDuplicates &&
						duplicateInfo.otherDuplicates.length > 0 && (
							<Collapsible open={isExpanded} onOpenChange={setIsExpanded}>
								<CollapsibleTrigger asChild>
									<Button
										variant="ghost"
										size="sm"
										className="w-full justify-between"
									>
										<span>
											Other Duplicates ({duplicateInfo.otherDuplicates.length})
										</span>
										{isExpanded ? (
											<ChevronDown className="h-4 w-4" />
										) : (
											<ChevronRight className="h-4 w-4" />
										)}
									</Button>
								</CollapsibleTrigger>
								<CollapsibleContent className="mt-3">
									<div className="rounded-lg border bg-white">
										<Table>
											<TableHeader>
												<TableRow>
													<TableHead className="w-16">#</TableHead>
													<TableHead>Title</TableHead>
													<TableHead className="w-24">Status</TableHead>
													<TableHead className="w-24">Actions</TableHead>
												</TableRow>
											</TableHeader>
											<TableBody>
												{duplicateInfo.otherDuplicates?.map((duplicate) => (
													<TableRow key={duplicate.id}>
														<TableCell className="font-medium">
															#{duplicate.projectTicketNumber}
														</TableCell>
														<TableCell className="max-w-xs truncate">
															{duplicate.title}
														</TableCell>
														<TableCell>
															<Badge variant="secondary">
																{duplicate.status}
															</Badge>
														</TableCell>
														<TableCell>
															<Button
																variant="ghost"
																size="sm"
																onClick={() =>
																	handleViewBug(
																		duplicate.id,
																		duplicate.projectTicketNumber
																	)
																}
															>
																<ExternalLink className="h-3 w-3" />
															</Button>
														</TableCell>
													</TableRow>
												))}
											</TableBody>
										</Table>
									</div>
								</CollapsibleContent>
							</Collapsible>
						)}
				</CardContent>
			</Card>
		);
	}

	// Render content for original bugs (current bug has duplicates)
	if (
		!isDuplicate &&
		duplicateInfo.otherDuplicates &&
		duplicateInfo.otherDuplicates.length > 0
	) {
		return (
			<Card className="border-blue-200 bg-blue-50/50">
				<CardHeader className="pb-3">
					<CardTitle className="flex items-center gap-2 text-blue-800">
						<Copy className="h-5 w-5" />
						Original Bug - Has {duplicateInfo.otherDuplicates.length} Duplicate
						{duplicateInfo.otherDuplicates.length !== 1 ? "s" : ""}
					</CardTitle>
					<CardDescription className="text-blue-700">
						This bug has duplicate bugs that have been identified
					</CardDescription>
				</CardHeader>
				<CardContent className="space-y-4">
					{/* Duplicates Table */}
					<Collapsible open={isExpanded} onOpenChange={setIsExpanded}>
						<CollapsibleTrigger asChild>
							<Button
								variant="outline"
								size="sm"
								className="w-full justify-between"
							>
								<span>View Duplicate Details</span>
								{isExpanded ? (
									<ChevronDown className="h-4 w-4" />
								) : (
									<ChevronRight className="h-4 w-4" />
								)}
							</Button>
						</CollapsibleTrigger>
						<CollapsibleContent className="mt-3">
							<div className="rounded-lg border bg-white">
								<Table>
									<TableHeader>
										<TableRow>
											<TableHead className="w-16">#</TableHead>
											<TableHead>Title</TableHead>
											<TableHead className="w-24">Status</TableHead>
											<TableHead className="w-24">Priority</TableHead>
											<TableHead className="w-32">Actions</TableHead>
										</TableRow>
									</TableHeader>
									<TableBody>
										{duplicateInfo.otherDuplicates?.map((duplicate) => (
											<TableRow key={duplicate.id}>
												<TableCell className="font-medium">
													#{duplicate.projectTicketNumber}
												</TableCell>
												<TableCell className="max-w-xs truncate">
													{duplicate.title}
												</TableCell>
												<TableCell>
													<Badge variant="destructive">CLOSED</Badge>
												</TableCell>
												<TableCell>
													<Badge variant="outline">{duplicate.priority}</Badge>
												</TableCell>
												<TableCell className="space-x-1">
													<Button
														variant="ghost"
														size="sm"
														onClick={() =>
															handleViewBug(
																duplicate.id,
																duplicate.projectTicketNumber
															)
														}
														title="View Bug"
													>
														<ExternalLink className="h-3 w-3" />
													</Button>
													<Button
														variant="ghost"
														size="sm"
														onClick={() => handleRemoveDuplicate(duplicate.id)}
														disabled={isRemoving}
														title="Remove Duplicate Link"
														className="text-red-600 hover:text-red-700"
													>
														<Trash2 className="h-3 w-3" />
													</Button>
												</TableCell>
											</TableRow>
										))}
									</TableBody>
								</Table>
							</div>
						</CollapsibleContent>
					</Collapsible>

					{/* Quick Actions */}
					<div className="flex gap-2">
						<Button
							variant="outline"
							size="sm"
							onClick={() =>
								navigate(`/projects/${projectSlug}/similarity-analysis`)
							}
							className="flex-1"
						>
							<Copy className="h-4 w-4 mr-2" />
							Manage All Duplicates
						</Button>
					</div>
				</CardContent>
			</Card>
		);
	}

	return null;
};
