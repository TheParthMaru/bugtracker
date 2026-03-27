/**
 * Gamification-related TypeScript types
 *
 * This file contains all TypeScript interfaces and types for gamification
 * functionality, matching the backend API specification.
 */

// Gamification response interfaces
export interface UserPointsResponse {
	userId: string;
	totalPoints: number;
	currentStreak: number;
	maxStreak: number;
	bugsResolved: number;
	lastActivity: string;
	userDisplayName: string;
}

export interface LeaderboardEntryResponse {
	leaderboardEntryId: string;
	projectId: string;
	userId: string;
	weeklyPoints: number;
	monthlyPoints: number;
	allTimePoints: number;
	bugsResolved: number;
	currentStreak: number;
	updatedAt: string;
	userDisplayName: string;
	rank: number;
}

export interface StreakInfoResponse {
	userId: string;
	currentStreak: number;
	maxStreak: number;
	lastLoginDate: string;
	streakStartDate: string;
}

export interface PointTransactionResponse {
	transactionId: string;
	userId: string;
	projectId?: string; // Optional for system activities
	points: number;
	reason: string;
	bugId?: number;
	earnedAt: string;
}

export interface PointTransactionRequest {
	userId: string;
	projectId?: string; // Optional for system activities
	points: number;
	reason: string;
	bugId?: number;
}

// Pagination response interface
export interface PageResponse<T> {
	content: T[];
	pageable: {
		pageNumber: number;
		pageSize: number;
		sort: {
			sorted: boolean;
			unsorted: boolean;
			empty: boolean;
		};
		offset: number;
		paged: boolean;
		unpaged: boolean;
	};
	totalElements: number;
	totalPages: number;
	last: boolean;
	size: number;
	number: number;
	sort: {
		sorted: boolean;
		unsorted: boolean;
		empty: boolean;
	};
	first: boolean;
	numberOfElements: number;
	empty: boolean;
}

// Timeframe types for leaderboards
export type LeaderboardTimeframe = "weekly" | "monthly" | "all-time";

// Component prop interfaces
export interface GamificationDashboardProps {
	userId: string;
}

export interface LeaderboardComponentProps {
	projectId: string;
	initialTimeframe?: LeaderboardTimeframe;
}

export interface UserGamificationProfileProps {
	userId: string;
}

export interface StreakVisualizationComponentProps {
	userId: string;
	currentStreak: number;
	maxStreak: number;
	lastLoginDate: string;
}
