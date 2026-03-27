-- Remove close_reason column from bugs table
-- Since we're not using close reasons anymore (users can add reasons in comments)

ALTER TABLE bugs DROP COLUMN IF EXISTS close_reason; 