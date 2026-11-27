-- Sprylo - Shipping Service
-- Database: shipping_db
-- PostgreSQL 13+
-- Run this file while connected to shipping_db.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracking_code VARCHAR(64) NOT NULL UNIQUE,
    order_id UUID NOT NULL,
    carrier VARCHAR(100) NOT NULL,
    receiver_name VARCHAR(150) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    shipping_address TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING_PICKUP'
        CHECK (status IN (
            'WAITING_PICKUP', 'PICKED_UP', 'IN_TRANSIT',
            'OUT_FOR_DELIVERY', 'DELIVERED', 'DELIVERY_FAILED', 'RETURNED'
        )),
    shipped_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipments_order_id ON shipments(order_id);
CREATE INDEX IF NOT EXISTS idx_shipments_status ON shipments(status);
CREATE INDEX IF NOT EXISTS idx_shipments_created_at ON shipments(created_at DESC);

CREATE TABLE IF NOT EXISTS shipment_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL
        CHECK (status IN (
            'WAITING_PICKUP', 'PICKED_UP', 'IN_TRANSIT',
            'OUT_FOR_DELIVERY', 'DELIVERED', 'DELIVERY_FAILED', 'RETURNED'
        )),
    location VARCHAR(255),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipment_events_shipment_id
    ON shipment_events(shipment_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_shipment_events_status
    ON shipment_events(status);
