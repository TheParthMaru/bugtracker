-- Migration V24: Rename slug columns to project_slug and team_slug for better clarity
-- This migration renames the generic 'slug' columns to more specific names
-- to avoid confusion between project slugs and team slugs

-- Rename projects.slug to projects.project_slug
ALTER TABLE projects RENAME COLUMN slug TO project_slug;

-- Rename teams.slug to teams.team_slug  
ALTER TABLE teams RENAME COLUMN slug TO team_slug;

-- Update the unique constraint names to match new column names
-- Drop existing constraints first
ALTER TABLE projects DROP CONSTRAINT IF EXISTS projects_slug_not_empty;
ALTER TABLE projects DROP CONSTRAINT IF EXISTS projects_slug_format;
ALTER TABLE teams DROP CONSTRAINT IF EXISTS teams_slug_check;

-- Add new constraints with updated names
ALTER TABLE projects ADD CONSTRAINT projects_project_slug_not_empty CHECK (LENGTH(TRIM(project_slug)) > 0);
ALTER TABLE projects ADD CONSTRAINT projects_project_slug_format CHECK (project_slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$');
ALTER TABLE teams ADD CONSTRAINT teams_team_slug_check CHECK (team_slug ~ '^[a-z0-9-]+$');

-- Update indexes to use new column names
DROP INDEX IF EXISTS idx_teams_slug;
CREATE INDEX idx_teams_team_slug ON teams(team_slug);

-- Note: projects table already has a unique constraint on project_slug from the original table creation
-- The unique constraint is maintained through the column rename
