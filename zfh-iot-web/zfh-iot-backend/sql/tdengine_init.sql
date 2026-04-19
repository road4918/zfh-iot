-- ============================================
-- TDengine 数据库初始化脚本 (TDengine 3.x)
-- 用于存储 IoT 设备时序数据（抄表数据）
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS zfh_iot_tsdb 
    BUFFER 256
    KEEP 365d
    DURATION 10d
    PRECISION 'ms';

USE zfh_iot_tsdb;

-- ============================================
-- 超级表: 表计读数数据
-- ============================================

CREATE STABLE IF NOT EXISTS meter_reading (
    ts                  TIMESTAMP,
    total_energy        DOUBLE,
    total_energy_unit   NCHAR(10),
    voltage_a           DOUBLE,
    voltage_b           DOUBLE,
    voltage_c           DOUBLE,
    current_a           DOUBLE,
    current_b           DOUBLE,
    current_c           DOUBLE,
    power_active        DOUBLE,
    power_reactive      DOUBLE,
    power_factor        DOUBLE,
    frequency           DOUBLE,
    temperature         DOUBLE,
    pressure            DOUBLE,
    flow_rate           DOUBLE,
    signal_quality      INT,
    battery_level       INT,
    raw_data            NCHAR(500)
) TAGS (
    tenant_id           BIGINT,
    gateway_id          BIGINT,
    meter_id            BIGINT,
    meter_type          INT
);

-- ============================================
-- 创建子表示例
-- ============================================

CREATE TABLE IF NOT EXISTS t_1001 USING meter_reading 
    TAGS (1, 1, 1001, 1);

CREATE TABLE IF NOT EXISTS t_1002 USING meter_reading 
    TAGS (1, 1, 1002, 1);

CREATE TABLE IF NOT EXISTS t_2001 USING meter_reading 
    TAGS (1, 1, 2001, 2);

-- ============================================
-- 常用查询示例
-- ============================================

-- 1. 查询单个表计最近 24 小时的数据
-- SELECT * FROM t_1001 WHERE ts > NOW - 1d;

-- 2. 查询所有电表 (meter_type=1) 的当前数据
-- SELECT * FROM meter_reading WHERE meter_type = 1 AND ts > NOW - 1h;

-- 3. 查询某个租户下所有表计的数据
-- SELECT * FROM meter_reading WHERE tenant_id = 1 AND ts > NOW - 1d;

-- 4. 查询某个网关下所有表计的平均电压
-- SELECT AVG(voltage_a), AVG(voltage_b), AVG(voltage_c) FROM meter_reading WHERE gateway_id = 1 AND ts > NOW - 1h;

-- 5. 按时间窗口聚合查询 (1小时窗口)
-- SELECT _irowts, AVG(total_energy), MAX(voltage_a) FROM t_1001 
-- WHERE ts > '2024-01-01 00:00:00' AND ts < '2024-01-02 00:00:00' INTERVAL(1h);

