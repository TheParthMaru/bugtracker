/**
 * Navbar Component
 *
 * Main navigation component that provides consistent navigation across the application.
 * It displays the application logo and main navigation links.
 *
 * Key Features:
 * - Application branding with logo
 * - Conditional navigation based on auth state
 * - User avatar with dropdown menu
 * - Responsive design
 * - Consistent styling with Tailwind CSS
 *
 * Navigation Links:
 * - Home: Application landing page (/home)
 * - Login: Authentication page (/auth/login)
 * - Register: User registration (/auth/register)
 * - Teams: Team management (when authenticated)
 * - My Teams: User's teams (when authenticated)
 */

import { Link, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import API from "@/services/api";
import { Settings, LogOut } from "lucide-react";
import { NotificationBell } from "@/components/notifications";

interface User {
	firstName: string;
	lastName: string;
}

export default function Navbar() {
	const [isOpen, setIsOpen] = useState(false);
	const [user, setUser] = useState<User | null>(null);
	const navigate = useNavigate();

	useEffect(() => {
		const token = localStorage.getItem("bugtracker_token");
		if (token) {
			// Fetch user data
			API.get("/profile")
				.then((response) => setUser(response.data))
				.catch(() => {
					localStorage.removeItem("bugtracker_token");
					navigate("/auth/login");
				});
		}
	}, [navigate]);

	const handleLogout = () => {
		localStorage.removeItem("bugtracker_token");
		navigate("/auth/login");
	};

	return (
		<nav className="w-full flex justify-between items-center p-4 bg-white shadow-sm">
			<div className="flex items-center space-x-8">
				<Link to="/" className="text-xl font-bold text-600">
					BugTracker
				</Link>

				{/* Main Navigation Links */}
				<div className="hidden md:flex items-center space-x-6">
					<Link
						to="/projects"
						className="text-sm font-medium text-gray-700 hover:text-gray-900 transition-colors"
					>
						Projects
					</Link>
					<Link
						to="/teams"
						className="text-sm font-medium text-gray-700 hover:text-gray-900 transition-colors"
					>
						Teams
					</Link>
					<Link
						to="/bugs"
						className="text-sm font-medium text-gray-700 hover:text-gray-900 transition-colors"
					>
						Bugs
					</Link>
					<Link
						to="/leaderboard"
						className="text-sm font-medium text-gray-700 hover:text-gray-900 transition-colors"
					>
						Leaderboard
					</Link>
				</div>
			</div>

			{user ? (
				<div className="flex items-center space-x-3">
					{/* Notification Bell */}
					<NotificationBell />

					{/* User Menu */}
					<div className="relative">
						<button
							onClick={() => setIsOpen(!isOpen)}
							className="flex items-center space-x-2 focus:outline-none"
						>
							<div className="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white">
								{user.firstName[0]}
								{user.lastName[0]}
							</div>
							<span className="text-sm font-medium text-gray-700">
								Welcome, {user.firstName}
							</span>
						</button>

						{isOpen && (
							<div className="absolute right-0 mt-2 w-56 bg-white rounded-md shadow-lg py-1 z-10 border">
								{/* User Info Section */}
								<div className="px-4 py-2 border-b border-gray-100">
									<div className="text-sm font-medium text-gray-900">
										{user.firstName} {user.lastName}
									</div>
									<div className="text-xs text-gray-500">Signed in</div>
								</div>

								{/* Navigation Links */}
								{/* <Link
								to="/projects"
								className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
								onClick={() => setIsOpen(false)}
							>
								Projects
							</Link>
							<Link
								to="/teams"
								className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
								onClick={() => setIsOpen(false)}
							>
								Teams
							</Link>
							<Link
								to="/bugs"
								className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
								onClick={() => setIsOpen(false)}
							>
								Bugs
							</Link>
							<Link
								to="/leaderboard"
								className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
								onClick={() => setIsOpen(false)}
							>
								Leaderboard
							</Link> */}
								<Link
									to="/profile"
									className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
									onClick={() => setIsOpen(false)}
								>
									<Settings className="h-4 w-4 mr-3 text-gray-500" />
									Profile
								</Link>

								{/* Divider */}
								<div className="border-t border-gray-100 my-1"></div>

								{/* Logout */}
								<button
									onClick={() => {
										handleLogout();
										setIsOpen(false);
									}}
									className="flex items-center w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
								>
									<LogOut className="h-4 w-4 mr-3 text-gray-500" />
									Sign Out
								</button>
							</div>
						)}
					</div>
				</div>
			) : (
				<div className="space-x-4">
					<Link
						to="/auth/login"
						className="text-sm font-medium px-4 py-2 bg-primary text-primary-foreground rounded-md"
					>
						Login
					</Link>
					<Link
						to="/auth/register"
						className="text-sm font-medium px-4 py-2 bg-secondary text-secondary-foreground rounded-md"
					>
						Register
					</Link>
				</div>
			)}
		</nav>
	);
}
