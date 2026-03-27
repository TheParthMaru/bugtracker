import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "@/services/api";
import { toast } from "react-toastify";
import Navbar from "@/components/Navbar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Footer } from "@/components/ui/footer";
import { ProfileBreadcrumb } from "@/components/ui/breadcrumb";
import { User as UserIcon, Edit } from "lucide-react";

type UserProfile = {
	id?: string;
	firstName: string;
	lastName: string;
	email: string;
	role: string;
	skills?: string[];
};

export default function ProfilePage() {
	const navigate = useNavigate();
	const [profile, setProfile] = useState<UserProfile | null>(null);
	const [loading, setLoading] = useState(true);
	const [isEditModalOpen, setIsEditModalOpen] = useState(false);
	const [editingProfile, setEditingProfile] = useState<UserProfile | null>(
		null
	);

	useEffect(() => {
		const fetchData = async () => {
			try {
				// Fetch user profile
				const profileResponse = await API.get("/profile");
				setProfile(profileResponse.data);
			} catch (err: any) {
				console.error("Data fetch failed:", err);
				toast.error("Failed to load profile data. Please login again.");
			} finally {
				setLoading(false);
			}
		};

		fetchData();
	}, []);

	const getRoleColor = (role: string) => {
		switch (role) {
			case "ADMIN":
				return "bg-red-100 text-red-800";
			case "DEVELOPER":
				return "bg-blue-100 text-blue-800";
			case "REPORTER":
				return "bg-green-100 text-green-800";
			default:
				return "bg-gray-100 text-gray-800";
		}
	};

	const handleEditProfile = () => {
		setEditingProfile({ ...profile! });
		setIsEditModalOpen(true);
	};

	const handleSaveProfile = async () => {
		if (!editingProfile) return;

		try {
			// Call the backend API to update the profile
			const response = await API.put("/users/profile", {
				firstName: editingProfile.firstName,
				lastName: editingProfile.lastName,
				skills: editingProfile.skills || [],
			});

			// Update the local state with the response from backend
			setProfile(response.data);
			toast.success("Profile updated successfully!");
			setIsEditModalOpen(false);
		} catch (error) {
			console.error("Failed to update profile:", error);
			const errorMessage =
				error instanceof Error
					? error.message
					: "Failed to update profile. Please try again.";
			toast.error(errorMessage);
		}
	};

	const handleCancelEdit = () => {
		setIsEditModalOpen(false);
		setEditingProfile(null);
	};

	if (loading) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<div className="flex-1 p-8 text-center">Loading profile...</div>
				<Footer />
			</div>
		);
	}

	if (!profile) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<div className="flex-1 p-8 text-center">Failed to load profile</div>
				<Footer />
			</div>
		);
	}

	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />
			<main className="flex-1">
				<div className="container mx-auto px-8 py-8 max-w-7xl">
					{/* Breadcrumb */}
					<ProfileBreadcrumb />

					{/* Page Header */}
					<div className="mb-8">
						<h1 className="text-3xl font-bold mb-2">Profile</h1>
						<p className="text-muted-foreground">
							Manage your account and personal information
						</p>
					</div>

					{/* User Information Card */}
					<Card className="w-full px-8 py-6">
						<CardHeader>
							<div className="flex items-center justify-between">
								<CardTitle className="text-lg font-semibold">
									Personal Information
								</CardTitle>
								<Button onClick={handleEditProfile} variant="outline" size="sm">
									<Edit className="h-4 w-4 mr-2" />
									Edit Profile
								</Button>
							</div>
						</CardHeader>
						<CardContent className="space-y-6">
							<div className="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-6">
								<div>
									<label className="text-sm font-medium text-muted-foreground">
										Full Name
									</label>
									<p className="text-lg font-medium">
										{profile.firstName} {profile.lastName}
									</p>
								</div>
								<div>
									<label className="text-sm font-medium text-muted-foreground">
										Email
									</label>
									<p className="text-lg font-medium">{profile.email}</p>
								</div>
								<div>
									<label className="text-sm font-medium text-muted-foreground">
										Role
									</label>
									<div className="mt-1">
										<Badge className={getRoleColor(profile.role)}>
											{profile.role}
										</Badge>
									</div>
								</div>
								<div>
									<label className="text-sm font-medium text-muted-foreground">
										User ID
									</label>
									<p className="text-lg font-mono break-all">{profile.id}</p>
								</div>
							</div>
							{profile.skills && profile.skills.length > 0 && (
								<div>
									<label className="text-sm font-medium text-muted-foreground">
										Skills
									</label>
									<div className="flex flex-wrap gap-2 mt-2">
										{profile.skills.map((skill, index) => (
											<Badge key={index} variant="secondary">
												{skill}
											</Badge>
										))}
									</div>
								</div>
							)}
						</CardContent>
					</Card>

					{/* Leaderboard Section */}
					<Card className="w-full px-8 py-6 mt-6">
						<CardHeader>
							<CardTitle className="text-lg font-semibold">
								Leaderboard
							</CardTitle>
						</CardHeader>
						<CardContent>
							<div className="flex items-center justify-between">
								<div>
									<p className="text-muted-foreground mb-2">
										Track your progress, achievements, and leaderboard rankings
									</p>
								</div>
								<Button
									onClick={() => navigate("/leaderboard")}
									variant="outline"
									size="sm"
								>
									View Dashboard
								</Button>
							</div>
						</CardContent>
					</Card>
				</div>
			</main>
			<Footer />

			{/* Edit Profile Modal */}
			{isEditModalOpen && editingProfile && (
				<div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
					<div className="bg-white rounded-lg p-6 w-full max-w-md mx-4">
						<h2 className="text-xl font-bold mb-4">Edit Profile</h2>

						<div className="space-y-4">
							<div>
								<label className="block text-sm font-medium text-gray-700 mb-1">
									First Name
								</label>
								<input
									type="text"
									value={editingProfile.firstName}
									onChange={(e) =>
										setEditingProfile({
											...editingProfile,
											firstName: e.target.value,
										})
									}
									className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
								/>
							</div>

							<div>
								<label className="block text-sm font-medium text-gray-700 mb-1">
									Last Name
								</label>
								<input
									type="text"
									value={editingProfile.lastName}
									onChange={(e) =>
										setEditingProfile({
											...editingProfile,
											lastName: e.target.value,
										})
									}
									className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
								/>
							</div>

							<div>
								<label className="block text-sm font-medium text-gray-700 mb-1">
									Email
								</label>
								<input
									type="email"
									value={editingProfile.email}
									onChange={(e) =>
										setEditingProfile({
											...editingProfile,
											email: e.target.value,
										})
									}
									className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
								/>
							</div>

							<div>
								<label className="block text-sm font-medium text-gray-700 mb-1">
									Skills (comma-separated)
								</label>
								<input
									type="text"
									value={editingProfile.skills?.join(", ") || ""}
									onChange={(e) => {
										// Store the raw input value, don't process it yet
										setEditingProfile({
											...editingProfile,
											skills: e.target.value.split(",").map((s) => s.trim()),
										});
									}}
									onBlur={(e) => {
										// Process and clean the skills when user finishes typing
										const cleanedSkills = e.target.value
											.split(",")
											.map((s) => s.trim())
											.filter((s) => s.length > 0);

										setEditingProfile({
											...editingProfile,
											skills: cleanedSkills,
										});
									}}
									className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
									placeholder="e.g., JavaScript, React, Node.js"
								/>
							</div>
						</div>

						<div className="flex justify-end space-x-3 mt-6">
							<Button onClick={handleCancelEdit} variant="outline">
								Cancel
							</Button>
							<Button onClick={handleSaveProfile}>Save Changes</Button>
						</div>
					</div>
				</div>
			)}
		</div>
	);
}
