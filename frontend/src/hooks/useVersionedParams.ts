import { useParams, useLocation } from "react-router-dom";

/**
 * Extracts route parameters, with a pathname fallback if useParams is empty
 * (e.g. unusual navigation or edge cases).
 *
 * @returns Extracted parameters and whether manual parsing was used
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

	// Expected segment pattern: .../projects/:projectSlug/bugs/:projectTicketNumber
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
