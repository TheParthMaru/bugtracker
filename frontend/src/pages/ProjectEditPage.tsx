/**
 * ProjectEditPage Component
 *
 * Allows project admins to edit project details.
 * This is a placeholder implementation that will be expanded later.
 *
 * Features:
 * - Project information editing
 * - Form validation
 * - Navigation with back button
 * - Proper routing with slug parameter
 */

import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { projectService } from "@/services/projectService";
import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";
import type { Project } from "@/types/project";
import { toast } from "react-toastify";

export function ProjectEditPage() {
	const { projectSlug } = useParams<{ projectSlug: string }>();
	const navigate = useNavigate();
	const [project, setProject] = useState<Project | null>(null);
	const [loading, setLoading] = useState(true);
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [formData, setFormData] = useState({
		name: "",
		description: "",
	});

	useEffect(() => {
		const loadProject = async () => {
			if (!projectSlug) {
				setError("Project slug is required");
				setLoading(false);
				return;
			}

			try {
				setLoading(true);
				setError(null);
				const projectData = await projectService.getProjectBySlug(projectSlug);
				setProject(projectData);
				setFormData({
					name: projectData.name,
					description: projectData.description || "",
				});
			} catch (err) {
				console.error("Failed to load project:", err);
				const errorMessage =
					err instanceof Error ? err.message : "Failed to load project";
				setError(errorMessage);
				toast.error(errorMessage);
			} finally {
				setLoading(false);
			}
		};

		loadProject();
	}, [projectSlug]);

	const handleBack = () => {
		navigate(`/projects/${projectSlug}`);
	};

	const handleSave = async () => {
		if (!project) return;

		console.log("ProjectEditPage -> handleSave -> Starting project update:", {
			projectId: project.id,
			oldName: project.name,
			oldSlug: project.projectSlug,
			newName: formData.name.trim(),
			newDescription: formData.description.trim() || undefined,
		});

		// Validate form data
		if (!formData.name.trim()) {
			toast.error("Project name is required");
			return;
		}

		if (formData.name.trim().length < 3) {
			toast.error("Project name must be at least 3 characters");
			return;
		}

		if (formData.name.trim().length > 100) {
			toast.error("Project name cannot exceed 100 characters");
			return;
		}

		if (formData.description && formData.description.length > 2000) {
			toast.error("Project description cannot exceed 2000 characters");
			return;
		}

		setSaving(true);
		try {
			// Call the actual project update service
			const updatedProject = await projectService.updateProject(
				project.projectSlug,
				{
					name: formData.name.trim(),
					description: formData.description.trim() || undefined,
				}
			);

			// Log the slug change for debugging
			console.log(
				"ProjectEditPage -> handleSave -> Project updated successfully:",
				{
					oldSlug: project.projectSlug,
					newSlug: updatedProject.projectSlug,
					oldName: project.name,
					newName: updatedProject.name,
					slugChanged: project.projectSlug !== updatedProject.projectSlug,
				}
			);

			toast.success("Project updated successfully!");

			// Navigate using the NEW slug from the backend response
			navigate(`/projects/${updatedProject.projectSlug}`);
		} catch (err) {
			console.error("Failed to update project:", err);
			toast.error(
				err instanceof Error ? err.message : "Failed to update project"
			);
		} finally {
			setSaving(false);
		}
	};

	if (loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-4xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="outline" size="sm" disabled>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
						</Button>
					</div>
					<div className="space-y-4">
						<div className="h-8 bg-muted rounded animate-pulse" />
						<div className="h-4 bg-muted rounded animate-pulse w-1/3" />
						<div className="h-32 bg-muted rounded animate-pulse" />
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	if (error || !project) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-4xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="outline" size="sm" onClick={handleBack}>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
						</Button>
					</div>
					<Card>
						<CardHeader>
							<CardTitle className="text-red-600">Project Not Found</CardTitle>
							<CardDescription>
								{error || "The requested project could not be found."}
							</CardDescription>
						</CardHeader>
						<CardContent>
							<Button onClick={handleBack}>
								<ArrowLeft className="h-4 w-4 mr-2" />
								Back to Project
							</Button>
						</CardContent>
					</Card>
				</main>
				<Footer />
			</div>
		);
	}

	// Check if user is admin
	if (!project.isUserAdmin) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-4xl">
					<div className="flex items-center gap-4 mb-8">
						<Button variant="outline" size="sm" onClick={handleBack}>
							<ArrowLeft className="h-4 w-4 mr-2" />
							Back to Project
						</Button>
					</div>
					<Card>
						<CardHeader>
							<CardTitle className="text-red-600">Access Denied</CardTitle>
							<CardDescription>
								You don't have permission to edit this project. Only project
								admins can modify project settings.
							</CardDescription>
						</CardHeader>
						<CardContent>
							<Button onClick={handleBack}>
								<ArrowLeft className="h-4 w-4 mr-2" />
								Back to Project
							</Button>
						</CardContent>
					</Card>
				</main>
				<Footer />
			</div>
		);
	}

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />

			<main className="flex-1 container mx-auto px-4 py-8 max-w-4xl">
				{/* Breadcrumb */}
				<ProjectBreadcrumb
					projectName={project.name}
					projectSlug={project.projectSlug}
					current="Edit"
					onBackClick={handleBack}
				/>

				{/* Edit Form */}
				<Card>
					<CardHeader>
						<CardTitle>Edit Project Settings</CardTitle>
						<CardDescription>
							Update your project's name and description. These changes will be
							visible to all project members.
						</CardDescription>
					</CardHeader>
					<CardContent className="space-y-6">
						<div className="space-y-2">
							<Label htmlFor="name">Project Name</Label>
							<Input
								id="name"
								value={formData.name}
								onChange={(e) =>
									setFormData({ ...formData, name: e.target.value })
								}
								placeholder="Enter project name"
							/>
							<div className="flex justify-between text-xs text-muted-foreground">
								<span>Required</span>
								<span
									className={formData.name.length > 100 ? "text-red-500" : ""}
								>
									{formData.name.length}/100
								</span>
							</div>
						</div>

						<div className="space-y-2">
							<Label htmlFor="description">Description</Label>
							<Textarea
								id="description"
								value={formData.description}
								onChange={(e) =>
									setFormData({ ...formData, description: e.target.value })
								}
								placeholder="Describe your project's purpose and goals"
								rows={4}
							/>
							<div className="flex justify-between text-xs text-muted-foreground">
								<span>Optional</span>
								<span
									className={
										formData.description.length > 2000 ? "text-red-500" : ""
									}
								>
									{formData.description.length}/2000
								</span>
							</div>
						</div>

						<div className="flex items-center justify-end gap-2 pt-4">
							<Button variant="outline" onClick={handleBack}>
								Cancel
							</Button>
							<Button onClick={handleSave} disabled={saving}>
								{saving ? (
									<>
										<div className="h-4 w-4 mr-2 animate-spin rounded-full border-2 border-current border-t-transparent" />
										Saving...
									</>
								) : (
									<>
										<Save className="h-4 w-4 mr-2" />
										Save Changes
									</>
								)}
							</Button>
						</div>
					</CardContent>
				</Card>

				{/* Commented the additional settings as it is not required for now */}
				{/* Additional Settings Placeholder
				<Card className="mt-6">
					<CardHeader>
						<CardTitle>Additional Settings</CardTitle>
						<CardDescription>
							More project settings will be available here in future updates.
						</CardDescription>
					</CardHeader>
					<CardContent>
						<div className="text-center py-8 text-muted-foreground">
							<Settings className="h-12 w-12 mx-auto mb-4 opacity-50" />
							<p>Additional settings coming soon</p>
							<p className="text-sm mt-2">
								Member management, project visibility, and more advanced
								settings will be available here.
							</p>
						</div>
					</CardContent>
				</Card> */}
			</main>

			<Footer />
		</div>
	);
}
