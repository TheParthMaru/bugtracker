/**
 * ToastTestComponent
 *
 * Test component to verify point notification toasts work correctly.
 * This is for development/testing purposes only.
 */

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PointNotificationService } from "@/services/pointNotificationService";

export default function ToastTestComponent() {
	const testWelcomeBonus = () => {
		PointNotificationService.showWelcomeBonus();
	};

	const testDailyLogin = () => {
		PointNotificationService.showDailyLogin();
	};

	const testBugResolution = () => {
		PointNotificationService.showBugResolution(
			100,
			"CRASH",
			"BugTracker",
			"#123"
		);
	};

	const testBugPenalty = () => {
		PointNotificationService.showBugPenalty(-10, "BugTracker", "#123");
	};

	const testGenericPoint = () => {
		PointNotificationService.showGenericPoint(25, "Team Collaboration");
	};

	return (
		<Card className="w-full max-w-md mx-auto">
			<CardHeader>
				<CardTitle>Toast Notification Tests</CardTitle>
			</CardHeader>
			<CardContent className="space-y-4">
				<Button onClick={testWelcomeBonus} className="w-full">
					Test Welcome Bonus Toast
				</Button>

				<Button onClick={testDailyLogin} className="w-full">
					Test Daily Login Toast
				</Button>

				<Button onClick={testBugResolution} className="w-full">
					Test Bug Resolution Toast
				</Button>

				<Button onClick={testBugPenalty} className="w-full">
					Test Bug Penalty Toast
				</Button>

				<Button onClick={testGenericPoint} className="w-full">
					Test Generic Point Toast
				</Button>
			</CardContent>
		</Card>
	);
}
