// API — set VITE_API_BASE_URL at build time for production (e.g. https://api.example.com/api/bugtracker/v1)
const DEFAULT_API_BASE_URL = "http://localhost:8080/api/bugtracker/v1";

export const API_BASE_URL =
	import.meta.env.VITE_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL;

/**
 * Native WebSocket endpoint for notifications.
 * Override with VITE_WS_URL if needed; otherwise derived from API_BASE_URL (same host, wss when API is https).
 */
function resolveWsNotificationsUrl(): string {
	const explicit = import.meta.env.VITE_WS_URL?.trim();
	if (explicit) {
		return explicit;
	}
	try {
		const u = new URL(API_BASE_URL);
		const wsProtocol = u.protocol === "https:" ? "wss:" : "ws:";
		return `${wsProtocol}//${u.host}/ws-notifications-native`;
	} catch {
		return "ws://localhost:8080/ws-notifications-native";
	}
}

export const WS_NOTIFICATIONS_URL = resolveWsNotificationsUrl();

// Application Configuration
export const APP_NAME = "BugTracker";
export const APP_VERSION = "1.0.0";

// Pagination Defaults
export const DEFAULT_PAGE_SIZE = 20;
export const MAX_PAGE_SIZE = 100;

// File Upload Limits
export const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
export const ALLOWED_FILE_TYPES = [
	"image/jpeg",
	"image/png",
	"image/gif",
	"application/pdf",
	"text/plain",
	"application/msword",
	"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
];

// Validation Limits
export const MAX_PROJECT_NAME_LENGTH = 100;
export const MAX_PROJECT_DESCRIPTION_LENGTH = 2000;
export const MAX_BUG_TITLE_LENGTH = 200;
export const MAX_BUG_DESCRIPTION_LENGTH = 5000;
export const MAX_COMMENT_LENGTH = 2000;

// Cache Configuration
export const CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
export const CACHE_PREFIX = "bugtracker_";

// Analytics Configuration
export const ANALYTICS_CACHE_DURATION = 10 * 60 * 1000; // 10 minutes
export const DEFAULT_ANALYTICS_PERIOD = 30; // days

// Error Messages
export const ERROR_MESSAGES = {
	NETWORK_ERROR: "Network error. Please check your connection.",
	UNAUTHORIZED: "You are not authorized to perform this action.",
	FORBIDDEN: "Access denied.",
	NOT_FOUND: "Resource not found.",
	VALIDATION_ERROR: "Please check your input and try again.",
	SERVER_ERROR: "Server error. Please try again later.",
	UNKNOWN_ERROR: "An unexpected error occurred.",
} as const;

// Success Messages
export const SUCCESS_MESSAGES = {
	PROJECT_CREATED: "Project created successfully.",
	PROJECT_UPDATED: "Project updated successfully.",
	PROJECT_DELETED: "Project deleted successfully.",
	BUG_CREATED: "Bug created successfully.",
	BUG_UPDATED: "Bug updated successfully.",
	BUG_DELETED: "Bug deleted successfully.",
	TEAM_CREATED: "Team created successfully.",
	TEAM_UPDATED: "Team updated successfully.",
	TEAM_DELETED: "Team deleted successfully.",
	MEMBER_ADDED: "Member added successfully.",
	MEMBER_REMOVED: "Member removed successfully.",
	COMMENT_ADDED: "Comment added successfully.",
	COMMENT_UPDATED: "Comment updated successfully.",
	COMMENT_DELETED: "Comment deleted successfully.",
	FILE_UPLOADED: "File uploaded successfully.",
	FILE_DELETED: "File deleted successfully.",
} as const;
