/**
 * WebSocket Notifications Hook
 *
 * Custom React hook for managing WebSocket connections and real-time notifications.
 * Handles connection lifecycle, message processing, and state management.
 *
 * Features:
 * - Automatic connection management
 * - Message type routing
 * - Connection state tracking
 * - Error handling and reconnection
 * - Integration with notification state
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

import { useEffect, useRef, useState, useCallback } from "react";
import { Client, StompConfig, IMessage } from "@stomp/stompjs";
import {
	Notification,
	NotificationMessage,
	CountMessage,
	ToastMessage,
	InAppMessage,
	WebSocketMessage,
} from "@/types/notification";
import { WS_NOTIFICATIONS_URL } from "@/config/constants";

interface UseWebSocketNotificationsOptions {
	userId?: string;
	enabled?: boolean;
	onNewNotification?: (notification: Notification) => void;
	onUnreadCountUpdate?: (count: number) => void;
	onToastMessage?: (toast: {
		title: string;
		message: string;
		type: "info" | "success" | "warning" | "error";
	}) => void;
	onInAppMessage?: (content: string, notificationId: number) => void;
}

interface UseWebSocketNotificationsReturn {
	isConnected: boolean;
	isConnecting: boolean;
	error: string | null;
	connectionAttempts: number;
	connect: () => void;
	disconnect: () => void;
	reconnect: () => void;
}

export function useWebSocketNotifications({
	userId,
	enabled = true,
	onNewNotification,
	onUnreadCountUpdate,
	onToastMessage,
	onInAppMessage,
}: UseWebSocketNotificationsOptions): UseWebSocketNotificationsReturn {
	// Connection state
	const [isConnected, setIsConnected] = useState(false);
	const [isConnecting, setIsConnecting] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [connectionAttempts, setConnectionAttempts] = useState(0);

	// Refs
	const clientRef = useRef<Client | null>(null);
	const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
	const subscriptionsRef = useRef<any[]>([]);

	const WS_URL = WS_NOTIFICATIONS_URL;
	const MAX_RECONNECT_ATTEMPTS = 5;
	const RECONNECT_DELAY = 3000;

	/**
	 * Handle incoming WebSocket messages
	 */
	const handleMessage = useCallback(
		(message: IMessage) => {
			try {
				const data: WebSocketMessage = JSON.parse(message.body);

				// console.log(
				// 	`useWebSocketNotifications.handleMessage - Message received for userId: ${userId}:`,
				// 	{
				// 		type: data.type,
				// 		timestamp: data.timestamp,
				// 		destination: message.headers.destination,
				// 	}
				// );

				switch (data.type) {
					case "notification": {
						const notificationMessage = data as NotificationMessage;
						onNewNotification?.(notificationMessage.data);
						break;
					}

					case "count": {
						const countMessage = data as CountMessage;
						onUnreadCountUpdate?.(countMessage.data.count);
						break;
					}

					case "toast": {
						const toastMessage = data as ToastMessage;
						onToastMessage?.(toastMessage.data);
						break;
					}

					case "in-app": {
						const inAppMessage = data as InAppMessage;
						onInAppMessage?.(
							inAppMessage.data.content,
							inAppMessage.data.notificationId
						);
						break;
					}

					default:
						console.warn("Unknown WebSocket message type:", data.type);
				}
			} catch (error) {
				console.error("WebSocket: Failed to parse message:", error);
			}
		},
		[onNewNotification, onUnreadCountUpdate, onToastMessage, onInAppMessage]
	);

	/**
	 * Subscribe to user-specific notification channels
	 */
	const subscribeToChannels = useCallback(
		(client: Client) => {
			if (!userId) {
				console.warn("Cannot subscribe to channels: userId not provided");
				return;
			}

			const subscriptions = [
				// New notifications
				client.subscribe(`/user/${userId}/notifications/new`, handleMessage),

				// Unread count updates
				client.subscribe(`/user/${userId}/notifications/count`, handleMessage),

				// Toast notifications
				client.subscribe(`/user/${userId}/notifications/toast`, handleMessage),

				// In-app notifications
				client.subscribe(`/user/${userId}/notifications/in-app`, handleMessage),

				// Global notifications (optional)
				client.subscribe("/topic/notifications/global", handleMessage),
			];

			subscriptionsRef.current = subscriptions;

			// console.log(
			// 	`useWebSocketNotifications.subscribeToChannels - Subscribed to notification channels for userId: ${userId}`
			// );
		},
		[userId, handleMessage]
	);

	/**
	 * Connect to WebSocket
	 */
	const connect = useCallback(() => {
		if (!enabled || !userId) {
			console.log("useWebSocketNotifications.connect - Connection skipped:", {
				enabled,
				userId: !!userId,
			});
			return;
		}

		// console.log(
		// 	`useWebSocketNotifications.connect - Starting connection for userId: ${userId}`
		// );

		if (clientRef.current?.connected) {
			console.log("WebSocket already connected");
			return;
		}

		// Validate JWT token before attempting connection
		const token = localStorage.getItem("bugtracker_token");
		if (
			!token ||
			token === "null" ||
			token === "undefined" ||
			token.trim() === ""
		) {
			console.warn("No valid JWT token found, skipping WebSocket connection");
			setError("Authentication required - please log in");
			setIsConnecting(false);
			return;
		}

		console.log("Connecting to WebSocket...", {
			userId,
			attempt: connectionAttempts + 1,
			tokenPresent: !!token,
		});
		setIsConnecting(true);
		setError(null);

		// Create STOMP client with native WebSocket
		const client = new Client({
			brokerURL: WS_URL,
			connectHeaders: {
				// Add authentication with validated token
				Authorization: `Bearer ${token}`,
			},
			debug: (str) => {
				if (process.env.NODE_ENV === "development") {
					// console.log("STOMP Debug:", str);
				}
			},
			reconnectDelay: RECONNECT_DELAY,
			heartbeatIncoming: 4000,
			heartbeatOutgoing: 4000,
		});

		// Connection event handlers
		client.onConnect = () => {
			console.log("WebSocket connected successfully");
			setIsConnected(true);
			setIsConnecting(false);
			setError(null);
			setConnectionAttempts(0);

			// Subscribe to channels
			subscribeToChannels(client);
		};

		client.onStompError = (frame) => {
			console.error("WebSocket: STOMP error:", frame);
			setError(`Connection error: ${frame.headers.message || "Unknown error"}`);
			setIsConnected(false);
			setIsConnecting(false);
		};

		client.onWebSocketError = (event) => {
			console.error("WebSocket: Connection error:", event);
			setError("WebSocket connection failed");
			setIsConnected(false);
			setIsConnecting(false);
		};

		client.onDisconnect = () => {
			console.log("WebSocket disconnected");
			setIsConnected(false);
			setIsConnecting(false);

			// Clear subscriptions
			subscriptionsRef.current = [];

			// Attempt reconnection if not manually disconnected
			if (enabled && connectionAttempts < MAX_RECONNECT_ATTEMPTS) {
				console.log(
					`Scheduling reconnection attempt ${
						connectionAttempts + 1
					}/${MAX_RECONNECT_ATTEMPTS}`
				);
				reconnectTimeoutRef.current = setTimeout(() => {
					setConnectionAttempts((prev) => prev + 1);
					connect();
				}, RECONNECT_DELAY);
			}
		};

		clientRef.current = client;
		client.activate();
	}, [enabled, userId, connectionAttempts, subscribeToChannels]);

	/**
	 * Disconnect from WebSocket
	 */
	const disconnect = useCallback(() => {
		console.log("Disconnecting WebSocket");

		// Clear reconnection timeout
		if (reconnectTimeoutRef.current) {
			clearTimeout(reconnectTimeoutRef.current);
			reconnectTimeoutRef.current = null;
		}

		// Unsubscribe from all channels
		subscriptionsRef.current.forEach((subscription) => {
			try {
				subscription.unsubscribe();
			} catch (error) {
				console.warn("Failed to unsubscribe:", error);
			}
		});
		subscriptionsRef.current = [];

		// Disconnect client
		if (clientRef.current) {
			clientRef.current.deactivate();
			clientRef.current = null;
		}

		setIsConnected(false);
		setIsConnecting(false);
		setError(null);
		setConnectionAttempts(0);
	}, []);

	/**
	 * Reconnect to WebSocket
	 */
	const reconnect = useCallback(() => {
		console.log("Manual reconnection triggered");
		disconnect();
		setTimeout(connect, 1000);
	}, [disconnect, connect]);

	// Auto-connect when enabled and userId available
	useEffect(() => {
		if (enabled && userId) {
			connect();
		} else {
			disconnect();
		}

		return () => {
			disconnect();
		};
	}, [enabled, userId]); // Only depend on enabled and userId

	// Cleanup on unmount
	useEffect(() => {
		return () => {
			disconnect();
		};
	}, []);

	return {
		isConnected,
		isConnecting,
		error,
		connectionAttempts,
		connect,
		disconnect,
		reconnect,
	};
}
