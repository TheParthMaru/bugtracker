/**
 * AddMemberModal Component
 *
 * A modal dialog for adding new members to a team with real user search and role selection.
 * Includes validation to prevent adding existing members and proper error handling.
 *
 * Features:
 * - Real user search with API integration (project-specific when available)
 * - Role selection (Admin/Member)
 * - Existing member validation
 * - Loading states during search and submission
 * - User avatar and info display
 * - Proper accessibility
 * - Error handling and validation
 */

import React, { useState, useEffect, useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
	UserPlus,
	Search,
	User,
	Mail,
	Loader2,
	Check,
	X,
	AlertCircle,
} from "lucide-react";
import {
	Dialog,
	DialogContent,
	DialogHeader,
	DialogTitle,
	DialogDescription,
	DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
	Form,
	FormControl,
	FormDescription,
	FormField,
	FormItem,
	FormLabel,
	FormMessage,
} from "@/components/ui/form";
import { RoleSelector } from "./RoleBadge";
import { cn } from "@/lib/utils";
import { TeamRole } from "@/types/team";
import { userService } from "@/services/userService";
import type {
	AddMemberModalProps,
	AddMemberRequest,
	TeamMember,
} from "@/types/team";
import type { User as UserType } from "@/types/user";

// Form validation schema
const addMemberSchema = z.object({
	userId: z.string().min(1, "Please select a user"),
	role: z.enum(["ADMIN", "MEMBER"] as const),
});

type AddMemberFormData = z.infer<typeof addMemberSchema>;

export function AddMemberModal({
	isOpen,
	onClose,
	onSubmit,
	teamId,
	existingMembers,
	isLoading = false,
	projectSlug, // Add projectSlug for project-specific search
}: AddMemberModalProps & { projectSlug?: string }) {
	const [searchTerm, setSearchTerm] = useState("");
	const [searchResults, setSearchResults] = useState<UserType[]>([]);
	const [isSearching, setIsSearching] = useState(false);
	const [selectedUsers, setSelectedUsers] = useState<UserType[]>([]);
	const [searchError, setSearchError] = useState<string | null>(null);

	const form = useForm<AddMemberFormData>({
		resolver: zodResolver(addMemberSchema),
		defaultValues: {
			userId: "",
			role: "MEMBER",
		},
	});

	// Get existing member IDs for validation
	const existingMemberIds = useMemo(
		() => new Set(existingMembers.map((member) => member.userId)),
		[existingMembers]
	);

	// Real user search function - uses project-specific search when available
	const searchUsers = async (query: string): Promise<UserType[]> => {
		if (!query.trim()) return [];

		try {
			let response;

			if (projectSlug) {
				// Use project-specific search to only get project members
				const { ProjectService } = await import("@/services/projectService");
				response = await ProjectService.getInstance().searchProjectMembers(
					projectSlug,
					{
						search: query,
						size: 10,
					}
				);
			} else {
				// Fallback to general user search (for standalone teams)
				response = await userService.searchUsers({
					search: query,
					size: 10,
				});
			}

			// Filter out existing members
			return response.content.filter(
				(user: UserType) => !existingMemberIds.has(user.id)
			);
		} catch (error) {
			console.error("User search error:", error);
			throw error;
		}
	};

	// Debounced search effect
	useEffect(() => {
		const debounceTimer = setTimeout(async () => {
			if (searchTerm.trim()) {
				setIsSearching(true);
				setSearchError(null);
				try {
					const results = await searchUsers(searchTerm);
					setSearchResults(results);
				} catch (error) {
					setSearchError("Failed to search users. Please try again.");
					setSearchResults([]);
				} finally {
					setIsSearching(false);
				}
			} else {
				setSearchResults([]);
			}
		}, 300);

		return () => clearTimeout(debounceTimer);
	}, [searchTerm, existingMemberIds]);

	// Handle user selection
	const handleUserSelect = (user: UserType) => {
		form.setValue("userId", user.id);
		setSelectedUsers([user]);
	};

	// Handle form submission
	const handleSubmit = (data: AddMemberFormData) => {
		const formData: AddMemberRequest = {
			userId: data.userId,
			role: data.role as TeamRole,
		};
		onSubmit(formData);
	};

	// Handle modal close
	const handleClose = () => {
		if (!isLoading) {
			form.reset();
			setSearchTerm("");
			setSearchResults([]);
			setSelectedUsers([]);
			setSearchError(null);
			onClose();
		}
	};

	// Get user initials
	const getUserInitials = (user: UserType): string => {
		return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
	};

	return (
		<Dialog open={isOpen} onOpenChange={handleClose}>
			<DialogContent className="sm:max-w-[600px] max-h-[80vh] overflow-y-auto">
				<DialogHeader>
					<DialogTitle className="flex items-center gap-2">
						<UserPlus className="h-5 w-5" />
						Add Team Members
					</DialogTitle>
					<DialogDescription>
						Search for users and add them to your team. They will be notified of
						the invitation.
						{projectSlug && (
							<span className="block mt-1 text-sm text-muted-foreground">
								Only project members can be added to teams.
							</span>
						)}
					</DialogDescription>
				</DialogHeader>

				<Form {...form}>
					<form
						onSubmit={form.handleSubmit(handleSubmit)}
						className="space-y-6"
					>
						{/* User Search */}
						<div className="space-y-4">
							<FormLabel>Search Users</FormLabel>
							<div className="relative">
								<Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
								<Input
									placeholder="Search by name or email..."
									value={searchTerm}
									onChange={(e) => setSearchTerm(e.target.value)}
									disabled={isLoading}
									className="pl-10"
								/>
								{isSearching && (
									<Loader2 className="absolute right-3 top-1/2 transform -translate-y-1/2 h-4 w-4 animate-spin" />
								)}
							</div>

							{/* Search Error */}
							{searchError && (
								<div className="flex items-center gap-2 p-3 border border-destructive/20 rounded-lg bg-destructive/10">
									<AlertCircle className="h-4 w-4 text-destructive" />
									<span className="text-sm text-destructive">
										{searchError}
									</span>
								</div>
							)}

							{/* Search Results */}
							{searchResults.length > 0 && (
								<div className="border rounded-lg max-h-48 overflow-y-auto">
									{searchResults.map((user: UserType) => {
										const isSelected = selectedUsers.find(
											(u: UserType) => u.id === user.id
										);
										return (
											<div
												key={user.id}
												className={cn(
													"flex items-center gap-3 p-3 cursor-pointer hover:bg-muted/50 border-b last:border-b-0 transition-colors",
													isSelected && "bg-primary/10 border-primary/20"
												)}
												onClick={() => handleUserSelect(user)}
											>
												<Avatar>
													<div className="w-8 h-8 bg-primary/10 rounded-full flex items-center justify-center">
														<span className="text-xs font-medium">
															{getUserInitials(user)}
														</span>
													</div>
												</Avatar>

												<div className="flex-1 min-w-0">
													<div className="flex items-center gap-2">
														<span className="font-medium truncate">
															{user.firstName} {user.lastName}
														</span>
														<Badge variant="secondary" className="text-xs">
															{user.role}
														</Badge>
													</div>
													<div className="flex items-center gap-1 text-sm text-muted-foreground">
														<Mail className="h-3 w-3" />
														<span className="truncate">{user.email}</span>
													</div>
												</div>

												{isSelected ? (
													<Check className="h-4 w-4 text-primary" />
												) : (
													<UserPlus className="h-4 w-4 text-muted-foreground" />
												)}
											</div>
										);
									})}
								</div>
							)}

							{/* No Results */}
							{searchTerm.trim() &&
								!isSearching &&
								searchResults.length === 0 &&
								!searchError && (
									<div className="text-center py-8 text-muted-foreground">
										<User className="h-8 w-8 mx-auto mb-2" />
										<p>No users found matching "{searchTerm}"</p>
										<p className="text-sm">Try a different search term</p>
									</div>
								)}
						</div>

						{/* Selected User Display */}
						{selectedUsers.length > 0 && (
							<div className="space-y-3">
								<FormLabel>Selected User</FormLabel>
								<div className="border rounded-lg p-3">
									<div className="flex items-center gap-3">
										<Avatar>
											<div className="w-8 h-8 bg-primary/10 rounded-full flex items-center justify-center">
												<span className="text-xs font-medium">
													{getUserInitials(selectedUsers[0])}
												</span>
											</div>
										</Avatar>
										<div className="flex-1 min-w-0">
											<div className="font-medium">
												{selectedUsers[0].firstName} {selectedUsers[0].lastName}
											</div>
											<div className="text-sm text-muted-foreground">
												{selectedUsers[0].email}
											</div>
										</div>
										<Button
											type="button"
											variant="ghost"
											size="sm"
											onClick={() => {
												setSelectedUsers([]);
												form.setValue("userId", "");
											}}
										>
											<X className="h-3 w-3" />
										</Button>
									</div>
								</div>
							</div>
						)}

						{/* Role Selection */}
						<FormField
							control={form.control}
							name="role"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Team Role</FormLabel>
									<FormControl>
										<RoleSelector
											value={field.value as TeamRole}
											onChange={field.onChange}
											disabled={isLoading}
										/>
									</FormControl>
									<FormDescription>
										Choose the role for the new team member
									</FormDescription>
									<FormMessage />
								</FormItem>
							)}
						/>

						{/* Submit Buttons */}
						<DialogFooter>
							<Button
								type="button"
								variant="outline"
								onClick={handleClose}
								disabled={isLoading}
							>
								Cancel
							</Button>
							<Button type="submit" disabled={isLoading || !selectedUsers[0]}>
								{isLoading ? (
									<>
										<Loader2 className="h-4 w-4 mr-2 animate-spin" />
										Adding...
									</>
								) : (
									<>
										<UserPlus className="h-4 w-4 mr-2" />
										Add Member
									</>
								)}
							</Button>
						</DialogFooter>
					</form>
				</Form>
			</DialogContent>
		</Dialog>
	);
}

// Hook for using the modal
export function useAddMemberModal() {
	const [isOpen, setIsOpen] = useState(false);

	const openModal = () => setIsOpen(true);
	const closeModal = () => setIsOpen(false);

	return {
		isOpen,
		openModal,
		closeModal,
	};
}

// Example usage component
export function AddMemberButton({
	teamId,
	existingMembers,
	onMemberAdded,
	disabled = false,
	projectSlug,
}: {
	teamId: string;
	existingMembers: TeamMember[];
	onMemberAdded?: (member: any) => void;
	disabled?: boolean;
	projectSlug?: string;
}) {
	const { isOpen, openModal, closeModal } = useAddMemberModal();
	const [isLoading, setIsLoading] = useState(false);

	const handleSubmit = async (data: AddMemberRequest) => {
		setIsLoading(true);
		try {
			// This would typically use the team service
			// const newMember = await teamService.addMember(teamId, data);
			// onMemberAdded?.(newMember);
			console.log("Adding member:", data);
			closeModal();
		} catch (error) {
			console.error("Failed to add member:", error);
		} finally {
			setIsLoading(false);
		}
	};

	return (
		<>
			<Button onClick={openModal} disabled={disabled} size="sm">
				<UserPlus className="h-4 w-4 mr-2" />
				Add Member
			</Button>

			<AddMemberModal
				isOpen={isOpen}
				onClose={closeModal}
				onSubmit={handleSubmit}
				teamId={teamId}
				existingMembers={existingMembers}
				isLoading={isLoading}
				projectSlug={projectSlug}
			/>
		</>
	);
}
