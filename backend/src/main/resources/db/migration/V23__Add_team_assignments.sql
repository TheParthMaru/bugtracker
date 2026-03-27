-- Migration: V23__Add_team_assignments.sql
-- Purpose: Add team assignment tracking for bugs
-- Date: 2025-01-25

-- Create bug_team_assignments table to track which teams are assigned to which bugs
CREATE TABLE bug_team_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bug_id BIGINT NOT NULL REFERENCES bugs(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    assigned_by UUID NOT NULL REFERENCES users(id),
    is_primary BOOLEAN DEFAULT FALSE,
    
    -- Ensure unique bug-team combinations
    CONSTRAINT uk_bug_team_assignment UNIQUE (bug_id, team_id),
    
    -- Ensure only one primary team per bug
    CONSTRAINT uk_bug_primary_team UNIQUE (bug_id, is_primary)
);

-- Create indexes for performance
CREATE INDEX idx_bug_team_assignments_bug_id ON bug_team_assignments(bug_id);
CREATE INDEX idx_bug_team_assignments_team_id ON bug_team_assignments(team_id);
CREATE INDEX idx_bug_team_assignments_assigned_by ON bug_team_assignments(assigned_by);
CREATE INDEX idx_bug_team_assignments_assigned_at ON bug_team_assignments(assigned_at);
CREATE INDEX idx_bug_team_assignments_primary ON bug_team_assignments(bug_id, is_primary);

-- Add comments for documentation
COMMENT ON TABLE bug_team_assignments IS 'Tracks which teams are assigned to which bugs for auto-assignment feature';
COMMENT ON COLUMN bug_team_assignments.bug_id IS 'Reference to the bug';
COMMENT ON COLUMN bug_team_assignments.team_id IS 'Reference to the assigned team';
COMMENT ON COLUMN bug_team_assignments.assigned_at IS 'When the team was assigned to the bug';
COMMENT ON COLUMN bug_team_assignments.assigned_by IS 'User who assigned the team (or system for auto-assignment)';
COMMENT ON COLUMN bug_team_assignments.is_primary IS 'Whether this is the primary team for the bug (only one per bug)';

-- Create a function to get the primary team for a bug
CREATE OR REPLACE FUNCTION get_bug_primary_team(bug_uuid BIGINT)
RETURNS UUID AS $$
BEGIN
    RETURN (
        SELECT team_id 
        FROM bug_team_assignments 
        WHERE bug_id = bug_uuid AND is_primary = TRUE
        LIMIT 1
    );
END;
$$ LANGUAGE plpgsql;

-- Create a function to get all teams assigned to a bug
CREATE OR REPLACE FUNCTION get_bug_assigned_teams(bug_uuid BIGINT)
RETURNS TABLE(team_id UUID, is_primary BOOLEAN, assigned_at TIMESTAMP WITH TIME ZONE) AS $$
BEGIN
    RETURN QUERY
    SELECT bta.team_id, bta.is_primary, bta.assigned_at
    FROM bug_team_assignments bta
    WHERE bta.bug_id = bug_uuid
    ORDER BY bta.is_primary DESC, bta.assigned_at ASC;
END;
$$ LANGUAGE plpgsql;

-- Create a function to count teams assigned to a bug
CREATE OR REPLACE FUNCTION count_bug_assigned_teams(bug_uuid BIGINT)
RETURNS INTEGER AS $$
BEGIN
    RETURN (
        SELECT COUNT(*)::INTEGER
        FROM bug_team_assignments
        WHERE bug_id = bug_uuid
    );
END;
$$ LANGUAGE plpgsql; 