/**
 * App Component
 *
 * Root component of the BugTracker application that sets up routing and
 * defines the main application structure.
 *
 * Key Features:
 * - React Router setup for navigation
 * - Route definitions for main application pages
 * - 404 handling for undefined routes
 *
 * Routes:
 * - /home: Landing page
 * - /auth/login: Authentication page
 * - /auth/register: User registration
 * - /profile: User profile (protected)
 * - /teams: Team management (protected)
 * - /projects: Project management (protected)
 * - /projects/:projectSlug/teams: Project teams (protected)
 * - /bugs: Bug management (protected)
 * - /bugs/create: Create new bug (protected)
 * - /bugs/:bugId: Bug details (protected)
 * - /projects/:projectSlug/analytics: Project analytics (protected)
 */

import { Routes, Route } from "react-router-dom";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { ToastManager } from "./components/notifications/ToastManager";
import { useState, useEffect } from "react";
import { LandingPage } from "./pages/LandingPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ProfilePage from "./pages/ProfilePage";
import { TeamsPage } from "./pages/TeamsPage";

import { TeamEditPage } from "./pages/TeamEditPage";

import { ProjectsListPage } from "./pages/ProjectsListPage";
import { ProjectDetailPage } from "./pages/ProjectDetailPage";
import { ProjectEditPage } from "./pages/ProjectEditPage";
import { ProjectMembersPage } from "./pages/ProjectMembersPage";
import { ProjectTeamDetailPage } from "./pages/ProjectTeamDetailPage";
import { ProjectTeamsPage } from "./pages/ProjectTeamsPage";
import { BugsPage } from "./pages/BugsPage";
import { CreateBugPage } from "./pages/CreateBugPage";
import { BugEditPage } from "./pages/BugEditPage";
import { AnalyticsPage } from "./pages/AnalyticsPage";
import { SimilarityAnalysisPage } from "./pages/SimilarityAnalysisPage";
import { BugDetailPage } from "./components/bugs";
import ProtectedRoute from "./components/ProtectedRoute";
import GamificationPage from "./pages/GamificationPage";
import UserGamificationProfilePage from "./pages/UserGamificationProfilePage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { NotificationPreferencesPage } from "./pages/NotificationPreferencesPage";
import ForgotPasswordPage from "./pages/ForgotPasswordPage";
import ResetPasswordPage from "./pages/ResetPasswordPage";
import { API_BASE_URL } from "./config/constants";

function App() {
	// Get current user ID for WebSocket notifications
	const [userId, setUserId] = useState<string | null>(null);

	useEffect(() => {
		const token = localStorage.getItem("bugtracker_token");
		if (token) {
			fetch(`${API_BASE_URL}/profile`, {
				headers: { Authorization: `Bearer ${token}` },
			})
				.then((res) => res.json())
				.then((user) => setUserId(user.id))
				.catch((err) => console.error("Failed to get user ID:", err));
		}
	}, []);

	return (
		<ErrorBoundary>
			<div className="min-h-screen flex flex-col">
				<Routes>
					<Route
						path="/"
						element={
							<ErrorBoundary>
								<LandingPage />
							</ErrorBoundary>
						}
					/>
					<Route
						path="/home"
						element={
							<ErrorBoundary>
								<LandingPage />
							</ErrorBoundary>
						}
					/>
					<Route path="/auth/login" element={<LoginPage />} />
					<Route path="/auth/register" element={<RegisterPage />} />
					<Route
						path="/auth/forgot-password"
						element={<ForgotPasswordPage />}
					/>
					<Route path="/auth/reset-password" element={<ResetPasswordPage />} />
					<Route
						path="/profile"
						element={
							<ProtectedRoute>
								<ProfilePage />
							</ProtectedRoute>
						}
					/>
					{/* Team Management Routes */}
					<Route
						path="/teams"
						element={
							<ProtectedRoute>
								<TeamsPage />
							</ProtectedRoute>
						}
					/>

					{/* Project Management Routes */}
					<Route
						path="/projects"
						element={
							<ProtectedRoute>
								<ProjectsListPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug"
						element={
							<ProtectedRoute>
								<ProjectDetailPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug/edit"
						element={
							<ProtectedRoute>
								<ProjectEditPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug/members"
						element={
							<ProtectedRoute>
								<ProjectMembersPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug/teams"
						element={
							<ProtectedRoute>
								<ProjectTeamsPage />
							</ProtectedRoute>
						}
					/>

					{/* Bug Management Routes */}
					<Route
						path="/bugs"
						element={
							<ProtectedRoute>
								<BugsPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug/bugs"
						element={
							<ProtectedRoute>
								<BugsPage />
							</ProtectedRoute>
						}
					/>
					{/* Removed conflicting global bug creation route - bugs should always be project-scoped */}
					<Route
						path="/projects/:projectSlug/bugs/create"
						element={
							<ProtectedRoute>
								<CreateBugPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug/bugs/:projectTicketNumber/edit"
						element={
							<ProtectedRoute>
								<BugEditPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug/bugs/:projectTicketNumber"
						element={
							<ProtectedRoute>
								<BugDetailPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug/similarity-analysis"
						element={
							<ProtectedRoute>
								<SimilarityAnalysisPage />
							</ProtectedRoute>
						}
					/>
					{/* Removed conflicting global bug routes - bugs should always be project-scoped */}

					{/* Analytics Routes */}
					<Route
						path="/projects/:projectSlug/analytics"
						element={
							<ProtectedRoute>
								<AnalyticsPage />
							</ProtectedRoute>
						}
					/>

					{/* Notification Routes */}
					<Route
						path="/notifications"
						element={
							<ProtectedRoute>
								<NotificationsPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/notifications/settings"
						element={
							<ProtectedRoute>
								<NotificationPreferencesPage />
							</ProtectedRoute>
						}
					/>

					{/* Leaderboard Routes */}
					<Route
						path="/leaderboard"
						element={
							<ProtectedRoute>
								<GamificationPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/leaderboard/profile/:userId"
						element={
							<ProtectedRoute>
								<UserGamificationProfilePage />
							</ProtectedRoute>
						}
					/>

					{/* Project-Teams Integration Routes */}
					<Route
						path="/projects/:projectSlug/teams"
						element={
							<ProtectedRoute>
								<ProjectTeamsPage />
							</ProtectedRoute>
						}
					/>

					<Route
						path="/projects/:projectSlug/teams/:teamSlug"
						element={
							<ProtectedRoute>
								<ProjectTeamDetailPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug/teams/:teamSlug/edit"
						element={
							<ProtectedRoute>
								<TeamEditPage />
							</ProtectedRoute>
						}
					/>
					<Route
						path="/projects/:projectSlug/teams/:teamSlug/members"
						element={
							<ProtectedRoute>
								<ProjectTeamDetailPage />
							</ProtectedRoute>
						}
					/>

					<Route
						path="*"
						element={<div className="p-10 text-center">404 - Not Found</div>}
					/>
				</Routes>
			</div>
			<ToastManager userId={userId || undefined} />
		</ErrorBoundary>
	);
}

export default App;
