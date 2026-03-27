/**
 * Notifications Components Export
 *
 * Central export point for all notification-related components.
 * Provides clean imports throughout the application.
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

export { NotificationBell } from "./NotificationBell";
export { NotificationDropdown } from "./NotificationDropdown";
export { NotificationItem } from "./NotificationItem";

// Re-export types for convenience
export type {
	NotificationBellProps,
	NotificationDropdownProps,
	NotificationItemProps,
} from "@/types/notification";
