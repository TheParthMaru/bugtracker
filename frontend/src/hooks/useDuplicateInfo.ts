import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { DuplicateInfoResponse } from "@/types/similarity";
import { bugService } from "@/services/bugService";

interface UseDuplicateInfoProps {
	projectSlug: string;
	bugId: number;
}

interface UseDuplicateInfoReturn {
	duplicateInfo: DuplicateInfoResponse | null;
	loading: boolean;
	error: string | null;
	refreshDuplicateInfo: () => Promise<void>;
	handleViewOriginal: () => void;
	handleViewDuplicates: () => void;
}

/**
 * Custom hook for managing duplicate information in bug detail pages.
 *
 * This hook provides:
 * - Loading duplicate information for a specific bug
 * - Navigation to original bug or duplicate bugs
 * - Error handling and refresh functionality
 * - Consistent state management
 */
export const useDuplicateInfo = ({
	projectSlug,
	bugId,
}: UseDuplicateInfoProps): UseDuplicateInfoReturn => {
	const navigate = useNavigate();
	const [duplicateInfo, setDuplicateInfo] =
		useState<DuplicateInfoResponse | null>(null);
	const [loading, setLoading] = useState(false); // Start as false, not true
	const [error, setError] = useState<string | null>(null);

	const loadDuplicateInfo = useCallback(async () => {
		// Don't load if we don't have valid data
		if (!projectSlug || !bugId || bugId <= 0) {
			console.log("useDuplicateInfo: Skipping load - invalid data:", {
				projectSlug,
				bugId,
			});
			return;
		}

		console.log("useDuplicateInfo: Loading duplicate info for:", {
			projectSlug,
			bugId,
		});

		try {
			setLoading(true);
			setError(null);

			// Use the ticket number method to avoid ID mapping issues
			const info = await bugService.getDuplicateInfoByTicketNumber(
				projectSlug,
				bugId
			);

			// Normalize duplicate flag in state for consistent rendering
			const normalized = {
				...info,
				isDuplicate: (info.isDuplicate ?? info.duplicate) === true,
			};

			console.log("useDuplicateInfo: Received duplicate info:", normalized);
			setDuplicateInfo(normalized as DuplicateInfoResponse);
		} catch (err) {
			const errorMessage =
				err instanceof Error
					? err.message
					: "Failed to load duplicate information";
			console.error("useDuplicateInfo: Error loading duplicate info:", err);
			setError(errorMessage);
		} finally {
			setLoading(false);
		}
	}, [projectSlug, bugId]);

	// Auto-load duplicate info when hook mounts or dependencies change
	useEffect(() => {
		loadDuplicateInfo();
	}, [projectSlug, bugId]); // Direct dependency on values, not the function

	// Debug: Log when duplicateInfo changes
	useEffect(() => {
		console.log(
			"useDuplicateInfo: State changed - duplicateInfo:",
			duplicateInfo
		);
	}, [duplicateInfo]);

	const refreshDuplicateInfo = useCallback(async () => {
		await loadDuplicateInfo();
	}, [loadDuplicateInfo]);

	const handleViewOriginal = useCallback(() => {
		if (duplicateInfo?.originalBug) {
			const originalBugUrl = `/projects/${projectSlug}/bugs/${duplicateInfo.originalBug.projectTicketNumber}`;
			navigate(originalBugUrl);
		}
	}, [duplicateInfo, projectSlug, navigate]);

	const handleViewDuplicates = useCallback(() => {
		if (
			duplicateInfo?.otherDuplicates &&
			duplicateInfo.otherDuplicates.length > 0
		) {
			// Navigate to similarity analysis page to show all duplicates
			const similarityUrl = `/projects/${projectSlug}/similarity-analysis`;
			navigate(similarityUrl);
		}
	}, [duplicateInfo, projectSlug, navigate]);

	return {
		duplicateInfo,
		loading,
		error,
		refreshDuplicateInfo,
		handleViewOriginal,
		handleViewDuplicates,
	};
};
