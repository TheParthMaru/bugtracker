-- Add custom tags column to bugs table
-- This allows for granular tagging alongside the existing label system

-- Add tags column as PostgreSQL array of strings
ALTER TABLE bugs ADD COLUMN tags TEXT[] DEFAULT '{}';

-- Create GIN index for efficient tag searching and filtering
CREATE INDEX idx_bugs_tags ON bugs USING GIN(tags);

-- Add constraint to ensure tags are not null (empty array is fine)
ALTER TABLE bugs ALTER COLUMN tags SET NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN bugs.tags IS 'Custom tags for granular bug categorization (e.g., React, CSS, API, Testing)'; 