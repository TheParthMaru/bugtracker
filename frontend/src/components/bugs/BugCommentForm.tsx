/**
 * BugCommentForm Component
 *
 * A comprehensive comment form component for creating and editing bug comments
 * with rich text support, file attachments, and mention functionality.
 *
 * Features:
 * - Rich text editor with markdown support
 * - File attachment integration
 * - User mention system
 * - Character count and validation
 * - Auto-save draft functionality
 * - Reply to specific comments
 * - Loading states and error handling
 * - Accessibility support
 */

import React, { useState, useEffect, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
	Send,
	Paperclip,
	AtSign,
	Smile,
	X,
	Save,
	Edit3,
	Loader2,
	AlertCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
	Popover,
	PopoverContent,
	PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import { toast } from "react-toastify";
import type { BugComment } from "@/types/bug";
import type { User as BugUser } from "@/types/bug";
import { BugAttachmentUpload } from "./BugAttachmentUpload";

// Form validation schema
const commentSchema = z.object({
	content: z
		.string()
		.min(1, "Comment cannot be empty")
		.max(5000, "Comment cannot exceed 5000 characters"),
});

type CommentFormData = z.infer<typeof commentSchema>;

interface BugCommentFormProps {
	onSubmit: (data: CommentFormData & { attachments?: File[] }) => Promise<void>;
	onCancel?: () => void;
	comment?: BugComment; // For editing existing comments
	parentComment?: BugComment; // For replying to comments
	projectMembers?: BugUser[]; // For mention suggestions
	placeholder?: string;
	submitLabel?: string;
	disabled?: boolean;
	className?: string;
}

// Emoji suggestions
const EMOJI_SUGGESTIONS = [
	"👍",
	"👎",
	"❤️",
	"😊",
	"😢",
	"😡",
	"🎉",
	"🚀",
	"🐛",
	"✅",
	"❌",
	"⚠️",
	"💡",
	"🔥",
	"💯",
];

export function BugCommentForm({
	onSubmit,
	onCancel,
	comment,
	parentComment,
	projectMembers = [],
	placeholder = "Write a comment...",
	submitLabel = "Post Comment",
	disabled = false,
	className,
}: BugCommentFormProps) {
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [showAttachments, setShowAttachments] = useState(false);
	const [attachments, setAttachments] = useState<File[]>([]);
	const [showMentions, setShowMentions] = useState(false);
	const [mentionQuery, setMentionQuery] = useState("");
	const [selectedMentionIndex, setSelectedMentionIndex] = useState(0);
	const [cursorPosition, setCursorPosition] = useState(0);
	const [draftKey, setDraftKey] = useState("");

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

	const form = useForm<CommentFormData>({
		resolver: zodResolver(commentSchema),
		defaultValues: {
			content: comment?.content || "",
		},
		mode: "onChange",
	});

	// Generate draft key for auto-save
	useEffect(() => {
		const key = comment
			? `comment-${comment.id}`
			: `comment-draft-${Date.now()}`;
		setDraftKey(key);
	}, [comment]);

	// Load draft on mount
	useEffect(() => {
		if (!comment) {
			const savedDraft = localStorage.getItem(draftKey);
			if (savedDraft) {
				try {
					const draft = JSON.parse(savedDraft);
					form.setValue("content", draft.content);
					setAttachments(draft.attachments || []);
				} catch (error) {
					console.error("Failed to load draft:", error);
				}
			}
		}
	}, [draftKey, comment, form]);

	// Auto-save draft
	useEffect(() => {
		if (!comment && form.watch("content")) {
			const draft = {
				content: form.watch("content"),
				attachments: attachments,
				timestamp: Date.now(),
			};
			localStorage.setItem(draftKey, JSON.stringify(draft));
		}
	}, [form.watch("content"), attachments, draftKey, comment]);

	// Clear draft after successful submission
	const clearDraft = useCallback(() => {
		if (!comment) {
			localStorage.removeItem(draftKey);
		}
	}, [draftKey, comment]);

	// Handle form submission
	const handleSubmit = async (data: CommentFormData) => {
		if (isSubmitting) return;

		setIsSubmitting(true);
		try {
			await onSubmit({
				...data,
				attachments: attachments.length > 0 ? attachments : undefined,
			});

			// Clear form and draft
			form.reset();
			setAttachments([]);
			setShowAttachments(false);
			clearDraft();

			// Toast notification will be handled by backend via WebSocket
		} catch (error) {
			console.error("Comment submission error:", error);
			toast.error("Failed to post comment");
		} finally {
			setIsSubmitting(false);
		}
	};

	// Handle textarea change with mention detection
	const handleTextareaChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
		const value = e.target.value;
		const cursorPos = e.target.selectionStart;
		setCursorPosition(cursorPos);

		// Check for mention trigger (@) - simple @ detection
		const beforeCursor = value.substring(0, cursorPos);
		const mentionMatch = beforeCursor.match(/@(\w*)$/);

		if (mentionMatch) {
			setMentionQuery(mentionMatch[1]);
			setShowMentions(true);
			setSelectedMentionIndex(0);
		} else {
			setShowMentions(false);
		}

		form.setValue("content", value);
	};

	// Handle mention selection
	const handleMentionSelect = (user: BugUser) => {
		const content = form.getValues("content");
		const beforeMention = content.substring(
			0,
			cursorPosition - mentionQuery.length - 1
		);
		const afterMention = content.substring(cursorPosition);
		// Store the full name as requested - simple and clean
		const displayName = getUserDisplayName(user);
		const newContent = `${beforeMention}@${displayName} ${afterMention}`;

		form.setValue("content", newContent);
		setShowMentions(false);
		setMentionQuery("");

		// Calculate new cursor position after the mention
		const newCursorPos = beforeMention.length + displayName.length + 2; // +2 for @ and space

		// Focus back to textarea and set cursor position
		setTimeout(() => {
			const textarea = document.querySelector(
				'textarea[name="content"]'
			) as HTMLTextAreaElement;
			if (textarea) {
				textarea.focus();
				textarea.setSelectionRange(newCursorPos, newCursorPos);
			}
		}, 0);
	};

	// Handle emoji selection
	const handleEmojiSelect = (emoji: string) => {
		const content = form.getValues("content");
		const newContent = content + emoji;
		form.setValue("content", newContent);

		// Focus back to textarea and position cursor at the end
		setTimeout(() => {
			const textarea = document.querySelector(
				'textarea[name="content"]'
			) as HTMLTextAreaElement;
			if (textarea) {
				textarea.focus();
				textarea.setSelectionRange(newContent.length, newContent.length);
			}
		}, 0);
	};

	// Handle keyboard navigation for mentions
	const handleKeyDown = (e: React.KeyboardEvent) => {
		if (showMentions) {
			if (e.key === "ArrowDown") {
				e.preventDefault();
				setSelectedMentionIndex((prev) =>
					Math.min(prev + 1, filteredMembers.length - 1)
				);
			} else if (e.key === "ArrowUp") {
				e.preventDefault();
				setSelectedMentionIndex((prev) => Math.max(prev - 1, 0));
			} else if (e.key === "Enter" && filteredMembers.length > 0) {
				e.preventDefault();
				handleMentionSelect(filteredMembers[selectedMentionIndex]);
			} else if (e.key === "Escape") {
				setShowMentions(false);
			}
		}
	};

	// Filter members for mentions
	const filteredMembers = projectMembers
		.filter((member) => {
			const fullName = getUserDisplayName(member).toLowerCase();
			const query = mentionQuery.toLowerCase();

			// Prioritize full name matches, then individual field matches
			return (
				fullName.includes(query) ||
				member.firstName.toLowerCase().includes(query) ||
				member.lastName.toLowerCase().includes(query) ||
				member.email.toLowerCase().includes(query)
			);
		})
		.sort((a, b) => {
			// Sort by relevance: exact matches first, then partial matches
			const aFullName = getUserDisplayName(a).toLowerCase();
			const bFullName = getUserDisplayName(b).toLowerCase();
			const query = mentionQuery.toLowerCase();

			const aExactMatch = aFullName === query;
			const bExactMatch = bFullName === query;

			if (aExactMatch && !bExactMatch) return -1;
			if (!aExactMatch && bExactMatch) return 1;

			// Then sort by full name match quality
			const aStartsWith = aFullName.startsWith(query);
			const bStartsWith = bFullName.startsWith(query);

			if (aStartsWith && !bStartsWith) return -1;
			if (!aStartsWith && bStartsWith) return 1;

			return aFullName.localeCompare(bFullName);
		})
		.slice(0, 5);

	// Handle file upload
	const handleFileUpload = async (files: File[]) => {
		setAttachments((prev) => [...prev, ...files]);
	};

	// Remove attachment
	const removeAttachment = (index: number) => {
		setAttachments((prev) => prev.filter((_, i) => i !== index));
	};

	// Format file size
	const formatFileSize = (bytes: number): string => {
		if (bytes === 0) return "0 Bytes";
		const k = 1024;
		const sizes = ["Bytes", "KB", "MB", "GB"];
		const i = Math.floor(Math.log(bytes) / Math.log(k));
		return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
	};

	const content = form.watch("content");
	const contentLength = content?.length || 0;
	const maxLength = 5000;
	const isOverLimit = contentLength > maxLength;

	return (
		<Card className={cn("w-full", className)}>
			<CardContent className="p-4">
				{/* Reply indicator */}
				{parentComment && (
					<div className="mb-3 p-2 bg-blue-50 rounded-md border border-blue-200">
						<div className="flex items-center gap-2 text-sm text-blue-700">
							<AtSign className="h-4 w-4" />
							<span>
								Replying to {getUserDisplayName(parentComment.author)}
							</span>
							<Button
								variant="ghost"
								size="sm"
								onClick={onCancel}
								className="h-6 w-6 p-0 ml-auto"
							>
								<X className="h-3 w-3" />
							</Button>
						</div>
					</div>
				)}

				<form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
					{/* Comment content */}
					<div className="relative">
						<Textarea
							name="content"
							placeholder={placeholder}
							className={cn(
								"min-h-24 resize-none",
								isOverLimit && "border-red-500 focus:border-red-500"
							)}
							{...form.register("content")}
							onChange={handleTextareaChange}
							onKeyDown={handleKeyDown}
							disabled={disabled || isSubmitting}
						/>

						{/* Character count */}
						<div className="flex items-center justify-between mt-2">
							<div className="flex items-center gap-2">
								{/* Toolbar */}
								<Popover>
									<PopoverTrigger asChild>
										<Button
											type="button"
											variant="ghost"
											size="sm"
											disabled={disabled || isSubmitting}
										>
											<AtSign className="h-4 w-4" />
										</Button>
									</PopoverTrigger>
									<PopoverContent className="w-80 p-2">
										<div className="space-y-2">
											<p className="text-sm font-medium">Mention someone</p>
											<div className="space-y-1">
												{projectMembers.map((member) => (
													<button
														key={member.id}
														type="button"
														onClick={() => handleMentionSelect(member)}
														className="flex items-center gap-2 w-full p-2 rounded hover:bg-gray-100 text-left"
													>
														<Avatar className="h-6 w-6">
															<AvatarFallback className="text-xs">
																{getUserInitials(member)}
															</AvatarFallback>
														</Avatar>
														<span className="text-sm">
															{getUserDisplayName(member)}
														</span>
													</button>
												))}
											</div>
										</div>
									</PopoverContent>
								</Popover>

								<Popover>
									<PopoverTrigger asChild>
										<Button
											type="button"
											variant="ghost"
											size="sm"
											disabled={disabled || isSubmitting}
										>
											<Smile className="h-4 w-4" />
										</Button>
									</PopoverTrigger>
									<PopoverContent className="w-64 p-2">
										<div className="grid grid-cols-5 gap-1">
											{EMOJI_SUGGESTIONS.map((emoji, index) => (
												<button
													key={index}
													type="button"
													onClick={() => handleEmojiSelect(emoji)}
													className="p-2 rounded hover:bg-gray-100 text-lg"
												>
													{emoji}
												</button>
											))}
										</div>
									</PopoverContent>
								</Popover>

								<Button
									type="button"
									variant="ghost"
									size="sm"
									onClick={() => setShowAttachments(!showAttachments)}
									disabled={disabled || isSubmitting}
									className={cn(showAttachments && "bg-blue-100 text-blue-700")}
								>
									<Paperclip className="h-4 w-4" />
								</Button>
							</div>

							<div className="flex items-center gap-2">
								{isOverLimit && (
									<AlertCircle className="h-4 w-4 text-red-500" />
								)}
								<span
									className={cn(
										"text-xs",
										isOverLimit ? "text-red-500" : "text-gray-500"
									)}
								>
									{contentLength}/{maxLength}
								</span>
							</div>
						</div>

						{/* Mention suggestions */}
						{showMentions && filteredMembers.length > 0 && (
							<div className="absolute bottom-full left-0 w-64 bg-white border border-gray-200 rounded-lg shadow-lg z-50 max-h-60 overflow-y-auto">
								<div className="p-2 border-b border-gray-200">
									<p className="text-sm font-medium text-gray-700">
										Mention someone ({filteredMembers.length} found)
									</p>
								</div>
								{filteredMembers.map((member, index) => (
									<button
										key={member.id}
										className={cn(
											"w-full p-3 text-left hover:bg-gray-50 transition-colors",
											index === selectedMentionIndex && "bg-blue-100"
										)}
										onClick={() => handleMentionSelect(member)}
									>
										<div className="flex items-center gap-3">
											<Avatar className="h-8 w-8">
												<AvatarFallback className="text-xs">
													{getUserInitials(member)}
												</AvatarFallback>
											</Avatar>
											<div className="flex-1 min-w-0">
												<div className="font-medium text-gray-900 truncate">
													{getUserDisplayName(member)}
												</div>
												<div className="text-sm text-gray-500 truncate">
													{member.email}
												</div>
											</div>
										</div>
									</button>
								))}
							</div>
						)}
					</div>

					{/* File attachments */}
					{showAttachments && (
						<div className="space-y-3">
							{attachments.length > 0 && (
								<div className="space-y-2">
									<h4 className="text-sm font-medium text-gray-900">
										Attachments ({attachments.length})
									</h4>
									<div className="space-y-2">
										{attachments.map((file, index) => (
											<div
												key={index}
												className="flex items-center gap-2 p-2 bg-gray-50 rounded-md"
											>
												<Paperclip className="h-4 w-4 text-gray-500" />
												<div className="flex-1 min-w-0">
													<p className="text-sm font-medium text-gray-900 truncate">
														{file.name}
													</p>
													<p className="text-xs text-gray-500">
														{formatFileSize(file.size)}
													</p>
												</div>
												<Button
													type="button"
													variant="ghost"
													size="sm"
													onClick={() => removeAttachment(index)}
													disabled={isSubmitting}
												>
													<X className="h-4 w-4" />
												</Button>
											</div>
										))}
									</div>
								</div>
							)}

							<BugAttachmentUpload
								onUpload={handleFileUpload}
								maxFiles={5}
								disabled={disabled || isSubmitting}
							/>
						</div>
					)}

					{/* Action buttons */}
					<div className="flex items-center gap-2">
						<Button
							type="submit"
							disabled={
								disabled || isSubmitting || isOverLimit || !content?.trim()
							}
							className="flex items-center gap-2"
						>
							{isSubmitting ? (
								<Loader2 className="h-4 w-4 animate-spin" />
							) : comment ? (
								<Save className="h-4 w-4" />
							) : (
								<Send className="h-4 w-4" />
							)}
							{isSubmitting ? "Posting..." : submitLabel}
						</Button>

						{onCancel && (
							<Button
								type="button"
								variant="outline"
								onClick={onCancel}
								disabled={disabled || isSubmitting}
							>
								Cancel
							</Button>
						)}

						{/* Draft indicator */}
						{!comment && content && (
							<Badge variant="secondary" className="text-xs">
								Draft saved
							</Badge>
						)}
					</div>
				</form>
			</CardContent>
		</Card>
	);
}
