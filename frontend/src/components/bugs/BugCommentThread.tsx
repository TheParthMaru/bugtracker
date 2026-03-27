/**
 * BugCommentThread Component
 *
 * A comprehensive comment thread component for displaying bug comments
 * with threading support, sorting options, and management functionality.
 *
 * Features:
 * - Threaded comment display with replies
 * - Comment sorting (newest, oldest, most relevant)
 * - Comment editing and deletion
 * - Reply functionality
 * - Rich text rendering with markdown
 * - User mentions highlighting
 * - Loading states and pagination
 * - Accessibility support
 */

import React, { useState, useCallback, useEffect } from "react";
import {
	MessageSquare,
	Reply,
	Edit3,
	Trash2,
	MoreHorizontal,
	ChevronDown,
	ChevronUp,
	SortAsc,
	SortDesc,
	Clock,
	ThumbsUp,
	User as UserIcon,
	Calendar,
	Loader2,
	AlertCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuSeparator,
	DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
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
import type { BugComment } from "@/types/bug";
import type { User as BugUser } from "@/types/bug";
import { BugCommentForm } from "./BugCommentForm";
import { BugAttachmentViewer } from "./BugAttachmentViewer";

interface BugCommentThreadProps {
	comments: BugComment[];
	onCreateComment: (data: {
		content: string;
		parentId?: number;
		attachments?: File[];
	}) => Promise<void>;
	onUpdateComment: (
		commentId: number,
		data: { content: string }
	) => Promise<void>;
	onDeleteComment: (commentId: number) => Promise<void>;
	onReply: (parentComment: BugComment) => void;
	currentUser?: BugUser;
	projectMembers?: BugUser[];
	canEdit?: (comment: BugComment) => boolean;
	canDelete?: (comment: BugComment) => boolean;
	canReply?: boolean;
	loading?: boolean;
	className?: string;
	hideHeader?: boolean;
	sortBy?: "newest" | "oldest" | "relevance";
}

// Comment sorting options
const SORT_OPTIONS = {
	newest: { label: "Newest First", icon: SortDesc },
	oldest: { label: "Oldest First", icon: SortAsc },
	relevance: { label: "Most Relevant", icon: ThumbsUp },
} as const;

type SortOption = keyof typeof SORT_OPTIONS;

export function BugCommentThread({
	comments,
	onCreateComment,
	onUpdateComment,
	onDeleteComment,
	onReply,
	currentUser,
	projectMembers = [],
	canEdit = () => false,
	canDelete = () => false,
	canReply = true,
	loading = false,
	className,
	hideHeader = false,
	sortBy: externalSortBy,
}: BugCommentThreadProps) {
	// Helper functions - moved to top to avoid temporal dead zone
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

	// Create lookup object for project member names (firstName -> fullName)
	const projectMemberNames = projectMembers.reduce((acc, member) => {
		const fullName = getUserDisplayName(member);
		// Use firstName as key to fullName for quick lookup
		acc[member.firstName] = fullName;
		return acc;
	}, {} as Record<string, string>);

	// State for comment management
	const [sortBy, setSortBy] = useState<SortOption>(externalSortBy || "newest");
	const [replyingTo, setReplyingTo] = useState<BugComment | null>(null);

	// Sync external sortBy prop with internal state
	useEffect(() => {
		if (externalSortBy) {
			setSortBy(externalSortBy);
		}
	}, [externalSortBy]);
	const [editingComment, setEditingComment] = useState<BugComment | null>(null);
	const [deletingComment, setDeletingComment] = useState<BugComment | null>(
		null
	);
	const [expandedReplies, setExpandedReplies] = useState<Set<number>>(
		new Set()
	);
	const [showDeleteDialog, setShowDeleteDialog] = useState(false);

	// Sort comments
	const sortedComments = React.useMemo(() => {
		const sorted = [...comments];
		switch (sortBy) {
			case "newest":
				return sorted.sort(
					(a, b) =>
						new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
				);
			case "oldest":
				return sorted.sort(
					(a, b) =>
						new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
				);
			case "relevance":
				// Sort by reply count first, then by date
				return sorted.sort((a, b) => {
					const aReplies = a.replies?.length || 0;
					const bReplies = b.replies?.length || 0;
					if (aReplies !== bReplies) {
						return bReplies - aReplies;
					}
					return (
						new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
					);
				});
			default:
				return sorted;
		}
	}, [comments, sortBy]);

	// Get top-level comments (no parent)
	const topLevelComments = sortedComments.filter((comment) => !comment.parent);

	// Handle reply
	const handleReply = useCallback((comment: BugComment) => {
		setReplyingTo(comment);
		setEditingComment(null);
	}, []);

	// Handle edit
	const handleEdit = useCallback((comment: BugComment) => {
		setEditingComment(comment);
		setReplyingTo(null);
	}, []);

	// Handle delete
	const handleDelete = useCallback(async (comment: BugComment) => {
		setDeletingComment(comment);
		setShowDeleteDialog(true);
	}, []);

	// Confirm delete
	const confirmDelete = useCallback(async () => {
		if (!deletingComment) return;

		try {
			await onDeleteComment(deletingComment.id);
			// Toast notification will be handled by backend via WebSocket
			setShowDeleteDialog(false);
			setDeletingComment(null);
		} catch (error) {
			console.error("Delete error:", error);
			toast.error("Failed to delete comment");
		}
	}, [deletingComment, onDeleteComment]);

	// Toggle reply expansion
	const toggleReplies = useCallback((commentId: number) => {
		setExpandedReplies((prev) => {
			const newSet = new Set(prev);
			if (newSet.has(commentId)) {
				newSet.delete(commentId);
			} else {
				newSet.add(commentId);
			}
			return newSet;
		});
	}, []);

	// Handle comment submission
	const handleCommentSubmit = useCallback(
		async (data: { content: string; attachments?: File[] }) => {
			try {
				if (editingComment) {
					await onUpdateComment(editingComment.id, { content: data.content });
					setEditingComment(null);
				} else if (replyingTo) {
					await onCreateComment({
						...data,
						parentId: replyingTo.id,
					});
					setReplyingTo(null);
				} else {
					await onCreateComment(data);
				}
			} catch (error) {
				console.error("Comment submission error:", error);
				throw error;
			}
		},
		[editingComment, replyingTo, onUpdateComment, onCreateComment]
	);

	// Format date
	const formatDate = (dateString: string): string => {
		const date = new Date(dateString);
		const now = new Date();
		const diffInHours = (now.getTime() - date.getTime()) / (1000 * 60 * 60);

		if (diffInHours < 1) {
			return "Just now";
		} else if (diffInHours < 24) {
			const hours = Math.floor(diffInHours);
			return `${hours} hour${hours !== 1 ? "s" : ""} ago`;
		} else if (diffInHours < 168) {
			// 7 days
			const days = Math.floor(diffInHours / 24);
			return `${days} day${days !== 1 ? "s" : ""} ago`;
		} else {
			return date.toLocaleDateString("en-US", {
				year: "numeric",
				month: "short",
				day: "numeric",
			});
		}
	};

	// Render comment content with mentions
	const renderContent = (
		content: string,
		projectMemberNames: Record<string, string>
	) => {
		// Simple approach: find exact mentions and highlight them
		const parts = [];
		let currentIndex = 0;

		// Get all possible mention texts from projectMemberNames
		const mentionTexts = Object.values(projectMemberNames);

		while (currentIndex < content.length) {
			const atIndex = content.indexOf("@", currentIndex);

			if (atIndex === -1) {
				// No more @ symbols, add remaining text
				parts.push(content.substring(currentIndex));
				break;
			}

			// Add text before the @
			if (atIndex > currentIndex) {
				parts.push(content.substring(currentIndex, atIndex));
			}

			// Find the longest matching mention
			let bestMatch = "";
			let bestEndIndex = atIndex + 1;

			for (const fullName of mentionTexts) {
				const endIndex = atIndex + fullName.length + 1; // +1 for @
				if (endIndex <= content.length) {
					const potentialMatch = content.substring(atIndex, endIndex);
					if (
						potentialMatch === `@${fullName}` &&
						fullName.length > bestMatch.length
					) {
						bestMatch = fullName;
						bestEndIndex = endIndex;
					}
				}
			}

			if (bestMatch) {
				// Highlight the exact mention
				parts.push(
					<span
						key={`mention-${atIndex}`}
						className="bg-blue-100 text-blue-800 px-1 rounded"
					>
						@{bestMatch}
					</span>
				);
				currentIndex = bestEndIndex;
			} else {
				// If no match found, just show the @ symbol and continue
				parts.push("@");
				currentIndex = atIndex + 1;
			}
		}

		return parts.length > 0 ? parts : [content];
	};

	// Render single comment
	const renderComment = (comment: BugComment, isReply = false) => {
		const hasReplies = comment.replies && comment.replies.length > 0;
		const isExpanded = expandedReplies.has(comment.id);
		const isEditing = editingComment?.id === comment.id;
		const isReplying = replyingTo?.id === comment.id;

		return (
			<div
				key={comment.id}
				className={cn(
					"space-y-3",
					isReply && "ml-8 border-l-2 border-gray-200 pl-4"
				)}
			>
				{/* Comment Card */}
				<Card
					className={cn("transition-all", isEditing && "ring-2 ring-blue-500")}
				>
					<CardHeader className="pb-3">
						<div className="flex items-start justify-between">
							<div className="flex items-center gap-3">
								<Avatar className="h-8 w-8">
									<AvatarFallback className="text-xs">
										{getUserInitials(comment.author)}
									</AvatarFallback>
								</Avatar>
								<div>
									<div className="flex items-center gap-2">
										<p className="text-sm font-medium text-gray-900">
											{getUserDisplayName(comment.author)}
										</p>
										{comment.author.id === currentUser?.id && (
											<Badge variant="secondary" className="text-xs">
												You
											</Badge>
										)}
									</div>
									<div className="flex items-center gap-2 text-xs text-gray-500">
										<Calendar className="h-3 w-3" />
										<span>{formatDate(comment.createdAt)}</span>
										{comment.updatedAt !== comment.createdAt && (
											<>
												<span>•</span>
												<span>Edited</span>
											</>
										)}
									</div>
								</div>
							</div>

							{/* Comment Actions */}
							<DropdownMenu>
								<DropdownMenuTrigger asChild>
									<Button variant="ghost" size="sm" className="h-8 w-8 p-0">
										<MoreHorizontal className="h-4 w-4" />
									</Button>
								</DropdownMenuTrigger>
								<DropdownMenuContent align="end">
									{canReply && (
										<DropdownMenuItem onClick={() => handleReply(comment)}>
											<Reply className="mr-2 h-4 w-4" />
											Reply
										</DropdownMenuItem>
									)}
									{canEdit(comment) && (
										<DropdownMenuItem onClick={() => handleEdit(comment)}>
											<Edit3 className="mr-2 h-4 w-4" />
											Edit
										</DropdownMenuItem>
									)}
									{canDelete(comment) && (
										<>
											<DropdownMenuSeparator />
											<DropdownMenuItem
												onClick={() => handleDelete(comment)}
												className="text-red-600 focus:text-red-600"
											>
												<Trash2 className="mr-2 h-4 w-4" />
												Delete
											</DropdownMenuItem>
										</>
									)}
								</DropdownMenuContent>
							</DropdownMenu>
						</div>
					</CardHeader>

					<CardContent className="pt-0">
						{/* Comment Content */}
						{isEditing ? (
							<BugCommentForm
								comment={comment}
								onSubmit={handleCommentSubmit}
								onCancel={() => setEditingComment(null)}
								projectMembers={projectMembers}
								submitLabel="Update Comment"
							/>
						) : (
							<div className="space-y-3">
								<div className="text-sm text-gray-900 whitespace-pre-wrap">
									{renderContent(comment.content, projectMemberNames)}
								</div>

								{/* Comment Attachments */}
								{comment.attachments && comment.attachments.length > 0 && (
									<BugAttachmentViewer
										attachments={comment.attachments}
										className="mt-3"
									/>
								)}
							</div>
						)}

						{/* Reply Form */}
						{isReplying && (
							<div className="mt-4">
								<BugCommentForm
									parentComment={comment}
									onSubmit={handleCommentSubmit}
									onCancel={() => setReplyingTo(null)}
									projectMembers={projectMembers}
									placeholder={`Reply to ${getUserDisplayName(
										comment.author
									)}...`}
									submitLabel="Post Reply"
								/>
							</div>
						)}

						{/* Replies Toggle */}
						{hasReplies && !isReply && (
							<div className="mt-3 pt-3 border-t border-gray-100">
								<Button
									variant="ghost"
									size="sm"
									onClick={() => toggleReplies(comment.id)}
									className="text-gray-600 hover:text-gray-900"
								>
									{isExpanded ? (
										<ChevronUp className="mr-1 h-4 w-4" />
									) : (
										<ChevronDown className="mr-1 h-4 w-4" />
									)}
									{comment.replies?.length} repl
									{comment.replies?.length === 1 ? "y" : "ies"}
								</Button>
							</div>
						)}
					</CardContent>
				</Card>

				{/* Replies */}
				{hasReplies && isExpanded && !isReply && (
					<div className="space-y-3">
						{comment.replies?.map((reply) => renderComment(reply, true))}
					</div>
				)}
			</div>
		);
	};

	if (loading) {
		return (
			<div className={cn("space-y-4", className)}>
				<div className="flex items-center justify-center py-8">
					<Loader2 className="h-8 w-8 animate-spin text-gray-400" />
					<span className="ml-2 text-gray-600">Loading comments...</span>
				</div>
			</div>
		);
	}

	return (
		<div className={cn("space-y-6", className)}>
			{/* Header */}
			{!hideHeader && (
				<div className="flex items-center justify-between">
					<div className="flex items-center gap-2">
						<MessageSquare className="h-5 w-5 text-gray-500" />
						<h3 className="text-lg font-semibold text-gray-900">
							Comments ({comments.length})
						</h3>
					</div>

					{/* Sort Options */}
					<Select
						value={sortBy}
						onValueChange={(value: SortOption) => setSortBy(value)}
					>
						<SelectTrigger className="w-40">
							<SelectValue />
						</SelectTrigger>
						<SelectContent>
							{Object.entries(SORT_OPTIONS).map(([key, option]) => {
								const Icon = option.icon;
								return (
									<SelectItem key={key} value={key}>
										<div className="flex items-center gap-2">
											<Icon className="h-4 w-4" />
											{option.label}
										</div>
									</SelectItem>
								);
							})}
						</SelectContent>
					</Select>
				</div>
			)}

			{/* New Comment Form */}
			{!replyingTo && !editingComment && (
				<BugCommentForm
					onSubmit={handleCommentSubmit}
					projectMembers={projectMembers}
					placeholder="Add a comment..."
					submitLabel="Post Comment"
				/>
			)}

			{/* Comments */}
			{topLevelComments.length === 0 ? (
				<div className="text-center py-8">
					<MessageSquare className="h-12 w-12 text-gray-400 mx-auto mb-4" />
					<h3 className="text-lg font-semibold text-gray-900 mb-2">
						No Comments Yet
					</h3>
					<p className="text-gray-600">
						Be the first to add a comment to this bug.
					</p>
				</div>
			) : (
				<div className="space-y-4">
					{topLevelComments.map((comment) => renderComment(comment))}
				</div>
			)}

			{/* Delete Confirmation Dialog */}
			<AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Delete Comment</AlertDialogTitle>
						<AlertDialogDescription>
							Are you sure you want to delete this comment? This action cannot
							be undone.
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={confirmDelete}
							className="bg-red-600 hover:bg-red-700"
						>
							Delete
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>
		</div>
	);
}
