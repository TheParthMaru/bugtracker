/**
 * BugStatusBadge Component
 *
 * A reusable badge component for displaying bug status with consistent styling.
 * Follows the same design pattern as ProjectRoleBadge with color coding.
 *
 * Features:
 * - Status-based color coding
 * - Multiple size variants
 * - Multiple style variants
 * - Accessibility support
 * - Consistent styling with design system
 */

import React from "react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
	BugStatus,
	type BugStatusBadgeProps,
	getBugStatusDisplayName,
	getBugStatusColor,
} from "@/types/bug";

export function BugStatusBadge({
	status,
	variant = "default",
	size = "md",
}: BugStatusBadgeProps) {
	const getStatusVariant = () => {
		switch (status) {
			case BugStatus.OPEN:
				return "bg-blue-100 text-blue-800 border-blue-200";
			case BugStatus.FIXED:
				return "bg-green-100 text-green-800 border-green-200";
			case BugStatus.CLOSED:
				return "bg-gray-100 text-gray-800 border-gray-200";
			case BugStatus.REOPENED:
				return "bg-red-100 text-red-800 border-red-200";
			default:
				return "bg-gray-100 text-gray-800 border-gray-200";
		}
	};

	const getStatusColor = () => {
		switch (status) {
			case BugStatus.OPEN:
				return "bg-blue-600";
			case BugStatus.FIXED:
				return "bg-green-600";
			case BugStatus.CLOSED:
				return "bg-gray-600";
			case BugStatus.REOPENED:
				return "bg-red-600";
			default:
				return "bg-gray-600";
		}
	};

	const getSizeClasses = () => {
		switch (size) {
			case "sm":
				return "px-2 py-0.5 text-xs";
			case "md":
				return "px-2.5 py-1 text-sm";
			case "lg":
				return "px-3 py-1.5 text-base";
			default:
				return "px-2.5 py-1 text-sm";
		}
	};

	return (
		<Badge
			variant={variant}
			className={cn(
				"font-medium",
				variant === "default" && getStatusVariant(),
				getSizeClasses()
			)}
		>
			{variant === "default" && (
				<div className={cn("w-2 h-2 rounded-full mr-1.5", getStatusColor())} />
			)}
			{getBugStatusDisplayName(status)}
		</Badge>
	);
}

export function BugPriorityBadge({
	priority,
	variant = "default",
	size = "md",
}: {
	priority: any;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
}) {
	const getPriorityVariant = () => {
		switch (priority) {
			case "CRASH":
				return "bg-red-100 text-red-800 border-red-200";
			case "CRITICAL":
				return "bg-orange-100 text-orange-800 border-orange-200";
			case "HIGH":
				return "bg-yellow-100 text-yellow-800 border-yellow-200";
			case "MEDIUM":
				return "bg-blue-100 text-blue-800 border-blue-200";
			case "LOW":
				return "bg-green-100 text-green-800 border-green-200";
			default:
				return "bg-gray-100 text-gray-800 border-gray-200";
		}
	};

	const getPriorityColor = () => {
		switch (priority) {
			case "CRASH":
				return "bg-red-600";
			case "CRITICAL":
				return "bg-orange-600";
			case "HIGH":
				return "bg-yellow-600";
			case "MEDIUM":
				return "bg-blue-600";
			case "LOW":
				return "bg-green-600";
			default:
				return "bg-gray-600";
		}
	};

	const getPriorityDisplayName = (priority: string): string => {
		switch (priority) {
			case "CRASH":
				return "Crash";
			case "CRITICAL":
				return "Critical";
			case "HIGH":
				return "High";
			case "MEDIUM":
				return "Medium";
			case "LOW":
				return "Low";
			default:
				return priority;
		}
	};

	const getSizeClasses = () => {
		switch (size) {
			case "sm":
				return "px-2 py-0.5 text-xs";
			case "md":
				return "px-2.5 py-1 text-sm";
			case "lg":
				return "px-3 py-1.5 text-base";
			default:
				return "px-2.5 py-1 text-sm";
		}
	};

	return (
		<Badge
			variant={variant}
			className={cn(
				"font-medium",
				variant === "default" && getPriorityVariant(),
				getSizeClasses()
			)}
		>
			{variant === "default" && (
				<div
					className={cn("w-2 h-2 rounded-full mr-1.5", getPriorityColor())}
				/>
			)}
			{getPriorityDisplayName(priority)}
		</Badge>
	);
}

export function BugTypeBadge({
	type,
	variant = "default",
	size = "md",
}: {
	type: any;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
}) {
	const getTypeVariant = () => {
		switch (type) {
			case "ISSUE":
				return "bg-red-100 text-red-800 border-red-200";
			case "TASK":
				return "bg-blue-100 text-blue-800 border-blue-200";
			case "SPEC":
				return "bg-purple-100 text-purple-800 border-purple-200";
			default:
				return "bg-gray-100 text-gray-800 border-gray-200";
		}
	};

	const getTypeColor = () => {
		switch (type) {
			case "ISSUE":
				return "bg-red-600";
			case "TASK":
				return "bg-blue-600";
			case "SPEC":
				return "bg-purple-600";
			default:
				return "bg-gray-600";
		}
	};

	const getTypeDisplayName = (type: string): string => {
		switch (type) {
			case "ISSUE":
				return "Issue";
			case "TASK":
				return "Task";
			case "SPEC":
				return "Specification";
			default:
				return type;
		}
	};

	const getSizeClasses = () => {
		switch (size) {
			case "sm":
				return "px-2 py-0.5 text-xs";
			case "md":
				return "px-2.5 py-1 text-sm";
			case "lg":
				return "px-3 py-1.5 text-base";
			default:
				return "px-2.5 py-1 text-sm";
		}
	};

	return (
		<Badge
			variant={variant}
			className={cn(
				"font-medium",
				variant === "default" && getTypeVariant(),
				getSizeClasses()
			)}
		>
			{variant === "default" && (
				<div className={cn("w-2 h-2 rounded-full mr-1.5", getTypeColor())} />
			)}
			{getTypeDisplayName(type)}
		</Badge>
	);
}

export function BugLabelBadge({
	label,
	variant = "default",
	size = "sm",
	onRemove,
}: {
	label: any;
	variant?: "default" | "outline" | "secondary";
	size?: "sm" | "md" | "lg";
	onRemove?: () => void;
}) {
	const getSizeClasses = () => {
		switch (size) {
			case "sm":
				return "px-2 py-0.5 text-xs";
			case "md":
				return "px-2.5 py-1 text-sm";
			case "lg":
				return "px-3 py-1.5 text-base";
			default:
				return "px-2 py-0.5 text-xs";
		}
	};

	return (
		<Badge
			variant={variant}
			className={cn(
				"font-medium",
				variant === "default" && "bg-gray-100 text-gray-800 border-gray-200",
				getSizeClasses(),
				onRemove && "cursor-pointer hover:bg-gray-200"
			)}
			onClick={onRemove}
		>
			{variant === "default" && (
				<div
					className="w-2 h-2 rounded-full mr-1.5"
					style={{ backgroundColor: label.color }}
				/>
			)}
			{label.name}
			{onRemove && (
				<span className="ml-1 text-gray-500 hover:text-gray-700">×</span>
			)}
		</Badge>
	);
}

// Utility functions for status checks
export function isOpenStatus(status: BugStatus | null): boolean {
	return status === BugStatus.OPEN;
}

export function isFixedStatus(status: BugStatus | null): boolean {
	return status === BugStatus.FIXED;
}

export function isClosedStatus(status: BugStatus | null): boolean {
	return status === BugStatus.CLOSED;
}

export function isReopenedStatus(status: BugStatus | null): boolean {
	return status === BugStatus.REOPENED;
}

export function canTransitionToStatus(
	currentStatus: BugStatus,
	newStatus: BugStatus
): boolean {
	switch (currentStatus) {
		case BugStatus.OPEN:
			return [BugStatus.FIXED, BugStatus.CLOSED].includes(newStatus);
		case BugStatus.FIXED:
			return [BugStatus.CLOSED, BugStatus.REOPENED].includes(newStatus);
		case BugStatus.CLOSED:
			return [BugStatus.REOPENED].includes(newStatus);
		case BugStatus.REOPENED:
			return [BugStatus.FIXED, BugStatus.CLOSED].includes(newStatus);
		default:
			return false;
	}
}

export function getValidStatusTransitions(
	currentStatus: BugStatus
): BugStatus[] {
	switch (currentStatus) {
		case BugStatus.OPEN:
			return [BugStatus.FIXED, BugStatus.CLOSED];
		case BugStatus.FIXED:
			return [BugStatus.CLOSED, BugStatus.REOPENED];
		case BugStatus.CLOSED:
			return [BugStatus.REOPENED];
		case BugStatus.REOPENED:
			return [BugStatus.FIXED, BugStatus.CLOSED];
		default:
			return [];
	}
}
