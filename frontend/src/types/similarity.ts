/**
 * Type definitions for bug similarity and duplicate detection features.
 *
 * These types correspond to the backend DTOs and ensure type safety
 * for similarity checking, duplicate marking, and configuration management.
 */

// === Similarity Algorithm Types ===

export enum SimilarityAlgorithm {
	COSINE = "COSINE",
	JACCARD = "JACCARD",
	LEVENSHTEIN = "LEVENSHTEIN",
}

export enum DuplicateDetectionMethod {
	MANUAL = "MANUAL",
	AUTOMATIC = "AUTOMATIC",
	HYBRID = "HYBRID",
}

// === Request Types ===

export interface SimilarityCheckRequest {
	title: string;
	description: string;
	projectId?: string;
	similarityThreshold?: number;
	maxResults?: number;
	includeClosedBugs?: boolean;
}

export interface MarkDuplicateRequest {
	originalBugId: number;
	confidenceScore?: number;
	additionalContext?: string;
	isAutomaticDetection?: boolean;
}

export interface ConfigurationUpdateRequest {
	weight?: number;
	threshold?: number;
	isEnabled?: boolean;
}

// === Response Types ===

export interface BugSimilarityResult {
	bugId: number;
	projectTicketNumber?: number;
	title: string;
	description: string;
	similarityScore: number;
	status: string;
	priority: string;
	assigneeName?: string;
	reporterName?: string;
	createdAt: string;
	updatedAt: string;
	algorithmScores: Record<SimilarityAlgorithm, number>;
	textFingerprint: string;
	isAlreadyMarkedDuplicate: boolean;
	originalBugId?: number;
}

export interface SimilarityConflictResponse {
	hasSimilarBugs: boolean;
	message: string;
	similarBugs: BugSimilarityResult[];
	highestSimilarity: number;
	totalSimilarCount: number;
	recommendation: string;
}

export interface SimilarityConfig {
	id: string;
	projectId: string;
	algorithmName: SimilarityAlgorithm;
	weight: number;
	threshold: number;
	isEnabled: boolean;
	createdAt: string;
	updatedAt: string;
}

export interface SimilarityStatistics {
	validCacheEntries: number;
	expiredCacheEntries: number;
	totalCacheEntries: number;
	totalDuplicates: number;
	algorithmUsage: Record<string, number>;
	hasEnabledAlgorithms: boolean;
}

export interface SimilarityHealth {
	status: "HEALTHY" | "NEEDS_CONFIGURATION" | "ERROR";
	hasEnabledAlgorithms: boolean;
	totalCacheEntries: number;
	recommendations: string[];
}

export interface ConfigurationValidation {
	isValid: boolean;
	hasEnabledAlgorithms: boolean;
	totalWeight: number;
	weightsSum: boolean;
	hasValidThresholds: boolean;
	enabledAlgorithmCount: number;
	recommendations: string[];
}

// === Utility Types ===

export interface SimilarityDisplayConfig {
	showAlgorithmBreakdown: boolean;
	showConfidenceScores: boolean;
	highlightHighSimilarity: boolean;
	maxDisplayResults: number;
}

export interface DuplicateDetectionSettings {
	enabled: boolean;
	realTimeCheck: boolean;
	debounceMs: number;
	minTitleLength: number;
	minDescriptionLength: number;
	warningThreshold: number;
	blockThreshold: number;
}

// === API Response Wrappers ===

export interface SimilarityApiResponse<T> {
	data: T;
	status: number;
	message?: string;
}

export interface SimilarityApiError {
	status: number;
	message: string;
	details?: Record<string, any>;
	timestamp: string;
}

// === Hook Return Types ===

export interface UseSimilarityCheckResult {
	similarBugs: BugSimilarityResult[];
	isChecking: boolean;
	lastCheckTime: Date | null;
	error: string | null;
	checkSimilarity: (title: string, description: string) => Promise<void>;
	clearResults: () => void;
}

export interface UseDuplicateDetectionResult extends UseSimilarityCheckResult {
	hasHighSimilarity: boolean;
	hasAnySimilarity: boolean;
	highSimilarityCount: number;
	shouldWarnUser: boolean;
	shouldBlockSubmission: boolean;
	proceedAnyway: boolean;
	setProceedAnyway: (proceed: boolean) => void;
	ensureConfigurationsInitialized: () => Promise<boolean>;
}

// === Component Props Types ===

export interface DuplicateDetectionProps {
	similarBugs: BugSimilarityResult[];
	isChecking: boolean;
	onViewBug: (bugId: number) => void;
	onProceedAnyway: () => void;
	settings?: SimilarityDisplayConfig;
}

export interface SimilarityCardProps {
	bug: BugSimilarityResult;
	onView: (bugId: number) => void;
	showAlgorithmBreakdown?: boolean;
	compact?: boolean;
}

export interface ConfigurationPanelProps {
	projectSlug: string;
	configurations: SimilarityConfig[];
	onConfigUpdate: (
		algorithm: SimilarityAlgorithm,
		config: ConfigurationUpdateRequest
	) => void;
	onReset: () => void;
	readonly?: boolean;
}

// === Form Types ===

export interface SimilarityFormData {
	title: string;
	description: string;
	checkSimilarity: boolean;
	proceedDespiteSimilarity: boolean;
}

export interface DuplicateMarkingFormData {
	originalBugId: number;
	confidenceScore: number;
	additionalContext: string;
	confirmDuplicate: boolean;
}

// === Constants ===

export const SIMILARITY_THRESHOLDS = {
	LOW: 0.3,
	MEDIUM: 0.6,
	HIGH: 0.8,
	VERY_HIGH: 0.9,
} as const;

export const DEFAULT_SIMILARITY_SETTINGS: DuplicateDetectionSettings = {
	enabled: true,
	realTimeCheck: true,
	debounceMs: 1000,
	minTitleLength: 5,
	minDescriptionLength: 20,
	warningThreshold: 0.6,
	blockThreshold: 0.85,
} as const;

export const DEFAULT_DISPLAY_CONFIG: SimilarityDisplayConfig = {
	showAlgorithmBreakdown: true,
	showConfidenceScores: true,
	highlightHighSimilarity: true,
	maxDisplayResults: 5,
} as const;

// === Type Guards ===

export const isBugSimilarityResult = (obj: any): obj is BugSimilarityResult => {
	return (
		obj &&
		typeof obj.bugId === "number" &&
		typeof obj.title === "string" &&
		typeof obj.description === "string" &&
		typeof obj.similarityScore === "number" &&
		obj.similarityScore >= 0 &&
		obj.similarityScore <= 1
	);
};

export const isSimilarityConfig = (obj: any): obj is SimilarityConfig => {
	return (
		obj &&
		typeof obj.id === "string" &&
		typeof obj.projectId === "string" &&
		Object.values(SimilarityAlgorithm).includes(obj.algorithmName) &&
		typeof obj.weight === "number" &&
		typeof obj.threshold === "number" &&
		typeof obj.isEnabled === "boolean"
	);
};

// === Utility Functions ===

export const getSimilarityLevel = (score: number): string => {
	if (score >= SIMILARITY_THRESHOLDS.VERY_HIGH) return "VERY_HIGH";
	if (score >= SIMILARITY_THRESHOLDS.HIGH) return "HIGH";
	if (score >= SIMILARITY_THRESHOLDS.MEDIUM) return "MEDIUM";
	return "LOW";
};

export const getSimilarityColor = (
	score: number
): "default" | "destructive" | "outline" | "secondary" => {
	if (score >= SIMILARITY_THRESHOLDS.VERY_HIGH) return "destructive";
	if (score >= SIMILARITY_THRESHOLDS.HIGH) return "destructive";
	if (score >= SIMILARITY_THRESHOLDS.MEDIUM) return "outline";
	return "secondary";
};

export const formatSimilarityPercentage = (score: number): string => {
	return `${(score * 100).toFixed(1)}%`;
};

export const shouldWarnUser = (
	bugs: BugSimilarityResult[],
	threshold = SIMILARITY_THRESHOLDS.HIGH
): boolean => {
	return bugs.some((bug) => bug.similarityScore >= threshold);
};

export const shouldBlockSubmission = (
	bugs: BugSimilarityResult[],
	threshold = SIMILARITY_THRESHOLDS.VERY_HIGH
): boolean => {
	return bugs.some((bug) => bug.similarityScore >= threshold);
};

// === Duplicate Information Types ===

export interface BugSummary {
	id: number;
	projectTicketNumber: number;
	title: string;
	status: string;
	priority: string;
	assigneeName?: string;
	reporterName?: string;
	createdAt: string;
	updatedAt: string;
}

export interface DuplicateRelationshipInfo {
	markedByUserName: string;
	markedAt: string;
}

export interface DuplicateInfoResponse {
	// Backend JSON often uses 'duplicate' (from Java getter isDuplicate())
	duplicate?: boolean;
	// Some parts of the app may still reference 'isDuplicate'; keep optional for compatibility
	isDuplicate?: boolean;
	originalBug?: BugSummary;
	relationshipInfo?: DuplicateRelationshipInfo;
	otherDuplicates?: BugSummary[];
	// Optional helper provided by backend in some responses
	otherDuplicatesCount?: number;
}

export interface BugDuplicateSummary {
	id: number;
	projectTicketNumber: number;
	title: string;
	status: string;
	priority: string;
	assigneeName?: string;
	reporterName?: string;
	createdAt: string;
	markedAsDuplicateAt: string;
	markedByUserName: string;
}

export interface DuplicateAnalyticsResponse {
	totalDuplicates: number;
	duplicatesByDetectionMethod: Record<string, number>;
	duplicatesByUser: Record<string, number>;

	// Utility methods
	hasDuplicates(): boolean;
	getManualDuplicates(): number;
	getAutomaticDuplicates(): number;
	getHybridDuplicates(): number;
	hasDetectionMethodBreakdown(): boolean;
	hasUserBreakdown(): boolean;
}

// === Duplicate Utility Functions ===

export function formatDate(dateString: string): string {
	return new Date(dateString).toLocaleDateString("en-US", {
		year: "numeric",
		month: "short",
		day: "numeric",
		hour: "2-digit",
		minute: "2-digit",
	});
}

export function getBugDisplayName(
	bug: BugSummary | BugDuplicateSummary
): string {
	return `#${bug.projectTicketNumber}: ${bug.title}`;
}

// === DuplicateAnalyticsResponse Implementation ===

export class DuplicateAnalyticsResponseImpl
	implements DuplicateAnalyticsResponse
{
	constructor(
		public totalDuplicates: number,
		public duplicatesByDetectionMethod: Record<string, number>,
		public duplicatesByUser: Record<string, number>
	) {}

	hasDuplicates(): boolean {
		return this.totalDuplicates > 0;
	}

	getManualDuplicates(): number {
		return this.duplicatesByDetectionMethod["MANUAL"] || 0;
	}

	getAutomaticDuplicates(): number {
		return this.duplicatesByDetectionMethod["AUTOMATIC"] || 0;
	}

	getHybridDuplicates(): number {
		return this.duplicatesByDetectionMethod["HYBRID"] || 0;
	}

	hasDetectionMethodBreakdown(): boolean {
		return (
			this.duplicatesByDetectionMethod &&
			Object.keys(this.duplicatesByDetectionMethod).length > 0
		);
	}

	hasUserBreakdown(): boolean {
		return (
			this.duplicatesByUser && Object.keys(this.duplicatesByUser).length > 0
		);
	}
}

// === Bug Similarity Relationship ===

export interface BugSimilarityRelationship {
	// Bug A information (the source bug)
	bugAId: number;
	bugAProjectTicketNumber?: number;
	bugATitle: string;
	bugADescription: string;
	bugAStatus: string;
	bugAPriority: string;
	bugAAssigneeName?: string;
	bugAReporterName?: string;
	bugACreatedAt: string;
	bugAUpdatedAt: string;

	// Bug B information (the similar bug)
	bugBId: number;
	bugBProjectTicketNumber?: number;
	bugBTitle: string;
	bugBDescription: string;
	bugBStatus: string;
	bugBPriority: string;
	bugBAssigneeName?: string;
	bugBReporterName?: string;
	bugBCreatedAt: string;
	bugBUpdatedAt: string;

	// Similarity information
	similarityScore: number;
	algorithmScores: Record<SimilarityAlgorithm, number>;
	textFingerprint: string;
	isAlreadyMarkedDuplicate: boolean;
	originalBugId?: number;
}
