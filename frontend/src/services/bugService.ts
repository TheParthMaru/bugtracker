/**
 * BugService
 *
 * Service layer for bug management operations.
 * Handles all bug-related API calls with proper error handling,
 * type safety, and response validation.
 *
 * Features:
 * - Complete CRUD operations for bugs
 * - Bug filtering and search
 * - Status and assignment management
 * - Label management
 * - Comment management
 * - Attachment management
 * - Analytics and statistics
 * - Error handling and response validation
 * - Request/response logging
 * - Type-safe API calls
 */

import API from "./api";
import { handleApiError } from "../lib/errorHandler";
import type {
	Bug,
	BugAttachment,
	BugComment,
	BugLabel,
	BugPriority,
	BugStatus,
	BugType,
	CloseReason,
	CreateBugRequest,
	UpdateBugRequest,
	BugSearchParams,
	BugsListResponse,
	BugApiError,
	BugValidationError,
	BugLoadingState,
	TeamAssignmentInfo,
} from "@/types/bug";
import type {
	SimilarityCheckRequest,
	BugSimilarityResult,
	BugSimilarityRelationship,
	MarkDuplicateRequest,
	SimilarityStatistics,
	SimilarityHealth,
	SimilarityConfig,
	ConfigurationUpdateRequest,
	ConfigurationValidation,
	SimilarityAlgorithm,
	DuplicateInfoResponse,
	BugDuplicateSummary,
	BugSummary,
	DuplicateAnalyticsResponse,
} from "@/types/similarity";
import { DuplicateAnalyticsResponseImpl } from "@/types/similarity";

export class BugService {
	private static instance: BugService;

	// Singleton pattern for consistent service usage
	public static getInstance(): BugService {
		if (!BugService.instance) {
			BugService.instance = new BugService();
		}
		return BugService.instance;
	}

	/**
	 * Bug CRUD Operations
	 */

	/**
	 * Create a new bug
	 */
	async createBug(projectSlug: string, data: CreateBugRequest): Promise<Bug> {
		try {
			console.log("BugService -> createBug -> Creating bug", {
				projectSlug,
				requestData: data,
				labelIdsInRequest: data.labelIds,
				labelIdsType: typeof data.labelIds,
				labelIdsIsArray: Array.isArray(data.labelIds),
				labelIdsLength: data.labelIds?.length || 0,
				tagsInRequest: data.tags,
				tagsType: typeof data.tags,
				tagsIsArray: Array.isArray(data.tags),
				tagsLength: data.tags?.length || 0,
			});

			const response = await API.post<Bug>(
				`/projects/${projectSlug}/bugs`,
				data
			);

			console.log("BugService -> createBug -> Bug creation response received", {
				responseStatus: response.status,
				responseData: response.data,
				createdBugLabels: response.data?.labels,
				createdBugLabelsCount: response.data?.labels?.length || 0,
				createdBugTags: response.data?.tags,
				createdBugTagsCount: response.data?.tags?.length || 0,
			});

			return response.data;
		} catch (error) {
			console.error("BugService -> createBug -> Bug creation failed", {
				projectSlug,
				requestData: data,
				error,
			});
			throw this.handleError(error, "Failed to create bug");
		}
	}

	/**
	 * Get bugs with pagination and filtering
	 */
	async getBugs(
		projectId: string,
		params?: BugSearchParams
	): Promise<BugsListResponse> {
		try {
			const queryParams = new URLSearchParams();

			if (params?.search) queryParams.append("searchTerm", params.search);
			if (params?.status) queryParams.append("status", params.status);
			if (params?.priority) queryParams.append("priority", params.priority);
			if (params?.type) queryParams.append("type", params.type);
			if (params?.assignee) queryParams.append("assignee", params.assignee);
			if (params?.reporter) queryParams.append("reporter", params.reporter);
			if (params?.labels) {
				params.labels.forEach((label) => queryParams.append("labels", label));
			}
			if (params?.page !== undefined)
				queryParams.append("page", params.page.toString());
			if (params?.size) queryParams.append("size", params.size.toString());
			if (params?.sort) queryParams.append("sort", params.sort);

			const url = `/projects/${projectId}/bugs${
				queryParams.toString() ? `?${queryParams.toString()}` : ""
			}`;

			const response = await API.get<BugsListResponse>(url);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch bugs");
		}
	}

	/**
	 * Get all bugs across all projects for the current user
	 */
	async getAllBugs(params?: BugSearchParams): Promise<BugsListResponse> {
		try {
			const response = await API.get<Bug[]>("/bugs/all");

			// Convert the list to a paginated response format
			const bugs = response.data;

			// Apply client-side filtering if needed
			let filteredBugs = bugs;

			if (params?.search) {
				const searchTerm = params.search.toLowerCase();
				filteredBugs = bugs.filter(
					(bug) =>
						bug.title.toLowerCase().includes(searchTerm) ||
						bug.description.toLowerCase().includes(searchTerm)
				);
			}

			if (params?.status && params.status !== "ALL") {
				filteredBugs = filteredBugs.filter(
					(bug) => bug.status === params.status
				);
			}

			if (params?.priority && params.priority !== "ALL") {
				filteredBugs = filteredBugs.filter(
					(bug) => bug.priority === params.priority
				);
			}

			if (params?.type && params.type !== "ALL") {
				filteredBugs = filteredBugs.filter((bug) => bug.type === params.type);
			}

			// Apply pagination
			const page = params?.page || 0;
			const size = params?.size || 20;
			const startIndex = page * size;
			const endIndex = startIndex + size;
			const paginatedBugs = filteredBugs.slice(startIndex, endIndex);

			return {
				content: paginatedBugs,
				pageable: {
					pageNumber: page,
					pageSize: size,
					sort: { sorted: false, unsorted: true, empty: true },
					offset: startIndex,
					paged: true,
					unpaged: false,
				},
				totalElements: filteredBugs.length,
				totalPages: Math.ceil(filteredBugs.length / size),
				last: endIndex >= filteredBugs.length,
				size: size,
				number: page,
				sort: { sorted: false, unsorted: true, empty: true },
				first: page === 0,
				numberOfElements: paginatedBugs.length,
				empty: paginatedBugs.length === 0,
			};
		} catch (error) {
			throw this.handleError(error, "Failed to fetch all bugs");
		}
	}

	/**
	 * Get bug by project ticket number
	 */
	async getBugByProjectTicketNumber(
		projectId: string,
		projectTicketNumber: number
	): Promise<Bug> {
		try {
			console.log(
				"BugService -> getBugByProjectTicketNumber -> Making API call",
				{
					projectId,
					projectTicketNumber,
					url: `/projects/${projectId}/bugs/${projectTicketNumber}`,
				}
			);

			const response = await API.get<Bug>(
				`/projects/${projectId}/bugs/${projectTicketNumber}`
			);

			console.log(
				"BugService -> getBugByProjectTicketNumber -> Raw API response received",
				{
					responseStatus: response.status,
					responseData: response.data,
					responseDataType: typeof response.data,
					labelsInResponse: response.data?.labels,
					labelsType: typeof response.data?.labels,
					labelsIsArray: Array.isArray(response.data?.labels),
					labelsLength: response.data?.labels?.length || 0,
					tagsInResponse: response.data?.tags,
					tagsType: typeof response.data?.tags,
					tagsIsArray: Array.isArray(response.data?.tags),
					tagsLength: response.data?.tags?.length || 0,
				}
			);

			return response.data;
		} catch (error) {
			console.error(
				"BugService -> getBugByProjectTicketNumber -> API call failed",
				{
					projectId,
					projectTicketNumber,
					error,
				}
			);
			throw this.handleError(error, "Failed to fetch bug");
		}
	}

	/**
	 * Update bug
	 */
	async updateBug(
		projectId: string,
		projectTicketNumber: number,
		data: UpdateBugRequest
	): Promise<Bug> {
		try {
			const response = await API.put<Bug>(
				`/projects/${projectId}/bugs/${projectTicketNumber}`,
				data
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to update bug");
		}
	}

	/**
	 * Delete bug
	 */
	async deleteBug(
		projectId: string,
		projectTicketNumber: number
	): Promise<void> {
		try {
			await API.delete(`/projects/${projectId}/bugs/${projectTicketNumber}`);
		} catch (error) {
			throw this.handleError(error, "Failed to delete bug");
		}
	}

	/**
	 * Update bug status
	 */
	async updateBugStatus(
		projectId: string,
		projectTicketNumber: number,
		status: BugStatus
	): Promise<Bug> {
		try {
			const response = await API.patch<Bug>(
				`/projects/${projectId}/bugs/${projectTicketNumber}/status`,
				{ status }
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to update bug status");
		}
	}

	/**
	 * Assign bug to user
	 */
	async assignBug(
		projectId: string,
		projectTicketNumber: number,
		assigneeId: string
	): Promise<Bug> {
		try {
			const response = await API.patch<Bug>(
				`/projects/${projectId}/bugs/${projectTicketNumber}/assign?assigneeId=${assigneeId}`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to assign bug");
		}
	}

	/**
	 * Unassign bug
	 */
	async unassignBug(
		projectId: string,
		projectTicketNumber: number
	): Promise<Bug> {
		try {
			const response = await API.patch<Bug>(
				`/projects/${projectId}/bugs/${projectTicketNumber}/unassign`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to unassign bug");
		}
	}

	/**
	 * Search bugs
	 */
	async searchBugs(
		projectId: string,
		searchTerm: string,
		params?: Omit<BugSearchParams, "search">
	): Promise<BugsListResponse> {
		try {
			const searchParams = { ...params, search: searchTerm };
			return this.getBugs(projectId, searchParams);
		} catch (error) {
			throw this.handleError(error, "Failed to search bugs");
		}
	}

	/**
	 * Get bugs by status
	 */
	async getBugsByStatus(
		projectId: string,
		status: string,
		params?: Omit<BugSearchParams, "status">
	): Promise<BugsListResponse> {
		try {
			const searchParams = { ...params, status: status as any };
			return this.getBugs(projectId, searchParams);
		} catch (error) {
			throw this.handleError(error, "Failed to fetch bugs by status");
		}
	}

	/**
	 * Get bugs by priority
	 */
	async getBugsByPriority(
		projectId: string,
		priority: string,
		params?: Omit<BugSearchParams, "priority">
	): Promise<BugsListResponse> {
		try {
			const searchParams = { ...params, priority: priority as any };
			return this.getBugs(projectId, searchParams);
		} catch (error) {
			throw this.handleError(error, "Failed to fetch bugs by priority");
		}
	}

	/**
	 * Get bugs by assignee
	 */
	async getBugsByAssignee(
		projectId: string,
		assigneeId: string,
		params?: Omit<BugSearchParams, "assignee">
	): Promise<BugsListResponse> {
		try {
			const searchParams = { ...params, assignee: assigneeId };
			return this.getBugs(projectId, searchParams);
		} catch (error) {
			throw this.handleError(error, "Failed to fetch bugs by assignee");
		}
	}

	/**
	 * Get bugs by reporter
	 */
	async getBugsByReporter(
		projectId: string,
		reporterId: string,
		params?: Omit<BugSearchParams, "reporter">
	): Promise<BugsListResponse> {
		try {
			const searchParams = { ...params, reporter: reporterId };
			return this.getBugs(projectId, searchParams);
		} catch (error) {
			throw this.handleError(error, "Failed to fetch bugs by reporter");
		}
	}

	/**
	 * Get all bugs assigned to current user across all projects
	 */
	async getMyAssignedBugs(): Promise<Bug[]> {
		try {
			const response = await API.get<Bug[]>("/bugs/my-assigned");
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch my assigned bugs");
		}
	}

	/**
	 * Get bug statistics
	 */
	async getBugStatistics(projectId: string): Promise<any> {
		// Assuming BugStatistics type is not directly imported, using 'any' for now
		try {
			const response = await API.get<any>( // Changed from BugStatistics to any
				`/projects/${projectId}/bugs/statistics`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch bug statistics");
		}
	}

	/**
	 * Get bugs requiring attention
	 */
	async getBugsRequiringAttention(projectId: string): Promise<Bug[]> {
		try {
			const response = await API.get<Bug[]>(
				`/projects/${projectId}/bugs/attention-required`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch bugs requiring attention");
		}
	}

	/**
	 * Get high priority unassigned bugs
	 */
	async getHighPriorityUnassignedBugs(projectId: string): Promise<Bug[]> {
		try {
			const response = await API.get<Bug[]>(
				`/projects/${projectId}/bugs/high-priority-unassigned`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(
				error,
				"Failed to fetch high priority unassigned bugs"
			);
		}
	}

	/**
	 * Get recent bugs
	 */
	async getRecentBugs(projectId: string): Promise<Bug[]> {
		try {
			const response = await API.get<Bug[]>(
				`/projects/${projectId}/bugs/recent`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch recent bugs");
		}
	}

	/**
	 * Label Management
	 */

	/**
	 * Get labels
	 */
	async getLabels(
		projectId: string,
		params?: any // Assuming BugLabelSearchParams type is not directly imported, using 'any' for now
	): Promise<any> {
		// Changed from BugLabelsListResponse to any
		try {
			const queryParams = new URLSearchParams();

			if (params?.search) queryParams.append("searchTerm", params.search);
			if (params?.isSystem !== undefined)
				queryParams.append("isSystem", params.isSystem.toString());
			if (params?.page !== undefined)
				queryParams.append("page", params.page.toString());
			if (params?.size) queryParams.append("size", params.size.toString());

			const url = `/projects/${projectId}/bug-labels${
				queryParams.toString() ? `?${queryParams.toString()}` : ""
			}`;
			const response = await API.get<any>(url); // Changed from BugLabelsListResponse to any
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch labels");
		}
	}

	/**
	 * Create label
	 */
	async createLabel(
		projectId: string,
		data: any // Assuming CreateLabelRequest type is not directly imported, using 'any' for now
	): Promise<any> {
		// Changed from BugLabel to any
		try {
			const response = await API.post<any>( // Changed from BugLabel to any
				`/projects/${projectId}/bug-labels`,
				data
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to create label");
		}
	}

	/**
	 * Update label
	 */
	async updateLabel(
		projectId: string,
		labelId: number,
		data: any // Assuming UpdateLabelRequest type is not directly imported, using 'any' for now
	): Promise<any> {
		// Changed from BugLabel to any
		try {
			const response = await API.put<any>( // Changed from BugLabel to any
				`/projects/${projectId}/bug-labels/${labelId}`,
				data
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to update label");
		}
	}

	/**
	 * Delete label
	 */
	async deleteLabel(projectId: string, labelId: number): Promise<void> {
		try {
			await API.delete(`/projects/${projectId}/bug-labels/${labelId}`);
		} catch (error) {
			throw this.handleError(error, "Failed to delete label");
		}
	}

	/**
	 * Comment Management
	 */

	/**
	 * Get comments
	 */
	async getComments(
		projectId: string,
		projectTicketNumber: number,
		params?: any // Assuming BugCommentSearchParams type is not directly imported, using 'any' for now
	): Promise<any> {
		// Changed from BugCommentsListResponse to any
		try {
			const queryParams = new URLSearchParams();

			if (params?.page !== undefined)
				queryParams.append("page", params.page.toString());
			if (params?.size) queryParams.append("size", params.size.toString());
			if (params?.sort) queryParams.append("sort", params.sort);

			const url = `/projects/${projectId}/bugs/${projectTicketNumber}/comments${
				queryParams.toString() ? `?${queryParams.toString()}` : ""
			}`;
			const response = await API.get<any>(url); // Changed from BugCommentsListResponse to any
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch comments");
		}
	}

	/**
	 * Create comment
	 */
	async createComment(
		projectId: string,
		projectTicketNumber: number,
		data: any // Assuming CreateCommentRequest type is not directly imported, using 'any' for now
	): Promise<any> {
		// Changed from BugComment to any
		try {
			const response = await API.post<any>( // Changed from BugComment to any
				`/projects/${projectId}/bugs/${projectTicketNumber}/comments`,
				data
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to create comment");
		}
	}

	/**
	 * Update comment
	 */
	async updateComment(
		projectId: string,
		projectTicketNumber: number,
		commentId: number,
		data: any // Assuming UpdateCommentRequest type is not directly imported, using 'any' for now
	): Promise<any> {
		// Changed from BugComment to any
		try {
			const response = await API.put<any>( // Changed from BugComment to any
				`/projects/${projectId}/bugs/${projectTicketNumber}/comments/${commentId}`,
				data
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to update comment");
		}
	}

	/**
	 * Delete comment
	 */
	async deleteComment(
		projectId: string,
		projectTicketNumber: number,
		commentId: number
	): Promise<void> {
		try {
			await API.delete(
				`/projects/${projectId}/bugs/${projectTicketNumber}/comments/${commentId}`
			);
		} catch (error) {
			throw this.handleError(error, "Failed to delete comment");
		}
	}

	/**
	 * Attachment Management
	 */

	/**
	 * Get attachments
	 */
	async getAttachments(
		projectId: string,
		projectTicketNumber: number,
		params?: any // Assuming BugAttachmentSearchParams type is not directly imported, using 'any' for now
	): Promise<any> {
		// Changed from BugAttachmentsListResponse to any
		try {
			const queryParams = new URLSearchParams();

			if (params?.page !== undefined)
				queryParams.append("page", params.page.toString());
			if (params?.size) queryParams.append("size", params.size.toString());

			const url = `/projects/${projectId}/bugs/${projectTicketNumber}/attachments${
				queryParams.toString() ? `?${queryParams.toString()}` : ""
			}`;
			const response = await API.get<any>(url); // Changed from BugAttachmentsListResponse to any
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch attachments");
		}
	}

	/**
	 * Upload attachment
	 */
	async uploadAttachment(
		projectId: string,
		projectTicketNumber: number,
		file: File
	): Promise<any> {
		// Changed from BugAttachment to any
		try {
			const formData = new FormData();
			formData.append("file", file);

			const response = await API.post<any>( // Changed from BugAttachment to any
				`/projects/${projectId}/bugs/${projectTicketNumber}/attachments`,
				formData,
				{
					headers: {
						"Content-Type": "multipart/form-data",
					},
				}
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to upload attachment");
		}
	}

	/**
	 * Download attachment
	 */
	async downloadAttachment(
		projectId: string,
		projectTicketNumber: number,
		attachmentId: number
	): Promise<Blob> {
		try {
			const response = await API.get(
				`/projects/${projectId}/bugs/${projectTicketNumber}/attachments/${attachmentId}`,
				{
					responseType: "blob",
				}
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to download attachment");
		}
	}

	/**
	 * Delete attachment
	 */
	async deleteAttachment(
		projectId: string,
		projectTicketNumber: number,
		attachmentId: number
	): Promise<void> {
		try {
			await API.delete(
				`/projects/${projectId}/bugs/${projectTicketNumber}/attachments/${attachmentId}`
			);
		} catch (error) {
			throw this.handleError(error, "Failed to delete attachment");
		}
	}

	/**
	 * Analytics
	 */

	/**
	 * Get bug analytics
	 */
	async getBugAnalytics(projectId: string): Promise<any> {
		// Assuming BugAnalytics type is not directly imported, using 'any' for now
		try {
			const response = await API.get<any>( // Changed from BugAnalytics to any
				`/projects/${projectId}/bugs/analytics`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch bug analytics");
		}
	}

	/**
	 * Get team bug analytics
	 */
	async getTeamBugAnalytics(projectId: string, teamId: string): Promise<any> {
		// Assuming BugAnalytics type is not directly imported, using 'any' for now
		try {
			const response = await API.get<any>( // Changed from BugAnalytics to any
				`/projects/${projectId}/teams/${teamId}/bugs/analytics`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch team bug analytics");
		}
	}

	// === Similarity and Duplicate Detection Methods ===

	/**
	 * Check for similar bugs before creating a new bug
	 */
	async checkSimilarity(
		projectSlug: string,
		request: SimilarityCheckRequest
	): Promise<BugSimilarityResult[]> {
		try {
			const response = await API.post<BugSimilarityResult[]>(
				`/projects/${projectSlug}/bugs/similarity/check`,
				request
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to check bug similarity");
		}
	}

	/**
	 * Mark a bug as duplicate of another bug
	 */
	async markAsDuplicate(
		projectSlug: string,
		bugId: number,
		request: MarkDuplicateRequest
	): Promise<void> {
		try {
			await API.post(
				`/projects/${projectSlug}/bugs/similarity/${bugId}/mark-duplicate`,
				request
			);
		} catch (error) {
			throw this.handleError(error, "Failed to mark bug as duplicate");
		}
	}

	/**
	 * Get similarity statistics for a project
	 */
	async getSimilarityStatistics(
		projectSlug: string
	): Promise<SimilarityStatistics> {
		try {
			const response = await API.get<SimilarityStatistics>(
				`/projects/${projectSlug}/bugs/similarity/statistics`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch similarity statistics");
		}
	}

	/**
	 * Compare two specific bugs and get detailed similarity scores
	 */
	async compareBugs(
		projectSlug: string,
		bugId1: number,
		bugId2: number
	): Promise<Record<string, number>> {
		try {
			const response = await API.get<Record<string, number>>(
				`/projects/${projectSlug}/bugs/similarity/compare/${bugId1}/${bugId2}`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to compare bugs");
		}
	}

	/**
	 * Get similarity configuration health for a project
	 */
	async getSimilarityHealth(projectSlug: string): Promise<SimilarityHealth> {
		try {
			const response = await API.get<SimilarityHealth>(
				`/projects/${projectSlug}/bugs/similarity/health`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch similarity health");
		}
	}

	/**
	 * Get similarity configurations for a project
	 */
	async getSimilarityConfigurations(
		projectSlug: string
	): Promise<SimilarityConfig[]> {
		try {
			const response = await API.get<SimilarityConfig[]>(
				`/projects/${projectSlug}/similarity-config`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(
				error,
				"Failed to fetch similarity configurations"
			);
		}
	}

	/**
	 * Get similarity analysis for all bugs in a project
	 */
	async getProjectSimilarityAnalysis(
		projectSlug: string,
		threshold: number = 0.4,
		searchTerm?: string,
		sortBy: string = "similarityScore",
		sortDirection: "asc" | "desc" = "desc",
		page: number = 0,
		size: number = 20,
		startDate?: string,
		endDate?: string
	): Promise<{
		content: BugSimilarityRelationship[];
		totalElements: number;
		totalPages: number;
		number: number;
		size: number;
	}> {
		try {
			const params = new URLSearchParams({
				threshold: threshold.toString(),
				sortBy,
				sortDirection,
				page: page.toString(),
				size: size.toString(),
			});

			if (searchTerm) {
				params.append("searchTerm", searchTerm);
			}

			if (startDate) {
				params.append("startDate", startDate);
			}

			if (endDate) {
				params.append("endDate", endDate);
			}

			const response = await API.get<{
				content: BugSimilarityRelationship[];
				page: {
					totalElements: number;
					totalPages: number;
					number: number;
					size: number;
				};
			}>(`/projects/${projectSlug}/bugs/similarity/analysis?${params}`);

			// Debug logging for pagination
			console.log(
				"BugService -> getProjectSimilarityAnalysis -> Raw API response:",
				response
			);
			console.log(
				"BugService -> getProjectSimilarityAnalysis -> Response data:",
				response.data
			);
			console.log(
				"BugService -> getProjectSimilarityAnalysis -> Pagination metadata:",
				{
					contentLength: response.data.content?.length,
					totalElements: response.data.page?.totalElements,
					totalPages: response.data.page?.totalPages,
					number: response.data.page?.number,
					size: response.data.page?.size,
				}
			);

			// Transform the nested structure to match expected interface
			return {
				content: response.data.content,
				totalElements: response.data.page?.totalElements || 0,
				totalPages: response.data.page?.totalPages || 0,
				number: response.data.page?.number || 0,
				size: response.data.page?.size || 0,
			};
		} catch (error) {
			throw this.handleError(
				error,
				"Failed to fetch project similarity analysis"
			);
		}
	}

	/**
	 * Update configuration for a specific algorithm
	 */
	async updateAlgorithmConfiguration(
		projectSlug: string,
		algorithmName: string,
		config: {
			weight: number;
			threshold: number;
			isEnabled: boolean;
		}
	): Promise<void> {
		try {
			await API.put(
				`/projects/${projectSlug}/similarity-config/${algorithmName}`,
				config
			);
		} catch (error) {
			throw this.handleError(error, "Failed to update algorithm configuration");
		}
	}

	/**
	 * Validate similarity configurations for a project
	 */
	async validateSimilarityConfigurations(
		projectSlug: string
	): Promise<ConfigurationValidation> {
		try {
			const response = await API.get<ConfigurationValidation>(
				`/projects/${projectSlug}/similarity-config/validation`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(
				error,
				"Failed to validate similarity configurations"
			);
		}
	}

	/**
	 * Reset similarity configurations to defaults for a project
	 */
	async resetSimilarityConfigurations(projectSlug: string): Promise<void> {
		try {
			await API.post(`/projects/${projectSlug}/similarity-config/reset`, {});
		} catch (error) {
			throw this.handleError(
				error,
				"Failed to reset similarity configurations"
			);
		}
	}

	/**
	 * Get bug team assignments
	 */
	async getBugTeamAssignments(
		projectId: string,
		projectTicketNumber: number
	): Promise<TeamAssignmentInfo[]> {
		try {
			const response = await API.get<TeamAssignmentInfo[]>(
				`/projects/${projectId}/bugs/${projectTicketNumber}/teams`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch bug team assignments");
		}
	}

	/**
	 * Get bug team assignment recommendations
	 */

	/**
	 * Error handling
	 */
	private handleError(error: any, defaultMessage: string): never {
		return handleApiError(error, defaultMessage, "BugService");
	}

	// === Duplicate Information Methods ===

	/**
	 * Get duplicate information for a bug
	 */
	async getDuplicateInfo(
		projectSlug: string,
		bugId: number
	): Promise<DuplicateInfoResponse> {
		try {
			const response = await API.get(
				`/projects/${projectSlug}/bugs/similarity/bugs/${bugId}/duplicate-info`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to get duplicate information");
		}
	}

	/**
	 * Get duplicate information for a bug by project ticket number
	 */
	async getDuplicateInfoByTicketNumber(
		projectSlug: string,
		projectTicketNumber: number
	): Promise<DuplicateInfoResponse> {
		try {
			const response = await API.get(
				`/projects/${projectSlug}/bugs/similarity/bugs/ticket/${projectTicketNumber}/duplicate-info`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(
				error,
				"Failed to get duplicate information by ticket number"
			);
		}
	}

	async getDuplicatesOfBug(
		projectSlug: string,
		bugId: number
	): Promise<BugDuplicateSummary[]> {
		try {
			const response = await API.get(
				`/projects/${projectSlug}/bugs/similarity/bugs/${bugId}/duplicates`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to get duplicates of bug");
		}
	}

	async getOriginalBug(
		projectSlug: string,
		bugId: number
	): Promise<BugSummary> {
		try {
			const response = await API.get(
				`/projects/${projectSlug}/bugs/similarity/bugs/${bugId}/original-bug`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to get original bug");
		}
	}

	/**
	 * Get duplicate analytics for a project
	 */
	async getDuplicateAnalytics(
		projectSlug: string
	): Promise<DuplicateAnalyticsResponse> {
		try {
			const response = await API.get(
				`/projects/${projectSlug}/bugs/similarity/duplicate-analytics`
			);
			// Transform raw data into DuplicateAnalyticsResponseImpl instance
			const rawData = response.data;
			return new DuplicateAnalyticsResponseImpl(
				rawData.totalDuplicates || 0,
				rawData.duplicatesByDetectionMethod || {},
				rawData.duplicatesByUser || {}
			);
		} catch (error) {
			throw this.handleError(error, "Failed to get duplicate analytics");
		}
	}

	/**
	 * Manually initialize similarity configurations for an existing project.
	 * This is useful for projects created before automatic initialization was implemented.
	 */
	async initializeSimilarityConfigurations(projectSlug: string): Promise<{
		message: string;
		initialized: boolean;
	}> {
		try {
			const response = await API.post(
				`/projects/${projectSlug}/bugs/similarity/initialize-configs`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(
				error,
				"Failed to initialize similarity configurations"
			);
		}
	}
}

// Export singleton instance
export const bugService = BugService.getInstance();
