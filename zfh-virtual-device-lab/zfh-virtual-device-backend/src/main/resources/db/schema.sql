-- 虚拟网关表
CREATE TABLE IF NOT EXISTS virtual_gateway (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    communication_address VARCHAR(50) NOT NULL UNIQUE,
    protocol VARCHAR(20) NOT NULL,
    comm_mode VARCHAR(20) NOT NULL,
    server_port INT,
    client_host VARCHAR(50),
    client_port INT,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    mqtt_broker VARCHAR(100),
    mqtt_client_id VARCHAR(50),
    mqtt_username VARCHAR(50),
    mqtt_password VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

-- 虚拟表计表
CREATE TABLE IF NOT EXISTS virtual_meter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gateway_id BIGINT,
    name VARCHAR(100) NOT NULL,
    meter_type VARCHAR(20) NOT NULL,
    communication_address VARCHAR(50) NOT NULL UNIQUE,
    protocol VARCHAR(20) NOT NULL,
    connection_mode VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    auto_report BOOLEAN NOT NULL DEFAULT TRUE,
    report_interval INT NOT NULL DEFAULT 30,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (gateway_id) REFERENCES virtual_gateway(id)
);

-- 数据配置模板表
CREATE TABLE IF NOT EXISTS meter_data_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    meter_type VARCHAR(20) NOT NULL,
    protocol VARCHAR(20),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

-- 模板数据项表
CREATE TABLE IF NOT EXISTS template_data_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    data_category VARCHAR(20) NOT NULL,
    initial_value DECIMAL(18,4),
    increment_min DECIMAL(18,4),
    increment_max DECIMAL(18,4),
    min_value DECIMAL(18,4),
    max_value DECIMAL(18,4),
    fluctuation_type VARCHAR(20),
    ratio_min DECIMAL(18,4),
    ratio_max DECIMAL(18,4),
    unit VARCHAR(20),
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (template_id) REFERENCES meter_data_template(id) ON DELETE CASCADE
);

-- 表计数据配置表
CREATE TABLE IF NOT EXISTS meter_data_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meter_id BIGINT NOT NULL,
    template_id BIGINT,
    data_type VARCHAR(50) NOT NULL,
    data_category VARCHAR(20) NOT NULL,
    current_value DECIMAL(18,4) NOT NULL DEFAULT 0,
    config_params TEXT,
    override_params TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (meter_id) REFERENCES virtual_meter(id) ON DELETE CASCADE,
    FOREIGN KEY (template_id) REFERENCES meter_data_template(id) ON DELETE SET NULL
);

-- 通讯日志表
CREATE TABLE IF NOT EXISTS communication_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_type VARCHAR(20) NOT NULL,
    device_id BIGINT NOT NULL,
    direction VARCHAR(10) NOT NULL,
    protocol VARCHAR(20) NOT NULL,
    raw_data TEXT NOT NULL,
    parsed_data TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_comm_log_device ON communication_log(device_type, device_id);
CREATE INDEX IF NOT EXISTS idx_comm_log_timestamp ON communication_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_meter_gateway ON virtual_meter(gateway_id);
CREATE INDEX IF NOT EXISTS idx_meter_type ON virtual_meter(meter_type);
CREATE INDEX IF NOT EXISTS idx_data_config_meter ON meter_data_config(meter_id);
