-- Migration: V31__Create_notification_templates_table.sql
-- Purpose: Create notification templates table for managing notification content
-- Date: 2025-01-25

-- Create notification_templates table
CREATE TABLE notification_templates (
    template_id BIGSERIAL PRIMARY KEY,
    template_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Template content
    subject_template TEXT,
    html_template TEXT,
    text_template TEXT,
    in_app_template TEXT,
    toast_template TEXT,

    -- Template variables (JSON schema)
    variables JSONB,

    -- Status and versioning
    is_active BOOLEAN DEFAULT TRUE,
    version INTEGER DEFAULT 1,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for notification_templates
CREATE INDEX idx_notification_templates_key ON notification_templates(template_key);
CREATE INDEX idx_notification_templates_active ON notification_templates(is_active);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_notification_templates_updated_at
    BEFORE UPDATE ON notification_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
