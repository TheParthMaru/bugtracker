-- Migration: Make project_id nullable in point_transactions for system activities
-- This allows system-level gamification activities (welcome bonus, daily login) 
-- without requiring a project association

-- Drop the existing foreign key constraint
ALTER TABLE point_transactions DROP CONSTRAINT IF EXISTS point_transactions_project_id_fkey;

-- Make project_id nullable
ALTER TABLE point_transactions ALTER COLUMN project_id DROP NOT NULL;

-- Re-add the foreign key constraint but allow NULL values
ALTER TABLE point_transactions ADD CONSTRAINT point_transactions_project_id_fkey 
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

-- Add a comment to clarify the purpose
COMMENT ON COLUMN point_transactions.project_id IS 'Project ID for project-specific activities, NULL for system activities (welcome bonus, daily login)';
