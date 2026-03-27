-- Remove ASSIGNED status from bug status constraint
-- First, drop the existing constraint
ALTER TABLE bugs DROP CONSTRAINT IF EXISTS bugs_status_check;

-- Add the new constraint without ASSIGNED
ALTER TABLE bugs ADD CONSTRAINT bugs_status_check 
    CHECK (status IN ('OPEN', 'FIXED', 'CLOSED', 'REOPENED'));

-- Update any existing bugs with ASSIGNED status to OPEN
UPDATE bugs SET status = 'OPEN' WHERE status = 'ASSIGNED'; 