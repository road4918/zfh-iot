-- 创建数据库
CREATE DATABASE IF NOT EXISTS zfh_iot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE zfh_iot;

-- ============================================
-- 租户管理模块
-- ============================================
CREATE TABLE sys_tenant (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Tenant ID',
    tenant_code     VARCHAR(64) NOT NULL UNIQUE COMMENT 'Tenant code',
    tenant_name     VARCHAR(128) NOT NULL COMMENT 'Tenant name',
    contact_name    VARCHAR(64) COMMENT 'Contact person',
    contact_phone   VARCHAR(20) COMMENT 'Contact phone',
    max_devices     INT DEFAULT 1000 COMMENT 'Max devices quota',
    max_gateways    INT DEFAULT 100 COMMENT 'Max gateways quota',
    storage_days    INT DEFAULT 365 COMMENT 'Data storage days',
    status          TINYINT DEFAULT 1 COMMENT 'Status: 0 disabled 1 enabled',
    deleted         TINYINT DEFAULT 0 COMMENT 'Deleted flag',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_code (tenant_code)
) COMMENT='Tenant table';

-- ============================================
-- 用户权限模块
-- ============================================
CREATE TABLE sys_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'User ID',
    tenant_id       BIGINT DEFAULT NULL COMMENT 'Tenant ID (null for platform users)',
    username        VARCHAR(64) NOT NULL COMMENT 'Username',
    password        VARCHAR(128) NOT NULL COMMENT 'Encrypted password',
    real_name       VARCHAR(64) COMMENT 'Real name',
    phone           VARCHAR(20) COMMENT 'Phone',
    email           VARCHAR(128) COMMENT 'Email',
    avatar          VARCHAR(256) COMMENT 'Avatar URL',
    user_type       TINYINT DEFAULT 1 COMMENT 'User type: 0 platform 1 tenant',
    status          TINYINT DEFAULT 1 COMMENT 'Status: 0 disabled 1 enabled',
    deleted         TINYINT DEFAULT 0 COMMENT 'Deleted flag',
    created_by      BIGINT COMMENT 'Created by user ID',
    last_login_time DATETIME COMMENT 'Last login time',
    last_login_ip   VARCHAR(64) COMMENT 'Last login IP',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    INDEX idx_tenant (tenant_id),
    INDEX idx_user_type (user_type),
    INDEX idx_status (status)
) COMMENT='User table';

CREATE TABLE sys_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT DEFAULT NULL COMMENT 'Tenant ID (null for platform roles)',
    role_code       VARCHAR(64) NOT NULL COMMENT 'Role code',
    role_name       VARCHAR(128) NOT NULL COMMENT 'Role name',
    description     VARCHAR(256) COMMENT 'Description',
    role_type       TINYINT DEFAULT 1 COMMENT 'Role type: 0 platform 1 tenant',
    status          TINYINT DEFAULT 1,
    deleted         TINYINT DEFAULT 0,
    created_by      BIGINT COMMENT 'Created by user ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code),
    INDEX idx_tenant (tenant_id),
    INDEX idx_role_type (role_type)
) COMMENT='Role table';

CREATE TABLE sys_user_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user (user_id),
    INDEX idx_role (role_id)
) COMMENT='User role relation';

CREATE TABLE sys_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id       BIGINT DEFAULT 0 COMMENT 'Parent permission ID',
    perm_code       VARCHAR(128) NOT NULL COMMENT 'Permission code',
    perm_name       VARCHAR(128) NOT NULL COMMENT 'Permission name',
    perm_type       TINYINT COMMENT 'Type: 1 menu 2 button 3 api',
    path            VARCHAR(256) COMMENT 'Route path/API path',
    component       VARCHAR(128) COMMENT 'Component path',
    icon            VARCHAR(64) COMMENT 'Icon',
    sort_order      INT DEFAULT 0 COMMENT 'Sort order',
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='Permission table';

CREATE TABLE sys_role_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id         BIGINT NOT NULL,
    perm_id         BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, perm_id)
) COMMENT='Role permission relation';

CREATE TABLE sys_operation_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT COMMENT 'Tenant ID',
    user_id         BIGINT COMMENT 'User ID',
    username        VARCHAR(64) COMMENT 'Username',
    operation       VARCHAR(128) COMMENT 'Operation description',
    method          VARCHAR(256) COMMENT 'Request method',
    params          TEXT COMMENT 'Request params',
    ip              VARCHAR(64) COMMENT 'IP address',
    user_agent      VARCHAR(512) COMMENT 'User agent',
    duration        INT COMMENT 'Duration (ms)',
    status          TINYINT COMMENT 'Status: 0 failed 1 success',
    error_msg       TEXT COMMENT 'Error message',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_user (user_id),
    INDEX idx_time (create_time)
) COMMENT='Operation log table';

-- ============================================
-- 档案管理模块
-- ============================================
CREATE TABLE iot_manufacturer (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT COMMENT 'Tenant ID',
    manufacturer_name VARCHAR(128) NOT NULL COMMENT 'Manufacturer name',
    contact_name    VARCHAR(64) COMMENT 'Contact person',
    contact_phone   VARCHAR(20) COMMENT 'Contact phone',
    address         VARCHAR(256) COMMENT 'Address',
    status          TINYINT DEFAULT 1,
    deleted         TINYINT DEFAULT 0,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT='Manufacturer table';

CREATE TABLE iot_protocol (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    protocol_code   VARCHAR(32) NOT NULL UNIQUE COMMENT 'Protocol code',
    protocol_name   VARCHAR(128) NOT NULL COMMENT 'Protocol name',
    protocol_type   TINYINT COMMENT 'Type: 1 standard 2 custom',
    version         VARCHAR(32) COMMENT 'Version',
    jar_path        VARCHAR(256) COMMENT 'JAR path',
    description     VARCHAR(512) COMMENT 'Description',
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='Protocol table';

CREATE TABLE iot_protocol_field (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    protocol_code   VARCHAR(32) NOT NULL COMMENT 'Protocol code',
    field_code      VARCHAR(64) NOT NULL COMMENT 'Field code',
    field_name      VARCHAR(128) COMMENT 'Field name',
    field_type      VARCHAR(32) COMMENT 'Field type',
    byte_offset     INT COMMENT 'Byte offset',
    byte_length     INT COMMENT 'Byte length',
    scale_factor    DECIMAL(10,4) DEFAULT 1.0000 COMMENT 'Scale factor',
    unit            VARCHAR(32) COMMENT 'Unit',
    description     VARCHAR(256),
    INDEX idx_protocol (protocol_code)
) COMMENT='Protocol field table';

CREATE TABLE iot_gateway (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id           BIGINT NOT NULL COMMENT 'Tenant ID',
    gateway_no          VARCHAR(64) NOT NULL COMMENT 'Gateway number',
    gateway_name        VARCHAR(128) COMMENT 'Gateway name',
    gateway_type        VARCHAR(32) COMMENT 'Gateway type',
    manufacturer_id     BIGINT COMMENT 'Manufacturer ID',
    protocol_code       VARCHAR(32) COMMENT 'Protocol code',
    comm_addr           VARCHAR(64) COMMENT 'Communication address',
    ip_address          VARCHAR(64) COMMENT 'IP address',
    port                INT COMMENT 'Port',
    device_limit        INT DEFAULT 100 COMMENT 'Device limit',
    heartbeat_interval  INT DEFAULT 60 COMMENT 'Heartbeat interval (seconds)',
    location            VARCHAR(255) COMMENT 'Installation location',
    status              TINYINT DEFAULT 0 COMMENT 'Status: 0 offline 1 online 2 disabled',
    last_online_time    DATETIME COMMENT 'Last online time',
    remark              VARCHAR(512) COMMENT 'Remark',
    deleted             TINYINT DEFAULT 0,
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_no (tenant_id, gateway_no),
    UNIQUE KEY uk_comm_addr (comm_addr),
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (status),
    INDEX idx_protocol (protocol_code)
) COMMENT='Gateway table';

CREATE TABLE iot_meter (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id           BIGINT NOT NULL COMMENT 'Tenant ID',
    gateway_id          BIGINT COMMENT 'Gateway ID',
    meter_no            VARCHAR(64) NOT NULL COMMENT 'Meter number',
    meter_name          VARCHAR(128) COMMENT 'Meter name',
    meter_type          TINYINT NOT NULL COMMENT 'Type: 1 electric 2 water 3 gas 4 heat',
    manufacturer_id     BIGINT COMMENT 'Manufacturer ID',
    protocol_code       VARCHAR(32) COMMENT 'Protocol code',
    device_address      VARCHAR(32) COMMENT 'Device address',
    ct_ratio            DECIMAL(10,2) DEFAULT 1.00 COMMENT 'CT ratio',
    pt_ratio            DECIMAL(10,2) DEFAULT 1.00 COMMENT 'PT ratio',
    meter_ratio         DECIMAL(10,2) DEFAULT 1.00 COMMENT 'Meter ratio',
    address             VARCHAR(256) COMMENT 'Installation address',
    longitude           DECIMAL(10,7) COMMENT 'Longitude',
    latitude            DECIMAL(10,7) COMMENT 'Latitude',
    install_time        DATE COMMENT 'Install date',
    status              TINYINT DEFAULT 0 COMMENT 'Status: 0 offline 1 online 2 disabled',
    last_online_time    DATETIME COMMENT 'Last online time',
    last_reading_time   DATETIME COMMENT 'Last reading time',
    remark              VARCHAR(512) COMMENT 'Remark',
    deleted             TINYINT DEFAULT 0,
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_no (tenant_id, meter_no),
    INDEX idx_tenant (tenant_id),
    INDEX idx_gateway (gateway_id),
    INDEX idx_type (meter_type),
    INDEX idx_status (status)
) COMMENT='Meter table';

CREATE TABLE iot_group (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL COMMENT 'Tenant ID',
    parent_id       BIGINT DEFAULT 0 COMMENT 'Parent group ID',
    group_name      VARCHAR(128) NOT NULL COMMENT 'Group name',
    group_type      TINYINT COMMENT 'Group type',
    description     VARCHAR(256) COMMENT 'Description',
    sort_order      INT DEFAULT 0,
    deleted         TINYINT DEFAULT 0,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_parent (parent_id)
) COMMENT='Group table';

CREATE TABLE iot_group_meter (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id    BIGINT NOT NULL,
    meter_id    BIGINT NOT NULL,
    UNIQUE KEY uk_group_meter (group_id, meter_id),
    INDEX idx_group (group_id),
    INDEX idx_meter (meter_id)
) COMMENT='Group meter relation';

-- ============================================
-- 初始化数据
-- ============================================

-- 默认租户
INSERT INTO sys_tenant (tenant_code, tenant_name, max_devices, max_gateways, status) 
VALUES ('admin', 'Default Tenant', 10000, 1000, 1);

-- 默认平台用户（密码：admin123，SHA-256加密）
INSERT INTO sys_user (tenant_id, username, password, real_name, user_type, status) 
VALUES (null, 'admin', 'c3284d0f94606de1fd2af172aba15bf3', 'System Administrator', 0, 1);

-- 默认平台角色
INSERT INTO sys_role (tenant_id, role_code, role_name, description, role_type, status) 
VALUES (null, 'admin', 'System Administrator', 'Full permissions', 0, 1);

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 默认权限（菜单）
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, component, icon, sort_order) VALUES
(0, 'dashboard', 'Dashboard', 1, '/dashboard', 'Dashboard', 'HomeFilled', 1),
(0, 'system', 'System Management', 1, '/system', NULL, 'Setting', 10),
(0, 'archive', 'Archive Management', 1, '/archive', NULL, 'Folder', 20),
(0, 'data', 'Reading Data', 1, '/data', NULL, 'DataLine', 30);

-- 系统管理子菜单
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, component, sort_order) VALUES
(2, 'tenant:list', 'Tenant Management', 1, '/system/tenant', 'system/tenant/index', 1),
(2, 'user:list', 'User Management', 1, '/system/user', 'system/user/index', 2),
(2, 'role:list', 'Role Management', 1, '/system/role', 'system/role/index', 3),
(2, 'log:list', 'Operation Log', 1, '/system/log', 'system/log/index', 4);

-- 租户管理按钮权限（挂在 tenant:list 下）
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order) VALUES
(5, 'tenant:create', '新增租户', 2, 1),
(5, 'tenant:update', '编辑租户', 2, 2),
(5, 'tenant:delete', '删除租户', 2, 3),
(5, 'tenant:status', '启用/禁用租户', 2, 4);

-- 用户管理按钮权限（挂在 user:list 下）
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order) VALUES
(6, 'user:create', '新增用户', 2, 1),
(6, 'user:update', '编辑用户', 2, 2),
(6, 'user:delete', '删除用户', 2, 3),
(6, 'user:reset-password', '重置密码', 2, 4),
(6, 'user:change-password', '修改密码', 2, 5);

-- 角色管理按钮权限（挂在 role:list 下）
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order) VALUES
(7, 'role:create', '新增角色', 2, 1),
(7, 'role:update', '编辑角色', 2, 2),
(7, 'role:delete', '删除角色', 2, 3),
(7, 'role:assign-permission', '分配权限', 2, 4);

-- 档案管理子菜单
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, component, sort_order) VALUES
(3, 'gateway:list', 'Gateway Management', 1, '/archive/gateway', 'archive/gateway/index', 1),
(3, 'meter:list', 'Meter Management', 1, '/archive/meter', 'archive/meter/index', 2),
(3, 'group:list', 'Group Management', 1, '/archive/group', 'archive/group/index', 3),
(3, 'protocol:list', 'Protocol Management', 1, '/archive/protocol', 'archive/protocol/index', 4),
(3, 'manufacturer:list', 'Manufacturer Management', 1, '/archive/manufacturer', 'archive/manufacturer/index', 5);

-- 网关管理按钮权限（挂在 gateway:list 下）
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order) VALUES
(16, 'gateway:create', '新增网关', 2, 1),
(16, 'gateway:update', '编辑网关', 2, 2),
(16, 'gateway:delete', '删除网关', 2, 3);

-- 表计管理按钮权限（挂在 meter:list 下）
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order) VALUES
(17, 'meter:create', '新增表计', 2, 1),
(17, 'meter:update', '编辑表计', 2, 2),
(17, 'meter:delete', '删除表计', 2, 3),
(17, 'meter:bind-gateway', '绑定网关', 2, 4),
(17, 'meter:unbind-gateway', '解绑网关', 2, 5);

-- 群组管理按钮权限（挂在 group:list 下）
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order) VALUES
(18, 'group:create', '新增群组', 2, 1),
(18, 'group:update', '编辑群组', 2, 2),
(18, 'group:delete', '删除群组', 2, 3),
(18, 'group:add-meter', '添加设备', 2, 4),
(18, 'group:remove-meter', '移除设备', 2, 5);

-- 抄表数据子菜单
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, component, sort_order) VALUES
(4, 'reading:current', 'Current Data', 1, '/data/current', 'data/current/index', 1),
(4, 'reading:history', 'History Data', 1, '/data/history', 'data/history/index', 2);

-- 抄表数据按钮权限（挂在 reading:current 下）
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order) VALUES
(29, 'reading:export', '数据导出', 2, 1);

-- 给管理员角色分配所有权限
INSERT INTO sys_role_permission (role_id, perm_id) 
SELECT 1, id FROM sys_permission;

-- 默认协议
INSERT INTO iot_protocol (protocol_code, protocol_name, protocol_type, version, description) VALUES
('modbus', 'Modbus TCP', 1, '1.0', 'Standard Modbus TCP protocol'),
('dlt645', 'DL/T645', 1, '2007', 'Electric power industry standard'),
('custom', 'Custom Protocol', 2, '1.0', 'Custom binary protocol');

-- ============================================
-- 表计数据表（MySQL存储，替代TDengine）
-- ============================================

-- 电表累积量数据表（表码数据）
CREATE TABLE data_power_cumulant (
    point_id        INT(8) NOT NULL COMMENT '计量点编码',
    phase_type      SMALLINT(2) NOT NULL COMMENT '分相类型：0)总',
    tariff_type     SMALLINT(2) NOT NULL COMMENT '费率类型：0)总，1)尖，2)峰，3)平，4)谷',
    data_time       DATETIME NOT NULL COMMENT '数据时间',
    forward_active  DECIMAL(20,4) DEFAULT NULL COMMENT '正向有功表码',
    reverse_active  DECIMAL(20,4) DEFAULT NULL COMMENT '反向有功表码',
    combined_active DECIMAL(20,4) DEFAULT NULL COMMENT '组合有功表码',
    forward_reactive    DECIMAL(20,4) DEFAULT NULL COMMENT '正向无功表码',
    reverse_reactive    DECIMAL(20,4) DEFAULT NULL COMMENT '反向无功表码',
    combined_reactive   DECIMAL(20,4) DEFAULT NULL COMMENT '组合无功表码',
    add_time        DATETIME DEFAULT NULL COMMENT '添加时间',
    update_time     DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (point_id, phase_type, tariff_type, data_time),
    INDEX idx_point_time (point_id, data_time),
    INDEX idx_data_time (data_time)
) COMMENT='电表累积量数据表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 电表瞬时数据表
CREATE TABLE data_power_instant (
    point_id    INT(8) NOT NULL COMMENT '计量点编码',
    data_time   DATETIME NOT NULL COMMENT '数据时间',
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
    add_time    DATETIME DEFAULT NULL COMMENT '添加时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (point_id, data_time),
    INDEX idx_point_time (point_id, data_time),
    INDEX idx_data_time (data_time)
) COMMENT='电表瞬时数据表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通用累积量数据表（水/气/热表）
CREATE TABLE data_cumulant (
    point_id        INT(8) NOT NULL COMMENT '计量点编码',
    energy_type     SMALLINT(2) NOT NULL COMMENT '能源种类：1)电 2)水 3)气 4)热',
    data_item       SMALLINT(4) NOT NULL COMMENT '数据项',
    data_time       DATETIME NOT NULL COMMENT '数据时间',
    value           DECIMAL(20,4) DEFAULT NULL COMMENT '累积值(表码)',
    battery_status  SMALLINT(2) DEFAULT NULL COMMENT '电池状态：0)正常，1)欠压',
    add_time        DATETIME DEFAULT NULL COMMENT '添加时间',
    update_time     DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (point_id, energy_type, data_item, data_time),
    INDEX idx_point_time (point_id, data_time),
    INDEX idx_data_time (data_time),
    INDEX idx_energy_type (energy_type)
) COMMENT='通用累积量数据表（水/气/热表）' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通用瞬时数据表
CREATE TABLE data_instant (
    point_id    DECIMAL(8,0) NOT NULL COMMENT '计量点编码',
    data_item   SMALLINT(4) NOT NULL COMMENT '数据项',
    data_time   DATETIME NOT NULL COMMENT '数据时间',
    value       DECIMAL(12,4) DEFAULT NULL COMMENT '瞬时值',
    add_time    DATETIME DEFAULT NULL COMMENT '添加时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (point_id, data_item, data_time),
    INDEX idx_point_time (point_id, data_time),
    INDEX idx_data_time (data_time)
) COMMENT='通用瞬时数据表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
