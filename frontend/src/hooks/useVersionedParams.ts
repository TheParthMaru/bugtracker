import { useParams, useLocation } from "react-router-dom";

/**
 * Custom hook that properly extracts route parameters from versioned URLs
 *
 * When using basename="/api/bugtracker/v1", React Router sometimes fails to
 * properly extract parameters. This hook provides a fallback by manually
 * parsing the URL path to extract the expected parameters.
 *
 * @returns Object containing the extracted parameters and a flag indicating if manual parsing was used
 */
export const useVersionedParams = () => {
	const params = useParams();
	const location = useLocation();

	// Check if React Router successfully extracted parameters
	const hasValidParams = Object.values(params).some(
		(param) => param !== undefined
	);

	if (hasValidParams) {
		// React Router worked correctly, return the params as-is
		return {
			params,
			usedFallback: false,
		};
	}

	// React Router failed to extract parameters, manually parse the URL
	console.warn(
		"useVersionedParams: React Router failed to extract params, using manual parsing",
		{
			location,
			params,
		}
	);

	// Extract project slug and project ticket number from the path
	// Expected format: /api/bugtracker/v1/projects/:projectSlug/bugs/:projectTicketNumber
	const pathParts = location.pathname.split("/");

	// Find the index of "projects" in the path
	const projectsIndex = pathParts.findIndex((part) => part === "projects");

	let projectSlug: string | undefined;
	let projectTicketNumber: string | undefined;

	if (projectsIndex !== -1 && projectsIndex + 1 < pathParts.length) {
		projectSlug = pathParts[projectsIndex + 1];

		// Look for "bugs" after the project slug
		const bugsIndex = pathParts.findIndex(
			(part, index) => index > projectsIndex + 1 && part === "bugs"
		);

		if (bugsIndex !== -1 && bugsIndex + 1 < pathParts.length) {
			projectTicketNumber = pathParts[bugsIndex + 1];
		}
	}

	const manualParams = {
		projectSlug, // Map to the route parameter name
		projectTicketNumber,
		...params, // Include any other params that React Router might have extracted
	};

	return {
		params: manualParams,
		usedFallback: true,
	};
};
