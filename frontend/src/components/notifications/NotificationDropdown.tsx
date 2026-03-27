/**
 * Notification Dropdown Component
 *
 * Dropdown panel that displays a list of notifications with actions.
 * Provides quick access to recent notifications and management options.
 *
 * Features:
 * - Notification list with pagination
 * - Mark as read/unread actions
 * - Dismiss notifications
 * - View all notifications link
 * - Real-time updates
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

import React, { useEffect, useState, useRef } from "react";
import { Link } from "react-router-dom";
import {
	Check,
	CheckCheck,
	Settings,
	ExternalLink,
	Loader2,
	Bell,
	AlertCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { notificationService } from "@/services/notificationService";
import { NotificationItem } from "./NotificationItem";
import type {
	Notification,
	NotificationsResponse,
	NotificationDropdownProps,
} from "@/types/notification";

interface NotificationDropdownExtendedProps extends NotificationDropdownProps {
	onUnreadCountChange?: (newCount: number) => void;
	refreshTrigger?: number; // Add trigger to refresh notifications
}

export function NotificationDropdown({
	isOpen,
	onClose,
	anchorElement,
	onUnreadCountChange,
	refreshTrigger,
}: NotificationDropdownExtendedProps) {
	// State
	const [notifications, setNotifications] = useState<Notification[]>([]);
	const [isLoading, setIsLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [hasMore, setHasMore] = useState(false);
	const [totalCount, setTotalCount] = useState(0);
	const [isMarkingAllRead, setIsMarkingAllRead] = useState(false);

	// Refs
	const dropdownRef = useRef<HTMLDivElement>(null);

	// Refresh notifications when refreshTrigger changes (real-time updates)
	useEffect(() => {
		if (isOpen && refreshTrigger) {
			fetchNotifications();
		}
	}, [refreshTrigger]);

	// Fetch notifications when dropdown opens
	useEffect(() => {
		if (isOpen) {
			fetchNotifications();
		}
	}, [isOpen]);

	// Handle click outside to close dropdown
	useEffect(() => {
		if (!isOpen) return;

		const handleClickOutside = (event: MouseEvent) => {
			const target = event.target as Node;

			// Don't close if clicking on the bell button or dropdown content
			if (
				anchorElement?.contains(target) ||
				dropdownRef.current?.contains(target)
			) {
				return;
			}

			onClose();
		};

		document.addEventListener("mousedown", handleClickOutside);
		return () => document.removeEventListener("mousedown", handleClickOutside);
	}, [isOpen, anchorElement, onClose]);

	// Fetch notifications
	const fetchNotifications = async () => {
		try {
			setIsLoading(true);
			setError(null);

			const response = await notificationService.getNotifications({
				page: 0,
				size: 10, // Show latest 10 notifications in dropdown
				unreadOnly: true, // Only show unread notifications in dropdown
			});

			setNotifications(response.content);
			setTotalCount(response.totalElements);
			setHasMore(response.totalElements > response.numberOfElements);

			console.log("Unread notifications fetched for dropdown:", {
				count: response.numberOfElements,
				total: response.totalElements,
				hasMore: response.totalElements > response.numberOfElements,
			});
		} catch (error) {
			console.error("Failed to fetch notifications:", error);
			setError("Failed to load notifications");
		} finally {
			setIsLoading(false);
		}
	};

	// Handle mark as read
	const handleMarkAsRead = async (notificationId: number) => {
		try {
			await notificationService.markAsRead(notificationId);

			// Remove notification from dropdown after marking as read (better UX)
			setNotifications((prev) =>
				prev.filter(
					(notification) => notification.notificationId !== notificationId
				)
			);

			// Update unread count (subtract 1 since we removed the notification)
			const currentUnreadCount = notifications.filter((n) => !n.isRead).length;
			const newUnreadCount = Math.max(0, currentUnreadCount - 1);
			onUnreadCountChange?.(newUnreadCount);

			console.log("Notification marked as read:", notificationId);
		} catch (error) {
			console.error("Failed to mark notification as read:", error);
		}
	};

	// Handle dismiss notification
	const handleDismiss = async (notificationId: number) => {
		try {
			await notificationService.dismissNotification(notificationId);

			// Remove from local state
			setNotifications((prev) =>
				prev.filter((n) => n.notificationId !== notificationId)
			);

			// Update counts
			setTotalCount((prev) => prev - 1);
			const wasUnread =
				notifications.find((n) => n.notificationId === notificationId)
					?.isRead === false;
			if (wasUnread) {
				const newUnreadCount = notifications.filter(
					(n) => !n.isRead && n.notificationId !== notificationId
				).length;
				onUnreadCountChange?.(newUnreadCount);
			}

			console.log("Notification dismissed:", notificationId);
		} catch (error) {
			console.error("Failed to dismiss notification:", error);
		}
	};

	// Handle mark all as read
	const handleMarkAllAsRead = async () => {
		try {
			setIsMarkingAllRead(true);
			const response = await notificationService.markAllAsRead();

			// Clear all notifications from dropdown since they're now read
			setNotifications([]);

			// Update counts to reflect empty state
			setTotalCount(0);
			onUnreadCountChange?.(0);

			console.log(
				"All notifications marked as read and removed from dropdown:",
				response.updatedCount
			);
		} catch (error) {
			console.error("Failed to mark all notifications as read:", error);
		} finally {
			setIsMarkingAllRead(false);
		}
	};

	// Handle notification click
	const handleNotificationClick = (notification: Notification) => {
		console.log("🔔 Notification clicked:", {
			notificationId: notification.notificationId,
			eventType: notification.eventType,
			projectSlug: notification.projectSlug,
			relatedProjectId: notification.relatedProjectId,
			message: notification.message,
			hasNewFields: !!(
				notification.projectSlug && notification.projectTicketNumber
			),
		});

		// Mark as read if not already read
		if (!notification.isRead) {
			handleMarkAsRead(notification.notificationId);
		}

		// Navigate to related resource if available
		if (notification.projectSlug && notification.projectTicketNumber) {
			const newUrl = `/projects/${notification.projectSlug}/bugs/${notification.projectTicketNumber}`;
			console.log("Navigating to bug:", newUrl);
			window.location.href = newUrl;
		} else if (notification.projectSlug) {
			const projectUrl = `/projects/${notification.projectSlug}`;
			console.log("✅ Using projectSlug for navigation:", {
				projectSlug: notification.projectSlug,
				url: projectUrl,
				eventType: notification.eventType,
			});
			window.location.href = projectUrl;
		} else if (notification.relatedBugId && notification.relatedProjectId) {
			// Fallback for older notifications without new fields
			const fallbackUrl = `/projects/${notification.relatedProjectId}/bugs/${notification.relatedBugId}`;
			console.warn(
				"Using fallback bug URL format for notification:",
				notification.notificationId,
				"URL:",
				fallbackUrl
			);
			window.location.href = fallbackUrl;
		} else if (notification.relatedTeamId) {
			window.location.href = "/teams";
		} else {
			console.warn("⚠️ No navigation target found for notification:", {
				notificationId: notification.notificationId,
				eventType: notification.eventType,
				projectSlug: notification.projectSlug,
				relatedProjectId: notification.relatedProjectId,
				relatedTeamId: notification.relatedTeamId,
			});
		}

		// Close dropdown
		onClose();
	};

	// Calculate position
	const getDropdownStyle = (): React.CSSProperties => {
		if (!anchorElement) return {};

		const rect = anchorElement.getBoundingClientRect();
		const dropdownWidth = 420;
		const dropdownHeight = 500;

		// Position to the right of the bell, but adjust if it would go off-screen
		let left = rect.right - dropdownWidth + 20;
		let top = rect.bottom + 8;

		// Adjust horizontal position if off-screen
		if (left < 10) {
			left = 10;
		} else if (left + dropdownWidth > window.innerWidth - 10) {
			left = window.innerWidth - dropdownWidth - 10;
		}

		// Adjust vertical position if off-screen
		if (top + dropdownHeight > window.innerHeight - 10) {
			top = rect.top - dropdownHeight - 8;
		}

		return {
			position: "fixed",
			left: `${left}px`,
			top: `${top}px`,
			zIndex: 50,
		};
	};

	if (!isOpen) return null;

	const unreadCount = notifications.filter((n) => !n.isRead).length;

	return (
		<>
			<div style={getDropdownStyle()}>
				<Card
					ref={dropdownRef}
					className="w-[420px] max-h-[500px] flex flex-col shadow-lg border border-gray-200 bg-white"
				>
					{/* Header */}
					<div className="flex items-center justify-between p-2 pt-0 border-b border-gray-100">
						<div className="flex items-center gap-2">
							<Bell className="h-4 w-4 text-gray-600" />
							<h3 className="font-semibold text-gray-900">Notifications</h3>
							{totalCount > 0 && (
								<Badge variant="secondary" className="text-xs">
									{totalCount} unread
								</Badge>
							)}
						</div>

						<div className="flex items-center gap-1">
							{notifications.length > 0 && (
								<Button
									variant="ghost"
									size="sm"
									onClick={handleMarkAllAsRead}
									disabled={isMarkingAllRead}
									className="text-xs"
								>
									{isMarkingAllRead ? (
										<Loader2 className="h-3 w-3 animate-spin mr-1" />
									) : (
										<CheckCheck className="h-3 w-3 mr-1" />
									)}
									Mark all read
								</Button>
							)}

							<Button variant="ghost" size="sm" asChild className="text-xs">
								<Link to="/notifications/settings">
									<Settings className="h-3 w-3 mr-1" />
									Settings
								</Link>
							</Button>
						</div>
					</div>

					{/* Content */}
					<div className="flex-1 overflow-hidden">
						{isLoading ? (
							<div className="flex items-center justify-center p-8">
								<Loader2 className="h-6 w-6 animate-spin text-gray-400" />
								<span className="ml-2 text-sm text-gray-500">
									Loading notifications...
								</span>
							</div>
						) : error ? (
							<div className="flex items-center justify-center p-6">
								<AlertCircle className="h-6 w-6 text-red-400" />
								<span className="ml-2 text-sm text-red-600">{error}</span>
							</div>
						) : notifications.length === 0 ? (
							<div className="flex flex-col items-center justify-center p-6 text-center">
								<Bell className="h-12 w-12 text-gray-300 mb-2" />
								<p className="text-sm text-gray-500 mb-1">
									No unread notifications
								</p>
								<p className="text-xs text-gray-400">You're all caught up!</p>
							</div>
						) : (
							<div className="max-h-80 overflow-y-auto">
								{notifications.map((notification) => (
									<NotificationItem
										key={notification.notificationId}
										notification={notification}
										onMarkAsRead={handleMarkAsRead}
										onDismiss={handleDismiss}
										onClick={handleNotificationClick}
									/>
								))}
							</div>
						)}
					</div>

					{/* Footer */}
					{(notifications.length > 0 || hasMore) && (
						<div className="border-t border-gray-100 p-1 pb-0">
							<Button
								variant="secondary"
								size="sm"
								asChild
								className="w-full text-xs"
							>
								<Link to="/notifications">
									View all notifications
									<ExternalLink className="h-3 w-3 ml-1" />
								</Link>
							</Button>
						</div>
					)}
				</Card>
			</div>
		</>
	);
}
