-- Remove private teams feature
-- All teams will be public by default

ALTER TABLE teams DROP COLUMN is_public; 