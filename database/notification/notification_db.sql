-- Sprylo - Notification Service
-- Database: notification_db
-- PostgreSQL 13+
-- Run this file while connected to notification_db.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(40) NOT NULL
        CHECK (type IN (
            'ORDER_CONFIRMATION', 'PAYMENT_SUCCESS',
            'SHIPPING_UPDATE', 'PROMOTION'
        )),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at
    ON notifications(created_at DESC);

CREATE TABLE IF NOT EXISTS notification_recipients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED')),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_recipient UNIQUE (notification_id, user_id),
    CONSTRAINT ck_notification_read_time
        CHECK ((is_read = FALSE AND read_at IS NULL) OR (is_read = TRUE AND read_at IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_notification_recipients_user_id
    ON notification_recipients(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_recipients_unread
    ON notification_recipients(user_id, created_at DESC)
    WHERE is_read = FALSE;
CREATE INDEX IF NOT EXISTS idx_notification_recipients_delivery_status
    ON notification_recipients(delivery_status);
