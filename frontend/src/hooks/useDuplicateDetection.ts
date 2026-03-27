import { useState, useCallback, useRef, useEffect, useMemo } from "react";
import {
	BugSimilarityResult,
	SimilarityCheckRequest,
} from "@/types/similarity";
import { bugService } from "@/services/bugService";
import { logger } from "@/utils/logger";

// Cache interface for storing similarity results
interface SimilarityCache {
	[key: string]: {
		results: BugSimilarityResult[];
		timestamp: number;
		expiresAt: number;
	};
}

// Cache configuration
const CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes
const MAX_CACHE_ENTRIES = 100;
const DEBOUNCE_DELAY_MS = 300;

// Similarity thresholds
const SIMILARITY_THRESHOLDS = {
	INFO: 0.4, // 40% - Show info warning
	WARNING: 0.7, // 70% - Show strong warning
	BLOCK: 0.9, // 90% - Block submission (but allow override)
} as const;

export interface UseDuplicateDetectionResult {
	// Core state
	similarBugs: BugSimilarityResult[];
	isChecking: boolean;
	lastCheckTime: Date | null;
	error: string | null;

	// Warning levels
	hasInfoWarning: boolean;
	hasWarning: boolean;
	hasBlockWarning: boolean;

	// Similarity counts
	infoCount: number;
	warningCount: number;
	blockCount: number;

	// Override state
	proceedAnyway: boolean;
	setProceedAnyway: (proceed: boolean) => void;

	// Actions
	checkSimilarity: (
		title: string,
		description: string,
		projectSlug: string
	) => Promise<void>;
	clearResults: () => void;
	clearCache: () => void;

	// Validation
	hasMinimumContent: (title: string, description: string) => boolean;
	getMinimumContentMessage: () => string;

	// Cache info
	cacheSize: number;
	cacheHitRate: number;
}

export function useDuplicateDetection(): UseDuplicateDetectionResult {
	// Core state
	const [similarBugs, setSimilarBugs] = useState<BugSimilarityResult[]>([]);
	const [isChecking, setIsChecking] = useState(false);
	const [lastCheckTime, setLastCheckTime] = useState<Date | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [proceedAnyway, setProceedAnyway] = useState(false);

	// Cache state
	const [cache, setCache] = useState<SimilarityCache>({});
	const [cacheStats, setCacheStats] = useState({ hits: 0, misses: 0 });

	// Refs for preventing infinite re-renders and API call tracking
	const abortControllerRef = useRef<AbortController | null>(null);
	const lastRequestRef = useRef<string>("");
	const isMountedRef = useRef(true);

	// Cleanup on unmount
	useEffect(() => {
		return () => {
			isMountedRef.current = false;
			if (abortControllerRef.current) {
				abortControllerRef.current.abort();
			}
		};
	}, []);

	// Memoized computed values to prevent unnecessary re-renders
	const warningLevels = useMemo(() => {
		const info = similarBugs.filter(
			(bug) => bug.similarityScore >= SIMILARITY_THRESHOLDS.INFO
		);
		const warning = similarBugs.filter(
			(bug) => bug.similarityScore >= SIMILARITY_THRESHOLDS.WARNING
		);
		const block = similarBugs.filter(
			(bug) => bug.similarityScore >= SIMILARITY_THRESHOLDS.BLOCK
		);

		return {
			hasInfoWarning: info.length > 0,
			hasWarning: warning.length > 0,
			hasBlockWarning: block.length > 0,
			infoCount: info.length,
			warningCount: warning.length,
			blockCount: block.length,
		};
	}, [similarBugs]);

	// Cache management functions
	const getCacheKey = useCallback(
		(title: string, description: string, projectSlug: string): string => {
			// Simple hash function for cache key
			const text = `${projectSlug}:${title.toLowerCase().trim()}:${description
				.toLowerCase()
				.trim()}`;
			let hash = 0;
			for (let i = 0; i < text.length; i++) {
				const char = text.charCodeAt(i);
				hash = (hash << 5) - hash + char;
				hash = hash & hash; // Convert to 32-bit integer
			}
			return `similarity_${Math.abs(hash)}`;
		},
		[]
	);

	const cleanupExpiredCache = useCallback(() => {
		const now = Date.now();
		setCache((prevCache) => {
			const newCache: SimilarityCache = {};
			let cleanedCount = 0;

			Object.entries(prevCache).forEach(([key, entry]) => {
				if (entry.expiresAt > now) {
					newCache[key] = entry;
				} else {
					cleanedCount++;
				}
			});

			// If cache is still too large, remove oldest entries
			if (Object.keys(newCache).length > MAX_CACHE_ENTRIES) {
				const sortedEntries = Object.entries(newCache).sort(
					([, a], [, b]) => a.timestamp - b.timestamp
				);

				// Keep only the newest MAX_CACHE_ENTRIES
				sortedEntries.slice(-MAX_CACHE_ENTRIES).forEach(([key, entry]) => {
					newCache[key] = entry;
				});
			}

			if (cleanedCount > 0) {
				logger.debug(
					"useDuplicateDetection",
					`Cleaned ${cleanedCount} expired cache entries`
				);
			}

			return newCache;
		});
	}, []);

	// Check if content meets minimum requirements for similarity analysis
	const hasMinimumContent = useCallback(
		(title: string, description: string): boolean => {
			return title.trim().length >= 1 && description.trim().length >= 10;
		},
		[]
	);

	// Get minimum content requirements message
	const getMinimumContentMessage = useCallback((): string => {
		return "Title must be at least 1 character and description must be at least 10 characters for duplicate detection";
	}, []);

	// Main similarity check function
	const checkSimilarity = useCallback(
		async (title: string, description: string, projectSlug: string) => {
			// Validate minimum content requirements
			if (!hasMinimumContent(title, description)) {
				const message = getMinimumContentMessage();
				setError(message);
				logger.debug(
					"useDuplicateDetection",
					"Content too short for similarity check",
					{
						titleLength: title.trim().length,
						descriptionLength: description.trim().length,
						message,
					}
				);
				return;
			}

			// Clear any previous errors
			setError(null);

			// Generate cache key
			const cacheKey = getCacheKey(title, description, projectSlug);

			// Check cache first
			const cached = cache[cacheKey];
			if (cached && Date.now() < cached.expiresAt) {
				setSimilarBugs(cached.results);
				setCacheStats((prev) => ({ ...prev, hits: prev.hits + 1 }));
				logger.debug(
					"useDuplicateDetection",
					"Cache hit for similarity check",
					{ cacheKey }
				);
				return;
			}

			// Cache miss
			setCacheStats((prev) => ({ ...prev, misses: prev.misses + 1 }));

			// Prevent duplicate API calls
			const requestKey = `${projectSlug}:${title}:${description}`;
			if (lastRequestRef.current === requestKey && isChecking) {
				logger.debug("useDuplicateDetection", "Duplicate request prevented", {
					requestKey,
				});
				return;
			}

			// Abort previous request if still pending
			if (abortControllerRef.current) {
				abortControllerRef.current.abort();
			}

			// Create new abort controller
			abortControllerRef.current = new AbortController();
			lastRequestRef.current = requestKey;

			try {
				setIsChecking(true);
				setError(null);

				logger.debug("useDuplicateDetection", "Starting similarity check", {
					projectSlug,
					titleLength: title.length,
					descriptionLength: description.length,
				});

				// Prepare request
				const request: SimilarityCheckRequest = {
					title: title.trim(),
					description: description.trim(),
					similarityThreshold: SIMILARITY_THRESHOLDS.INFO, // Check from 40% up
					maxResults: 10,
					includeClosedBugs: false,
				};

				// Make API call
				const results = await bugService.checkSimilarity(projectSlug, request);

				// Check if component is still mounted
				if (!isMountedRef.current) {
					return;
				}

				// Update state
				setSimilarBugs(results);
				setLastCheckTime(new Date());

				// Cache the results
				const now = Date.now();
				setCache((prevCache) => ({
					...prevCache,
					[cacheKey]: {
						results,
						timestamp: now,
						expiresAt: now + CACHE_DURATION_MS,
					},
				}));

				logger.info("useDuplicateDetection", "Similarity check completed", {
					projectSlug,
					resultsCount: results.length,
					highestSimilarity:
						results.length > 0
							? Math.max(...results.map((r) => r.similarityScore))
							: 0,
				});
			} catch (error) {
				// Check if component is still mounted
				if (!isMountedRef.current) {
					return;
				}

				// Handle abort errors separately
				if (error instanceof Error && error.name === "AbortError") {
					logger.debug("useDuplicateDetection", "Similarity check aborted");
					return;
				}

				// Handle other errors
				const errorMessage =
					error instanceof Error ? error.message : "Failed to check similarity";
				setError(errorMessage);
				logger.error("useDuplicateDetection", "Similarity check failed", {
					projectSlug,
					error: errorMessage,
				});
			} finally {
				// Check if component is still mounted
				if (!isMountedRef.current) {
					return;
				}

				setIsChecking(false);
				abortControllerRef.current = null;
			}
		},
		[cache, getCacheKey, hasMinimumContent, getMinimumContentMessage]
	);

	// Utility functions
	const clearResults = useCallback(() => {
		setSimilarBugs([]);
		setError(null);
		setProceedAnyway(false);
	}, []);

	const clearCache = useCallback(() => {
		setCache({});
		setCacheStats({ hits: 0, misses: 0 });
		logger.debug("useDuplicateDetection", "Cache cleared");
	}, []);

	// Cleanup expired cache entries periodically
	useEffect(() => {
		const interval = setInterval(cleanupExpiredCache, CACHE_DURATION_MS);
		return () => clearInterval(interval);
	}, [cleanupExpiredCache]);

	// Calculate cache statistics
	const cacheSize = Object.keys(cache).length;
	const cacheHitRate =
		cacheStats.hits + cacheStats.misses > 0
			? cacheStats.hits / (cacheStats.hits + cacheStats.misses)
			: 0;

	return {
		// Core state
		similarBugs,
		isChecking,
		lastCheckTime,
		error,

		// Warning levels
		...warningLevels,

		// Override state
		proceedAnyway,
		setProceedAnyway,

		// Actions
		checkSimilarity,
		clearResults,
		clearCache,

		// Validation
		hasMinimumContent,
		getMinimumContentMessage,

		// Cache info
		cacheSize,
		cacheHitRate,
	};
}
