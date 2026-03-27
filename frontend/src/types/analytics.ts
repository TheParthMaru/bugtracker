// Analytics Types for Bug Tracking
import type { Bug } from "./bug";

export interface ProjectBugStatistics {
	totalBugs: number;
	openBugs: number;
	assignedBugs: number;
	fixedBugs: number;
	closedBugs: number;
	reopenedBugs: number;
	priorityDistribution: Record<string, number>;
	typeDistribution: Record<string, number>;
	statusDistribution: Record<string, number>;
	resolutionRate: number;
}

export interface UserStats {
	userId: string;
	firstName: string;
	lastName: string;
	count: number;
	fullName: string;
}

export interface AssigneeStats extends UserStats {
	resolvedCount: number;
}

export interface TeamBugStats {
	teamId: string;
	teamName: string;
	teamDescription: string;
	teamSlug: string;
	totalBugs: number;
	openBugs: number;
	resolvedBugs: number;
	resolutionRate: number;
}

export interface TeamPerformanceStatistics {
	assigneeStats: AssigneeStats[];
	reporterStats: UserStats[];
	resolutionTimeStats: Array<[string, number]>; // [userId, avgResolutionTime]
	teamBugStats: TeamBugStats[];
}

export interface BugVelocityMetrics {
	totalResolved: number;
	daysInPeriod: number;
	bugsPerDay: number;
}

export interface BugQualityMetrics {
	reopenRate: number;
	invalidRate: number;
	duplicateRate: number;
	reopenedBugs: number;
	invalidBugs: number;
	duplicateBugs: number;
}

export interface ProjectReport {
	statistics: ProjectBugStatistics;
	teamStats: TeamPerformanceStatistics;
	velocity: BugVelocityMetrics;
	quality: BugQualityMetrics;
	creationTrends: Array<[string, number]>; // [date, count]
	resolutionTrends: Array<[string, number]>; // [date, count]
	startDate: string;
	endDate: string;
}

// Chart Data Types
export interface ChartDataPoint {
	label: string;
	value: number;
	color?: string;
}

export interface TrendDataPoint {
	date: string;
	value: number;
}

export interface PerformanceMetrics {
	resolutionTimeByPriority: ChartDataPoint[];
	resolutionTimeByType: ChartDataPoint[];
	velocityMetrics: BugVelocityMetrics;
}

export interface RiskAnalysis {
	attentionRequired: Bug[];
	highPriorityUnassigned: Bug[];
	longResolutionTimes: Bug[];
}

// Filter Types
export interface AnalyticsFilters {
	startDate: string;
	endDate: string;
	teamId?: string;
	assigneeId?: string;
	priority?: string;
	type?: string;
}

// Export Types
export interface ExportOptions {
	format: "pdf" | "csv" | "excel";
	includeCharts: boolean;
	includeTrends: boolean;
	dateRange: {
		start: string;
		end: string;
	};
}

// Dashboard Types
export interface AnalyticsDashboard {
	overview: ProjectBugStatistics;
	trends: {
		creation: TrendDataPoint[];
		resolution: TrendDataPoint[];
	};
	performance: PerformanceMetrics;
	risks: RiskAnalysis;
	quality: BugQualityMetrics;
}

// Component Props
export interface AnalyticsChartProps {
	data: ChartDataPoint[];
	title: string;
	type: "pie" | "bar" | "line" | "doughnut";
	height?: number;
	width?: number;
	className?: string;
}

export interface TrendChartProps {
	data: TrendDataPoint[];
	title: string;
	color?: string;
	height?: number;
	width?: number;
	className?: string;
}

export interface StatisticsCardProps {
	title: string;
	value: number | string;
	change?: number;
	changeType?: "increase" | "decrease" | "neutral";
	icon?: React.ReactNode;
	className?: string;
}

export interface RiskCardProps {
	title: string;
	bugs: Bug[];
	priority: "high" | "medium" | "low";
	onViewAll?: () => void;
	className?: string;
}

// API Response Types
export interface AnalyticsApiResponse<T> {
	data: T;
	success: boolean;
	message?: string;
}

export interface TrendApiResponse {
	data: Array<[string, number]>;
	success: boolean;
	message?: string;
}

// Utility Types
export interface DateRange {
	start: Date;
	end: Date;
}

export interface AnalyticsPeriod {
	label: string;
	startDate: string;
	endDate: string;
}

// Predefined periods for quick selection
export const ANALYTICS_PERIODS: AnalyticsPeriod[] = [
	{
		label: "Last 7 days",
		startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
		endDate: new Date().toISOString(),
	},
	{
		label: "Last 30 days",
		startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
		endDate: new Date().toISOString(),
	},
	{
		label: "Last 90 days",
		startDate: new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString(),
		endDate: new Date().toISOString(),
	},
	{
		label: "This month",
		startDate: new Date(
			new Date().getFullYear(),
			new Date().getMonth(),
			1
		).toISOString(),
		endDate: new Date().toISOString(),
	},
	{
		label: "Last month",
		startDate: new Date(
			new Date().getFullYear(),
			new Date().getMonth() - 1,
			1
		).toISOString(),
		endDate: new Date(
			new Date().getFullYear(),
			new Date().getMonth(),
			0
		).toISOString(),
	},
];

// Color schemes for charts
export const CHART_COLORS = {
	primary: "#3b82f6",
	secondary: "#64748b",
	success: "#10b981",
	warning: "#f59e0b",
	danger: "#ef4444",
	info: "#06b6d4",
	purple: "#8b5cf6",
	pink: "#ec4899",
	orange: "#f97316",
	teal: "#14b8a6",
};

export const PRIORITY_COLORS = {
	CRASH: CHART_COLORS.danger,
	CRITICAL: CHART_COLORS.orange,
	HIGH: CHART_COLORS.warning,
	MEDIUM: CHART_COLORS.info,
	LOW: CHART_COLORS.success,
};

export const STATUS_COLORS = {
	OPEN: CHART_COLORS.danger,
	ASSIGNED: CHART_COLORS.warning,
	FIXED: CHART_COLORS.success,
	CLOSED: CHART_COLORS.secondary,
	REOPENED: CHART_COLORS.orange,
};

export const TYPE_COLORS = {
	ISSUE: CHART_COLORS.danger,
	TASK: CHART_COLORS.info,
	SPEC: CHART_COLORS.purple,
};
