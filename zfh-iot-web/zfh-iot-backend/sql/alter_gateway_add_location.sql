-- Migration: Add location column to iot_gateway table
-- Date: 2026-04-12

ALTER TABLE iot_gateway
ADD COLUMN location VARCHAR(255) COMMENT 'Installation location' AFTER heartbeat_interval;
