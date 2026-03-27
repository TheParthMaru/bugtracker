import { useState, useEffect, useCallback } from "react";
import { DuplicateAnalyticsResponse } from "@/types/similarity";
import { bugService } from "@/services/bugService";

interface UseDuplicateAnalyticsProps {
	projectSlug: string;
}

interface UseDuplicateAnalyticsReturn {
	analytics: DuplicateAnalyticsResponse | null;
	loading: boolean;
	error: string | null;
	refreshAnalytics: () => Promise<void>;
}

/**
 * Custom hook for managing duplicate analytics data.
 *
 * This hook provides:
 * - Loading duplicate analytics for a project
 * - Error handling and refresh functionality
 * - Consistent state management for analytics dashboard
 */
export const useDuplicateAnalytics = ({
	projectSlug,
}: UseDuplicateAnalyticsProps): UseDuplicateAnalyticsReturn => {
	const [analytics, setAnalytics] = useState<DuplicateAnalyticsResponse | null>(
		null
	);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	const loadAnalytics = useCallback(async () => {
		try {
			setLoading(true);
			setError(null);

			const data = await bugService.getDuplicateAnalytics(projectSlug);
			setAnalytics(data);
		} catch (err) {
			const errorMessage =
				err instanceof Error
					? err.message
					: "Failed to load duplicate analytics";
			setError(errorMessage);
			console.error("Error loading duplicate analytics:", err);
		} finally {
			setLoading(false);
		}
	}, [projectSlug]);

	useEffect(() => {
		loadAnalytics();
	}, [loadAnalytics]);

	const refreshAnalytics = useCallback(async () => {
		await loadAnalytics();
	}, [loadAnalytics]);

	return {
		analytics,
		loading,
		error,
		refreshAnalytics,
	};
};
