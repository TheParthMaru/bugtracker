/**
 * API Service
 *
 * This file sets up and exports a configured Axios instance for making HTTP requests to the backend API.
 *
 * Key Features:
 * - Creates a base Axios instance with the API base URL
 * - Automatically adds JWT authentication token to requests
 * - Provides a centralized way to make API calls throughout the application
 *
 * Usage:
 * import API from '@/services/api'
 *
 * // Make authenticated requests
 * const response = await API.get('/endpoint')
 * const data = await API.post('/endpoint', payload)
 *
 * This lets us call API.get("/profile") without repeating token logic every time.
 */

import axios from "axios";
import { API_BASE_URL } from "@/config/constants";

const API = axios.create({
	baseURL: API_BASE_URL,
	timeout: 10000, // 10 second timeout
});

// Request interceptor for authentication and logging
API.interceptors.request.use(
	(config) => {
		// Add Authorization header if token exists
		const token = localStorage.getItem("bugtracker_token");
		if (token) {
			config.headers.Authorization = `Bearer ${token}`;
		}

		// Log request in development
		if (process.env.NODE_ENV === "development") {
			console.log("🚀 API Request:", {
				method: config.method?.toUpperCase(),
				url: config.url,
				baseURL: config.baseURL,
				params: config.params,
				data: config.data,
				headers: {
					...config.headers,
					Authorization: config.headers.Authorization
						? "[REDACTED]"
						: undefined,
				},
			});
		}

		// Add debug logging for similarity config requests
		if (config.url?.includes("similarity-config")) {
			console.log("🔍 DEBUG: Similarity config request detected");
			console.log("  - Full URL:", config.baseURL + config.url);
			console.log("  - Token exists:", !!token);
			console.log(
				"  - Token preview:",
				token ? token.substring(0, 20) + "..." : "none"
			);
		}

		// Add debug logging for team-related requests
		if (config.url?.includes("/teams/")) {
			console.log("🔍 DEBUG: Team request detected");
			console.log("  - Full URL:", config.baseURL + config.url);
			console.log("  - Method:", config.method?.toUpperCase());
			console.log("  - Params:", config.params);
			console.log("  - Data:", config.data);
		}

		return config;
	},
	(error) => {
		console.error("❌ API Request Error:", error);
		return Promise.reject(error);
	}
);

// Response interceptor for logging and error handling
API.interceptors.response.use(
	(response) => {
		// Log response in development
		if (process.env.NODE_ENV === "development") {
			console.log("✅ API Response:", {
				status: response.status,
				statusText: response.statusText,
				url: response.config.url,
				method: response.config.method?.toUpperCase(),
				data: response.data,
				headers: response.headers,
			});
		}

		// Add debug logging for similarity config responses
		if (response.config.url?.includes("similarity-config")) {
			console.log("🔍 DEBUG: Similarity config response received");
			console.log("  - Status:", response.status);
			console.log("  - Data:", response.data);
		}

		// Add debug logging for team-related responses
		if (response.config.url?.includes("/teams/")) {
			console.log("🔍 DEBUG: Team response received");
			console.log("  - Status:", response.status);
			console.log("  - URL:", response.config.url);
			console.log(
				"  - Data preview:",
				response.data
					? {
							id: response.data.id,
							name: response.data.name,
							teamSlug: response.data.teamSlug,
					  }
					: "No data"
			);
		}

		return response;
	},
	(error) => {
		// Log error response
		if (process.env.NODE_ENV === "development") {
			console.error("❌ API Error Response:", {
				status: error.response?.status,
				statusText: error.response?.statusText,
				url: error.config?.url,
				method: error.config?.method?.toUpperCase(),
				data: error.response?.data,
				message: error.message,
			});
		}

		// Add debug logging for similarity config errors
		if (error.config?.url?.includes("similarity-config")) {
			console.log("🔍 DEBUG: Similarity config error detected");
			console.log("  - Error status:", error.response?.status);
			console.log("  - Error data:", error.response?.data);
			console.log("  - Error message:", error.message);
			console.log("  - Full error object:", error);
		}

		// Add debug logging for team-related errors
		if (error.config?.url?.includes("/teams/")) {
			console.log("🔍 DEBUG: Team error detected");
			console.log("  - Error status:", error.response?.status);
			console.log("  - Error data:", error.response?.data);
			console.log("  - Error message:", error.message);
			console.log("  - Full URL:", error.config?.baseURL + error.config?.url);
			console.log("  - Full error object:", error);
		}

		// Handle authentication errors globally
		if (error.response?.status === 401) {
			// Token expired or invalid
			localStorage.removeItem("bugtracker_token");

			// Don't redirect if we're already on the login page or if it's a login request
			const isLoginRequest = error.config?.url?.includes("/auth/login");
			const isOnLoginPage = window.location.pathname === "/auth/login";

			if (!isOnLoginPage && !isLoginRequest) {
				window.location.href = "/auth/login";
			}
		}

		return Promise.reject(error);
	}
);

export default API;
