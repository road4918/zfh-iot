-- ============================================
-- 表计实时数据表（MySQL存储，只保留最新数据）
-- ============================================

-- 电表累积量实时数据表
CREATE TABLE data_power_cumulant_realtime (
    point_id        INT(8) NOT NULL COMMENT '计量点编码',
    phase_type      SMALLINT(2) NOT NULL COMMENT '分相类型：0)总',
    tariff_type     SMALLINT(2) NOT NULL COMMENT '费率类型：0)总，1)尖，2)峰，3)平，4)谷',
    data_time       DATETIME NOT NULL COMMENT '最新数据时间',
    forward_active  DECIMAL(20,4) DEFAULT NULL COMMENT '正向有功表码',
    reverse_active  DECIMAL(20,4) DEFAULT NULL COMMENT '反向有功表码',
    combined_active DECIMAL(20,4) DEFAULT NULL COMMENT '组合有功表码',
    forward_reactive    DECIMAL(20,4) DEFAULT NULL COMMENT '正向无功表码',
    reverse_reactive    DECIMAL(20,4) DEFAULT NULL COMMENT '反向无功表码',
    combined_reactive   DECIMAL(20,4) DEFAULT NULL COMMENT '组合无功表码',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (point_id, phase_type, tariff_type),
    INDEX idx_point_id (point_id),
    INDEX idx_update_time (update_time)
) COMMENT='电表累积量实时数据表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 电表瞬时量实时数据表
CREATE TABLE data_power_instant_realtime (
    point_id    INT(8) NOT NULL COMMENT '计量点编码',
    data_time   DATETIME NOT NULL COMMENT '最新数据时间',
    p           DECIMAL(8,4) DEFAULT NULL COMMENT '总有功功率',
    pa          DECIMAL(8,4) DEFAULT NULL COMMENT 'A相有功功率',
    pb          DECIMAL(8,4) DEFAULT NULL COMMENT 'B相有功功率',
    pc          DECIMAL(8,4) DEFAULT NULL COMMENT 'C相有功功率',
    q           DECIMAL(8,4) DEFAULT NULL COMMENT '总无功功率',
    qa          DECIMAL(8,4) DEFAULT NULL COMMENT 'A相无功功率',
    qb          DECIMAL(8,4) DEFAULT NULL COMMENT 'B相无功功率',
    qc          DECIMAL(8,4) DEFAULT NULL COMMENT 'C相无功功率',
    s           DECIMAL(8,4) DEFAULT NULL COMMENT '总视在功率',
    sa          DECIMAL(8,4) DEFAULT NULL COMMENT 'A相视在功率',
    sb          DECIMAL(8,4) DEFAULT NULL COMMENT 'B相视在功率',
    sc          DECIMAL(8,4) DEFAULT NULL COMMENT 'C相视在功率',
    ia          DECIMAL(8,4) DEFAULT NULL COMMENT 'A相电流有效值',
    ib          DECIMAL(8,4) DEFAULT NULL COMMENT 'B相电流有效值',
    ic          DECIMAL(8,4) DEFAULT NULL COMMENT 'C相电流有效值',
    iz          DECIMAL(8,4) DEFAULT NULL COMMENT '0序电流有效值',
    ua          DECIMAL(8,4) DEFAULT NULL COMMENT 'A相电压有效值',
    ub          DECIMAL(8,4) DEFAULT NULL COMMENT 'B相电压有效值',
    uc          DECIMAL(8,4) DEFAULT NULL COMMENT 'C相电压有效值',
    uab         DECIMAL(8,4) DEFAULT NULL COMMENT 'AB线电压',
    ubc         DECIMAL(8,4) DEFAULT NULL COMMENT 'BC线电压',
    uca         DECIMAL(8,4) DEFAULT NULL COMMENT 'CA线电压',
    pf          DECIMAL(8,4) DEFAULT NULL COMMENT '总功率因数',
    pfa         DECIMAL(8,4) DEFAULT NULL COMMENT 'A相功率因数',
    pfb         DECIMAL(8,4) DEFAULT NULL COMMENT 'B相功率因数',
    pfc         DECIMAL(8,4) DEFAULT NULL COMMENT 'C相功率因数',
    hz          DECIMAL(8,4) DEFAULT NULL COMMENT '频率',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (point_id),
    INDEX idx_data_time (data_time),
    INDEX idx_update_time (update_time)
) COMMENT='电表瞬时量实时数据表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通用累积量实时数据表（水/气/热表）
CREATE TABLE data_cumulant_realtime (
    point_id        INT(8) NOT NULL COMMENT '计量点编码',
    energy_type     SMALLINT(2) NOT NULL COMMENT '能源种类：1)电 2)水 3)气 4)热',
    data_item       SMALLINT(4) NOT NULL COMMENT '数据项',
    data_time       DATETIME NOT NULL COMMENT '最新数据时间',
    value           DECIMAL(20,4) DEFAULT NULL COMMENT '累积值(表码)',
    battery_status  SMALLINT(2) DEFAULT NULL COMMENT '电池状态：0)正常，1)欠压',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (point_id, energy_type, data_item),
    INDEX idx_point_id (point_id),
    INDEX idx_energy_type (energy_type),
    INDEX idx_update_time (update_time)
) COMMENT='通用累积量实时数据表（水/气/热表）' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通用瞬时量实时数据表
CREATE TABLE data_instant_realtime (
    point_id    DECIMAL(8,0) NOT NULL COMMENT '计量点编码',
    data_item   SMALLINT(4) NOT NULL COMMENT '数据项',
    data_time   DATETIME NOT NULL COMMENT '最新数据时间',
    value       DECIMAL(12,4) DEFAULT NULL COMMENT '瞬时值',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (point_id, data_item),
    INDEX idx_point_id (point_id),
    INDEX idx_update_time (update_time)
) COMMENT='通用瞬时量实时数据表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
