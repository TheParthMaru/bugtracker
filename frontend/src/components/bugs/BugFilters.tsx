/**
 * BugFilters Component
 *
 * A comprehensive filtering component for bugs with search, status, priority,
 * type, assignee, and label filters. Follows the same design pattern as
 * TeamSearchFilters with enhanced functionality.
 *
 * Features:
 * - Real-time search with debouncing
 * - Multiple filter types (status, priority, type, assignee, labels)
 * - Clear all filters functionality
 * - Loading states
 * - Responsive design
 * - Accessibility support
 */

import React, { useState, useEffect } from "react";
import { Search, Filter, X, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { BugFiltersProps } from "@/types/bug";
import { BugStatus, BugPriority, BugType } from "@/types/bug";
import { BugLabelBadge } from "./BugStatusBadge";

// Filter options
const STATUS_OPTIONS = {
	ALL: "All Statuses",
	OPEN: "Open",
	FIXED: "Fixed",
	CLOSED: "Closed",
	REOPENED: "Reopened",
} as const;

const PRIORITY_OPTIONS = {
	ALL: "All Priorities",
	CRASH: "Crash",
	CRITICAL: "Critical",
	HIGH: "High",
	MEDIUM: "Medium",
	LOW: "Low",
} as const;

const TYPE_OPTIONS = {
	ALL: "All Types",
	ISSUE: "Issue",
	TASK: "Task",
	SPEC: "Specification",
} as const;

const ASSIGNEE_OPTIONS = {
	ALL: "All Assignees",
	UNASSIGNED: "Unassigned",
	ASSIGNED: "Assigned",
	ASSIGNED_TO_ME: "Assigned to Me",
} as const;

export function BugFilters({
	searchTerm,
	onSearchChange,
	status,
	onStatusChange,
	priority,
	onPriorityChange,
	type,
	onTypeChange,
	assignee,
	onAssigneeChange,
	labels,
	onLabelsChange,
	onClearAll,
	hasActiveFilters,
	isLoading = false,
	projectMembers = [],
	currentUserId,
}: BugFiltersProps & {
	projectMembers?: any[];
	currentUserId?: string;
}) {
	const [availableLabels, setAvailableLabels] = useState<any[]>([]);
	const [availableMembers, setAvailableMembers] = useState<any[]>([]);
	const [localSearchTerm, setLocalSearchTerm] = useState(searchTerm);

	// Load available data
	useEffect(() => {
		loadAvailableData();
	}, []);

	const loadAvailableData = async () => {
		try {
			// TODO: Load available labels and members from API
			setAvailableLabels([
				{ id: 1, name: "Frontend", color: "#3B82F6" },
				{ id: 2, name: "Backend", color: "#EF4444" },
				{ id: 3, name: "Database", color: "#8B5CF6" },
				{ id: 4, name: "UI/UX", color: "#10B981" },
				{ id: 5, name: "Bug", color: "#F59E0B" },
				{ id: 6, name: "Feature", color: "#10B981" },
			]);
			setAvailableMembers([
				{ id: "1", firstName: "John", lastName: "Doe" },
				{ id: "2", firstName: "Jane", lastName: "Smith" },
				{ id: "3", firstName: "Bob", lastName: "Wilson" },
			]);
		} catch (error) {
			console.error("Failed to load available data:", error);
		}
	};

	// Handle search with debouncing
	useEffect(() => {
		const timer = setTimeout(() => {
			onSearchChange(localSearchTerm);
		}, 300);

		return () => clearTimeout(timer);
	}, [localSearchTerm]); // Remove onSearchChange from dependencies

	// Handle label selection
	const handleLabelToggle = (label: any) => {
		const isSelected = labels.includes(label.name);
		if (isSelected) {
			onLabelsChange(labels.filter((l) => l !== label.name));
		} else {
			onLabelsChange([...labels, label.name]);
		}
	};

	// Handle label removal
	const handleLabelRemove = (labelName: string) => {
		onLabelsChange(labels.filter((l) => l !== labelName));
	};

	// Clear search
	const handleClearSearch = () => {
		setLocalSearchTerm("");
		onSearchChange("");
	};

	// Clear specific filter
	const handleClearFilter = (filterType: string) => {
		switch (filterType) {
			case "status":
				onStatusChange("ALL");
				break;
			case "priority":
				onPriorityChange("ALL");
				break;
			case "type":
				onTypeChange("ALL");
				break;
			case "assignee":
				onAssigneeChange("ALL");
				break;
			case "labels":
				onLabelsChange([]);
				break;
		}
	};

	return (
		<div className="space-y-4">
			{/* Search Bar */}
			<div className="relative">
				<Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
				<Input
					placeholder="Search bugs by title or description..."
					value={localSearchTerm}
					onChange={(e) => setLocalSearchTerm(e.target.value)}
					className="pl-10 pr-10"
					disabled={isLoading}
				/>
				{localSearchTerm && (
					<Button
						variant="ghost"
						size="sm"
						onClick={handleClearSearch}
						className="absolute right-2 top-1/2 transform -translate-y-1/2 h-6 w-6 p-0"
					>
						<X className="h-3 w-3" />
					</Button>
				)}
			</div>

			{/* Filter Controls */}
			<div className="flex flex-wrap gap-3">
				{/* Status Filter */}
				<div className="flex items-center gap-2">
					<Filter className="h-4 w-4 text-gray-500" />
					<Select
						value={status}
						onValueChange={(value) => onStatusChange(value as any)}
						disabled={isLoading}
					>
						<SelectTrigger className="w-32">
							<SelectValue />
						</SelectTrigger>
						<SelectContent>
							{Object.entries(STATUS_OPTIONS).map(([key, label]) => (
								<SelectItem key={key} value={key}>
									{label}
								</SelectItem>
							))}
						</SelectContent>
					</Select>
					{status !== "ALL" && (
						<Button
							variant="ghost"
							size="sm"
							onClick={() => handleClearFilter("status")}
							className="h-6 w-6 p-0"
						>
							<X className="h-3 w-3" />
						</Button>
					)}
				</div>

				{/* Priority Filter */}
				<div className="flex items-center gap-2">
					<Select
						value={priority}
						onValueChange={(value) => onPriorityChange(value as any)}
						disabled={isLoading}
					>
						<SelectTrigger className="w-32">
							<SelectValue />
						</SelectTrigger>
						<SelectContent>
							{Object.entries(PRIORITY_OPTIONS).map(([key, label]) => (
								<SelectItem key={key} value={key}>
									{label}
								</SelectItem>
							))}
						</SelectContent>
					</Select>
					{priority !== "ALL" && (
						<Button
							variant="ghost"
							size="sm"
							onClick={() => handleClearFilter("priority")}
							className="h-6 w-6 p-0"
						>
							<X className="h-3 w-3" />
						</Button>
					)}
				</div>

				{/* Type Filter */}
				<div className="flex items-center gap-2">
					<Select
						value={type}
						onValueChange={(value) => onTypeChange(value as any)}
						disabled={isLoading}
					>
						<SelectTrigger className="w-32">
							<SelectValue />
						</SelectTrigger>
						<SelectContent>
							{Object.entries(TYPE_OPTIONS).map(([key, label]) => (
								<SelectItem key={key} value={key}>
									{label}
								</SelectItem>
							))}
						</SelectContent>
					</Select>
					{type !== "ALL" && (
						<Button
							variant="ghost"
							size="sm"
							onClick={() => handleClearFilter("type")}
							className="h-6 w-6 p-0"
						>
							<X className="h-3 w-3" />
						</Button>
					)}
				</div>

				{/* Assignee Filter */}
				<div className="flex items-center gap-2">
					<Select
						value={assignee}
						onValueChange={(value) => onAssigneeChange(value as any)}
						disabled={isLoading}
					>
						<SelectTrigger className="w-32">
							<SelectValue />
						</SelectTrigger>
						<SelectContent>
							{Object.entries(ASSIGNEE_OPTIONS).map(([key, label]) => (
								<SelectItem key={key} value={key}>
									{label}
								</SelectItem>
							))}
							{projectMembers.length > 0 && (
								<>
									<SelectItem value="separator" disabled>
										──────────
									</SelectItem>
									{projectMembers.map((member) => (
										<SelectItem key={member.userId} value={member.userId}>
											{member.firstName && member.lastName
												? `${member.firstName} ${member.lastName}`
												: member.userName || member.userEmail}
										</SelectItem>
									))}
								</>
							)}
						</SelectContent>
					</Select>
					{assignee !== "ALL" && (
						<Button
							variant="ghost"
							size="sm"
							onClick={() => handleClearFilter("assignee")}
							className="h-6 w-6 p-0"
						>
							<X className="h-3 w-3" />
						</Button>
					)}
				</div>

				{/* Clear All Filters */}
				{hasActiveFilters && (
					<Button
						variant="outline"
						size="sm"
						onClick={onClearAll}
						disabled={isLoading}
						className="text-gray-600"
					>
						Clear All
					</Button>
				)}

				{/* Loading Indicator */}
				{isLoading && (
					<div className="flex items-center gap-2 text-gray-500">
						<Loader2 className="h-4 w-4 animate-spin" />
						<span className="text-sm">Loading...</span>
					</div>
				)}
			</div>

			{/* Active Labels */}
			{labels.length > 0 && (
				<div className="flex flex-wrap gap-2">
					<span className="text-sm text-gray-500">Active labels:</span>
					{labels.map((labelName) => {
						const label = availableLabels.find((l) => l.name === labelName);
						return (
							<BugLabelBadge
								key={labelName}
								label={label || { id: 0, name: labelName, color: "#6B7280" }}
								onRemove={() => handleLabelRemove(labelName)}
							/>
						);
					})}
				</div>
			)}

			{/* Available Labels */}
			<div className="border-t pt-4">
				<div className="mb-2">
					<span className="text-sm font-medium text-gray-700">Labels:</span>
				</div>
				<div className="flex flex-wrap gap-2">
					{availableLabels.map((label) => {
						const isSelected = labels.includes(label.name);
						return (
							<button
								key={label.id}
								type="button"
								onClick={() => handleLabelToggle(label)}
								className={cn(
									"px-3 py-1 rounded-full text-xs font-medium border transition-colors",
									isSelected
										? "bg-blue-100 text-blue-800 border-blue-300"
										: "bg-gray-100 text-gray-700 border-gray-300 hover:bg-gray-200"
								)}
								disabled={isLoading}
							>
								<div
									className="w-2 h-2 rounded-full mr-1.5 inline-block"
									style={{ backgroundColor: label.color }}
								/>
								{label.name}
							</button>
						);
					})}
				</div>
			</div>
		</div>
	);
}
