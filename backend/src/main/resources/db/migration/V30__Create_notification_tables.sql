-- Migration: V30__Create_notification_tables.sql
-- Purpose: Create core notification tables for the notification system
-- Date: 2025-01-25

-- Ensure UUID extension is available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create user_notifications table
CREATE TABLE user_notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,

    -- Status and timestamps
    is_read BOOLEAN DEFAULT FALSE,
    is_dismissed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,

    -- Related entities
    related_bug_id BIGINT REFERENCES bugs(id) ON DELETE SET NULL,
    related_project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    related_team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    related_user_id UUID REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for user_notifications
CREATE INDEX idx_user_notifications_user_id ON user_notifications(user_id);
CREATE INDEX idx_user_notifications_event_type ON user_notifications(event_type);
CREATE INDEX idx_user_notifications_created ON user_notifications(created_at);
CREATE INDEX idx_user_notifications_unread ON user_notifications(user_id, is_read) WHERE is_read = FALSE;

-- Create notification_preferences table
CREATE TABLE notification_preferences (
    preference_id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Channel preferences (HOW to receive notifications)
    in_app_enabled BOOLEAN DEFAULT TRUE,
    email_enabled BOOLEAN DEFAULT TRUE,
    toast_enabled BOOLEAN DEFAULT TRUE,

    -- Event type preferences (WHAT events to be notified about)
    bug_assigned BOOLEAN DEFAULT TRUE,
    bug_status_changed BOOLEAN DEFAULT TRUE,
    bug_priority_changed BOOLEAN DEFAULT FALSE,
    bug_commented BOOLEAN DEFAULT TRUE,
    bug_mentioned BOOLEAN DEFAULT TRUE,
    bug_attachment_added BOOLEAN DEFAULT FALSE,

    project_invited BOOLEAN DEFAULT TRUE,
    project_role_changed BOOLEAN DEFAULT TRUE,
    project_member_joined BOOLEAN DEFAULT FALSE,

    team_invited BOOLEAN DEFAULT TRUE,
    team_role_changed BOOLEAN DEFAULT TRUE,
    team_member_joined BOOLEAN DEFAULT FALSE,

    gamification_points BOOLEAN DEFAULT TRUE,
    gamification_achievements BOOLEAN DEFAULT TRUE,
    gamification_leaderboard BOOLEAN DEFAULT FALSE,

    -- Email specific settings
    email_frequency VARCHAR(20) DEFAULT 'IMMEDIATE' CHECK (email_frequency IN ('IMMEDIATE', 'DAILY', 'WEEKLY')),
    timezone VARCHAR(50) DEFAULT 'UTC',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_notification_preferences_user UNIQUE(user_id)
);

-- Create indexes for notification_preferences
CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);

-- Create trigger to automatically update updated_at timestamp for notification_preferences
CREATE TRIGGER update_notification_preferences_updated_at
    BEFORE UPDATE ON notification_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
