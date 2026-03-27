import { useCallback, useRef, useEffect } from "react";
import { logger } from "@/utils/logger";

interface UseDebouncedSimilarityCheckOptions {
	delay?: number;
	onCheck: (
		title: string,
		description: string,
		projectSlug: string
	) => Promise<void>;
}

export function useDebouncedSimilarityCheck({
	delay = 300,
	onCheck,
}: UseDebouncedSimilarityCheckOptions) {
	// Refs to prevent infinite re-renders and track state
	const timeoutRef = useRef<NodeJS.Timeout | null>(null);
	const lastCheckRef = useRef<string>("");
	const isCheckingRef = useRef(false);

	// Cleanup on unmount
	useEffect(() => {
		return () => {
			if (timeoutRef.current) {
				clearTimeout(timeoutRef.current);
			}
		};
	}, []);

	// Debounced similarity check function
	const debouncedCheck = useCallback(
		(title: string, description: string, projectSlug: string) => {
			// Clear existing timeout
			if (timeoutRef.current) {
				clearTimeout(timeoutRef.current);
			}

			// Create request key to prevent duplicate checks
			const requestKey = `${projectSlug}:${title}:${description}`;

			// If this is the same request and we're already checking, skip
			if (lastCheckRef.current === requestKey && isCheckingRef.current) {
				logger.debug(
					"useDebouncedSimilarityCheck",
					"Duplicate request prevented",
					{ requestKey }
				);
				return;
			}

			// Set timeout for debounced check
			timeoutRef.current = setTimeout(async () => {
				try {
					// Update tracking refs
					lastCheckRef.current = requestKey;
					isCheckingRef.current = true;

					logger.debug(
						"useDebouncedSimilarityCheck",
						"Executing debounced similarity check",
						{
							projectSlug,
							titleLength: title.length,
							descriptionLength: description.length,
						}
					);

					// Execute the similarity check
					await onCheck(title, description, projectSlug);
				} catch (error) {
					logger.error(
						"useDebouncedSimilarityCheck",
						"Similarity check failed",
						{
							projectSlug,
							error: error instanceof Error ? error.message : "Unknown error",
						}
					);
				} finally {
					isCheckingRef.current = false;
				}
			}, delay);
		},
		[delay, onCheck]
	);

	// Immediate check function (for form submission)
	const immediateCheck = useCallback(
		(title: string, description: string, projectSlug: string) => {
			// Clear any pending debounced check
			if (timeoutRef.current) {
				clearTimeout(timeoutRef.current);
				timeoutRef.current = null;
			}

			// Execute immediately
			return onCheck(title, description, projectSlug);
		},
		[onCheck]
	);

	// Cancel pending checks
	const cancelPendingCheck = useCallback(() => {
		if (timeoutRef.current) {
			clearTimeout(timeoutRef.current);
			timeoutRef.current = null;
		}
	}, []);

	return {
		debouncedCheck,
		immediateCheck,
		cancelPendingCheck,
	};
}
