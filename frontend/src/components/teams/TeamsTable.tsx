/**
 * TeamsTable Component
 *
 * A unified, scalable table component for displaying teams with consistent
 * join/leave functionality. This replaces the card-based implementations
 * and provides better scalability for projects with many teams.
 *
 * Features:
 * - Table layout for better data density
 * - Consistent join/leave functionality
 * - Role-based action buttons
 * - Search and filtering capabilities
 * - Responsive design
 */

import React from "react";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Users, UserPlus, LogOut, Settings, Trash2, Eye } from "lucide-react";
import { TeamRole } from "@/types/team";
import type { Team } from "@/types/team";

interface TeamsTableProps {
	teams: Team[];
	onJoin?: (teamId: string) => void;
	onLeave?: (teamId: string) => void;
	onEdit?: (teamId: string) => void;
	onDelete?: (teamId: string) => void;
	onViewDetails?: (teamSlug: string) => void;
	loading?: boolean;
	disabled?: boolean;
}

export function TeamsTable({
	teams,
	onJoin,
	onLeave,
	onEdit,
	onDelete,
	onViewDetails,
	loading = false,
	disabled = false,
}: TeamsTableProps) {
	const getRoleBadge = (role: TeamRole | null) => {
		if (!role) return null;

		const variant = role === TeamRole.ADMIN ? "default" : "secondary";
		return (
			<Badge variant={variant} className="text-xs">
				{role}
			</Badge>
		);
	};

	const formatDate = (dateString: string | null) => {
		if (!dateString) return "N/A";
		return new Date(dateString).toLocaleDateString();
	};

	return (
		<div className="space-y-4">
			{/* Teams Table */}
			<div className="border rounded-lg">
				<Table>
					<TableHeader>
						<TableRow>
							<TableHead>Team</TableHead>
							<TableHead>Description</TableHead>
							<TableHead>Members</TableHead>
							<TableHead>Created</TableHead>
							<TableHead>Your Role</TableHead>
							<TableHead className="text-right">Actions</TableHead>
						</TableRow>
					</TableHeader>
					<TableBody>
						{teams.map((team) => {
							const isMember = team.currentUserRole !== null;
							const isAdmin = team.currentUserRole === TeamRole.ADMIN;

							return (
								<TableRow key={team.teamSlug} className="hover:bg-muted/50">
									<TableCell>
										<div className="flex items-center gap-2">
											<Users className="h-4 w-4 text-muted-foreground" />
											<div>
												<div className="font-medium">{team.name}</div>
												<div className="text-sm text-muted-foreground">
													{team.teamSlug}
												</div>
											</div>
										</div>
									</TableCell>
									<TableCell>
										<div className="max-w-xs">
											{team.description || (
												<span className="text-muted-foreground italic">
													No description
												</span>
											)}
										</div>
									</TableCell>
									<TableCell>
										<div className="text-center">
											<span className="font-medium">
												{team.memberCount || 0}
											</span>
										</div>
									</TableCell>
									<TableCell>
										<div className="text-sm">
											<div>{formatDate(team.createdAt)}</div>
											{team.creatorName && (
												<div className="text-muted-foreground text-xs">
													by {team.creatorName}
												</div>
											)}
										</div>
									</TableCell>
									<TableCell>{getRoleBadge(team.currentUserRole)}</TableCell>
									<TableCell className="text-right">
										<div className="flex items-center justify-end gap-2">
											{/* View Button - Always available */}
											{onViewDetails && (
												<Button
													variant="outline"
													size="sm"
													onClick={() => onViewDetails(team.teamSlug)}
													disabled={disabled || loading}
												>
													<Eye className="h-4 w-4 mr-1" />
													View
												</Button>
											)}

											{/* Join/Leave Buttons */}
											{!isMember && onJoin && (
												<Button
													variant="default"
													size="sm"
													onClick={() => onJoin(team.id)}
													disabled={disabled || loading}
												>
													<UserPlus className="h-4 w-4 mr-1" />
													Join
												</Button>
											)}

											{isMember && onLeave && (
												<Button
													variant="outline"
													size="sm"
													onClick={() => onLeave(team.id)}
													disabled={disabled || loading}
												>
													<LogOut className="h-4 w-4 mr-1" />
													Leave
												</Button>
											)}

											{/* Admin Actions */}
											{isAdmin && onEdit && (
												<Button
													variant="outline"
													size="sm"
													onClick={() => onEdit(team.id)}
													disabled={disabled || loading}
												>
													<Settings className="h-4 w-4" />
													<span className="sr-only">Edit team</span>
												</Button>
											)}

											{isAdmin && onDelete && (
												<Button
													variant="destructive"
													size="sm"
													onClick={() => onDelete(team.id)}
													disabled={disabled || loading}
												>
													<Trash2 className="h-4 w-4" />
													<span className="sr-only">Delete team</span>
												</Button>
											)}
										</div>
									</TableCell>
								</TableRow>
							);
						})}
					</TableBody>
				</Table>

				{/* Empty State */}
				{teams.length === 0 && !loading && (
					<div className="text-center py-12">
						<Users className="h-16 w-16 mx-auto mb-4 text-muted-foreground opacity-50" />
						<h3 className="text-lg font-semibold mb-2">No teams found</h3>
						<p className="text-muted-foreground">
							No teams have been created yet.
						</p>
					</div>
				)}

				{/* Loading State */}
				{loading && (
					<div className="text-center py-8">
						<div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
						<p className="mt-2 text-muted-foreground">Loading teams...</p>
					</div>
				)}
			</div>

			{/* Results Count */}
			{teams.length > 0 && (
				<div className="text-center text-sm text-muted-foreground">
					Found {teams.length} team{teams.length !== 1 ? "s" : ""}
				</div>
			)}
		</div>
	);
}
