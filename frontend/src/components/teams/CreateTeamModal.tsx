/**
 * CreateTeamModal Component
 *
 * A modal dialog for creating new teams with project selection.
 * Teams must be created within a project context.
 *
 * Features:
 * - Project selection dropdown (mandatory)
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
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
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
import { projectService } from "@/services/projectService";
import type { Project } from "@/types/project";
import type { CreateTeamRequest } from "@/types/team";

// Form validation schema
const createTeamSchema = z.object({
	projectId: z.string().min(1, "Please select a project"),
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

type CreateTeamFormData = z.infer<typeof createTeamSchema>;

export interface CreateTeamModalProps {
	isOpen: boolean;
	onClose: () => void;
	onSubmit: (data: CreateTeamRequest & { projectSlug: string }) => void;
	isLoading?: boolean;
}

export function CreateTeamModal({
	isOpen,
	onClose,
	onSubmit,
	isLoading = false,
}: CreateTeamModalProps) {
	const [previewSlug, setPreviewSlug] = useState("");
	const [projects, setProjects] = useState<Project[]>([]);
	const [projectsLoading, setProjectsLoading] = useState(false);
	const [selectedProject, setSelectedProject] = useState<Project | null>(null);

	const form = useForm<CreateTeamFormData>({
		resolver: zodResolver(createTeamSchema),
		defaultValues: {
			projectId: "",
			name: "",
			description: "",
		},
	});

	// Load user's admin projects (only admins can create teams)
	useEffect(() => {
		const loadProjects = async () => {
			if (!isOpen) return;

			try {
				setProjectsLoading(true);
				const userProjects = await projectService.getUserProjects();
				// Filter to only show projects where user is admin (only admins can create teams)
				const adminProjects = userProjects.filter(
					(project) => project.userRole === "ADMIN"
				);
				setProjects(adminProjects);
			} catch (error) {
				console.error("Failed to load projects:", error);
			} finally {
				setProjectsLoading(false);
			}
		};

		loadProjects();
	}, [isOpen]);

	// Generate slug preview with project context
	const generateSlug = (projectSlug: string, teamName: string): string => {
		const teamSlug = teamName
			.toLowerCase()
			.replace(/[^a-z0-9\s-]/g, "")
			.replace(/\s+/g, "-")
			.replace(/-+/g, "-")
			.trim();
		return `${projectSlug}-${teamSlug}`;
	};

	// Watch form fields for slug preview
	const watchedProjectId = form.watch("projectId");
	const watchedName = form.watch("name");

	useEffect(() => {
		if (watchedProjectId && watchedName) {
			const project = projects.find((p) => p.id === watchedProjectId);
			if (project) {
				setSelectedProject(project);
				setPreviewSlug(generateSlug(project.projectSlug, watchedName));
			}
		} else {
			setPreviewSlug("");
			setSelectedProject(null);
		}
	}, [watchedProjectId, watchedName, projects]);

	// Handle form submission
	const handleSubmit = (data: CreateTeamFormData) => {
		if (!selectedProject) {
			return;
		}

		const formData: CreateTeamRequest & { projectSlug: string } = {
			name: data.name.trim(),
			description: data.description?.trim() || undefined,
			projectSlug: selectedProject.projectSlug,
		};
		onSubmit(formData);
	};

	// Handle modal close
	const handleClose = () => {
		if (!isLoading) {
			form.reset();
			setPreviewSlug("");
			setSelectedProject(null);
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
						Create New Team
					</DialogTitle>
					<DialogDescription>
						Create a new team within a project. You can change these settings
						later.
					</DialogDescription>
				</DialogHeader>

				<Form {...form}>
					<form
						onSubmit={form.handleSubmit(handleSubmit)}
						className="space-y-6"
					>
						{/* Project Selection */}
						<FormField
							control={form.control}
							name="projectId"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Project *</FormLabel>
									<FormControl>
										<Select
											value={field.value}
											onValueChange={field.onChange}
											disabled={isLoading || projectsLoading}
										>
											<SelectTrigger
												className={cn(
													form.formState.errors.projectId &&
														"border-destructive"
												)}
											>
												<SelectValue placeholder="Select a project" />
											</SelectTrigger>
											<SelectContent>
												{projectsLoading ? (
													<div className="px-2 py-1.5 text-sm text-muted-foreground">
														Loading projects...
													</div>
												) : projects.length === 0 ? (
													<div className="px-2 py-1.5 text-sm text-muted-foreground">
														No admin projects available
													</div>
												) : (
													projects.map((project) => (
														<SelectItem key={project.id} value={project.id}>
															<div className="flex items-center gap-2">
																<FolderOpen className="h-4 w-4" />
																{project.name}
															</div>
														</SelectItem>
													))
												)}
											</SelectContent>
										</Select>
									</FormControl>
									<FormDescription>
										Select the project where this team will be created. You can
										change this later.
									</FormDescription>
									<FormMessage />
								</FormItem>
							)}
						/>

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
						{previewSlug && selectedProject && (
							<div className="rounded-lg border bg-muted/50 p-3">
								<div className="flex items-center gap-2 text-sm">
									<span className="text-muted-foreground">Team URL:</span>
									<code className="rounded bg-muted px-2 py-1 text-xs">
										/projects/{previewSlug}
									</code>
								</div>
								<div className="mt-1 text-xs text-muted-foreground">
									Project:{" "}
									<span className="font-medium">{selectedProject.name}</span>
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
								disabled={
									isLoading ||
									!form.formState.isValid ||
									projectsLoading ||
									projects.length === 0
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
export function useCreateTeamModal() {
	const [isOpen, setIsOpen] = useState(false);

	const openModal = () => setIsOpen(true);
	const closeModal = () => setIsOpen(false);

	return {
		isOpen,
		openModal,
		closeModal,
	};
}
