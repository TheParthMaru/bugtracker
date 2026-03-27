/**
 * BugCard Component
 *
 * A reusable card component for displaying bug information with actions.
 * Follows the same design pattern as other card components with horizontal layout.
 *
 * Features:
 * - Bug information display (title, description, status, priority, etc.)
 * - Role-based action buttons (edit, delete, view details, assign)
 * - Loading states and skeleton components
 * - Status and priority badges
 * - Responsive design
 * - Accessibility features
 */

import React from "react";
import {
	Card,
	CardContent,
	CardDescription,
	CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
	MessageSquare,
	Paperclip,
	Calendar,
	User,
	Settings,
	Eye,
	UserPlus,
	Trash2,
	Edit,
	Edit3,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { BugCardProps } from "@/types/bug";
import {
	BugStatusBadge,
	BugPriorityBadge,
	BugTypeBadge,
	BugLabelBadge,
} from "./BugStatusBadge";

export function BugCard({
	bug,
	onEdit,
	onDelete,
	onViewDetails,
	onAssign,
	onStatusChange,
	isLoading = false,
	disabled = false,
}: BugCardProps) {
	// Handle action buttons
	const handleEdit = () => {
		if (onEdit && !disabled && !isLoading) {
			onEdit(bug.projectTicketNumber);
		}
	};

	const handleDelete = () => {
		if (onDelete && !disabled && !isLoading) {
			onDelete(bug.projectTicketNumber);
		}
	};

	const handleViewDetails = () => {
		if (onViewDetails && !disabled && !isLoading) {
			onViewDetails(bug.projectTicketNumber);
		}
	};

	const handleAssign = () => {
		if (onAssign && !disabled && !isLoading) {
			onAssign(bug.projectTicketNumber);
		}
	};

	// Format dates
	const formatDate = (dateString: string | null): string => {
		if (!dateString) return "Unknown";
		return new Date(dateString).toLocaleDateString("en-US", {
			year: "numeric",
			month: "short",
			day: "numeric",
		});
	};

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

	// Truncate description
	const truncateDescription = (
		description: string,
		maxLength: number = 100
	): string => {
		if (description.length <= maxLength) {
			return description;
		}
		return description.substring(0, maxLength) + "...";
	};

	return (
		<Card
			className={cn(
				"relative transition-all duration-200 hover:shadow-md",
				disabled && "opacity-50 cursor-not-allowed",
				isLoading && "animate-pulse"
			)}
		>
			<CardContent className="p-6">
				<div className="flex items-center justify-between">
					<div className="flex-1 min-w-0">
						<div className="flex items-center gap-3 mb-2">
							<CardTitle className="text-lg truncate">{bug.title}</CardTitle>
							<BugStatusBadge status={bug.status} size="sm" />
							<BugPriorityBadge priority={bug.priority} size="sm" />
							<BugTypeBadge type={bug.type} size="sm" />
						</div>

						<CardDescription className="text-sm text-gray-600 mb-3">
							{truncateDescription(bug.description)}
						</CardDescription>

						<div className="flex items-center gap-4 text-sm text-gray-500 mb-3">
							<div className="flex items-center gap-1">
								<User className="h-4 w-4" />
								<span>Reporter: {getUserDisplayName(bug.reporter)}</span>
							</div>
							{bug.assignee && (
								<div className="flex items-center gap-1">
									<UserPlus className="h-4 w-4" />
									<span>Assignee: {getUserDisplayName(bug.assignee)}</span>
								</div>
							)}
							<div className="flex items-center gap-1">
								<Calendar className="h-4 w-4" />
								<span>Created: {formatDate(bug.createdAt)}</span>
							</div>
						</div>

						{/* Labels */}
						{bug.labels && bug.labels.length > 0 && (
							<div className="flex flex-wrap gap-1 mb-3">
								{bug.labels.slice(0, 3).map((label) => (
									<BugLabelBadge key={label.id} label={label} size="sm" />
								))}
								{bug.labels.length > 3 && (
									<span className="text-xs text-gray-500">
										+{bug.labels.length - 3} more
									</span>
								)}
							</div>
						)}

						{/* Tags */}
						{bug.tags && bug.tags.length > 0 && (
							<div className="flex flex-wrap gap-1 mb-3">
								{bug.tags.slice(0, 5).map((tag) => (
									<span
										key={tag}
										className="px-2 py-1 bg-gray-100 text-gray-700 rounded-full text-xs font-medium border border-gray-200"
									>
										{tag}
									</span>
								))}
								{bug.tags.length > 5 && (
									<span className="text-xs text-gray-500">
										+{bug.tags.length - 5} more
									</span>
								)}
							</div>
						)}

						{/* Team Assignments */}
						{bug.assignedTeams && bug.assignedTeams.length > 0 && (
							<div className="flex flex-wrap gap-1 mb-3">
								{bug.assignedTeams.slice(0, 3).map((team) => (
									<span
										key={team.teamId}
										className={cn(
											"px-2 py-1 rounded-full text-xs font-medium border",
											team.isPrimary
												? "bg-blue-100 text-blue-700 border-blue-200"
												: "bg-purple-100 text-purple-700 border-purple-200"
										)}
									>
										{team.teamName}
										{team.isPrimary && " (Primary)"}
									</span>
								))}
								{bug.assignedTeams.length > 3 && (
									<span className="text-xs text-gray-500">
										+{bug.assignedTeams.length - 3} more teams
									</span>
								)}
							</div>
						)}

						{/* Stats */}
						<div className="flex items-center gap-4 text-xs text-gray-500">
							<div className="flex items-center gap-1">
								<MessageSquare className="h-3 w-3" />
								<span>{bug.commentCount} comments</span>
							</div>
							<div className="flex items-center gap-1">
								<Paperclip className="h-3 w-3" />
								<span>{bug.attachmentCount} attachments</span>
							</div>
						</div>
					</div>

					{/* Action buttons */}
					<div className="flex items-center gap-2 ml-4">
						<Button
							variant="outline"
							size="sm"
							onClick={handleViewDetails}
							disabled={disabled || isLoading}
							className="flex items-center gap-1"
						>
							<Eye className="h-3 w-3" />
							View
						</Button>

						{onEdit && (
							<Button
								variant="outline"
								size="sm"
								onClick={handleEdit}
								disabled={disabled || isLoading}
								className="flex items-center gap-1"
							>
								<Edit className="h-3 w-3" />
								Edit
							</Button>
						)}

						{onAssign && !bug.assignee && (
							<Button
								variant="outline"
								size="sm"
								onClick={handleAssign}
								disabled={disabled || isLoading}
								className="flex items-center gap-1"
							>
								<UserPlus className="h-3 w-3" />
								Assign
							</Button>
						)}

						{onDelete && (
							<Button
								variant="outline"
								size="sm"
								onClick={handleDelete}
								disabled={disabled || isLoading}
								className="flex items-center gap-1 text-red-600 hover:text-red-700 hover:bg-red-50"
							>
								<Trash2 className="h-3 w-3" />
								Delete
							</Button>
						)}
					</div>
				</div>
			</CardContent>
		</Card>
	);
}

export function BugCardSkeleton() {
	return (
		<Card className="animate-pulse">
			<CardContent className="p-6">
				<div className="flex items-center justify-between">
					<div className="flex-1 min-w-0">
						<div className="flex items-center gap-3 mb-2">
							<div className="h-6 bg-gray-200 rounded w-48"></div>
							<div className="h-5 bg-gray-200 rounded w-16"></div>
							<div className="h-5 bg-gray-200 rounded w-20"></div>
							<div className="h-5 bg-gray-200 rounded w-24"></div>
						</div>

						<div className="h-4 bg-gray-200 rounded w-full mb-3"></div>

						<div className="flex items-center gap-4 mb-3">
							<div className="h-4 bg-gray-200 rounded w-32"></div>
							<div className="h-4 bg-gray-200 rounded w-36"></div>
							<div className="h-4 bg-gray-200 rounded w-28"></div>
						</div>

						<div className="flex flex-wrap gap-1 mb-3">
							<div className="h-5 bg-gray-200 rounded w-16"></div>
							<div className="h-5 bg-gray-200 rounded w-20"></div>
							<div className="h-5 bg-gray-200 rounded w-18"></div>
						</div>

						<div className="flex items-center gap-4">
							<div className="h-3 bg-gray-200 rounded w-20"></div>
							<div className="h-3 bg-gray-200 rounded w-24"></div>
						</div>
					</div>

					<div className="flex items-center gap-2 ml-4">
						<div className="h-8 bg-gray-200 rounded w-16"></div>
						<div className="h-8 bg-gray-200 rounded w-16"></div>
						<div className="h-8 bg-gray-200 rounded w-16"></div>
					</div>
				</div>
			</CardContent>
		</Card>
	);
}
