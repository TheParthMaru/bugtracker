/**
 * TeamSearchFilters Component
 *
 * Comprehensive search and filtering component for teams with:
 * - Real-time search with debouncing
 * - Multiple filter options
 * - Sorting capabilities
 * - Clear all filters functionality
 * - URL state management
 * - Accessibility support
 */

import React from "react";
import {
	Search,
	Filter,
	X,
	SortAsc,
	SortDesc,
	Users,
	Globe,
	Lock,
	Shield,
	User,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger,
	DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

// Filter options
export const FILTER_OPTIONS = {
	all: { label: "All Teams", icon: Users },
	admin: { label: "Teams I Admin", icon: Shield },
	member: { label: "Teams I'm In", icon: User },
} as const;

// Sort options
export const SORT_OPTIONS = {
	name_asc: { label: "Name (A-Z)", field: "name", direction: "asc" },
	name_desc: { label: "Name (Z-A)", field: "name", direction: "desc" },
	created_asc: {
		label: "Created (Oldest)",
		field: "createdAt",
		direction: "asc",
	},
	created_desc: {
		label: "Created (Newest)",
		field: "createdAt",
		direction: "desc",
	},
	members_asc: {
		label: "Members (Low to High)",
		field: "memberCount",
		direction: "asc",
	},
	members_desc: {
		label: "Members (High to Low)",
		field: "memberCount",
		direction: "desc",
	},
} as const;

export interface TeamSearchFiltersProps {
	searchTerm: string;
	onSearchChange: (value: string) => void;
	filter: keyof typeof FILTER_OPTIONS;
	onFilterChange: (value: keyof typeof FILTER_OPTIONS) => void;
	sortBy: keyof typeof SORT_OPTIONS;
	onSortChange: (value: keyof typeof SORT_OPTIONS) => void;
	onClearAll: () => void;
	hasActiveFilters: boolean;
	isLoading?: boolean;
}

export function TeamSearchFilters({
	searchTerm,
	onSearchChange,
	filter,
	onFilterChange,
	sortBy,
	onSortChange,
	onClearAll,
	hasActiveFilters,
	isLoading = false,
}: TeamSearchFiltersProps) {
	const handleClearSearch = () => {
		onSearchChange("");
	};

	const handleClearFilter = () => {
		onFilterChange("all");
	};

	const handleClearSort = () => {
		onSortChange("name_asc");
	};

	return (
		<Card>
			<CardContent className="p-4">
				<div className="flex flex-col gap-4">
					{/* Search Bar */}
					<div className="relative">
						<Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
						<Input
							placeholder="Search teams by name or description..."
							value={searchTerm}
							onChange={(e) => onSearchChange(e.target.value)}
							className="pl-10 pr-10"
							disabled={isLoading}
							aria-label="Search teams"
						/>
						{searchTerm && (
							<Button
								variant="ghost"
								size="sm"
								onClick={handleClearSearch}
								className="absolute right-1 top-1/2 transform -translate-y-1/2 h-8 w-8 p-0"
								aria-label="Clear search"
							>
								<X className="h-4 w-4" />
							</Button>
						)}
					</div>

					{/* Filters and Sort Row */}
					<div className="flex flex-col sm:flex-row gap-3">
						{/* Filter Dropdown */}
						<div className="flex-1">
							<Select
								value={filter}
								onValueChange={(value) =>
									onFilterChange(value as keyof typeof FILTER_OPTIONS)
								}
								disabled={isLoading}
							>
								<SelectTrigger className="w-full">
									<div className="flex items-center gap-2">
										<Filter className="h-4 w-4" />
										<SelectValue />
									</div>
								</SelectTrigger>
								<SelectContent>
									{Object.entries(FILTER_OPTIONS).map(([key, option]) => {
										const Icon = option.icon;
										return (
											<SelectItem key={key} value={key}>
												<div className="flex items-center gap-2">
													<Icon className="h-4 w-4" />
													{option.label}
												</div>
											</SelectItem>
										);
									})}
								</SelectContent>
							</Select>
						</div>

						{/* Sort Dropdown */}
						<div className="flex-1">
							<Select
								value={sortBy}
								onValueChange={(value) =>
									onSortChange(value as keyof typeof SORT_OPTIONS)
								}
								disabled={isLoading}
							>
								<SelectTrigger className="w-full">
									<div className="flex items-center gap-2">
										{sortBy.includes("desc") ? (
											<SortDesc className="h-4 w-4" />
										) : (
											<SortAsc className="h-4 w-4" />
										)}
										<SelectValue />
									</div>
								</SelectTrigger>
								<SelectContent>
									{Object.entries(SORT_OPTIONS).map(([key, option]) => (
										<SelectItem key={key} value={key}>
											{option.label}
										</SelectItem>
									))}
								</SelectContent>
							</Select>
						</div>

						{/* Clear All Button */}
						{hasActiveFilters && (
							<Button
								variant="outline"
								onClick={onClearAll}
								disabled={isLoading}
								className="w-full sm:w-auto"
							>
								<X className="h-4 w-4 mr-2" />
								Clear All
							</Button>
						)}
					</div>

					{/* Active Filters Display */}
					{hasActiveFilters && (
						<div className="flex flex-wrap gap-2">
							{searchTerm && (
								<Badge variant="secondary" className="gap-1">
									Search: "{searchTerm}"
									<Button
										variant="ghost"
										size="sm"
										onClick={handleClearSearch}
										className="h-4 w-4 p-0 hover:bg-transparent"
									>
										<X className="h-3 w-3" />
									</Button>
								</Badge>
							)}
							{filter !== "all" && (
								<Badge variant="secondary" className="gap-1">
									{FILTER_OPTIONS[filter].label}
									<Button
										variant="ghost"
										size="sm"
										onClick={handleClearFilter}
										className="h-4 w-4 p-0 hover:bg-transparent"
									>
										<X className="h-3 w-3" />
									</Button>
								</Badge>
							)}
							{sortBy !== "name_asc" && (
								<Badge variant="secondary" className="gap-1">
									{SORT_OPTIONS[sortBy].label}
									<Button
										variant="ghost"
										size="sm"
										onClick={handleClearSort}
										className="h-4 w-4 p-0 hover:bg-transparent"
									>
										<X className="h-3 w-3" />
									</Button>
								</Badge>
							)}
						</div>
					)}
				</div>
			</CardContent>
		</Card>
	);
}
