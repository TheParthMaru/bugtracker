/**
 * BugDetailPage Component
 *
 * A comprehensive bug detail page that integrates all attachment and comment
 * functionality with the core bug information display.
 *
 * Features:
 * - Complete bug information display
 * - File attachment management (upload, view, download, delete)
 * - Comment system with threading and replies
 * - Status and assignment management
 * - Label management
 * - Real-time updates
 * - Loading states and error handling
 * - Responsive design
 */

import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useVersionedParams } from "../../hooks/useVersionedParams";
import {
	ArrowLeft,
	Edit3,
	Trash2,
	UserPlus,
	Settings,
	Download,
	Upload,
	MessageSquare,
	Paperclip,
	Calendar,
	User as UserIcon,
	AlertCircle,
	CheckCircle,
	Loader2,
	ChevronDown,
	ChevronRight,
	Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import {
	Dialog,
	DialogContent,
	DialogHeader,
	DialogTitle,
	DialogDescription,
	DialogFooter,
} from "@/components/ui/dialog";
import {
	AlertDialog,
	AlertDialogAction,
	AlertDialogCancel,
	AlertDialogContent,
	AlertDialogDescription,
	AlertDialogFooter,
	AlertDialogHeader,
	AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { toast } from "react-toastify";

import Navbar from "@/components/Navbar";
import { PointNotificationService } from "@/services/pointNotificationService";
import { Footer } from "@/components/ui/footer";
import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";
import { bugService } from "@/services/bugService";
import { projectService } from "@/services/projectService";
import {
	BugStatusBadge,
	BugPriorityBadge,
	BugTypeBadge,
	BugLabelBadge,
} from "@/components/bugs";
import { BugAttachmentUpload } from "./BugAttachmentUpload";
import { BugAttachmentViewer } from "./BugAttachmentViewer";
import { BugCommentThread } from "./BugCommentThread";
import { DuplicateStatusBadge } from "./DuplicateStatusBadge";
import type { Bug, BugAttachment, BugComment } from "@/types/bug";
import { BugStatus, BugPriority, BugType } from "@/types/bug";
import type { User as BugUser } from "@/types/bug";
import type { ProjectMember } from "@/types/project";
import type { TeamAssignmentInfo } from "@/types/bug";
import { useDuplicateInfo } from "@/hooks/useDuplicateInfo";

export function BugDetailPage() {
	const {
		params: { projectSlug, projectTicketNumber },
		usedFallback,
	} = useVersionedParams();
	const navigate = useNavigate();

	// Debug: Log the exact URL params received
	console.log("🔍 BugDetailPage: useParams received:", {
		projectSlug,
		projectTicketNumber,
		allParams: { projectSlug, projectTicketNumber },
		currentUrl: window.location.href,
		currentPathname: window.location.pathname,
	});

	// State
	const [bug, setBug] = useState<Bug | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [showAttachmentUpload, setShowAttachmentUpload] = useState(false);
	const [showDeleteDialog, setShowDeleteDialog] = useState(false);
	const [showStatusDialog, setShowStatusDialog] = useState(false);
	const [showPriorityDialog, setShowPriorityDialog] = useState(false);
	const [showTypeDialog, setShowTypeDialog] = useState(false);
	const [showAssignDialog, setShowAssignDialog] = useState(false);
	const [selectedStatus, setSelectedStatus] = useState<BugStatus | "">("");
	const [selectedPriority, setSelectedPriority] = useState<BugPriority | "">(
		""
	);
	const [selectedType, setSelectedType] = useState<BugType | "">("");
	const [selectedAssignee, setSelectedAssignee] = useState<string>("");

	const [attachmentsExpanded, setAttachmentsExpanded] = useState(true);
	const [commentsExpanded, setCommentsExpanded] = useState(true);
	const [commentSortBy, setCommentSortBy] = useState<
		"newest" | "oldest" | "relevance"
	>("newest");
	const [projectMembers, setProjectMembers] = useState<ProjectMember[]>([]);
	const [loadingProjectMembers, setLoadingProjectMembers] = useState(false);
	const [currentProjectSlug, setCurrentProjectSlug] = useState<string>("");

	// Team assignments state
	const [teamAssignments, setTeamAssignments] = useState<TeamAssignmentInfo[]>(
		[]
	);
	const [loadingTeamAssignments, setLoadingTeamAssignments] = useState(false);

	// Duplicate information hook - only run when we have valid data
	const duplicateInfoHook = useDuplicateInfo({
		projectSlug: currentProjectSlug || "",
		bugId: bug?.projectTicketNumber || 0,
	});

	// Only load duplicate info when we have valid data
	useEffect(() => {
		console.log("Duplicate Info Debug - Values:", {
			currentProjectSlug,
			bugProjectTicketNumber: bug?.projectTicketNumber,
			hasValidData:
				currentProjectSlug &&
				bug?.projectTicketNumber &&
				bug.projectTicketNumber > 0,
		});

		if (
			currentProjectSlug &&
			bug?.projectTicketNumber &&
			bug.projectTicketNumber > 0
		) {
			console.log("Loading duplicate info for:", {
				projectSlug: currentProjectSlug,
				bugId: bug.projectTicketNumber,
			});
			// Refresh duplicate info when bug data is available
			duplicateInfoHook.refreshDuplicateInfo();
		}
	}, [currentProjectSlug, bug?.projectTicketNumber]);

	// Load bug data
	useEffect(() => {
		if (projectTicketNumber) {
			loadBugData();
		}
	}, [projectTicketNumber]);

	// Initialize selected priority when dialog opens
	useEffect(() => {
		if (showPriorityDialog && bug) {
			setSelectedPriority(bug.priority);
		}
	}, [showPriorityDialog, bug]);

	// Initialize selected type when dialog opens
	useEffect(() => {
		if (showTypeDialog && bug) {
			setSelectedType(bug.type);
		}
	}, [showTypeDialog, bug]);

	const loadBugData = async () => {
		try {
			setLoading(true);
			setError(null);

			console.log("BugDetailPage -> loadBugData -> Starting bug data load", {
				projectSlug,
				projectTicketNumber,
				urlProjectSlug: projectSlug,
				urlProjectTicketNumber: projectTicketNumber,
			});

			let foundBug: Bug | null = null;
			let projectId: string | null = null;
			let resolvedProjectSlug: string | null = null;

			// If projectSlug is available from URL, use it directly
			if (projectSlug) {
				console.log(
					"BugDetailPage -> loadBugData -> Using project slug from URL",
					{ projectSlug }
				);
				try {
					const project = await projectService.getProjectBySlug(projectSlug);
					console.log("BugDetailPage -> loadBugData -> Found project", {
						projectId: project.id,
						projectName: project.name,
						projectSlug: project.projectSlug,
					});

					const bugData = await bugService.getBugByProjectTicketNumber(
						project.id,
						parseInt(projectTicketNumber)
					);
					console.log("BugDetailPage -> loadBugData -> API response received", {
						bugId: bugData.id,
						bugTitle: bugData.title,
						projectTicketNumber: bugData.projectTicketNumber,
						projectId: bugData.projectId,
						labelsCount: bugData.labels ? bugData.labels.length : 0,
						labels: bugData.labels,
						tagsCount: bugData.tags ? bugData.tags.length : 0,
						tags: bugData.tags,
						fullBugData: bugData,
					});

					foundBug = bugData;
					projectId = project.id;
					resolvedProjectSlug = project.projectSlug;
				} catch (error) {
					console.error(
						"BugDetailPage -> loadBugData -> Failed to load bug with project slug",
						error
					);
					throw new Error("Bug not found in specified project");
				}
			} else {
				console.log(
					"BugDetailPage -> loadBugData -> No project slug in URL, using fallback search"
				);
				// Fallback: search through all projects to find the bug
				// This is a workaround since we don't have a global bug endpoint
				const allProjects = await projectService.getUserProjects();
				console.log(
					"BugDetailPage -> loadBugData -> Searching through projects",
					allProjects.map((p) => ({
						id: p.id,
						name: p.name,
						projectSlug: p.projectSlug,
					}))
				);

				// Search through all projects to find the bug
				for (const project of allProjects) {
					try {
						console.log(
							`BugDetailPage -> loadBugData -> Searching project ${project.name} (${project.projectSlug}) for bug #${projectTicketNumber}`
						);
						// Use the proper bug details endpoint instead of the list endpoint
						const bugData = await bugService.getBugByProjectTicketNumber(
							project.id,
							parseInt(projectTicketNumber)
						);
						console.log(
							`BugDetailPage -> loadBugData -> Found bug in project ${project.name}`,
							{
								bugId: bugData.id,
								bugTitle: bugData.title,
								projectTicketNumber: bugData.projectTicketNumber,
								labelsCount: bugData.labels ? bugData.labels.length : 0,
								labels: bugData.labels,
								tagsCount: bugData.tags ? bugData.tags.length : 0,
								tags: bugData.tags,
								fullBugData: bugData,
							}
						);
						foundBug = bugData;
						projectId = project.id;
						resolvedProjectSlug = project.projectSlug;
						break;
					} catch (error) {
						// Continue searching other projects
						console.warn(
							`BugDetailPage -> loadBugData -> Failed to search project ${project.name}`,
							error
						);
					}
				}
			}

			if (!foundBug || !projectId || !resolvedProjectSlug) {
				throw new Error("Bug not found");
			}

			console.log("BugDetailPage -> loadBugData -> Setting bug data in state", {
				bug: foundBug,
				labelsCount: foundBug.labels ? foundBug.labels.length : 0,
				labels: foundBug.labels,
				tagsCount: foundBug.tags ? foundBug.tags.length : 0,
				tags: foundBug.tags,
			});

			// Set the bug data and project slug
			setBug(foundBug);
			setCurrentProjectSlug(resolvedProjectSlug);

			// Load project members for assignment using project slug
			await loadProjectMembers(resolvedProjectSlug);
		} catch (error) {
			console.error(
				"BugDetailPage -> loadBugData -> Failed to load bug",
				error
			);
			setError("Failed to load bug details");
			toast.error("Failed to load bug details");
		} finally {
			setLoading(false);
		}
	};

	// Load team assignments when bug data changes
	useEffect(() => {
		if (bug) {
			loadTeamAssignments();
		}
	}, [bug]);

	// Load project members
	const loadProjectMembers = async (projectSlug: string) => {
		try {
			setLoadingProjectMembers(true);
			const members = await projectService.getProjectMembers(projectSlug);
			setProjectMembers(members.content);
		} catch (error) {
			console.error("Failed to load project members:", error);
		} finally {
			setLoadingProjectMembers(false);
		}
	};

	// Load team assignments
	const loadTeamAssignments = async () => {
		if (!bug) return;

		try {
			setLoadingTeamAssignments(true);
			const assignments = await bugService.getBugTeamAssignments(
				bug.projectId,
				bug.projectTicketNumber
			);
			setTeamAssignments(assignments);
		} catch (error) {
			console.error("Failed to load team assignments:", error);
			// Don't show error toast as this is not critical
		} finally {
			setLoadingTeamAssignments(false);
		}
	};

	// Handle file upload
	const handleFileUpload = async (files: File[]) => {
		if (!bug) return;

		try {
			// Get the project ID from the bug data
			const projectId = bug.projectId;

			// Upload each file to the backend
			for (const file of files) {
				await bugService.uploadAttachment(
					projectId,
					bug.projectTicketNumber,
					file
				);
			}

			toast.success(`${files.length} file(s) uploaded successfully`);
			setShowAttachmentUpload(false);

			// Reload bug data to get updated attachments
			await loadBugData();
		} catch (error) {
			console.error("Upload error:", error);
			toast.error("Failed to upload files");
		}
	};

	// Handle file download
	const handleFileDownload = async (attachment: BugAttachment) => {
		if (!bug) return;

		try {
			// Get the project ID from the bug data
			const projectId = bug.projectId;

			// Download the file from the backend
			const blob = await bugService.downloadAttachment(
				projectId,
				bug.projectTicketNumber,
				attachment.id
			);
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement("a");
			a.href = url;
			a.download = attachment.originalFilename;
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);
			window.URL.revokeObjectURL(url);

			toast.success("File downloaded successfully");
		} catch (error) {
			console.error("Download error:", error);
			toast.error("Failed to download file");
		}
	};

	// Handle file delete
	const handleFileDelete = async (attachment: BugAttachment) => {
		if (!bug) return;

		try {
			// Get the project ID from the bug data
			const projectId = bug.projectId;

			// Delete the file from the backend
			await bugService.deleteAttachment(
				projectId,
				bug.projectTicketNumber,
				attachment.id
			);

			toast.success("File deleted successfully");

			// Reload bug data to get updated attachments
			await loadBugData();
		} catch (error) {
			console.error("Delete error:", error);
			toast.error("Failed to delete file");
		}
	};

	// Handle comment creation
	const handleCommentCreate = async (data: {
		content: string;
		parentId?: number;
		attachments?: File[];
	}) => {
		if (!bug) return;

		try {
			await bugService.createComment(bug.projectId, bug.projectTicketNumber, {
				content: data.content,
				parentId: data.parentId,
			});
			// Toast notification will be handled by backend via WebSocket
			await loadBugData();
		} catch (error) {
			console.error("Comment creation error:", error);
			toast.error("Failed to create comment");
		}
	};

	// Handle comment update
	const handleCommentUpdate = async (
		commentId: number,
		data: { content: string }
	) => {
		if (!bug) return;

		try {
			await bugService.updateComment(
				bug.projectId,
				bug.projectTicketNumber,
				commentId,
				{
					content: data.content,
				}
			);
			// Toast notification will be handled by backend via WebSocket
			await loadBugData();
		} catch (error) {
			console.error("Comment update error:", error);
			toast.error("Failed to update comment");
		}
	};

	// Handle comment deletion
	const handleCommentDelete = async (commentId: number) => {
		if (!bug) return;

		try {
			await bugService.deleteComment(
				bug.projectId,
				bug.projectTicketNumber,
				commentId
			);
			// Toast notification will be handled by backend via WebSocket
			await loadBugData();
		} catch (error) {
			console.error("Comment deletion error:", error);
			toast.error("Failed to delete comment");
		}
	};

	// Handle status change
	const handleStatusChange = async () => {
		if (!bug || !selectedStatus) return;

		try {
			await bugService.updateBugStatus(
				bug.projectId,
				bug.projectTicketNumber,
				selectedStatus
			);
			toast.success("Bug status updated successfully");
			setShowStatusDialog(false);

			// Note: Gamification notifications are now handled by the backend notification system
			// Points will be awarded to the assignee and notifications sent automatically

			// Show appropriate notification for bug reopening
			if (selectedStatus === BugStatus.REOPENED) {
				// Check if this is a reopening transition (from FIXED or CLOSED)
				if (bug.status === BugStatus.FIXED || bug.status === BugStatus.CLOSED) {
					// IMPORTANT: Show success message to admin who is reopening
					// Penalty notification will be shown to the fixer when they access the system
					setTimeout(() => {
						console.log(
							"BugDetailPage: Showing bug reopen success message to admin",
							{
								projectName: bug.projectName || "Unknown Project",
								ticketNumber: `#${bug.projectTicketNumber}`,
								adminAction: "reopening bug",
							}
						);

						// Show success message to admin
						toast.success(
							`Bug reopened successfully. The fixer has been penalized for the incorrect fix.`,
							{
								autoClose: 5000,
								position: "top-right",
							}
						);
					}, 1000); // Small delay to ensure status update is processed
				}
			}

			await loadBugData();
		} catch (error) {
			console.error("Status update error:", error);
			toast.error("Failed to update bug status");
		}
	};

	// Handle priority change
	const handlePriorityChange = async () => {
		if (!bug || !selectedPriority) return;

		try {
			await bugService.updateBug(bug.projectId, bug.projectTicketNumber, {
				priority: selectedPriority,
			});
			toast.success("Bug priority updated successfully");
			setShowPriorityDialog(false);
			await loadBugData();
		} catch (error) {
			console.error("Priority update error:", error);
			toast.error("Failed to update bug priority");
		}
	};

	// Handle type change
	const handleTypeChange = async () => {
		if (!bug || !selectedType) return;

		try {
			await bugService.updateBug(bug.projectId, bug.projectTicketNumber, {
				type: selectedType,
			});
			toast.success("Bug type updated successfully");
			setShowTypeDialog(false);
			await loadBugData();
		} catch (error) {
			console.error("Type update error:", error);
			toast.error("Failed to update bug type");
		}
	};

	// Handle assignee change
	const handleAssigneeChange = async () => {
		if (!bug || !selectedAssignee) return;

		try {
			if (selectedAssignee === "unassign") {
				// Call unassign endpoint
				await bugService.unassignBug(bug.projectId, bug.projectTicketNumber);
				toast.success("Bug unassigned successfully");
			} else {
				// Call assign endpoint with user ID
				await bugService.assignBug(
					bug.projectId,
					bug.projectTicketNumber,
					selectedAssignee
				);
				toast.success("Bug assigned successfully");
			}
			setShowAssignDialog(false);
			await loadBugData();
		} catch (error) {
			console.error("Assignee update error:", error);
			toast.error("Failed to update bug assignment");
		}
	};

	// Handle bug deletion
	const handleDeleteBug = async () => {
		if (!bug) return;

		try {
			await bugService.deleteBug(bug.projectId, bug.projectTicketNumber);
			toast.success("Bug deleted successfully");
			navigate(`/projects/${currentProjectSlug}/bugs`);
		} catch (error) {
			console.error("Delete error:", error);
			toast.error("Failed to delete bug");
		}
	};

	// Handle edit bug
	const handleEditBug = () => {
		if (currentProjectSlug) {
			navigate(
				`/projects/${currentProjectSlug}/bugs/${bug?.projectTicketNumber}/edit`
			);
		}
	};

	// Handle assign to team
	const handleAssignToTeam = () => {
		if (currentProjectSlug) {
			navigate(`/projects/${currentProjectSlug}/teams`);
		}
	};

	// Get user display name
	const getUserDisplayName = (user: BugUser): string => {
		if (user.firstName && user.lastName) {
			return `${user.firstName} ${user.lastName}`;
		}
		if (user.firstName) {
			return user.firstName;
		}
		if (user.lastName) {
			return user.lastName;
		}
		return user.email || "Unknown User";
	};

	// Get user initials
	const getUserInitials = (user: BugUser): string => {
		if (user.firstName && user.lastName) {
			return `${user.firstName[0]}${user.lastName[0]}`;
		}
		if (user.firstName) {
			return user.firstName[0];
		}
		if (user.lastName) {
			return user.lastName[0];
		}
		return user.email?.[0]?.toUpperCase() || "U";
	};

	// Get bug resolution points based on priority
	const getBugResolutionPoints = (priority: BugPriority): number => {
		switch (priority) {
			case BugPriority.CRASH:
				return 100;
			case BugPriority.CRITICAL:
				return 75;
			case BugPriority.HIGH:
				return 50;
			case BugPriority.MEDIUM:
				return 25;
			case BugPriority.LOW:
				return 10;
			default:
				return 0;
		}
	};

	// Format date
	const formatDate = (dateString: string): string => {
		return new Date(dateString).toLocaleDateString("en-US", {
			year: "numeric",
			month: "long",
			day: "numeric",
			hour: "2-digit",
			minute: "2-digit",
		});
	};

	if (loading) {
		return (
			<div className="min-h-screen bg-gray-50">
				<Navbar />
				<div className="container mx-auto px-4 py-8">
					<div className="flex items-center justify-center py-16">
						<Loader2 className="h-8 w-8 animate-spin text-gray-400" />
						<span className="ml-2 text-gray-600">Loading bug details...</span>
					</div>
				</div>
				<Footer />
			</div>
		);
	}

	if (error || !bug) {
		return (
			<div className="min-h-screen bg-gray-50">
				<Navbar />
				<div className="container mx-auto px-4 py-8">
					<div className="flex items-center justify-center py-16">
						<div className="text-center">
							<AlertCircle className="h-12 w-12 text-red-500 mx-auto mb-4" />
							<h2 className="text-xl font-semibold text-gray-900 mb-2">
								{error || "Bug not found"}
							</h2>
							<Button onClick={() => navigate(`/bugs`)}>
								<ArrowLeft className="mr-2 h-4 w-4" />
								Back to Bugs
							</Button>
						</div>
					</div>
				</div>
				<Footer />
			</div>
		);
	}

	return (
		<div className="min-h-screen bg-gray-50">
			<Navbar />

			<div className="container mx-auto px-4 py-8">
				{/* Breadcrumb */}
				<ProjectBreadcrumb
					projectName={bug.projectName}
					projectSlug={currentProjectSlug}
					section="Bugs"
					sectionHref={`/projects/${currentProjectSlug}/bugs`}
					current={`#${bug.projectTicketNumber}`}
				/>

				{/* Header */}
				<div className="flex items-start justify-between mb-6">
					<div className="flex-1">
						<div className="flex items-center gap-3 mb-2">
							<h1 className="text-2xl font-bold text-gray-900">{bug.title}</h1>
							<Badge variant="outline">#{bug.projectTicketNumber}</Badge>
						</div>
						<div className="flex items-center gap-2 text-sm text-muted-foreground">
							<span>Bug #{bug.projectTicketNumber}</span>
							<span>•</span>
							<span>Created {formatDate(bug.createdAt)}</span>
						</div>
					</div>

					{/* Action Buttons */}
					<div className="flex items-center gap-2">
						<Button variant="outline" onClick={() => setShowAssignDialog(true)}>
							<UserPlus className="mr-2 h-4 w-4" />
							Assign
						</Button>
						<Button variant="outline" onClick={() => setShowStatusDialog(true)}>
							<Settings className="mr-2 h-4 w-4" />
							Status
						</Button>
						<Button
							variant="outline"
							onClick={() => setShowPriorityDialog(true)}
						>
							<Settings className="mr-2 h-4 w-4" />
							Priority
						</Button>
						<Button variant="outline" onClick={() => setShowTypeDialog(true)}>
							<Settings className="mr-2 h-4 w-4" />
							Type
						</Button>
						<Button variant="outline" onClick={handleEditBug}>
							<Edit3 className="mr-2 h-4 w-4" />
							Edit
						</Button>
						<Button
							variant="outline"
							onClick={() => setShowDeleteDialog(true)}
							className="text-red-600 hover:text-red-700"
						>
							<Trash2 className="mr-2 h-4 w-4" />
							Delete
						</Button>
					</div>
				</div>

				<div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
					{/* Main Content */}
					<div className="lg:col-span-2 space-y-6">
						{/* Bug Information */}
						<Card>
							<CardHeader>
								<CardTitle>Description</CardTitle>
							</CardHeader>
							<CardContent>
								<div className="prose max-w-none">
									<p className="text-gray-700 whitespace-pre-wrap">
										{bug.description}
									</p>
								</div>
							</CardContent>
						</Card>

						{/* Duplicate Status Badge */}
						{duplicateInfoHook.duplicateInfo && (
							<DuplicateStatusBadge
								duplicateInfo={duplicateInfoHook.duplicateInfo}
								projectSlug={currentProjectSlug}
								bugId={bug.projectTicketNumber}
								onDuplicateRemoved={duplicateInfoHook.refreshDuplicateInfo}
							/>
						)}

						{/* Attachments */}
						<Card>
							<CardHeader>
								<div className="flex items-center justify-between">
									<CardTitle className="flex items-center gap-2">
										<button
											onClick={() =>
												setAttachmentsExpanded(!attachmentsExpanded)
											}
											className="flex items-center gap-2 hover:bg-gray-100 rounded-lg p-1 transition-colors"
										>
											{attachmentsExpanded ? (
												<ChevronDown className="h-4 w-4 text-gray-600" />
											) : (
												<ChevronRight className="h-4 w-4 text-gray-600" />
											)}
											<Paperclip className="h-5 w-5" />
											Attachments ({bug.attachments.length})
										</button>
									</CardTitle>
									<div className="flex items-center gap-2">
										<Button
											variant="outline"
											size="sm"
											onClick={() => setShowAttachmentUpload(true)}
										>
											<Upload className="mr-2 h-4 w-4" />
											Upload
										</Button>
									</div>
								</div>
							</CardHeader>
							<CardContent>
								<BugAttachmentViewer
									attachments={bug.attachments}
									projectId={bug.projectId}
									projectTicketNumber={bug.projectTicketNumber}
									onDownload={handleFileDownload}
									onDelete={handleFileDelete}
									canDelete={true}
									isExpanded={attachmentsExpanded}
								/>
							</CardContent>
						</Card>

						{/* Comments */}
						<Card>
							<CardHeader>
								<div className="flex items-center justify-between">
									<CardTitle className="flex items-center gap-2">
										<button
											onClick={() => setCommentsExpanded(!commentsExpanded)}
											className="flex items-center gap-2 hover:bg-gray-100 rounded-lg p-1 transition-colors"
										>
											{commentsExpanded ? (
												<ChevronDown className="h-4 w-4 text-gray-600" />
											) : (
												<ChevronRight className="h-4 w-4 text-gray-600" />
											)}
											<MessageSquare className="h-5 w-5" />
											Comments ({bug.comments.length})
										</button>
									</CardTitle>
									{commentsExpanded && (
										<Select
											value={commentSortBy}
											onValueChange={(
												value: "newest" | "oldest" | "relevance"
											) => setCommentSortBy(value)}
										>
											<SelectTrigger className="w-40">
												<SelectValue />
											</SelectTrigger>
											<SelectContent>
												<SelectItem value="newest">Newest First</SelectItem>
												<SelectItem value="oldest">Oldest First</SelectItem>
												<SelectItem value="relevance">Most Relevant</SelectItem>
											</SelectContent>
										</Select>
									)}
								</div>
							</CardHeader>
							<CardContent>
								{commentsExpanded && (
									<BugCommentThread
										comments={bug.comments}
										onCreateComment={handleCommentCreate}
										onUpdateComment={handleCommentUpdate}
										onDeleteComment={handleCommentDelete}
										onReply={() => {}}
										currentUser={
											projectMembers[0]
												? {
														id: projectMembers[0].userId,
														firstName: projectMembers[0].firstName,
														lastName: projectMembers[0].lastName,
														email: projectMembers[0].userEmail,
												  }
												: {
														id: "1",
														firstName: "Current",
														lastName: "User",
														email: "current@user.com",
												  }
										} // Use first project member or fallback
										projectMembers={projectMembers.map((member) => ({
											id: member.userId,
											firstName: member.firstName,
											lastName: member.lastName,
											email: member.userEmail,
										}))}
										canEdit={(comment) =>
											comment.author.id === (projectMembers[0]?.userId || "1")
										}
										canDelete={(comment) =>
											comment.author.id === (projectMembers[0]?.userId || "1")
										}
										canReply={true}
										hideHeader={true}
										sortBy={commentSortBy}
									/>
								)}
							</CardContent>
						</Card>
					</div>

					{/* Sidebar */}
					<div className="space-y-6">
						{/* Status and Priority */}
						<Card>
							<CardHeader>
								<CardTitle>Details</CardTitle>
							</CardHeader>
							<CardContent className="space-y-4">
								<div>
									<label className="text-sm font-medium text-gray-700">
										Status
									</label>
									<div className="mt-1">
										<BugStatusBadge status={bug.status} size="lg" />
									</div>
								</div>

								<div>
									<label className="text-sm font-medium text-gray-700">
										Priority
									</label>
									<div className="mt-1">
										<BugPriorityBadge priority={bug.priority} size="lg" />
									</div>
								</div>

								<div>
									<label className="text-sm font-medium text-gray-700">
										Type
									</label>
									<div className="mt-1">
										<BugTypeBadge type={bug.type} size="lg" />
									</div>
								</div>

								<div>
									<label className="text-sm font-medium text-gray-700">
										Reporter
									</label>
									<div className="mt-1">
										<div className="flex items-center gap-2">
											<Avatar className="h-6 w-6">
												<AvatarFallback className="text-xs">
													{getUserInitials(bug.reporter)}
												</AvatarFallback>
											</Avatar>
											<span className="text-sm text-gray-900">
												{getUserDisplayName(bug.reporter)}
											</span>
										</div>
									</div>
								</div>

								<div>
									<label className="text-sm font-medium text-gray-700">
										Assignee
									</label>
									<div className="mt-1">
										{bug.assignee ? (
											<div className="flex items-center gap-2">
												<Avatar className="h-6 w-6">
													<AvatarFallback className="text-xs">
														{getUserInitials(bug.assignee)}
													</AvatarFallback>
												</Avatar>
												<span className="text-sm text-gray-900">
													{getUserDisplayName(bug.assignee)}
												</span>
											</div>
										) : (
											<div className="flex items-center gap-2">
												<UserIcon className="h-4 w-4 text-gray-400" />
												<span className="text-sm text-gray-500">
													Unassigned
												</span>
											</div>
										)}
									</div>
								</div>

								{/* Team Information */}
								<div>
									<label className="text-sm font-medium text-gray-700">
										Team
									</label>
									<div className="mt-1">
										{loadingTeamAssignments ? (
											<div className="flex items-center gap-2">
												<Loader2 className="h-4 w-4 animate-spin text-gray-400" />
												<span className="text-sm text-gray-500">
													Loading teams...
												</span>
											</div>
										) : teamAssignments.length > 0 ? (
											<div className="space-y-2">
												{teamAssignments.map((team) => (
													<div
														key={team.teamId}
														className="flex items-center gap-2"
													>
														<Users className="h-4 w-4 text-blue-500" />
														<span className="text-sm text-gray-900">
															{team.teamName}
															{team.isPrimary && (
																<span className="ml-2 text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
																	Primary
																</span>
															)}
														</span>
													</div>
												))}
											</div>
										) : (
											<div className="flex items-center gap-2">
												<Users className="h-4 w-4 text-gray-400" />
												<span className="text-sm text-gray-500">
													Not assigned to a team
												</span>
											</div>
										)}
										<Button
											variant="outline"
											size="sm"
											className="mt-2"
											onClick={handleAssignToTeam}
										>
											<Users className="mr-2 h-3 w-3" />
											Assign to Team
										</Button>
									</div>
								</div>
							</CardContent>
						</Card>

						{/* Labels */}
						<Card>
							<CardHeader>
								<CardTitle>Labels</CardTitle>
							</CardHeader>
							<CardContent>
								{(() => {
									console.log(
										"BugDetailPage -> Labels Section -> Rendering labels section",
										{
											bugExists: !!bug,
											bugLabels: bug?.labels,
											labelsType: typeof bug?.labels,
											labelsIsArray: Array.isArray(bug?.labels),
											labelsLength: bug?.labels?.length || 0,
											labelsCondition: bug?.labels && bug.labels.length > 0,
											fullBugObject: bug,
										}
									);

									if (bug?.labels && bug.labels.length > 0) {
										console.log(
											"BugDetailPage -> Labels Section -> Rendering labels",
											{
												labels: bug.labels,
												labelsCount: bug.labels.length,
											}
										);
										return (
											<div className="flex flex-wrap gap-2">
												{bug.labels.map((label) => (
													<BugLabelBadge
														key={label.id}
														label={label}
														size="sm"
													/>
												))}
											</div>
										);
									} else {
										console.log(
											"BugDetailPage -> Labels Section -> No labels to render",
											{
												bugLabels: bug?.labels,
												labelsLength: bug?.labels?.length || 0,
											}
										);
										return (
											<div className="text-center py-4 text-gray-500">
												<AlertCircle className="h-8 w-8 mx-auto mb-2 text-gray-400" />
												<p>No labels assigned to this bug</p>
											</div>
										);
									}
								})()}
							</CardContent>
						</Card>

						{/* Tags */}
						{bug.tags && bug.tags.length > 0 && (
							<Card>
								<CardHeader>
									<CardTitle>Tags</CardTitle>
								</CardHeader>
								<CardContent>
									<div className="flex flex-wrap gap-2">
										{bug.tags.map((tag) => (
											<span
												key={tag}
												className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm font-medium border border-gray-200"
											>
												{tag}
											</span>
										))}
									</div>
								</CardContent>
							</Card>
						)}

						{/* Activity */}
						<Card>
							<CardHeader>
								<CardTitle>Activity</CardTitle>
							</CardHeader>
							<CardContent className="space-y-3">
								<div className="flex items-center gap-3 text-sm">
									<div className="w-2 h-2 bg-blue-500 rounded-full"></div>
									<div>
										<p className="text-gray-900">Bug created</p>
										<p className="text-gray-500">{formatDate(bug.createdAt)}</p>
									</div>
								</div>
								{bug.updatedAt !== bug.createdAt && (
									<div className="flex items-center gap-3 text-sm">
										<div className="w-2 h-2 bg-gray-400 rounded-full"></div>
										<div>
											<p className="text-gray-900">Last updated</p>
											<p className="text-gray-500">
												{formatDate(bug.updatedAt)}
											</p>
										</div>
									</div>
								)}
								{bug.closedAt && (
									<div className="flex items-center gap-3 text-sm">
										<div className="w-2 h-2 bg-green-500 rounded-full"></div>
										<div>
											<p className="text-gray-900">Bug closed</p>
											<p className="text-gray-500">
												{formatDate(bug.closedAt)}
											</p>
										</div>
									</div>
								)}
							</CardContent>
						</Card>

						{/* Leaderboard Info */}
						<Card>
							<CardHeader>
								<CardTitle className="text-sm">Leaderboard</CardTitle>
							</CardHeader>
							<CardContent>
								<div className="text-center py-2">
									<p className="text-xs text-gray-500 mb-2">
										Resolve bugs to earn points!
									</p>
									<Button
										variant="outline"
										size="sm"
										className="w-full"
										onClick={() => navigate("/leaderboard")}
									>
										View Progress
									</Button>
								</div>
							</CardContent>
						</Card>
					</div>
				</div>
			</div>

			{/* Attachment Upload Dialog */}
			<Dialog
				open={showAttachmentUpload}
				onOpenChange={setShowAttachmentUpload}
			>
				<DialogContent className="max-w-2xl">
					<DialogHeader>
						<DialogTitle>Upload Attachments</DialogTitle>
						<DialogDescription>
							Upload files to provide additional context for this bug.
						</DialogDescription>
					</DialogHeader>
					<BugAttachmentUpload
						onUpload={handleFileUpload}
						onCancel={() => setShowAttachmentUpload(false)}
						maxFiles={5}
					/>
				</DialogContent>
			</Dialog>

			{/* Status Update Dialog */}
			<Dialog open={showStatusDialog} onOpenChange={setShowStatusDialog}>
				<DialogContent>
					<DialogHeader>
						<DialogTitle>Update Status</DialogTitle>
						<DialogDescription>
							Change the status of this bug.
						</DialogDescription>
					</DialogHeader>
					<div className="space-y-4">
						<div>
							<label className="text-sm font-medium text-gray-700">
								Status
							</label>
							<Select
								value={selectedStatus}
								onValueChange={(value: BugStatus) => setSelectedStatus(value)}
							>
								<SelectTrigger className="mt-1">
									<SelectValue placeholder="Select status" />
								</SelectTrigger>
								<SelectContent>
									<SelectItem value="OPEN">Open</SelectItem>
									<SelectItem value="FIXED">Fixed</SelectItem>
									<SelectItem value="CLOSED">Closed</SelectItem>
									<SelectItem value="REOPENED">Reopened</SelectItem>
								</SelectContent>
							</Select>
						</div>
					</div>
					<DialogFooter>
						<Button
							variant="outline"
							onClick={() => setShowStatusDialog(false)}
						>
							Cancel
						</Button>
						<Button onClick={handleStatusChange} disabled={!selectedStatus}>
							Update Status
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			{/* Priority Update Dialog */}
			<Dialog open={showPriorityDialog} onOpenChange={setShowPriorityDialog}>
				<DialogContent>
					<DialogHeader>
						<DialogTitle>Update Priority</DialogTitle>
						<DialogDescription>
							Change the priority of this bug.
						</DialogDescription>
					</DialogHeader>
					<div className="space-y-4">
						<div>
							<label className="text-sm font-medium text-gray-700">
								Priority
							</label>
							<Select
								value={selectedPriority}
								onValueChange={(value: BugPriority) =>
									setSelectedPriority(value)
								}
							>
								<SelectTrigger className="mt-1">
									<SelectValue placeholder="Select priority" />
								</SelectTrigger>
								<SelectContent>
									<SelectItem value="CRASH">Crash</SelectItem>
									<SelectItem value="CRITICAL">Critical</SelectItem>
									<SelectItem value="HIGH">High</SelectItem>
									<SelectItem value="MEDIUM">Medium</SelectItem>
									<SelectItem value="LOW">Low</SelectItem>
								</SelectContent>
							</Select>
						</div>
					</div>
					<DialogFooter>
						<Button
							variant="outline"
							onClick={() => setShowPriorityDialog(false)}
						>
							Cancel
						</Button>
						<Button onClick={handlePriorityChange} disabled={!selectedPriority}>
							Update Priority
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			{/* Type Update Dialog */}
			<Dialog open={showTypeDialog} onOpenChange={setShowTypeDialog}>
				<DialogContent>
					<DialogHeader>
						<DialogTitle>Update Type</DialogTitle>
						<DialogDescription>Change the type of this bug.</DialogDescription>
					</DialogHeader>
					<div className="space-y-4">
						<div>
							<label className="text-sm font-medium text-gray-700">Type</label>
							<Select
								value={selectedType}
								onValueChange={(value: BugType) => setSelectedType(value)}
							>
								<SelectTrigger className="mt-1">
									<SelectValue placeholder="Select type" />
								</SelectTrigger>
								<SelectContent>
									<SelectItem value="ISSUE">Issue</SelectItem>
									<SelectItem value="TASK">Task</SelectItem>
									<SelectItem value="SPEC">Specification</SelectItem>
								</SelectContent>
							</Select>
						</div>
					</div>
					<DialogFooter>
						<Button variant="outline" onClick={() => setShowTypeDialog(false)}>
							Cancel
						</Button>
						<Button onClick={handleTypeChange} disabled={!selectedType}>
							Update Type
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			{/* Assignment Dialog */}
			<Dialog open={showAssignDialog} onOpenChange={setShowAssignDialog}>
				<DialogContent>
					<DialogHeader>
						<DialogTitle>Assign Bug</DialogTitle>
						<DialogDescription>
							Assign this bug to a team member.
						</DialogDescription>
					</DialogHeader>
					<div>
						<label className="text-sm font-medium text-gray-700">
							Assignee
						</label>
						<Select
							value={selectedAssignee}
							onValueChange={setSelectedAssignee}
							disabled={loadingProjectMembers}
						>
							<SelectTrigger className="mt-1">
								<SelectValue
									placeholder={
										loadingProjectMembers
											? "Loading members..."
											: "Select assignee"
									}
								/>
							</SelectTrigger>
							<SelectContent>
								<SelectItem value="unassign">Unassign</SelectItem>
								{projectMembers.map((member) => (
									<SelectItem key={member.userId} value={member.userId}>
										{member.firstName && member.lastName
											? `${member.firstName} ${member.lastName}`
											: member.userName || member.userEmail}
									</SelectItem>
								))}
							</SelectContent>
						</Select>
					</div>
					<DialogFooter>
						<Button
							variant="outline"
							onClick={() => setShowAssignDialog(false)}
						>
							Cancel
						</Button>
						<Button onClick={handleAssigneeChange}>Assign</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			{/* Delete Confirmation Dialog */}
			<AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Delete Bug</AlertDialogTitle>
						<AlertDialogDescription>
							Are you sure you want to delete this bug? This action cannot be
							undone.
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={handleDeleteBug}
							className="bg-red-600 hover:bg-red-700"
						>
							Delete
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>

			<Footer />
		</div>
	);
}
