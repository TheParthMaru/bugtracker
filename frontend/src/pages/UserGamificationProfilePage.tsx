/**
 * UserGamificationProfilePage
 *
 * Page for viewing a specific user's gamification profile.
 * Handles route parameters and user authentication.
 */

import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import UserGamificationProfile from "@/components/gamification/UserGamificationProfile";
import API from "@/services/api";

interface User {
	id: string;
	firstName: string;
	lastName: string;
}

export default function UserGamificationProfilePage() {
	const [currentUser, setCurrentUser] = useState<User | null>(null);
	const [loading, setLoading] = useState(true);
	const navigate = useNavigate();
	const { userId } = useParams<{ userId: string }>();

	useEffect(() => {
		const token = localStorage.getItem("bugtracker_token");
		if (!token) {
			navigate("/auth/login");
			return;
		}

		// Fetch current user data
		API.get("/profile")
			.then((response) => {
				setCurrentUser(response.data);
				setLoading(false);
			})
			.catch(() => {
				localStorage.removeItem("bugtracker_token");
				navigate("/auth/login");
			});
	}, [navigate]);

	if (loading) {
		return (
			<div className="min-h-screen flex items-center justify-center">
				<div className="text-lg">Loading profile...</div>
			</div>
		);
	}

	if (!currentUser || !userId) {
		return null;
	}

	// Users can only view their own leaderboard profile
	if (currentUser.id !== userId) {
		navigate("/leaderboard");
		return null;
	}

	return <UserGamificationProfile userId={userId} />;
}
