import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Loader2, Save, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
import { Badge } from "@/components/ui/badge";
import { toast } from "react-toastify";

import Navbar from "@/components/Navbar";
import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";
import { bugService } from "@/services/bugService";
import { projectService } from "@/services/projectService";
import type { Bug, CreateBugRequest } from "@/types/bug";
import { BugStatus, BugPriority, BugType } from "@/types/bug";
import type { BugLabel } from "@/types/bug";

export function BugEditPage() {
	const { projectSlug, projectTicketNumber } = useParams<{
		projectSlug: string;
		projectTicketNumber: string;
	}>();
	const navigate = useNavigate();

	const [loading, setLoading] = useState(true);
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [projectName, setProjectName] = useState("");
	const [bug, setBug] = useState<Bug | null>(null);
	const [availableLabels, setAvailableLabels] = useState<BugLabel[]>([]);

	// Form state
	const [formData, setFormData] = useState<CreateBugRequest>({
		title: "",
		description: "",
		type: BugType.ISSUE,
		priority: BugPriority.MEDIUM,
		assigneeId: undefined,
		labelIds: [],
		tags: [],
	});
	const [newTag, setNewTag] = useState("");

	useEffect(() => {
		if (projectSlug && projectTicketNumber) {
			loadData();
		}
	}, [projectSlug, projectTicketNumber]);

	const loadData = async () => {
		if (!projectSlug || !projectTicketNumber) return;

		try {
			setLoading(true);
			setError(null);

			// Load project name
			const project = await projectService.getProjectBySlug(projectSlug);
			setProjectName(project.name);

			// Load bug data
			const bugData = await bugService.getBugByProjectTicketNumber(
				projectSlug,
				parseInt(projectTicketNumber)
			);
			setBug(bugData);

			// Set form data
			setFormData({
				title: bugData.title,
				description: bugData.description,
				type: bugData.type,
				priority: bugData.priority,
				assigneeId: bugData.assignee?.id,
				labelIds: bugData.labels.map((label) => label.id),
				tags: bugData.tags || [],
			});

			// Load available labels
			const labels = await bugService.getLabels(projectSlug);
			setAvailableLabels(labels.content);
		} catch (err) {
			console.error("Failed to load data:", err);
			setError("Failed to load bug data");
			toast.error("Failed to load bug data");
		} finally {
			setLoading(false);
		}
	};

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		if (!projectSlug || !projectTicketNumber) return;

		try {
			setSaving(true);

			// Update the bug
			await bugService.updateBug(
				projectSlug,
				parseInt(projectTicketNumber),
				formData
			);

			toast.success("Bug updated successfully");
			navigate(`/projects/${projectSlug}/bugs/${projectTicketNumber}`);
		} catch (err) {
			console.error("Failed to update bug:", err);
			toast.error("Failed to update bug");
		} finally {
			setSaving(false);
		}
	};

	const handleCancel = () => {
		if (projectSlug && projectTicketNumber) {
			navigate(`/projects/${projectSlug}/bugs/${projectTicketNumber}`);
		} else {
			navigate(-1);
		}
	};

	if (loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8">
					<div className="flex items-center justify-center h-64">
						<Loader2 className="h-8 w-8 animate-spin" />
					</div>
				</main>
			</div>
		);
	}

	if (error || !bug) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8">
					<div className="text-center">
						<p className="text-red-600">{error || "Bug not found"}</p>
						<Button onClick={() => navigate(-1)} className="mt-4">
							Go Back
						</Button>
					</div>
				</main>
			</div>
		);
	}

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />

			<main className="flex-1 container mx-auto px-4 py-4">
				{/* Breadcrumb */}
				<ProjectBreadcrumb
					projectName={projectName}
					projectSlug={projectSlug || ""}
					section="Bugs"
					sectionHref={`/projects/${projectSlug}/bugs`}
					current="Edit Bug"
				/>

				{/* Header */}
				<div className="mb-8">
					<h1 className="text-3xl font-bold text-gray-900">Edit Bug</h1>
					<p className="text-gray-600 mt-1">
						Update bug information for {projectName}
					</p>
				</div>

				{/* Edit Form */}
				<Card>
					<CardHeader>
						<CardTitle>Bug Information</CardTitle>
					</CardHeader>
					<CardContent>
						<form onSubmit={handleSubmit} className="space-y-6">
							{/* Title */}
							<div className="space-y-2">
								<Label htmlFor="title">Title</Label>
								<Input
									id="title"
									value={formData.title}
									onChange={(e) =>
										setFormData({ ...formData, title: e.target.value })
									}
									placeholder="Enter bug title"
									required
								/>
							</div>

							{/* Description */}
							<div className="space-y-2">
								<Label htmlFor="description">Description</Label>
								<Textarea
									id="description"
									value={formData.description}
									onChange={(e) =>
										setFormData({ ...formData, description: e.target.value })
									}
									placeholder="Describe the bug in detail"
									rows={6}
									required
								/>
							</div>

							{/* Labels */}
							<div className="space-y-2">
								<Label>Labels</Label>
								<div className="flex flex-wrap gap-2">
									{availableLabels.map((label) => (
										<Badge
											key={label.id}
											variant={
												formData.labelIds?.includes(label.id)
													? "default"
													: "outline"
											}
											className="cursor-pointer"
											onClick={() => {
												const newLabelIds = formData.labelIds?.includes(
													label.id
												)
													? formData.labelIds.filter((id) => id !== label.id)
													: [...(formData.labelIds || []), label.id];
												setFormData({ ...formData, labelIds: newLabelIds });
											}}
										>
											{label.name}
										</Badge>
									))}
								</div>
							</div>

							{/* Tags */}
							<div className="space-y-2">
								<Label>Tags</Label>
								<div className="space-y-3">
									{/* Current Tags */}
									<div className="flex flex-wrap gap-2">
										{formData.tags?.map((tag, index) => (
											<Badge
												key={index}
												variant="secondary"
												className="cursor-pointer hover:bg-red-100"
												onClick={() => {
													const newTags =
														formData.tags?.filter((_, i) => i !== index) || [];
													setFormData({ ...formData, tags: newTags });
												}}
											>
												{tag} ×
											</Badge>
										))}
									</div>

									{/* Add New Tag */}
									<div className="flex gap-2">
										<Input
											value={newTag}
											onChange={(e) => setNewTag(e.target.value)}
											placeholder="Add a tag..."
											onKeyPress={(e) => {
												if (e.key === "Enter") {
													e.preventDefault();
													if (
														newTag.trim() &&
														!formData.tags?.includes(newTag.trim())
													) {
														setFormData({
															...formData,
															tags: [...(formData.tags || []), newTag.trim()],
														});
														setNewTag("");
													}
												}
											}}
										/>
										<Button
											type="button"
											variant="outline"
											onClick={() => {
												if (
													newTag.trim() &&
													!formData.tags?.includes(newTag.trim())
												) {
													setFormData({
														...formData,
														tags: [...(formData.tags || []), newTag.trim()],
													});
													setNewTag("");
												}
											}}
										>
											Add
										</Button>
									</div>
								</div>
							</div>

							{/* Form Actions */}
							<div className="flex items-center justify-end gap-4 pt-6 border-t">
								<Button type="button" variant="outline" onClick={handleCancel}>
									<X className="mr-2 h-4 w-4" />
									Cancel
								</Button>
								<Button type="submit" disabled={saving}>
									{saving ? (
										<Loader2 className="mr-2 h-4 w-4 animate-spin" />
									) : (
										<Save className="mr-2 h-4 w-4" />
									)}
									Save Changes
								</Button>
							</div>
						</form>
					</CardContent>
				</Card>
			</main>
		</div>
	);
}
