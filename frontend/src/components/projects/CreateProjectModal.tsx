/**
 * CreateProjectModal Component
 *
 * A modal dialog for creating new projects with form validation and proper UX.
 * Includes all necessary fields and validation for project creation.
 *
 * Features:
 * - Form validation with real-time feedback
 * - Loading states during submission
 * - Proper accessibility with focus management
 * - Character count for fields
 * - Error handling and display
 * - Slug preview generation
 * - Responsive design
 */

import React, { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { FolderPlus, Loader2, Check, X } from "lucide-react";
import {
	Dialog,
	DialogContent,
	DialogHeader,
	DialogTitle,
	DialogDescription,
	DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
	Form,
	FormControl,
	FormDescription,
	FormField,
	FormItem,
	FormLabel,
	FormMessage,
} from "@/components/ui/form";
import { cn } from "@/lib/utils";
import type {
	CreateProjectModalProps,
	CreateProjectRequest,
} from "@/types/project";

// Form validation schema
const createProjectSchema = z.object({
	name: z
		.string()
		.min(2, "Project name must be at least 2 characters")
		.max(100, "Project name cannot exceed 100 characters")
		.regex(
			/^[a-zA-Z0-9\s\-_]+$/,
			"Project name can only contain letters, numbers, spaces, hyphens, and underscores"
		),
	description: z
		.string()
		.max(500, "Description cannot exceed 500 characters")
		.optional()
		.or(z.literal("")),
});

type CreateProjectFormData = z.infer<typeof createProjectSchema>;

export function CreateProjectModal({
	isOpen,
	onClose,
	onSubmit,
	isLoading = false,
}: CreateProjectModalProps) {
	const [previewSlug, setPreviewSlug] = useState("");

	const form = useForm<CreateProjectFormData>({
		resolver: zodResolver(createProjectSchema),
		defaultValues: {
			name: "",
			description: "",
		},
		mode: "onChange", // Enable real-time validation
	});

	// Generate slug preview
	const generateSlug = (name: string): string => {
		return name
			.toLowerCase()
			.replace(/[^a-z0-9\s-]/g, "")
			.replace(/\s+/g, "-")
			.replace(/-+/g, "-")
			.trim();
	};

	// Watch name field for slug preview
	const watchedName = form.watch("name");
	useEffect(() => {
		if (watchedName) {
			setPreviewSlug(generateSlug(watchedName));
		} else {
			setPreviewSlug("");
		}
	}, [watchedName]);

	// Handle form submission
	const handleSubmit = (data: CreateProjectFormData) => {
		const formData: CreateProjectRequest = {
			name: data.name.trim(),
			description: data.description?.trim() || undefined,
		};
		onSubmit(formData);
	};

	// Handle modal close
	const handleClose = () => {
		if (!isLoading) {
			form.reset();
			setPreviewSlug("");
			onClose();
		}
	};

	// Reset form when modal opens
	useEffect(() => {
		if (isOpen) {
			form.reset();
			setPreviewSlug("");
		}
	}, [isOpen, form]);

	// Handle escape key
	useEffect(() => {
		const handleEscape = (e: KeyboardEvent) => {
			if (e.key === "Escape" && isOpen && !isLoading) {
				handleClose();
			}
		};

		if (isOpen) {
			document.addEventListener("keydown", handleEscape);
			return () => document.removeEventListener("keydown", handleEscape);
		}
	}, [isOpen, isLoading]);

	return (
		<Dialog open={isOpen} onOpenChange={handleClose}>
			<DialogContent className="sm:max-w-[500px]">
				<DialogHeader>
					<DialogTitle className="flex items-center gap-2">
						<FolderPlus className="h-5 w-5" />
						Create New Project
					</DialogTitle>
					<DialogDescription>
						Create a new project to organize your teams and track bugs. You can
						update these settings later.
					</DialogDescription>
				</DialogHeader>

				<Form {...form}>
					<form
						onSubmit={form.handleSubmit(handleSubmit)}
						className="space-y-6"
					>
						{/* Project Name */}
						<FormField
							control={form.control}
							name="name"
							render={({ field }) => (
								<FormItem>
									<FormLabel>
										Project Name <span className="text-destructive">*</span>
									</FormLabel>
									<FormControl>
										<Input
											{...field}
											placeholder="Enter project name"
											disabled={isLoading}
											className={cn(
												form.formState.errors.name && "border-destructive"
											)}
										/>
									</FormControl>
									<div className="flex items-center justify-between">
										<FormDescription>
											This will be the display name for your project
										</FormDescription>
										<span
											className={cn(
												"text-xs",
												field.value.length > 100
													? "text-destructive"
													: "text-muted-foreground"
											)}
										>
											{field.value.length}/100
										</span>
									</div>
									<FormMessage />
								</FormItem>
							)}
						/>

						{/* Slug Preview */}
						{previewSlug && (
							<div className="rounded-lg border bg-muted/50 p-3">
								<div className="flex items-center gap-2 text-sm">
									<span className="text-muted-foreground">Project URL:</span>
									<code className="rounded bg-muted px-2 py-1 text-xs break-all">
										/projects/{previewSlug}
									</code>
								</div>
							</div>
						)}

						{/* Description */}
						<FormField
							control={form.control}
							name="description"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Description</FormLabel>
									<FormControl>
										<Textarea
											{...field}
											placeholder="Describe your project's purpose and goals"
											disabled={isLoading}
											className={cn(
												"min-h-[100px] resize-none",
												form.formState.errors.description &&
													"border-destructive"
											)}
										/>
									</FormControl>
									<div className="flex items-center justify-between">
										<FormDescription>
											Help others understand what your project is about
										</FormDescription>
										<span
											className={cn(
												"text-xs",
												(field.value?.length || 0) > 500
													? "text-destructive"
													: "text-muted-foreground"
											)}
										>
											{field.value?.length || 0}/500
										</span>
									</div>
									<FormMessage />
								</FormItem>
							)}
						/>

						{/* Information Note */}
						<div className="rounded-lg border bg-blue-50 p-3">
							<div className="text-sm text-blue-800">
								<strong>Note:</strong> All projects are publicly visible in this
								open-source bug tracker. Anyone can request to join your
								project, but you'll need to approve their membership.
							</div>
						</div>

						{/* Submit Buttons */}
						<DialogFooter>
							<Button
								type="button"
								variant="outline"
								onClick={handleClose}
								disabled={isLoading}
							>
								Cancel
							</Button>
							<Button
								type="submit"
								disabled={isLoading || !form.formState.isValid}
								className={cn(
									!form.formState.isValid && "opacity-50 cursor-not-allowed"
								)}
								title={
									!form.formState.isValid
										? "Please fill in all required fields correctly"
										: undefined
								}
							>
								{isLoading ? (
									<>
										<Loader2 className="h-4 w-4 mr-2 animate-spin" />
										Creating...
									</>
								) : (
									<>
										<Check className="h-4 w-4 mr-2" />
										Create Project
									</>
								)}
							</Button>
						</DialogFooter>
					</form>
				</Form>
			</DialogContent>
		</Dialog>
	);
}

// Hook for using the modal
export function useCreateProjectModal() {
	const [isOpen, setIsOpen] = useState(false);

	const openModal = () => setIsOpen(true);
	const closeModal = () => setIsOpen(false);

	return {
		isOpen,
		openModal,
		closeModal,
	};
}

// Example usage component
export function CreateProjectButton({
	onProjectCreated,
	disabled = false,
}: {
	onProjectCreated?: (project: any) => void;
	disabled?: boolean;
}) {
	const { isOpen, openModal, closeModal } = useCreateProjectModal();
	const [isLoading, setIsLoading] = useState(false);

	const handleSubmit = async (data: CreateProjectRequest) => {
		setIsLoading(true);
		try {
			// This would typically use the project service
			// const newProject = await projectService.createProject(data);
			// onProjectCreated?.(newProject);
			console.log("Creating project:", data);
			closeModal();
		} catch (error) {
			console.error("Failed to create project:", error);
		} finally {
			setIsLoading(false);
		}
	};

	return (
		<>
			<Button onClick={openModal} disabled={disabled}>
				<FolderPlus className="h-4 w-4 mr-2" />
				Create Project
			</Button>

			<CreateProjectModal
				isOpen={isOpen}
				onClose={closeModal}
				onSubmit={handleSubmit}
				isLoading={isLoading}
			/>
		</>
	);
}
