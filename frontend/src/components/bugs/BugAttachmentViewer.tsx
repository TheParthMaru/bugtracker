/**
 * BugAttachmentViewer Component
 *
 * A comprehensive attachment viewer component for displaying bug attachments
 * with preview capabilities, download functionality, and management options.
 *
 * Features:
 * - File preview for images and documents
 * - Download functionality for all file types
 * - File type icons and metadata display
 * - Delete functionality with confirmation
 * - Grid and list view options
 * - Loading states and error handling
 * - Accessibility support
 */

import React, { useState } from "react";
import {
	Download,
	Trash2,
	Eye,
	File,
	Image,
	FileText,
	Archive,
	Video,
	Music,
	X,
	ChevronDown,
	ChevronRight,
	AlertCircle,
	CheckCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
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
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { toast } from "react-toastify";
import API from "@/services/api";
import type { BugAttachment } from "@/types/bug";

interface BugAttachmentViewerProps {
	attachments: BugAttachment[];
	projectId?: string;
	projectTicketNumber?: number; // Changed from bugId to projectTicketNumber
	onDownload?: (attachment: BugAttachment) => Promise<void>;
	onDelete?: (attachment: BugAttachment) => Promise<void>;
	canDelete?: boolean;
	isExpanded?: boolean;
	className?: string;
}

// File type categories
const FILE_TYPE_CATEGORIES = {
	image: [
		"image/jpeg",
		"image/jpg",
		"image/png",
		"image/gif",
		"image/webp",
		"image/svg+xml",
	],
	document: [
		"application/pdf",
		"application/msword",
		"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
		"application/vnd.ms-excel",
		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
		"text/plain",
		"text/csv",
		"application/json",
		"text/xml",
	],
	archive: [
		"application/zip",
		"application/x-rar-compressed",
		"application/x-7z-compressed",
		"application/x-tar",
		"application/gzip",
	],
	video: ["video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv"],
	audio: ["audio/mpeg", "audio/wav", "audio/ogg", "audio/mp3"],
};

export function BugAttachmentViewer({
	attachments,
	projectId,
	projectTicketNumber,
	onDownload,
	onDelete,
	canDelete = false,
	isExpanded = true,
	className,
}: BugAttachmentViewerProps) {
	const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
	const [attachmentToDelete, setAttachmentToDelete] =
		useState<BugAttachment | null>(null);
	const [downloading, setDownloading] = useState<string | null>(null);
	const [deleting, setDeleting] = useState<string | null>(null);

	// Format file size
	const formatFileSize = (bytes: number): string => {
		if (bytes === 0) return "0 Bytes";
		const k = 1024;
		const sizes = ["Bytes", "KB", "MB", "GB"];
		const i = Math.floor(Math.log(bytes) / Math.log(k));
		return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
	};

	// Get file type category
	const getFileTypeCategory = (mimeType: string): string => {
		for (const [category, types] of Object.entries(FILE_TYPE_CATEGORIES)) {
			if (types.includes(mimeType)) {
				return category;
			}
		}
		return "other";
	};

	// Get file icon
	const getFileIcon = (mimeType: string) => {
		const category = getFileTypeCategory(mimeType);
		switch (category) {
			case "image":
				return <Image className="h-4 w-4" />;
			case "document":
				return <FileText className="h-4 w-4" />;
			case "archive":
				return <Archive className="h-4 w-4" />;
			case "video":
				return <Video className="h-4 w-4" />;
			case "audio":
				return <Music className="h-4 w-4" />;
			default:
				return <File className="h-4 w-4" />;
		}
	};

	// Get file type color
	const getFileTypeColor = (mimeType: string): string => {
		const category = getFileTypeCategory(mimeType);
		switch (category) {
			case "image":
				return "bg-blue-100 text-blue-800";
			case "document":
				return "bg-green-100 text-green-800";
			case "archive":
				return "bg-purple-100 text-purple-800";
			case "video":
				return "bg-red-100 text-red-800";
			case "audio":
				return "bg-yellow-100 text-yellow-800";
			default:
				return "bg-gray-100 text-gray-800";
		}
	};

	// Handle download
	const handleDownload = async (attachment: BugAttachment) => {
		if (!onDownload) return;

		setDownloading(attachment.id.toString());
		try {
			await onDownload(attachment);
			toast.success("File downloaded successfully");
		} catch (error) {
			console.error("Download error:", error);
			toast.error("Failed to download file");
		} finally {
			setDownloading(null);
		}
	};

	// Handle preview - open in new tab
	const handlePreview = async (attachment: BugAttachment) => {
		try {
			// Get the authentication token
			const token = localStorage.getItem("bugtracker_token");
			if (!token) {
				toast.error("Authentication required to view file");
				return;
			}

			// Check if projectId and bugId are available
			if (!projectId || !projectTicketNumber) {
				toast.error("Cannot preview file: missing project or bug context");
				return;
			}

			// Create a blob URL with authentication
			const response = await fetch(
				`${API.defaults.baseURL}/projects/${projectId}/bugs/${projectTicketNumber}/attachments/${attachment.id}`,
				{
					headers: {
						Authorization: `Bearer ${token}`,
					},
				}
			);

			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}

			const blob = await response.blob();
			const url = window.URL.createObjectURL(blob);
			window.open(url, "_blank");

			// Clean up the blob URL after a delay
			setTimeout(() => window.URL.revokeObjectURL(url), 1000);
		} catch (error) {
			console.error("Preview error:", error);
			toast.error("Failed to open file");
		}
	};

	// Handle delete
	const handleDelete = async (attachment: BugAttachment) => {
		if (!onDelete) return;

		setDeleting(attachment.id.toString());
		try {
			await onDelete(attachment);
			setDeleteDialogOpen(false);
			setAttachmentToDelete(null);
		} catch (error) {
			console.error("Delete error:", error);
			toast.error("Failed to delete file");
		} finally {
			setDeleting(null);
		}
	};

	// Open delete confirmation
	const openDeleteDialog = (attachment: BugAttachment) => {
		setAttachmentToDelete(attachment);
		setDeleteDialogOpen(true);
	};

	// Format date
	const formatDate = (dateString: string): string => {
		return new Date(dateString).toLocaleDateString("en-US", {
			year: "numeric",
			month: "short",
			day: "numeric",
			hour: "2-digit",
			minute: "2-digit",
		});
	};

	// Get user display name
	const getUserDisplayName = (user: any): string => {
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

	if (attachments.length === 0) {
		return (
			<div className={cn("text-center py-8", className)}>
				<File className="h-12 w-12 text-gray-400 mx-auto mb-4" />
				<h3 className="text-lg font-semibold text-gray-900 mb-2">
					No Attachments
				</h3>
				<p className="text-gray-600">
					No files have been attached to this bug yet.
				</p>
			</div>
		);
	}

	return (
		<div className={cn("space-y-4", className)}>
			{/* Attachments Grid */}
			{isExpanded && (
				<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 mt-4">
					{attachments.map((attachment) => (
						<Card
							key={attachment.id}
							className="group hover:shadow-md transition-shadow"
						>
							<CardContent className="p-4">
								<div className="space-y-3">
									{/* File Icon */}
									<div className="relative">
										<div className="aspect-square rounded-lg bg-gray-100 flex items-center justify-center">
											{getFileIcon(attachment.mimeType)}
										</div>

										{/* Action Overlay */}
										<div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-50 transition-all duration-200 flex items-center justify-center opacity-0 group-hover:opacity-100">
											<div className="flex items-center gap-2">
												<Button
													variant="secondary"
													size="sm"
													onClick={() => handlePreview(attachment)}
												>
													<Eye className="h-4 w-4" />
												</Button>
												<Button
													variant="secondary"
													size="sm"
													onClick={() => handleDownload(attachment)}
													disabled={downloading === attachment.id.toString()}
												>
													{downloading === attachment.id.toString() ? (
														<CheckCircle className="h-4 w-4" />
													) : (
														<Download className="h-4 w-4" />
													)}
												</Button>
												{canDelete && (
													<Button
														variant="destructive"
														size="sm"
														onClick={() => openDeleteDialog(attachment)}
													>
														<Trash2 className="h-4 w-4" />
													</Button>
												)}
											</div>
										</div>
									</div>

									{/* File Info */}
									<div className="space-y-1">
										<p className="text-sm font-medium text-gray-900 truncate">
											{attachment.originalFilename}
										</p>
										<div className="flex items-center gap-2 text-xs text-gray-500">
											<Badge
												variant="outline"
												className={getFileTypeColor(attachment.mimeType)}
											>
												{attachment.mimeType.split("/")[1]?.toUpperCase() ||
													"FILE"}
											</Badge>
											<span>{formatFileSize(attachment.fileSize)}</span>
										</div>
										<p className="text-xs text-gray-400">
											Uploaded by {getUserDisplayName(attachment.uploadedBy)}
										</p>
										<p className="text-xs text-gray-400">
											{formatDate(attachment.createdAt)}
										</p>
									</div>
								</div>
							</CardContent>
						</Card>
					))}
				</div>
			)}

			{/* Delete Confirmation Dialog */}
			<AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Delete Attachment</AlertDialogTitle>
						<AlertDialogDescription>
							Are you sure you want to delete "
							{attachmentToDelete?.originalFilename}"? This action cannot be
							undone.
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={() =>
								attachmentToDelete && handleDelete(attachmentToDelete)
							}
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
