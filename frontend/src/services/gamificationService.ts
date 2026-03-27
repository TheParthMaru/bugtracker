/**
 * GamificationService
 *
 * Service layer for gamification operations.
 * Handles user points, leaderboards, streaks, and point transactions.
 *
 * Features:
 * - User points and statistics
 * - Project leaderboards
 * - User streaks
 * - Point transaction history
 * - Error handling and response validation
 * - Type-safe API calls
 */

import API from "./api";
import type {
	UserPointsResponse,
	LeaderboardEntryResponse,
	StreakInfoResponse,
	PointTransactionResponse,
	PointTransactionRequest,
	PageResponse,
	LeaderboardTimeframe,
} from "@/types/gamification";

export class GamificationService {
	private static instance: GamificationService;

	// Singleton pattern for consistent service usage
	public static getInstance(): GamificationService {
		if (!GamificationService.instance) {
			GamificationService.instance = new GamificationService();
		}
		return GamificationService.instance;
	}

	/**
	 * Get user's gamification points and statistics
	 */
	async getUserPoints(userId: string): Promise<UserPointsResponse> {
		try {
			const response = await API.get<UserPointsResponse>(
				`/leaderboard/points/${userId}`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch user points");
		}
	}

	/**
	 * Get user's point transaction history with pagination
	 */
	async getPointHistory(
		userId: string,
		page: number = 0,
		size: number = 20
	): Promise<PageResponse<PointTransactionResponse>> {
		try {
			const response = await API.get<PageResponse<PointTransactionResponse>>(
				`/leaderboard/points/${userId}/transactions`,
				{
					params: {
						page,
						size,
						sort: "earnedAt,desc",
					},
				}
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch point history");
		}
	}

	/**
	 * Get project leaderboard with timeframe selection and pagination
	 */
	async getProjectLeaderboard(
		projectId: string,
		timeframe: LeaderboardTimeframe = "all-time",
		page: number = 0,
		size: number = 20
	): Promise<PageResponse<LeaderboardEntryResponse>> {
		try {
			const response = await API.get<PageResponse<LeaderboardEntryResponse>>(
				`/leaderboard/leaderboard/${projectId}`,
				{
					params: {
						timeframe,
						page,
						size,
						sort:
							timeframe === "all-time"
								? "allTimePoints,desc"
								: timeframe === "monthly"
								? "monthlyPoints,desc"
								: "weeklyPoints,desc",
					},
				}
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch project leaderboard");
		}
	}

	/**
	 * Get specific user's project statistics
	 */
	async getUserProjectStats(
		projectId: string,
		userId: string
	): Promise<LeaderboardEntryResponse> {
		try {
			const response = await API.get<LeaderboardEntryResponse>(
				`/leaderboard/leaderboard/${projectId}/user/${userId}`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch user project stats");
		}
	}

	/**
	 * Get user's streak information
	 */
	async getUserStreak(userId: string): Promise<StreakInfoResponse> {
		try {
			const response = await API.get<StreakInfoResponse>(
				`/leaderboard/streaks/${userId}`
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to fetch user streak");
		}
	}

	/**
	 * Award points to a user (typically called by event listeners or for testing)
	 */
	async awardPoints(
		request: PointTransactionRequest
	): Promise<PointTransactionResponse> {
		try {
			const response = await API.post<PointTransactionResponse>(
				"/leaderboard/points/award",
				request
			);
			return response.data;
		} catch (error) {
			throw this.handleError(error, "Failed to award points");
		}
	}

	/**
	 * Error handling utility
	 */
	private handleError(error: any, defaultMessage: string): never {
		console.error("GamificationService Error:", error);

		if (error.response?.data?.message) {
			throw new Error(error.response.data.message);
		}

		if (error.response?.status === 404) {
			throw new Error("Gamification data not found");
		}

		if (error.response?.status === 403) {
			throw new Error("Access denied to gamification data");
		}

		if (error.response?.status === 401) {
			throw new Error("Authentication required for gamification data");
		}

		throw new Error(defaultMessage);
	}
}

// Export singleton instance
export const gamificationService = GamificationService.getInstance();
