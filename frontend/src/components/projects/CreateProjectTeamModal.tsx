/**
 * CreateProjectTeamModal Component
 *
 * A modal dialog for creating new teams within a project context.
 * Adapted from CreateTeamModal with project-scoped functionality.
 *
 * Features:
 * - Form validation with real-time feedback
 * - Project-scoped slug preview
 * - Loading states during submission
 * - Proper accessibility with focus management
 * - Character count for fields
 * - Error handling and display
 * - Responsive design
 */

import React, { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
	Users,
	FolderOpen,
	Loader2,
	AlertCircle,
	X,
	Check,
} from "lucide-react";
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
import { Label } from "@/components/ui/label";
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
import type { CreateProjectTeamModalProps } from "@/types/project";
import type { Team, CreateTeamRequest } from "@/types/team";

// Form validation schema
const createProjectTeamSchema = z.object({
	name: z
		.string()
		.min(2, "Team name must be at least 2 characters")
		.max(100, "Team name cannot exceed 100 characters")
		.regex(
			/^[a-zA-Z0-9\s\-_]+$/,
			"Team name can only contain letters, numbers, spaces, hyphens, and underscores"
		),
	description: z
		.string()
		.max(500, "Description cannot exceed 500 characters")
		.optional()
		.or(z.literal("")),
});

type CreateProjectTeamFormData = z.infer<typeof createProjectTeamSchema>;

export function CreateProjectTeamModal({
	isOpen,
	onClose,
	onSubmit,
	projectSlug,
	projectName,
	isLoading = false,
}: CreateProjectTeamModalProps) {
	const [previewSlug, setPreviewSlug] = useState("");

	const form = useForm<CreateProjectTeamFormData>({
		resolver: zodResolver(createProjectTeamSchema),
		defaultValues: {
			name: "",
			description: "",
		},
	});

	// Generate slug preview with project context
	const generateSlug = (name: string): string => {
		const teamSlug = name
			.toLowerCase()
			.replace(/[^a-z0-9\s-]/g, "")
			.replace(/\s+/g, "-")
			.replace(/-+/g, "-")
			.trim();
		return `${projectSlug}-${teamSlug}`;
	};

	// Watch name field for slug preview
	const watchedName = form.watch("name");
	useEffect(() => {
		if (watchedName) {
			setPreviewSlug(generateSlug(watchedName));
		} else {
			setPreviewSlug("");
		}
	}, [watchedName, projectSlug]);

	// Handle form submission
	const handleSubmit = (data: CreateProjectTeamFormData) => {
		const formData: CreateTeamRequest = {
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
						<Users className="h-5 w-5" />
						Create Team in {projectName}
					</DialogTitle>
					<DialogDescription>
						Create a new team within the "{projectName}" project. You can change
						these settings later.
					</DialogDescription>
				</DialogHeader>

				<Form {...form}>
					<form
						onSubmit={form.handleSubmit(handleSubmit)}
						className="space-y-6"
					>
						{/* Team Name */}
						<FormField
							control={form.control}
							name="name"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Team Name *</FormLabel>
									<FormControl>
										<Input
											{...field}
											placeholder="Enter team name"
											disabled={isLoading}
											className={cn(
												form.formState.errors.name && "border-destructive"
											)}
										/>
									</FormControl>
									<div className="flex items-center justify-between">
										<FormDescription>
											This will be the display name for your team
										</FormDescription>
										<span className="text-xs text-muted-foreground">
											{field.value.length}/100
										</span>
									</div>
									<FormMessage />
								</FormItem>
							)}
						/>

						{/* Slug Preview with Project Context */}
						{previewSlug && (
							<div className="rounded-lg border bg-muted/50 p-3">
								<div className="flex items-center gap-2 text-sm">
									<span className="text-muted-foreground">Team URL:</span>
									<code className="rounded bg-muted px-2 py-1 text-xs">
										/projects/{previewSlug}
									</code>
								</div>
								<div className="mt-1 text-xs text-muted-foreground">
									Project: <span className="font-medium">{projectName}</span>
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
											placeholder="Describe your team's purpose and goals within this project"
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
											Help others understand what your team does in this project
										</FormDescription>
										<span className="text-xs text-muted-foreground">
											{field.value?.length || 0}/500
										</span>
									</div>
									<FormMessage />
								</FormItem>
							)}
						/>

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
							>
								{isLoading ? (
									<>
										<Loader2 className="h-4 w-4 mr-2 animate-spin" />
										Creating...
									</>
								) : (
									<>
										<Check className="h-4 w-4 mr-2" />
										Create Team
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
export function useCreateProjectTeamModal() {
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
export function CreateProjectTeamButton({
	projectSlug,
	projectName,
	onTeamCreated,
	disabled = false,
}: {
	projectSlug: string;
	projectName: string;
	onTeamCreated?: (team: Team) => void;
	disabled?: boolean;
}) {
	const { isOpen, openModal, closeModal } = useCreateProjectTeamModal();
	const [isLoading, setIsLoading] = useState(false);

	const handleSubmit = async (data: CreateTeamRequest) => {
		setIsLoading(true);
		try {
			// This would typically use the project service
			// const newTeam = await projectService.createProjectTeam(projectSlug, data);
			// onTeamCreated?.(newTeam);
			console.log("Creating team in project:", { projectSlug, data });
			closeModal();
		} catch (error) {
			console.error("Failed to create team:", error);
		} finally {
			setIsLoading(false);
		}
	};

	return (
		<>
			<Button onClick={openModal} disabled={disabled}>
				<Users className="h-4 w-4 mr-2" />
				Create Team
			</Button>

			<CreateProjectTeamModal
				isOpen={isOpen}
				onClose={closeModal}
				onSubmit={handleSubmit}
				projectSlug={projectSlug}
				projectName={projectName}
				isLoading={isLoading}
			/>
		</>
	);
}
