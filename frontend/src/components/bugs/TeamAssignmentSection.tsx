/**
 * TeamAssignmentSection Component
 *
 * A component for displaying and managing team assignments for bugs.
 * Shows auto-detected teams based on labels and allows manual team selection.
 *
 * Features:
 * - Auto-detected team display
 * - Manual team selection
 * - Team member skill matching
 * - Assignment override controls
 */

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Separator } from "@/components/ui/separator";
import {
	Users,
	AlertTriangle,
	Plus,
	X,
	CheckCircle,
	UserCheck,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
	TeamAssignmentRecommendation,
	TeamAssignmentInfo,
	TeamMemberSkillMatch,
	AssignmentType,
} from "@/types/bug";
import type { Team } from "@/types/team";

interface TeamAssignmentSectionProps {
	projectSlug: string;
	labels: number[];
	tags: string[];
	onTeamAssignmentChange: (teamIds: string[]) => void;
	availableTeams: Team[];
	isLoading?: boolean;
}

export function TeamAssignmentSection({
	projectSlug,
	labels,
	tags,
	onTeamAssignmentChange,
	availableTeams,
	isLoading = false,
}: TeamAssignmentSectionProps) {
	const [recommendation, setRecommendation] =
		useState<TeamAssignmentRecommendation | null>(null);
	const [selectedTeamIds, setSelectedTeamIds] = useState<string[]>([]);
	const [isLoadingRecommendation, setIsLoadingRecommendation] = useState(false);
	const [error, setError] = useState<string | null>(null);

	// Get team assignment recommendation when labels or tags change
	useEffect(() => {
		if (labels.length > 0 || tags.length > 0) {
			getTeamAssignmentRecommendation();
		} else {
			setRecommendation(null);
			setSelectedTeamIds([]);
		}
	}, [labels, tags, projectSlug]);

	// Update parent component when team assignments change
	useEffect(() => {
		onTeamAssignmentChange(selectedTeamIds);
	}, [selectedTeamIds, onTeamAssignmentChange]);

	const getTeamAssignmentRecommendation = async () => {
		if (labels.length === 0 && tags.length === 0) return;

		setIsLoadingRecommendation(true);
		setError(null);

		try {
			// Create a temporary request object for team analysis API call
			const requestData = {
				title: "Temporary title for team analysis",
				description: "Temporary description for team analysis",
				type: "ISSUE" as const,
				priority: "MEDIUM" as const,
				labelIds: labels,
				tags: tags,
			};

			const response = await fetch(
				`/api/bugtracker/v1/projects/${projectSlug}/bugs/team-assignment-recommendation`,
				{
					method: "POST",
					headers: {
						"Content-Type": "application/json",
						Authorization: `Bearer ${localStorage.getItem("bugtracker_token")}`,
					},
					body: JSON.stringify(requestData),
				}
			);

			if (!response.ok) {
				throw new Error("Failed to get team assignment recommendation");
			}

			const data: TeamAssignmentRecommendation = await response.json();
			setRecommendation(data);

			// Auto-select recommended teams
			if (data.hasTeams()) {
				const recommendedTeamIds = data.assignedTeams.map(
					(team) => team.teamId
				);
				setSelectedTeamIds(recommendedTeamIds);
			}
		} catch (err) {
			console.error("Error getting team assignment recommendation:", err);
			setError("Failed to get team assignment recommendation");
		} finally {
			setIsLoadingRecommendation(false);
		}
	};

	const handleAddTeam = (teamId: string) => {
		if (!selectedTeamIds.includes(teamId)) {
			setSelectedTeamIds([...selectedTeamIds, teamId]);
		}
	};

	const handleRemoveTeam = (teamId: string) => {
		setSelectedTeamIds(selectedTeamIds.filter((id) => id !== teamId));
	};

	const getAssignmentTypeDisplay = (type: AssignmentType) => {
		switch (type) {
			case AssignmentType.SINGLE_TEAM:
				return {
					text: "Single Team",
					color: "text-blue-600",
					icon: CheckCircle,
				};
			case AssignmentType.MULTI_TEAM:
				return {
					text: "Multiple Teams",
					color: "text-purple-600",
					icon: Users,
				};
			case AssignmentType.NO_TEAM_FOUND:
				return {
					text: "No Teams Found",
					color: "text-yellow-600",
					icon: AlertTriangle,
				};
			case AssignmentType.PARTIAL_MATCH:
				return {
					text: "Partial Match",
					color: "text-orange-600",
					icon: AlertTriangle,
				};
			default:
				return { text: "Unknown", color: "text-gray-600", icon: AlertTriangle };
		}
	};

	const renderTeamAssignmentInfo = (team: TeamAssignmentInfo) => {
		const teamMembers = recommendation?.teamMemberSkills[team.teamId] || [];
		const hasSkilledMembers = teamMembers.length > 0;

		return (
			<Card key={team.teamId} className="border-l-4 border-l-blue-500">
				<CardHeader className="pb-3">
					<div className="flex items-center justify-between">
						<div className="flex items-center gap-2">
							<CardTitle className="text-base">{team.teamName}</CardTitle>
							{team.isPrimary && (
								<Badge variant="secondary" className="text-xs">
									Primary
								</Badge>
							)}
						</div>
						<Button
							variant="ghost"
							size="sm"
							onClick={() => handleRemoveTeam(team.teamId)}
							className="h-6 w-6 p-0"
						>
							<X className="h-3 w-3" />
						</Button>
					</div>
					<div className="flex items-center gap-2 text-sm text-gray-600">
						<Users className="h-4 w-4" />
						<span>{team.memberCount} members</span>
						<span>•</span>
						<span>Match: {(team.labelMatchScore * 100).toFixed(0)}%</span>
					</div>
				</CardHeader>
				<CardContent className="pt-0">
					{/* Matching Labels */}
					{team.matchingLabels.length > 0 && (
						<div className="mb-3">
							<p className="text-sm text-gray-600 mb-2">Matching labels:</p>
							<div className="flex flex-wrap gap-1">
								{team.matchingLabels.map((label) => (
									<Badge key={label} variant="outline" className="text-xs">
										{label}
									</Badge>
								))}
							</div>
						</div>
					)}

					{/* Team Member Skills */}
					{hasSkilledMembers && (
						<div>
							<p className="text-sm text-gray-600 mb-2">Skilled members:</p>
							<div className="space-y-2">
								{teamMembers.slice(0, 3).map((member) => (
									<div
										key={member.userId}
										className="flex items-center justify-between p-2 bg-gray-50 rounded-md"
									>
										<div className="flex items-center gap-2">
											<UserCheck className="h-4 w-4 text-green-600" />
											<span className="text-sm font-medium">
												{member.firstName} {member.lastName}
											</span>
										</div>
										<div className="flex items-center gap-1">
											<span className="text-xs text-gray-500">
												{(member.skillRelevanceScore * 100).toFixed(0)}% match
											</span>
											<Badge variant="outline" className="text-xs">
												{member.primarySkill}
											</Badge>
										</div>
									</div>
								))}
								{teamMembers.length > 3 && (
									<p className="text-xs text-gray-500 text-center">
										+{teamMembers.length - 3} more skilled members
									</p>
								)}
							</div>
						</div>
					)}

					{/* Assignment Reason */}
					<p className="text-xs text-gray-500 mt-2">{team.assignmentReason}</p>
				</CardContent>
			</Card>
		);
	};

	if (isLoading || isLoadingRecommendation) {
		return (
			<div className="space-y-4">
				<div className="h-6 bg-gray-200 rounded animate-pulse" />
				<div className="h-32 bg-gray-200 rounded animate-pulse" />
			</div>
		);
	}

	return (
		<div className="space-y-4">
			<div className="flex items-center gap-2">
				<Users className="h-5 w-5 text-gray-600" />
				<h3 className="text-lg font-semibold text-gray-800">
					Team Assignment{" "}
					<span className="text-gray-500 text-sm">(Auto-detected)</span>
				</h3>
			</div>

			{/* Error Display */}
			{error && (
				<Alert variant="destructive">
					<AlertTriangle className="h-4 w-4" />
					<AlertDescription>{error}</AlertDescription>
				</Alert>
			)}

			{/* Team Assignment Recommendation */}
			{recommendation && (
				<div className="space-y-4">
					{/* Recommendation Summary */}
					<div className="flex items-center justify-between p-3 bg-blue-50 rounded-lg border border-blue-200">
						<div className="flex items-center gap-2">
							{(() => {
								const {
									text,
									color,
									icon: Icon,
								} = getAssignmentTypeDisplay(recommendation.assignmentType);
								return (
									<>
										<Icon className={cn("h-4 w-4", color)} />
										<span className={cn("font-medium", color)}>{text}</span>
									</>
								);
							})()}
						</div>
						<div className="text-sm text-gray-600">
							Confidence: {(recommendation.confidenceScore * 100).toFixed(0)}%
						</div>
					</div>

					{/* Recommendation Message */}
					<p className="text-sm text-gray-700">{recommendation.message}</p>

					{/* Auto-Detected Teams */}
					{recommendation.hasTeams() && (
						<div className="space-y-3">
							<h4 className="font-medium text-gray-800">
								Auto-detected Teams:
							</h4>
							<div className="grid gap-3">
								{recommendation.assignedTeams.map((team) =>
									renderTeamAssignmentInfo(team)
								)}
							</div>
						</div>
					)}

					{/* No Teams Found */}
					{recommendation.isNoTeamFound() && (
						<Alert>
							<AlertTriangle className="h-4 w-4" />
							<AlertDescription>
								{recommendation.message}
								<br />
								<Button
									variant="link"
									className="p-0 h-auto text-blue-700 underline"
									onClick={() => {
										// TODO: Navigate to team creation
										console.log("Navigate to team creation");
									}}
								>
									Create a team
								</Button>{" "}
								or assign manually below.
							</AlertDescription>
						</Alert>
					)}
				</div>
			)}

			<Separator />

			{/* Manual Team Selection */}
			<div className="space-y-3">
				<h4 className="font-medium text-gray-800">
					Add/Change Teams Manually:
				</h4>
				<div className="flex gap-2">
					<Select onValueChange={handleAddTeam}>
						<SelectTrigger className="w-full">
							<SelectValue placeholder="Select team to add..." />
						</SelectTrigger>
						<SelectContent>
							{availableTeams
								.filter((team) => !selectedTeamIds.includes(team.id))
								.map((team) => (
									<SelectItem key={team.id} value={team.id}>
										{team.name}
									</SelectItem>
								))}
						</SelectContent>
					</Select>
					<Button
						variant="outline"
						onClick={() => {
							// This will be handled by the Select onValueChange
						}}
						disabled={
							availableTeams.filter(
								(team) => !selectedTeamIds.includes(team.id)
							).length === 0
						}
					>
						<Plus className="h-4 w-4 mr-1" />
						Add Team
					</Button>
				</div>

				{/* Selected Teams Summary */}
				{selectedTeamIds.length > 0 && (
					<div className="p-3 bg-green-50 rounded-lg border border-green-200">
						<div className="flex items-center gap-2 mb-2">
							<CheckCircle className="h-4 w-4 text-green-600" />
							<span className="font-medium text-green-800">
								Bug will be assigned to {selectedTeamIds.length} team(s)
							</span>
						</div>
						<div className="flex flex-wrap gap-2">
							{selectedTeamIds.map((teamId) => {
								const team = availableTeams.find((t) => t.id === teamId);
								return (
									<Badge key={teamId} variant="secondary" className="text-sm">
										{team?.name || teamId}
									</Badge>
								);
							})}
						</div>
					</div>
				)}
			</div>
		</div>
	);
}
