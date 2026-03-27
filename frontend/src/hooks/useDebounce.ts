/**
 * useDebounce Hook
 *
 * Custom hook for debouncing values to optimize performance.
 * Useful for search inputs, API calls, and other operations that should
 * be delayed to avoid excessive processing.
 *
 * @param value - The value to debounce
 * @param delay - The delay in milliseconds (default: 300ms)
 * @returns The debounced value
 */

import { useState, useEffect } from "react";

export function useDebounce<T>(value: T, delay: number = 300): T {
	const [debouncedValue, setDebouncedValue] = useState<T>(value);

	useEffect(() => {
		const handler = setTimeout(() => {
			setDebouncedValue(value);
		}, delay);

		return () => {
			clearTimeout(handler);
		};
	}, [value, delay]);

	return debouncedValue;
}
