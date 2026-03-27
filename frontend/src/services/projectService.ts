/**
 * ProjectService
 *
 * Service layer for project management operations.
 * Handles all project-related API calls with proper error handling,
 * type safety, and response validation.
 *
 * Features:
 * - Complete CRUD operations for projects
 * - Project member management with approval workflow
 * - User project operations
 * - Error handling and response validation
 * - Request/response logging
 * - Type-safe API calls
 * - Caching integration
 */

import API from "./api";
import { projectCacheService } from "./cacheService";
import type {
	Project,
	ProjectMember,
	CreateProjectRequest,
	UpdateProjectRequest,
	UpdateMemberRoleRequest,
	ProjectsListResponse,
	ProjectMembersListResponse,
	ProjectSearchParams,
	ProjectMemberSearchParams,
	ProjectDetailPageData,
	UserProjectMembership,
	ProjectStats,
	ApiError,
	ValidationError,
	// Project-teams integration types
	ProjectTeamsListResponse,
	ProjectTeamSearchParams,
	ProjectTeamMemberSearchParams,
	TeamSuccessResponse,
} from "@/types/project";
import type { UserSearchResponse } from "@/types/user";
import { ProjectRole } from "@/types/project";
import type {
	Team,
	CreateTeamRequest,
	UpdateTeamRequest,
	AddMemberRequest,
	TeamMembersListResponse,
} from "@/types/team";
import { TeamRole } from "@/types/team";

export class ProjectService {
	private static instance: ProjectService;

	// Singleton pattern for consistent service usage
	public static getInstance(): ProjectService {
		if (!ProjectService.instance) {
			ProjectService.instance = new ProjectService();
		}
		return ProjectService.instance;
	}

	/**
	 * Project CRUD Operations
	 */

	/**
	 * Create a new project
	 */
	async createProject(data: CreateProjectRequest): Promise<Project> {
		try {
			const response = await API.post<Project>("/projects", data);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectsList();
			projectCacheService.invalidateUserProjects();

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to create project");
		}
	}

	/**
	 * Get projects with pagination and filtering
	 */
	async getProjects(
		params?: ProjectSearchParams,
		bypassCache: boolean = false
	): Promise<ProjectsListResponse> {
		try {
			console.log(
				"getProjects called with params:",
				params,
				"bypassCache:",
				bypassCache
			);

			// Create cache key from params
			const cacheKey = params ? JSON.stringify(params) : undefined;

			// Try to get from cache first (unless bypassing)
			if (!bypassCache) {
				const cachedData = projectCacheService.getProjectsList(cacheKey);
				if (cachedData) {
					console.log("Returning cached projects list:", cachedData);
					return cachedData;
				}
			}

			console.log("Fetching projects from API with endpoint: /projects");
			const response = await API.get<ProjectsListResponse>("/projects", {
				params,
			});

			console.log("API Response for projects:", response.data);

			// Cache the response
			projectCacheService.setProjectsList(response.data, cacheKey);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch projects");
		}
	}

	/**
	 * Get project by ID
	 */
	async getProjectById(projectId: string): Promise<Project> {
		try {
			// Try cache first
			const cachedData = projectCacheService.getProjectDetail(projectId);
			if (cachedData) {
				return cachedData;
			}

			const response = await API.get<Project>(`/projects/${projectId}`);

			// Cache the response
			projectCacheService.setProjectDetail(projectId, response.data);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch project details");
		}
	}

	/**
	 * Get project by slug
	 */
	async getProjectBySlug(projectSlug: string): Promise<Project> {
		try {
			// Try cache first
			const cachedData = projectCacheService.getProjectBySlug(projectSlug);
			if (cachedData) {
				return cachedData;
			}

			const response = await API.get<Project>(`/projects/${projectSlug}`);

			// Cache the response
			projectCacheService.setProjectBySlug(projectSlug, response.data);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch project details");
		}
	}

	/**
	 * Update project
	 */
	async updateProject(
		projectSlug: string,
		data: UpdateProjectRequest
	): Promise<Project> {
		try {
			const response = await API.put<Project>(`/projects/${projectSlug}`, data);

			// Invalidate relevant caches
			projectCacheService.invalidateProject(response.data.id);
			projectCacheService.invalidateProjectBySlug(projectSlug);
			projectCacheService.invalidateProjectsList();

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to update project");
		}
	}

	/**
	 * Delete project
	 */
	async deleteProject(projectSlug: string): Promise<void> {
		try {
			await API.delete(`/projects/${projectSlug}`);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectBySlug(projectSlug);
			projectCacheService.invalidateProjectsList();
			projectCacheService.invalidateUserProjects();
		} catch (error) {
			throw this.handleError(error, "Failed to delete project");
		}
	}

	/**
	 * Project Membership Operations
	 */

	/**
	 * Get project members with pagination and filtering
	 */
	async getProjectMembers(
		projectSlug: string,
		params?: ProjectMemberSearchParams
	): Promise<ProjectMembersListResponse> {
		try {
			// Try cache first for basic member list (no params)
			if (!params) {
				const cachedData = projectCacheService.getProjectMembers(projectSlug);
				if (cachedData) {
					return cachedData;
				}
			}

			const response = await API.get<ProjectMembersListResponse>(
				`/projects/${projectSlug}/members`,
				{ params }
			);

			// Cache basic member list
			if (!params) {
				projectCacheService.setProjectMembers(projectSlug, response.data);
			}

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch project members");
		}
	}

	/**
	 * Get pending join requests (admin only)
	 */
	async getPendingRequests(projectSlug: string): Promise<ProjectMember[]> {
		try {
			const response = await API.get<ProjectMembersListResponse>(
				`/projects/${projectSlug}/requests`
			);
			return response.data.content;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch pending requests");
		}
	}

	/**
	 * Join project (request to join)
	 */
	async joinProject(projectSlug: string): Promise<ProjectMember> {
		try {
			const response = await API.post<ProjectMember>(
				`/projects/${projectSlug}/join`
			);

			// Aggressively invalidate all relevant caches to ensure fresh data
			projectCacheService.invalidateProjectBySlug(projectSlug);
			projectCacheService.invalidateProjectMembers(projectSlug);
			projectCacheService.invalidateProjectsList();
			projectCacheService.invalidateUserProjects();

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to join project");
		}
	}

	/**
	 * Approve member join request (admin only)
	 */
	async approveMember(
		projectSlug: string,
		userId: string
	): Promise<ProjectMember> {
		try {
			const response = await API.post<ProjectMember>(
				`/projects/${projectSlug}/members/${userId}/approve`
			);

			// Aggressively invalidate all relevant caches to ensure fresh data
			projectCacheService.invalidateProjectBySlug(projectSlug);
			projectCacheService.invalidateProjectMembers(projectSlug);
			projectCacheService.invalidateProjectsList();
			projectCacheService.invalidateUserProjects();

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to approve member");
		}
	}

	/**
	 * Reject member join request (admin only)
	 */
	async rejectMember(projectSlug: string, userId: string): Promise<void> {
		try {
			await API.post(`/projects/${projectSlug}/members/${userId}/reject`);

			// Aggressively invalidate all relevant caches to ensure fresh data
			projectCacheService.invalidateProjectBySlug(projectSlug);
			projectCacheService.invalidateProjectMembers(projectSlug);
			projectCacheService.invalidateProjectsList();
			projectCacheService.invalidateUserProjects();
		} catch (error) {
			throw this.handleError(error, "Failed to reject member");
		}
	}

	/**
	 * Update member role (admin only)
	 */
	async updateMemberRole(
		projectSlug: string,
		userId: string,
		data: UpdateMemberRoleRequest
	): Promise<ProjectMember> {
		try {
			const response = await API.put<ProjectMember>(
				`/projects/${projectSlug}/members/${userId}/role`,
				data
			);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectMembers(projectSlug);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to update member role");
		}
	}

	/**
	 * Remove member from project (admin only)
	 */
	async removeMember(projectSlug: string, userId: string): Promise<void> {
		try {
			await API.delete(`/projects/${projectSlug}/members/${userId}`);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectMembers(projectSlug);
		} catch (error) {
			throw this.handleError(error, "Failed to remove member");
		}
	}

	/**
	 * Leave project
	 */
	async leaveProject(projectSlug: string): Promise<void> {
		try {
			await API.post(`/projects/${projectSlug}/leave`);

			// Aggressively invalidate all relevant caches to ensure fresh data
			projectCacheService.invalidateProjectBySlug(projectSlug);
			projectCacheService.invalidateProjectMembers(projectSlug);
			projectCacheService.invalidateProjectsList();
			projectCacheService.invalidateUserProjects();

			// Force clear all project-related cache to ensure immediate update
			projectCacheService.clear();
		} catch (error) {
			throw this.handleError(error, "Failed to leave project");
		}
	}

	/**
	 * User Project Operations
	 */

	/**
	 * Get current user's projects
	 */
	async getUserProjects(bypassCache: boolean = false): Promise<Project[]> {
		try {
			console.log("getUserProjects called with bypassCache:", bypassCache);

			// Try to get from cache first (unless bypassing)
			if (!bypassCache) {
				const cachedData = projectCacheService.getUserProjects();
				if (cachedData) {
					console.log("Returning cached user projects:", cachedData);
					return cachedData;
				}
			}

			console.log("Fetching user projects from API...");
			const response = await API.get<ProjectsListResponse>(
				"/projects/users/me/projects"
			);

			console.log("API Response for user projects:", response.data);
			console.log("Projects content:", response.data.content);
			console.log("Number of projects:", response.data.content.length);

			// Cache the response
			projectCacheService.setUserProjects(response.data.content);

			return response.data.content;
		} catch (error) {
			console.error("Error fetching user projects:", error);
			throw this.handleError(error, "Failed to fetch user projects");
		}
	}

	/**
	 * Get user's project memberships with details
	 */
	async getUserProjectMemberships(): Promise<UserProjectMembership[]> {
		try {
			const response = await API.get<UserProjectMembership[]>(
				"/projects/users/me/project-memberships"
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch user project memberships");
		}
	}

	/**
	 * Get user's project statistics
	 *
	 * @deprecated Use frontend calculation instead - this endpoint doesn't exist
	 */
	async getUserProjectStats(): Promise<ProjectStats> {
		throw new Error(
			"This endpoint is not implemented. Use frontend calculation instead."
		);
	}

	/**
	 * Utility Methods
	 */

	/**
	 * Get project details with members and requests
	 */
	async getProjectDetailPageData(
		projectSlug: string
	): Promise<ProjectDetailPageData> {
		try {
			const [project, membersResponse] = await Promise.all([
				this.getProjectBySlug(projectSlug),
				this.getProjectMembers(projectSlug),
			]);

			// Get pending requests if user is admin
			let pendingRequests: ProjectMember[] = [];
			if (project.userRole === ProjectRole.ADMIN) {
				try {
					pendingRequests = await this.getPendingRequests(projectSlug);
				} catch (error) {
					// Non-critical error, continue without pending requests
					console.warn("Failed to fetch pending requests:", error);
				}
			}

			// Determine user permissions
			const userRole = project.userRole;
			const userStatus = project.userMembershipStatus;
			const canEdit = userRole === ProjectRole.ADMIN;
			const canDelete = userRole === ProjectRole.ADMIN;
			const canManageMembers = userRole === ProjectRole.ADMIN;

			return {
				project,
				members: membersResponse.content,
				pendingRequests,
				userRole,
				userStatus,
				canEdit,
				canDelete,
				canManageMembers,
			};
		} catch (error) {
			throw this.handleError(error, "Failed to fetch project details");
		}
	}

	/**
	 * Search projects by name
	 */
	async searchProjects(
		query: string,
		params?: Omit<ProjectSearchParams, "search">
	): Promise<ProjectsListResponse> {
		return this.getProjects({ ...params, search: query });
	}

	/**
	 * Check if user is project member
	 */
	async isProjectMember(projectId: string): Promise<boolean> {
		try {
			const userProjects = await this.getUserProjects();
			return userProjects.some((project) => project.id === projectId);
		} catch (error) {
			// If we can't fetch user projects, assume not a member
			return false;
		}
	}

	/**
	 * Check if user is project admin
	 */
	async isProjectAdmin(projectId: string): Promise<boolean> {
		try {
			const userProjects = await this.getUserProjects();
			const project = userProjects.find((p) => p.id === projectId);
			return project?.userRole === ProjectRole.ADMIN || false;
		} catch (error) {
			// If we can't fetch user projects, assume not an admin
			return false;
		}
	}

	/**
	 * Get user's role in a specific project
	 */
	async getUserRoleInProject(projectId: string): Promise<ProjectRole | null> {
		try {
			const userProjects = await this.getUserProjects();
			const project = userProjects.find((p) => p.id === projectId);
			return project?.userRole || null;
		} catch (error) {
			return null;
		}
	}

	// PROJECT-TEAMS INTEGRATION METHODS

	/**
	 * Team Management within Projects
	 */

	/**
	 * Get teams within a project
	 */
	async getProjectTeams(
		projectSlug: string,
		params?: ProjectTeamSearchParams,
		bypassCache: boolean = false
	): Promise<ProjectTeamsListResponse> {
		try {
			// Create cache key from params
			const cacheKey = params ? JSON.stringify(params) : undefined;

			// Try to get from cache first (unless bypassing)
			if (!bypassCache) {
				const cachedData = projectCacheService.getProjectTeams(
					projectSlug,
					cacheKey
				);
				if (cachedData) {
					return cachedData;
				}
			}

			const response = await API.get<ProjectTeamsListResponse>(
				`/projects/${projectSlug}/teams`,
				{ params }
			);

			// Cache the response
			projectCacheService.setProjectTeams(projectSlug, response.data, cacheKey);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch project teams");
		}
	}

	/**
	 * Create a team within a project
	 */
	async createProjectTeam(
		projectSlug: string,
		data: CreateTeamRequest
	): Promise<Team> {
		try {
			const response = await API.post<Team>(
				`/projects/${projectSlug}/teams`,
				data
			);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectTeams(projectSlug);
			projectCacheService.invalidateProject(projectSlug);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to create team in project");
		}
	}

	/**
	 * Get a specific team within a project
	 */
	async getProjectTeam(projectSlug: string, teamSlug: string): Promise<Team> {
		console.log("projectService -> getProjectTeam -> Called with:", {
			projectSlug,
			teamSlug,
		});
		try {
			const url = `/projects/${projectSlug}/teams/${teamSlug}`;
			console.log(
				"projectService -> getProjectTeam -> Making API call to:",
				url
			);
			const response = await API.get<Team>(url);
			console.log(
				"projectService -> getProjectTeam -> API response received:",
				{
					status: response.status,
					data: {
						id: response.data.id,
						name: response.data.name,
						teamSlug: response.data.teamSlug,
					},
				}
			);
			return response.data;
		} catch (error) {
			console.error(
				"projectService -> getProjectTeam -> API call failed:",
				error
			);
			throw this.handleError(error, "Failed to fetch project team");
		}
	}

	/**
	 * Update a team within a project
	 */
	async updateProjectTeam(
		projectSlug: string,
		teamSlug: string,
		data: UpdateTeamRequest
	): Promise<TeamSuccessResponse> {
		try {
			const response = await API.put<TeamSuccessResponse>(
				`/projects/${projectSlug}/teams/${teamSlug}`,
				data
			);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectTeams(projectSlug);
			projectCacheService.invalidateProject(projectSlug);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to update project team");
		}
	}

	/**
	 * Delete a team within a project
	 */
	async deleteProjectTeam(
		projectSlug: string,
		teamSlug: string
	): Promise<TeamSuccessResponse> {
		try {
			const response = await API.delete<TeamSuccessResponse>(
				`/projects/${projectSlug}/teams/${teamSlug}`
			);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectTeams(projectSlug);
			projectCacheService.invalidateProject(projectSlug);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to delete project team");
		}
	}

	/**
	 * Team Member Management within Projects
	 */

	/**
	 * Get team members within a project
	 */
	async getProjectTeamMembers(
		projectSlug: string,
		teamSlug: string,
		params?: ProjectTeamMemberSearchParams,
		bypassCache: boolean = false
	): Promise<TeamMembersListResponse> {
		try {
			// Create cache key from params
			const cacheKey = params ? JSON.stringify(params) : undefined;

			// Try to get from cache first (unless bypassing)
			if (!bypassCache) {
				const cachedData = projectCacheService.getProjectTeamMembers(
					projectSlug,
					teamSlug,
					cacheKey
				);
				if (cachedData) {
					return cachedData;
				}
			}

			const response = await API.get<TeamMembersListResponse>(
				`/projects/${projectSlug}/teams/${teamSlug}/members`,
				{ params }
			);

			// Cache the response
			projectCacheService.setProjectTeamMembers(
				projectSlug,
				teamSlug,
				response.data,
				cacheKey
			);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch project team members");
		}
	}

	/**
	 * Add a member to a team within a project
	 */
	async addProjectTeamMember(
		projectSlug: string,
		teamSlug: string,
		data: AddMemberRequest
	): Promise<TeamSuccessResponse> {
		try {
			const response = await API.post<TeamSuccessResponse>(
				`/projects/${projectSlug}/teams/${teamSlug}/members`,
				data
			);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectTeamMembers(projectSlug, teamSlug);
			projectCacheService.invalidateProjectTeams(projectSlug);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to add member to project team");
		}
	}

	/**
	 * Search for project members (for adding to teams)
	 */
	async searchProjectMembers(
		projectSlug: string,
		params?: {
			search?: string;
			page?: number;
			size?: number;
		}
	): Promise<UserSearchResponse> {
		try {
			const response = await API.get<UserSearchResponse>(
				`/projects/${projectSlug}/members/search`,
				{ params }
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to search project members");
		}
	}

	/**
	 * Update a team member's role within a project
	 */
	async updateProjectTeamMemberRole(
		projectSlug: string,
		teamSlug: string,
		userId: string,
		data: UpdateMemberRoleRequest
	): Promise<TeamSuccessResponse> {
		try {
			const response = await API.put<TeamSuccessResponse>(
				`/projects/${projectSlug}/teams/${teamSlug}/members/${userId}`,
				data
			);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectTeamMembers(projectSlug, teamSlug);
			projectCacheService.invalidateProjectTeams(projectSlug);

			return response.data;
		} catch (error) {
			throw this.handleError(
				error,
				"Failed to update project team member role"
			);
		}
	}

	/**
	 * Remove a member from a team within a project
	 */
	async removeProjectTeamMember(
		projectSlug: string,
		teamSlug: string,
		userId: string
	): Promise<TeamSuccessResponse> {
		try {
			const response = await API.delete<TeamSuccessResponse>(
				`/projects/${projectSlug}/teams/${teamSlug}/members/${userId}`
			);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectTeamMembers(projectSlug, teamSlug);
			projectCacheService.invalidateProjectTeams(projectSlug);

			return response.data;
		} catch (error) {
			throw this.handleError(
				error,
				"Failed to remove member from project team"
			);
		}
	}

	/**
	 * Leave a team within a project
	 */
	async leaveProjectTeam(
		projectSlug: string,
		teamSlug: string
	): Promise<TeamSuccessResponse> {
		try {
			const response = await API.post<TeamSuccessResponse>(
				`/projects/${projectSlug}/teams/${teamSlug}/leave`
			);

			// Invalidate relevant caches
			projectCacheService.invalidateProjectTeamMembers(projectSlug, teamSlug);
			projectCacheService.invalidateProjectTeams(projectSlug);
			projectCacheService.invalidateUserProjects();

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to leave project team");
		}
	}

	/**
	 * Permission Checking
	 */

	/**
	 * Check if user can create teams in a project
	 */
	async canCreateTeamsInProject(projectSlug: string): Promise<boolean> {
		try {
			const project = await this.getProjectBySlug(projectSlug);
			return project.isUserAdmin || false;
		} catch (error) {
			return false;
		}
	}

	/**
	 * Check if user can manage a team within a project
	 */
	async canManageProjectTeam(
		projectSlug: string,
		teamSlug: string
	): Promise<boolean> {
		try {
			const [project, team] = await Promise.all([
				this.getProjectBySlug(projectSlug),
				this.getProjectTeam(projectSlug, teamSlug),
			]);

			// Project admin can manage any team
			if (project.isUserAdmin) {
				return true;
			}

			// Team admin can manage their team
			return team.currentUserRole === TeamRole.ADMIN;
		} catch (error) {
			return false;
		}
	}

	/**
	 * Error Handling
	 */

	private handleError(error: any, defaultMessage: string): never {
		// Log error for debugging
		console.error("ProjectService Error:", error);

		// Handle different error types
		if (error.response?.data) {
			const errorData = error.response.data;

			// Handle API errors with structured format
			if (errorData.code && errorData.message) {
				const apiError: ApiError = errorData;
				throw new Error(apiError.message);
			}

			// Handle validation errors
			if (errorData.errors && errorData.status === "error") {
				const validationError: ValidationError = errorData;
				const errorMessages = Object.values(validationError.errors).join(", ");
				throw new Error(errorMessages);
			}

			// Handle string error messages
			if (typeof errorData === "string") {
				throw new Error(errorData);
			}

			// Handle object with message property
			if (errorData.message) {
				throw new Error(errorData.message);
			}
		}

		// Handle network errors
		if (error.code === "NETWORK_ERROR") {
			throw new Error("Network error. Please check your connection.");
		}

		// Handle timeout errors
		if (error.code === "ECONNABORTED") {
			throw new Error("Request timeout. Please try again.");
		}

		// Handle HTTP status codes
		if (error.response?.status) {
			switch (error.response.status) {
				case 401:
					throw new Error("You are not authorized. Please log in again.");
				case 403:
					throw new Error("You don't have permission to perform this action.");
				case 404:
					throw new Error("The requested project was not found.");
				case 409:
					throw new Error("There was a conflict with your request.");
				case 422:
					throw new Error("Invalid data provided.");
				case 500:
					throw new Error("Server error. Please try again later.");
				default:
					throw new Error(`HTTP ${error.response.status}: ${defaultMessage}`);
			}
		}

		// Fallback to default message
		throw new Error(defaultMessage);
	}
}

// Export singleton instance
export const projectService = ProjectService.getInstance();
