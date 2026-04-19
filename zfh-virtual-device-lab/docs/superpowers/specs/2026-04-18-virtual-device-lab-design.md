# zfh-virtual-device-lab 产品需求与设计文档

**版本**: v1.0  
**日期**: 2026-04-18  
**作者**: AI Assistant  
**状态**: 待审查

---

## 1. 项目概述

### 1.1 背景

zfh-iot 物联网平台需要一套测试用的虚拟设备系统，用于模拟真实场景下的网关和表计设备，支持平台的功能测试、协议对接测试和压力测试。

### 1.2 目标

开发一个独立的虚拟设备实验室（zfh-virtual-device-lab），提供：
- 虚拟网关（TCP直连/MQTT）
- 虚拟表计（电表/水表/热表/气表，支持直连或通过网关连接）
- Web页面维护虚拟设备信息
- 实时数据模拟与上报
- 双向通讯（上行上报 + 下行指令处理）
- 通讯日志记录与查看

### 1.3 使用场景

- 开发与测试阶段：模拟真实设备与 zfh-iot 平台对接
- 协议验证：验证平台对不同协议的解析和处理能力
- 压力测试：模拟大量设备并发连接和数据上报
- 问题复现：通过虚拟设备复现现场问题

---

## 2. 技术栈

### 2.1 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 基础框架 |
| Java | 17 | 开发语言 |
| Netty | 4.1.x | TCP服务端/客户端 |
| Eclipse Paho | 1.2.x | MQTT客户端 |
| H2 Database | 2.2.x | 嵌入式数据库 |
| MyBatis Plus | 3.5.5 | ORM框架 |
| WebSocket (STOMP) | - | 实时推送 |
| Lombok | - | 代码简化 |

### 2.2 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.4.x | 前端框架 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI组件库 |
| WebSocket Client | - | 实时通讯 |

### 2.3 项目结构

```
zfh-virtual-device-lab/
├── zfh-virtual-device-backend/     # Spring Boot后端
│   ├── src/main/java/
│   │   └── com/zfh/virtualdevice/
│   │       ├── controller/         # REST API控制器
│   │       ├── service/            # 业务逻辑层
│   │       ├── entity/             # 数据实体
│   │       ├── mapper/             # MyBatis映射器
│   │       ├── protocol/           # 协议处理模块
│   │       │   ├── handler/        # 协议处理器
│   │       │   ├── factory/        # 协议工厂
│   │       │   └── codec/          # 编解码器
│   │       ├── device/             # 设备管理模块
│   │       │   ├── manager/        # 生命周期管理
│   │       │   ├── connection/     # 网络连接
│   │       │   └── simulation/     # 数据模拟引擎
│   │       ├── websocket/          # WebSocket配置
│   │       └── config/             # 配置类
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/                     # 数据库初始化脚本
│   └── pom.xml
├── zfh-virtual-device-frontend/    # Vue前端
│   ├── src/
│   │   ├── views/                  # 页面组件
│   │   │   ├── gateway/            # 网关管理
│   │   │   ├── meter/              # 表计管理
│   │   │   ├── template/           # 配置模板
│   │   │   ├── logs/               # 通讯日志
│   │   │   └── dashboard/          # 实时监控
│   │   ├── components/             # 公共组件
│   │   ├── api/                    # API接口封装
│   │   ├── stores/                 # Pinia状态管理
│   │   └── utils/                  # 工具函数
│   ├── package.json
│   └── vite.config.js
└── docs/                           # 文档
```

---

## 3. 数据模型设计

### 3.1 虚拟网关（VirtualGateway）

**约束说明：** `communication_address` 在全局范围内唯一（网关和表计不可重复）。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键，自增 |
| name | VARCHAR(100) | 是 | 网关名称 |
| communication_address | VARCHAR(50) | 是 | 通讯地址（唯一标识） |
| protocol | VARCHAR(20) | 是 | 通讯协议：TCP_DIRECT / MQTT |
| comm_mode | VARCHAR(20) | 是 | 通讯模式：SERVER（服务端）/ CLIENT（客户端） |
| server_port | INT | 条件 | 服务端模式：侦听端口 |
| client_host | VARCHAR(50) | 条件 | 客户端模式：服务器IP |
| client_port | INT | 条件 | 客户端模式：服务器端口 |
| status | VARCHAR(20) | 是 | 运行状态：OFFLINE / ONLINE / ERROR |
| mqtt_broker | VARCHAR(100) | 条件 | MQTT模式：Broker地址 |
| mqtt_client_id | VARCHAR(50) | 条件 | MQTT模式：Client ID |
| mqtt_username | VARCHAR(50) | 否 | MQTT认证用户名 |
| mqtt_password | VARCHAR(100) | 否 | MQTT认证密码 |
| created_at | TIMESTAMP | 是 | 创建时间 |
| updated_at | TIMESTAMP | 是 | 更新时间 |

**约束：**
- `comm_mode = SERVER` 时，`server_port` 必填
- `comm_mode = CLIENT` 时，`client_host` 和 `client_port` 必填
- `protocol = MQTT` 时，`mqtt_broker` 和 `mqtt_client_id` 必填

### 3.2 虚拟表计（VirtualMeter）

**MVP阶段说明：** MVP仅支持电表（ELECTRIC）和水表（WATER），仅支持直连模式（DIRECT）。热表/气表和网关模式在数据库中预留字段，但后端暂不实现相关业务逻辑。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键，自增 |
| gateway_id | BIGINT | FK | 所属网关ID（MVP必须为NULL，直连模式） |
| name | VARCHAR(100) | 是 | 表计名称 |
| meter_type | VARCHAR(20) | 是 | 类型：ELECTRIC / WATER / HEAT / GAS（MVP仅支持ELECTRIC/WATER） |
| communication_address | VARCHAR(50) | 是 | 通讯地址 |
| protocol | VARCHAR(20) | 是 | 通讯协议：TCP_DIRECT / MQTT（MVP仅支持MQTT） |
| connection_mode | VARCHAR(20) | 是 | 连接方式：DIRECT / GATEWAY（MVP仅支持DIRECT） |
| status | VARCHAR(20) | 是 | 运行状态 |
| auto_report | BOOLEAN | 是 | 是否自动上报，默认true |
| report_interval | INT | 是 | 自动上报间隔（秒），默认30 |
| created_at | TIMESTAMP | 是 | 创建时间 |
| updated_at | TIMESTAMP | 是 | 更新时间 |

**约束：**
- `connection_mode = GATEWAY` 时，`gateway_id` 必填（MVP不支持）
- `connection_mode = DIRECT` 时，`gateway_id` 必须为NULL
- `communication_address` 全局唯一（与网关不可重复）

**级联删除规则：**
- 删除网关时，自动删除其下属的所有表计（级联删除）（MVP不涉及，因为MVP无网关模式）
- 删除网关前，必须先停止其连接

### 3.3 数据配置模板（MeterDataTemplate）

**删除规则：** 删除模板时，已应用该模板的表计数据配置保留，但 `template_id` 设为NULL（解引用而非级联删除）。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键，自增 |
| template_name | VARCHAR(100) | 是 | 模板名称 |
| meter_type | VARCHAR(20) | 是 | 适用设备类型 |
| protocol | VARCHAR(20) | 否 | 适用协议（NULL表示通配） |
| description | VARCHAR(500) | 否 | 模板描述 |
| created_at | TIMESTAMP | 是 | 创建时间 |
| updated_at | TIMESTAMP | 是 | 更新时间 |

### 3.4 模板数据项（TemplateDataItem）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键，自增 |
| template_id | BIGINT | FK | 所属模板ID |
| data_type | VARCHAR(50) | 是 | 数据项标识：total_energy / voltage / current / power / power_factor / total_water / flow_rate 等 |
| data_category | VARCHAR(20) | 是 | 数据分类：ACCUMULATING（累积量）/ FLUCTUATING（波动量）/ RATIO（比值量） |
| initial_value | DECIMAL(18,4) | 否 | 累积量：初始值 |
| increment_min | DECIMAL(18,4) | 否 | 累积量：每次最小增长量 |
| increment_max | DECIMAL(18,4) | 否 | 累积量：每次最大增长量 |
| min_value | DECIMAL(18,4) | 否 | 波动量：最小值 |
| max_value | DECIMAL(18,4) | 否 | 波动量：最大值 |
| fluctuation_type | VARCHAR(20) | 否 | 波动方式：RANDOM（随机）/ SINE（正弦波）/ STEP（阶梯） |
| ratio_min | DECIMAL(18,4) | 否 | 比值量：最小比值 |
| ratio_max | DECIMAL(18,4) | 否 | 比值量：最大比值 |
| unit | VARCHAR(20) | 否 | 单位：kWh / V / A / W / m³ 等 |
| sort_order | INT | 是 | 排序，默认0 |

### 3.5 表计数据配置（MeterDataConfig）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键，自增 |
| meter_id | BIGINT | FK | 表计ID |
| template_id | BIGINT | FK | 引用的模板ID（可选） |
| data_type | VARCHAR(50) | 是 | 数据项标识 |
| data_category | VARCHAR(20) | 是 | 数据分类 |
| current_value | DECIMAL(18,4) | 是 | 当前值（运行时） |
| config_params | JSON | 否 | 完整参数配置（JSON格式，见下方Schema） |
| override_params | JSON | 否 | 覆盖参数（JSON格式，结构与config_params相同，仅包含需要覆盖的字段） |
| created_at | TIMESTAMP | 是 | 创建时间 |
| updated_at | TIMESTAMP | 是 | 更新时间 |

**config_params JSON Schema：**
```json
{
  "initial_value": 100.0000,        // 累积量：初始值
  "increment_min": 0.0100,          // 累积量：每次最小增长量
  "increment_max": 0.0500,          // 累积量：每次最大增长量
  "min_value": 210.0000,            // 波动量：最小值
  "max_value": 240.0000,            // 波动量：最大值
  "fluctuation_type": "RANDOM",     // 波动方式：RANDOM / SINE / STEP
  "ratio_min": 0.0000,              // 比值量：最小比值
  "ratio_max": 1.0000,              // 比值量：最大比值
  "unit": "kWh"                     // 单位
}
```

**注意：** `config_params` 存储完整的参数配置。当表计引用模板时，`config_params` 初始为模板参数副本，`override_params` 用于存储对该表计的个性化调整（仅包含差异字段）。运行时优先使用 `override_params` 中的值，不存在时使用 `config_params` 中的值。

### 3.6 通讯日志（CommunicationLog）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键，自增 |
| device_type | VARCHAR(20) | 是 | 设备类型：GATEWAY / METER |
| device_id | BIGINT | 是 | 设备ID |
| direction | VARCHAR(10) | 是 | 方向：UP（上行）/ DOWN（下行） |
| protocol | VARCHAR(20) | 是 | 协议类型：TCP / MQTT / DLT645 / GB3761 等 |
| raw_data | TEXT | 是 | 原始报文（Hex编码） |
| parsed_data | TEXT | 否 | 解析后的数据（JSON格式） |
| timestamp | TIMESTAMP | 是 | 时间戳 |

### 3.7 实体关系

```
VirtualGateway (1) ──────< (N) VirtualMeter
    │                           │
    │                           │
    └── CommunicationLog <──────┘

MeterDataTemplate (1) ───< (N) TemplateDataItem
    │
    └── (N) MeterDataConfig (通过 template_id 关联)
```

---

## 4. 通讯层与应用层协议设计

### 4.1 传输层实现

#### 4.1.1 TCP直连

基于 Netty 4.x 实现。TCP作为传输层，承载应用层协议数据。

**MVP阶段说明：** TCP直连和MQTT是两种不同的传输层模式。MVP阶段优先实现MQTT+JSON协议（使用Eclipse Paho连接MQTT Broker）。TCP直连模式在MVP中搭建基础框架（Netty Server/Client），但应用层协议仅支持MQTT+JSON（MQTT本身基于TCP，但这里是直连TCP通道承载JSON报文，不同于MQTT协议）。后续阶段将在TCP直连上实现DL/T 645等二进制协议。

- **服务端模式（Server）**：
  - `ServerBootstrap` 绑定指定端口
  - `ChannelInitializer` 配置编解码器和业务处理器
  - 等待 zfh-iot 平台连接
  - 一个网关对应一个 `ServerChannel`

- **客户端模式（Client）**：
  - `Bootstrap` 主动连接 zfh-iot 平台地址
  - 连接断开后支持自动重连（指数退避策略）
  - 一个网关/表计对应一个 `Channel`

**连接管理：**
- 使用 `ChannelGroup` 管理所有活跃连接
- 连接建立/断开时更新设备状态并推送WebSocket通知

#### 4.1.2 MQTT

基于 Eclipse Paho Java Client：

- 每个设备独立创建 `MqttClient` 实例
- 配置参数：Broker地址、Client ID、用户名/密码（可选）
- 连接断开后自动重连

**Topic约定：**
- 上行：`telemetry/{deviceType}/{deviceId}`
- 下行：`command/{deviceType}/{deviceId}`
- 响应：`response/{deviceType}/{deviceId}`

**MQTT Payload JSON格式（MVP）：**

```json
// 上行报文（表计上报数据）
{
  "deviceId": 1,
  "deviceAddress": "METER001",
  "timestamp": "2026-04-18T10:30:00",
  "data": {
    "total_energy": 1523.4567,
    "voltage": 220.5,
    "current": 5.2,
    "power_factor": 0.92
  },
  "units": {
    "total_energy": "kWh",
    "voltage": "V",
    "current": "A",
    "power_factor": ""
  }
}

// 下行指令（平台下发）
{
  "commandType": "READ_DATA",
  "dataId": "total_energy",
  "timestamp": "2026-04-18T10:30:00",
  "params": {}
}

// 响应报文
{
  "deviceId": 1,
  "commandType": "READ_DATA",
  "status": "SUCCESS",
  "data": {
    "total_energy": 1523.4567
  },
  "timestamp": "2026-04-18T10:30:01"
}
```

### 4.2 应用层协议适配框架

采用**策略模式**实现协议可扩展。

#### 4.2.1 核心数据契约

```java
// 设备上下文：包含设备运行时状态和连接信息
@Data
public class DeviceContext {
    private Long deviceId;                    // 设备ID
    private DeviceType deviceType;            // 设备类型：GATEWAY / METER
    private String communicationAddress;      // 通讯地址
    private ProtocolType protocolType;        // 协议类型
    private Channel nettyChannel;             // Netty连接通道（TCP模式）
    private MqttClient mqttClient;            // MQTT客户端（MQTT模式）
    private Map<String, Object> attributes;   // 扩展属性（协议特定状态）
    
    // 发送二进制数据到平台（TCP模式）
    public void send(byte[] data) { ... }
    
    // 发送JSON数据到平台（MQTT模式）
    public void sendJson(String topic, Object payload) { ... }
    
    // 获取表计当前数据值（仅表计类型设备有效）
    public BigDecimal getMeterData(String dataType) { ... }
    
    // 更新设备参数
    public void updateParams(Map<String, Object> params) { ... }
}

// 设备上行数据
@Data
public class DeviceData {
    private Long deviceId;                    // 设备ID
    private String deviceAddress;             // 设备通讯地址
    private DeviceType deviceType;            // 设备类型
    private LocalDateTime timestamp;          // 时间戳
    private Map<String, Object> data;         // 数据项集合（key=data_type, value=当前值）
    private Map<String, String> units;        // 单位映射
}

// 平台下行指令
@Data
public class Command {
    private CommandType type;                 // 指令类型：READ_DATA / SET_PARAMS / REMOTE_CONTROL / TIME_SYNC
    private String dataId;                    // 数据标识（如DL/T 645的数据标识DI）
    private Map<String, Object> params;       // 指令参数
    private LocalDateTime timestamp;          // 指令时间戳
    private String rawFrame;                  // 原始报文帧（Hex）
}

// 指令类型枚举
public enum CommandType {
    READ_DATA,        // 读取数据
    SET_PARAMS,       // 设置参数
    REMOTE_CONTROL,   // 远程控制（电表：拉闸/合闸）
    TIME_SYNC,        // 广播校时
    UNKNOWN           // 未知指令
}
```

#### 4.2.2 协议处理器接口

```java
public interface ProtocolHandler {
    ProtocolType getProtocolType();
    
    // 编码上行数据为平台报文
    // 返回值：EncodedMessage（包含二进制数据或MQTT消息）
    EncodedMessage encode(DeviceData data, DeviceContext ctx);
    
    // 解析下行指令
    // 输入：DecodedMessage（从TCP字节流或MQTT消息解析）
    Command parseCommand(DecodedMessage message, DeviceContext ctx);
    
    // 处理下行指令
    void processCommand(DeviceContext ctx, Command cmd);
    
    // 初始化连接（协议特定的握手逻辑）
    void onConnected(DeviceContext ctx);
    
    // 连接断开清理
    void onDisconnected(DeviceContext ctx);
}

// 编码后的消息（支持二进制和MQTT两种模式）
@Data
public class EncodedMessage {
    private MessageType type;         // BINARY / MQTT
    private byte[] binaryData;        // TCP二进制报文
    private String mqttTopic;         // MQTT Topic
    private String mqttPayload;       // MQTT Payload（JSON字符串）
    private int mqttQos;              // MQTT QoS等级
}

// 解码前的消息
@Data
public class DecodedMessage {
    private MessageType type;         // BINARY / MQTT
    private byte[] binaryData;        // TCP原始字节
    private String mqttTopic;         // MQTT Topic
    private String mqttPayload;       // MQTT Payload（JSON字符串）
}
```

#### 4.2.3 协议工厂

```java
@Component
public class ProtocolFactory {
    private final Map<ProtocolType, ProtocolHandler> handlers = new HashMap<>();
    
    public ProtocolHandler getHandler(ProtocolType type) {
        return handlers.get(type);
    }
}
```

### 4.3 支持的应用层协议

| 协议 | 类型 | 说明 | 优先级 |
|------|------|------|--------|
| MQTT+JSON | 通用 | MQTT传输JSON格式数据 | **P0（MVP实现）** |
| DL/T 645-2007 | 电表 | 多功能电能表通信协议 | P1（第二阶段） |
| 国网376.1 | 采集终端 | 电力用户用电信息采集系统通信协议 | P1（第二阶段） |
| DL/T 698.45 | 物联网 | 面向对象的用电信息采集数据交换协议 | P2（第三阶段） |
| Modbus RTU/TCP | 通用 | 工业标准协议 | P2（第三阶段） |

### 4.4 下行指令处理

**指令类型：**
- **读取数据**：平台请求设备上报当前数据（所有设备类型通用）
- **设置参数**：平台设置设备参数（如上报间隔、时间等，所有设备类型通用）
- **远程控制**：平台下发控制指令
  - **电表**：远程拉闸/合闸（仅ELECTRIC类型支持）
  - **水表/热表/气表**：阀门控制（预留，后续实现）
- **广播校时**：平台统一校准设备时间（所有设备类型通用）

**处理流程：**
1. 平台下发指令 → 虚拟设备接收原始报文
2. `ProtocolHandler.parseCommand()` 解析指令
3. `ProtocolHandler.processCommand()` 执行业务逻辑
4. 生成响应报文 → 返回给平台
5. 记录通讯日志（DOWN + UP）

**错误处理：**
- 解析失败：返回协议特定的错误帧（如DL/T 645的异常响应）
- 不支持的数据标识：返回"非法数据标识"错误
- 设备类型不匹配：返回"设备不支持此操作"错误（如非电表收到拉闸指令）

---

## 5. Web 页面功能设计

### 5.1 页面模块

| 模块 | 路由 | 功能 |
|------|------|------|
| 网关管理 | `/gateways` | 虚拟网关的增删改查、连接控制、状态监控 |
| 表计管理 | `/meters` | 虚拟表计的增删改查、所属网关配置、连接控制 |
| 配置模板 | `/templates` | 数据配置模板的创建、编辑、管理 |
| 通讯日志 | `/logs` | 实时查看设备收发报文、筛选查询、导出 |
| 实时监控 | `/dashboard` | 仪表盘展示设备状态、报文统计、拓扑图 |

### 5.2 网关管理页面

**列表视图：**
- 卡片式/表格双模式切换
- 每行/卡片显示：名称、通讯地址、协议、通讯模式、端口信息、状态
- 状态颜色标识：灰色（离线）、绿色（在线）、红色（异常）
- 操作按钮：编辑、删除、启动/停止、查看日志

**创建/编辑表单：**
- 基本信息：名称、通讯地址
- 协议选择：TCP直连 / MQTT（单选）
- 通讯模式：服务端 / 客户端（单选）
  - 服务端：填写侦听端口
  - 客户端：填写服务器IP和端口
- MQTT配置（当协议为MQTT时显示）：Broker地址、Client ID、认证信息

**批量操作：**
- 批量启动/停止
- 批量删除

### 5.3 表计管理页面

**列表视图：**
- 类似网关管理的卡片/表格双模式
- 显示：名称、类型、通讯地址、协议、连接方式、所属网关、状态
- 操作按钮：编辑、删除、启动/停止、配置数据、查看日志

**创建/编辑表单：**
- 基本信息：名称、表计类型（电表/水表/热表/气表）
- 连接方式：直连 / 通过网关
  - 通过网关：选择所属网关（下拉选择已创建的网关）
- 通讯地址、协议
- 上报设置：是否自动上报、上报间隔

**数据配置：**
- 单个配置：为当前表计选择模板或手动配置
- 批量配置：选择多个表计 → 应用模板 → 选择模板 → 确认

### 5.4 配置模板页面 [P1 - 第二阶段]

**模板列表：**
- 显示：模板名称、适用设备类型、适用协议、数据项数量
- 操作：编辑、删除、复制、应用

**模板编辑：**
- 基本信息：名称、适用设备类型、适用协议、描述
- 数据项列表：
  - 添加数据项按钮
  - 每行：数据类型、数据分类、参数配置（根据分类动态显示字段）
  - 操作：编辑、删除、排序

**应用模板弹窗：**
- 左侧：设备筛选（按类型、协议、状态）
- 右侧：已选择的设备列表
- 底部：选择模板 → 确认应用

### 5.5 通讯日志页面

**布局：**
- 左侧：设备树（网关 → 表计层级）
- 右侧：报文列表

**报文列表：**
- 时间轴式展示
- 每行：时间戳、设备名称、方向图标（↑绿色/↓蓝色）、协议、原始报文、解析结果
- 点击展开查看详情（Hex和ASCII切换）
- 自动滚动开关
- 清空日志按钮

**筛选条件：**
- 时间范围
- 设备类型和ID
- 方向（上行/下行）
- 协议类型

**导出功能：**
- 导出当前筛选结果为 CSV/Excel

### 5.6 实时监控页面 [P1 - 第二阶段]

**统计卡片：**
- 在线网关数 / 总网关数
- 在线表计数 / 总表计数
- 今日上行报文数
- 今日下行报文数

**设备状态拓扑图：**
- 中心节点：zfh-iot 平台
- 一级节点：在线网关
- 二级节点：在线表计
- 连线状态：实线（正常）/ 虚线（离线）/ 红色（异常）

**实时报文流：**
- 底部小窗口显示最近10条报文
- 点击跳转到通讯日志页面

### 5.7 WebSocket 设计

**连接地址：** `/ws/virtual-device`

**认证机制：**
- WebSocket握手时，客户端需在URL中携带JWT Token：`/ws/virtual-device?token={jwt_token}`
- 服务端在握手阶段验证Token有效性
- 认证失败返回 `401 Unauthorized` 并关闭连接
- 认证成功后，客户端订阅以下频道：
  - `/topic/device-status`：设备状态变更广播
  - `/topic/comm-logs`：通讯日志实时推送 [P1]
  - `/topic/stats`：统计指标更新
  - `/topic/notifications`：系统通知

**消息类型：**

```json
// 1. 设备状态变更
{
  "type": "DEVICE_STATUS",
  "deviceType": "GATEWAY",
  "deviceId": 1,
  "status": "ONLINE",
  "timestamp": "2026-04-18T10:30:00"
}

// 2. 通讯日志推送
{
  "type": "COMM_LOG",
  "logId": 12345,
  "deviceType": "METER",
  "deviceId": 2,
  "deviceName": "电表-001",
  "direction": "UP",
  "protocol": "DLT645",
  "rawData": "68AA...16",
  "parsedData": "{\"voltage\":220.5,\"current\":5.2}",
  "timestamp": "2026-04-18T10:30:01"
}

// 3. 统计数据更新
{
  "type": "STATS_UPDATE",
  "onlineGateways": 5,
  "totalGateways": 10,
  "onlineMeters": 25,
  "totalMeters": 50,
  "todayUpMessages": 1523,
  "todayDownMessages": 89
}

// 4. 系统通知
{
  "type": "NOTIFICATION",
  "level": "WARNING",
  "message": "网关[gateway-001]连接断开，正在重试...",
  "timestamp": "2026-04-18T10:30:05"
}
```

---

## 6. 核心功能模块设计

### 6.1 设备连接抽象（DeviceConnection）

```java
// 设备连接的统一抽象，封装Netty Channel或MQTT Client
public interface DeviceConnection {
    Long getDeviceId();
    DeviceType getDeviceType();
    ConnectionStatus getStatus();
    
    // 建立连接
    void connect() throws ConnectionException;
    
    // 断开连接
    void disconnect();
    
    // 发送数据到平台
    void send(byte[] data) throws SendException;
    
    // 是否已连接
    boolean isConnected();
}

// TCP连接实现
public class TcpDeviceConnection implements DeviceConnection {
    private Channel nettyChannel;
    // ... 实现Netty Channel的封装
}

// MQTT连接实现
public class MqttDeviceConnection implements DeviceConnection {
    private MqttClient mqttClient;
    // ... 实现MQTT Client的封装
}
```

### 6.2 设备生命周期管理器（DeviceLifecycleManager）

**职责：**
- 管理所有虚拟设备的连接生命周期
- 处理设备启动、停止、重启
- 维护设备状态
- 处理级联操作（网关停止时自动停止下属表计）
- **应用启动时自动恢复**：读取数据库中状态为ONLINE的设备，自动重建连接

**启动恢复流程：**
```
ApplicationStartedEvent → 查询status=ONLINE的网关和表计 → 按依赖顺序启动
（先启动网关，再启动其下属的表计）→ 更新实际连接状态
```

**核心方法：**
```java
public interface DeviceLifecycleManager {
    // 启动网关
    void startGateway(Long gatewayId);
    
    // 停止网关（级联停止下属表计）
    void stopGateway(Long gatewayId);
    
    // 重启网关
    void restartGateway(Long gatewayId);
    
    // 启动表计（直连模式）
    void startMeter(Long meterId);
    
    // 停止表计
    void stopMeter(Long meterId);
    
    // 获取设备连接状态
    ConnectionStatus getStatus(Long deviceId, DeviceType type);
    
    // 获取所有活跃连接
    Map<Long, DeviceConnection> getActiveConnections();
}
```

### 6.3 数据模拟引擎（DataSimulationEngine）

**职责：**
- 为每个表计生成模拟数据
- 管理自动上报任务
- 支持硬编码的数据生成算法（累积量/波动量/比值量）
- **自定义公式（P3）**：支持用户通过JavaScript/SpEL表达式定义数据生成规则，不在MVP范围内

**数据生成策略：**

| 数据分类 | 生成算法 | 示例 |
|----------|----------|------|
| ACCUMULATING | current + random(increment_min, increment_max) | 电量：每次增加 0.01~0.05 kWh |
| FLUCTUATING | random(min, max) 或 sine_wave(base, amplitude) | 电压：220 ± 20V 随机波动 |
| RATIO | random(ratio_min, ratio_max) | 功率因数：0.85~0.95 |

**定时任务：**
- 使用 `ScheduledThreadPoolExecutor`
- 每个表计独立调度任务
- 支持动态修改上报间隔

### 6.4 协议处理器示例 [P1 - 第二阶段]

> 以下以 DL/T 645-2007 为例展示协议处理器的实现方式，该协议属于第二阶段实现内容。

**帧格式：**
```
68H + 地址域(6B) + 68H + 控制码(1B) + 数据长度(1B) + 数据域(nB) + 校验码(1B) + 16H
```

**上行编码：**
1. 根据数据项构建数据域
2. 数据域减33H处理
3. 计算帧长度和校验码
4. 组装完整帧

**下行解析：**
1. 校验帧头和帧尾
2. 验证校验码
3. 数据域加33H还原
4. 根据控制码和数据标识解析指令类型
5. 执行对应业务逻辑

**支持的控制码：**
- `0x01`：读取数据
- `0x04`：写数据
- `0x08`：广播校时
- `0x0C`：更改通信速率

### 6.5 通讯日志记录器（CommunicationLogger）

**职责：**
- 异步记录所有设备通讯报文
- 通过WebSocket实时推送日志到前端
- 支持日志查询和导出

**性能考虑：**
- 使用 `@Async` 异步写入数据库
- 定期清理日志（保留7天，可通过配置调整）
- 前端展示最近1000条，历史数据支持分页查询

---

## 7. API 设计

### 7.1 认证 API

```
POST   /api/auth/login              # 用户登录
       Request Body: { "username": "admin", "password": "xxx" }
       Response: { "token": "jwt_token", "expiresIn": 3600 }

POST   /api/auth/logout             # 用户登出
       Header: Authorization: Bearer {token}

GET    /api/auth/profile            # 获取当前用户信息
       Header: Authorization: Bearer {token}

POST   /api/auth/refresh            # 刷新Token
       Request Body: { "refreshToken": "xxx" }
       Response: { "token": "new_jwt_token", "refreshToken": "new_refresh_token", "expiresIn": 3600 }
```

**JWT Token 设计：**
- 签发者：`zfh-virtual-device-lab`
- 有效期：1小时
- 包含字段：userId、username、roles
- 刷新机制：使用Refresh Token（有效期7天）

### 7.2 网关管理 API

```
GET    /api/gateways              # 查询网关列表（支持分页、筛选）
GET    /api/gateways/{id}         # 查询网关详情
POST   /api/gateways              # 创建网关
PUT    /api/gateways/{id}         # 更新网关
DELETE /api/gateways/{id}         # 删除网关
POST   /api/gateways/{id}/start   # 启动网关连接
POST   /api/gateways/{id}/stop    # 停止网关连接
GET    /api/gateways/{id}/logs    # 查询网关通讯日志

# 批量操作
POST   /api/gateways/batch-start  # 批量启动
       Request Body: { "ids": [1, 2, 3] }
POST   /api/gateways/batch-stop   # 批量停止
       Request Body: { "ids": [1, 2, 3] }
POST   /api/gateways/batch-delete # 批量删除
       Request Body: { "ids": [1, 2, 3] }
```

### 7.3 表计管理 API

```
GET    /api/meters                # 查询表计列表
GET    /api/meters/{id}           # 查询表计详情
POST   /api/meters                # 创建表计
PUT    /api/meters/{id}           # 更新表计
DELETE /api/meters/{id}           # 删除表计
POST   /api/meters/{id}/start     # 启动表计连接
POST   /api/meters/{id}/stop      # 停止表计连接
POST   /api/meters/{id}/report    # 手动触发上报
GET    /api/meters/{id}/logs      # 查询表计通讯日志

# 批量操作
POST   /api/meters/batch-start    # 批量启动
       Request Body: { "ids": [1, 2, 3] }
POST   /api/meters/batch-stop     # 批量停止
       Request Body: { "ids": [1, 2, 3] }
POST   /api/meters/batch-delete   # 批量删除
       Request Body: { "ids": [1, 2, 3] }
POST   /api/meters/batch-config   # 批量应用模板
       Request Body: { "meterIds": [1, 2, 3], "templateId": 1 }
```

### 7.4 数据配置 API

```
GET    /api/templates                  # 查询模板列表
GET    /api/templates/{id}             # 查询模板详情
POST   /api/templates                  # 创建模板
PUT    /api/templates/{id}             # 更新模板
DELETE /api/templates/{id}             # 删除模板
POST   /api/templates/{id}/apply       # 应用模板到指定设备
       Request Body: { "meterIds": [1, 2, 3] }

GET    /api/meters/{id}/data-config    # 查询表计数据配置
PUT    /api/meters/{id}/data-config    # 更新表计数据配置
```

### 7.5 通讯日志 API

```
GET    /api/logs                     # 查询日志列表（支持筛选、分页）
       Query Params: deviceType, deviceId, direction, protocol, startTime, endTime
DELETE /api/logs                     # 清空日志（带确认）
GET    /api/logs/export              # 导出日志
```

### 7.6 实时监控 API

```
GET    /api/dashboard/stats          # 获取统计指标
GET    /api/dashboard/topology       # 获取设备拓扑数据
GET    /api/dashboard/recent-logs    # 获取最近报文（默认20条）
```

### 7.7 批量操作响应格式

批量操作返回每个设备独立的执行结果：

```json
{
  "code": 200,
  "message": "batch operation completed",
  "data": {
    "total": 3,
    "success": 2,
    "failed": 1,
    "results": [
      { "id": 1, "status": "SUCCESS" },
      { "id": 2, "status": "SUCCESS" },
      { "id": 3, "status": "FAILED", "error": "设备不存在" }
    ]
  },
  "timestamp": "2026-04-18T10:30:00"
}
```

### 7.8 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-04-18T10:30:00"
}
```

---

## 8. 错误处理与边界情况

### 8.1 连接层错误

| 错误场景 | 处理方式 |
|----------|----------|
| **端口冲突（服务端模式）** | 启动时检测端口占用，返回明确错误：`端口{port}已被占用`，状态保持 OFFLINE |
| **连接超时（客户端模式）** | 首次连接超时（30秒）后进入重试，最大重试10次，间隔指数退避（5s, 10s, 20s...最大60s） |
| **连接断开** | 自动重连（同超时策略），超过最大重试次数后状态变为 ERROR，推送WebSocket通知 |
| **ERROR状态恢复** | 用户通过Web页面点击"重启"按钮手动恢复；或系统每5分钟自动尝试一次从ERROR恢复到重连流程 |
| **重复通讯地址** | 创建/更新时校验数据库唯一约束，返回 `通讯地址已存在` 错误 |
| **Broker连接失败（MQTT）** | 同TCP客户端超时处理，使用Paho内置重连机制，参数见配置章节 |

### 8.2 协议层错误

| 错误场景 | 处理方式 |
|----------|----------|
| **报文解析失败** | 记录错误日志，丢弃报文，不响应（避免对平台造成干扰） |
| **未知控制码/指令** | 返回协议特定的错误响应帧（如DL/T 645的异常响应码0xD1），告知平台指令不被支持 |
| **数据标识不存在** | 返回协议特定的错误响应帧，告知平台数据标识无效 |
| **校验失败** | 丢弃报文，记录日志，不响应 |
| **设备类型不支持指令** | 返回协议特定的错误响应帧，告知平台设备不支持此操作（如非电表收到拉闸指令） |

### 8.3 数据模拟错误

| 错误场景 | 处理方式 |
|----------|----------|
| **配置参数缺失** | 使用默认值（initial_value=0, increment_min=0.01, increment_max=0.05等） |
| **数值溢出** | 累积量使用BigDecimal防止溢出，超出合理范围时记录警告日志 |
| **定时任务异常** | 单个表计任务异常不影响其他表计，异常任务停止并记录日志 |

### 8.4 系统级错误

| 错误场景 | 处理方式 |
|----------|----------|
| **数据库连接失败** | 应用启动失败，记录错误日志 |
| **内存不足** | 限制最大并发设备数（默认1000），超过时拒绝启动新设备 |
| **线程池耗尽** | 动态调整线程池大小，或返回 `系统繁忙，请稍后重试` |

---

## 9. 部署方案

### 9.1 开发环境

```bash
# 1. 启动后端
cd zfh-virtual-device-backend
mvn spring-boot:run
# 默认端口：8080

# 2. 启动前端
cd zfh-virtual-device-frontend
npm install
npm run dev
# 默认端口：5173
```

### 9.2 生产部署

**方案一：Spring Boot 集成前端**
- 前端构建为静态资源：`npm run build`
- 将 `dist/` 内容复制到后端 `src/main/resources/static/`
- 打包为单一 jar：`mvn clean package`
- 运行：`java -jar zfh-virtual-device-backend-1.0.0.jar`
- 访问：`http://localhost:8080`

**方案二：前后端分离部署**
- 后端 jar 部署在服务器A（端口8080）
- 前端静态资源部署在 Nginx（端口80）
- Nginx 配置反向代理到后端 API

### 9.3 配置参数

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/virtual-device-db;DB_CLOSE_DELAY=-1;MODE=MySQL
    username: sa
    password: 
  h2:
    console:
      enabled: true
      path: /h2-console
  
# 虚拟设备配置
virtual-device:
  # TCP连接池配置
  tcp:
    boss-threads: 2
    worker-threads: 8
    reconnect-base-interval: 5000    # 重连基础间隔（毫秒），首次重试等待5秒
    reconnect-max-interval: 60000    # 重连最大间隔（毫秒），指数退避上限60秒
    reconnect-max-attempts: 10       # 最大重试次数
    reconnect-backoff-multiplier: 2  # 退避乘数
  
  # MQTT配置
  mqtt:
    default-qos: 1
    connection-timeout: 30
    keep-alive-interval: 60
    reconnect-base-interval: 5000    # 重连基础间隔（毫秒）
    reconnect-max-interval: 60000    # 重连最大间隔（毫秒）
    reconnect-max-attempts: 10       # 最大重试次数
    reconnect-backoff-multiplier: 2  # 退避乘数
  
  # 数据模拟配置
  simulation:
    default-report-interval: 30
    max-concurrent-devices: 1000
    scheduler-pool-size: 50
  
  # 日志配置
  logging:
    retention-days: 7
    max-records-per-query: 10000
```

---

## 10. 非功能性需求

### 10.1 性能指标

| 指标 | 目标值 |
|------|--------|
| 单实例支持虚拟设备数 | ≥ 1000 台 |
| 并发TCP连接数 | ≥ 1000 |
| 数据上报频率 | 支持1秒级上报 |
| WebSocket推送延迟 | < 100ms |
| API响应时间 | < 200ms（P95） |

### 10.2 可靠性

- 设备连接断开后自动重连（指数退避）
- 应用重启后自动恢复设备连接状态（持久化到数据库）
- 异常隔离：单个设备异常不影响其他设备

### 10.3 安全性

- 简单的登录认证（基于Spring Security）
- WebSocket连接使用JWT Token认证
- 密码加密存储（BCrypt）
- CORS配置
- SQL注入防护（MyBatis参数化查询）

### 10.4 可扩展性

- 协议处理器通过接口 + 自动注册实现，新增协议无需修改核心代码
- 配置模板机制支持快速复用
- **自定义公式（后续版本P3）**：支持JavaScript/SpEL表达式定义数据生成规则，不在MVP范围内

---

## 11. 后续规划

### 11.1 第一阶段（MVP）- 当前实施范围
- [ ] 基础框架搭建（Spring Boot + Vue）
- [ ] 虚拟网关管理（TCP服务端/客户端）
- [ ] 虚拟表计管理（直连模式）
- [ ] 数据模拟引擎（基础数据生成，支持累积量/波动量/比值量）
- [ ] MQTT协议支持（MQTT+JSON）
- [ ] Web页面基础功能（CRUD + 连接控制 + 通讯日志查看）

**MVP范围说明：**
- **设备类型**：仅支持电表（ELECTRIC）和水表（WATER），热表/气表在UI中预留但后端暂不实现
- **连接方式**：仅支持直连模式（DIRECT），网关模式（一对多）延后到第二阶段
- **协议**：仅实现MQTT+JSON，DL/T 645和376.1协议延后
- **功能**：基础CRUD、连接控制、数据模拟、通讯日志查看

### 11.2 第二阶段
- [ ] DL/T 645-2007 协议支持
- [ ] 国网376.1 协议支持
- [ ] 表计通过网关连接（一对多）
- [ ] 通讯日志实时查看（WebSocket推送）
- [ ] 数据配置模板
- [ ] 实时监控仪表盘

### 11.3 第三阶段
- [ ] 批量操作优化
- [ ] 压力测试工具集成
- [ ] 协议扩展框架完善（DL/T 698.45、Modbus）
- [ ] 更多设备类型（热表、气表）
- [ ] 数据模拟高级配置（自定义公式、场景模拟）

---

## 12. 附录

### 12.1 术语表

| 术语 | 说明 |
|------|------|
| 虚拟网关 | 模拟真实物联网网关的软件实体 |
| 虚拟表计 | 模拟真实电表/水表/热表/气表的软件实体 |
| 直连模式 | 表计直接与平台建立网络连接 |
| 网关模式 | 表计通过网关间接与平台通讯 |
| 上行 | 设备向平台上报数据 |
| 下行 | 平台向设备下发指令 |
| 数据配置模板 | 预定义的数据项参数集合，可批量应用到设备 |

### 12.2 数据项类型参考

**电表（ELECTRIC）- MVP支持：**
- total_energy：累计电量（kWh）- 累积量
- voltage_a/b/c：三相电压（V）- 波动量
- current_a/b/c：三相电流（A）- 波动量
- power：有功功率（W）- 波动量
- power_factor：功率因数 - 比值量
- frequency：频率（Hz）- 波动量

**水表（WATER）- MVP支持：**
- total_water：累计用水量（m³）- 累积量
- flow_rate：瞬时流量（m³/h）- 波动量
- pressure：水压（MPa）- 波动量

**热表（HEAT）- P2支持：**
- total_heat：累计热量（GJ）- 累积量
- supply_temp：供水温度（℃）- 波动量
- return_temp：回水温度（℃）- 波动量

**气表（GAS）- P2支持：**
- total_gas：累计用气量（m³）- 累积量
- pressure：气压（kPa）- 波动量

---

**文档结束**
