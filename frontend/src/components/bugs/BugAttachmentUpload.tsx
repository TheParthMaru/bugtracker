/**
 * BugAttachmentUpload Component
 *
 * A comprehensive file upload component for bug attachments with drag-and-drop
 * support, progress tracking, file validation, and preview capabilities.
 *
 * Features:
 * - Drag and drop file upload
 * - Multiple file selection
 * - File type and size validation
 * - Upload progress tracking
 * - File preview for images
 * - Error handling and retry
 * - Accessibility support
 */

import React, { useState, useCallback, useRef } from "react";
import {
	Upload,
	X,
	File,
	Image,
	AlertCircle,
	CheckCircle,
	Loader2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";
import { toast } from "react-toastify";

interface FileUpload {
	id: string;
	file: File;
	progress: number;
	status: "pending" | "uploading" | "success" | "error";
	error?: string;
	preview?: string;
}

interface BugAttachmentUploadProps {
	onUpload: (files: File[]) => Promise<void>;
	onCancel?: () => void;
	maxFiles?: number;
	maxFileSize?: number; // in bytes
	allowedTypes?: string[];
	disabled?: boolean;
	className?: string;
}

// Default file size limit (10MB)
const DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

// Default allowed file types
const DEFAULT_ALLOWED_TYPES = [
	// Images
	"image/jpeg",
	"image/jpg",
	"image/png",
	"image/gif",
	"image/webp",
	"image/svg+xml",
	// Documents
	"application/pdf",
	"application/msword",
	"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
	"application/vnd.ms-excel",
	"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
	"text/plain",
	"text/csv",
	// Archives
	"application/zip",
	"application/x-rar-compressed",
	"application/x-7z-compressed",
	// Logs
	"text/plain",
	"application/json",
	"text/xml",
];

export function BugAttachmentUpload({
	onUpload,
	onCancel,
	maxFiles = 5, // TODO: Consider reducing this limit for production deployment to prevent abuse
	maxFileSize = DEFAULT_MAX_FILE_SIZE,
	allowedTypes = DEFAULT_ALLOWED_TYPES,
	disabled = false,
	className,
}: BugAttachmentUploadProps) {
	const [uploads, setUploads] = useState<FileUpload[]>([]);
	const [isDragOver, setIsDragOver] = useState(false);
	const [isUploading, setIsUploading] = useState(false);
	const fileInputRef = useRef<HTMLInputElement>(null);

	// Validate file
	const validateFile = (file: File): string | null => {
		// Check file size
		if (file.size > maxFileSize) {
			return `File size exceeds ${formatFileSize(maxFileSize)}`;
		}

		// Check file type
		if (!allowedTypes.includes(file.type)) {
			return `File type ${file.type} is not allowed`;
		}

		// Check if we already have this file
		const existingFile = uploads.find(
			(upload) =>
				upload.file.name === file.name && upload.file.size === file.size
		);
		if (existingFile) {
			return "File already selected";
		}

		return null;
	};

	// Format file size
	const formatFileSize = (bytes: number): string => {
		if (bytes === 0) return "0 Bytes";
		const k = 1024;
		const sizes = ["Bytes", "KB", "MB", "GB"];
		const i = Math.floor(Math.log(bytes) / Math.log(k));
		return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
	};

	// Get file icon
	const getFileIcon = (file: File) => {
		if (file.type.startsWith("image/")) {
			return <Image className="h-4 w-4" />;
		}
		return <File className="h-4 w-4" />;
	};

	// Handle file selection
	const handleFileSelect = useCallback(
		(files: FileList | null) => {
			if (!files) return;

			const newUploads: FileUpload[] = [];
			const errors: string[] = [];

			Array.from(files).forEach((file) => {
				const error = validateFile(file);
				if (error) {
					errors.push(`${file.name}: ${error}`);
					return;
				}

				if (uploads.length + newUploads.length >= maxFiles) {
					errors.push(`Maximum ${maxFiles} files allowed`);
					return;
				}

				const upload: FileUpload = {
					id: Math.random().toString(36).substr(2, 9),
					file,
					progress: 0,
					status: "pending",
				};

				// Generate preview for images
				if (file.type.startsWith("image/")) {
					const reader = new FileReader();
					reader.onload = (e) => {
						setUploads((prev) =>
							prev.map((u) =>
								u.id === upload.id
									? { ...u, preview: e.target?.result as string }
									: u
							)
						);
					};
					reader.readAsDataURL(file);
				}

				newUploads.push(upload);
			});

			if (errors.length > 0) {
				errors.forEach((error) => toast.error(error));
			}

			if (newUploads.length > 0) {
				setUploads((prev) => [...prev, ...newUploads]);
			}
		},
		[uploads, maxFiles, maxFileSize, allowedTypes]
	);

	// Handle drag and drop
	const handleDragOver = useCallback(
		(e: React.DragEvent) => {
			e.preventDefault();
			if (!disabled) {
				setIsDragOver(true);
			}
		},
		[disabled]
	);

	const handleDragLeave = useCallback((e: React.DragEvent) => {
		e.preventDefault();
		setIsDragOver(false);
	}, []);

	const handleDrop = useCallback(
		(e: React.DragEvent) => {
			e.preventDefault();
			setIsDragOver(false);
			if (!disabled) {
				handleFileSelect(e.dataTransfer.files);
			}
		},
		[disabled, handleFileSelect]
	);

	// Handle file input change
	const handleFileInputChange = useCallback(
		(e: React.ChangeEvent<HTMLInputElement>) => {
			handleFileSelect(e.target.files);
			e.target.value = ""; // Reset input
		},
		[handleFileSelect]
	);

	// Remove file
	const removeFile = useCallback((id: string) => {
		setUploads((prev) => prev.filter((upload) => upload.id !== id));
	}, []);

	// Retry upload
	const retryUpload = useCallback((id: string) => {
		setUploads((prev) =>
			prev.map((upload) =>
				upload.id === id
					? { ...upload, status: "pending", progress: 0, error: undefined }
					: upload
			)
		);
	}, []);

	// Start upload
	const startUpload = useCallback(async () => {
		if (uploads.length === 0 || isUploading) return;

		setIsUploading(true);
		const pendingUploads = uploads.filter(
			(upload) => upload.status === "pending"
		);

		try {
			// Update status to uploading
			setUploads((prev) =>
				prev.map((upload) =>
					pendingUploads.some((p) => p.id === upload.id)
						? { ...upload, status: "uploading" as const }
						: upload
				)
			);

			// Simulate upload progress for each file
			const uploadPromises = pendingUploads.map(async (upload) => {
				// Simulate progress updates
				const progressInterval = setInterval(() => {
					setUploads((prev) =>
						prev.map((u) => {
							if (u.id === upload.id && u.status === "uploading") {
								const newProgress = Math.min(
									u.progress + Math.random() * 20,
									90
								);
								return { ...u, progress: newProgress };
							}
							return u;
						})
					);
				}, 200);

				try {
					// Call the actual upload function
					await onUpload([upload.file]);

					clearInterval(progressInterval);

					// Mark as success
					setUploads((prev) =>
						prev.map((u) =>
							u.id === upload.id
								? { ...u, status: "success" as const, progress: 100 }
								: u
						)
					);
				} catch (error) {
					clearInterval(progressInterval);

					// Mark as error
					setUploads((prev) =>
						prev.map((u) =>
							u.id === upload.id
								? {
										...u,
										status: "error" as const,
										error:
											error instanceof Error ? error.message : "Upload failed",
								  }
								: u
						)
					);
				}
			});

			await Promise.all(uploadPromises);

			// Show success message
			const successCount = uploads.filter((u) => u.status === "success").length;
			if (successCount > 0) {
				toast.success(`${successCount} file(s) uploaded successfully`);
			}
		} catch (error) {
			console.error("Upload error:", error);
			toast.error("Upload failed");
		} finally {
			setIsUploading(false);
		}
	}, [uploads, isUploading, onUpload]);

	// Clear all files
	const clearAll = useCallback(() => {
		setUploads([]);
	}, []);

	// Get upload button text
	const getUploadButtonText = () => {
		if (isUploading) return "Uploading...";
		if (uploads.length === 0) return "Select Files";
		return `Upload ${uploads.length} File${uploads.length !== 1 ? "s" : ""}`;
	};

	// Check if upload button should be disabled
	const isUploadDisabled = disabled || isUploading || uploads.length === 0;

	return (
		<div className={cn("space-y-4", className)}>
			{/* File Input */}
			<input
				ref={fileInputRef}
				type="file"
				multiple
				accept={allowedTypes.join(",")}
				onChange={handleFileInputChange}
				className="hidden"
				disabled={disabled}
			/>

			{/* Upload Area */}
			<Card
				className={cn(
					"border-2 border-dashed transition-colors",
					isDragOver
						? "border-blue-500 bg-blue-50"
						: "border-gray-300 hover:border-gray-400",
					disabled && "opacity-50 cursor-not-allowed"
				)}
				onDragOver={handleDragOver}
				onDragLeave={handleDragLeave}
				onDrop={handleDrop}
			>
				<CardContent className="p-6 text-center">
					<Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
					<h3 className="text-lg font-semibold text-gray-900 mb-2">
						Upload Files
					</h3>
					<p className="text-gray-600 mb-4">
						Drag and drop files here, or{" "}
						<button
							type="button"
							onClick={() => fileInputRef.current?.click()}
							disabled={disabled}
							className="text-blue-600 hover:text-blue-700 underline"
						>
							browse files
						</button>
					</p>
					<div className="text-sm text-gray-500">
						<p>
							Maximum {maxFiles} files, {formatFileSize(maxFileSize)} each
						</p>
						<p>Supported: Images, Documents, Archives, Logs</p>
					</div>
				</CardContent>
			</Card>

			{/* File List */}
			{uploads.length > 0 && (
				<div className="space-y-2">
					<div className="flex items-center justify-between">
						<h4 className="text-sm font-medium text-gray-900">
							Selected Files ({uploads.length})
						</h4>
						<Button
							variant="ghost"
							size="sm"
							onClick={clearAll}
							disabled={isUploading}
						>
							Clear All
						</Button>
					</div>

					{uploads.map((upload) => (
						<Card key={upload.id} className="overflow-hidden">
							<CardContent className="p-4">
								<div className="flex items-center gap-3">
									{/* File Preview/Icon */}
									<div className="flex-shrink-0">
										{upload.preview ? (
											<img
												src={upload.preview}
												alt={upload.file.name}
												className="h-10 w-10 rounded object-cover"
											/>
										) : (
											<div className="h-10 w-10 rounded bg-gray-100 flex items-center justify-center">
												{getFileIcon(upload.file)}
											</div>
										)}
									</div>

									{/* File Info */}
									<div className="flex-1 min-w-0">
										<div className="flex items-center gap-2">
											<p className="text-sm font-medium text-gray-900 truncate">
												{upload.file.name}
											</p>
											{upload.status === "success" && (
												<CheckCircle className="h-4 w-4 text-green-500" />
											)}
											{upload.status === "error" && (
												<AlertCircle className="h-4 w-4 text-red-500" />
											)}
											{upload.status === "uploading" && (
												<Loader2 className="h-4 w-4 text-blue-500 animate-spin" />
											)}
										</div>
										<p className="text-xs text-gray-500">
											{formatFileSize(upload.file.size)}
										</p>
										{upload.error && (
											<p className="text-xs text-red-500 mt-1">
												{upload.error}
											</p>
										)}
									</div>

									{/* Actions */}
									<div className="flex items-center gap-2">
										{upload.status === "error" && (
											<Button
												variant="ghost"
												size="sm"
												onClick={() => retryUpload(upload.id)}
												disabled={isUploading}
											>
												Retry
											</Button>
										)}
										<Button
											variant="ghost"
											size="sm"
											onClick={() => removeFile(upload.id)}
											disabled={isUploading}
										>
											<X className="h-4 w-4" />
										</Button>
									</div>
								</div>

								{/* Progress Bar */}
								{upload.status === "uploading" && (
									<div className="mt-3">
										<Progress value={upload.progress} className="h-2" />
										<p className="text-xs text-gray-500 mt-1">
											{Math.round(upload.progress)}% uploaded
										</p>
									</div>
								)}
							</CardContent>
						</Card>
					))}
				</div>
			)}

			{/* Action Buttons */}
			<div className="flex items-center gap-3">
				<Button
					onClick={startUpload}
					disabled={isUploadDisabled}
					className="flex-1"
				>
					{isUploading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
					{getUploadButtonText()}
				</Button>
				{onCancel && (
					<Button variant="outline" onClick={onCancel} disabled={isUploading}>
						Cancel
					</Button>
				)}
			</div>
		</div>
	);
}
