import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { BugType, BugPriority, CreateBugRequest } from "@/types/bug";
import { BugStatus } from "@/types/bug";
import { Team } from "@/types/team";
import { BugLabel } from "@/types/bug";
import { Project } from "@/types/project";
import { bugService } from "@/services/bugService";
import { teamService } from "@/services/teamService";
import { bugLabelService } from "@/services/bugLabelService";
import { projectService } from "@/services/projectService";
import { logger } from "@/utils/logger";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { ProjectPicker } from "@/components/ui/project-picker";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { DuplicateDetectionWarning } from "@/components/bugs/DuplicateDetectionWarning";
import { useDuplicateDetection } from "@/hooks/useDuplicateDetection";
import { useDebouncedSimilarityCheck } from "@/hooks/useDebouncedSimilarityCheck";
import { Info, AlertTriangle } from "lucide-react";

// Custom error type for label service errors
interface LabelServiceError extends Error {
	name: "LabelServiceError";
	statusCode: number;
}

interface CreateBugFormData {
	title: string;
	description: string;
	type: BugType;
	priority: BugPriority;
	assigneeId?: string;
	labelIds: Set<number>;
	tags: Set<string>;
}

export const CreateBugPage: React.FC = () => {
	const navigate = useNavigate();
	const { projectSlug: urlProjectSlug } = useParams<{ projectSlug: string }>();

	// Form data state
	const [formData, setFormData] = useState<CreateBugFormData>({
		title: "",
		description: "",
		type: BugType.ISSUE,
		priority: BugPriority.MEDIUM,
		labelIds: new Set(),
		tags: new Set(),
	});

	// UI state
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [isLoadingProjects, setIsLoadingProjects] = useState(true);
	const [isLoadingTeams, setIsLoadingTeams] = useState(false);
	const [isLoadingLabels, setIsLoadingLabels] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [success, setSuccess] = useState<string | null>(null);

	// Data state
	const [availableProjects, setAvailableProjects] = useState<Project[]>([]);
	const [selectedProjectSlug, setSelectedProjectSlug] = useState<string>(
		urlProjectSlug || ""
	);
	const [availableTeams, setAvailableTeams] = useState<Team[]>([]);
	const [availableLabels, setAvailableLabels] = useState<BugLabel[]>([]);
	const [newTag, setNewTag] = useState("");
	const [newLabel, setNewLabel] = useState({
		name: "",
	}); // Simplified: removed color and description
	const [isCreatingLabel, setIsCreatingLabel] = useState(false);

	// Duplicate detection hooks
	const duplicateDetection = useDuplicateDetection();
	const { debouncedCheck, immediateCheck, cancelPendingCheck } =
		useDebouncedSimilarityCheck({
			onCheck: duplicateDetection.checkSimilarity,
		});

	// Define all callback functions FIRST, before useEffect hooks
	const loadAvailableProjects = async () => {
		try {
			setIsLoadingProjects(true);
			logger.debug("CreateBugPage", "Loading available projects");

			const projects = await projectService.getUserProjects();
			// Ensure projects is always an array
			const projectsArray = Array.isArray(projects) ? projects : [];
			setAvailableProjects(projectsArray);

			// Auto-select the project from URL if available, or first project if no URL project
			if (
				urlProjectSlug &&
				projectsArray.find((p) => p.projectSlug === urlProjectSlug)
			) {
				setSelectedProjectSlug(urlProjectSlug);
				logger.debug("CreateBugPage", "Auto-selected project from URL", {
					projectSlug: urlProjectSlug,
				});
			} else if (
				projectsArray.length > 0 &&
				(!selectedProjectSlug || selectedProjectSlug === "")
			) {
				// Only auto-select first project if no project is currently selected
				setSelectedProjectSlug(projectsArray[0].projectSlug);
				logger.debug("CreateBugPage", "Auto-selected first available project", {
					projectSlug: projectsArray[0].projectSlug,
				});
			}

			logger.debug("CreateBugPage", "Projects loaded successfully", {
				projectCount: projectsArray.length,
				selectedProject: selectedProjectSlug,
			});
		} catch (error) {
			logger.error("CreateBugPage", "Failed to load projects", {
				error: error instanceof Error ? error.message : "Unknown error",
			});
			setError("Failed to load available projects");
			// Reset to empty array on error
			setAvailableProjects([]);
		} finally {
			setIsLoadingProjects(false);
		}
	};

	const loadAvailableTeams = useCallback(async () => {
		if (!selectedProjectSlug) return;

		try {
			setIsLoadingTeams(true);
			logger.debug("CreateBugPage", "Loading available teams", {
				projectSlug: selectedProjectSlug,
			});

			const teams = await teamService.getProjectTeams(selectedProjectSlug);
			// Ensure teams is always an array
			const teamsArray = Array.isArray(teams) ? teams : [];
			setAvailableTeams(teamsArray);

			logger.debug("CreateBugPage", "Teams loaded successfully", {
				projectSlug: selectedProjectSlug,
				teamCount: teamsArray.length,
			});
		} catch (error) {
			logger.error("CreateBugPage", "Failed to load teams", {
				projectSlug: selectedProjectSlug,
				error: error instanceof Error ? error.message : "Unknown error",
			});
			setError("Failed to load available teams");
			// Reset to empty array on error
			setAvailableTeams([]);
		} finally {
			setIsLoadingTeams(false);
		}
	}, [selectedProjectSlug]);

	const loadAvailableLabels = useCallback(async () => {
		if (!selectedProjectSlug) return;

		try {
			setIsLoadingLabels(true);
			logger.debug("CreateBugPage", "Loading available labels", {
				projectSlug: selectedProjectSlug,
			});

			const labels = await bugLabelService.getLabels(selectedProjectSlug);
			// Ensure labels is always an array
			const labelsArray = Array.isArray(labels) ? labels : [];
			setAvailableLabels(labelsArray);

			logger.debug("CreateBugPage", "Labels loaded successfully", {
				projectSlug: selectedProjectSlug,
				labelCount: labelsArray.length,
			});
		} catch (error: unknown) {
			// Handle specific label service errors
			if (
				error &&
				typeof error === "object" &&
				"name" in error &&
				error.name === "LabelServiceError"
			) {
				const labelError = error as LabelServiceError;
				logger.error("CreateBugPage", "Label loading failed", {
					projectSlug: selectedProjectSlug,
					error: labelError.message,
					statusCode: labelError.statusCode,
				});
				setError(`Label loading failed: ${labelError.message}`);
			} else {
				logger.error("CreateBugPage", "Failed to load labels", {
					projectSlug: selectedProjectSlug,
					error: error instanceof Error ? error.message : "Unknown error",
				});
				setError("Failed to load available labels");
			}
			// Reset to empty array on error
			setAvailableLabels([]);
		} finally {
			setIsLoadingLabels(false);
		}
	}, [selectedProjectSlug]);

	const handleProjectChange = useCallback(
		(projectSlug: string) => {
			logger.debug("CreateBugPage", "Project selection changed", {
				projectSlug,
				previousSelection: selectedProjectSlug,
			});

			// Find the selected project for additional logging
			const selectedProject = availableProjects.find(
				(p) => p.projectSlug === projectSlug
			);
			logger.debug("CreateBugPage", "Selected project details", {
				projectSlug,
				projectName: selectedProject?.name,
				projectId: selectedProject?.id,
			});

			setSelectedProjectSlug(projectSlug);

			// Reset form data when project changes
			setFormData({
				title: "",
				description: "",
				type: BugType.ISSUE,
				priority: BugPriority.MEDIUM,
				labelIds: new Set(),
				tags: new Set(),
			});
			setError(null);
			setSuccess(null);
			duplicateDetection.clearResults();
		},
		[duplicateDetection, selectedProjectSlug, availableProjects]
	);

	const handleInputChange = useCallback(
		(
			field: keyof CreateBugFormData,
			value: string | BugType | BugPriority | number | Set<number> | Set<string>
		) => {
			setFormData((prev) => ({ ...prev, [field]: value }));
		},
		[]
	);

	const handleLabelToggle = useCallback((labelId: number) => {
		setFormData((prev) => {
			const newLabelIds = new Set(prev.labelIds);
			if (newLabelIds.has(labelId)) {
				newLabelIds.delete(labelId);
			} else {
				newLabelIds.add(labelId);
			}
			return { ...prev, labelIds: newLabelIds };
		});
	}, []);

	const handleTagAdd = useCallback(() => {
		if (newTag.trim() && !formData.tags.has(newTag.trim())) {
			setFormData((prev) => {
				const newTags = new Set(prev.tags);
				newTags.add(newTag.trim());
				return { ...prev, tags: newTags };
			});
			setNewTag("");
		}
	}, [newTag, formData.tags]);

	const handleTagRemove = useCallback((tag: string) => {
		setFormData((prev) => {
			const newTags = new Set(prev.tags);
			newTags.delete(tag);
			return { ...prev, tags: newTags };
		});
	}, []);

	const handleLabelCreate = useCallback(async () => {
		if (!newLabel.name.trim() || !selectedProjectSlug) return;

		try {
			setIsCreatingLabel(true);
			logger.debug("CreateBugPage", "Creating new label", {
				newLabel,
				projectSlug: selectedProjectSlug,
			});

			const createdLabel = await bugLabelService.createLabel(
				selectedProjectSlug,
				{
					name: newLabel.name.trim(),
					color: "#3B82F6", // Default color
					description: "", // Default description
				}
			);

			// Add the new label to available labels
			setAvailableLabels((prev) => [...prev, createdLabel]);

			// Reset form
			setNewLabel({ name: "" });

			logger.debug("CreateBugPage", "Label created successfully", {
				labelId: createdLabel.id,
			});
		} catch (error: unknown) {
			// Handle specific label service errors
			if (
				error &&
				typeof error === "object" &&
				"name" in error &&
				error.name === "LabelServiceError"
			) {
				const labelError = error as LabelServiceError;
				logger.error("CreateBugPage", "Label creation service error", {
					projectSlug: selectedProjectSlug,
					error: labelError.message,
					statusCode: labelError.statusCode,
				});
				setError(`Label creation failed: ${labelError.message}`);
			} else {
				const errorMessage =
					error instanceof Error ? error.message : "Failed to create label";
				logger.error("CreateBugPage", "Label creation failed", {
					projectSlug: selectedProjectSlug,
					error: errorMessage,
				});
				setError(errorMessage);
			}
		} finally {
			setIsCreatingLabel(false);
		}
	}, [newLabel, selectedProjectSlug]);

	const handleSubmit = useCallback(
		async (e: React.FormEvent) => {
			e.preventDefault();

			if (!selectedProjectSlug) {
				setError("Please select a project first");
				return;
			}

			// Validate minimum content requirements for duplicate detection
			if (
				!duplicateDetection.hasMinimumContent(
					formData.title,
					formData.description
				)
			) {
				setError(duplicateDetection.getMinimumContentMessage());
				return;
			}

			try {
				setIsSubmitting(true);
				setError(null);
				setSuccess(null);

				// Cancel any pending similarity checks
				cancelPendingCheck();

				// Step 1: Check for similar bugs before creation
				logger.info(
					"CreateBugPage",
					"Checking for similar bugs before creation",
					{
						projectSlug: selectedProjectSlug,
						title: formData.title,
						descriptionLength: formData.description.length,
					}
				);

				try {
					await immediateCheck(
						formData.title,
						formData.description,
						selectedProjectSlug
					);
				} catch (error) {
					logger.warn(
						"CreateBugPage",
						"Similarity check failed, proceeding with bug creation",
						{
							error: error instanceof Error ? error.message : "Unknown error",
						}
					);
					// Continue with bug creation even if similarity check fails
				}

				// Step 2: Check if user wants to proceed despite similarity
				const hasHighSimilarity =
					duplicateDetection.hasBlockWarning || duplicateDetection.hasWarning;
				if (hasHighSimilarity && !duplicateDetection.proceedAnyway) {
					// Show error and stop submission
					setError(
						"Please review the similar bugs above and either mark as duplicate or proceed anyway."
					);
					setIsSubmitting(false);
					return;
				}

				logger.info("CreateBugPage", "Submitting bug creation", {
					projectSlug: selectedProjectSlug,
					formData: {
						...formData,
						labelIds: Array.from(formData.labelIds),
						tags: Array.from(formData.tags),
					},
					similarBugsFound: duplicateDetection.similarBugs.length,
					proceedingAnyway: duplicateDetection.proceedAnyway,
				});

				console.log(
					"CreateBugPage -> handleSubmit -> Bug creation request data",
					{
						title: formData.title,
						description: formData.description,
						type: formData.type,
						priority: formData.priority,
						assigneeId: formData.assigneeId,
						labelIds: Array.from(formData.labelIds),
						labelIdsType: typeof formData.labelIds,
						labelIdsIsSet: formData.labelIds instanceof Set,
						labelIdsSize: formData.labelIds.size,
						tags: Array.from(formData.tags),
						tagsType: typeof formData.tags,
						tagsIsSet: formData.tags instanceof Set,
						tagsSize: formData.tags.size,
						assignedTeamIds: [],
					}
				);

				// Step 3: Create the bug
				const createBugRequest: CreateBugRequest = {
					title: formData.title,
					description: formData.description,
					type: formData.type,
					priority: formData.priority,
					assigneeId: formData.assigneeId,
					labelIds: Array.from(formData.labelIds),
					tags: Array.from(formData.tags),
					assignedTeamIds: [], // Will be auto-assigned
				};

				console.log(
					"CreateBugPage -> handleSubmit -> Final createBugRequest object",
					{
						createBugRequest,
						labelIdsInRequest: createBugRequest.labelIds,
						labelIdsLength: createBugRequest.labelIds.length,
						tagsInRequest: createBugRequest.tags,
						tagsLength: createBugRequest.tags.length,
					}
				);

				const createdBug = await bugService.createBug(
					selectedProjectSlug,
					createBugRequest
				);

				console.log(
					"CreateBugPage -> handleSubmit -> Bug created successfully",
					{
						createdBug,
						createdBugId: createdBug.id,
						createdBugLabels: createdBug.labels,
						createdBugLabelsCount: createdBug.labels?.length || 0,
						createdBugTags: createdBug.tags,
						createdBugTagsCount: createdBug.tags?.length || 0,
					}
				);

				logger.info("CreateBugPage", "Bug created successfully", {
					projectSlug: selectedProjectSlug,
					bugId: createdBug.id,
				});

				// Show success message
				setSuccess(
					`Bug "${createdBug.title}" created successfully! Teams will be auto-assigned.`
				);

				// Navigate to the created bug details page
				navigate(
					`/projects/${selectedProjectSlug}/bugs/${createdBug.projectTicketNumber}`
				);
			} catch (error) {
				const errorMessage =
					error instanceof Error ? error.message : "Failed to create bug";
				logger.error("CreateBugPage", "Bug creation failed", {
					projectSlug: selectedProjectSlug,
					error: errorMessage,
				});
				setError(errorMessage);
			} finally {
				setIsSubmitting(false);
			}
		},
		[
			formData,
			selectedProjectSlug,
			navigate,
			cancelPendingCheck,
			immediateCheck,
			duplicateDetection,
		]
	);

	const handleCancel = useCallback(() => {
		if (selectedProjectSlug) {
			navigate(`/projects/${selectedProjectSlug}/bugs`);
		} else {
			navigate("/projects");
		}
	}, [selectedProjectSlug, navigate]);

	const resetForm = useCallback(() => {
		setFormData({
			title: "",
			description: "",
			type: BugType.ISSUE,
			priority: BugPriority.MEDIUM,
			labelIds: new Set(),
			tags: new Set(),
		});
		setError(null);
		duplicateDetection.clearResults();
	}, [duplicateDetection]);

	// ===== useEffect hooks AFTER all callback functions =====

	// Load available projects on component mount
	useEffect(() => {
		loadAvailableProjects();
	}, []); // Empty dependency array - only run on mount

	// Load teams and labels when project selection changes
	useEffect(() => {
		if (selectedProjectSlug) {
			loadAvailableTeams();
			loadAvailableLabels();
			// Clear duplicate detection results when project changes
			duplicateDetection.clearResults();
		} else {
			setAvailableTeams([]);
			setAvailableLabels([]);
		}
	}, [selectedProjectSlug, loadAvailableTeams, loadAvailableLabels]);

	// Cleanup on unmount
	useEffect(() => {
		return () => {
			// Cancel any pending similarity checks
			cancelPendingCheck();
			// Clear duplicate detection results
			duplicateDetection.clearResults();
		};
	}, [cancelPendingCheck]);

	if (isLoadingProjects) {
		return (
			<div className="min-h-screen bg-gray-50 flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-4xl">
					<div className="flex items-center justify-center h-64">
						<LoadingSpinner size="lg" />
						<span className="ml-3 text-gray-600">Loading projects...</span>
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	if (!Array.isArray(availableProjects) || availableProjects.length === 0) {
		return (
			<div className="min-h-screen bg-gray-50 flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8 max-w-4xl">
					<Alert variant="destructive">
						<AlertDescription>
							You don't have access to any projects. Please join a project first
							or contact an administrator.
						</AlertDescription>
					</Alert>
				</main>
				<Footer />
			</div>
		);
	}

	return (
		<div className="min-h-screen bg-gray-50 flex flex-col">
			<Navbar />

			<main className="flex-1 container mx-auto px-4 py-8 max-w-4xl">
				<div className="mb-6">
					<h1 className="text-3xl font-bold text-gray-900 mb-2">
						Create New Bug
					</h1>
					<p className="text-gray-600">
						Report a new bug or issue for a project
					</p>
				</div>

				{error && (
					<Alert variant="destructive" className="mb-4">
						<AlertDescription>{error}</AlertDescription>
					</Alert>
				)}

				{success && (
					<Alert className="mb-4">
						<AlertDescription>{success}</AlertDescription>
					</Alert>
				)}

				<Card className="p-6">
					<form onSubmit={handleSubmit} className="space-y-6">
						{/* Project Selection */}
						<div>
							<label
								htmlFor="project"
								className="block text-sm font-medium text-gray-700 mb-2"
							>
								Project *
							</label>
							<ProjectPicker
								projects={availableProjects}
								selectedProjectSlug={selectedProjectSlug}
								onProjectSelect={handleProjectChange}
								placeholder="Select a project"
								disabled={isLoadingProjects}
							/>
						</div>

						{/* Title */}
						<div>
							<label
								htmlFor="title"
								className="block text-sm font-medium text-gray-700 mb-2"
							>
								Title *
							</label>
							<Input
								id="title"
								type="text"
								value={formData.title}
								onChange={(e) => handleInputChange("title", e.target.value)}
								onBlur={() => {
									// Trigger similarity check on blur with debounce
									if (
										selectedProjectSlug &&
										formData.title.trim() &&
										formData.description.trim()
									) {
										debouncedCheck(
											formData.title,
											formData.description,
											selectedProjectSlug
										);
									}
								}}
								placeholder="Title of the bug"
								required
								maxLength={255}
							/>
							{/* Validation message for duplicate detection */}
							{formData.title.trim().length === 0 && (
								<div className="mt-2 text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-md p-2">
									<Info className="inline h-4 w-4 mr-1" />
									Title is required for duplicate detection to work.
								</div>
							)}
						</div>

						{/* Description */}
						<div>
							<label
								htmlFor="description"
								className="block text-sm font-medium text-gray-700 mb-2"
							>
								Description *
							</label>
							<Textarea
								id="description"
								value={formData.description}
								onChange={(e) =>
									handleInputChange("description", e.target.value)
								}
								onBlur={() => {
									// Trigger similarity check on blur with debounce
									if (
										selectedProjectSlug &&
										formData.title.trim() &&
										formData.description.trim()
									) {
										debouncedCheck(
											formData.title,
											formData.description,
											selectedProjectSlug
										);
									}
								}}
								placeholder="Detailed description of the bug, steps to reproduce, expected vs actual behavior"
								required
								rows={6}
							/>
							{/* Validation message for duplicate detection */}
							{formData.description.trim().length > 0 &&
								formData.description.trim().length < 10 && (
									<div className="mt-2 text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-md p-2">
										<Info className="inline h-4 w-4 mr-1" />
										Description must be at least 10 characters for duplicate
										detection to work.
									</div>
								)}
						</div>

						{/* Duplicate Detection Warning */}
						{duplicateDetection.similarBugs.length > 0 && (
							<DuplicateDetectionWarning
								similarBugs={duplicateDetection.similarBugs}
								isChecking={duplicateDetection.isChecking}
								projectSlug={selectedProjectSlug}
								onViewBug={(bugId) => {
									// Navigate to the similar bug
									if (selectedProjectSlug) {
										navigate(`/projects/${selectedProjectSlug}/bugs/${bugId}`);
									}
								}}
								onMarkAsDuplicate={async (bugId) => {
									// Find the original bug data from similar bugs
									const originalBug = duplicateDetection.similarBugs.find(
										(bug) => bug.bugId === bugId
									);

									if (!originalBug || !selectedProjectSlug) {
										setError("Failed to get original bug data");
										return;
									}

									try {
										setIsSubmitting(true);
										setError(null);

										// Get the full bug details to access type, labels, tags
										let fullBugDetails: any = null;
										if (originalBug.projectTicketNumber) {
											try {
												// Get project ID first
												const project = await projectService.getProjectBySlug(
													selectedProjectSlug
												);
												fullBugDetails =
													await bugService.getBugByProjectTicketNumber(
														project.id,
														originalBug.projectTicketNumber
													);
											} catch (error) {
												logger.warn(
													"CreateBugPage",
													"Failed to get full bug details, using fallback",
													{ error }
												);
											}
										}

										// Create bug with smart metadata copying
										const createBugRequest: CreateBugRequest = {
											title:
												formData.title || `Duplicate: ${originalBug.title}`,
											description:
												formData.description ||
												`Duplicate of bug #${originalBug.projectTicketNumber}`,
											type: fullBugDetails?.type || BugType.ISSUE, // Copy from original or fallback
											priority:
												fullBugDetails?.priority ||
												(originalBug.priority as BugPriority), // Copy from original or fallback
											assigneeId: formData.assigneeId, // Keep user's choice if set
											labelIds: fullBugDetails?.labelIds || [], // Copy from original
											tags: fullBugDetails?.tags || [], // Copy from original
											assignedTeamIds: [], // Will be auto-assigned
										};

										// Step 1: Create the bug
										const createdBug = await bugService.createBug(
											selectedProjectSlug,
											createBugRequest
										);

										logger.info(
											"CreateBugPage",
											"Duplicate bug created successfully",
											{
												projectSlug: selectedProjectSlug,
												bugId: createdBug.id,
												originalBugId: bugId,
											}
										);

										// Note: Bug status will be automatically set to CLOSED when marked as duplicate

										// Step 2: Mark as duplicate
										try {
											await bugService.markAsDuplicate(
												selectedProjectSlug,
												createdBug.id,
												{
													originalBugId: bugId,
													confidenceScore: 1.0,
													additionalContext:
														"Marked as duplicate during creation",
													isAutomaticDetection: false,
												}
											);

											logger.info(
												"CreateBugPage",
												"Bug marked as duplicate successfully",
												{
													projectSlug: selectedProjectSlug,
													bugId: createdBug.id,
													duplicateOfBugId: bugId,
												}
											);

											setSuccess(
												`Duplicate bug "${createdBug.title}" created and linked to bug #${originalBug.projectTicketNumber}!`
											);
										} catch (duplicateError) {
											logger.warn(
												"CreateBugPage",
												"Failed to mark bug as duplicate",
												{
													projectSlug: selectedProjectSlug,
													bugId: createdBug.id,
													duplicateOfBugId: bugId,
													error:
														duplicateError instanceof Error
															? duplicateError.message
															: "Unknown error",
												}
											);

											// Still show success for bug creation, but warn about duplicate marking
											setSuccess(
												`Bug "${createdBug.title}" created successfully! Note: Failed to mark as duplicate.`
											);
										}

										// Navigate to the created bug details page
										navigate(
											`/projects/${selectedProjectSlug}/bugs/${createdBug.projectTicketNumber}`
										);
									} catch (error) {
										const errorMessage =
											error instanceof Error
												? error.message
												: "Failed to create duplicate bug";
										logger.error(
											"CreateBugPage",
											"Duplicate bug creation failed",
											{
												projectSlug: selectedProjectSlug,
												error: errorMessage,
											}
										);
										setError(errorMessage);
									} finally {
										setIsSubmitting(false);
									}
								}}
								onProceedAnyway={() => {
									duplicateDetection.setProceedAnyway(true);
								}}
								onDismiss={() => {
									duplicateDetection.clearResults();
								}}
							/>
						)}

						{/* Duplicate Detection Error */}
						{duplicateDetection.error && (
							<div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-md">
								<div className="flex items-center">
									<AlertTriangle className="h-5 w-5 text-red-400 mr-2" />
									<div>
										<h4 className="text-sm font-medium text-red-800">
											Duplicate Detection Warning
										</h4>
										<p className="text-sm text-red-700 mt-1">
											{duplicateDetection.error}
										</p>
									</div>
								</div>
							</div>
						)}

						{/* Type and Priority */}
						<div className="grid grid-cols-1 md:grid-cols-2 gap-4">
							<div>
								<label
									htmlFor="type"
									className="block text-sm font-medium text-gray-700 mb-2"
								>
									Type *
								</label>
								<select
									id="type"
									value={formData.type}
									onChange={(e) =>
										handleInputChange("type", e.target.value as BugType)
									}
									required
									className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
								>
									{Object.values(BugType).map((type) => (
										<option key={type} value={type}>
											{type === BugType.ISSUE
												? "Issue"
												: type === BugType.TASK
												? "Task"
												: "Specification"}
										</option>
									))}
								</select>
							</div>

							<div>
								<label
									htmlFor="priority"
									className="block text-sm font-medium text-gray-700 mb-2"
								>
									Priority *
								</label>
								<select
									id="priority"
									value={formData.priority}
									onChange={(e) =>
										handleInputChange("priority", e.target.value as BugPriority)
									}
									required
									className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
								>
									{Object.values(BugPriority).map((priority) => (
										<option key={priority} value={priority}>
											{priority === BugPriority.CRASH
												? "Crash"
												: priority === BugPriority.CRITICAL
												? "Critical"
												: priority === BugPriority.HIGH
												? "High"
												: priority === BugPriority.MEDIUM
												? "Medium"
												: "Low"}
										</option>
									))}
								</select>
							</div>
						</div>

						{/* Labels */}
						<div>
							<label className="block text-sm font-medium text-gray-700 mb-2">
								Labels
							</label>

							{/* Label Creation Form */}
							<div className="mb-4 p-4 border border-gray-200 rounded-lg bg-gray-50">
								<h4 className="text-sm font-medium text-gray-700 mb-3">
									Create New Label
								</h4>
								<div className="flex gap-3 mb-3">
									<Input
										type="text"
										value={newLabel.name}
										onChange={(e) =>
											setNewLabel((prev) => ({ ...prev, name: e.target.value }))
										}
										placeholder="Label name"
										maxLength={50}
										className="flex-1"
									/>
									<Button
										onClick={handleLabelCreate}
										disabled={!newLabel.name.trim() || isCreatingLabel}
										className="px-6"
									>
										{isCreatingLabel ? (
											<>
												<LoadingSpinner size="sm" className="mr-2" />
												Creating...
											</>
										) : (
											"Create Label"
										)}
									</Button>
								</div>
							</div>

							{/* Existing Labels */}
							{isLoadingLabels ? (
								<LoadingSpinner size="sm" />
							) : (
								<div className="flex flex-wrap gap-2">
									{Array.isArray(availableLabels) &&
										availableLabels.map((label) => (
											<Badge
												key={label.id}
												variant={
													formData.labelIds.has(label.id)
														? "default"
														: "outline"
												}
												className="cursor-pointer"
												onClick={() => handleLabelToggle(label.id)}
												style={{
													backgroundColor: formData.labelIds.has(label.id)
														? label.color
														: undefined,
												}}
											>
												{label.name}
											</Badge>
										))}
									{(!Array.isArray(availableLabels) ||
										availableLabels.length === 0) && (
										<span className="text-gray-500 text-sm">
											No labels found for this project. Create one above!
										</span>
									)}
								</div>
							)}
						</div>

						{/* Tags */}
						<div>
							<label className="block text-sm font-medium text-gray-700 mb-2">
								Tags
							</label>
							<div className="flex gap-2 mb-2">
								<Input
									type="text"
									value={newTag}
									onChange={(e) => setNewTag(e.target.value)}
									placeholder="Add a tag"
									onKeyPress={(e) =>
										e.key === "Enter" && (e.preventDefault(), handleTagAdd())
									}
								/>
								<Button
									type="button"
									onClick={handleTagAdd}
									disabled={!newTag.trim()}
								>
									Add
								</Button>
							</div>
							<div className="flex flex-wrap gap-2">
								{Array.from(formData.tags).map((tag) => (
									<Badge
										key={tag}
										variant="secondary"
										className="cursor-pointer"
										onClick={() => handleTagRemove(tag)}
									>
										{tag} ×
									</Badge>
								))}
							</div>
						</div>

						{/* Teams Info */}
						<div className="bg-blue-50 p-4 rounded-lg">
							<h3 className="text-sm font-medium text-blue-900 mb-2">
								Team Assignment
							</h3>
							{isLoadingTeams ? (
								<LoadingSpinner size="sm" />
							) : (
								<p className="text-sm text-blue-700">
									Teams will be automatically assigned based on the bug's labels
									and priority after creation.
									{Array.isArray(availableTeams) &&
										availableTeams.length > 0 &&
										` ${availableTeams.length} teams available.`}
								</p>
							)}
						</div>

						{/* Form Actions */}
						<div className="flex justify-end gap-3 pt-6 border-t">
							<Button type="button" variant="outline" onClick={handleCancel}>
								Cancel
							</Button>
							<Button
								type="submit"
								disabled={isSubmitting || !selectedProjectSlug}
							>
								{isSubmitting ? (
									<>
										<LoadingSpinner size="sm" className="mr-2" />
										Creating Bug...
									</>
								) : (
									"Create Bug"
								)}
							</Button>
						</div>
					</form>
				</Card>
			</main>

			<Footer />
		</div>
	);
};
