-- Migration: V33__Add_notification_indexes.sql
-- Purpose: Add additional performance indexes for notification queries
-- Date: 2025-01-25

-- Additional composite indexes for user_notifications
CREATE INDEX idx_user_notifications_user_event ON user_notifications(user_id, event_type);
CREATE INDEX idx_user_notifications_user_created ON user_notifications(user_id, created_at DESC);
CREATE INDEX idx_user_notifications_related_bug ON user_notifications(related_bug_id) WHERE related_bug_id IS NOT NULL;
CREATE INDEX idx_user_notifications_related_project ON user_notifications(related_project_id) WHERE related_project_id IS NOT NULL;
CREATE INDEX idx_user_notifications_related_team ON user_notifications(related_team_id) WHERE related_team_id IS NOT NULL;

-- Additional indexes for notification_preferences
CREATE INDEX idx_notification_preferences_email_enabled ON notification_preferences(email_enabled) WHERE email_enabled = TRUE;
CREATE INDEX idx_notification_preferences_in_app_enabled ON notification_preferences(in_app_enabled) WHERE in_app_enabled = TRUE;

-- Additional indexes for notification_delivery_log
CREATE INDEX idx_notification_delivery_log_status_retry ON notification_delivery_log(status, next_retry_at) WHERE status = 'FAILED';
CREATE INDEX idx_notification_delivery_log_external_id ON notification_delivery_log(external_id) WHERE external_id IS NOT NULL;
