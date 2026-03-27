/**
 * Notification Item Component
 *
 * Individual notification item with actions and status indicators.
 * Displays notification content, timestamp, and provides interaction options.
 *
 * Features:
 * - Rich notification content display
 * - Read/unread status indicators
 * - Quick actions (mark as read, dismiss)
 * - Event type icons and styling
 * - Relative timestamps
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

import React from "react";
import { formatDistanceToNow } from "date-fns";
import {
	Bug,
	Users,
	FolderOpen,
	Trophy,
	MessageCircle,
	AlertCircle,
	Check,
	X,
	Dot,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
	NOTIFICATION_EVENT_TYPES,
	type NotificationItemProps,
} from "@/types/notification";

export function NotificationItem({
	notification,
	onMarkAsRead,
	onDismiss,
	onClick,
}: NotificationItemProps) {
	// Get icon for notification type
	const getNotificationIcon = (eventType: string) => {
		switch (eventType) {
			case NOTIFICATION_EVENT_TYPES.BUG_ASSIGNED:
			case NOTIFICATION_EVENT_TYPES.BUG_STATUS_CHANGED:
			case NOTIFICATION_EVENT_TYPES.BUG_PRIORITY_CHANGED:
			case NOTIFICATION_EVENT_TYPES.BUG_ATTACHMENT_ADDED:
				return <Bug className="h-4 w-4" />;

			case NOTIFICATION_EVENT_TYPES.BUG_COMMENTED:
			case NOTIFICATION_EVENT_TYPES.BUG_MENTIONED:
				return <MessageCircle className="h-4 w-4" />;

			case NOTIFICATION_EVENT_TYPES.PROJECT_ROLE_CHANGED:
			case NOTIFICATION_EVENT_TYPES.PROJECT_MEMBER_JOINED:
				return <FolderOpen className="h-4 w-4" />;

			case NOTIFICATION_EVENT_TYPES.TEAM_ROLE_CHANGED:
			case NOTIFICATION_EVENT_TYPES.TEAM_MEMBER_JOINED:
				return <Users className="h-4 w-4" />;

			case NOTIFICATION_EVENT_TYPES.GAMIFICATION_POINTS:
			case NOTIFICATION_EVENT_TYPES.GAMIFICATION_ACHIEVEMENTS:
			case NOTIFICATION_EVENT_TYPES.GAMIFICATION_LEADERBOARD:
				return <Trophy className="h-4 w-4" />;

			default:
				return <AlertCircle className="h-4 w-4" />;
		}
	};

	// Get color scheme for notification type
	const getNotificationColors = (eventType: string) => {
		switch (eventType) {
			case NOTIFICATION_EVENT_TYPES.BUG_ASSIGNED:
			case NOTIFICATION_EVENT_TYPES.BUG_STATUS_CHANGED:
			case NOTIFICATION_EVENT_TYPES.BUG_PRIORITY_CHANGED:
			case NOTIFICATION_EVENT_TYPES.BUG_ATTACHMENT_ADDED:
				return {
					icon: "text-red-500",
					bg: "bg-red-50",
					border: "border-red-100",
				};

			case NOTIFICATION_EVENT_TYPES.BUG_COMMENTED:
			case NOTIFICATION_EVENT_TYPES.BUG_MENTIONED:
				return {
					icon: "text-purple-500",
					bg: "bg-purple-50",
					border: "border-purple-100",
				};

			case NOTIFICATION_EVENT_TYPES.PROJECT_ROLE_CHANGED:
			case NOTIFICATION_EVENT_TYPES.PROJECT_MEMBER_JOINED:
				return {
					icon: "text-blue-500",
					bg: "bg-blue-50",
					border: "border-blue-100",
				};

			case NOTIFICATION_EVENT_TYPES.TEAM_ROLE_CHANGED:
			case NOTIFICATION_EVENT_TYPES.TEAM_MEMBER_JOINED:
				return {
					icon: "text-green-500",
					bg: "bg-green-50",
					border: "border-green-100",
				};

			case NOTIFICATION_EVENT_TYPES.GAMIFICATION_POINTS:
			case NOTIFICATION_EVENT_TYPES.GAMIFICATION_ACHIEVEMENTS:
			case NOTIFICATION_EVENT_TYPES.GAMIFICATION_LEADERBOARD:
				return {
					icon: "text-yellow-500",
					bg: "bg-yellow-50",
					border: "border-yellow-100",
				};

			default:
				return {
					icon: "text-gray-500",
					bg: "bg-gray-50",
					border: "border-gray-100",
				};
		}
	};

	// Format relative time
	const formatRelativeTime = (dateString: string) => {
		try {
			const date = new Date(dateString);
			return formatDistanceToNow(date, { addSuffix: true });
		} catch (error) {
			return "Unknown time";
		}
	};

	// Handle mark as read click
	const handleMarkAsRead = (e: React.MouseEvent) => {
		e.stopPropagation();
		e.preventDefault();
		console.log("Mark as read clicked:", notification.notificationId);
		onMarkAsRead?.(notification.notificationId);
	};

	// Handle dismiss click
	const handleDismiss = (e: React.MouseEvent) => {
		e.stopPropagation();
		e.preventDefault();
		console.log("Dismiss clicked:", notification.notificationId);
		onDismiss?.(notification.notificationId);
	};

	// Handle item click
	const handleClick = () => {
		onClick?.(notification);
	};

	const colors = getNotificationColors(notification.eventType);
	const icon = getNotificationIcon(notification.eventType);
	const relativeTime = formatRelativeTime(notification.createdAt);

	return (
		<div
			className={cn(
				"group relative flex items-start gap-2 p-2 border-b border-gray-100 hover:bg-gray-50 cursor-pointer transition-colors",
				!notification.isRead && "bg-blue-50/30"
			)}
			onClick={handleClick}
		>
			{/* Icon */}
			<div
				className={cn(
					"flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center border",
					colors.bg,
					colors.border
				)}
			>
				<div className={colors.icon}>{icon}</div>
			</div>

			{/* Content */}
			<div className="flex-1 min-w-0">
				<div className="flex items-start justify-between gap-2">
					<div className="flex-1 min-w-0 max-w-full">
						<h4
							className={cn(
								"text-sm font-medium text-gray-900 line-clamp-1 break-words overflow-wrap-anywhere",
								!notification.isRead && "font-semibold"
							)}
						>
							{notification.title}
						</h4>
						<p className="text-sm text-gray-600 line-clamp-2 mt-1 break-words overflow-wrap-anywhere">
							{notification.message}
						</p>
					</div>
				</div>

				{/* Footer */}
				<div className="flex items-center justify-between mt-2">
					<span className="text-xs text-gray-500">{relativeTime}</span>

					{/* Read/Unread indicator and Actions */}
					<div className="flex items-center gap-3 relative z-10">
						{/* Read/Unread indicator - always visible */}
						{notification.isRead ? (
							<Check className="h-4 w-4 text-green-500 flex-shrink-0" />
						) : (
							<Dot className="h-5 w-5 text-blue-500 flex-shrink-0" />
						)}

						{/* Action buttons - always visible */}
						<div className="flex items-center gap-1">
							{/* Mark as read button - only show for unread notifications since read ones are removed */}
							{!notification.isRead && (
								<Button
									variant="ghost"
									size="sm"
									onClick={handleMarkAsRead}
									className="h-7 px-3 text-xs relative z-20 border-2 border-green-300 mr-2 hover:bg-green-100"
									title="Mark as read"
									style={{
										color: "#16a34a",
										pointerEvents: "auto",
									}}
								>
									<Check className="h-3 w-3" style={{ color: "#16a34a" }} />
								</Button>
							)}

							<Button
								variant="ghost"
								size="sm"
								onClick={handleDismiss}
								className="h-7 px-3 text-xs hover:bg-red-100 relative z-20 border-2 border-red-300 ml-2"
								title="Dismiss"
								style={{ color: "#dc2626", pointerEvents: "auto" }} // red-600
							>
								<X className="h-3 w-3" style={{ color: "#dc2626" }} />
							</Button>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
}
