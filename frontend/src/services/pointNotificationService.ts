/**
 * Point Notification Service
 *
 * Centralized service for displaying gamification point notifications.
 * Uses the existing React Toastify system for consistent styling and behavior.
 *
 * Features:
 * - Welcome bonus notifications
 * - Daily login notifications
 * - Bug resolution notifications
 * - Penalty notifications
 * - Consistent toast styling and behavior
 */

import { toast } from "react-toastify";

export class PointNotificationService {
	/**
	 * Show welcome bonus notification when user first accesses gamification
	 */
	static showWelcomeBonus() {
		const message = "🪙+1 point Welcome bonus";
		console.log(
			"PointNotificationService: Showing welcome bonus notification",
			{ message }
		);

		toast.success(message, {
			autoClose: 5000,
			hideProgressBar: false,
			closeOnClick: true,
			pauseOnHover: true,
			draggable: true,
		});
	}

	/**
	 * Show daily login notification when daily login points are awarded
	 */
	static showDailyLogin() {
		const message = "🪙+1 point Daily login bonus";
		console.log("PointNotificationService: Showing daily login notification", {
			message,
		});

		toast.success(message, {
			autoClose: 4000,
			hideProgressBar: false,
			closeOnClick: true,
			pauseOnHover: true,
			draggable: true,
		});
	}

	/**
	 * Show bug resolution notification when user resolves a bug
	 */
	static showBugResolution(
		points: number,
		priority: string,
		projectName?: string,
		ticketNumber?: string
	) {
		let message = `🪙+${points} points for resolving ${priority} priority bug`;

		if (projectName && ticketNumber) {
			message += ` | ${projectName} - ${ticketNumber}`;
		}

		console.log(
			"PointNotificationService: Showing bug resolution notification",
			{
				message,
				points,
				priority,
				projectName,
				ticketNumber,
			}
		);

		toast.success(message, {
			autoClose: 4000,
			hideProgressBar: false,
			closeOnClick: true,
			pauseOnHover: true,
			draggable: true,
		});
	}

	/**
	 * Show bug penalty notification when bug is reopened
	 */
	static showBugPenalty(
		points: number,
		projectName?: string,
		ticketNumber?: string
	) {
		let message = `🪙${points} points penalty for reopening bug`;

		if (projectName && ticketNumber) {
			message += ` | ${projectName} - ${ticketNumber}`;
		}

		console.log("PointNotificationService: Showing bug penalty notification", {
			message,
			points,
			projectName,
			ticketNumber,
		});

		toast.error(message, {
			autoClose: 4000,
			hideProgressBar: false,
			closeOnClick: true,
			pauseOnHover: true,
			draggable: true,
		});
	}

	/**
	 * Show generic point notification for other point types
	 */
	static showGenericPoint(points: number, reason: string) {
		const isPositive = points > 0;
		const toastMethod = isPositive ? toast.success : toast.error;
		const message = `🪙${points > 0 ? "+" : ""}${points} points for ${reason}`;

		console.log(
			"PointNotificationService: Showing generic point notification",
			{
				message,
				points,
				reason,
				isPositive,
			}
		);

		toastMethod(message, {
			autoClose: 4000,
			hideProgressBar: false,
			closeOnClick: true,
			pauseOnHover: true,
			draggable: true,
		});
	}
}
