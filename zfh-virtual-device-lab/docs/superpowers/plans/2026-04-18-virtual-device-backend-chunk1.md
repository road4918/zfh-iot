# zfh-virtual-device-lab Backend Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Spring Boot backend for zfh-virtual-device-lab MVP, supporting virtual gateway/meter management, MQTT+JSON protocol, data simulation, and communication logging.

**Architecture:** Single Spring Boot 2.7 application with embedded H2 database. Uses Netty for TCP connections, Eclipse Paho for MQTT, WebSocket/STOMP for real-time frontend updates. Protocol handlers use strategy pattern for extensibility.

**Tech Stack:** Spring Boot 2.7.18, Java 17, Netty 4.1.x, Eclipse Paho 1.2.x, H2 Database, MyBatis Plus 3.5.5, WebSocket/STOMP, Lombok

---

## Chunk 1: Project Initialization & Database Schema

### Task 1: Create Spring Boot Project Structure

**Files:**
- Create: `zfh-virtual-device-backend/pom.xml`
- Create: `zfh-virtual-device-backend/src/main/resources/application.yml`
- Create: `zfh-virtual-device-backend/src/main/resources/db/schema.sql`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/VirtualDeviceApplication.java`

- [ ] **Step 1: Create Maven POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/>
    </parent>
    
    <groupId>com.zfh</groupId>
    <artifactId>zfh-virtual-device-backend</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <java.version>17</java.version>
        <netty.version>4.1.100.Final</netty.version>
        <paho.version>1.2.5</paho.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>${netty.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.eclipse.paho</groupId>
            <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
            <version>${paho.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: zfh-virtual-device-backend
  
  datasource:
    url: jdbc:h2:file:./data/virtual-device-db;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  sql:
    init:
      schema-locations: classpath:db/schema.sql
      mode: always

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

virtual-device:
  tcp:
    boss-threads: 2
    worker-threads: 8
    reconnect-base-interval: 5000
    reconnect-max-interval: 60000
    reconnect-max-attempts: 10
    reconnect-backoff-multiplier: 2
  mqtt:
    default-broker: tcp://localhost:1883
    default-qos: 1
    connection-timeout: 30
    keep-alive-interval: 60
    reconnect-base-interval: 5000
    reconnect-max-interval: 60000
    reconnect-max-attempts: 10
    reconnect-backoff-multiplier: 2
  simulation:
    default-report-interval: 30
    max-concurrent-devices: 1000
    scheduler-pool-size: 50
  logging:
    retention-days: 7
    max-records-per-query: 10000

jwt:
  secret: zfh-virtual-device-lab-secret-key-2026
  expiration: 3600000
  refresh-expiration: 604800000
```

- [ ] **Step 3: Create database schema**

```sql
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
```

- [ ] **Step 4: Create main application class**

```java
package com.zfh.virtualdevice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class VirtualDeviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VirtualDeviceApplication.class, args);
    }
}
```

- [ ] **Step 5: Verify project builds**

Run:
```bash
cd zfh-virtual-device-backend
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add zfh-virtual-device-backend/
git commit -m "feat: initialize Spring Boot project with database schema"
```

---

### Task 2: Create Entity Classes

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/entity/VirtualGateway.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/entity/VirtualMeter.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/entity/MeterDataTemplate.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/entity/TemplateDataItem.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/entity/MeterDataConfig.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/entity/CommunicationLog.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/enums/DeviceStatus.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/enums/DeviceType.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/enums/ProtocolType.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/enums/CommMode.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/enums/MeterType.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/enums/DataCategory.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/enums/Direction.java`

- [ ] **Step 1: Create enums**

```java
// DeviceStatus.java
package com.zfh.virtualdevice.enums;

public enum DeviceStatus {
    OFFLINE, ONLINE, ERROR
}
```

```java
// DeviceType.java
package com.zfh.virtualdevice.enums;

public enum DeviceType {
    GATEWAY, METER
}
```

```java
// ProtocolType.java
package com.zfh.virtualdevice.enums;

public enum ProtocolType {
    TCP_DIRECT, MQTT
}
```

```java
// CommMode.java
package com.zfh.virtualdevice.enums;

public enum CommMode {
    SERVER, CLIENT
}
```

```java
// MeterType.java
package com.zfh.virtualdevice.enums;

public enum MeterType {
    ELECTRIC, WATER, HEAT, GAS
}
```

```java
// DataCategory.java
package com.zfh.virtualdevice.enums;

public enum DataCategory {
    ACCUMULATING, FLUCTUATING, RATIO
}
```

```java
// Direction.java
package com.zfh.virtualdevice.enums;

public enum Direction {
    UP, DOWN
}
```

- [ ] **Step 2: Create VirtualGateway entity**

```java
package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("virtual_gateway")
public class VirtualGateway {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String communicationAddress;
    
    @EnumValue
    private ProtocolType protocol;
    
    @EnumValue
    private CommMode commMode;
    
    private Integer serverPort;
    private String clientHost;
    private Integer clientPort;
    
    @EnumValue
    private DeviceStatus status;
    
    private String mqttBroker;
    private String mqttClientId;
    private String mqttUsername;
    private String mqttPassword;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
```

- [ ] **Step 3: Create VirtualMeter entity**

```java
package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("virtual_meter")
public class VirtualMeter {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long gatewayId;
    private String name;
    
    @EnumValue
    private MeterType meterType;
    
    private String communicationAddress;
    
    @EnumValue
    private ProtocolType protocol;
    
    private String connectionMode;
    
    @EnumValue
    private DeviceStatus status;
    
    private Boolean autoReport;
    private Integer reportInterval;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
```

- [ ] **Step 4: Create remaining entities**

```java
// MeterDataTemplate.java
package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meter_data_template")
public class MeterDataTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String templateName;
    
    @EnumValue
    private MeterType meterType;
    
    @EnumValue
    private ProtocolType protocol;
    
    private String description;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
```

```java
// TemplateDataItem.java
package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("template_data_item")
public class TemplateDataItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long templateId;
    private String dataType;
    
    @EnumValue
    private DataCategory dataCategory;
    
    private BigDecimal initialValue;
    private BigDecimal incrementMin;
    private BigDecimal incrementMax;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String fluctuationType;
    private BigDecimal ratioMin;
    private BigDecimal ratioMax;
    private String unit;
    private Integer sortOrder;
}
```

```java
// MeterDataConfig.java
package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("meter_data_config")
public class MeterDataConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long meterId;
    private Long templateId;
    private String dataType;
    
    @EnumValue
    private DataCategory dataCategory;
    
    private BigDecimal currentValue;
    private String configParams;
    private String overrideParams;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

```java
// CommunicationLog.java
package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("communication_log")
public class CommunicationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @EnumValue
    private DeviceType deviceType;
    
    private Long deviceId;
    
    @EnumValue
    private Direction direction;
    
    private String protocol;
    private String rawData;
    private String parsedData;
    
    private LocalDateTime timestamp;
}
```

- [ ] **Step 5: Create MyBatis Plus meta object handler**

```java
package com.zfh.virtualdevice.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

- [ ] **Step 6: Verify compilation**

Run:
```bash
cd zfh-virtual-device-backend
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/entity/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/enums/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/config/
git commit -m "feat: add entity classes and enums"
```

---

