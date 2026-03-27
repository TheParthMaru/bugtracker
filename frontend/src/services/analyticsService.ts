import API from "./api";
import type {
	ProjectBugStatistics,
	TeamPerformanceStatistics,
	ProjectReport,
	ExportOptions,
} from "../types/analytics";

export class AnalyticsService {
	private static instance: AnalyticsService;

	private constructor() {
		// No baseUrl needed - using centralized API service
	}

	public static getInstance(): AnalyticsService {
		if (!AnalyticsService.instance) {
			AnalyticsService.instance = new AnalyticsService();
		}
		return AnalyticsService.instance;
	}

	// Basic Statistics
	async getProjectStatistics(
		projectId: string,
		startDate?: string,
		endDate?: string
	): Promise<ProjectBugStatistics> {
		try {
			const params: any = {};
			if (startDate) params.startDate = startDate;
			if (endDate) params.endDate = endDate;

			console.log("📊 getProjectStatistics called with params:", {
				projectId,
				startDate,
				endDate,
				params,
			});

			const response = await API.get<ProjectBugStatistics>(
				`/projects/${projectId}/analytics/statistics`,
				{
					params,
				}
			);
			return response.data;
		} catch (error) {
			this.handleError(error, "Failed to fetch project statistics");
		}
	}

	async getTeamPerformanceStatistics(
		projectId: string,
		startDate?: string,
		endDate?: string
	): Promise<TeamPerformanceStatistics> {
		try {
			const params: any = {};
			if (startDate) params.startDate = startDate;
			if (endDate) params.endDate = endDate;

			console.log("👥 getTeamPerformanceStatistics called with params:", {
				projectId,
				startDate,
				endDate,
				params,
			});

			const response = await API.get<TeamPerformanceStatistics>(
				`/projects/${projectId}/analytics/team-performance`,
				{
					params,
				}
			);
			return response.data;
		} catch (error) {
			this.handleError(error, "Failed to fetch team performance statistics");
		}
	}

	// Reporting
	async generateProjectReport(
		projectId: string,
		startDate: string,
		endDate: string
	): Promise<ProjectReport> {
		try {
			const response = await API.get<ProjectReport>(
				`/projects/${projectId}/analytics/reports/comprehensive`,
				{
					params: { startDate, endDate },
				}
			);
			return response.data;
		} catch (error) {
			this.handleError(error, "Failed to generate project report");
		}
	}

	// Export functionality
	async exportReport(projectId: string, options: ExportOptions): Promise<Blob> {
		try {
			const response = await API.post<Blob>(
				`/projects/${projectId}/analytics/export`,
				options,
				{
					responseType: "blob",
				}
			);
			return response.data;
		} catch (error) {
			this.handleError(error, "Failed to export report");
		}
	}

	// No mock data - only real backend data is used

	private handleError(error: any, defaultMessage: string): never {
		console.error("Analytics Service Error:", error);
		throw new Error(error.message || defaultMessage);
	}
}

export const analyticsService = AnalyticsService.getInstance();
