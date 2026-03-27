-- Create projects table for bug tracker application
-- This table stores all project information similar to JIRA projects
-- Each project can have multiple teams and members
CREATE TABLE projects (
    -- Primary key using UUID for distributed system compatibility
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Project name must be unique across the system
    -- VARCHAR(100) provides sufficient length for project names
    name VARCHAR(100) NOT NULL UNIQUE,
    
    -- Optional project description for additional context
    description TEXT,
    
    -- URL-friendly slug generated from project name
    -- Used for clean URLs like /projects/my-awesome-project
    slug VARCHAR(120) NOT NULL UNIQUE,
    
    -- Reference to the user who created/owns the project
    -- This user automatically becomes the project admin
    admin_id UUID NOT NULL REFERENCES users(id),
    
    -- Audit timestamps for tracking creation and modifications
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Soft delete flag - allows "deleting" projects without losing referential integrity
    -- When false, project is considered deleted but data remains for audit purposes
    is_active BOOLEAN DEFAULT true,
    
    -- Data validation constraints
    -- Ensure project name is not empty or just whitespace
    CONSTRAINT projects_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    
    -- Ensure slug is not empty
    CONSTRAINT projects_slug_not_empty CHECK (LENGTH(TRIM(slug)) > 0),
    
    -- Enforce slug format: lowercase alphanumeric with hyphens
    -- Example: "my-awesome-project"
    CONSTRAINT projects_slug_format CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$')
); 