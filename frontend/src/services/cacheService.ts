/**
 * CacheService
 *
 * Simple in-memory cache service for team data with TTL support.
 * Helps reduce API calls for frequently accessed data.
 *
 * Features:
 * - TTL (time-to-live) support
 * - Memory-based storage
 * - Cache invalidation
 * - Size limits
 * - Type-safe operations
 */

interface CacheItem<T> {
	data: T;
	timestamp: number;
	ttl: number;
}

interface CacheStats {
	hits: number;
	misses: number;
	size: number;
	maxSize: number;
}

export class CacheService {
	private cache = new Map<string, CacheItem<any>>();
	private readonly maxSize: number;
	private stats: CacheStats;

	constructor(maxSize: number = 100) {
		this.maxSize = maxSize;
		this.stats = {
			hits: 0,
			misses: 0,
			size: 0,
			maxSize,
		};
	}

	/**
	 * Set cache item with TTL
	 */
	set<T>(key: string, data: T, ttlMinutes: number = 5): void {
		// Clean expired items before adding
		this.cleanExpired();

		// Remove oldest items if cache is full
		if (this.cache.size >= this.maxSize) {
			this.evictOldest();
		}

		const item: CacheItem<T> = {
			data,
			timestamp: Date.now(),
			ttl: ttlMinutes * 60 * 1000, // Convert minutes to milliseconds
		};

		this.cache.set(key, item);
		this.stats.size = this.cache.size;
	}

	/**
	 * Get cache item
	 */
	get<T>(key: string): T | null {
		const item = this.cache.get(key);

		if (!item) {
			this.stats.misses++;
			return null;
		}

		// Check if item has expired
		if (Date.now() - item.timestamp > item.ttl) {
			this.cache.delete(key);
			this.stats.misses++;
			this.stats.size = this.cache.size;
			return null;
		}

		this.stats.hits++;
		return item.data;
	}

	/**
	 * Delete cache item
	 */
	delete(key: string): boolean {
		const result = this.cache.delete(key);
		this.stats.size = this.cache.size;
		return result;
	}

	/**
	 * Check if cache has key
	 */
	has(key: string): boolean {
		const item = this.cache.get(key);

		if (!item) {
			return false;
		}

		// Check if item has expired
		if (Date.now() - item.timestamp > item.ttl) {
			this.cache.delete(key);
			this.stats.size = this.cache.size;
			return false;
		}

		return true;
	}

	/**
	 * Clear all cache
	 */
	clear(): void {
		this.cache.clear();
		this.stats.size = 0;
		this.stats.hits = 0;
		this.stats.misses = 0;
	}

	/**
	 * Get cache statistics
	 */
	getStats(): CacheStats {
		return { ...this.stats };
	}

	/**
	 * Clean expired items
	 */
	private cleanExpired(): void {
		const now = Date.now();
		const keysToDelete: string[] = [];

		for (const [key, item] of this.cache.entries()) {
			if (now - item.timestamp > item.ttl) {
				keysToDelete.push(key);
			}
		}

		keysToDelete.forEach((key) => this.cache.delete(key));
		this.stats.size = this.cache.size;
	}

	/**
	 * Evict oldest items when cache is full
	 */
	private evictOldest(): void {
		let oldestKey: string | null = null;
		let oldestTimestamp = Date.now();

		for (const [key, item] of this.cache.entries()) {
			if (item.timestamp < oldestTimestamp) {
				oldestTimestamp = item.timestamp;
				oldestKey = key;
			}
		}

		if (oldestKey) {
			this.cache.delete(oldestKey);
			this.stats.size = this.cache.size;
		}
	}

	/**
	 * Get cache hit rate
	 */
	getHitRate(): number {
		const total = this.stats.hits + this.stats.misses;
		return total > 0 ? (this.stats.hits / total) * 100 : 0;
	}
}

/**
 * Team-specific cache service
 */
export class TeamCacheService {
	private cache = new CacheService(50); // Smaller cache for team data

	// Cache keys
	private static readonly KEYS = {
		TEAMS_LIST: "teams:list",
		USER_TEAMS: "user:teams",
		TEAM_DETAIL: (id: string) => `team:${id}`,
		TEAM_MEMBERS: (id: string) => `team:${id}:members`,
		TEAM_BY_SLUG: (teamSlug: string) => `team:${teamSlug}`,
	};

	// Cache TTL in minutes
	private static readonly TTL = {
		TEAMS_LIST: 2, // 2 minutes for team lists
		USER_TEAMS: 5, // 5 minutes for user teams
		TEAM_DETAIL: 10, // 10 minutes for team details
		TEAM_MEMBERS: 5, // 5 minutes for team members
		TEAM_BY_SLUG: 10, // 10 minutes for team by slug
	};

	/**
	 * Teams list cache
	 */
	getTeamsList(params?: string): any {
		const key = params
			? `${TeamCacheService.KEYS.TEAMS_LIST}:${params}`
			: TeamCacheService.KEYS.TEAMS_LIST;
		return this.cache.get(key);
	}

	setTeamsList(data: any, params?: string): void {
		const key = params
			? `${TeamCacheService.KEYS.TEAMS_LIST}:${params}`
			: TeamCacheService.KEYS.TEAMS_LIST;
		this.cache.set(key, data, TeamCacheService.TTL.TEAMS_LIST);
	}

	/**
	 * User teams cache
	 */
	getUserTeams(): any {
		return this.cache.get(TeamCacheService.KEYS.USER_TEAMS);
	}

	setUserTeams(data: any): void {
		this.cache.set(
			TeamCacheService.KEYS.USER_TEAMS,
			data,
			TeamCacheService.TTL.USER_TEAMS
		);
	}

	/**
	 * Team detail cache
	 */
	getTeamDetail(id: string): any {
		return this.cache.get(TeamCacheService.KEYS.TEAM_DETAIL(id));
	}

	setTeamDetail(id: string, data: any): void {
		this.cache.set(
			TeamCacheService.KEYS.TEAM_DETAIL(id),
			data,
			TeamCacheService.TTL.TEAM_DETAIL
		);
	}

	/**
	 * Team members cache
	 */
	getTeamMembers(id: string): any {
		return this.cache.get(TeamCacheService.KEYS.TEAM_MEMBERS(id));
	}

	setTeamMembers(id: string, data: any): void {
		this.cache.set(
			TeamCacheService.KEYS.TEAM_MEMBERS(id),
			data,
			TeamCacheService.TTL.TEAM_MEMBERS
		);
	}

	/**
	 * Team by slug cache
	 */
	getTeamBySlug(teamSlug: string): any {
		return this.cache.get(TeamCacheService.KEYS.TEAM_BY_SLUG(teamSlug));
	}

	setTeamBySlug(teamSlug: string, data: any): void {
		this.cache.set(
			TeamCacheService.KEYS.TEAM_BY_SLUG(teamSlug),
			data,
			TeamCacheService.TTL.TEAM_BY_SLUG
		);
	}

	/**
	 * Invalidate team-related cache
	 */
	invalidateTeam(id: string): void {
		this.cache.delete(TeamCacheService.KEYS.TEAM_DETAIL(id));
		this.cache.delete(TeamCacheService.KEYS.TEAM_MEMBERS(id));
		this.invalidateTeamsList();
		this.invalidateUserTeams();
	}

	/**
	 * Invalidate teams list cache
	 */
	invalidateTeamsList(): void {
		// Clear all teams list cache entries (including filtered ones)
		// Note: This is a simplified approach - in a real implementation,
		// you might want to track cache keys separately
		this.cache.clear(); // Clear all cache for simplicity
	}

	/**
	 * Invalidate user teams cache
	 */
	invalidateUserTeams(): void {
		this.cache.delete(TeamCacheService.KEYS.USER_TEAMS);
	}

	/**
	 * Clear all cache
	 */
	clear(): void {
		this.cache.clear();
	}

	/**
	 * Get cache statistics
	 */
	getStats() {
		return this.cache.getStats();
	}

	/**
	 * Get cache hit rate
	 */
	getHitRate(): number {
		return this.cache.getHitRate();
	}
}

/**
 * Project-specific cache service
 */
export class ProjectCacheService {
	private cache = new CacheService(50); // Smaller cache for project data

	private static readonly KEYS = {
		PROJECTS_LIST: "projects:list",
		USER_PROJECTS: "projects:user",
		PROJECT_DETAIL: "project:detail",
		PROJECT_MEMBERS: "project:members",
		PROJECT_BY_SLUG: "project:projectSlug",
		// Project-teams integration keys
		PROJECT_TEAMS: "project:teams",
		PROJECT_TEAM_MEMBERS: "project:team:members",
	};

	private static readonly TTL = {
		PROJECTS_LIST: 5, // 5 minutes
		USER_PROJECTS: 3, // 3 minutes
		PROJECT_DETAIL: 10, // 10 minutes
		PROJECT_MEMBERS: 5, // 5 minutes
		PROJECT_BY_SLUG: 10, // 10 minutes
		// Project-teams integration TTL
		PROJECT_TEAMS: 5, // 5 minutes
		PROJECT_TEAM_MEMBERS: 5, // 5 minutes
	};

	/**
	 * Projects list cache
	 */
	getProjectsList(params?: string): any {
		const key = params
			? `${ProjectCacheService.KEYS.PROJECTS_LIST}:${params}`
			: ProjectCacheService.KEYS.PROJECTS_LIST;
		return this.cache.get(key);
	}

	setProjectsList(data: any, params?: string): void {
		const key = params
			? `${ProjectCacheService.KEYS.PROJECTS_LIST}:${params}`
			: ProjectCacheService.KEYS.PROJECTS_LIST;
		this.cache.set(key, data, ProjectCacheService.TTL.PROJECTS_LIST);
	}

	/**
	 * User projects cache
	 */
	getUserProjects(): any {
		return this.cache.get(ProjectCacheService.KEYS.USER_PROJECTS);
	}

	setUserProjects(data: any): void {
		this.cache.set(
			ProjectCacheService.KEYS.USER_PROJECTS,
			data,
			ProjectCacheService.TTL.USER_PROJECTS
		);
	}

	/**
	 * Project detail cache
	 */
	getProjectDetail(id: string): any {
		const key = `${ProjectCacheService.KEYS.PROJECT_DETAIL}:${id}`;
		return this.cache.get(key);
	}

	setProjectDetail(id: string, data: any): void {
		const key = `${ProjectCacheService.KEYS.PROJECT_DETAIL}:${id}`;
		this.cache.set(key, data, ProjectCacheService.TTL.PROJECT_DETAIL);
	}

	/**
	 * Project members cache
	 */
	getProjectMembers(projectSlug: string): any {
		const key = `${ProjectCacheService.KEYS.PROJECT_MEMBERS}:${projectSlug}`;
		return this.cache.get(key);
	}

	setProjectMembers(projectSlug: string, data: any): void {
		const key = `${ProjectCacheService.KEYS.PROJECT_MEMBERS}:${projectSlug}`;
		this.cache.set(key, data, ProjectCacheService.TTL.PROJECT_MEMBERS);
	}

	/**
	 * Project by slug cache
	 */
	getProjectBySlug(projectSlug: string): any {
		const key = `${ProjectCacheService.KEYS.PROJECT_BY_SLUG}:${projectSlug}`;
		return this.cache.get(key);
	}

	setProjectBySlug(projectSlug: string, data: any): void {
		const key = `${ProjectCacheService.KEYS.PROJECT_BY_SLUG}:${projectSlug}`;
		this.cache.set(key, data, ProjectCacheService.TTL.PROJECT_BY_SLUG);
	}

	/**
	 * Cache invalidation methods
	 */
	invalidateProject(id: string): void {
		const key = `${ProjectCacheService.KEYS.PROJECT_DETAIL}:${id}`;
		this.cache.delete(key);
		// Also invalidate related caches
		this.invalidateProjectsList();
		this.invalidateUserProjects();
	}

	invalidateProjectBySlug(projectSlug: string): void {
		const key = `${ProjectCacheService.KEYS.PROJECT_BY_SLUG}:${projectSlug}`;
		this.cache.delete(key);
	}

	invalidateProjectMembers(projectSlug: string): void {
		const key = `${ProjectCacheService.KEYS.PROJECT_MEMBERS}:${projectSlug}`;
		this.cache.delete(key);
	}

	invalidateProjectsList(): void {
		// Clear all project list variations
		const allKeys = Array.from(this.cache["cache"].keys());
		allKeys.forEach((key) => {
			if (key.startsWith(ProjectCacheService.KEYS.PROJECTS_LIST)) {
				this.cache.delete(key);
			}
		});
	}

	invalidateUserProjects(): void {
		this.cache.delete(ProjectCacheService.KEYS.USER_PROJECTS);
	}

	// ============================================================================
	// PROJECT-TEAMS INTEGRATION CACHE METHODS
	// ============================================================================

	/**
	 * Get project teams from cache
	 */
	getProjectTeams(projectSlug: string, params?: string): any {
		const key = `${ProjectCacheService.KEYS.PROJECT_TEAMS}:${projectSlug}${
			params ? `:${params}` : ""
		}`;
		return this.cache.get(key);
	}

	/**
	 * Set project teams in cache
	 */
	setProjectTeams(projectSlug: string, data: any, params?: string): void {
		const key = `${ProjectCacheService.KEYS.PROJECT_TEAMS}:${projectSlug}${
			params ? `:${params}` : ""
		}`;
		this.cache.set(key, data, ProjectCacheService.TTL.PROJECT_TEAMS);
	}

	/**
	 * Get project team members from cache
	 */
	getProjectTeamMembers(
		projectSlug: string,
		teamSlug: string,
		params?: string
	): any {
		const key = `${
			ProjectCacheService.KEYS.PROJECT_TEAM_MEMBERS
		}:${projectSlug}:${teamSlug}${params ? `:${params}` : ""}`;
		return this.cache.get(key);
	}

	/**
	 * Set project team members in cache
	 */
	setProjectTeamMembers(
		projectSlug: string,
		teamSlug: string,
		data: any,
		params?: string
	): void {
		const key = `${
			ProjectCacheService.KEYS.PROJECT_TEAM_MEMBERS
		}:${projectSlug}:${teamSlug}${params ? `:${params}` : ""}`;
		this.cache.set(key, data, ProjectCacheService.TTL.PROJECT_TEAM_MEMBERS);
	}

	/**
	 * Invalidate project teams cache
	 */
	invalidateProjectTeams(projectSlug: string): void {
		// Delete all project teams keys for this project
		const keys = Array.from(this.cache["cache"].keys());
		const projectTeamsKeys = keys.filter((key) =>
			key.startsWith(`${ProjectCacheService.KEYS.PROJECT_TEAMS}:${projectSlug}`)
		);
		projectTeamsKeys.forEach((key) => this.cache.delete(key));
	}

	/**
	 * Invalidate project team members cache
	 */
	invalidateProjectTeamMembers(projectSlug: string, teamSlug: string): void {
		// Delete all project team members keys for this team
		const keys = Array.from(this.cache["cache"].keys());
		const teamMembersKeys = keys.filter((key) =>
			key.startsWith(
				`${ProjectCacheService.KEYS.PROJECT_TEAM_MEMBERS}:${projectSlug}:${teamSlug}`
			)
		);
		teamMembersKeys.forEach((key) => this.cache.delete(key));
	}

	/**
	 * Clear all project-related cache
	 */
	clear(): void {
		this.cache.clear();
	}

	/**
	 * Get cache statistics
	 */
	getStats() {
		return this.cache.getStats();
	}

	/**
	 * Get cache hit rate
	 */
	getHitRate(): number {
		return this.cache.getHitRate();
	}
}

// Export singleton instances
export const projectCacheService = new ProjectCacheService();

// Export singleton instance
export const teamCacheService = new TeamCacheService();
export default teamCacheService;
