/**
 * BugsTable Component
 *
 * A table-based view for displaying bugs with better data density and scanning capabilities.
 * Clickable rows provide intuitive navigation to bug details for editing and management.
 *
 * Features:
 * - Compact table layout for better scalability
 * - Ticket number as primary column with default sorting
 * - Clickable rows for navigation to bug details
 * - Responsive design with horizontal scroll
 * - Clean, minimal interface focused on data display
 */

import React from "react";
import { format } from "date-fns";
import { Bug } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import type { Bug as BugType } from "@/types/bug";
import {
	BugStatusBadge,
	BugPriorityBadge,
	BugTypeBadge,
} from "./BugStatusBadge";

interface BugsTableProps {
	bugs: BugType[];
	onViewDetails?: (projectTicketNumber: number) => void;
	isLoading?: boolean;
	disabled?: boolean;
	hasActiveFilters?: boolean;
	onClearFilters?: () => void;
	onCreateBug?: () => void;
}

export function BugsTable({
	bugs,
	onViewDetails,
	isLoading = false,
	disabled = false,
	hasActiveFilters = false,
	onClearFilters,
	onCreateBug,
}: BugsTableProps) {
	if (isLoading) {
		return (
			<div className="space-y-3">
				{Array.from({ length: 5 }).map((_, i) => (
					<div key={i} className="h-16 bg-muted animate-pulse rounded-md" />
				))}
			</div>
		);
	}

	if (bugs.length === 0) {
		return (
			<Card className="p-8 text-center">
				<Bug className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
				<h3 className="text-lg font-semibold mb-2">
					{hasActiveFilters ? "No bugs found" : "No bugs yet"}
				</h3>
				<p className="text-muted-foreground mb-4">
					{hasActiveFilters
						? "Try adjusting your search or filters"
						: "Create the first bug to get started"}
				</p>
				{hasActiveFilters ? (
					<Button variant="outline" onClick={onClearFilters}>
						Clear Filters
					</Button>
				) : (
					<Button onClick={onCreateBug}>
						<Bug className="h-4 w-4 mr-2" />
						Create Bug
					</Button>
				)}
			</Card>
		);
	}

	// Get user display name
	const getUserDisplayName = (user: any): string => {
		if (user.firstName && user.lastName) {
			return `${user.firstName} ${user.lastName}`;
		}
		if (user.firstName) {
			return user.firstName;
		}
		if (user.lastName) {
			return user.lastName;
		}
		return user.email || "Unknown User";
	};

	// Get user initials
	const getUserInitials = (user: any): string => {
		if (user.firstName && user.lastName) {
			return `${user.firstName[0]}${user.lastName[0]}`;
		}
		if (user.firstName) {
			return user.firstName[0];
		}
		if (user.lastName) {
			return user.lastName[0];
		}
		return user.email?.[0]?.toUpperCase() || "U";
	};

	return (
		<div className="rounded-lg border border-gray-200 shadow-sm bg-white">
			<Table>
				<TableHeader>
					<TableRow className="border-b">
						<TableHead className="w-[100px] py-3 px-3 text-sm font-medium">
							Ticket #
						</TableHead>
						<TableHead className="w-[280px] py-3 px-3 text-sm font-medium">
							Title
						</TableHead>
						<TableHead className="w-[100px] py-3 px-3 text-sm font-medium">
							Status
						</TableHead>
						<TableHead className="w-[100px] py-3 px-3 text-sm font-medium">
							Priority
						</TableHead>
						<TableHead className="w-[100px] py-3 px-3 text-sm font-medium">
							Type
						</TableHead>
						<TableHead className="w-[120px] py-3 px-3 text-sm font-medium">
							Reporter
						</TableHead>
						<TableHead className="w-[120px] py-3 px-3 text-sm font-medium">
							Assignee
						</TableHead>
						<TableHead className="w-[120px] py-3 px-3 text-sm font-medium">
							Created
						</TableHead>
					</TableRow>
				</TableHeader>
				<TableBody>
					{bugs.map((bug) => (
						<TableRow
							key={bug.projectTicketNumber}
							className="hover:bg-gray-50 cursor-pointer transition-all duration-200 hover:shadow-sm border-b border-gray-100"
							onClick={() => onViewDetails?.(bug.projectTicketNumber)}
							role="button"
							tabIndex={0}
							onKeyDown={(e) => {
								if (e.key === "Enter" || e.key === " ") {
									e.preventDefault();
									onViewDetails?.(bug.projectTicketNumber);
								}
							}}
							aria-label={`View details for bug #${bug.projectTicketNumber}`}
						>
							{/* Ticket Number */}
							<TableCell className="py-3 px-3">
								<div className="flex items-center gap-2">
									<span className="font-mono font-medium text-sm text-blue-600">
										#{bug.projectTicketNumber}
									</span>
								</div>
							</TableCell>

							{/* Title */}
							<TableCell className="py-3 px-3">
								<div className="max-w-[260px]">
									<span className="text-sm font-medium line-clamp-2">
										{bug.title}
									</span>
								</div>
							</TableCell>

							{/* Status */}
							<TableCell className="py-3 px-3">
								<BugStatusBadge status={bug.status} size="sm" />
							</TableCell>

							{/* Priority */}
							<TableCell className="py-3 px-3">
								<BugPriorityBadge priority={bug.priority} size="sm" />
							</TableCell>

							{/* Type */}
							<TableCell className="py-3 px-3">
								<BugTypeBadge type={bug.type} size="sm" />
							</TableCell>

							{/* Reporter */}
							<TableCell className="py-3 px-3">
								<div className="flex items-center gap-2">
									<Avatar className="h-6 w-6">
										<AvatarFallback className="text-xs">
											{getUserInitials(bug.reporter)}
										</AvatarFallback>
									</Avatar>
									<span className="text-sm truncate">
										{getUserDisplayName(bug.reporter)}
									</span>
								</div>
							</TableCell>

							{/* Assignee */}
							<TableCell className="py-3 px-3">
								{bug.assignee ? (
									<div className="flex items-center gap-2">
										<Avatar className="h-6 w-6">
											<AvatarFallback className="text-xs">
												{getUserInitials(bug.assignee)}
											</AvatarFallback>
										</Avatar>
										<span className="text-sm truncate">
											{getUserDisplayName(bug.assignee)}
										</span>
									</div>
								) : (
									<span className="text-sm text-muted-foreground">
										Unassigned
									</span>
								)}
							</TableCell>

							{/* Created Date */}
							<TableCell className="py-3 px-3">
								<span className="text-sm">
									{bug.createdAt
										? format(new Date(bug.createdAt), "dd/MM/yyyy")
										: "N/A"}
								</span>
							</TableCell>
						</TableRow>
					))}
				</TableBody>
			</Table>
		</div>
	);
}
