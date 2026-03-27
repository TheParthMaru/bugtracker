-- Migration: V32__Create_notification_delivery_log_table.sql
-- Purpose: Create notification delivery tracking table for monitoring email/notification delivery
-- Date: 2025-01-25

-- Create notification_delivery_log table
CREATE TABLE notification_delivery_log (
    delivery_id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT REFERENCES user_notifications(notification_id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'IN_APP', 'TOAST')),

    -- Delivery status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED')),
    recipient_email VARCHAR(255),

    -- Delivery details
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    error_message TEXT,
    external_id VARCHAR(255), -- Resend message ID

    -- Retry information
    retry_count INTEGER DEFAULT 0 CHECK (retry_count >= 0),
    next_retry_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for notification_delivery_log
CREATE INDEX idx_notification_delivery_log_notification ON notification_delivery_log(notification_id);
CREATE INDEX idx_notification_delivery_log_status ON notification_delivery_log(status);
CREATE INDEX idx_notification_delivery_log_retry ON notification_delivery_log(next_retry_at) WHERE status = 'FAILED';
CREATE INDEX idx_notification_delivery_log_channel ON notification_delivery_log(channel);
CREATE INDEX idx_notification_delivery_log_created ON notification_delivery_log(created_at);
