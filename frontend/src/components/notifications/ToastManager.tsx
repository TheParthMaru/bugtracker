/**
 * Toast Manager Component
 *
 * Manages toast notifications from both manual triggers and WebSocket messages.
 * Integrates with react-toastify for consistent toast behavior across the app.
 *
 * Features:
 * - WebSocket toast message handling
 * - Manual toast triggers
 * - Consistent styling with app theme
 * - Auto-dismiss and manual dismiss
 * - Different toast types (info, success, warning, error)
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

import React, { useEffect } from "react";
import { toast, ToastContainer, ToastOptions } from "react-toastify";
import {
	Bell,
	CheckCircle,
	AlertCircle,
	AlertTriangle,
	Info,
} from "lucide-react";
import { useWebSocketNotifications } from "@/hooks/useWebSocketNotifications";
import "react-toastify/dist/ReactToastify.css";

interface ToastManagerProps {
	userId?: string;
	enabled?: boolean;
}

export function ToastManager({ userId, enabled = true }: ToastManagerProps) {
	// WebSocket connection for toast messages
	useWebSocketNotifications({
		userId,
		enabled,
		onToastMessage: (toastData) => {
			showToast(toastData.title, toastData.message, toastData.type);
		},
	});

	// Show toast with custom styling
	const showToast = (
		title: string,
		message: string,
		type: "info" | "success" | "warning" | "error" = "info"
	) => {
		const options: ToastOptions = {
			position: "top-right",
			autoClose: 5000,
			hideProgressBar: false,
			closeOnClick: true,
			pauseOnHover: true,
			draggable: true,
			className: "custom-toast",
		};

		const content = (
			<div className="flex items-start gap-3">
				<div className="flex-shrink-0 mt-0.5">{getToastIcon(type)}</div>
				<div className="flex-1 min-w-0">
					<div className="font-medium text-gray-900 text-sm">{title}</div>
					<div className="text-gray-600 text-sm mt-1">{message}</div>
				</div>
			</div>
		);

		switch (type) {
			case "success":
				toast.success(content, options);
				break;
			case "warning":
				toast.warning(content, options);
				break;
			case "error":
				toast.error(content, options);
				break;
			case "info":
			default:
				toast.info(content, options);
				break;
		}
	};

	// Get icon for toast type
	const getToastIcon = (type: "info" | "success" | "warning" | "error") => {
		switch (type) {
			case "success":
				return <CheckCircle className="h-5 w-5 text-green-500" />;
			case "warning":
				return <AlertTriangle className="h-5 w-5 text-yellow-500" />;
			case "error":
				return <AlertCircle className="h-5 w-5 text-red-500" />;
			case "info":
			default:
				return <Info className="h-5 w-5 text-blue-500" />;
		}
	};

	return (
		<ToastContainer
			position="top-right"
			autoClose={5000}
			hideProgressBar={false}
			newestOnTop
			closeOnClick
			rtl={false}
			pauseOnFocusLoss
			draggable
			pauseOnHover
			theme="light"
			className="custom-toast-container"
			toastClassName="custom-toast"
		/>
	);
}

// Export utility functions for manual toast triggers
export const toastUtils = {
	success: (title: string, message: string) => {
		const content = (
			<div className="flex items-start gap-3">
				{/* <CheckCircle className="h-5 w-5 text-green-500 flex-shrink-0 mt-0.5" /> */}
				<div className="flex-1 min-w-0">
					<div className="font-medium text-gray-900 text-sm">{title}</div>
					<div className="text-gray-600 text-sm mt-1">{message}</div>
				</div>
			</div>
		);
		toast.success(content);
	},

	error: (title: string, message: string) => {
		const content = (
			<div className="flex items-start gap-3">
				<AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0 mt-0.5" />
				<div className="flex-1 min-w-0">
					<div className="font-medium text-gray-900 text-sm">{title}</div>
					<div className="text-gray-600 text-sm mt-1">{message}</div>
				</div>
			</div>
		);
		toast.error(content);
	},

	warning: (title: string, message: string) => {
		const content = (
			<div className="flex items-start gap-3">
				<AlertTriangle className="h-5 w-5 text-yellow-500 flex-shrink-0 mt-0.5" />
				<div className="flex-1 min-w-0">
					<div className="font-medium text-gray-900 text-sm">{title}</div>
					<div className="text-gray-600 text-sm mt-1">{message}</div>
				</div>
			</div>
		);
		toast.warning(content);
	},

	info: (title: string, message: string) => {
		const content = (
			<div className="flex items-start gap-3">
				<Info className="h-5 w-5 text-blue-500 flex-shrink-0 mt-0.5" />
				<div className="flex-1 min-w-0">
					<div className="font-medium text-gray-900 text-sm">{title}</div>
					<div className="text-gray-600 text-sm mt-1">{message}</div>
				</div>
			</div>
		);
		toast.info(content);
	},

	notification: (title: string, message: string) => {
		const content = (
			<div className="flex items-start gap-3">
				<Bell className="h-5 w-5 text-blue-500 flex-shrink-0 mt-0.5" />
				<div className="flex-1 min-w-0">
					<div className="font-medium text-gray-900 text-sm">{title}</div>
					<div className="text-gray-600 text-sm mt-1">{message}</div>
				</div>
			</div>
		);
		toast.info(content, {
			className: "notification-toast",
			icon: false,
		});
	},
};
