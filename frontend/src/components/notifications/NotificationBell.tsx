/**
 * Notification Bell Component
 *
 * A bell icon with unread count badge that serves as the main entry point
 * to the notification system. Integrates with the existing navbar.
 *
 * Features:
 * - Real-time unread count updates
 * - Visual notification indicator
 * - Dropdown trigger for notification list
 * - Consistent with existing UI patterns
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

import React, { useState, useEffect, useRef } from "react";
import { Bell } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { notificationService } from "@/services/notificationService";
import { NotificationDropdown } from "./NotificationDropdown";
import { useWebSocketNotifications } from "@/hooks/useWebSocketNotifications";
import type { NotificationBellProps } from "@/types/notification";
import { API_BASE_URL } from "@/config/constants";

export function NotificationBell({ className }: NotificationBellProps) {
	// State
	const [unreadCount, setUnreadCount] = useState(0);
	const [isDropdownOpen, setIsDropdownOpen] = useState(false);
	const [isLoading, setIsLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [refreshTrigger, setRefreshTrigger] = useState(0);

	// Refs
	const bellRef = useRef<HTMLButtonElement>(null);

	// Get current user ID (you might need to adjust this based on your auth implementation)
	const [userId, setUserId] = useState<string | null>(null);

	// Fetch user ID on mount
	useEffect(() => {
		const token = localStorage.getItem("bugtracker_token");
		if (token) {
			// You might need to decode the JWT or fetch user profile
			// For now, I'll assume you have a way to get the current user ID
			// This should match your existing auth patterns
			fetch(`${API_BASE_URL}/profile`, {
				headers: { Authorization: `Bearer ${token}` },
			})
				.then((res) => res.json())
				.then((user) => setUserId(user.id))
				.catch((err) => console.error("Failed to get user ID:", err));
		}
	}, []);

	// WebSocket connection for real-time updates
	const {
		isConnected,
		isConnecting,
		error: wsError,
	} = useWebSocketNotifications({
		userId: userId || undefined,
		enabled: !!userId,
		onUnreadCountUpdate: (count) => {
			console.log("BELL: Unread count updated via WebSocket:", count);
			setUnreadCount(count);
		},
		onNewNotification: (notification) => {
			console.log("BELL: New notification received:", notification);
			// Trigger dropdown refresh for real-time updates
			setRefreshTrigger((prev) => prev + 1);
			// Also manually increment count as backup
			setUnreadCount((prev) => prev + 1);
		},
	});

	// Debug WebSocket status
	useEffect(() => {
		console.log("NotificationBell: WebSocket Status:", {
			userId,
			isConnected,
			isConnecting,
			wsError,
			enabled: !!userId,
		});
	}, [userId, isConnected, isConnecting, wsError]);

	// Always poll for updates as backup (regardless of WebSocket status)
	useEffect(() => {
		if (!userId) return;

		console.log(
			"NotificationBell: Starting background polling for unread count"
		);

		const pollForUpdates = async () => {
			try {
				const response = await notificationService.getUnreadCount();
				const currentCount = response.count;

				// Only update if count actually changed to avoid unnecessary re-renders
				setUnreadCount((prevCount) => {
					if (prevCount !== currentCount) {
						console.log(
							"NotificationBell: Count changed from",
							prevCount,
							"to",
							currentCount
						);
						return currentCount;
					}
					return prevCount;
				});
			} catch (error) {
				console.error("NotificationBell: Failed to poll unread count:", error);
			}
		};

		// Poll every 15 seconds regardless of WebSocket status
		const interval = setInterval(pollForUpdates, 15000);
		return () => clearInterval(interval);
	}, [userId]);

	// Fetch initial unread count
	useEffect(() => {
		if (!userId) return;

		const fetchUnreadCount = async () => {
			try {
				setIsLoading(true);
				setError(null);
				const response = await notificationService.getUnreadCount();
				setUnreadCount(response.count);
				console.log("Initial unread count fetched:", response.count);
			} catch (error) {
				console.error("Failed to fetch unread count:", error);
				setError("Failed to load notifications");
			} finally {
				setIsLoading(false);
			}
		};

		fetchUnreadCount();
	}, [userId]);

	// Handle bell click
	const handleBellClick = async () => {
		setIsDropdownOpen(!isDropdownOpen);

		// Refresh unread count when opening dropdown
		if (!isDropdownOpen && userId) {
			try {
				const response = await notificationService.getUnreadCount();
				console.log(
					"NotificationBell: Refreshed unread count on open:",
					response.count
				);
				setUnreadCount(response.count);
			} catch (error) {
				console.error(
					"NotificationBell: Failed to refresh unread count:",
					error
				);
			}
		}
	};

	// Handle dropdown close
	const handleDropdownClose = () => {
		setIsDropdownOpen(false);
	};

	// Handle notification read (callback from dropdown)
	const handleNotificationRead = (newUnreadCount: number) => {
		setUnreadCount(newUnreadCount);
	};

	// Don't render if no user
	if (!userId) {
		return null;
	}

	return (
		<div className="relative">
			<Button
				ref={bellRef}
				variant="ghost"
				size="icon"
				className={cn(
					"relative h-11 w-11 hover:bg-gray-100 focus:ring-1 focus:ring-blue-500 focus:ring-offset-1",
					className
				)}
				onClick={handleBellClick}
				aria-label={`Notifications ${
					unreadCount > 0 ? `(${unreadCount} unread)` : ""
				}`}
				disabled={isLoading}
			>
				<Bell
					className={cn(
						"h-10 w-10 transition-colors",
						unreadCount > 0 ? "text-blue-600" : "text-gray-600",
						isLoading && "animate-pulse"
					)}
				/>

				{/* Unread count badge */}
				{unreadCount > 0 && (
					<Badge
						variant="destructive"
						className="absolute -top-1 -right-1 h-5 min-w-5 px-1 text-xs font-medium flex items-center justify-center"
					>
						{unreadCount > 99 ? "99+" : unreadCount}
					</Badge>
				)}

				{/* Connection status indicator */}
				{!isConnected && !isLoading && (
					<div
						className="absolute -top-1 -left-1 h-2 w-2 rounded-full bg-yellow-400 border border-white"
						title="Notifications may be delayed - reconnecting..."
					/>
				)}

				{/* Error indicator */}
				{error && (
					<div
						className="absolute -top-1 -left-1 h-2 w-2 rounded-full bg-red-400 border border-white"
						title={error}
					/>
				)}
			</Button>

			{/* Notification Dropdown */}
			<NotificationDropdown
				isOpen={isDropdownOpen}
				onClose={handleDropdownClose}
				anchorElement={bellRef.current}
				onUnreadCountChange={handleNotificationRead}
				refreshTrigger={refreshTrigger}
			/>
		</div>
	);
}
