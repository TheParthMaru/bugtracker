-- Migration: Create basic gamification tables
-- This migration creates the core tables for the gamification module

-- User Points Table - Stores user gamification statistics
CREATE TABLE user_points (
    user_points_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_points INTEGER NOT NULL DEFAULT 0,
    current_streak INTEGER NOT NULL DEFAULT 0,
    max_streak INTEGER NOT NULL DEFAULT 0,
    bugs_resolved INTEGER NOT NULL DEFAULT 0,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Point Transactions Table - Audit trail of all point transactions
CREATE TABLE point_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    points INTEGER NOT NULL,
    reason VARCHAR(100) NOT NULL,
    bug_id BIGINT REFERENCES bugs(id) ON DELETE SET NULL,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Project Leaderboards Table - Project-specific leaderboard data
CREATE TABLE project_leaderboards (
    leaderboard_entry_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    weekly_points INTEGER DEFAULT 0,
    monthly_points INTEGER DEFAULT 0,
    all_time_points INTEGER DEFAULT 0,
    bugs_resolved INTEGER DEFAULT 0,
    current_streak INTEGER DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, user_id)
);

-- User Streaks Table - User login streak tracking
CREATE TABLE user_streaks (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_streak INTEGER DEFAULT 0,
    max_streak INTEGER DEFAULT 0,
    last_login_date DATE
);

-- Basic indexes for performance
CREATE INDEX idx_user_points_user_id ON user_points(user_id);
CREATE INDEX idx_point_transactions_user_id ON point_transactions(user_id);
CREATE INDEX idx_point_transactions_project_id ON point_transactions(project_id);
CREATE INDEX idx_point_transactions_earned_at ON point_transactions(earned_at);
CREATE INDEX idx_project_leaderboards_project_id ON project_leaderboards(project_id);
CREATE INDEX idx_project_leaderboards_user_id ON project_leaderboards(user_id);