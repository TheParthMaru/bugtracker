/**
 * Notifications Page
 *
 * Dedicated page for viewing and managing all user notifications.
 * Provides comprehensive notification management with filtering, pagination, and bulk actions.
 *
 * Features:
 * - Paginated notification list
 * - Filter by read/unread status
 * - Filter by notification type
 * - Bulk mark as read/unread
 * - Search functionality
 * - Real-time updates via WebSocket
 * - Responsive design
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

import React, { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import {
	Bell,
	Settings,
	Search,
	Filter,
	CheckCheck,
	RotateCcw,
	Loader2,
	AlertCircle,
	ChevronLeft,
	ChevronRight,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { Alert } from "@/components/ui/alert";
import { cn } from "@/lib/utils";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { SimpleBreadcrumb } from "@/components/ui/breadcrumb";
import { notificationService } from "@/services/notificationService";
import { NotificationItem } from "@/components/notifications/NotificationItem";
import { useWebSocketNotifications } from "@/hooks/useWebSocketNotifications";
import { toastUtils } from "@/components/notifications/ToastManager";
import type {
	Notification,
	NotificationsResponse,
	NotificationFilters,
	NotificationStats,
} from "@/types/notification";
import { API_BASE_URL } from "@/config/constants";

export function NotificationsPage() {
	// State
	const [notifications, setNotifications] = useState<Notification[]>([]);
	const [totalCount, setTotalCount] = useState(0);
	const [unreadCount, setUnreadCount] = useState(0);
	const [isLoading, setIsLoading] = useState(true);
	const [isLoadingMore, setIsLoadingMore] = useState(false);
	const [isBulkProcessing, setBulkProcessing] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [searchQuery, setSearchQuery] = useState("");
	const [selectedTab, setSelectedTab] = useState<"all" | "unread">("all");
	const [selectedType, setSelectedType] = useState<string>("all");
	const [currentPage, setCurrentPage] = useState(0);
	const [hasMore, setHasMore] = useState(false);

	// Get current user ID for WebSocket
	const [userId, setUserId] = useState<string | null>(null);

	// Fetch user ID on mount
	useEffect(() => {
		const token = localStorage.getItem("bugtracker_token");
		if (token) {
			fetch(`${API_BASE_URL}/profile`, {
				headers: { Authorization: `Bearer ${token}` },
			})
				.then((res) => res.json())
				.then((user) => setUserId(user.id))
				.catch((err) => console.error("Failed to get user ID:", err));
		}
	}, []);

	// WebSocket connection for real-time updates
	useWebSocketNotifications({
		userId: userId || undefined,
		enabled: !!userId,
		onNewNotification: (notification) => {
			console.log("New notification received in page:", notification);
			// Add to the top of the list if it matches current filters
			if (shouldIncludeNotification(notification)) {
				setNotifications((prev) => [notification, ...prev]);
				setTotalCount((prev) => prev + 1);
				if (!notification.isRead) {
					setUnreadCount((prev) => prev + 1);
				}
			}
		},
		onUnreadCountUpdate: (count) => {
			setUnreadCount(count);
		},
	});

	// Check if notification should be included based on current filters
	const shouldIncludeNotification = (notification: Notification): boolean => {
		if (selectedTab === "unread" && notification.isRead) return false;
		if (selectedType !== "all" && notification.eventType !== selectedType)
			return false;
		if (
			searchQuery &&
			!notification.title.toLowerCase().includes(searchQuery.toLowerCase()) &&
			!notification.message.toLowerCase().includes(searchQuery.toLowerCase())
		)
			return false;
		return true;
	};

	// Build filters from current state
	const buildFilters = useCallback(
		(page: number = 0): NotificationFilters => {
			const filters: NotificationFilters = {
				page,
				size: 20,
			};

			if (selectedTab === "unread") {
				filters.unreadOnly = true;
			}

			if (selectedType !== "all") {
				filters.type = selectedType;
			}

			return filters;
		},
		[selectedTab, selectedType]
	);

	// Load notifications
	const loadNotifications = useCallback(
		async (page: number = 0, append: boolean = false) => {
			try {
				if (!append) {
					setIsLoading(true);
				} else {
					setIsLoadingMore(true);
				}
				setError(null);

				const filters = buildFilters(page);
				const response = await notificationService.getNotifications(filters);

				if (append) {
					setNotifications((prev) => [...prev, ...response.content]);
				} else {
					setNotifications(response.content);
				}

				setTotalCount(response.totalElements);
				setCurrentPage(page);
				setHasMore(!response.last);

				// Calculate unread count from current results
				const currentUnreadCount = response.content.filter(
					(n) => !n.isRead
				).length;
				if (!append) {
					setUnreadCount(currentUnreadCount);
				}

				console.log("Notifications loaded:", {
					page,
					count: response.numberOfElements,
					total: response.totalElements,
					hasMore: !response.last,
				});
			} catch (error) {
				console.error("❌ Failed to load notifications:", error);
				setError("Failed to load notifications");
			} finally {
				setIsLoading(false);
				setIsLoadingMore(false);
			}
		},
		[buildFilters]
	);

	// Load notifications when filters change
	useEffect(() => {
		if (userId) {
			loadNotifications(0, false);
		}
	}, [userId, selectedTab, selectedType, loadNotifications]);

	// Handle search with debounce
	useEffect(() => {
		if (!userId) return;

		const timeoutId = setTimeout(() => {
			loadNotifications(0, false);
		}, 500);

		return () => clearTimeout(timeoutId);
	}, [searchQuery, userId, loadNotifications]);

	// Load more notifications
	const loadMore = () => {
		if (hasMore && !isLoadingMore) {
			loadNotifications(currentPage + 1, true);
		}
	};

	// Handle mark as read
	const handleMarkAsRead = async (notificationId: number) => {
		try {
			await notificationService.markAsRead(notificationId);

			// Update local state
			setNotifications((prev) =>
				prev.map((notification) =>
					notification.notificationId === notificationId
						? {
								...notification,
								isRead: true,
								readAt: new Date().toISOString(),
						  }
						: notification
				)
			);

			// Update unread count
			setUnreadCount((prev) => Math.max(0, prev - 1));

			console.log("✅ Notification marked as read:", notificationId);
		} catch (error) {
			console.error("❌ Failed to mark notification as read:", error);
			toastUtils.error("Error", "Failed to mark notification as read");
		}
	};

	// Handle dismiss notification
	const handleDismiss = async (notificationId: number) => {
		try {
			await notificationService.dismissNotification(notificationId);

			// Remove from local state
			const wasUnread =
				notifications.find((n) => n.notificationId === notificationId)
					?.isRead === false;

			setNotifications((prev) =>
				prev.filter((n) => n.notificationId !== notificationId)
			);

			setTotalCount((prev) => prev - 1);
			if (wasUnread) {
				setUnreadCount((prev) => Math.max(0, prev - 1));
			}

			console.log("✅ Notification dismissed:", notificationId);
		} catch (error) {
			console.error("❌ Failed to dismiss notification:", error);
			toastUtils.error("Error", "Failed to dismiss notification");
		}
	};

	// Handle notification click
	const handleNotificationClick = (notification: Notification) => {
		// Mark as read if not already read
		if (!notification.isRead) {
			handleMarkAsRead(notification.notificationId);
		}

		// Navigate to related resource if available
		if (notification.projectSlug && notification.projectTicketNumber) {
			window.location.href = `/projects/${notification.projectSlug}/bugs/${notification.projectTicketNumber}`;
		} else if (notification.projectSlug) {
			window.location.href = `/projects/${notification.projectSlug}`;
		} else if (notification.relatedBugId && notification.relatedProjectId) {
			console.warn(
				"Using fallback bug URL format for notification:",
				notification.notificationId
			);
			window.location.href = `/projects/${notification.relatedProjectId}/bugs/${notification.relatedBugId}`;
		} else if (notification.relatedTeamId) {
			window.location.href = "/teams";
		}
	};

	// Handle bulk mark all as read
	const handleMarkAllAsRead = async () => {
		try {
			setBulkProcessing(true);
			const response = await notificationService.markAllAsRead();

			// Update local state
			setNotifications((prev) =>
				prev.map((notification) => ({
					...notification,
					isRead: true,
					readAt: new Date().toISOString(),
				}))
			);

			setUnreadCount(0);

			toastUtils.success(
				"All Read",
				`Marked ${response.updatedCount} notifications as read`
			);

			console.log(
				"✅ All notifications marked as read:",
				response.updatedCount
			);
		} catch (error) {
			console.error("❌ Failed to mark all notifications as read:", error);
			toastUtils.error("Error", "Failed to mark all notifications as read");
		} finally {
			setBulkProcessing(false);
		}
	};

	// Notification type options for filter
	const typeOptions = [
		{ value: "all", label: "All Types" },
		{ value: "BUG_ASSIGNED", label: "Bug Assigned" },
		{ value: "BUG_STATUS_CHANGED", label: "Bug Status Changed" },
		{ value: "BUG_COMMENTED", label: "Bug Commented" },

		{ value: "GAMIFICATION_POINTS", label: "Points Earned" },
	];

	if (isLoading) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-4">
					<SimpleBreadcrumb section="Notifications" />
					<div className="flex items-center justify-center min-h-96">
						<Loader2 className="h-8 w-8 animate-spin text-gray-400" />
						<span className="ml-3 text-gray-600">Loading notifications...</span>
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	return (
		<>
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-4">
					{/* Breadcrumb */}
					<SimpleBreadcrumb section="Notifications" />

					<div className="container mx-auto px-4 py-8 max-w-4xl">
						{/* Header */}
						<div className="flex items-center justify-between mb-8">
							<div className="flex items-center gap-3">
								<Bell className="h-6 w-6 text-blue-600" />
								<div>
									<h1 className="text-2xl font-bold text-gray-900">
										Notifications
									</h1>
									<p className="text-gray-600">
										{totalCount > 0 ? (
											<>
												{totalCount} total notifications
												{unreadCount > 0 && (
													<span className="text-blue-600">
														{" "}
														• {unreadCount} unread
													</span>
												)}
											</>
										) : (
											"No notifications"
										)}
									</p>
								</div>
							</div>

							<div className="flex items-center gap-2">
								{unreadCount > 0 && (
									<Button
										variant="outline"
										size="sm"
										onClick={handleMarkAllAsRead}
										disabled={isBulkProcessing}
									>
										{isBulkProcessing ? (
											<Loader2 className="h-4 w-4 animate-spin mr-2" />
										) : (
											<CheckCheck className="h-4 w-4 mr-2" />
										)}
										Mark All Read
									</Button>
								)}
								<Button variant="outline" size="sm" asChild>
									<Link to="/notifications/settings">
										<Settings className="h-4 w-4 mr-2" />
										Settings
									</Link>
								</Button>
							</div>
						</div>

						{/* Error Alert */}
						{error && (
							<Alert variant="destructive" className="mb-6">
								<AlertCircle className="h-4 w-4" />
								<div>
									<h3 className="font-medium">Error</h3>
									<p className="mt-1 text-sm">{error}</p>
									<Button
										variant="outline"
										size="sm"
										onClick={() => loadNotifications(0, false)}
										className="mt-2"
									>
										<RotateCcw className="h-3 w-3 mr-1" />
										Try Again
									</Button>
								</div>
							</Alert>
						)}

						{/* Filters */}
						<Card className="mb-6">
							<CardContent className="p-4">
								<div className="flex flex-col md:flex-row gap-4">
									{/* Search */}
									<div className="flex-1">
										<div className="relative">
											<Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
											<Input
												placeholder="Search notifications..."
												value={searchQuery}
												onChange={(e) => setSearchQuery(e.target.value)}
												className="pl-10"
											/>
										</div>
									</div>

									{/* Type Filter */}
									<div className="w-full md:w-48">
										<Select
											value={selectedType}
											onValueChange={setSelectedType}
										>
											<SelectTrigger>
												<SelectValue placeholder="Filter by type" />
											</SelectTrigger>
											<SelectContent>
												{typeOptions.map((option) => (
													<SelectItem key={option.value} value={option.value}>
														{option.label}
													</SelectItem>
												))}
											</SelectContent>
										</Select>
									</div>
								</div>
							</CardContent>
						</Card>

						{/* Tabs */}
						<Tabs
							value={selectedTab}
							onValueChange={(value: any) => setSelectedTab(value)}
							className="mb-6"
						>
							<TabsList>
								<TabsTrigger value="all">
									All Notifications
									{totalCount > 0 && (
										<Badge variant="secondary" className="ml-2">
											{totalCount}
										</Badge>
									)}
								</TabsTrigger>
								<TabsTrigger value="unread">
									Unread
									{unreadCount > 0 && (
										<Badge variant="destructive" className="ml-2">
											{unreadCount}
										</Badge>
									)}
								</TabsTrigger>
							</TabsList>

							<TabsContent value="all" className="mt-6">
								{/* All Notifications */}
								{notifications.length === 0 ? (
									<div className="text-center py-12">
										<Bell className="h-12 w-12 text-gray-300 mx-auto mb-4" />
										<h3 className="text-lg font-medium text-gray-900 mb-2">
											No notifications found
										</h3>
										<p className="text-gray-600">
											{searchQuery || selectedType !== "all"
												? "Try adjusting your filters"
												: "You're all caught up!"}
										</p>
									</div>
								) : (
									<Card>
										<div className="divide-y divide-gray-100">
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

										{/* Load More */}
										{hasMore && (
											<div className="p-4 text-center border-t border-gray-100">
												<Button
													variant="outline"
													onClick={loadMore}
													disabled={isLoadingMore}
												>
													{isLoadingMore ? (
														<Loader2 className="h-4 w-4 animate-spin mr-2" />
													) : (
														<ChevronRight className="h-4 w-4 mr-2" />
													)}
													Load More
												</Button>
											</div>
										)}
									</Card>
								)}
							</TabsContent>

							<TabsContent value="unread" className="mt-6">
								{/* Unread Notifications */}
								{notifications.filter((n) => !n.isRead).length === 0 ? (
									<div className="text-center py-12">
										<CheckCheck className="h-12 w-12 text-green-400 mx-auto mb-4" />
										<h3 className="text-lg font-medium text-gray-900 mb-2">
											All caught up!
										</h3>
										<p className="text-gray-600">
											You have no unread notifications
										</p>
									</div>
								) : (
									<Card>
										<div className="divide-y divide-gray-100">
											{notifications
												.filter((n) => !n.isRead)
												.map((notification) => (
													<NotificationItem
														key={notification.notificationId}
														notification={notification}
														onMarkAsRead={handleMarkAsRead}
														onDismiss={handleDismiss}
														onClick={handleNotificationClick}
													/>
												))}
										</div>
									</Card>
								)}
							</TabsContent>
						</Tabs>
					</div>
				</main>
				<Footer />
			</div>
		</>
	);
}
