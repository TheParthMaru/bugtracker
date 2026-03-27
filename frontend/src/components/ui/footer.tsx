/**
 * Footer Component
 *
 * Reusable footer component for consistent footer across all pages.
 * Includes copyright information and branding.
 */

import React from "react";

export function Footer() {
	return (
		<footer className="border-t py-6 mt-auto">
			<div className="container mx-auto px-4">
				<p className="text-center text-sm text-muted-foreground">
					© 2025 BugTracker. Made with ♥️ By Parth Maru. All rights reserved.
				</p>
			</div>
		</footer>
	);
}
