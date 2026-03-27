/**
 * useSearchParams Hook
 *
 * Custom hook for managing search parameters in the URL.
 * Provides bookmarkable and shareable search states.
 *
 * @param initialParams - Initial search parameters
 * @returns Object with current params and update functions
 */

import { useSearchParams as useRouterSearchParams } from "react-router-dom";
import { useCallback, useMemo } from "react";

export interface SearchParams {
	search?: string;
	filter?: string;
	sortBy?: string;
	sortDir?: "asc" | "desc";
	page?: string;
	create?: string;
	myTeams?: string;
	projectFilter?: string;
	projectSlug?: string;
	viewMode?: string;
	// Bug-specific parameters
	status?: string;
	priority?: string;
	type?: string;
	assignee?: string;
	labels?: string;
}

export function useSearchParams(initialParams: SearchParams = {}) {
	const [searchParams, setSearchParams] = useRouterSearchParams();

	// Get current params from URL or use defaults
	const currentParams = useMemo(() => {
		const params: SearchParams = { ...initialParams };

		// Read from URL params
		const search = searchParams.get("search");
		const filter = searchParams.get("filter");
		const sortBy = searchParams.get("sortBy");
		const sortDir = searchParams.get("sortDir") as "asc" | "desc";
		const page = searchParams.get("page");
		const create = searchParams.get("create");
		const myTeams = searchParams.get("myTeams");
		const projectFilter = searchParams.get("projectFilter");
		const projectSlug = searchParams.get("projectSlug");
		const viewMode = searchParams.get("viewMode");
		const status = searchParams.get("status");
		const priority = searchParams.get("priority");
		const type = searchParams.get("type");
		const assignee = searchParams.get("assignee");
		const labels = searchParams.get("labels");

		if (search !== null) params.search = search;
		if (filter !== null) params.filter = filter;
		if (sortBy !== null) params.sortBy = sortBy;
		if (sortDir !== null) params.sortDir = sortDir;
		if (page !== null) params.page = page;
		if (create !== null) params.create = create;
		if (myTeams !== null) params.myTeams = myTeams;
		if (projectFilter !== null) params.projectFilter = projectFilter;
		if (projectSlug !== null) params.projectSlug = projectSlug;
		if (viewMode !== null) params.viewMode = viewMode;
		if (status !== null) params.status = status;
		if (priority !== null) params.priority = priority;
		if (type !== null) params.type = type;
		if (assignee !== null) params.assignee = assignee;
		if (labels !== null) params.labels = labels;

		return params;
	}, [searchParams, initialParams]);

	// Update a single parameter
	const updateParam = useCallback(
		(key: keyof SearchParams, value: string | undefined) => {
			setSearchParams((prev) => {
				const newParams = new URLSearchParams(prev);

				if (value === undefined || value === "") {
					newParams.delete(key);
				} else {
					newParams.set(key, value);
				}

				// Reset page when changing search/filter/sort (but not when changing page itself)
				if (
					["search", "filter", "sortBy", "sortDir"].includes(key) &&
					key !== "page"
				) {
					newParams.delete("page");
				}

				return newParams;
			});
		},
		[setSearchParams]
	);

	// Update multiple parameters at once
	const updateParams = useCallback(
		(updates: Partial<SearchParams>) => {
			setSearchParams((prev) => {
				const newParams = new URLSearchParams(prev);

				Object.entries(updates).forEach(([key, value]) => {
					if (value === undefined || value === "") {
						newParams.delete(key);
					} else {
						newParams.set(key, value);
					}
				});

				// Reset page when changing search/filter/sort (but not when changing page itself)
				const hasSearchChanges = Object.keys(updates).some(
					(key) =>
						["search", "filter", "sortBy", "sortDir"].includes(key) &&
						key !== "page"
				);

				if (hasSearchChanges) {
					newParams.delete("page");
				}

				return newParams;
			});
		},
		[setSearchParams]
	);

	// Clear all search parameters
	const clearParams = useCallback(() => {
		setSearchParams(new URLSearchParams());
	}, [setSearchParams]);

	// Clear specific parameters
	const clearParam = useCallback(
		(key: keyof SearchParams) => {
			updateParam(key, undefined);
		},
		[updateParam]
	);

	return {
		params: currentParams,
		updateParam,
		updateParams,
		clearParams,
		clearParam,
	};
}
