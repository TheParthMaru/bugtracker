/**
 * TeamService
 *
 * Service layer for team management operations.
 * Handles all team-related API calls with proper error handling,
 * type safety, and response validation.
 *
 * Features:
 * - Complete CRUD operations for teams
 * - Team member management
 * - User team operations
 * - Error handling and response validation
 * - Request/response logging
 * - Type-safe API calls
 */

import API from "./api";
import { teamCacheService } from "./cacheService";
import type {
	Team,
	TeamMember,
	TeamDetailResponse,
	CreateTeamRequest,
	UpdateTeamRequest,
	AddMemberRequest,
	UpdateMemberRoleRequest,
	TeamMembersListResponse,
	TeamMemberSearchParams,
	ApiError,
	ValidationError,
} from "@/types/team";

export class TeamService {
	private static instance: TeamService;

	// Singleton pattern for consistent service usage
	public static getInstance(): TeamService {
		if (!TeamService.instance) {
			TeamService.instance = new TeamService();
		}
		return TeamService.instance;
	}

	/**
	 * Team CRUD Operations
	 */

	/**
	 * Create a new team within a project
	 */
	async createTeam(
		data: CreateTeamRequest & { projectSlug: string }
	): Promise<Team> {
		try {
			const { projectSlug, ...teamData } = data;
			const response = await API.post<Team>("/teams", teamData, {
				params: { projectSlug },
			});

			// Invalidate relevant caches
			teamCacheService.invalidateTeamsList();
			teamCacheService.invalidateUserTeams();

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to create team");
		}
	}

	/**
	 * Get teams for a specific project
	 */
	async getProjectTeams(projectSlug: string): Promise<Team[]> {
		try {
			// The backend returns a Page<TeamResponse> object, so we need to extract the content
			const response = await API.get<any>(`/projects/${projectSlug}/teams`);

			// Handle pagination response - extract teams from content array
			if (
				response.data &&
				response.data.content &&
				Array.isArray(response.data.content)
			) {
				return response.data.content;
			}

			// Fallback: if response is already an array (for backward compatibility)
			if (Array.isArray(response.data)) {
				return response.data;
			}

			// If neither, return empty array
			console.warn(
				"Unexpected response structure from getProjectTeams:",
				response.data
			);
			return [];
		} catch (error) {
			throw this.handleError(error, "Failed to fetch project teams");
		}
	}

	/**
	 * Get team by slug within a project context
	 */
	async getTeamBySlug(projectSlug: string, teamSlug: string): Promise<Team> {
		console.log("teamService -> getTeamBySlug -> Called with:", {
			projectSlug,
			teamSlug,
		});
		try {
			const url = `/projects/${projectSlug}/teams/${teamSlug}`;
			console.log("teamService -> getTeamBySlug -> Making API call to:", url);
			const response = await API.get<Team>(url);
			console.log("teamService -> getTeamBySlug -> API response received:", {
				status: response.status,
				data: {
					id: response.data.id,
					name: response.data.name,
					teamSlug: response.data.teamSlug,
				},
			});
			return response.data;
		} catch (error) {
			console.error("teamService -> getTeamBySlug -> API call failed:", error);
			throw this.handleError(error, "Failed to fetch team details");
		}
	}

	/**
	 * Update team within a project context
	 */
	async updateTeam(
		projectSlug: string,
		teamSlug: string,
		data: UpdateTeamRequest
	): Promise<Team> {
		console.log("teamService -> updateTeam -> Called with:", {
			projectSlug,
			teamSlug,
			data,
		});
		try {
			const url = `/projects/${projectSlug}/teams/${teamSlug}`;
			console.log("teamService -> updateTeam -> Making API call to:", url);
			const response = await API.put<Team>(url, data);
			console.log("teamService -> updateTeam -> API response received:", {
				status: response.status,
				data: {
					id: response.data.id,
					name: response.data.name,
					teamSlug: response.data.teamSlug,
					projectSlug: response.data.projectSlug,
				},
			});

			// Invalidate relevant caches
			teamCacheService.invalidateTeam(teamSlug);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to update team");
		}
	}

	/**
	 * Delete team within a project context
	 */
	async deleteTeam(projectSlug: string, teamSlug: string): Promise<void> {
		try {
			await API.delete(`/projects/${projectSlug}/teams/${teamSlug}`);

			// Invalidate relevant caches
			teamCacheService.invalidateTeam(teamSlug);
		} catch (error) {
			throw this.handleError(error, "Failed to delete team");
		}
	}

	/**
	 * Team Member Management Operations
	 */

	/**
	 * Get team members with pagination within a project context
	 */
	async getTeamMembers(
		projectSlug: string,
		teamSlug: string,
		params?: TeamMemberSearchParams
	): Promise<TeamMembersListResponse> {
		try {
			const response = await API.get<TeamMembersListResponse>(
				`/projects/${projectSlug}/teams/${teamSlug}/members`,
				{ params }
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch team members");
		}
	}

	/**
	 * Add member to team within a project context
	 */
	async addMember(
		projectSlug: string,
		teamSlug: string,
		data: AddMemberRequest
	): Promise<TeamMember> {
		try {
			const response = await API.post<TeamMember>(
				`/projects/${projectSlug}/teams/${teamSlug}/members`,
				data
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to add team member");
		}
	}

	/**
	 * Update member role within a project context
	 */
	async updateMemberRole(
		projectSlug: string,
		teamSlug: string,
		userId: string,
		data: UpdateMemberRoleRequest
	): Promise<TeamMember> {
		try {
			const response = await API.put<TeamMember>(
				`/projects/${projectSlug}/teams/${teamSlug}/members/${userId}`,
				data
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to update member role");
		}
	}

	/**
	 * Remove member from team within a project context
	 */
	async removeMember(
		projectSlug: string,
		teamSlug: string,
		userId: string
	): Promise<void> {
		try {
			await API.delete(
				`/projects/${projectSlug}/teams/${teamSlug}/members/${userId}`
			);
		} catch (error) {
			throw this.handleError(error, "Failed to remove team member");
		}
	}

	/**
	 * Leave team within a project context
	 */
	async leaveTeam(projectSlug: string, teamSlug: string): Promise<void> {
		try {
			await API.post(`/projects/${projectSlug}/teams/${teamSlug}/leave`);
		} catch (error) {
			throw this.handleError(error, "Failed to leave team");
		}
	}

	/**
	 * User Team Operations
	 */

	/**
	 * Get current user's teams
	 */
	async getUserTeams(): Promise<Team[]> {
		try {
			// Try to get from cache first
			const cachedData = teamCacheService.getUserTeams();
			if (cachedData) {
				return cachedData;
			}

			const response = await API.get<Team[]>("/users/me/teams");

			// Cache the response
			teamCacheService.setUserTeams(response.data);

			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch user teams");
		}
	}

	/**
	 * Utility Methods
	 */

	/**
	 * Join a public team within a project context
	 */
	async joinTeam(projectSlug: string, teamSlug: string): Promise<TeamMember> {
		try {
			// Call the join endpoint directly
			const response = await API.post(
				`/projects/${projectSlug}/teams/${teamSlug}/join`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to join team");
		}
	}

	/**
	 * Get team details with members within a project context
	 */
	async getTeamDetails(
		projectSlug: string,
		teamSlug: string
	): Promise<TeamDetailResponse> {
		try {
			const [team, membersResponse] = await Promise.all([
				this.getTeamBySlug(projectSlug, teamSlug),
				this.getTeamMembers(projectSlug, teamSlug),
			]);

			return {
				...team,
				members: membersResponse.content,
			};
		} catch (error) {
			throw this.handleError(error, "Failed to fetch team details");
		}
	}

	/**
	 * Check if user is team member
	 */
	async isTeamMember(teamId: string): Promise<boolean> {
		try {
			const userTeams = await this.getUserTeams();
			return userTeams.some((team) => team.id === teamId);
		} catch (error) {
			// If we can't fetch user teams, assume not a member
			return false;
		}
	}

	/**
	 * Check if user is team admin
	 */
	async isTeamAdmin(teamId: string): Promise<boolean> {
		try {
			const userTeams = await this.getUserTeams();
			const team = userTeams.find((team) => team.id === teamId);
			return team?.currentUserRole === "ADMIN";
		} catch (error) {
			return false;
		}
	}

	/**
	 * Error Handling
	 */

	private handleError(error: any, defaultMessage: string): never {
		// Log error for debugging
		console.error("TeamService Error:", error);

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
					throw new Error("The requested resource was not found.");
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
export const teamService = TeamService.getInstance();

// Export default for convenient importing
export default teamService;
