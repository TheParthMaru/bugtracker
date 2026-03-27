-- Migration: Restructure point_transactions table
-- This migration adds new columns points_credited and points_deducted,
-- drops the old points column, and adds missing constraints

-- 1. Add new columns for points tracking
ALTER TABLE point_transactions 
ADD COLUMN points_credited INTEGER DEFAULT 0,
ADD COLUMN points_deducted INTEGER DEFAULT 0;

-- 2. Drop old points column (no longer needed)
ALTER TABLE point_transactions DROP COLUMN points;

-- 3. Drop the old welcome bonus constraint (it won't work with new reason format)
DROP INDEX IF EXISTS unique_welcome_bonus_per_user;

-- 4. Drop old constraints that are no longer valid
ALTER TABLE point_transactions DROP CONSTRAINT IF EXISTS point_transactions_points_check;
ALTER TABLE point_transactions DROP CONSTRAINT IF EXISTS point_transactions_reason_length_check;

-- 5. Add new constraints for the new structure
ALTER TABLE point_transactions
ADD CONSTRAINT point_transactions_points_credited_check CHECK (points_credited >= 0),
ADD CONSTRAINT point_transactions_points_deducted_check CHECK (points_deducted >= 0);

-- 6. Add new unique indexes for the new reason format
CREATE UNIQUE INDEX unique_welcome_bonus_per_user 
ON point_transactions (user_id) 
WHERE reason = 'welcome-bonus';


-- 7. Add unique index to prevent duplicate daily login transactions per user per day
CREATE UNIQUE INDEX unique_daily_login_per_user_per_day 
ON point_transactions (user_id, DATE(earned_at)) 
WHERE reason = 'daily-login';