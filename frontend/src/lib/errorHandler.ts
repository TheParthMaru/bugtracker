/**
 * Standardized error handling utility for API services
 */

export interface ApiError {
	message: string;
	status?: number;
}

export interface ValidationError {
	errors: Record<string, string[]>;
}

/**
 * Standardized error handler for API services
 * @param error - The error object from axios or other sources
 * @param defaultMessage - Default message to use if no specific error is found
 * @param serviceName - Name of the service for logging purposes
 * @returns Never - always throws an Error
 */
export function handleApiError(
	error: any,
	defaultMessage: string,
	serviceName: string = "Service"
): never {
	console.error(`${serviceName} Error:`, error);

	// Handle Axios response errors with API error data
	if (error.response?.data) {
		const apiError = error.response.data as ApiError;
		if (apiError.message) {
			throw new Error(apiError.message);
		}
	}

	// Handle validation errors (422)
	if (error.response?.status === 422) {
		const validationError = error.response.data as ValidationError;
		if (validationError.errors) {
			const errorMessage = Object.values(validationError.errors)
				.flat()
				.join(", ");
			throw new Error(errorMessage || "Validation failed");
		}
	}

	// Handle network errors
	if (
		error.code === "NETWORK_ERROR" ||
		error.message?.includes("Network Error")
	) {
		throw new Error("Network error: Unable to connect to the server");
	}

	// Handle timeout errors
	if (error.code === "ECONNABORTED" || error.message?.includes("timeout")) {
		throw new Error("Request timeout: Server is taking too long to respond");
	}

	// Handle HTTP status errors
	if (error.response?.status) {
		switch (error.response.status) {
			case 400:
				throw new Error("Bad request: Invalid data provided");
			case 401:
				throw new Error("Authentication required: Please log in again");
			case 403:
				throw new Error(
					"Access denied: You don't have permission to perform this action"
				);
			case 404:
				throw new Error(
					"Resource not found: The requested data is not available"
				);
			case 409:
				throw new Error(
					"Conflict: The resource already exists or is in an invalid state"
				);
			case 422:
				throw new Error("Validation failed: Please check your input");
			case 500:
				throw new Error("Server error: Please try again later");
			case 502:
				throw new Error("Bad gateway: Server is temporarily unavailable");
			case 503:
				throw new Error("Service unavailable: Server is temporarily down");
			default:
				throw new Error(`HTTP ${error.response.status}: ${defaultMessage}`);
		}
	}

	// Handle other error types
	if (error.message) {
		throw new Error(error.message);
	}

	throw new Error(defaultMessage);
}
