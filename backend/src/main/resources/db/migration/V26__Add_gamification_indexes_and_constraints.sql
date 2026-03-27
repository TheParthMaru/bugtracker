-- Migration: Add essential gamification constraints
-- This migration adds basic data integrity constraints

-- Add basic constraints for data validation
ALTER TABLE user_points 
ADD CONSTRAINT user_points_total_points_check CHECK (total_points >= 0),
ADD CONSTRAINT user_points_current_streak_check CHECK (current_streak >= 0),
ADD CONSTRAINT user_points_max_streak_check CHECK (max_streak >= 0),
ADD CONSTRAINT user_points_bugs_resolved_check CHECK (bugs_resolved >= 0);

ALTER TABLE point_transactions 
ADD CONSTRAINT point_transactions_points_check CHECK (points != 0),
ADD CONSTRAINT point_transactions_reason_length_check CHECK (LENGTH(TRIM(reason)) >= 3);

ALTER TABLE project_leaderboards 
ADD CONSTRAINT project_leaderboards_weekly_points_check CHECK (weekly_points >= 0),
ADD CONSTRAINT project_leaderboards_monthly_points_check CHECK (monthly_points >= 0),
ADD CONSTRAINT project_leaderboards_all_time_points_check CHECK (all_time_points >= 0),
ADD CONSTRAINT project_leaderboards_bugs_resolved_check CHECK (bugs_resolved >= 0),
ADD CONSTRAINT project_leaderboards_current_streak_check CHECK (current_streak >= 0);

ALTER TABLE user_streaks 
ADD CONSTRAINT user_streaks_current_streak_check CHECK (current_streak >= 0),
ADD CONSTRAINT user_streaks_max_streak_check CHECK (max_streak >= 0);

-- Add a few essential composite indexes for common queries
CREATE INDEX idx_point_transactions_user_project ON point_transactions(user_id, project_id);
CREATE INDEX idx_project_leaderboards_project_points ON project_leaderboards(project_id, all_time_points DESC);
CREATE INDEX idx_user_points_streak_rank ON user_points(current_streak DESC, total_points DESC);


