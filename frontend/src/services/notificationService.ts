/**
 * Notification Service
 *
 * Service class for handling notification-related API calls.
 * Follows the same patterns as existing services (ProjectService, BugService).
 *
 * Features:
 * - RESTful API integration
 * - Error handling and logging
 * - Type-safe responses
 * - Consistent with existing service patterns
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

import API from "@/services/api";
import {
	Notification,
	NotificationsResponse,
	NotificationFilters,
	UnreadCount,
	NotificationPreferences,
	UpdateNotificationPreferencesRequest,
} from "@/types/notification";

export class NotificationService {
	private static instance: NotificationService;

	public static getInstance(): NotificationService {
		if (!NotificationService.instance) {
			NotificationService.instance = new NotificationService();
		}
		return NotificationService.instance;
	}

	/**
	 * Get user notifications with pagination and filtering
	 */
	async getNotifications(
		filters?: NotificationFilters
	): Promise<NotificationsResponse> {
		try {
			console.log("Fetching notifications with filters:", filters);

			const response = await API.get<NotificationsResponse>("/notifications", {
				params: filters,
			});

			console.log("Notifications fetched successfully:", {
				total: response.data.totalElements,
				page: response.data.number,
				size: response.data.numberOfElements,
			});

			return response.data;
		} catch (error) {
			console.error("Failed to fetch notifications:", error);
			throw this.handleError(error, "Failed to fetch notifications");
		}
	}

	/**
	 * Get unread notification count
	 */
	async getUnreadCount(): Promise<UnreadCount> {
		try {
			const response = await API.get<UnreadCount>(
				"/notifications/unread-count"
			);

			console.log("Unread count fetched:", response.data.count);
			return response.data;
		} catch (error) {
			console.error("Failed to fetch unread count:", error);
			throw this.handleError(error, "Failed to fetch unread count");
		}
	}

	/**
	 * Mark a notification as read
	 */
	async markAsRead(notificationId: number): Promise<void> {
		try {
			console.log("Marking notification as read:", notificationId);

			await API.put(`/notifications/${notificationId}/read`);

			console.log("Notification marked as read:", notificationId);
		} catch (error) {
			console.error("Failed to mark notification as read:", error);
			throw this.handleError(error, "Failed to mark notification as read");
		}
	}

	/**
	 * Mark all notifications as read
	 */
	async markAllAsRead(): Promise<{
		success: boolean;
		updatedCount: number;
		message: string;
	}> {
		try {
			console.log("Marking all notifications as read");

			const response = await API.put<{
				success: boolean;
				updatedCount: number;
				message: string;
			}>("/notifications/read-all");

			console.log(
				"All notifications marked as read:",
				response.data.updatedCount
			);
			return response.data;
		} catch (error) {
			console.error("Failed to mark all notifications as read:", error);
			throw this.handleError(error, "Failed to mark all notifications as read");
		}
	}

	/**
	 * Dismiss a notification
	 */
	async dismissNotification(notificationId: number): Promise<void> {
		try {
			console.log("Dismissing notification:", notificationId);

			await API.delete(`/notifications/${notificationId}`);

			console.log("Notification dismissed:", notificationId);
		} catch (error) {
			console.error("Failed to dismiss notification:", error);
			throw this.handleError(error, "Failed to dismiss notification");
		}
	}

	/**
	 * Get user notification preferences
	 */
	async getPreferences(): Promise<NotificationPreferences> {
		try {
			console.log("Fetching notification preferences");

			const response = await API.get<NotificationPreferences>(
				"/notification-preferences"
			);

			console.log("Notification preferences fetched successfully");
			return response.data;
		} catch (error) {
			console.error("Failed to fetch notification preferences:", error);
			throw this.handleError(error, "Failed to fetch notification preferences");
		}
	}

	/**
	 * Update user notification preferences
	 */
	async updatePreferences(
		preferences: UpdateNotificationPreferencesRequest
	): Promise<NotificationPreferences> {
		try {
			console.log("Updating notification preferences:", preferences);

			const response = await API.put<NotificationPreferences>(
				"/notification-preferences",
				preferences
			);

			console.log("Notification preferences updated successfully");
			return response.data;
		} catch (error) {
			console.error("Failed to update notification preferences:", error);
			throw this.handleError(
				error,
				"Failed to update notification preferences"
			);
		}
	}

	/**
	 * Reset notification preferences to defaults
	 */
	async resetPreferencesToDefaults(): Promise<{
		success: boolean;
		message: string;
		preferences: NotificationPreferences;
	}> {
		try {
			console.log("Resetting notification preferences to defaults");

			const response = await API.post<{
				success: boolean;
				message: string;
				preferences: NotificationPreferences;
			}>("/notification-preferences/reset");

			console.log("Notification preferences reset to defaults");
			return response.data;
		} catch (error) {
			console.error("Failed to reset notification preferences:", error);
			throw this.handleError(error, "Failed to reset notification preferences");
		}
	}

	/**
	 * Test endpoints (for development)
	 */
	async initializeTemplates(): Promise<string> {
		try {
			console.log("Initializing notification templates");

			const response = await API.post<string>(
				"/test/notifications/init-templates"
			);

			console.log("Notification templates initialized");
			return response.data;
		} catch (error) {
			console.error("Failed to initialize templates:", error);
			throw this.handleError(error, "Failed to initialize templates");
		}
	}

	async testBugAssignedNotification(data: {
		userId: string;
		bugId: number;
		projectId: string;
	}): Promise<Notification> {
		try {
			console.log("Testing bug assigned notification:", data);

			const response = await API.post<Notification>(
				"/test/notifications/bug-assigned",
				data
			);

			console.log("Bug assigned notification test completed");
			return response.data;
		} catch (error) {
			console.error("Failed to test bug assigned notification:", error);
			throw this.handleError(error, "Failed to test bug assigned notification");
		}
	}

	async testEmail(data: {
		toEmail: string;
		subject: string;
		content: string;
	}): Promise<string> {
		try {
			console.log("Testing email notification:", data);

			const response = await API.post<string>(
				"/test/notifications/test-email",
				data
			);

			console.log("Email notification test completed");
			return response.data;
		} catch (error) {
			console.error("Failed to test email notification:", error);
			throw this.handleError(error, "Failed to test email notification");
		}
	}

	/**
	 * Handle API errors consistently with other services
	 */
	private handleError(error: any, defaultMessage: string): Error {
		if (error.response) {
			// Server responded with error status
			const status = error.response.status;
			const message =
				error.response.data?.message ||
				error.response.data?.error ||
				defaultMessage;

			console.error(`API Error ${status}:`, message);
			return new Error(`${message} (${status})`);
		} else if (error.request) {
			// Network error
			console.error("Network Error:", error.request);
			return new Error("Network error - please check your connection");
		} else {
			// Other error
			console.error("Error:", error.message);
			return new Error(error.message || defaultMessage);
		}
	}
}

// Export singleton instance
export const notificationService = NotificationService.getInstance();
