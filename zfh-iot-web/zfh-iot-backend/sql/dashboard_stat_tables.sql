-- ============================================================
-- Dashboard Statistics Tables
-- 新增统计相关表结构
-- ============================================================

-- 1. 设备每日统计快照表
CREATE TABLE IF NOT EXISTS `stat_device_daily` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '设备总数',
    `online_count` INT NOT NULL DEFAULT 0 COMMENT '在线设备数',
    `offline_count` INT NOT NULL DEFAULT 0 COMMENT '离线设备数',
    `abnormal_count` INT NOT NULL DEFAULT 0 COMMENT '异常设备数',
    `inactive_count` INT NOT NULL DEFAULT 0 COMMENT '未激活设备数',
    `online_rate` DECIMAL(8,2) DEFAULT 0.00 COMMENT '在线率(%)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_date` (`tenant_id`, `stat_date`),
    KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备每日统计快照表';

-- 2. 设备命令表
CREATE TABLE IF NOT EXISTS `iot_command` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `device_id` BIGINT NOT NULL COMMENT '设备ID(iot_meter.id)',
    `gateway_id` BIGINT DEFAULT NULL COMMENT '网关ID',
    `command_type` VARCHAR(64) NOT NULL COMMENT '命令类型',
    `command_content` TEXT COMMENT '命令内容(JSON)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '命令状态: 0=等待, 1=已发送, 2=已送达, 3=成功, 4=失败, 5=超时, 6=超期, 7=取消',
    `response_content` TEXT COMMENT '响应内容',
    `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
    `ack_time` DATETIME DEFAULT NULL COMMENT '确认时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT DEFAULT 0 COMMENT '软删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备命令表';

-- 3. 推送每日统计表
CREATE TABLE IF NOT EXISTS `stat_push_daily` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '推送总数',
    `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_date` (`tenant_id`, `stat_date`),
    KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送每日统计表';

-- 4. 消息每日统计表
CREATE TABLE IF NOT EXISTS `stat_message_daily` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `message_count` BIGINT NOT NULL DEFAULT 0 COMMENT '上报消息数',
    `command_count` BIGINT NOT NULL DEFAULT 0 COMMENT '下发命令数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_date` (`tenant_id`, `stat_date`),
    KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息每日统计表';

-- 5. 命令每日统计表（按状态聚合，便于快速查询）
CREATE TABLE IF NOT EXISTS `stat_command_daily` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `delivered_count` INT NOT NULL DEFAULT 0 COMMENT '已送达',
    `failed_count` INT NOT NULL DEFAULT 0 COMMENT '失败',
    `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功',
    `overdue_count` INT NOT NULL DEFAULT 0 COMMENT '超期',
    `timeout_count` INT NOT NULL DEFAULT 0 COMMENT '超时',
    `cancelled_count` INT NOT NULL DEFAULT 0 COMMENT '取消',
    `waiting_count` INT NOT NULL DEFAULT 0 COMMENT '等待',
    `sent_count` INT NOT NULL DEFAULT 0 COMMENT '已发送',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_date` (`tenant_id`, `stat_date`),
    KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命令每日统计表';

-- ============================================================
-- 更新 iot_meter 的 status 字段含义
-- 0=离线, 1=在线, 2=未激活, 3=异常
-- 注意: 如果之前有 status=2 的记录(原为禁用), 需要确认是否迁移
-- ============================================================
