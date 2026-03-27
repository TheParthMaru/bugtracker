/**
 * Notification Preferences Page
 *
 * Dedicated page for managing user notification preferences.
 * Allows users to configure channels, event types, and email settings.
 *
 * Features:
 * - Channel preferences (in-app, email, toast)
 * - Event type toggles (bugs, projects, teams, gamification)
 * - Email frequency settings
 * - Timezone configuration
 * - Reset to defaults option
 * - Real-time save feedback
 *
 * @author Notification Module Team
 * @version 1.0
 * @since 2025-01
 */

import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
	Save,
	RefreshCw,
	Bell,
	Mail,
	Smartphone,
	Bug,
	FolderOpen,
	Users,
	Trophy,
	Clock,
	Globe,
	Loader2,
	AlertCircle,
	CheckCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardContent } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Alert } from "@/components/ui/alert";
import { cn } from "@/lib/utils";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { SimpleBreadcrumb } from "@/components/ui/breadcrumb";
import { notificationService } from "@/services/notificationService";
import { toastUtils } from "@/components/notifications/ToastManager";
import type {
	NotificationPreferences,
	UpdateNotificationPreferencesRequest,
	EmailFrequency,
} from "@/types/notification";

export function NotificationPreferencesPage() {
	// State
	const [preferences, setPreferences] =
		useState<NotificationPreferences | null>(null);
	const [isLoading, setIsLoading] = useState(true);
	const [isSaving, setIsSaving] = useState(false);
	const [isResetting, setIsResetting] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [hasChanges, setHasChanges] = useState(false);
	const [lastSaved, setLastSaved] = useState<Date | null>(null);

	const navigate = useNavigate();

	// Load preferences on mount
	useEffect(() => {
		loadPreferences();
	}, []);

	// Load preferences from API
	const loadPreferences = async () => {
		try {
			setIsLoading(true);
			setError(null);
			const data = await notificationService.getPreferences();
			setPreferences(data);
			setHasChanges(false);
			console.log("Notification preferences loaded:", data);
		} catch (error) {
			console.error("Failed to load preferences:", error);
			setError("Failed to load notification preferences");
		} finally {
			setIsLoading(false);
		}
	};

	// Update preference field
	const updatePreference = <
		K extends keyof UpdateNotificationPreferencesRequest
	>(
		field: K,
		value: UpdateNotificationPreferencesRequest[K]
	) => {
		if (!preferences) return;

		setPreferences((prev) => ({
			...prev!,
			[field]: value,
		}));
		setHasChanges(true);
	};

	// Save preferences
	const savePreferences = async () => {
		if (!preferences || !hasChanges) return;

		try {
			setIsSaving(true);
			setError(null);

			const updateData: UpdateNotificationPreferencesRequest = {
				inAppEnabled: preferences.inAppEnabled,
				emailEnabled: preferences.emailEnabled,
				toastEnabled: preferences.toastEnabled,
				bugAssigned: preferences.bugAssigned,
				bugStatusChanged: preferences.bugStatusChanged,
				bugPriorityChanged: preferences.bugPriorityChanged,
				bugCommented: preferences.bugCommented,
				bugMentioned: preferences.bugMentioned,
				bugAttachmentAdded: preferences.bugAttachmentAdded,

				projectRoleChanged: preferences.projectRoleChanged,
				projectMemberJoined: preferences.projectMemberJoined,

				teamRoleChanged: preferences.teamRoleChanged,
				teamMemberJoined: preferences.teamMemberJoined,
				gamificationPoints: preferences.gamificationPoints,
				gamificationAchievements: preferences.gamificationAchievements,
				gamificationLeaderboard: preferences.gamificationLeaderboard,
				emailFrequency: preferences.emailFrequency,
				timezone: preferences.timezone,
			};

			const updatedPreferences = await notificationService.updatePreferences(
				updateData
			);
			setPreferences(updatedPreferences);
			setHasChanges(false);
			setLastSaved(new Date());

			toastUtils.success(
				"Preferences Saved",
				"Your notification preferences have been updated successfully."
			);

			console.log("Notification preferences saved:", updatedPreferences);
		} catch (error) {
			console.error("Failed to save preferences:", error);
			setError("Failed to save notification preferences");
			toastUtils.error(
				"Save Failed",
				"Unable to save your notification preferences. Please try again."
			);
		} finally {
			setIsSaving(false);
		}
	};

	// Reset to defaults
	const resetToDefaults = async () => {
		try {
			setIsResetting(true);
			setError(null);

			const response = await notificationService.resetPreferencesToDefaults();
			setPreferences(response.preferences);
			setHasChanges(false);
			setLastSaved(new Date());

			toastUtils.success(
				"Preferences Reset",
				"Your notification preferences have been reset to defaults."
			);

			console.log("Notification preferences reset:", response);
		} catch (error) {
			console.error("Failed to reset preferences:", error);
			setError("Failed to reset notification preferences");
			toastUtils.error(
				"Reset Failed",
				"Unable to reset your notification preferences. Please try again."
			);
		} finally {
			setIsResetting(false);
		}
	};

	// Timezone options (simplified list)
	const timezoneOptions = [
		{ value: "UTC", label: "UTC (Coordinated Universal Time)" },
		{ value: "America/New_York", label: "Eastern Time (ET)" },
		{ value: "America/Chicago", label: "Central Time (CT)" },
		{ value: "America/Denver", label: "Mountain Time (MT)" },
		{ value: "America/Los_Angeles", label: "Pacific Time (PT)" },
		{ value: "Europe/London", label: "British Time (GMT/BST)" },
		{ value: "Europe/Paris", label: "Central European Time (CET)" },
		{ value: "Asia/Tokyo", label: "Japan Standard Time (JST)" },
		{ value: "Asia/Shanghai", label: "China Standard Time (CST)" },
		{ value: "Australia/Sydney", label: "Australian Eastern Time (AET)" },
	];

	// Email frequency options
	const emailFrequencyOptions: { value: EmailFrequency; label: string }[] = [
		{ value: "IMMEDIATE", label: "Immediate" },
		{ value: "DAILY", label: "Daily Digest" },
		{ value: "WEEKLY", label: "Weekly Summary" },
	];

	if (isLoading) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-4">
					<SimpleBreadcrumb section="Notification Settings" />
					<div className="flex items-center justify-center min-h-96">
						<Loader2 className="h-8 w-8 animate-spin text-gray-400" />
						<span className="ml-3 text-gray-600">Loading preferences...</span>
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	if (error && !preferences) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-4">
					<SimpleBreadcrumb section="Notification Settings" />
					<div className="container mx-auto px-4 py-8">
						<Alert variant="destructive">
							<AlertCircle className="h-4 w-4" />
							<div>
								<h3 className="font-medium">Error Loading Preferences</h3>
								<p className="mt-1 text-sm">{error}</p>
								<Button
									variant="outline"
									size="sm"
									onClick={loadPreferences}
									className="mt-2"
								>
									<RefreshCw className="h-3 w-3 mr-1" />
									Try Again
								</Button>
							</div>
						</Alert>
					</div>
				</main>
				<Footer />
			</div>
		);
	}

	if (!preferences) return null;

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />
			<main className="flex-1 container mx-auto px-4 py-4">
				{/* Breadcrumb */}
				<SimpleBreadcrumb section="Notification Settings" />

				<div className="container mx-auto px-4 py-8 max-w-4xl">
					{/* Header */}
					<div className="mb-8">
						<div className="flex items-center gap-3 mb-2">
							<Bell className="h-6 w-6 text-blue-600" />
							<h1 className="text-2xl font-bold text-gray-900">
								Notification Preferences
							</h1>
						</div>
						<p className="text-gray-600">
							Customize how and when you receive notifications about your
							projects, bugs, and team activities.
						</p>
					</div>

					{/* Error Alert */}
					{error && (
						<Alert variant="destructive" className="mb-6">
							<AlertCircle className="h-4 w-4" />
							<div>
								<h3 className="font-medium">Error</h3>
								<p className="mt-1 text-sm">{error}</p>
							</div>
						</Alert>
					)}

					{/* Last Saved Indicator */}
					{lastSaved && (
						<div className="mb-6 flex items-center gap-2 text-sm text-green-600">
							<CheckCircle className="h-4 w-4" />
							<span>
								Last saved {lastSaved.toLocaleTimeString()} on{" "}
								{lastSaved.toLocaleDateString()}
							</span>
						</div>
					)}

					<div className="space-y-6">
						{/* Channel Preferences */}
						<Card>
							<CardHeader>
								<h2 className="text-lg font-semibold flex items-center gap-2">
									<Smartphone className="h-5 w-5 text-blue-600" />
									Notification Channels
								</h2>
								<p className="text-sm text-gray-600">
									Choose how you want to receive notifications
								</p>
							</CardHeader>
							<CardContent className="space-y-4">
								<div className="flex items-center justify-between">
									<div className="flex items-center gap-3">
										<Bell className="h-4 w-4 text-gray-600" />
										<div>
											<Label className="text-sm font-medium">
												In-App Notifications
											</Label>
											<p className="text-xs text-gray-500">
												Show notifications in the notification dropdown
											</p>
										</div>
									</div>
									<Switch
										checked={preferences.inAppEnabled}
										onCheckedChange={(checked) =>
											updatePreference("inAppEnabled", checked)
										}
									/>
								</div>

								<div className="flex items-center justify-between">
									<div className="flex items-center gap-3">
										<Mail className="h-4 w-4 text-gray-600" />
										<div>
											<Label className="text-sm font-medium">
												Email Notifications
											</Label>
											<p className="text-xs text-gray-500">
												Receive notifications via email
											</p>
										</div>
									</div>
									<Switch
										checked={preferences.emailEnabled}
										onCheckedChange={(checked) =>
											updatePreference("emailEnabled", checked)
										}
									/>
								</div>

								<div className="flex items-center justify-between">
									<div className="flex items-center gap-3">
										<Smartphone className="h-4 w-4 text-gray-600" />
										<div>
											<Label className="text-sm font-medium">
												Toast Notifications
											</Label>
											<p className="text-xs text-gray-500">
												Show popup notifications in the browser
											</p>
										</div>
									</div>
									<Switch
										checked={preferences.toastEnabled}
										onCheckedChange={(checked) =>
											updatePreference("toastEnabled", checked)
										}
									/>
								</div>
							</CardContent>
						</Card>

						{/* Bug Notifications */}
						<Card>
							<CardHeader>
								<h2 className="text-lg font-semibold flex items-center gap-2">
									<Bug className="h-5 w-5 text-red-600" />
									Bug Notifications
								</h2>
								<p className="text-sm text-gray-600">
									Get notified about bug-related activities
								</p>
							</CardHeader>
							<CardContent className="space-y-4">
								{[
									{
										key: "bugAssigned" as const,
										label: "Bug Assigned",
										description: "When a bug is assigned to you",
									},
									{
										key: "bugStatusChanged" as const,
										label: "Status Changes",
										description: "When bug status is updated",
									},
									{
										key: "bugPriorityChanged" as const,
										label: "Priority Changes",
										description: "When bug priority is modified",
									},
									{
										key: "bugCommented" as const,
										label: "New Comments",
										description:
											"When someone comments on bugs you're involved with",
									},
									{
										key: "bugMentioned" as const,
										label: "Mentions",
										description: "When you're mentioned in bug discussions",
									},
									{
										key: "bugAttachmentAdded" as const,
										label: "Attachments Added",
										description: "When files are attached to bugs",
									},
								].map(({ key, label, description }) => (
									<div key={key} className="flex items-center justify-between">
										<div>
											<Label className="text-sm font-medium">{label}</Label>
											<p className="text-xs text-gray-500">{description}</p>
										</div>
										<Switch
											checked={preferences[key]}
											onCheckedChange={(checked) =>
												updatePreference(key, checked)
											}
										/>
									</div>
								))}
							</CardContent>
						</Card>

						{/* Project Notifications */}
						<Card>
							<CardHeader>
								<h2 className="text-lg font-semibold flex items-center gap-2">
									<FolderOpen className="h-5 w-5 text-blue-600" />
									Project Notifications
								</h2>
								<p className="text-sm text-gray-600">
									Stay updated on project activities
								</p>
							</CardHeader>
							<CardContent className="space-y-4">
								{[
									{
										key: "projectRoleChanged" as const,
										label: "Role Changes",
										description: "When your project role is modified",
									},
									{
										key: "projectMemberJoined" as const,
										label: "New Members",
										description: "When new members join your projects",
									},
								].map(({ key, label, description }) => (
									<div key={key} className="flex items-center justify-between">
										<div>
											<Label className="text-sm font-medium">{label}</Label>
											<p className="text-xs text-gray-500">{description}</p>
										</div>
										<Switch
											checked={preferences[key]}
											onCheckedChange={(checked) =>
												updatePreference(key, checked)
											}
										/>
									</div>
								))}
							</CardContent>
						</Card>

						{/* Team Notifications */}
						<Card>
							<CardHeader>
								<h2 className="text-lg font-semibold flex items-center gap-2">
									<Users className="h-5 w-5 text-green-600" />
									Team Notifications
								</h2>
								<p className="text-sm text-gray-600">
									Keep track of team changes and activities
								</p>
							</CardHeader>
							<CardContent className="space-y-4">
								{[
									{
										key: "teamRoleChanged" as const,
										label: "Role Changes",
										description: "When your team role is modified",
									},
									{
										key: "teamMemberJoined" as const,
										label: "New Members",
										description: "When new members join your teams",
									},
								].map(({ key, label, description }) => (
									<div key={key} className="flex items-center justify-between">
										<div>
											<Label className="text-sm font-medium">{label}</Label>
											<p className="text-xs text-gray-500">{description}</p>
										</div>
										<Switch
											checked={preferences[key]}
											onCheckedChange={(checked) =>
												updatePreference(key, checked)
											}
										/>
									</div>
								))}
							</CardContent>
						</Card>

						{/* Gamification Notifications */}
						<Card>
							<CardHeader>
								<h2 className="text-lg font-semibold flex items-center gap-2">
									<Trophy className="h-5 w-5 text-yellow-600" />
									Gamification Notifications
								</h2>
								<p className="text-sm text-gray-600">
									Get notified about points, achievements, and leaderboard
									updates
								</p>
							</CardHeader>
							<CardContent className="space-y-4">
								{[
									{
										key: "gamificationPoints" as const,
										label: "Points Earned",
										description: "When you earn points for activities",
									},
									{
										key: "gamificationAchievements" as const,
										label: "Achievements Unlocked",
										description: "When you unlock new achievements",
									},
									{
										key: "gamificationLeaderboard" as const,
										label: "Leaderboard Updates",
										description: "Weekly leaderboard position updates",
									},
								].map(({ key, label, description }) => (
									<div key={key} className="flex items-center justify-between">
										<div>
											<Label className="text-sm font-medium">{label}</Label>
											<p className="text-xs text-gray-500">{description}</p>
										</div>
										<Switch
											checked={preferences[key]}
											onCheckedChange={(checked) =>
												updatePreference(key, checked)
											}
										/>
									</div>
								))}
							</CardContent>
						</Card>

						{/* Email Settings */}
						<Card>
							<CardHeader>
								<h2 className="text-lg font-semibold flex items-center gap-2">
									<Mail className="h-5 w-5 text-purple-600" />
									Email Settings
								</h2>
								<p className="text-sm text-gray-600">
									Configure email notification frequency and preferences
								</p>
							</CardHeader>
							<CardContent className="space-y-4">
								<div className="grid grid-cols-1 md:grid-cols-2 gap-4">
									<div className="space-y-2">
										<Label className="text-sm font-medium flex items-center gap-2">
											<Clock className="h-4 w-4" />
											Email Frequency
										</Label>
										<Select
											value={preferences.emailFrequency}
											onValueChange={(value: EmailFrequency) =>
												updatePreference("emailFrequency", value)
											}
										>
											<SelectTrigger>
												<SelectValue placeholder="Select frequency" />
											</SelectTrigger>
											<SelectContent>
												{emailFrequencyOptions.map((option) => (
													<SelectItem key={option.value} value={option.value}>
														{option.label}
													</SelectItem>
												))}
											</SelectContent>
										</Select>
										<p className="text-xs text-gray-500">
											How often you want to receive email notifications
										</p>
									</div>

									<div className="space-y-2">
										<Label className="text-sm font-medium flex items-center gap-2">
											<Globe className="h-4 w-4" />
											Timezone
										</Label>
										<Select
											value={preferences.timezone}
											onValueChange={(value: string) =>
												updatePreference("timezone", value)
											}
										>
											<SelectTrigger>
												<SelectValue placeholder="Select timezone" />
											</SelectTrigger>
											<SelectContent>
												{timezoneOptions.map((option) => (
													<SelectItem key={option.value} value={option.value}>
														{option.label}
													</SelectItem>
												))}
											</SelectContent>
										</Select>
										<p className="text-xs text-gray-500">
											Your timezone for scheduling email notifications
										</p>
									</div>
								</div>
							</CardContent>
						</Card>

						{/* Action Buttons */}
						<div className="flex items-center justify-between pt-6 border-t border-gray-200">
							<Button
								variant="outline"
								onClick={resetToDefaults}
								disabled={isResetting || isSaving}
							>
								{isResetting ? (
									<Loader2 className="h-4 w-4 animate-spin mr-2" />
								) : (
									<RefreshCw className="h-4 w-4 mr-2" />
								)}
								Reset to Defaults
							</Button>

							<div className="flex items-center gap-3">
								<Button
									variant="outline"
									onClick={() => navigate("/notifications")}
								>
									View Notifications
								</Button>
								<Button
									onClick={savePreferences}
									disabled={!hasChanges || isSaving}
									className={cn(
										hasChanges && "bg-blue-600 hover:bg-blue-700 text-white"
									)}
								>
									{isSaving ? (
										<Loader2 className="h-4 w-4 animate-spin mr-2" />
									) : (
										<Save className="h-4 w-4 mr-2" />
									)}
									{hasChanges ? "Save Changes" : "Saved"}
								</Button>
							</div>
						</div>
					</div>
				</div>
			</main>
			<Footer />
		</div>
	);
}
