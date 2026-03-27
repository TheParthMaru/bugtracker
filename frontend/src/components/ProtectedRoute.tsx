/**
 * ProtectedRoute Component
 *
 * This component is used to protect routes that require authentication.
 * It checks for a valid authentication token in localStorage and redirects to /auth/login if not found.
 *
 * This redirects users to /auth/login if they aren’t authenticated.
 *
 * Usage:
 * <ProtectedRoute>
 *   <YourComponent />
 * </ProtectedRoute>
 */

import React from "react";
import { Navigate } from "react-router-dom";
import type { ReactNode } from "react";

export default function ProtectedRoute({ children }: { children: ReactNode }) {
	const token = localStorage.getItem("bugtracker_token");

	if (!token) {
		return <Navigate to="/auth/login" replace />;
	}

	return <>{children}</>;
}
