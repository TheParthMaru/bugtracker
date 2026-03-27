import API from "./api";
import { BugLabel } from "@/types/bug";

// Single Responsibility: Define clear interfaces for different concerns
export interface CreateLabelRequest {
	name: string;
	color?: string; // Made optional with default
	description?: string; // Made optional with default
}

export interface UpdateLabelRequest {
	name?: string;
	color?: string;
	description?: string;
}

// Open/Closed: Define error types that can be extended
export class LabelServiceError extends Error {
	constructor(
		message: string,
		public statusCode?: number,
		public originalError?: any
	) {
		super(message);
		this.name = "LabelServiceError";
	}
}

// Interface Segregation: Separate concerns for different operations
export interface ILabelReader {
	getLabels(projectSlug: string): Promise<BugLabel[]>;
}

export interface ILabelWriter {
	createLabel(projectSlug: string, data: CreateLabelRequest): Promise<BugLabel>;
	updateLabel(
		projectSlug: string,
		labelId: number,
		data: UpdateLabelRequest
	): Promise<BugLabel>;
	deleteLabel(projectSlug: string, labelId: number): Promise<void>;
}

// Dependency Inversion: Use interfaces instead of concrete implementations
export class BugLabelService implements ILabelReader, ILabelWriter {
	private readonly baseUrl = "/projects"; // Remove /api/bugtracker/v1 since it's already in API baseURL

	/**
	 * Get all labels for a project
	 * Single Responsibility: Only handles label retrieval
	 */
	async getLabels(projectSlug: string): Promise<BugLabel[]> {
		try {
			const response = await API.get(
				`${this.baseUrl}/${projectSlug}/bug-labels`
			);

			// Extract content from paginated response
			const labels = response.data.content || [];

			// Validate response structure
			if (!Array.isArray(labels)) {
				throw new LabelServiceError(
					"Invalid response format: expected array of labels"
				);
			}

			return labels;
		} catch (error: any) {
			const errorMessage = this.extractErrorMessage(error);
			throw new LabelServiceError(
				`Failed to fetch labels: ${errorMessage}`,
				error.response?.status,
				error
			);
		}
	}

	/**
	 * Create a new label
	 * Single Responsibility: Only handles label creation
	 */
	async createLabel(
		projectSlug: string,
		data: CreateLabelRequest
	): Promise<BugLabel> {
		try {
			// Validate input data
			this.validateCreateLabelRequest(data);

			const response = await API.post(
				`${this.baseUrl}/${projectSlug}/bug-labels`,
				data
			);

			// Validate response
			if (!response.data || !response.data.id) {
				throw new LabelServiceError(
					"Invalid response format: missing label data"
				);
			}

			return response.data;
		} catch (error: any) {
			const errorMessage = this.extractErrorMessage(error);
			throw new LabelServiceError(
				`Failed to create label: ${errorMessage}`,
				error.response?.status,
				error
			);
		}
	}

	/**
	 * Update an existing label
	 * Single Responsibility: Only handles label updates
	 */
	async updateLabel(
		projectSlug: string,
		labelId: number,
		data: UpdateLabelRequest
	): Promise<BugLabel> {
		try {
			// Validate input data
			this.validateUpdateLabelRequest(data);

			const response = await API.put(
				`${this.baseUrl}/${projectSlug}/bug-labels/${labelId}`,
				data
			);

			// Validate response
			if (!response.data || !response.data.id) {
				throw new LabelServiceError(
					"Invalid response format: missing label data"
				);
			}

			return response.data;
		} catch (error: any) {
			const errorMessage = this.extractErrorMessage(error);
			throw new LabelServiceError(
				`Failed to update label: ${errorMessage}`,
				error.response?.status,
				error
			);
		}
	}

	/**
	 * Delete a label
	 * Single Responsibility: Only handles label deletion
	 */
	async deleteLabel(projectSlug: string, labelId: number): Promise<void> {
		try {
			await API.delete(`${this.baseUrl}/${projectSlug}/bug-labels/${labelId}`);
		} catch (error: any) {
			const errorMessage = this.extractErrorMessage(error);
			throw new LabelServiceError(
				`Failed to delete label: ${errorMessage}`,
				error.response?.status,
				error
			);
		}
	}

	// Private helper methods for validation and error handling
	private validateCreateLabelRequest(data: CreateLabelRequest): void {
		if (!data.name || data.name.trim().length === 0) {
			throw new LabelServiceError("Label name is required");
		}
		if (!data.color || data.color.trim().length === 0) {
			throw new LabelServiceError("Label color is required");
		}
		if (data.name.length > 50) {
			throw new LabelServiceError("Label name cannot exceed 50 characters");
		}
		if (data.description && data.description.length > 255) {
			throw new LabelServiceError(
				"Label description cannot exceed 255 characters"
			);
		}
	}

	private validateUpdateLabelRequest(data: UpdateLabelRequest): void {
		if (data.name !== undefined && data.name.trim().length === 0) {
			throw new LabelServiceError("Label name cannot be empty");
		}
		if (data.name && data.name.length > 50) {
			throw new LabelServiceError("Label name cannot exceed 50 characters");
		}
		if (data.description && data.description.length > 255) {
			throw new LabelServiceError(
				"Label description cannot exceed 255 characters"
			);
		}
	}

	private extractErrorMessage(error: any): string {
		if (error.response?.data?.message) {
			return error.response.data.message;
		}
		if (error.message) {
			return error.message;
		}
		if (error.response?.status === 500) {
			return "Internal server error";
		}
		if (error.response?.status === 404) {
			return "Resource not found";
		}
		if (error.response?.status === 400) {
			return "Bad request";
		}
		return "Unknown error occurred";
	}
}

// Export a singleton instance
export const bugLabelService = new BugLabelService();
