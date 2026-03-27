-- Performance indexes for projects module
-- These indexes are designed to optimize common query patterns
-- and improve application response times

-- ============== PROJECTS TABLE INDEXES ==============

-- Index for slug-based lookups (most common project access pattern)
-- Used for URLs like /projects/{slug}
CREATE INDEX idx_projects_slug ON projects(slug);

-- Index for finding projects by admin (admin dashboard queries)
-- Used when admins want to see "My Projects"
CREATE INDEX idx_projects_admin_id ON projects(admin_id);

-- Index for project name searches (partial matching)
-- Used for project search functionality
CREATE INDEX idx_projects_name ON projects(name);

-- Index for active project filtering
-- Used to quickly filter out soft-deleted projects
CREATE INDEX idx_projects_is_active ON projects(is_active);

-- Index for chronological sorting of projects
-- Used for "Recently Created" project listings
CREATE INDEX idx_projects_created_at ON projects(created_at);

-- Composite index for active projects ordered by creation date
-- Optimizes the common query: "Get all active projects, newest first"
CREATE INDEX idx_projects_active_created ON projects(is_active, created_at DESC);

-- ============== PROJECT_MEMBERS TABLE INDEXES ==============

-- Index for finding members of a specific project
-- Used for project member listings and permission checks
CREATE INDEX idx_project_members_project_id ON project_members(project_id);

-- Index for finding all projects a user belongs to
-- Used for "My Projects" user dashboard queries
CREATE INDEX idx_project_members_user_id ON project_members(user_id);

-- Index for filtering by membership status
-- Used for finding pending approval requests
CREATE INDEX idx_project_members_status ON project_members(status);

-- Index for role-based queries
-- Used for finding project admins quickly
CREATE INDEX idx_project_members_role ON project_members(role);

-- Composite index for project-user lookups (most frequent permission check)
-- Optimizes queries like "Is user X a member of project Y?"
CREATE INDEX idx_project_members_project_user ON project_members(project_id, user_id);

-- Composite index for user's active memberships
-- Optimizes "Get all active projects for user X"
CREATE INDEX idx_project_members_user_status ON project_members(user_id, status);

-- Composite index for project admins lookup
-- Optimizes "Find all admins of project X"
CREATE INDEX idx_project_members_project_role ON project_members(project_id, role);

-- Composite index for pending requests per project
-- Optimizes admin dashboard showing pending approval requests
CREATE INDEX idx_project_members_project_status ON project_members(project_id, status); 