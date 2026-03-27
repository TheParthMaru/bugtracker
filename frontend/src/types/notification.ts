/**
 * Notification Type Definitions
 *
 * TypeScript interfaces and types for the notification system.
 * These match the backend DTOs and provide type safety throughout the frontend.
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

// ===== Core Notification Types =====

export interface Notification {
	notificationId: number;
	eventType: string;
	title: string;
	message: string;
	data?: string;
	isRead: boolean;
	isDismissed: boolean;
	createdAt: string;
	readAt?: string;
	relatedBugId?: number;
	relatedProjectId?: string;
	relatedTeamId?: string;
	relatedUserId?: string;
	projectSlug?: string;
	projectTicketNumber?: number;
}

export interface UnreadCount {
	count: number;
}

// ===== Notification Preferences Types =====

export type EmailFrequency = "IMMEDIATE" | "DAILY" | "WEEKLY";

export interface NotificationPreferences {
	preferenceId: number;
	userId: string;

	// Channel preferences
	inAppEnabled: boolean;
	emailEnabled: boolean;
	toastEnabled: boolean;

	// Bug notification preferences
	bugAssigned: boolean;
	bugStatusChanged: boolean;
	bugPriorityChanged: boolean;
	bugCommented: boolean;
	bugMentioned: boolean;
	bugAttachmentAdded: boolean;

	projectRoleChanged: boolean;
	projectMemberJoined: boolean;

	teamRoleChanged: boolean;
	teamMemberJoined: boolean;

	// Gamification notification preferences
	gamificationPoints: boolean;
	gamificationAchievements: boolean;
	gamificationLeaderboard: boolean;

	// Email specific settings
	emailFrequency: EmailFrequency;
	timezone: string;

	// Metadata
	createdAt: string;
	updatedAt: string;
}

export interface UpdateNotificationPreferencesRequest {
	// Channel preferences
	inAppEnabled: boolean;
	emailEnabled: boolean;
	toastEnabled: boolean;

	// Bug notification preferences
	bugAssigned: boolean;
	bugStatusChanged: boolean;
	bugPriorityChanged: boolean;
	bugCommented: boolean;
	bugMentioned: boolean;
	bugAttachmentAdded: boolean;

	projectRoleChanged: boolean;
	projectMemberJoined: boolean;

	teamRoleChanged: boolean;
	teamMemberJoined: boolean;

	// Gamification notification preferences
	gamificationPoints: boolean;
	gamificationAchievements: boolean;
	gamificationLeaderboard: boolean;

	// Email specific settings
	emailFrequency: EmailFrequency;
	timezone: string;
}

// ===== WebSocket Message Types =====

export interface WebSocketMessage {
	type: "notification" | "count" | "toast" | "in-app";
	timestamp: number;
}

export interface NotificationMessage extends WebSocketMessage {
	type: "notification";
	data: Notification;
}

export interface CountMessage extends WebSocketMessage {
	type: "count";
	data: {
		count: number;
	};
}

export interface ToastMessage extends WebSocketMessage {
	type: "toast";
	data: {
		title: string;
		message: string;
		type: "info" | "success" | "warning" | "error";
	};
}

export interface InAppMessage extends WebSocketMessage {
	type: "in-app";
	data: {
		content: string;
		notificationId: number;
	};
}

// ===== API Response Types =====

export interface NotificationsResponse {
	content: Notification[];
	pageable: {
		sort: {
			empty: boolean;
			sorted: boolean;
			unsorted: boolean;
		};
		offset: number;
		pageSize: number;
		pageNumber: number;
		paged: boolean;
		unpaged: boolean;
	};
	last: boolean;
	totalElements: number;
	totalPages: number;
	size: number;
	number: number;
	sort: {
		empty: boolean;
		sorted: boolean;
		unsorted: boolean;
	};
	first: boolean;
	numberOfElements: number;
	empty: boolean;
}

export interface NotificationFilters {
	page?: number;
	size?: number;
	unreadOnly?: boolean;
	type?: string;
}

// ===== Event Type Constants =====

export const NOTIFICATION_EVENT_TYPES = {
	// Bug events
	BUG_ASSIGNED: "BUG_ASSIGNED",
	BUG_STATUS_CHANGED: "BUG_STATUS_CHANGED",
	BUG_PRIORITY_CHANGED: "BUG_PRIORITY_CHANGED",
	BUG_COMMENTED: "BUG_COMMENTED",
	BUG_MENTIONED: "BUG_MENTIONED",
	BUG_ATTACHMENT_ADDED: "BUG_ATTACHMENT_ADDED",

	// Project events
	PROJECT_INVITED: "PROJECT_INVITED",
	PROJECT_ROLE_CHANGED: "PROJECT_ROLE_CHANGED",
	PROJECT_MEMBER_JOINED: "PROJECT_MEMBER_JOINED",

	// Team events
	TEAM_INVITED: "TEAM_INVITED",
	TEAM_ROLE_CHANGED: "TEAM_ROLE_CHANGED",
	TEAM_MEMBER_JOINED: "TEAM_MEMBER_JOINED",

	// Gamification events
	GAMIFICATION_POINTS: "GAMIFICATION_POINTS",
	GAMIFICATION_ACHIEVEMENTS: "GAMIFICATION_ACHIEVEMENTS",
	GAMIFICATION_LEADERBOARD: "GAMIFICATION_LEADERBOARD",
} as const;

export type NotificationEventType =
	(typeof NOTIFICATION_EVENT_TYPES)[keyof typeof NOTIFICATION_EVENT_TYPES];

// ===== Utility Types =====

export interface NotificationGroup {
	date: string;
	notifications: Notification[];
}

export interface NotificationStats {
	total: number;
	unread: number;
	today: number;
	thisWeek: number;
}

// ===== Component Props Types =====

export interface NotificationBellProps {
	className?: string;
}

export interface NotificationDropdownProps {
	isOpen: boolean;
	onClose: () => void;
	anchorElement?: HTMLElement | null;
}

export interface NotificationItemProps {
	notification: Notification;
	onMarkAsRead?: (id: number) => void;
	onDismiss?: (id: number) => void;
	onClick?: (notification: Notification) => void;
}

export interface NotificationListProps {
	notifications: Notification[];
	loading?: boolean;
	onLoadMore?: () => void;
	hasMore?: boolean;
	onMarkAsRead?: (id: number) => void;
	onDismiss?: (id: number) => void;
	onNotificationClick?: (notification: Notification) => void;
}

export interface ToastProps {
	id: string;
	title: string;
	message: string;
	type: "info" | "success" | "warning" | "error";
	duration?: number;
	onClose: (id: string) => void;
}

export interface ToastContainerProps {
	position?: "top-right" | "top-left" | "bottom-right" | "bottom-left";
	maxToasts?: number;
}
