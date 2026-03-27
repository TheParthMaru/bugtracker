/**
 * ProjectsTable Component
 *
 * A table-based view for displaying projects with better data density and scanning capabilities.
 * Provides inline actions and sortable columns for efficient project management.
 *
 * Features:
 * - Compact table layout for better scalability
 * - Sortable columns (integrates with existing sorting logic)
 * - Inline action buttons
 * - Responsive design with horizontal scroll
 * - Consistent with existing project management functionality
 */

import React from "react";
import { format } from "date-fns";
import { Eye, UserPlus, FolderPlus, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { ProjectRole, type Project } from "@/types/project";
import { ProjectRoleBadge } from "./ProjectRoleBadge";

interface ProjectsTableProps {
	projects: Project[];
	onJoin?: (projectId: string) => void;
	onViewDetails?: (projectId: string) => void;
	isLoading?: boolean;
	disabled?: boolean;
	hasActiveFilters?: boolean;
	onClearFilters?: () => void;
	onCreateProject?: () => void;
}

export function ProjectsTable({
	projects,
	onJoin,
	onViewDetails,
	isLoading = false,
	disabled = false,
	hasActiveFilters = false,
	onClearFilters,
	onCreateProject,
}: ProjectsTableProps) {
	if (isLoading) {
		return (
			<div className="space-y-3">
				{Array.from({ length: 5 }).map((_, i) => (
					<div key={i} className="h-16 bg-muted animate-pulse rounded-md" />
				))}
			</div>
		);
	}

	if (projects.length === 0) {
		return (
			<Card className="p-8 text-center">
				<FolderPlus className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
				<h3 className="text-lg font-semibold mb-2">
					{hasActiveFilters ? "No projects found" : "No projects yet"}
				</h3>
				<p className="text-muted-foreground mb-4">
					{hasActiveFilters
						? "Try adjusting your search or filters"
						: "Create the first project to get started"}
				</p>
				{hasActiveFilters ? (
					<Button variant="outline" onClick={onClearFilters}>
						Clear Filters
					</Button>
				) : (
					<Button onClick={onCreateProject}>
						<FolderPlus className="h-4 w-4 mr-2" />
						Create Project
					</Button>
				)}
			</Card>
		);
	}

	return (
		<div className="rounded-lg border border-gray-200 shadow-sm bg-white">
			<Table>
				<TableHeader>
					<TableRow className="border-b">
						<TableHead className="w-[220px] py-3 px-3 text-sm font-medium">
							Project
						</TableHead>
						<TableHead className="w-[280px] py-3 px-3 text-sm font-medium">
							Description
						</TableHead>
						<TableHead className="w-[80px] py-3 px-3 text-sm font-medium text-center">
							Members
						</TableHead>
						<TableHead className="w-[160px] py-3 px-3 text-sm font-medium">
							Created
						</TableHead>
						<TableHead className="w-[90px] py-3 px-3 text-sm font-medium">
							Role
						</TableHead>
						<TableHead className="w-[100px] py-3 px-3 text-sm font-medium text-center">
							Actions
						</TableHead>
					</TableRow>
				</TableHeader>
				<TableBody>
					{projects.map((project) => {
						const hasRole = !!project.userRole;

						return (
							<TableRow
								key={project.id}
								className="hover:bg-gray-50 cursor-pointer transition-all duration-200 hover:shadow-sm border-b border-gray-100"
								onClick={() => onViewDetails?.(project.id)}
								role="button"
								tabIndex={0}
								onKeyDown={(e) => {
									if (e.key === "Enter" || e.key === " ") {
										e.preventDefault();
										onViewDetails?.(project.id);
									}
								}}
								aria-label={`View details for ${project.name}`}
							>
								<TableCell className="font-medium py-3 px-3">
									<div className="flex items-center gap-2">
										<Users className="h-4 w-4 text-muted-foreground" />
										<div className="flex flex-col">
											<span className="truncate font-medium">
												{project.name}
											</span>
											<span className="text-xs text-muted-foreground truncate">
												{project.projectSlug}
											</span>
										</div>
									</div>
								</TableCell>
								<TableCell className="py-3 px-3">
									<div className="max-w-[260px]">
										<span className="text-sm text-muted-foreground line-clamp-2">
											{project.description || "No description available"}
										</span>
									</div>
								</TableCell>
								<TableCell className="text-center py-3 px-3">
									<span className="text-sm">{project.memberCount || 0}</span>
								</TableCell>
								<TableCell className="py-3 px-3">
									<div className="flex flex-col">
										<span className="text-sm">
											{project.createdAt
												? format(new Date(project.createdAt), "dd/MM/yyyy")
												: "N/A"}
										</span>
										<span className="text-xs text-muted-foreground">
											by{" "}
											{project.adminFirstName && project.adminLastName
												? `${project.adminFirstName} ${project.adminLastName}`
												: project.adminFirstName ||
												  project.adminLastName ||
												  "Unknown"}
										</span>
									</div>
								</TableCell>
								<TableCell className="py-3 px-3">
									{hasRole ? (
										<ProjectRoleBadge role={project.userRole!} />
									) : (
										<span className="text-sm text-muted-foreground">-</span>
									)}
								</TableCell>
								<TableCell className="py-3 px-3">
									<div className="flex items-center justify-center gap-2">
										{/* View Details Button */}
										<Button
											variant="outline"
											size="sm"
											onClick={(e) => {
												e.stopPropagation();
												onViewDetails?.(project.id);
											}}
											disabled={disabled}
											className="h-7 px-2 text-xs"
										>
											<Eye className="h-3 w-3 mr-1" />
											View
										</Button>

										{/* Join Button - Only show if user is not a member */}
										{!hasRole && (
											<Button
												variant="outline"
												size="sm"
												onClick={(e) => {
													e.stopPropagation();
													onJoin?.(project.id);
												}}
												disabled={disabled}
												className="h-7 px-2 text-xs"
											>
												<UserPlus className="h-3 w-3 mr-1" />
												Join
											</Button>
										)}
									</div>
								</TableCell>
							</TableRow>
						);
					})}
				</TableBody>
			</Table>
		</div>
	);
}
