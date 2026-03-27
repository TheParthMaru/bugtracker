/**
 * UserService
 *
 * Service layer for user management operations.
 * Handles user search, profile management, and user-related API calls.
 *
 * Features:
 * - User search functionality
 * - User profile operations
 * - Error handling and response validation
 * - Type-safe API calls
 */

import API from "./api";
import type {
	User,
	ApiError,
	ValidationError,
	UserSearchResponse,
} from "@/types/user";

export interface UserSearchParams {
	search?: string;
	page?: number;
	size?: number;
	role?: string;
}

export class UserService {
	private static instance: UserService;

	// Singleton pattern for consistent service usage
	public static getInstance(): UserService {
		if (!UserService.instance) {
			UserService.instance = new UserService();
		}
		return UserService.instance;
	}

	/**
	 * Search users with pagination and filtering
	 */
	async searchUsers(params?: UserSearchParams): Promise<UserSearchResponse> {
		try {
			const response = await API.get<UserSearchResponse>("/users/search", {
				params,
			});
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to search users");
		}
	}

	/**
	 * Get current user profile
	 */
	async getCurrentUser(): Promise<User> {
		try {
			const response = await API.get<User>("/profile");
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch user profile");
		}
	}

	/**
	 * Get user by ID
	 */
	async getUserById(userId: string): Promise<User> {
		try {
			const response = await API.get<User>(`/users/${userId}`);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch user details");
		}
	}

	/**
	 * Get users by IDs (for bulk operations)
	 */
	async getUsersByIds(userIds: string[]): Promise<User[]> {
		try {
			const response = await API.post<User[]>("/users/bulk", { userIds });
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch users");
		}
	}

	/**
	 * Error handling utility
	 */
	private handleError(error: any, defaultMessage: string): never {
		console.error("UserService Error:", error);

		if (error.response?.data) {
			const apiError = error.response.data as ApiError;
			throw new Error(apiError.message || defaultMessage);
		}

		if (error.response?.status === 422) {
			const validationError = error.response.data as ValidationError;
			const errorMessage = Object.values(validationError.errors).join(", ");
			throw new Error(errorMessage || "Validation failed");
		}

		if (error.response?.status === 404) {
			throw new Error("User not found");
		}

		if (error.response?.status === 403) {
			throw new Error("Access denied");
		}

		if (error.response?.status === 401) {
			throw new Error("Authentication required");
		}

		throw new Error(defaultMessage);
	}
}

// Export singleton instance
export const userService = UserService.getInstance();
