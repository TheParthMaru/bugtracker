/**
 * GamificationPage
 *
 * Main page for the gamification module that handles user authentication
 * and renders the gamification dashboard for the current user.
 */

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import GamificationDashboard from "@/components/gamification/GamificationDashboard";
import API from "@/services/api";
import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";
import { Button } from "@/components/ui/button";

interface User {
	id: string;
	firstName: string;
	lastName: string;
}

export default function GamificationPage() {
	const [user, setUser] = useState<User | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const navigate = useNavigate();

	useEffect(() => {
		console.log("🔍 GamificationPage: useEffect triggered");

		const token = localStorage.getItem("bugtracker_token");
		console.log("🔍 GamificationPage: Token exists:", !!token);

		if (!token) {
			console.log("🔍 GamificationPage: No token, redirecting to login");
			navigate("/auth/login");
			return;
		}

		// Fetch current user data
		console.log("🔍 GamificationPage: Fetching user profile...");
		API.get("/profile")
			.then((response) => {
				console.log(
					"🔍 GamificationPage: Profile fetched successfully:",
					response.data
				);
				setUser(response.data);
				setLoading(false);
			})
			.catch((err) => {
				console.error("🔍 GamificationPage: Profile fetch failed:", err);
				setError(err.message || "Failed to fetch user profile");
				setLoading(false);
				// Don't redirect immediately, show error first
			});
	}, [navigate]);

	if (loading) {
		console.log("🔍 GamificationPage: Rendering loading state");
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<div className="flex-1 flex items-center justify-center">
					<div className="text-lg">Loading leaderboard...</div>
				</div>
				<Footer />
			</div>
		);
	}

	if (error) {
		console.log("🔍 GamificationPage: Rendering error state:", error);
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<div className="flex-1 flex items-center justify-center">
					<div className="text-center">
						<div className="text-lg text-red-600 mb-4">
							Error loading leaderboard
						</div>
						<div className="text-sm text-gray-600 mb-4">{error}</div>
						<Button onClick={() => window.location.reload()}>Retry</Button>
					</div>
				</div>
				<Footer />
			</div>
		);
	}

	if (!user) {
		console.log("🔍 GamificationPage: No user data, showing fallback");
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<div className="flex-1 flex items-center justify-center">
					<div className="text-center">
						<div className="text-lg text-gray-600 mb-4">
							No user data available
						</div>
						<Button onClick={() => navigate("/profile")}>Go to Profile</Button>
					</div>
				</div>
				<Footer />
			</div>
		);
	}

	console.log(
		"🔍 GamificationPage: Rendering GamificationDashboard with userId:",
		user.id
	);
	return <GamificationDashboard userId={user.id} />;
}
