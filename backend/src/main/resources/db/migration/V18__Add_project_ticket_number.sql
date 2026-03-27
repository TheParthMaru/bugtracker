-- Add project-specific ticket number to bugs table
ALTER TABLE bugs ADD COLUMN project_ticket_number INTEGER;

-- Create a unique constraint to ensure project-specific ticket numbers are unique within each project
CREATE UNIQUE INDEX idx_bugs_project_ticket_number ON bugs(project_id, project_ticket_number);

-- Create a sequence for each project to generate ticket numbers
CREATE OR REPLACE FUNCTION get_next_project_ticket_number(project_uuid UUID)
RETURNS INTEGER AS $$
DECLARE
    next_number INTEGER;
BEGIN
    -- Get the next ticket number for the project
    SELECT COALESCE(MAX(project_ticket_number), 0) + 1
    INTO next_number
    FROM bugs
    WHERE project_id = project_uuid;
    
    RETURN next_number;
END;
$$ LANGUAGE plpgsql;

-- Update existing bugs to have project-specific ticket numbers
-- This will assign sequential numbers within each project using a window function
UPDATE bugs 
SET project_ticket_number = subquery.row_num
FROM (
    SELECT 
        id,
        ROW_NUMBER() OVER (
            PARTITION BY project_id 
            ORDER BY created_at, id
        ) as row_num
    FROM bugs
) AS subquery
WHERE bugs.id = subquery.id;

-- Make the column NOT NULL after populating existing data
ALTER TABLE bugs ALTER COLUMN project_ticket_number SET NOT NULL; 