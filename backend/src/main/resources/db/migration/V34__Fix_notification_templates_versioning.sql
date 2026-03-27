-- Migration: V34__Fix_notification_templates_versioning.sql
-- Purpose: Fix notification templates versioning by allowing multiple versions of the same template
-- Date: 2025-08-30

-- Drop the unique constraint on template_key to allow multiple versions
ALTER TABLE notification_templates DROP CONSTRAINT IF EXISTS notification_templates_template_key_key;

-- Add composite unique constraint on (template_key, version) to allow multiple versions
ALTER TABLE notification_templates ADD CONSTRAINT uk_notification_templates_key_version UNIQUE (template_key, version);

-- Add index for better performance on template key lookups
CREATE INDEX IF NOT EXISTS idx_notification_templates_key_version ON notification_templates(template_key, version);

-- Log the schema change
COMMENT ON TABLE notification_templates IS 'Notification templates with versioning support - multiple versions allowed per template_key';
COMMENT ON CONSTRAINT uk_notification_templates_key_version ON notification_templates IS 'Composite unique constraint allowing multiple versions of the same template';
