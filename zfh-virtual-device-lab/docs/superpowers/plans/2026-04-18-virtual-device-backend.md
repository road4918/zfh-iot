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

## Chunk 2: Mapper Layer & Basic CRUD APIs

### Task 3: Create MyBatis Mappers

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/VirtualGatewayMapper.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/VirtualMeterMapper.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/CommunicationLogMapper.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/MeterDataConfigMapper.java`

- [ ] **Step 1: Create mapper interfaces**

```java
package com.zfh.virtualdevice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zfh.virtualdevice.entity.VirtualGateway;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VirtualGatewayMapper extends BaseMapper<VirtualGateway> {
}
```

```java
package com.zfh.virtualdevice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zfh.virtualdevice.entity.VirtualMeter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VirtualMeterMapper extends BaseMapper<VirtualMeter> {
}
```

```java
package com.zfh.virtualdevice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zfh.virtualdevice.entity.CommunicationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommunicationLogMapper extends BaseMapper<CommunicationLog> {
}
```

```java
package com.zfh.virtualdevice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zfh.virtualdevice.entity.MeterDataConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeterDataConfigMapper extends BaseMapper<MeterDataConfig> {
}
```

- [ ] **Step 2: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/mapper/
git commit -m "feat: add MyBatis mappers"
```

---

### Task 4: Gateway CRUD API

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/dto/Result.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/VirtualGatewayController.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/VirtualGatewayService.java`

- [ ] **Step 1: Create统一响应DTO**

```java
package com.zfh.virtualdevice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    public static <T> Result<T> success() {
        return success(null);
    }
    
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
}
```

- [ ] **Step 2: Create Gateway Service**

```java
package com.zfh.virtualdevice.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.mapper.VirtualGatewayMapper;
import org.springframework.stereotype.Service;

@Service
public class VirtualGatewayService extends ServiceImpl<VirtualGatewayMapper, VirtualGateway> {
}
```

- [ ] **Step 3: Create Gateway Controller**

```java
package com.zfh.virtualdevice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.service.VirtualGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gateways")
public class VirtualGatewayController {
    
    @Autowired
    private VirtualGatewayService gatewayService;
    
    @GetMapping
    public Result<Page<VirtualGateway>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<VirtualGateway> page = new Page<>(current, size);
        return Result.success(gatewayService.page(page));
    }
    
    @GetMapping("/{id}")
    public Result<VirtualGateway> getById(@PathVariable Long id) {
        return Result.success(gatewayService.getById(id));
    }
    
    @PostMapping
    public Result<Void> save(@RequestBody VirtualGateway gateway) {
        // Check duplicate communication address
        VirtualGateway existing = gatewayService.lambdaQuery()
                .eq(VirtualGateway::getCommunicationAddress, gateway.getCommunicationAddress())
                .one();
        if (existing != null) {
            return Result.error("通讯地址已存在");
        }
        
        gateway.setStatus(com.zfh.virtualdevice.enums.DeviceStatus.OFFLINE);
        gatewayService.save(gateway);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody VirtualGateway gateway) {
        gateway.setId(id);
        gatewayService.updateById(gateway);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        gatewayService.removeById(id);
        return Result.success();
    }
}
```

- [ ] **Step 4: Test Gateway API**

Run application:
```bash
cd zfh-virtual-device-backend
mvn spring-boot:run
```

Test create:
```bash
curl -X POST http://localhost:8080/api/gateways \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试网关",
    "communicationAddress": "GW001",
    "protocol": "MQTT",
    "commMode": "CLIENT",
    "clientHost": "localhost",
    "clientPort": 1883,
    "mqttBroker": "tcp://localhost:1883",
    "mqttClientId": "gw-001"
  }'
```
Expected: `{"code":200,"message":"success","timestamp":"..."}`

Test list:
```bash
curl http://localhost:8080/api/gateways
```
Expected: List with created gateway

- [ ] **Step 5: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/dto/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/
git commit -m "feat: add gateway CRUD API"
```

---

### Task 5: Meter CRUD API

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/VirtualMeterController.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/VirtualMeterService.java`

- [ ] **Step 1: Create Meter Service**

```java
package com.zfh.virtualdevice.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import org.springframework.stereotype.Service;

@Service
public class VirtualMeterService extends ServiceImpl<VirtualMeterMapper, VirtualMeter> {
}
```

- [ ] **Step 2: Create Meter Controller**

```java
package com.zfh.virtualdevice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.service.VirtualMeterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meters")
public class VirtualMeterController {
    
    @Autowired
    private VirtualMeterService meterService;
    
    @GetMapping
    public Result<Page<VirtualMeter>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<VirtualMeter> page = new Page<>(current, size);
        return Result.success(meterService.page(page));
    }
    
    @GetMapping("/{id}")
    public Result<VirtualMeter> getById(@PathVariable Long id) {
        return Result.success(meterService.getById(id));
    }
    
    @PostMapping
    public Result<Void> save(@RequestBody VirtualMeter meter) {
        // Check duplicate communication address
        VirtualMeter existing = meterService.lambdaQuery()
                .eq(VirtualMeter::getCommunicationAddress, meter.getCommunicationAddress())
                .one();
        if (existing != null) {
            return Result.error("通讯地址已存在");
        }
        
        meter.setStatus(com.zfh.virtualdevice.enums.DeviceStatus.OFFLINE);
        meterService.save(meter);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody VirtualMeter meter) {
        meter.setId(id);
        meterService.updateById(meter);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        meterService.removeById(id);
        return Result.success();
    }
}
```

- [ ] **Step 3: Test Meter API**

Test create:
```bash
curl -X POST http://localhost:8080/api/meters \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试电表",
    "meterType": "ELECTRIC",
    "communicationAddress": "METER001",
    "protocol": "MQTT",
    "connectionMode": "DIRECT",
    "autoReport": true,
    "reportInterval": 30
  }'
```

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/VirtualMeterController.java
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/VirtualMeterService.java
git commit -m "feat: add meter CRUD API"
```

---

## Chunk 3: Protocol Framework & MQTT Implementation

### Task 6: Protocol Framework Core

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/EncodedMessage.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/DecodedMessage.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/MessageType.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/DeviceData.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/Command.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/CommandType.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/DeviceContext.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/ProtocolHandler.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/ProtocolFactory.java`

- [ ] **Step 1: Create message types**

```java
// MessageType.java
package com.zfh.virtualdevice.protocol;

public enum MessageType {
    BINARY, MQTT
}
```

```java
// EncodedMessage.java
package com.zfh.virtualdevice.protocol;

import lombok.Data;

@Data
public class EncodedMessage {
    private MessageType type;
    private byte[] binaryData;
    private String mqttTopic;
    private String mqttPayload;
    private int mqttQos;
}
```

```java
// DecodedMessage.java
package com.zfh.virtualdevice.protocol;

import lombok.Data;

@Data
public class DecodedMessage {
    private MessageType type;
    private byte[] binaryData;
    private String mqttTopic;
    private String mqttPayload;
}
```

```java
// CommandType.java
package com.zfh.virtualdevice.protocol;

public enum CommandType {
    READ_DATA, SET_PARAMS, REMOTE_CONTROL, TIME_SYNC, UNKNOWN
}
```

```java
// Command.java
package com.zfh.virtualdevice.protocol;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class Command {
    private CommandType type;
    private String dataId;
    private Map<String, Object> params;
    private LocalDateTime timestamp;
    private String rawFrame;
}
```

```java
// DeviceData.java
package com.zfh.virtualdevice.protocol;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class DeviceData {
    private Long deviceId;
    private String deviceAddress;
    private com.zfh.virtualdevice.enums.DeviceType deviceType;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
    private Map<String, String> units;
}
```

```java
// DeviceContext.java
package com.zfh.virtualdevice.protocol;

import io.netty.channel.Channel;
import lombok.Data;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class DeviceContext {
    private Long deviceId;
    private com.zfh.virtualdevice.enums.DeviceType deviceType;
    private String communicationAddress;
    private com.zfh.virtualdevice.enums.ProtocolType protocolType;
    private Channel nettyChannel;
    private MqttClient mqttClient;
    private Map<String, Object> attributes;
    
    public void send(byte[] data) {
        if (nettyChannel != null && nettyChannel.isActive()) {
            nettyChannel.writeAndFlush(data);
        }
    }
    
    public void sendJson(String topic, Object payload) {
        // Implemented by MQTT connection manager
    }
    
    public BigDecimal getMeterData(String dataType) {
        // Will be implemented with MeterDataConfig integration
        return BigDecimal.ZERO;
    }
    
    public void updateParams(Map<String, Object> params) {
        if (attributes != null) {
            attributes.putAll(params);
        }
    }
}
```

- [ ] **Step 2: Create ProtocolHandler interface**

```java
package com.zfh.virtualdevice.protocol;

import com.zfh.virtualdevice.enums.ProtocolType;

public interface ProtocolHandler {
    ProtocolType getProtocolType();
    
    EncodedMessage encode(DeviceData data, DeviceContext ctx);
    
    Command parseCommand(DecodedMessage message, DeviceContext ctx);
    
    void processCommand(DeviceContext ctx, Command cmd);
    
    void onConnected(DeviceContext ctx);
    
    void onDisconnected(DeviceContext ctx);
}
```

- [ ] **Step 3: Create ProtocolFactory**

```java
package com.zfh.virtualdevice.protocol;

import com.zfh.virtualdevice.enums.ProtocolType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProtocolFactory {
    
    @Autowired
    private List<ProtocolHandler> handlers;
    
    private final Map<ProtocolType, ProtocolHandler> handlerMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        for (ProtocolHandler handler : handlers) {
            handlerMap.put(handler.getProtocolType(), handler);
        }
    }
    
    public ProtocolHandler getHandler(ProtocolType type) {
        return handlerMap.get(type);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/
git commit -m "feat: add protocol framework core"
```

---

### Task 7: MQTT+JSON Protocol Handler

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/handler/MqttJsonHandler.java`

- [ ] **Step 1: Implement MQTT+JSON handler**

```java
package com.zfh.virtualdevice.protocol.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zfh.virtualdevice.enums.ProtocolType;
import com.zfh.virtualdevice.protocol.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class MqttJsonHandler implements ProtocolHandler {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.MQTT;
    }
    
    @Override
    public EncodedMessage encode(DeviceData data, DeviceContext ctx) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("deviceId", data.getDeviceId());
            payload.put("deviceAddress", data.getDeviceAddress());
            payload.put("timestamp", data.getTimestamp().toString());
            payload.put("data", data.getData());
            payload.put("units", data.getUnits());
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            EncodedMessage message = new EncodedMessage();
            message.setType(MessageType.MQTT);
            message.setMqttTopic("telemetry/" + data.getDeviceType().name().toLowerCase() + "/" + data.getDeviceId());
            message.setMqttPayload(jsonPayload);
            message.setMqttQos(1);
            
            return message;
        } catch (Exception e) {
            log.error("Failed to encode MQTT message", e);
            throw new RuntimeException("Encode failed", e);
        }
    }
    
    @Override
    public Command parseCommand(DecodedMessage message, DeviceContext ctx) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getMqttPayload(), Map.class);
            
            Command cmd = new Command();
            String cmdType = (String) payload.get("commandType");
            cmd.setType(CommandType.valueOf(cmdType));
            cmd.setDataId((String) payload.get("dataId"));
            cmd.setParams((Map<String, Object>) payload.get("params"));
            cmd.setTimestamp(java.time.LocalDateTime.now());
            cmd.setRawFrame(message.getMqttPayload());
            
            return cmd;
        } catch (Exception e) {
            log.error("Failed to parse MQTT command", e);
            Command cmd = new Command();
            cmd.setType(CommandType.UNKNOWN);
            return cmd;
        }
    }
    
    @Override
    public void processCommand(DeviceContext ctx, Command cmd) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("deviceId", ctx.getDeviceId());
            response.put("commandType", cmd.getType().name());
            response.put("status", "SUCCESS");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            switch (cmd.getType()) {
                case READ_DATA:
                    Object value = ctx.getMeterData(cmd.getDataId());
                    Map<String, Object> data = new HashMap<>();
                    data.put(cmd.getDataId(), value);
                    response.put("data", data);
                    break;
                case SET_PARAMS:
                    ctx.updateParams(cmd.getParams());
                    break;
                case TIME_SYNC:
                    // Update device time
                    break;
                default:
                    response.put("status", "UNSUPPORTED");
            }
            
            String responseTopic = "response/" + ctx.getDeviceType().name().toLowerCase() + "/" + ctx.getDeviceId();
            ctx.sendJson(responseTopic, response);
            
        } catch (Exception e) {
            log.error("Failed to process command", e);
        }
    }
    
    @Override
    public void onConnected(DeviceContext ctx) {
        log.info("MQTT device connected: {}", ctx.getDeviceId());
    }
    
    @Override
    public void onDisconnected(DeviceContext ctx) {
        log.info("MQTT device disconnected: {}", ctx.getDeviceId());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/handler/
git commit -m "feat: add MQTT+JSON protocol handler"
```

---

## Chunk 4: Device Connection Management

### Task 8: Device Connection Abstraction

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/connection/DeviceConnection.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/connection/MqttDeviceConnection.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/connection/ConnectionException.java`

- [ ] **Step 1: Create connection interfaces**

```java
// DeviceConnection.java
package com.zfh.virtualdevice.device.connection;

import com.zfh.virtualdevice.enums.DeviceStatus;
import com.zfh.virtualdevice.enums.DeviceType;

public interface DeviceConnection {
    Long getDeviceId();
    DeviceType getDeviceType();
    DeviceStatus getStatus();
    
    void connect() throws ConnectionException;
    void disconnect();
    void send(byte[] data) throws ConnectionException;
    boolean isConnected();
}
```

```java
// ConnectionException.java
package com.zfh.virtualdevice.device.connection;

public class ConnectionException extends Exception {
    public ConnectionException(String message) {
        super(message);
    }
    
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: Create MQTT connection implementation**

```java
package com.zfh.virtualdevice.device.connection;

import com.zfh.virtualdevice.enums.DeviceStatus;
import com.zfh.virtualdevice.enums.DeviceType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

@Slf4j
public class MqttDeviceConnection implements DeviceConnection {
    
    private final Long deviceId;
    private final DeviceType deviceType;
    private final String broker;
    private final String clientId;
    private final String username;
    private final String password;
    
    private MqttClient client;
    private DeviceStatus status = DeviceStatus.OFFLINE;
    
    public MqttDeviceConnection(Long deviceId, DeviceType deviceType, 
                                 String broker, String clientId,
                                 String username, String password) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.broker = broker;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
    }
    
    @Override
    public Long getDeviceId() {
        return deviceId;
    }
    
    @Override
    public DeviceType getDeviceType() {
        return deviceType;
    }
    
    @Override
    public DeviceStatus getStatus() {
        return status;
    }
    
    @Override
    public void connect() throws ConnectionException {
        try {
            client = new MqttClient(broker, clientId, new MemoryPersistence());
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);
            
            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
            }
            if (password != null && !password.isEmpty()) {
                options.setPassword(password.toCharArray());
            }
            
            client.connect(options);
            status = DeviceStatus.ONLINE;
            
            log.info("MQTT device {} connected to {}", deviceId, broker);
        } catch (Exception e) {
            status = DeviceStatus.ERROR;
            throw new ConnectionException("Failed to connect to MQTT broker", e);
        }
    }
    
    @Override
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
            status = DeviceStatus.OFFLINE;
            log.info("MQTT device {} disconnected", deviceId);
        } catch (Exception e) {
            log.error("Error disconnecting MQTT client", e);
        }
    }
    
    @Override
    public void send(byte[] data) throws ConnectionException {
        try {
            if (client != null && client.isConnected()) {
                client.publish(getTopic(), data, 1, false);
            } else {
                throw new ConnectionException("MQTT client not connected");
            }
        } catch (Exception e) {
            throw new ConnectionException("Failed to publish message", e);
        }
    }
    
    public void sendJson(String topic, String payload) throws ConnectionException {
        try {
            if (client != null && client.isConnected()) {
                client.publish(topic, payload.getBytes(), 1, false);
            } else {
                throw new ConnectionException("MQTT client not connected");
            }
        } catch (Exception e) {
            throw new ConnectionException("Failed to publish JSON message", e);
        }
    }
    
    @Override
    public boolean isConnected() {
        return client != null && client.isConnected();
    }
    
    private String getTopic() {
        return "telemetry/" + deviceType.name().toLowerCase() + "/" + deviceId;
    }
    
    public MqttClient getClient() {
        return client;
    }
    
    public void setStatus(DeviceStatus status) {
        this.status = status;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/connection/
git commit -m "feat: add device connection abstraction and MQTT implementation"
```

---

### Task 9: Device Lifecycle Manager

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/DeviceLifecycleManager.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/ConnectionStatus.java`

- [ ] **Step 1: Create lifecycle manager**

```java
package com.zfh.virtualdevice.device.manager;

import com.zfh.virtualdevice.device.connection.*;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.enums.*;
import com.zfh.virtualdevice.mapper.VirtualGatewayMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DeviceLifecycleManager {
    
    @Autowired
    private VirtualGatewayMapper gatewayMapper;
    
    @Autowired
    private VirtualMeterMapper meterMapper;
    
    private final Map<String, DeviceConnection> activeConnections = new ConcurrentHashMap<>();
    
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStart() {
        log.info("Application started, recovering device connections...");
        // Recovery logic will be implemented after WebSocket is ready
    }
    
    public void startGateway(Long gatewayId) {
        VirtualGateway gateway = gatewayMapper.selectById(gatewayId);
        if (gateway == null) {
            log.error("Gateway not found: {}", gatewayId);
            return;
        }
        
        try {
            String key = "GATEWAY_" + gatewayId;
            
            if (gateway.getProtocol() == ProtocolType.MQTT) {
                MqttDeviceConnection conn = new MqttDeviceConnection(
                    gatewayId,
                    DeviceType.GATEWAY,
                    gateway.getMqttBroker(),
                    gateway.getMqttClientId(),
                    gateway.getMqttUsername(),
                    gateway.getMqttPassword()
                );
                conn.connect();
                activeConnections.put(key, conn);
                
                gateway.setStatus(DeviceStatus.ONLINE);
                gatewayMapper.updateById(gateway);
                
                log.info("Gateway {} started successfully", gatewayId);
            }
        } catch (Exception e) {
            log.error("Failed to start gateway {}", gatewayId, e);
            gateway.setStatus(DeviceStatus.ERROR);
            gatewayMapper.updateById(gateway);
        }
    }
    
    public void stopGateway(Long gatewayId) {
        String key = "GATEWAY_" + gatewayId;
        DeviceConnection conn = activeConnections.remove(key);
        if (conn != null) {
            conn.disconnect();
        }
        
        VirtualGateway gateway = new VirtualGateway();
        gateway.setId(gatewayId);
        gateway.setStatus(DeviceStatus.OFFLINE);
        gatewayMapper.updateById(gateway);
        
        log.info("Gateway {} stopped", gatewayId);
    }
    
    public void startMeter(Long meterId) {
        VirtualMeter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            log.error("Meter not found: {}", meterId);
            return;
        }
        
        try {
            String key = "METER_" + meterId;
            
            if (meter.getProtocol() == ProtocolType.MQTT) {
                // For MVP, meters connect directly via MQTT
                MqttDeviceConnection conn = new MqttDeviceConnection(
                    meterId,
                    DeviceType.METER,
                    "tcp://localhost:1883", // Default broker, should be configurable
                    "meter-" + meter.getCommunicationAddress(),
                    null,
                    null
                );
                conn.connect();
                activeConnections.put(key, conn);
                
                meter.setStatus(DeviceStatus.ONLINE);
                meterMapper.updateById(meter);
                
                log.info("Meter {} started successfully", meterId);
            }
        } catch (Exception e) {
            log.error("Failed to start meter {}", meterId, e);
            meter.setStatus(DeviceStatus.ERROR);
            meterMapper.updateById(meter);
        }
    }
    
    public void stopMeter(Long meterId) {
        String key = "METER_" + meterId;
        DeviceConnection conn = activeConnections.remove(key);
        if (conn != null) {
            conn.disconnect();
        }
        
        VirtualMeter meter = new VirtualMeter();
        meter.setId(meterId);
        meter.setStatus(DeviceStatus.OFFLINE);
        meterMapper.updateById(meter);
        
        log.info("Meter {} stopped", meterId);
    }
    
    public DeviceConnection getConnection(Long deviceId, DeviceType type) {
        String key = type.name() + "_" + deviceId;
        return activeConnections.get(key);
    }
    
    public Map<String, DeviceConnection> getActiveConnections() {
        return new ConcurrentHashMap<>(activeConnections);
    }
}
```

- [ ] **Step 2: Add start/stop endpoints to controllers**

Modify: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/VirtualGatewayController.java`

Add:
```java
@Autowired
private DeviceLifecycleManager lifecycleManager;

@PostMapping("/{id}/start")
public Result<Void> start(@PathVariable Long id) {
    lifecycleManager.startGateway(id);
    return Result.success();
}

@PostMapping("/{id}/stop")
public Result<Void> stop(@PathVariable Long id) {
    lifecycleManager.stopGateway(id);
    return Result.success();
}
```

Modify: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/VirtualMeterController.java`

Add:
```java
@Autowired
private DeviceLifecycleManager lifecycleManager;

@PostMapping("/{id}/start")
public Result<Void> start(@PathVariable Long id) {
    lifecycleManager.startMeter(id);
    return Result.success();
}

@PostMapping("/{id}/stop")
public Result<Void> stop(@PathVariable Long id) {
    lifecycleManager.stopMeter(id);
    return Result.success();
}
```

- [ ] **Step 3: Test device connection**

Start MQTT broker (using Docker):
```bash
docker run -d --name mosquitto -p 1883:1883 eclipse-mosquitto
```

Create and start a gateway:
```bash
curl -X POST http://localhost:8080/api/gateways \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MQTT网关",
    "communicationAddress": "GW002",
    "protocol": "MQTT",
    "commMode": "CLIENT",
    "mqttBroker": "tcp://localhost:1883",
    "mqttClientId": "gw-002"
  }'

curl -X POST http://localhost:8080/api/gateways/1/start
```

Check status:
```bash
curl http://localhost:8080/api/gateways/1
```
Expected: status should be ONLINE

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/
git commit -m "feat: add device lifecycle manager with MQTT connection"
```

---

## Chunk 5: Data Simulation Engine

### Task 10: Data Simulation Engine

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/simulation/DataSimulationEngine.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/simulation/DataGenerator.java`

- [ ] **Step 1: Create data generator**

```java
package com.zfh.virtualdevice.device.simulation;

import com.zfh.virtualdevice.entity.MeterDataConfig;
import com.zfh.virtualdevice.enums.DataCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class DataGenerator {
    
    private static final Random random = new Random();
    
    public static BigDecimal generateNextValue(MeterDataConfig config) {
        switch (config.getDataCategory()) {
            case ACCUMULATING:
                return generateAccumulating(config);
            case FLUCTUATING:
                return generateFluctuating(config);
            case RATIO:
                return generateRatio(config);
            default:
                return config.getCurrentValue();
        }
    }
    
    private static BigDecimal generateAccumulating(MeterDataConfig config) {
        BigDecimal current = config.getCurrentValue();
        BigDecimal min = parseParam(config, "incrementMin", BigDecimal.valueOf(0.01));
        BigDecimal max = parseParam(config, "incrementMax", BigDecimal.valueOf(0.05));
        
        BigDecimal increment = randomBetween(min, max);
        return current.add(increment).setScale(4, RoundingMode.HALF_UP);
    }
    
    private static BigDecimal generateFluctuating(MeterDataConfig config) {
        BigDecimal min = parseParam(config, "minValue", BigDecimal.valueOf(0));
        BigDecimal max = parseParam(config, "maxValue", BigDecimal.valueOf(100));
        
        return randomBetween(min, max).setScale(4, RoundingMode.HALF_UP);
    }
    
    private static BigDecimal generateRatio(MeterDataConfig config) {
        BigDecimal min = parseParam(config, "ratioMin", BigDecimal.ZERO);
        BigDecimal max = parseParam(config, "ratioMax", BigDecimal.ONE);
        
        return randomBetween(min, max).setScale(4, RoundingMode.HALF_UP);
    }
    
    private static BigDecimal randomBetween(BigDecimal min, BigDecimal max) {
        BigDecimal range = max.subtract(min);
        BigDecimal randomValue = range.multiply(BigDecimal.valueOf(random.nextDouble()));
        return min.add(randomValue);
    }
    
    private static BigDecimal parseParam(MeterDataConfig config, String key, BigDecimal defaultValue) {
        try {
            // Parse from configParams JSON
            String params = config.getConfigParams();
            if (params != null && !params.isEmpty()) {
                // Simple JSON parsing - in production use Jackson
                String searchKey = "\"" + key + "\"";
                int start = params.indexOf(searchKey);
                if (start != -1) {
                    int colon = params.indexOf(":", start);
                    int comma = params.indexOf(",", colon);
                    int end = comma != -1 ? comma : params.indexOf("}", colon);
                    String value = params.substring(colon + 1, end).trim();
                    return new BigDecimal(value);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors, use default
        }
        return defaultValue;
    }
}
```

- [ ] **Step 2: Create simulation engine**

```java
package com.zfh.virtualdevice.device.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zfh.virtualdevice.device.connection.MqttDeviceConnection;
import com.zfh.virtualdevice.device.manager.DeviceLifecycleManager;
import com.zfh.virtualdevice.entity.MeterDataConfig;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.enums.DeviceType;
import com.zfh.virtualdevice.mapper.MeterDataConfigMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import com.zfh.virtualdevice.protocol.DeviceData;
import com.zfh.virtualdevice.protocol.EncodedMessage;
import com.zfh.virtualdevice.protocol.ProtocolFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class DataSimulationEngine {
    
    @Autowired
    private VirtualMeterMapper meterMapper;
    
    @Autowired
    private MeterDataConfigMapper configMapper;
    
    @Autowired
    private DeviceLifecycleManager lifecycleManager;
    
    @Autowired
    private ProtocolFactory protocolFactory;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ScheduledExecutorService scheduler;
    private final Map<Long, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(50);
    }
    
    @PreDestroy
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }
    
    public void startAutoReport(Long meterId) {
        VirtualMeter meter = meterMapper.selectById(meterId);
        if (meter == null || !meter.getAutoReport()) {
            return;
        }
        
        int interval = meter.getReportInterval();
        
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
            () -> generateAndReport(meterId),
            interval,
            interval,
            TimeUnit.SECONDS
        );
        
        activeTasks.put(meterId, task);
        log.info("Auto report started for meter {} with interval {}s", meterId, interval);
    }
    
    public void stopAutoReport(Long meterId) {
        ScheduledFuture<?> task = activeTasks.remove(meterId);
        if (task != null) {
            task.cancel(false);
            log.info("Auto report stopped for meter {}", meterId);
        }
    }
    
    private void generateAndReport(Long meterId) {
        try {
            VirtualMeter meter = meterMapper.selectById(meterId);
            if (meter == null || meter.getStatus() != com.zfh.virtualdevice.enums.DeviceStatus.ONLINE) {
                return;
            }
            
            List<MeterDataConfig> configs = configMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeterDataConfig>()
                    .eq(MeterDataConfig::getMeterId, meterId)
            );
            
            if (configs.isEmpty()) {
                // Create default configs if none exist
                createDefaultConfigs(meter);
                configs = configMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeterDataConfig>()
                        .eq(MeterDataConfig::getMeterId, meterId)
                );
            }
            
            Map<String, Object> data = new HashMap<>();
            Map<String, String> units = new HashMap<>();
            
            for (MeterDataConfig config : configs) {
                BigDecimal newValue = DataGenerator.generateNextValue(config);
                config.setCurrentValue(newValue);
                configMapper.updateById(config);
                
                data.put(config.getDataType(), newValue);
                units.put(config.getDataType(), getUnit(config));
            }
            
            DeviceData deviceData = DeviceData.builder()
                .deviceId(meterId)
                .deviceAddress(meter.getCommunicationAddress())
                .deviceType(DeviceType.METER)
                .timestamp(LocalDateTime.now())
                .data(data)
                .units(units)
                .build();
            
            // Get connection and send
            var connection = lifecycleManager.getConnection(meterId, DeviceType.METER);
            if (connection instanceof MqttDeviceConnection) {
                MqttDeviceConnection mqttConn = (MqttDeviceConnection) connection;
                var handler = protocolFactory.getHandler(meter.getProtocol());
                EncodedMessage message = handler.encode(deviceData, null);
                mqttConn.sendJson(message.getMqttTopic(), message.getMqttPayload());
            }
            
        } catch (Exception e) {
            log.error("Failed to generate and report data for meter {}", meterId, e);
        }
    }
    
    private void createDefaultConfigs(VirtualMeter meter) {
        if (meter.getMeterType() == com.zfh.virtualdevice.enums.MeterType.ELECTRIC) {
            createConfig(meter.getId(), "total_energy", com.zfh.virtualdevice.enums.DataCategory.ACCUMULATING, 
                        "kWh", "{\"initialValue\":0,\"incrementMin\":0.01,\"incrementMax\":0.05}");
            createConfig(meter.getId(), "voltage", com.zfh.virtualdevice.enums.DataCategory.FLUCTUATING, 
                        "V", "{\"minValue\":210,\"maxValue\":240,\"fluctuationType\":\"RANDOM\"}");
            createConfig(meter.getId(), "current", com.zfh.virtualdevice.enums.DataCategory.FLUCTUATING, 
                        "A", "{\"minValue\":0,\"maxValue\":60,\"fluctuationType\":\"RANDOM\"}");
            createConfig(meter.getId(), "power_factor", com.zfh.virtualdevice.enums.DataCategory.RATIO, 
                        "", "{\"ratioMin\":0.85,\"ratioMax\":0.95}");
        } else if (meter.getMeterType() == com.zfh.virtualdevice.enums.MeterType.WATER) {
            createConfig(meter.getId(), "total_water", com.zfh.virtualdevice.enums.DataCategory.ACCUMULATING, 
                        "m³", "{\"initialValue\":0,\"incrementMin\":0.001,\"incrementMax\":0.01}");
            createConfig(meter.getId(), "flow_rate", com.zfh.virtualdevice.enums.DataCategory.FLUCTUATING, 
                        "m³/h", "{\"minValue\":0,\"maxValue\":10,\"fluctuationType\":\"RANDOM\"}");
        }
    }
    
    private void createConfig(Long meterId, String dataType, com.zfh.virtualdevice.enums.DataCategory category, 
                             String unit, String params) {
        MeterDataConfig config = new MeterDataConfig();
        config.setMeterId(meterId);
        config.setDataType(dataType);
        config.setDataCategory(category);
        config.setCurrentValue(BigDecimal.ZERO);
        config.setConfigParams(params);
        configMapper.insert(config);
    }
    
    private String getUnit(MeterDataConfig config) {
        // Parse unit from configParams
        try {
            String params = config.getConfigParams();
            if (params != null) {
                int start = params.indexOf("\"unit\"");
                if (start != -1) {
                    int quote = params.indexOf("\"", params.indexOf(":", start) + 1);
                    int end = params.indexOf("\"", quote + 1);
                    return params.substring(quote + 1, end);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }
}
```

- [ ] **Step 3: Integrate simulation with lifecycle**

Modify: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/DeviceLifecycleManager.java`

Add:
```java
@Autowired
private DataSimulationEngine simulationEngine;
```

In `startMeter()`, after successful connection:
```java
simulationEngine.startAutoReport(meterId);
```

In `stopMeter()`, before disconnect:
```java
simulationEngine.stopAutoReport(meterId);
```

- [ ] **Step 4: Test data simulation**

Create a meter, start it, and verify data is published:
```bash
# Create meter
curl -X POST http://localhost:8080/api/meters \
  -H "Content-Type: application/json" \
  -d '{
    "name": "模拟电表",
    "meterType": "ELECTRIC",
    "communicationAddress": "METER002",
    "protocol": "MQTT",
    "autoReport": true,
    "reportInterval": 5
  }'

# Start meter
curl -X POST http://localhost:8080/api/meters/1/start

# Wait 10 seconds and check MQTT messages (using mosquitto_sub)
mosquitto_sub -h localhost -t "telemetry/meter/1" -v
```

Expected: JSON messages published every 5 seconds with voltage, current, etc.

- [ ] **Step 5: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/simulation/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/
git commit -m "feat: add data simulation engine with auto-report"
```

---

## Chunk 6: Communication Logging & WebSocket

### Task 11: Communication Logging

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/CommunicationLogService.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/CommunicationLogController.java`

- [ ] **Step 1: Create log service**

```java
package com.zfh.virtualdevice.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.virtualdevice.entity.CommunicationLog;
import com.zfh.virtualdevice.mapper.CommunicationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommunicationLogService extends ServiceImpl<CommunicationLogMapper, CommunicationLog> {
    
    @Async
    public void logAsync(CommunicationLog commLog) {
        try {
            save(commLog);
        } catch (Exception e) {
            log.error("Failed to save communication log", e);
        }
    }
}
```

- [ ] **Step 2: Create log controller**

```java
package com.zfh.virtualdevice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.entity.CommunicationLog;
import com.zfh.virtualdevice.service.CommunicationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class CommunicationLogController {
    
    @Autowired
    private CommunicationLogService logService;
    
    @GetMapping
    public Result<Page<CommunicationLog>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String protocol) {
        
        Page<CommunicationLog> page = new Page<>(current, size);
        var query = logService.lambdaQuery();
        
        if (deviceType != null) {
            query.eq(CommunicationLog::getDeviceType, deviceType);
        }
        if (deviceId != null) {
            query.eq(CommunicationLog::getDeviceId, deviceId);
        }
        if (direction != null) {
            query.eq(CommunicationLog::getDirection, direction);
        }
        if (protocol != null) {
            query.eq(CommunicationLog::getProtocol, protocol);
        }
        
        query.orderByDesc(CommunicationLog::getTimestamp);
        return Result.success(logService.page(page, query.getWrapper()));
    }
    
    @DeleteMapping
    public Result<Void> clear() {
        logService.remove(null);
        return Result.success();
    }
}
```

- [ ] **Step 3: Add logging to simulation engine**

Modify: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/simulation/DataSimulationEngine.java`

Add:
```java
@Autowired
private CommunicationLogService logService;
```

In `generateAndReport()`, after sending:
```java
// Log the communication
CommunicationLog commLog = new CommunicationLog();
commLog.setDeviceType(DeviceType.METER);
commLog.setDeviceId(meterId);
commLog.setDirection(Direction.UP);
commLog.setProtocol("MQTT");
commLog.setRawData(message.getMqttPayload());
commLog.setParsedData(objectMapper.writeValueAsString(data));
commLog.setTimestamp(LocalDateTime.now());
logService.logAsync(commLog);
```

- [ ] **Step 4: Test logging**

```bash
curl "http://localhost:8080/api/logs?deviceId=1"
```

Expected: List of communication logs with UP direction

- [ ] **Step 5: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/service/CommunicationLogService.java
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/CommunicationLogController.java
git commit -m "feat: add communication logging"
```

---

### Task 12: WebSocket Real-time Updates

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/websocket/WebSocketConfig.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/websocket/DeviceWebSocketController.java`

- [ ] **Step 1: Configure WebSocket**

```java
package com.zfh.virtualdevice.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/virtual-device")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

- [ ] **Step 2: Create WebSocket controller**

```java
package com.zfh.virtualdevice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class DeviceWebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void sendDeviceStatus(Long deviceId, String deviceType, String status) {
        Map<String, Object> message = Map.of(
            "type", "DEVICE_STATUS",
            "deviceType", deviceType,
            "deviceId", deviceId,
            "status", status,
            "timestamp", java.time.LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/device-status", message);
    }
    
    public void sendCommLog(Long logId, Long deviceId, String deviceName, 
                           String direction, String protocol, String rawData, String parsedData) {
        Map<String, Object> message = Map.of(
            "type", "COMM_LOG",
            "logId", logId,
            "deviceType", "METER",
            "deviceId", deviceId,
            "deviceName", deviceName,
            "direction", direction,
            "protocol", protocol,
            "rawData", rawData,
            "parsedData", parsedData,
            "timestamp", java.time.LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/comm-logs", message);
    }
    
    public void sendStats(int onlineGateways, int totalGateways, 
                         int onlineMeters, int totalMeters,
                         long todayUpMessages, long todayDownMessages) {
        Map<String, Object> message = Map.of(
            "type", "STATS_UPDATE",
            "onlineGateways", onlineGateways,
            "totalGateways", totalGateways,
            "onlineMeters", onlineMeters,
            "totalMeters", totalMeters,
            "todayUpMessages", todayUpMessages,
            "todayDownMessages", todayDownMessages
        );
        messagingTemplate.convertAndSend("/topic/stats", message);
    }
}
```

- [ ] **Step 3: Integrate WebSocket with lifecycle**

Modify: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/DeviceLifecycleManager.java`

Add:
```java
@Autowired
private DeviceWebSocketController webSocketController;
```

In `startGateway()`, after status update:
```java
webSocketController.sendDeviceStatus(gatewayId, "GATEWAY", DeviceStatus.ONLINE.name());
```

In `stopGateway()`, after status update:
```java
webSocketController.sendDeviceStatus(gatewayId, "GATEWAY", DeviceStatus.OFFLINE.name());
```

In `startMeter()`, after status update:
```java
webSocketController.sendDeviceStatus(meterId, "METER", DeviceStatus.ONLINE.name());
```

In `stopMeter()`, after status update:
```java
webSocketController.sendDeviceStatus(meterId, "METER", DeviceStatus.OFFLINE.name());
```

- [ ] **Step 4: Test WebSocket**

Use browser console or wscat:
```bash
npm install -g wscat
wscat -c ws://localhost:8080/ws/virtual-device
> {"command":"subscribe","destination":"/topic/device-status"}
```

Start a device and verify message received.

- [ ] **Step 5: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/websocket/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/
git commit -m "feat: add WebSocket real-time updates"
```

---

## Chunk 7: Authentication & Security

### Task 13: JWT Authentication

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/security/JwtUtils.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/security/JwtAuthenticationFilter.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/security/SecurityConfig.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/AuthController.java`

- [ ] **Step 1: Create JWT utilities**

```java
package com.zfh.virtualdevice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Create authentication filter**

```java
package com.zfh.virtualdevice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtils.validateToken(token)) {
                String username = jwtUtils.getUsernameFromToken(token);
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Create security config**

```java
package com.zfh.virtualdevice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/api/auth/**", "/h2-console/**", "/ws/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .headers().frameOptions().sameOrigin()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 4: Create auth controller**

```java
package com.zfh.virtualdevice.controller;

import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // In-memory user for MVP (should use database in production)
    private final String adminUsername = "admin";
    private final String adminPassword; // Will be set in constructor
    
    public AuthController() {
        this.adminPassword = new BCryptPasswordEncoder().encode("admin123");
    }
    
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        if (!adminUsername.equals(username) || !passwordEncoder.matches(password, adminPassword)) {
            return Result.error(401, "用户名或密码错误");
        }
        
        String token = jwtUtils.generateToken(username);
        String refreshToken = jwtUtils.generateRefreshToken(username);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("expiresIn", 3600);
        
        return Result.success(result);
    }
    
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        if (!jwtUtils.validateToken(refreshToken)) {
            return Result.error(401, "无效的刷新令牌");
        }
        
        String username = jwtUtils.getUsernameFromToken(refreshToken);
        String newToken = jwtUtils.generateToken(username);
        String newRefreshToken = jwtUtils.generateRefreshToken(username);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("refreshToken", newRefreshToken);
        result.put("expiresIn", 3600);
        
        return Result.success(result);
    }
    
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", adminUsername);
        profile.put("roles", new String[]{"ADMIN"});
        return Result.success(profile);
    }
}
```

- [ ] **Step 5: Add JWT dependency to POM**

Modify: `zfh-virtual-device-backend/pom.xml`

Add to dependencies:
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 6: Test authentication**

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Test protected endpoint without token
curl http://localhost:8080/api/gateways
# Expected: 403 Forbidden

# Test with token
curl http://localhost:8080/api/gateways \
  -H "Authorization: Bearer {token_from_login}"
# Expected: 200 OK with data
```

- [ ] **Step 7: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/security/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/AuthController.java
git add zfh-virtual-device-backend/pom.xml
git commit -m "feat: add JWT authentication and security"
```

---

## Chunk 8: Final Integration & Testing

### Task 14: Integration Testing

- [ ] **Step 1: Run full application**

```bash
cd zfh-virtual-device-backend
mvn clean package -DskipTests
java -jar target/zfh-virtual-device-backend-1.0.0.jar
```

- [ ] **Step 2: End-to-end test**

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

# 2. Create gateway
curl -X POST http://localhost:8080/api/gateways \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "测试网关",
    "communicationAddress": "GW001",
    "protocol": "MQTT",
    "commMode": "CLIENT",
    "mqttBroker": "tcp://localhost:1883",
    "mqttClientId": "gw-001"
  }'

# 3. Create meter
curl -X POST http://localhost:8080/api/meters \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "测试电表",
    "meterType": "ELECTRIC",
    "communicationAddress": "METER001",
    "protocol": "MQTT",
    "autoReport": true,
    "reportInterval": 5
  }'

# 4. Start meter
curl -X POST http://localhost:8080/api/meters/1/start \
  -H "Authorization: Bearer $TOKEN"

# 5. Check logs after 10 seconds
curl "http://localhost:8080/api/logs?deviceId=1" \
  -H "Authorization: Bearer $TOKEN"
```

- [ ] **Step 3: Commit final version**

```bash
git add .
git commit -m "feat: complete backend MVP implementation"
```

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-18-virtual-device-backend.md`. Ready to execute?**

**Next Steps:**
1. Review and approve this plan
2. Execute using subagent-driven-development or executing-plans skill
3. Then create frontend implementation plan
