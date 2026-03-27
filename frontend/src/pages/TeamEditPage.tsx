/**
 * TeamEditPage Component
 *
 * Page for editing team information with form validation and proper UX.
 * Allows team admins to update team name, description, and privacy settings.
 *
 * Features:
 * - Form validation with real-time feedback
 * - Loading states during submission
 * - Proper accessibility with focus management
 * - Team privacy settings
 * - Character count for fields
 * - Error handling and display
 * - Responsive design
 * - Breadcrumb navigation
 */

import React, { useState, useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
	Users,
	Save,
	ArrowLeft,
	Loader2,
	AlertCircle,
	Globe,
	Lock,
	Check,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Footer } from "@/components/ui/footer";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import {
	Form,
	FormControl,
	FormDescription,
	FormField,
	FormItem,
	FormLabel,
	FormMessage,
} from "@/components/ui/form";
import { teamService } from "@/services/teamService";
import { RoleBadge, hasAdminPermissions } from "@/components/teams";
import { cn } from "@/lib/utils";
import { ProjectBreadcrumb } from "@/components/ui/breadcrumb";
import type { Team, UpdateTeamRequest } from "@/types/team";

// Form validation schema
const updateTeamSchema = z.object({
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

type UpdateTeamFormData = z.infer<typeof updateTeamSchema>;

export function TeamEditPage() {
	const { projectSlug, teamSlug } = useParams<{
		projectSlug: string;
		teamSlug: string;
	}>();
	const navigate = useNavigate();

	console.log("TeamEditPage -> Component mounted -> URL params:", {
		projectSlug,
		teamSlug,
	});

	// State management
	const [team, setTeam] = useState<Team | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [isSaving, setIsSaving] = useState(false);
	const [previewSlug, setPreviewSlug] = useState("");

	// Form setup
	const form = useForm<UpdateTeamFormData>({
		resolver: zodResolver(updateTeamSchema),
		defaultValues: {
			name: "",
			description: "",
		},
	});

	// Load team data
	const loadTeam = async () => {
		console.log("TeamEditPage -> loadTeam -> Starting with params:", {
			projectSlug,
			teamSlug,
		});
		if (!projectSlug || !teamSlug) {
			console.error("TeamEditPage -> loadTeam -> Missing required params:", {
				projectSlug,
				teamSlug,
			});
			setError("Missing project or team information. Please check the URL.");
			return;
		}

		try {
			setLoading(true);
			setError(null);

			console.log(
				"TeamEditPage -> loadTeam -> Calling teamService.getTeamBySlug with:",
				{ projectSlug, teamSlug }
			);
			const teamData = await teamService.getTeamBySlug(projectSlug, teamSlug);
			console.log("TeamEditPage -> loadTeam -> Received team data:", {
				id: teamData.id,
				name: teamData.name,
				teamSlug: teamData.teamSlug,
				projectSlug: teamData.projectSlug,
			});
			setTeam(teamData);

			// Check if user has admin permissions
			if (!hasAdminPermissions(teamData.currentUserRole ?? null)) {
				setError("You don't have permission to edit this team");
				return;
			}

			// Populate form with current team data
			form.reset({
				name: teamData.name,
				description: teamData.description || "",
			});

			// Set initial slug preview
			setPreviewSlug(teamData.teamSlug);
		} catch (error) {
			setError(error instanceof Error ? error.message : "Failed to load team");
		} finally {
			setLoading(false);
		}
	};

	// Initial load
	useEffect(() => {
		if (projectSlug && teamSlug) {
			loadTeam();
		}
	}, [projectSlug, teamSlug]);

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
	const handleSubmit = async (data: UpdateTeamFormData) => {
		console.log("TeamEditPage -> handleSubmit -> Starting submission with:", {
			formData: data,
			team: { id: team?.id, name: team?.name, teamSlug: team?.teamSlug },
			projectSlug,
		});

		if (!team || !team.teamSlug || !projectSlug) {
			console.error("TeamEditPage -> handleSubmit -> Invalid team data:", {
				team: !!team,
				teamTeamSlug: !!team?.teamSlug,
				projectSlug: !!projectSlug,
			});
			setError("Invalid team data. Please refresh the page and try again.");
			return;
		}

		setIsSaving(true);
		try {
			const updateData: UpdateTeamRequest = {
				name: data.name.trim(),
				description: data.description?.trim() || undefined,
			};

			console.log(
				"TeamEditPage -> handleSubmit -> Calling teamService.updateTeam with:",
				{
					projectSlug,
					teamSlug: team.teamSlug,
					updateData,
				}
			);

			const updatedTeam = await teamService.updateTeam(
				projectSlug,
				team.teamSlug,
				updateData
			);

			// Navigate to the updated team page using the updated team slug
			console.log(
				"TeamEditPage -> handleSubmit -> Navigating to updated team:",
				{
					projectSlug,
					oldTeamSlug: team.teamSlug,
					newTeamSlug: updatedTeam.teamSlug,
					teamName: updatedTeam.name,
				}
			);
			navigate(`/projects/${projectSlug}/teams/${updatedTeam.teamSlug}`);
		} catch (error) {
			console.error("Failed to update team:", error);
			setError(
				error instanceof Error ? error.message : "Failed to update team"
			);
		} finally {
			setIsSaving(false);
		}
	};

	// Handle cancel
	const handleCancel = () => {
		if (team && projectSlug) {
			navigate(`/projects/${projectSlug}/teams/${team.teamSlug}`);
		} else {
			navigate("/teams");
		}
	};

	// Loading state
	if (loading || !projectSlug || !teamSlug) {
		return (
			<div className="container mx-auto px-4 py-8 max-w-4xl">
				<div className="flex items-center gap-4 mb-8">
					<Button variant="ghost" size="sm" disabled>
						<ArrowLeft className="h-4 w-4 mr-2" />
						Back
					</Button>
				</div>
				<Card>
					<CardContent className="p-6">
						<div className="flex items-center gap-4">
							<Loader2 className="h-8 w-8 animate-spin" />
							<div className="space-y-2">
								<div className="h-6 bg-muted rounded w-48 animate-pulse" />
								<div className="h-4 bg-muted rounded w-32 animate-pulse" />
							</div>
						</div>
						{(!projectSlug || !teamSlug) && (
							<div className="mt-4 text-sm text-muted-foreground">
								Loading route parameters...
							</div>
						)}
					</CardContent>
				</Card>
			</div>
		);
	}

	// Error state
	if (error || !team || !team.teamSlug) {
		return (
			<div className="container mx-auto px-4 py-8 max-w-4xl">
				<div className="flex items-center gap-4 mb-8">
					<Button variant="ghost" size="sm" onClick={() => navigate("/teams")}>
						<ArrowLeft className="h-4 w-4 mr-2" />
						Back to Teams
					</Button>
				</div>
				<Card>
					<CardContent className="p-8 text-center">
						<AlertCircle className="h-12 w-12 mx-auto text-destructive mb-4" />
						<h3 className="text-lg font-semibold mb-2">Cannot Edit Team</h3>
						<p className="text-muted-foreground mb-4">
							{error ||
								"The team you're looking for doesn't exist or you don't have permission to edit it."}
						</p>
						<Button onClick={() => navigate("/teams")}>Back to Teams</Button>
					</CardContent>
				</Card>
			</div>
		);
	}

	return (
		<div className="container mx-auto px-4 py-4">
			{/* Breadcrumb Navigation */}
			<ProjectBreadcrumb
				projectName={team.projectSlug}
				projectSlug={projectSlug}
				section="Teams"
				sectionHref={`/projects/${projectSlug}/teams`}
				current="Edit"
				onBackClick={() =>
					navigate(`/projects/${projectSlug}/teams/${team.teamSlug}`)
				}
			/>

			{/* Page Header */}
			<div className="mb-8">
				<h1 className="text-3xl font-bold tracking-tight">Edit Team</h1>
				<p className="text-muted-foreground mt-1">
					Update your team's information and settings
				</p>
			</div>

			{/* Error Display */}
			{error && (
				<Card className="mb-6 border-destructive/20 bg-destructive/10">
					<CardContent className="p-4">
						<div className="flex items-center gap-2">
							<AlertCircle className="h-4 w-4 text-destructive" />
							<span className="text-destructive">{error}</span>
						</div>
					</CardContent>
				</Card>
			)}

			{/* Edit Form */}
			<Card>
				<CardHeader>
					<CardTitle className="flex items-center gap-2">
						<Users className="h-5 w-5" />
						Team Information
					</CardTitle>
				</CardHeader>
				<CardContent>
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
												disabled={isSaving}
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
												{(field.value || "").length}/100
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
										<span className="text-muted-foreground">Team URL:</span>
										<code className="rounded bg-muted px-2 py-1 text-xs">
											/projects/{projectSlug}/teams/{previewSlug}
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
											<textarea
												{...field}
												placeholder="Describe your team's purpose and goals..."
												disabled={isSaving}
												className={cn(
													"flex min-h-[100px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
													form.formState.errors.description &&
														"border-destructive"
												)}
											/>
										</FormControl>
										<div className="flex items-center justify-between">
											<FormDescription>
												Optional description to help others understand your team
											</FormDescription>
											<span className="text-xs text-muted-foreground">
												{(field.value || "").length}/500
											</span>
										</div>
										<FormMessage />
									</FormItem>
								)}
							/>

							{/* Current Team Info */}
							<div className="rounded-lg border bg-muted/50 p-4">
								<h4 className="font-medium mb-2">Current Team Information</h4>
								<div className="space-y-2 text-sm">
									<div className="flex items-center gap-2">
										<span className="text-muted-foreground">Current URL:</span>
										<code className="rounded bg-background px-2 py-1 text-xs">
											/teams/{team.teamSlug}
										</code>
									</div>
									<div className="flex items-center gap-2">
										<span className="text-muted-foreground">Your Role:</span>
										{team.currentUserRole && (
											<RoleBadge role={team.currentUserRole} size="sm" />
										)}
									</div>
									<div className="flex items-center gap-2">
										<span className="text-muted-foreground">Members:</span>
										<span>
											{team.memberCount} member
											{team.memberCount !== 1 ? "s" : ""}
										</span>
									</div>
								</div>
							</div>

							{/* Form Actions */}
							<div className="flex items-center gap-4 pt-6">
								<Button
									type="button"
									variant="outline"
									onClick={handleCancel}
									disabled={isSaving}
								>
									Cancel
								</Button>
								<Button
									type="submit"
									disabled={isSaving || !form.formState.isValid}
								>
									{isSaving ? (
										<>
											<Loader2 className="h-4 w-4 mr-2 animate-spin" />
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
						</form>
					</Form>
				</CardContent>
			</Card>
			<Footer />
		</div>
	);
}
