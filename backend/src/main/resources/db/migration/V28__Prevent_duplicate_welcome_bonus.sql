-- V28__Prevent_duplicate_welcome_bonus.sql
-- This migration adds constraints to prevent duplicate welcome bonus transactions

-- Step 1: Create a partial unique index to prevent multiple welcome bonus transactions per user
-- This ensures each user can only have one welcome bonus transaction
CREATE UNIQUE INDEX unique_welcome_bonus_per_user 
ON point_transactions (user_id) 
WHERE reason = 'First leaderboard access - Welcome bonus';

-- Step 2: Add a check constraint to ensure user_points.total_points is always >= 0
ALTER TABLE user_points 
ADD CONSTRAINT check_total_points_non_negative 
CHECK (total_points >= 0);

-- Step 3: Add a check constraint to ensure user_points.current_streak is always >= 1
ALTER TABLE user_points 
ADD CONSTRAINT check_current_streak_minimum 
CHECK (current_streak >= 1);

-- Step 4: Add a check constraint to ensure user_points.max_streak is always >= current_streak
ALTER TABLE user_points 
ADD CONSTRAINT check_max_streak_consistency 
CHECK (max_streak >= current_streak);
