-- Migration: Add comm_addr column to iot_gateway table
-- Date: 2026-04-13

ALTER TABLE iot_gateway
ADD COLUMN comm_addr VARCHAR(64) COMMENT 'Communication address' AFTER protocol_code,
ADD UNIQUE KEY uk_comm_addr (comm_addr);
