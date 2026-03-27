/**
 * ProjectTeamCard Component
 *
 * A reusable card component for displaying team information within a project context.
 * Adapted from TeamCard with project-scoped navigation and actions.
 *
 * Features:
 * - Team information display with project context
 * - Project-scoped action buttons
 * - Role-based permissions
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
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
	Users,
	Settings,
	UserPlus,
	LogOut,
	Eye,
	Trash2,
	FolderOpen,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { TeamRole } from "@/types/team";
import { RoleBadge } from "@/components/teams/RoleBadge";
import type { ProjectTeamCardProps } from "@/types/project";

export function ProjectTeamCard({
	team,
	projectSlug,
	onEdit,
	onDelete,
	onViewDetails,
	isLoading = false,
	disabled = false,
}: ProjectTeamCardProps) {
	const currentUserRole = team.currentUserRole;
	const isAdmin = currentUserRole === TeamRole.ADMIN;
	const isMember = currentUserRole !== null;

	const handleEdit = () => {
		if (onEdit && !disabled && !isLoading) {
			onEdit(projectSlug, team.teamSlug);
		}
	};

	const handleDelete = () => {
		if (onDelete && !disabled && !isLoading) {
			onDelete(projectSlug, team.teamSlug);
		}
	};

	const handleViewDetails = () => {
		if (onViewDetails && !disabled && !isLoading) {
			onViewDetails(projectSlug, team.teamSlug);
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
			<CardHeader className="pb-3">
				<div className="flex items-start justify-between">
					<div className="flex-1 min-w-0">
						<div className="flex items-center gap-2 mb-1">
							<CardTitle className="text-lg truncate">{team.name}</CardTitle>
						</div>
						<CardDescription className="line-clamp-2">
							{team.description || "No description available"}
						</CardDescription>
					</div>

					{currentUserRole && <RoleBadge role={currentUserRole} size="sm" />}
				</div>
			</CardHeader>

			<CardContent className="space-y-4">
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

				{/* Team slug with project context */}
				<div className="text-xs text-muted-foreground font-mono bg-muted/50 px-2 py-1 rounded">
					{projectSlug}/{team.teamSlug}
				</div>

				{/* Action Buttons */}
				<div className="flex items-center gap-2 pt-2">
					{/* View Details - Always available if callback provided */}
					{onViewDetails && (
						<Button
							variant="outline"
							size="sm"
							onClick={handleViewDetails}
							disabled={disabled || isLoading}
							className="flex-1"
						>
							<Eye className="h-4 w-4 mr-1" />
							View Details
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

				{/* Loading state overlay */}
				{isLoading && (
					<div className="absolute inset-0 bg-background/50 flex items-center justify-center">
						<div className="flex items-center gap-2 text-sm text-muted-foreground">
							<div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
							<span>Loading...</span>
						</div>
					</div>
				)}

				{/* Timestamps */}
				{team.createdAt && (
					<div className="text-xs text-muted-foreground">
						Created {new Date(team.createdAt).toLocaleDateString()}
					</div>
				)}
			</CardContent>
		</Card>
	);
}

// Loading skeleton component
export function ProjectTeamCardSkeleton() {
	return (
		<Card className="animate-pulse">
			<CardHeader className="pb-3">
				<div className="flex items-start justify-between">
					<div className="flex-1 min-w-0">
						<div className="h-6 bg-muted rounded w-3/4 mb-2"></div>
						<div className="h-4 bg-muted rounded w-full"></div>
					</div>
					<div className="h-6 bg-muted rounded w-16"></div>
				</div>
			</CardHeader>
			<CardContent className="space-y-4">
				<div className="flex items-center gap-4">
					<div className="h-4 bg-muted rounded w-24"></div>
					<div className="h-4 bg-muted rounded w-32"></div>
				</div>
				<div className="h-6 bg-muted rounded w-full"></div>
				<div className="flex items-center gap-2 pt-2">
					<div className="h-8 bg-muted rounded flex-1"></div>
					<div className="h-8 bg-muted rounded w-16"></div>
				</div>
			</CardContent>
		</Card>
	);
}
