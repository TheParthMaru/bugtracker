/**
 * PendingRequestsModal Component
 *
 * Modal for project admins to view and manage pending join requests.
 * Provides approve/reject functionality with proper loading states and feedback.
 *
 * Features:
 * - List of pending requests with user details
 * - Approve/reject actions with confirmation
 * - Loading states and error handling
 * - Real-time updates after actions
 * - Empty state when no pending requests
 */

import React, { useState, useEffect } from "react";
import {
	Dialog,
	DialogContent,
	DialogDescription,
	DialogHeader,
	DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import {
	Users,
	CheckCircle,
	X,
	Clock,
	UserPlus,
	AlertCircle,
	Loader2,
} from "lucide-react";
import { projectService } from "@/services/projectService";
import type { ProjectMember } from "@/types/project";
import { toast } from "react-toastify";

interface PendingRequestsModalProps {
	isOpen: boolean;
	onClose: () => void;
	projectSlug: string;
	projectName: string;
	onRequestsUpdated?: () => void; // Callback to refresh parent data
}

export function PendingRequestsModal({
	isOpen,
	onClose,
	projectSlug,
	projectName,
	onRequestsUpdated,
}: PendingRequestsModalProps) {
	const [pendingRequests, setPendingRequests] = useState<ProjectMember[]>([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [processingRequests, setProcessingRequests] = useState<Set<string>>(
		new Set()
	);

	// Load pending requests when modal opens
	useEffect(() => {
		if (isOpen) {
			loadPendingRequests();
		}
	}, [isOpen, projectSlug]);

	const loadPendingRequests = async () => {
		try {
			setLoading(true);
			setError(null);
			const requests = await projectService.getPendingRequests(projectSlug);
			setPendingRequests(requests);
		} catch (err) {
			console.error("Failed to load pending requests:", err);
			setError(
				err instanceof Error ? err.message : "Failed to load pending requests"
			);
			toast.error("Failed to load pending requests");
		} finally {
			setLoading(false);
		}
	};

	const handleApprove = async (userId: string, userName: string) => {
		try {
			setProcessingRequests((prev) => new Set(prev).add(userId));
			await projectService.approveMember(projectSlug, userId);

			// Remove from pending requests
			setPendingRequests((prev) =>
				prev.filter((request) => request.userId !== userId)
			);

			toast.success(`Approved ${userName}'s join request`);

			// Notify parent to refresh data
			onRequestsUpdated?.();
		} catch (err) {
			console.error("Failed to approve request:", err);
			toast.error(
				err instanceof Error ? err.message : "Failed to approve request"
			);
		} finally {
			setProcessingRequests((prev) => {
				const newSet = new Set(prev);
				newSet.delete(userId);
				return newSet;
			});
		}
	};

	const handleReject = async (userId: string, userName: string) => {
		try {
			setProcessingRequests((prev) => new Set(prev).add(userId));
			await projectService.rejectMember(projectSlug, userId);

			// Remove from pending requests
			setPendingRequests((prev) =>
				prev.filter((request) => request.userId !== userId)
			);

			toast.success(`Rejected ${userName}'s join request`);

			// Notify parent to refresh data
			onRequestsUpdated?.();
		} catch (err) {
			console.error("Failed to reject request:", err);
			toast.error(
				err instanceof Error ? err.message : "Failed to reject request"
			);
		} finally {
			setProcessingRequests((prev) => {
				const newSet = new Set(prev);
				newSet.delete(userId);
				return newSet;
			});
		}
	};

	const getUserInitials = (firstName?: string, lastName?: string): string => {
		if (firstName && lastName) {
			return `${firstName[0]}${lastName[0]}`.toUpperCase();
		}
		if (firstName) {
			return firstName.slice(0, 2).toUpperCase();
		}
		if (lastName) {
			return lastName.slice(0, 2).toUpperCase();
		}
		return "U"; // Default fallback
	};

	const formatDate = (dateString: string): string => {
		return new Date(dateString).toLocaleDateString("en-US", {
			year: "numeric",
			month: "short",
			day: "numeric",
			hour: "2-digit",
			minute: "2-digit",
		});
	};

	return (
		<Dialog open={isOpen} onOpenChange={onClose}>
			<DialogContent className="max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
				<DialogHeader>
					<DialogTitle className="flex items-center gap-2">
						<Users className="h-5 w-5" />
						Pending Join Requests
					</DialogTitle>
					<DialogDescription>
						Review and manage join requests for{" "}
						<span className="font-medium">{projectName}</span>
					</DialogDescription>
				</DialogHeader>

				<div className="flex-1 overflow-y-auto">
					{loading ? (
						<div className="flex items-center justify-center py-8">
							<Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
							<span className="ml-2 text-muted-foreground">
								Loading requests...
							</span>
						</div>
					) : error ? (
						<Card className="border-destructive">
							<CardContent className="pt-6">
								<div className="flex items-center gap-2 text-destructive">
									<AlertCircle className="h-5 w-5" />
									<span className="font-medium">Error Loading Requests</span>
								</div>
								<p className="text-sm text-muted-foreground mt-1">{error}</p>
								<Button
									variant="outline"
									size="sm"
									onClick={loadPendingRequests}
									className="mt-3"
								>
									Try Again
								</Button>
							</CardContent>
						</Card>
					) : pendingRequests.length === 0 ? (
						<Card>
							<CardContent className="pt-6">
								<div className="text-center py-8">
									<CheckCircle className="h-12 w-12 mx-auto mb-4 text-green-500 opacity-50" />
									<h3 className="text-lg font-semibold mb-2">
										No Pending Requests
									</h3>
									<p className="text-muted-foreground">
										All join requests have been processed.
									</p>
								</div>
							</CardContent>
						</Card>
					) : (
						<div className="space-y-4">
							{pendingRequests.map((request) => {
								const isProcessing = processingRequests.has(request.userId);
								const userName =
									request.userName ||
									`${request.firstName} ${request.lastName}`.trim() ||
									"Unknown User";
								const userEmail = request.userEmail || "No email provided";
								const initials = getUserInitials(
									request.firstName,
									request.lastName
								);

								return (
									<Card key={request.id} className="relative">
										<CardContent className="pt-6">
											<div className="flex items-start gap-4">
												<Avatar className="h-12 w-12 flex-shrink-0">
													<AvatarFallback>{initials}</AvatarFallback>
												</Avatar>

												<div className="flex-1 min-w-0">
													<div className="flex items-start justify-between gap-2">
														<div className="min-w-0 flex-1">
															<h4 className="font-medium truncate">
																{userName}
															</h4>
															<p className="text-sm text-muted-foreground truncate">
																{userEmail}
															</p>
															<div className="flex items-center gap-2 mt-2">
																<Clock className="h-3 w-3 text-muted-foreground" />
																<span className="text-xs text-muted-foreground">
																	Requested{" "}
																	{request.requestedAt
																		? formatDate(request.requestedAt)
																		: "Unknown"}
																</span>
															</div>
														</div>

														<div className="flex items-center gap-2 flex-shrink-0">
															<Badge variant="secondary" className="text-xs">
																{request.role}
															</Badge>
														</div>
													</div>

													{/* Message field removed - not available in current ProjectMember type */}

													<div className="flex items-center gap-2 mt-4">
														<Button
															size="sm"
															onClick={() =>
																handleApprove(request.userId, userName)
															}
															disabled={isProcessing}
															className="flex-1"
														>
															{isProcessing ? (
																<Loader2 className="h-4 w-4 animate-spin" />
															) : (
																<CheckCircle className="h-4 w-4" />
															)}
															<span className="ml-2">Approve</span>
														</Button>

														<Button
															variant="outline"
															size="sm"
															onClick={() =>
																handleReject(request.userId, userName)
															}
															disabled={isProcessing}
															className="flex-1"
														>
															{isProcessing ? (
																<Loader2 className="h-4 w-4 animate-spin" />
															) : (
																<X className="h-4 w-4" />
															)}
															<span className="ml-2">Reject</span>
														</Button>
													</div>
												</div>
											</div>
										</CardContent>
									</Card>
								);
							})}
						</div>
					)}
				</div>

				<Separator className="my-4" />

				<div className="flex items-center justify-between">
					<div className="text-sm text-muted-foreground">
						{pendingRequests.length} pending request
						{pendingRequests.length !== 1 ? "s" : ""}
					</div>

					<div className="flex items-center gap-2">
						<Button variant="outline" onClick={onClose}>
							Close
						</Button>
						{pendingRequests.length > 0 && (
							<Button onClick={loadPendingRequests} variant="outline">
								Refresh
							</Button>
						)}
					</div>
				</div>
			</DialogContent>
		</Dialog>
	);
}
