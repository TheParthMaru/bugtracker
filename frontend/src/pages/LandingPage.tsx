/**
 * LandingPage Component
 *
 * Main landing page of the BugTracker application that serves as the entry point for users.
 * For authenticated users, it shows a personalized dashboard with their projects, teams, and analytics.
 * For unauthenticated users, it shows the application overview and features.
 *
 * Key Features:
 * - Personalized dashboard for authenticated users
 * - Real-time user analytics and statistics
 * - Quick access to user's projects and teams
 * - Feature highlights for new users
 * - Responsive grid layout
 * - Navigation bar integration
 *
 * Sections:
 * - Hero: Personalized welcome or application introduction
 * - User Analytics: Real user data (projects, teams, activity)
 * - Quick Actions: Browse projects/teams, create new items
 * - Features: Grid of key application features (for new users)
 * - Stats: Application usage statistics (for new users)
 */

import { useEffect, useState, useMemo } from "react";
import Navbar from "@/components/Navbar";
import {
	BarChart3,
	Users,
	Bug,
	GitPullRequest,
	Folder,
	Plus,
	ArrowRight,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { projectService } from "@/services/projectService";
import { teamService } from "@/services/teamService";
import { userService } from "@/services/userService";
import { bugService } from "@/services/bugService";
// Removed unused imports - daily login notification now handled in LoginPage.tsx
import type { Project, ProjectStats } from "@/types/project";
import type { Team } from "@/types/team";
import type { User } from "@/types/user";
import type { Bug as BugType } from "@/types/bug";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ProjectRoleBadge } from "@/components/projects";
import { RoleBadge } from "@/components/teams";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";

export function LandingPage() {
	const navigate = useNavigate();
	const token = localStorage.getItem("bugtracker_token");

	// State for authenticated user data
	const [user, setUser] = useState<User | null>(null);
	const [userProjects, setUserProjects] = useState<Project[]>([]);
	const [userTeams, setUserTeams] = useState<Team[]>([]);
	const [userBugs, setUserBugs] = useState<BugType[]>([]);
	const [projectStats, setProjectStats] = useState<ProjectStats | null>(null);
	const [loading, setLoading] = useState(true);
	const [activeTab, setActiveTab] = useState<"bugs" | "projects" | "teams">(
		"bugs"
	);
	const [bugSortBy, setBugSortBy] = useState<
		"ticket_asc" | "ticket_desc" | "created_desc" | "created_asc"
	>("ticket_asc");

	// Sort bugs based on selected sort option
	const sortedBugs = useMemo(() => {
		const sorted = [...userBugs];
		switch (bugSortBy) {
			case "ticket_asc":
				return sorted.sort(
					(a, b) => (a.projectTicketNumber || 0) - (b.projectTicketNumber || 0)
				);
			case "ticket_desc":
				return sorted.sort(
					(a, b) => (b.projectTicketNumber || 0) - (a.projectTicketNumber || 0)
				);
			case "created_desc":
				return sorted.sort(
					(a, b) =>
						new Date(b.createdAt || "").getTime() -
						new Date(a.createdAt || "").getTime()
				);
			case "created_asc":
				return sorted.sort(
					(a, b) =>
						new Date(a.createdAt || "").getTime() -
						new Date(b.createdAt || "").getTime()
				);
			default:
				return sorted;
		}
	}, [userBugs, bugSortBy]);

	// Helper function to get project name from team data
	const getProjectName = (team: Team): string => {
		// First try to use the projectName we added during loading
		if ((team as any).projectName) {
			return (team as any).projectName;
		}
		// Fallback to looking up by projectSlug
		if (team.projectSlug) {
			const project = userProjects.find(
				(p) => p.projectSlug === team.projectSlug
			);
			return project?.name || team.projectSlug;
		}
		return "No Project";
	};

	// Load user data if authenticated
	useEffect(() => {
		const loadUserData = async () => {
			if (!token) {
				console.log("No token found, setting loading to false");
				setLoading(false);
				return;
			}

			try {
				console.log("Starting to load user data...");
				setLoading(true);

				// Load user profile
				console.log("Loading user profile...");
				const userData = await userService.getCurrentUser();
				console.log("User profile loaded:", userData);
				setUser(userData);

				// Load user's projects and bugs in parallel first
				console.log("Loading projects and bugs in parallel...");
				const [projects, bugs] = await Promise.allSettled([
					projectService.getUserProjects(),
					bugService.getMyAssignedBugs(),
				]);

				// Handle projects result
				let projectsData: Project[] = [];
				if (projects.status === "fulfilled") {
					projectsData = projects.value;
					setUserProjects(projectsData);
					console.log("User projects loaded successfully:", projectsData);
				} else {
					console.error("Failed to load user projects:", projects.reason);
					setUserProjects([]);
				}

				// Load only teams where the user is actually a member
				console.log("Loading user's teams...");
				try {
					const userTeamsData = await teamService.getUserTeams();
					console.log("User teams loaded successfully:", userTeamsData);
					setUserTeams(userTeamsData);
				} catch (error) {
					console.error("Failed to load user teams:", error);
					setUserTeams([]);
				}

				// Handle bugs result
				let bugsData: BugType[] = [];
				if (bugs.status === "fulfilled") {
					bugsData = bugs.value;
					setUserBugs(bugsData);
					console.log("User bugs loaded successfully:", bugsData);
				} else {
					console.error("Failed to load user bugs:", bugs.reason);
					setUserBugs([]);
				}

				// Calculate project statistics from the actual data we received
				const stats: ProjectStats = {
					totalProjects: projectsData.length,
					adminProjects: projectsData.filter((p) => p.userRole === "ADMIN")
						.length,
					memberProjects: projectsData.filter((p) => p.userRole === "MEMBER")
						.length,
					pendingRequests: projectsData.reduce(
						(total, project) => total + (project.pendingRequestCount || 0),
						0
					),
				};
				console.log("Calculated stats from actual data:", stats);
				setProjectStats(stats);

				// Daily login notification is now handled in LoginPage.tsx
				// No need to check transactions here anymore
			} catch (error) {
				console.error("Failed to load user data:", error);
				// Only clear token if it's an authentication error
				if (error instanceof Error && error.message.includes("401")) {
					localStorage.removeItem("bugtracker_token");
				}
			} finally {
				console.log("Setting loading to false in finally block");
				setLoading(false);
			}
		};

		loadUserData();
	}, [token]);

	const handleBrowseProjects = () => {
		if (token) {
			navigate("/projects?filter=my-projects");
		} else {
			navigate("/auth/login");
		}
	};

	const handleBrowseTeams = () => {
		if (token) {
			navigate("/teams");
		} else {
			navigate("/auth/login");
		}
	};

	const handleCreateProject = () => {
		if (token) {
			navigate("/projects?create=1");
		} else {
			navigate("/auth/login");
		}
	};

	const handleCreateTeam = () => {
		if (token) {
			navigate("/teams");
		} else {
			navigate("/auth/login");
		}
	};

	const handleBrowseBugs = () => {
		if (token) {
			navigate("/bugs");
		} else {
			navigate("/auth/login");
		}
	};

	const handleCreateBug = () => {
		if (token) {
			navigate("/bugs");
		} else {
			navigate("/auth/login");
		}
	};

	// Show loading state
	if (loading) {
		console.log("Showing loading state");
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8">
					<div className="flex items-center justify-center h-64">
						<div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
						<span className="ml-2 text-muted-foreground">
							Loading your dashboard...
						</span>
					</div>
				</main>
			</div>
		);
	}

	// Authenticated User Dashboard
	console.log(
		"Checking authenticated dashboard, token:",
		!!token,
		"user:",
		!!user
	);
	if (token && user) {
		return (
			<div className="min-h-screen flex flex-col">
				<Navbar />
				<main className="flex-1 container mx-auto px-4 py-8">
					{/* Personalized Welcome Section */}
					<div className="mb-8">
						<h1 className="text-3xl font-bold mb-2">
							Welcome back, {user.firstName}! 👋
						</h1>
						<p className="text-muted-foreground">
							Here's what's happening with your projects and teams today.
						</p>
					</div>

					{/* NEW LAYOUT: Tabular Dashboard with Tabs */}
					<div className="space-y-6">
						{/* Analytics Summary Cards */}
						<div className="grid grid-cols-1 md:grid-cols-4 gap-4">
							<Card>
								<CardHeader className="flex flex-row items-center justify-between">
									<CardTitle className="font-medium">Total Projects</CardTitle>
									<Folder className="h-5 w-5 text-muted-foreground" />
								</CardHeader>
								<CardContent>
									<div className="text-2xl font-bold">
										{userProjects.length}
									</div>
									<p className="pt-2 text-muted-foreground">
										Projects you're part of
									</p>
								</CardContent>
							</Card>

							<Card>
								<CardHeader className="flex flex-row items-center justify-between">
									<CardTitle className="font-medium">Total Teams</CardTitle>
									<Users className="h-5 w-5 text-muted-foreground" />
								</CardHeader>
								<CardContent>
									<div className="text-2xl font-bold">{userTeams.length}</div>
									<p className="pt-2 text-muted-foreground">
										Teams you're part of
									</p>
								</CardContent>
							</Card>

							<Card>
								<CardHeader className="flex flex-row items-center justify-between">
									<CardTitle className="font-medium">My Tickets</CardTitle>
									<Bug className="h-5 w-5 text-muted-foreground" />
								</CardHeader>
								<CardContent>
									<div className="text-2xl font-bold">{userBugs.length}</div>
									<p className="pt-2 text-muted-foreground">
										Bugs assigned to you
									</p>
								</CardContent>
							</Card>

							<Card>
								<CardHeader className="flex flex-row items-center justify-between">
									<CardTitle className="font-medium">Quick Actions</CardTitle>
									<Plus className="h-5 w-5 text-muted-foreground" />
								</CardHeader>
								<CardContent>
									<div className="space-y-2">
										<Button
											onClick={handleCreateProject}
											variant="outline"
											size="sm"
											className="w-full justify-start"
										>
											<Plus className="h-3 w-3 mr-2" />
											New Project
										</Button>
										<Button
											onClick={handleCreateTeam}
											variant="outline"
											size="sm"
											className="w-full justify-start"
										>
											<Plus className="h-3 w-3 mr-2" />
											New Team
										</Button>
										<Button
											onClick={handleCreateBug}
											variant="outline"
											size="sm"
											className="w-full justify-start"
										>
											<Plus className="h-3 w-3 mr-2" />
											Create Bug
										</Button>
									</div>
								</CardContent>
							</Card>
						</div>

						{/* Tabbed Data View */}
						<Card>
							<CardHeader>
								<div className="flex items-center justify-between">
									<CardTitle>My Dashboard</CardTitle>
									<hr />
									<div className="flex space-x-1">
										<Button
											variant={activeTab === "bugs" ? "default" : "outline"}
											size="sm"
											onClick={() => setActiveTab("bugs")}
											className="flex items-center gap-2"
										>
											<Bug className="h-4 w-4" />
											My Tickets ({userBugs.length})
										</Button>
										<Button
											variant={activeTab === "projects" ? "default" : "outline"}
											size="sm"
											onClick={() => setActiveTab("projects")}
											className="flex items-center gap-2"
										>
											<Folder className="h-4 w-4" />
											My Projects ({userProjects.length})
										</Button>
										<Button
											variant={activeTab === "teams" ? "default" : "outline"}
											size="sm"
											onClick={() => setActiveTab("teams")}
											className="flex items-center gap-2"
										>
											<Users className="h-4 w-4" />
											My Teams ({userTeams.length})
										</Button>
									</div>
								</div>
							</CardHeader>
							<CardContent>
								{/* Bugs Tab */}
								{activeTab === "bugs" && (
									<div>
										<div className="flex items-center justify-between mb-4">
											<h3 className="text-lg font-semibold">My Tickets</h3>
											<div className="flex items-center gap-2">
												<Select
													value={bugSortBy}
													onValueChange={(
														value:
															| "ticket_asc"
															| "ticket_desc"
															| "created_desc"
															| "created_asc"
													) => setBugSortBy(value)}
												>
													<SelectTrigger className="w-48">
														<SelectValue />
													</SelectTrigger>
													<SelectContent>
														<SelectItem value="ticket_asc">
															Ticket # (Low to High)
														</SelectItem>
														<SelectItem value="ticket_desc">
															Ticket # (High to Low)
														</SelectItem>
														<SelectItem value="created_desc">
															Newest First
														</SelectItem>
														<SelectItem value="created_asc">
															Oldest First
														</SelectItem>
													</SelectContent>
												</Select>
												<Button
													variant="outline"
													size="sm"
													onClick={handleBrowseBugs}
												>
													View All
													<ArrowRight className="h-4 w-4 ml-1" />
												</Button>
											</div>
										</div>
										{sortedBugs.length > 0 ? (
											<div className="overflow-x-auto">
												<table className="w-full">
													<thead>
														<tr className="border-b">
															<th className="text-left p-2 font-medium">
																Ticket Number
															</th>
															<th className="text-left p-2 font-medium">
																Title
															</th>
															<th className="text-left p-2 font-medium">
																Project Name
															</th>
															<th className="text-left p-2 font-medium">
																Status
															</th>
															<th className="text-left p-2 font-medium">
																Priority
															</th>
															<th className="text-left p-2 font-medium">
																Created
															</th>
														</tr>
													</thead>
													<tbody>
														{sortedBugs.slice(0, 10).map((bug) => (
															<tr
																key={bug.projectTicketNumber}
																className="border-b hover:bg-muted/50"
															>
																<td className="p-2">
																	<button
																		onClick={() =>
																			navigate(
																				`/projects/${bug.projectSlug}/bugs/${bug.projectTicketNumber}`
																			)
																		}
																		className="font-mono font-medium text-sm text-blue-600 hover:text-blue-800 hover:underline focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 rounded"
																	>
																		{bug.projectTicketNumber}
																	</button>
																</td>
																<td className="p-2">
																	<button
																		onClick={() =>
																			navigate(
																				`/projects/${bug.projectSlug}/bugs/${bug.projectTicketNumber}`
																			)
																		}
																		className="font-medium text-left hover:text-blue-600 hover:underline focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 rounded"
																	>
																		{bug.title}
																	</button>
																</td>
																<td className="p-2 text-sm">
																	{bug.projectName}
																</td>
																<td className="p-2">
																	<span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
																		{bug.status}
																	</span>
																</td>
																<td className="p-2">
																	<span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">
																		{bug.priority}
																	</span>
																</td>
																<td className="p-2 text-sm text-muted-foreground">
																	{new Date(
																		bug.createdAt || ""
																	).toLocaleDateString()}
																</td>
															</tr>
														))}
													</tbody>
												</table>
											</div>
										) : (
											<div className="text-center py-8">
												<Bug className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
												<h3 className="text-lg font-medium text-muted-foreground mb-2">
													No Tickets Yet
												</h3>
												<p className="text-sm text-muted-foreground mb-4">
													You don't have any bugs assigned to you yet.
												</p>
												<Button onClick={handleBrowseBugs} variant="outline">
													Browse Bugs
												</Button>
											</div>
										)}
									</div>
								)}

								{/* Projects Tab */}
								{activeTab === "projects" && (
									<div>
										<div className="flex items-center justify-between mb-4">
											<h3 className="text-lg font-semibold">My Projects</h3>
											<Button
												variant="outline"
												size="sm"
												onClick={handleBrowseProjects}
											>
												View All
												<ArrowRight className="h-4 w-4 ml-1" />
											</Button>
										</div>
										{userProjects.length > 0 ? (
											<div className="overflow-x-auto">
												<table className="w-full">
													<thead>
														<tr className="border-b">
															<th className="text-left p-2 font-medium">
																Project Name
															</th>
															<th className="text-left p-2 font-medium">
																Role
															</th>
															<th className="text-left p-2 font-medium">
																Members
															</th>
															<th className="text-left p-2 font-medium">
																Created
															</th>
														</tr>
													</thead>
													<tbody>
														{userProjects
															.sort((a, b) => a.name.localeCompare(b.name))
															.slice(0, 10)
															.map((project) => (
																<tr
																	key={project.id}
																	className="border-b hover:bg-muted/50 cursor-pointer"
																	onClick={() =>
																		navigate(`/projects/${project.projectSlug}`)
																	}
																>
																	<td className="p-2">
																		<div className="font-medium">
																			{project.name}
																		</div>
																		<div className="text-xs text-muted-foreground">
																			{project.projectSlug}
																		</div>
																	</td>
																	<td className="p-2">
																		{project.userRole && (
																			<ProjectRoleBadge
																				role={project.userRole}
																				size="sm"
																			/>
																		)}
																	</td>
																	<td className="p-2 text-sm">
																		{project.memberCount} members
																	</td>
																	<td className="p-2 text-sm text-muted-foreground">
																		{new Date(
																			project.createdAt || ""
																		).toLocaleDateString()}
																	</td>
																</tr>
															))}
													</tbody>
												</table>
											</div>
										) : (
											<div className="text-center py-8">
												<Folder className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
												<h3 className="text-lg font-medium text-muted-foreground mb-2">
													No Projects Yet
												</h3>
												<p className="text-sm text-muted-foreground mb-4">
													You haven't joined any projects yet.
												</p>
												<Button
													onClick={handleBrowseProjects}
													variant="outline"
												>
													Browse Projects
												</Button>
											</div>
										)}
									</div>
								)}

								{/* Teams Tab */}
								{activeTab === "teams" && (
									<div>
										<div className="flex items-center justify-between mb-4">
											<h3 className="text-lg font-semibold">My Teams</h3>
											<Button
												variant="outline"
												size="sm"
												onClick={handleBrowseTeams}
											>
												View All
												<ArrowRight className="h-4 w-4 ml-1" />
											</Button>
										</div>
										{userTeams.length > 0 ? (
											<div className="overflow-x-auto">
												<table className="w-full">
													<thead>
														<tr className="border-b">
															<th className="text-left p-2 font-medium">
																Team Name
															</th>
															<th className="text-left p-2 font-medium">
																Project
															</th>
															<th className="text-left p-2 font-medium">
																Role
															</th>
															<th className="text-left p-2 font-medium">
																Members
															</th>
															<th className="text-left p-2 font-medium">
																Created
															</th>
														</tr>
													</thead>
													<tbody>
														{userTeams
															.sort(
																(a, b) =>
																	new Date(b.createdAt || "").getTime() -
																	new Date(a.createdAt || "").getTime()
															)
															.slice(0, 10)
															.map((team) => (
																<tr
																	key={team.id}
																	className="border-b hover:bg-muted/50 cursor-pointer"
																	onClick={() =>
																		navigate(
																			`/projects/${team.projectSlug}/teams/${team.teamSlug}`
																		)
																	}
																>
																	<td className="p-2">
																		<div className="font-medium">
																			{team.name}
																		</div>
																		<div className="text-xs text-muted-foreground">
																			{team.teamSlug}
																		</div>
																	</td>
																	<td className="p-2">
																		<div className="font-medium text-sm">
																			{getProjectName(team)}
																		</div>
																		<div className="text-xs text-muted-foreground">
																			{team.projectSlug || "No Project"}
																		</div>
																	</td>
																	<td className="p-2">
																		{team.currentUserRole && (
																			<RoleBadge
																				role={team.currentUserRole}
																				size="sm"
																			/>
																		)}
																	</td>
																	<td className="p-2 text-sm">
																		{team.memberCount} members
																	</td>
																	<td className="p-2 text-sm text-muted-foreground">
																		{new Date(
																			team.createdAt || ""
																		).toLocaleDateString()}
																	</td>
																</tr>
															))}
													</tbody>
												</table>
											</div>
										) : (
											<div className="text-center py-8">
												<Users className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
												<h3 className="text-lg font-medium text-muted-foreground mb-2">
													No Teams Yet
												</h3>
												<p className="text-sm text-muted-foreground mb-4">
													You haven't joined any teams yet.
												</p>
												<Button onClick={handleBrowseTeams} variant="outline">
													Browse Teams
												</Button>
											</div>
										)}
									</div>
								)}
							</CardContent>
						</Card>
					</div>
				</main>
			</div>
		);
	}

	// Unauthenticated User Landing Page
	console.log("Showing unauthenticated landing page");
	return (
		<div className="min-h-screen flex flex-col">
			<Navbar />
			<main className="flex-1 container mx-auto px-4 py-8">
				<div className="flex flex-col items-center justify-center space-y-6 mb-16">
					<h2 className="text-3xl font-bold">Welcome to BugTracker</h2>
					<p className="text-muted-foreground text-center max-w-2xl">
						Track, manage, and resolve bugs efficiently with our powerful bug
						tracking and issue reporting system. Streamline your development
						workflow and keep your projects organized.
					</p>
					<div className="flex space-x-4">
						<button
							onClick={handleBrowseProjects}
							className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90"
						>
							Browse Projects
						</button>
						<button
							onClick={handleBrowseTeams}
							className="px-4 py-2 bg-secondary text-secondary-foreground rounded-md hover:bg-secondary/90"
						>
							Browse Teams
						</button>
					</div>
				</div>

				{/* Features Section */}
				<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-16">
					<div className="p-6 rounded-lg border bg-card">
						<Bug className="h-8 w-8 text-primary mb-4" />
						<h3 className="text-lg font-semibold mb-2">Bug Tracking</h3>
						<p className="text-muted-foreground">
							Track and manage bugs with detailed reports and status updates.
						</p>
					</div>
					<div className="p-6 rounded-lg border bg-card">
						<GitPullRequest className="h-8 w-8 text-primary mb-4" />
						<h3 className="text-lg font-semibold mb-2">Issue Management</h3>
						<p className="text-muted-foreground">
							Report and track issues with customizable workflows.
						</p>
					</div>
					<div className="p-6 rounded-lg border bg-card">
						<Users className="h-8 w-8 text-primary mb-4" />
						<h3 className="text-lg font-semibold mb-2">Team Collaboration</h3>
						<p className="text-muted-foreground">
							Work together seamlessly with team-based project management.
						</p>
					</div>
					<div className="p-6 rounded-lg border bg-card">
						<BarChart3 className="h-8 w-8 text-primary mb-4" />
						<h3 className="text-lg font-semibold mb-2">Analytics</h3>
						<p className="text-muted-foreground">
							Get insights with detailed reports and analytics.
						</p>
					</div>
				</div>

				{/* Stats Section - Show real data or remove for unauthenticated users */}
				<div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-16">
					<div className="p-6 rounded-lg border bg-card">
						<h3 className="text-2xl font-bold text-primary">Join Us</h3>
						<p className="text-muted-foreground">Start tracking bugs today</p>
					</div>
					<div className="p-6 rounded-lg border bg-card">
						<h3 className="text-2xl font-bold text-primary">Collaborate</h3>
						<p className="text-muted-foreground">Work with your team</p>
					</div>
					<div className="p-6 rounded-lg border bg-card">
						<h3 className="text-2xl font-bold text-primary">Improve</h3>
						<p className="text-muted-foreground">Build better software</p>
					</div>
				</div>
			</main>
			<footer className="border-t py-6">
				<div className="container mx-auto px-4">
					<p className="text-center text-sm text-muted-foreground">
						© 2025 BugTracker. Made with ♥️ By Parth Maru. All rights reserved.
					</p>
				</div>
			</footer>
		</div>
	);
}
