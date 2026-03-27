-- Migration: V8__Migrate_teams_to_project_submodule.sql
-- Purpose: Integrate teams as sub-modules within projects
-- Date: 2025-01-25

-- Step 1: Add project_id column (nullable initially for migration)
ALTER TABLE teams ADD COLUMN project_id UUID;

-- Step 2: Create a default project for existing teams (if any exist)
-- This ensures existing teams have a valid project association
-- INSERT INTO projects (id, name, description, slug, admin_id, created_at, updated_at, is_active)
-- SELECT 
--     gen_random_uuid(),
--     'Legacy Teams Project',
--     'Default project for existing teams during migration',
--     'legacy-teams-project',
--     u.id,
--     NOW(),
--     NOW(),
--     true
-- FROM users u
-- WHERE NOT EXISTS (
--     SELECT 1 FROM projects WHERE slug = 'legacy-teams-project'
-- )
-- LIMIT 1;

-- Step 3: Update existing teams to use default project
-- UPDATE teams 
-- SET project_id = (SELECT id FROM projects WHERE slug = 'legacy-teams-project')
-- WHERE project_id IS NULL;

-- Step 4: Make project_id non-nullable
ALTER TABLE teams ALTER COLUMN project_id SET NOT NULL;

-- Step 5: Add foreign key constraint
ALTER TABLE teams ADD CONSTRAINT fk_teams_project 
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

-- Step 6: Add unique constraints for project-scoped team names and slugs
ALTER TABLE teams ADD CONSTRAINT uk_teams_project_name 
    UNIQUE (project_id, name);
ALTER TABLE teams ADD CONSTRAINT uk_teams_project_slug 
    UNIQUE (project_id, slug);

-- Step 7: Add indexes for performance
CREATE INDEX idx_teams_project_id ON teams(project_id);
CREATE INDEX idx_teams_project_name ON teams(project_id, name);

-- Step 8: Update team slugs to include project prefix
-- This ensures team slugs follow the new format: <project-slug>-<team-slug>
UPDATE teams t
SET slug = p.slug || '-' || t.slug
FROM projects p
WHERE t.project_id = p.id 
  AND t.slug NOT LIKE p.slug || '-%';

-- Step 9: Increase slug column length to accommodate project prefix
-- Team slugs can now be up to 200 characters (project-slug + team-slug)
ALTER TABLE teams ALTER COLUMN slug TYPE VARCHAR(200); 