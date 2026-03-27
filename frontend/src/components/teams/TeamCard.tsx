/**
 * TeamCard Component
 *
 * A reusable card component for displaying team information with actions.
 * Follows the existing design patterns and includes responsive design.
 *
 * Features:
 * - Team information display (name, description, member count, etc.)
 * - Role-based action buttons
 * - Loading states
 * - Responsive design
 * - Accessibility features
 */

import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { RoleBadge } from "./RoleBadge";
import {
	Users,
	Lock,
	Globe,
	Settings,
	UserPlus,
	LogOut,
	Eye,
	Trash2,
	FolderOpen,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { TeamRole } from "@/types/team";
import type { TeamCardProps } from "@/types/team";

export function TeamCard({
	team,
	onJoin,
	onLeave,
	onEdit,
	onDelete,
	onViewDetails,
	isLoading = false,
	disabled = false,
}: TeamCardProps) {
	const currentUserRole = team.currentUserRole;
	const isAdmin = currentUserRole === TeamRole.ADMIN;
	const isMember = currentUserRole !== null;

	const handleJoin = () => {
		if (onJoin && !disabled && !isLoading) {
			onJoin(team.id);
		}
	};

	const handleLeave = () => {
		if (onLeave && !disabled && !isLoading) {
			onLeave(team.id);
		}
	};

	const handleEdit = () => {
		if (onEdit && !disabled && !isLoading) {
			onEdit(team.id);
		}
	};

	const handleDelete = () => {
		if (onDelete && !disabled && !isLoading) {
			onDelete(team.id);
		}
	};

	const handleViewDetails = () => {
		if (onViewDetails && !disabled && !isLoading) {
			onViewDetails(team.id);
		}
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
							<CardTitle className="text-lg truncate">{team.name}</CardTitle>
							{currentUserRole && (
								<RoleBadge role={currentUserRole} size="sm" />
							)}
						</div>
						<CardDescription className="line-clamp-1 mb-2">
							{team.description || "No description available"}
						</CardDescription>
						{/* Project Information */}
						{team.projectSlug && (
							<div className="flex items-center gap-1 text-sm text-muted-foreground">
								<FolderOpen className="h-3 w-3" />
								<span>Project: {team.projectSlug}</span>
							</div>
						)}
					</div>

					{/* Team Stats and Actions */}
					<div className="flex items-center gap-6">
						{/* Team Stats */}
						<div className="flex items-center gap-4 text-sm text-muted-foreground">
							<div className="flex items-center gap-1">
								<Users className="h-4 w-4" />
								<span>
									{team.memberCount} member{team.memberCount !== 1 ? "s" : ""}
								</span>
							</div>
							<div className="flex items-center gap-1">
								<Avatar className="h-5 w-5">
									<AvatarFallback className="text-xs">
										{team.creatorName
											.split(" ")
											.map((n) => n[0])
											.join("")}
									</AvatarFallback>
								</Avatar>
								<span>by {team.creatorName}</span>
							</div>
						</div>

						{/* Action Buttons */}
						<div className="flex items-center gap-2">
							{/* View Details - Always available if callback provided */}
							{onViewDetails && (
								<Button
									variant="outline"
									size="sm"
									onClick={handleViewDetails}
									disabled={disabled || isLoading}
								>
									<Eye className="h-4 w-4 mr-1" />
									View
								</Button>
							)}

							{/* Join Team - Only for non-members */}
							{!isMember && onJoin && (
								<Button
									variant="default"
									size="sm"
									onClick={handleJoin}
									disabled={disabled || isLoading}
								>
									<UserPlus className="h-4 w-4 mr-1" />
									Join
								</Button>
							)}

							{/* Member Actions */}
							{isMember && (
								<>
									{/* Edit Team - Only for admins */}
									{isAdmin && onEdit && (
										<Button
											variant="outline"
											size="sm"
											onClick={handleEdit}
											disabled={disabled || isLoading}
										>
											<Settings className="h-4 w-4" />
											<span className="sr-only">Edit team settings</span>
										</Button>
									)}

									{/* Leave Team - For all members */}
									{onLeave && (
										<Button
											variant="outline"
											size="sm"
											onClick={handleLeave}
											disabled={disabled || isLoading}
										>
											<LogOut className="h-4 w-4" />
											<span className="sr-only">Leave team</span>
										</Button>
									)}

									{/* Delete Team - Only for admins */}
									{isAdmin && onDelete && (
										<Button
											variant="outline"
											size="sm"
											onClick={handleDelete}
											disabled={disabled || isLoading}
											className="text-destructive hover:text-destructive"
										>
											<Trash2 className="h-4 w-4" />
											<span className="sr-only">Delete team</span>
										</Button>
									)}
								</>
							)}
						</div>
					</div>
				</div>

				{/* Loading state overlay */}
				{isLoading && (
					<div className="absolute inset-0 bg-background/50 flex items-center justify-center">
						<div className="flex items-center gap-2 text-sm text-muted-foreground">
							<div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
							<span>Loading...</span>
						</div>
					</div>
				)}
			</CardContent>
		</Card>
	);
}

// Loading skeleton component
export function TeamCardSkeleton() {
	return (
		<Card className="animate-pulse">
			<CardContent className="p-6">
				<div className="flex items-center justify-between">
					<div className="flex-1 min-w-0">
						<div className="h-6 bg-muted rounded w-3/4 mb-2"></div>
						<div className="h-4 bg-muted rounded w-full mb-2"></div>
						<div className="h-4 bg-muted rounded w-1/2"></div>
					</div>
					<div className="flex items-center gap-6">
						<div className="flex items-center gap-4">
							<div className="h-4 bg-muted rounded w-24"></div>
							<div className="h-4 bg-muted rounded w-32"></div>
						</div>
						<div className="flex items-center gap-2">
							<div className="h-8 bg-muted rounded w-16"></div>
							<div className="h-8 bg-muted rounded w-16"></div>
						</div>
					</div>
				</div>
			</CardContent>
		</Card>
	);
}
