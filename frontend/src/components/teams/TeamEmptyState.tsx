/**
 * TeamEmptyState Component
 *
 * Handles empty states for teams with different scenarios:
 * - No teams at all
 * - No teams matching search/filters
 * - Loading state
 * - Error state
 */

import React from "react";
import {
	Users,
	Search,
	Filter,
	Plus,
	AlertCircle,
	Loader2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export interface TeamEmptyStateProps {
	type: "no-teams" | "no-results" | "loading" | "error";
	searchTerm?: string;
	filter?: string;
	onCreateTeam?: () => void;
	onClearFilters?: () => void;
	onRetry?: () => void;
	error?: string;
	className?: string;
}

export function TeamEmptyState({
	type,
	searchTerm,
	filter,
	onCreateTeam,
	onClearFilters,
	onRetry,
	error,
	className,
}: TeamEmptyStateProps) {
	const getContent = () => {
		switch (type) {
			case "loading":
				return {
					icon: Loader2,
					title: "Loading teams...",
					description: "Please wait while we fetch your teams.",
					iconClassName: "animate-spin",
				};

			case "error":
				return {
					icon: AlertCircle,
					title: "Failed to load teams",
					description: error || "Something went wrong while loading teams.",
					action: onRetry && (
						<Button onClick={onRetry} variant="outline">
							Try Again
						</Button>
					),
				};

			case "no-results":
				const hasFilters = searchTerm || (filter && filter !== "all");
				return {
					icon: Search,
					title: "No teams found",
					description: hasFilters
						? "No teams match your current search and filters."
						: "No teams available at the moment.",
					action: hasFilters && onClearFilters && (
						<Button onClick={onClearFilters} variant="outline">
							Clear Filters
						</Button>
					),
				};

			case "no-teams":
			default:
				return {
					icon: Users,
					title: "No teams yet",
					description:
						"Get started by creating your first team to collaborate with others.",
					action: onCreateTeam && (
						<Button onClick={onCreateTeam}>
							<Plus className="h-4 w-4 mr-2" />
							Create Your First Team
						</Button>
					),
				};
		}
	};

	const content = getContent();
	const Icon = content.icon;

	return (
		<Card className={cn("", className)}>
			<CardContent className="p-8 text-center">
				<div className="flex flex-col items-center gap-4">
					<Icon
						className={cn(
							"h-12 w-12 text-muted-foreground",
							content.iconClassName
						)}
					/>
					<div className="space-y-2">
						<h3 className="text-lg font-semibold">{content.title}</h3>
						<p className="text-muted-foreground max-w-sm">
							{content.description}
						</p>
					</div>
					{content.action && <div className="mt-4">{content.action}</div>}
				</div>
			</CardContent>
		</Card>
	);
}
