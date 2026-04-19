# 物联网平台详细设计文档

> 版本：v1.0  
> 基于PRD：zfh-iot.md v1.0  
> 创建时间：2026-03-29  
> 状态：初稿

---

## 文档目的

本文档根据《物联网平台需求文档（PRD）》进行技术层面的详细设计，涵盖系统架构、模块设计、数据库设计、API接口设计、核心业务流程设计等内容，为开发团队提供完整的技术实现参考。

---

## 1. 系统架构设计

### 1.1 总体架构

采用分层架构设计，从上到下分为：**Web管理后台层**、**应用服务层**、**前置通信层**、**前置业务层**、**前置存盘层**。

```
┌─────────────────────────────────────────────────────────────────┐
│                        Web 管理后台                              │
│   Vue3 + Element Plus (SPA单页应用)                              │
│   ├─ 首页大屏 (数据可视化)                                       │
│   ├─ 系统管理 (租户/用户/角色/日志)                              │
│   ├─ 档案管理 (网关/表计/群组/协议/厂商)                         │
│   ├─ 抄表数据 (实时/历史数据查询)                                │
│   └─ 报警管理 (预留扩展)                                         │
└─────────────────────────┬───────────────────────────────────────┘
                          │ HTTPS / WebSocket
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      应用服务层 (Spring Boot)                    │
│   ├─ Controller层 (REST API 控制器)                             │
│   ├─ Service层 (业务逻辑处理)                                    │
│   ├─ Mapper层 (数据访问层)                                       │
│   ├─ 权限控制 (Shiro + JWT)                                      │
│   └─ 任务调度 (XXL-Job / Quartz)                                 │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    前置通信层 ⭐ 核心 (Netty)                     │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│   │ TCP Server   │  │ UDP Server   │  │ MQTT Handler │         │
│   │ Port: 8080   │  │ Port: 8081   │  │ (集成EMQX)   │         │
│   │ Channel Pool │  │ Datagram     │  │ Pub/Sub      │         │
│   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│          └─────────────────┴─────────────────┘                  │
│                          │                                      │
│                   连接管理器 (Connection Manager)                │
│   ├─ 设备认证 (DeviceAuthHandler)                                │
│   ├─ 心跳检测 (HeartbeatHandler)                                 │
│   ├─ IP白名单 (IpWhitelistHandler)                               │
│   └─ 限流控制 (RateLimitHandler)                                 │
└─────────────────────────┬───────────────────────────────────────┘
                          │ 内部消息总线 (Disruptor)
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     前置业务层                                   │
│   ├─ 协议解析引擎 (ProtocolEngine)                               │
│   │   ├─ 协议注册中心 (ProtocolRegistry)                        │
│   │   ├─ 协议加载器 (SPI机制)                                    │
│   │   └─ 报文解码器 (MessageDecoder)                            │
│   ├─ 报文校验 (CRC/Length/Range Validator)                       │
│   ├─ 数据转换器 (DataTransformer)                                │
│   └─ 指令下发器 (CommandDispatcher)                              │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     前置存盘层                                   │
│   ┌───────────────────────────────────────────────────────┐    │
│   │               数据缓冲队列 (Disruptor RingBuffer)        │    │
│   │   ├─ 内存队列 (高性能写入)                               │    │
│   │   └─ 磁盘持久化 (防丢数据)                               │    │
│   └───────────────────────────────────────────────────────┘    │
│                          │                                      │
│          ┌───────────────┼───────────────┐                     │
│          ▼               ▼               ▼                     │
│   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐          │
│   │ 批量写入器   │ │  批量写入器  │ │  批量写入器  │          │
│   │ (1000条/批) │ │  (1000条/批) │ │  (1000条/批) │          │
│   └──────┬───────┘ └──────┬───────┘ └──────┬───────┘          │
│          │                │                │                   │
│          ▼                ▼                ▼                   │
│   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐          │
│   │   TDengine   │ │    MySQL     │ │    Redis     │          │
│   │  (时序数据)  │ │  (关系数据)  │ │   (缓存)     │          │
│   │  抄表历史    │ │  设备档案    │ │  设备状态    │          │
│   │  数据点      │ │  用户信息    │ │  会话信息    │          │
│   └──────────────┘ └──────────────┘ └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 技术栈选型

| 层级 | 技术选型 | 版本建议 | 选型理由 |
|------|----------|----------|----------|
| **前置通信** | Netty | 4.1.x | 高性能NIO框架，支持10万+并发 |
| **MQTT Broker** | EMQX | 5.x | 企业级MQTT服务器，支持百万连接 |
| **后端框架** | Spring Boot | 3.x | 成熟生态，快速开发 |
| **ORM框架** | MyBatis-Plus | 3.5.x | 增强CRUD，代码生成 |
| **权限框架** | Apache Shiro | 1.12.x | 轻量级，支持多租户 |
| **缓存** | Redis + Caffeine | 7.x / 3.x | 多级缓存策略 |
| **消息队列** | Disruptor | 4.x | 高性能内存队列，无锁设计 |
| **时序数据库** | TDengine | 3.x | 国产，专为IoT优化 |
| **关系数据库** | MySQL | 8.0 | 稳定可靠，生态成熟 |
| **前端框架** | Vue3 + Element Plus | 3.x / 2.x | 现代化前端技术栈 |
| **任务调度** | XXL-Job | 2.4.x | 分布式任务调度 |
| **日志框架** | Logback + ELK | - | 集中式日志管理 |

---

## 2. 模块详细设计

### 2.1 前置通信层设计

#### 2.1.1 TCP Server 模块

**职责**：处理设备长连接，支持心跳保持，最大并发10万+连接

**核心组件**：

```java
// TCP服务器启动器
@Component
public class TcpServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    
    @PostConstruct
    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast(new IdleStateHandler(60, 0, 0))  // 60秒读超时
                        .addLast(new DeviceAuthHandler())          // 设备认证
                        .addLast(new IpWhitelistHandler())         // IP白名单
                        .addLast(new RateLimitHandler())           // 限流控制
                        .addLast(new HeartbeatHandler())           // 心跳处理
                        .addLast(new MessageDecoder())             // 报文解码
                        .addLast(new BusinessHandler());           // 业务处理
                }
            });
    }
}
```

**Channel Pipeline 设计**：

```
┌─────────────────────────────────────────────────────────────┐
│                    TCP Channel Pipeline                      │
├─────────────────────────────────────────────────────────────┤
│  1. IdleStateHandler (60s读超时检测)                         │
│     └─ 触发userEventTriggered事件，关闭超时连接               │
├─────────────────────────────────────────────────────────────┤
│  2. DeviceAuthHandler (设备认证)                              │
│     └─ 首包认证，验证Device ID + Token，失败则断开连接         │
├─────────────────────────────────────────────────────────────┤
│  3. IpWhitelistHandler (IP白名单)                             │
│     └─ 验证客户端IP是否在白名单内                              │
├─────────────────────────────────────────────────────────────┤
│  4. RateLimitHandler (限流控制)                               │
│     └─ 令牌桶算法，限制单设备请求频率                           │
├─────────────────────────────────────────────────────────────┤
│  5. HeartbeatHandler (心跳处理)                               │
│     └─ 维护设备在线状态，更新最后心跳时间                        │
├─────────────────────────────────────────────────────────────┤
│  6. MessageDecoder (报文解码)                                 │
│     └─ 根据协议标识选择对应解码器，解析二进制报文                 │
├─────────────────────────────────────────────────────────────┤
│  7. BusinessHandler (业务处理器)                              │
│     └─ 将解析后的数据推送到业务层处理                           │
└─────────────────────────────────────────────────────────────┘
```

#### 2.1.2 UDP Server 模块

**职责**：处理无连接数据包上报，适用于频率上报的设备

```java
@Component
public class UdpServer {
    private EventLoopGroup group;
    
    @PostConstruct
    public void start() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioDatagramChannel.class)
            .option(ChannelOption.SO_RCVBUF, 65535)
            .handler(new ChannelInitializer<NioDatagramChannel>() {
                @Override
                protected void initChannel(NioDatagramChannel ch) {
                    ch.pipeline()
                        .addLast(new DatagramRateLimitHandler())
                        .addLast(new DatagramDecoder())
                        .addLast(new DatagramBusinessHandler());
                }
            });
    }
}
```

**TCP vs UDP 对比**：

| 特性 | TCP Server | UDP Server |
|------|------------|------------|
| 连接方式 | 长连接 | 无连接 |
| 适用场景 | 需要双向通信、指令下发 | 仅上报数据、高频上报 |
| 设备状态 | 可维护在线/离线状态 | 仅接收数据，不维护状态 |
| 性能指标 | 10万+并发连接 | 5万+ QPS |
| 可靠性 | 高（可靠传输） | 低（可能丢包） |
| 协议支持 | Modbus TCP、自定义长连接协议 | 简单上报协议 |

#### 2.1.3 连接管理器 (ConnectionManager)

**职责**：统一管理所有设备连接，支持连接查询、状态维护

```java
@Component
public class ConnectionManager {
    // 设备ID -> Channel 映射
    private final ConcurrentHashMap<String, Channel> deviceChannelMap = 
        new ConcurrentHashMap<>();
    
    // Channel -> 设备信息 映射
    private final ConcurrentHashMap<Channel, DeviceConnection> channelDeviceMap = 
        new ConcurrentHashMap<>();
    
    // 注册连接
    public void register(String deviceId, Channel channel, DeviceAuthInfo authInfo) {
        DeviceConnection conn = new DeviceConnection(deviceId, channel, authInfo);
        deviceChannelMap.put(deviceId, channel);
        channelDeviceMap.put(channel, conn);
        // 更新设备在线状态到Redis
        redisTemplate.opsForValue().set(
            "device:online:" + deviceId, 
            "1", 
            Duration.ofMinutes(5)
        );
    }
    
    // 注销连接
    public void unregister(Channel channel) {
        DeviceConnection conn = channelDeviceMap.remove(channel);
        if (conn != null) {
            deviceChannelMap.remove(conn.getDeviceId());
            // 更新设备离线状态
            redisTemplate.delete("device:online:" + conn.getDeviceId());
        }
    }
    
    // 获取设备Channel
    public Channel getChannel(String deviceId) {
        return deviceChannelMap.get(deviceId);
    }
    
    // 获取在线设备数
    public int getOnlineCount() {
        return deviceChannelMap.size();
    }
}
```

#### 2.1.4 设备认证 (DeviceAuthHandler)

**认证流程**：

```
设备首次连接
     │
     ▼
┌─────────────────┐
│ 接收首包数据     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐    否    ┌─────────────────┐
│ 解析Device ID   │─────────▶│ 返回认证失败     │
│ 和Token         │          │ 断开连接         │
└────────┬────────┘          └─────────────────┘
         │ 是
         ▼
┌─────────────────┐    否    ┌─────────────────┐
│ 验证Token有效性  │─────────▶│ 返回认证失败     │
│ (查数据库/缓存)  │          │ 断开连接         │
└────────┬────────┘          └─────────────────┘
         │ 是
         ▼
┌─────────────────┐    否    ┌─────────────────┐
│ 验证IP白名单     │─────────▶│ 返回认证失败     │
└────────┬────────┘          │ 断开连接         │
         │ 是                └─────────────────┘
         ▼
┌─────────────────┐
│ 注册到连接管理器 │
│ 更新在线状态     │
│ 返回认证成功     │
└─────────────────┘
```

**认证报文格式（示例）**：

```
┌──────────┬──────────┬──────────┬─────────────────┐
│  帧头    │  长度    │  命令字  │     数据域      │
│  2字节   │  2字节   │  1字节   │    N字节        │
├──────────┼──────────┼──────────┼─────────────────┤
│ 0xAA55   │ 0x0020   │ 0x01     │ Device ID(16B)  │
│          │          │(认证)    │ Token(16B)      │
└──────────┴──────────┴──────────┴─────────────────┘
```

### 2.2 前置业务层设计

#### 2.2.1 协议解析引擎 (ProtocolEngine)

**架构设计**：

```
┌─────────────────────────────────────────────────────────────┐
│                    ProtocolEngine                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ProtocolRegistry (协议注册中心)           │   │
│  │  Map<protocolCode, ProtocolParser>                   │   │
│  │  ├─ modbus -> ModbusProtocolParser                   │   │
│  │  ├─ dlt645 -> Dlt645ProtocolParser                   │   │
│  │  ├─ custom1 -> CustomProtocolParser1                 │   │
│  │  └─ custom2 -> CustomProtocolParser2                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           ProtocolLoader (SPI协议加载器)              │   │
│  │  ├─ 扫描classpath下的ProtocolParser实现类             │   │
│  │  ├─ 动态加载jar包中的协议实现                         │   │
│  │  └─ 热加载支持（运行时添加新协议）                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              MessageDecoder (报文解码器)              │   │
│  │  ├─ 识别协议类型（通过端口或报文特征）                  │   │
│  │  ├─ 调用对应ProtocolParser解析                        │   │
│  │  ├─ 数据校验（CRC/长度/范围）                         │   │
│  │  └─ 转换为统一的DeviceData对象                        │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**SPI协议扩展机制**：

```java
// 协议解析器接口
public interface ProtocolParser {
    // 协议编码
    String getProtocolCode();
    
    // 协议名称
    String getProtocolName();
    
    // 解码报文
    DeviceData decode(ByteBuf buffer);
    
    // 编码指令
    ByteBuf encode(Command command);
    
    // 验证报文完整性
    boolean validate(ByteBuf buffer);
}

// SPI配置：META-INF/services/com.zfh.iot.protocol.ProtocolParser
// 内容：
// com.zfh.iot.protocol.modbus.ModbusProtocolParser
// com.zfh.iot.protocol.dlt645.Dlt645ProtocolParser
// com.zfh.iot.protocol.custom.CustomProtocolParser
```

#### 2.2.2 报文校验器

**校验链设计**：

```java
public class ValidationChain {
    private final List<DataValidator> validators = Arrays.asList(
        new LengthValidator(),      // 长度校验
        new CrcValidator(),         // CRC校验
        new RangeValidator()        // 数值范围校验
    );
    
    public ValidationResult validate(DeviceData data) {
        for (DataValidator validator : validators) {
            ValidationResult result = validator.validate(data);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.success();
    }
}
```

#### 2.2.3 指令下发器 (CommandDispatcher)

**职责**：将平台指令下发到指定设备

```java
@Service
public class CommandDispatcher {
    @Autowired
    private ConnectionManager connectionManager;
    
    @Autowired
    private ProtocolEngine protocolEngine;
    
    /**
     * 下发指令到设备
     */
    public CommandResult sendCommand(String deviceId, Command command) {
        // 1. 获取设备连接
        Channel channel = connectionManager.getChannel(deviceId);
        if (channel == null || !channel.isActive()) {
            return CommandResult.fail("设备不在线");
        }
        
        // 2. 获取设备协议类型
        String protocolCode = getDeviceProtocol(deviceId);
        
        // 3. 编码指令
        ProtocolParser parser = protocolEngine.getParser(protocolCode);
        ByteBuf commandData = parser.encode(command);
        
        // 4. 发送并等待响应
        Promise<CommandResult> promise = channel.eventLoop().newPromise();
        channel.attr(CommandHandler.COMMAND_PROMISE).set(promise);
        channel.writeAndFlush(commandData);
        
        // 5. 超时等待响应
        try {
            return promise.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return CommandResult.fail("指令响应超时");
        }
    }
}
```

### 2.3 前置存盘层设计

#### 2.3.1 数据缓冲架构

```
┌─────────────────────────────────────────────────────────────┐
│                    数据缓冲层 (Disruptor)                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   生产者 (多线程)              RingBuffer (2^20 slots)        │
│   ├─ TCP Handler ───────────▶  ┌───┬───┬───┬───┬───┐       │
│   ├─ UDP Handler ───────────▶  │ 0 │ 1 │ 2 │ 3 │...│       │
│   ├─ MQTT Handler ──────────▶  └───┴───┴───┴───┴───┘       │
│   └─ ...                     无锁队列，单线程消费             │
│                                                              │
│                                    │                        │
│                                    ▼                        │
│                           ┌─────────────────┐               │
│                           │  EventHandler   │               │
│                           │  (单线程消费)    │               │
│                           └────────┬────────┘               │
│                                    │                        │
│                    ┌───────────────┼───────────────┐        │
│                    ▼               ▼               ▼        │
│            ┌──────────┐   ┌──────────┐   ┌──────────┐      │
│            │ 时序数据  │   │ 关系数据  │   │  缓存    │      │
│            │ 缓冲区   │   │ 缓冲区   │   │ 缓冲区   │      │
│            │(TDengine)│   │ (MySQL) │   │ (Redis) │      │
│            └────┬─────┘   └────┬─────┘   └────┬─────┘      │
│                 │              │              │             │
│                 ▼              ▼              ▼             │
│            ┌──────────┐   ┌──────────┐   ┌──────────┐      │
│            │ 批量写入  │   │ 批量写入  │   │ 批量写入  │      │
│            │ 1000条/批│   │ 500条/批 │   │ 异步写入  │      │
│            └──────────┘   └──────────┘   └──────────┘      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Disruptor配置**：

```java
@Configuration
public class DisruptorConfig {
    
    @Bean
    public Disruptor<DeviceDataEvent> deviceDataDisruptor() {
        // RingBuffer大小：2^20 = 1,048,576
        int bufferSize = 1 << 20;
        
        Disruptor<DeviceDataEvent> disruptor = new Disruptor<>(
            DeviceDataEvent::new,
            bufferSize,
            Executors.defaultThreadFactory(),
            ProducerType.MULTI,          // 多生产者
            new BusySpinWaitStrategy()   // 高性能等待策略
        );
        
        // 设置消费者
        disruptor.handleEventsWith(
            new TimeseriesDataHandler(),    // 时序数据处理器
            new RelationalDataHandler(),    // 关系数据处理器
            new CacheUpdateHandler()        // 缓存更新处理器
        );
        
        return disruptor;
    }
}
```

#### 2.3.2 批量写入策略

```java
@Component
public class BatchWriter {
    private final List<DeviceDataPoint> buffer = new ArrayList<>();
    private static final int BATCH_SIZE = 1000;
    private static final int FLUSH_INTERVAL_MS = 1000;
    
    @Scheduled(fixedRate = FLUSH_INTERVAL_MS)
    public void scheduledFlush() {
        flush();
    }
    
    public void add(DeviceDataPoint data) {
        synchronized (buffer) {
            buffer.add(data);
            if (buffer.size() >= BATCH_SIZE) {
                flush();
            }
        }
    }
    
    private void flush() {
        List<DeviceDataPoint> batch;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            batch = new ArrayList<>(buffer);
            buffer.clear();
        }
        
        // 批量写入TDengine
        String sql = buildInsertSql(batch);
        tdengineTemplate.execute(sql);
    }
}
```

---

## 3. 数据库设计

### 3.1 关系数据库设计 (MySQL)

#### 3.1.1 租户管理模块

```sql
-- 租户表
CREATE TABLE sys_tenant (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '租户ID',
    tenant_code     VARCHAR(64) NOT NULL UNIQUE COMMENT '租户编码',
    tenant_name     VARCHAR(128) NOT NULL COMMENT '租户名称',
    contact_name    VARCHAR(64) COMMENT '联系人',
    contact_phone   VARCHAR(20) COMMENT '联系电话',
    max_devices     INT DEFAULT 1000 COMMENT '最大设备数配额',
    max_gateways    INT DEFAULT 100 COMMENT '最大网关数配额',
    storage_days    INT DEFAULT 365 COMMENT '数据存储天数',
    status          TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_code (tenant_code)
) COMMENT='租户表';
```

#### 3.1.2 用户权限模块

```sql
-- 用户表
CREATE TABLE sys_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    tenant_id       BIGINT NOT NULL COMMENT '租户ID',
    username        VARCHAR(64) NOT NULL COMMENT '用户名',
    password        VARCHAR(128) NOT NULL COMMENT '加密密码',
    real_name       VARCHAR(64) COMMENT '真实姓名',
    phone           VARCHAR(20) COMMENT '手机号',
    email           VARCHAR(128) COMMENT '邮箱',
    avatar          VARCHAR(256) COMMENT '头像URL',
    status          TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip   VARCHAR(64) COMMENT '最后登录IP',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_username (tenant_id, username),
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (status)
) COMMENT='用户表';

-- 角色表
CREATE TABLE sys_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL COMMENT '租户ID',
    role_code       VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name       VARCHAR(128) NOT NULL COMMENT '角色名称',
    description     VARCHAR(256) COMMENT '描述',
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_code (tenant_id, role_code),
    INDEX idx_tenant (tenant_id)
) COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user (user_id),
    INDEX idx_role (role_id)
) COMMENT='用户角色关联表';

-- 权限表
CREATE TABLE sys_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id       BIGINT DEFAULT 0 COMMENT '父权限ID',
    perm_code       VARCHAR(128) NOT NULL COMMENT '权限编码',
    perm_name       VARCHAR(128) NOT NULL COMMENT '权限名称',
    perm_type       TINYINT COMMENT '类型：1菜单 2按钮 3接口',
    path            VARCHAR(256) COMMENT '路由路径/API路径',
    component       VARCHAR(128) COMMENT '组件路径',
    icon            VARCHAR(64) COMMENT '图标',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id         BIGINT NOT NULL,
    perm_id         BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, perm_id)
) COMMENT='角色权限关联表';

-- 操作日志表
CREATE TABLE sys_operation_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT COMMENT '租户ID',
    user_id         BIGINT COMMENT '用户ID',
    username        VARCHAR(64) COMMENT '用户名',
    operation       VARCHAR(128) COMMENT '操作描述',
    method          VARCHAR(256) COMMENT '请求方法',
    params          TEXT COMMENT '请求参数',
    ip              VARCHAR(64) COMMENT 'IP地址',
    user_agent      VARCHAR(512) COMMENT 'User-Agent',
    duration        INT COMMENT '执行时长(ms)',
    status          TINYINT COMMENT '状态：0失败 1成功',
    error_msg       TEXT COMMENT '错误信息',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_user (user_id),
    INDEX idx_time (create_time)
) COMMENT='操作日志表';
```

#### 3.1.3 档案管理模块

```sql
-- 网关表
CREATE TABLE iot_gateway (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id           BIGINT NOT NULL COMMENT '租户ID',
    gateway_no          VARCHAR(64) NOT NULL COMMENT '网关编号',
    gateway_name        VARCHAR(128) COMMENT '网关名称',
    gateway_type        VARCHAR(32) COMMENT '网关类型',
    manufacturer_id     BIGINT COMMENT '厂商ID',
    protocol_code       VARCHAR(32) COMMENT '协议编码',
    ip_address          VARCHAR(64) COMMENT 'IP地址',
    port                INT COMMENT '端口',
    device_limit        INT DEFAULT 100 COMMENT '最大接入设备数',
    heartbeat_interval  INT DEFAULT 60 COMMENT '心跳间隔(秒)',
    status              TINYINT DEFAULT 0 COMMENT '状态：0离线 1在线 2禁用',
    last_online_time    DATETIME COMMENT '最后在线时间',
    remark              VARCHAR(512) COMMENT '备注',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_no (tenant_id, gateway_no),
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (status),
    INDEX idx_protocol (protocol_code)
) COMMENT='网关表';

-- 表计表
CREATE TABLE iot_meter (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id           BIGINT NOT NULL COMMENT '租户ID',
    gateway_id          BIGINT COMMENT '所属网关ID',
    meter_no            VARCHAR(64) NOT NULL COMMENT '表计编号',
    meter_name          VARCHAR(128) COMMENT '表计名称',
    meter_type          TINYINT NOT NULL COMMENT '类型：1电表 2水表 3气表 4热表',
    manufacturer_id     BIGINT COMMENT '厂商ID',
    protocol_code       VARCHAR(32) COMMENT '协议编码',
    device_address      VARCHAR(32) COMMENT '设备地址',
    ct_ratio            DECIMAL(10,2) DEFAULT 1.00 COMMENT 'CT变比',
    pt_ratio            DECIMAL(10,2) DEFAULT 1.00 COMMENT 'PT变比',
    meter_ratio         DECIMAL(10,2) DEFAULT 1.00 COMMENT '表计倍率',
    address             VARCHAR(256) COMMENT '安装地址',
    longitude           DECIMAL(10,7) COMMENT '经度',
    latitude            DECIMAL(10,7) COMMENT '纬度',
    install_time        DATE COMMENT '安装日期',
    status              TINYINT DEFAULT 0 COMMENT '状态：0离线 1在线 2禁用',
    last_online_time    DATETIME COMMENT '最后在线时间',
    last_reading_time   DATETIME COMMENT '最后抄表时间',
    remark              VARCHAR(512) COMMENT '备注',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_no (tenant_id, meter_no),
    INDEX idx_tenant (tenant_id),
    INDEX idx_gateway (gateway_id),
    INDEX idx_type (meter_type),
    INDEX idx_status (status)
) COMMENT='表计表';

-- 群组表
CREATE TABLE iot_group (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL COMMENT '租户ID',
    parent_id       BIGINT DEFAULT 0 COMMENT '父群组ID',
    group_name      VARCHAR(128) NOT NULL COMMENT '群组名称',
    group_type      TINYINT COMMENT '群组类型',
    description     VARCHAR(256) COMMENT '描述',
    sort_order      INT DEFAULT 0,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_parent (parent_id)
) COMMENT='群组表';

-- 群组设备关联表
CREATE TABLE iot_group_meter (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id    BIGINT NOT NULL,
    meter_id    BIGINT NOT NULL,
    UNIQUE KEY uk_group_meter (group_id, meter_id),
    INDEX idx_group (group_id),
    INDEX idx_meter (meter_id)
) COMMENT='群组设备关联表';

-- 协议配置表
CREATE TABLE iot_protocol (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    protocol_code   VARCHAR(32) NOT NULL UNIQUE COMMENT '协议编码',
    protocol_name   VARCHAR(128) NOT NULL COMMENT '协议名称',
    protocol_type   TINYINT COMMENT '类型：1标准协议 2自定义协议',
    version         VARCHAR(32) COMMENT '版本',
    jar_path        VARCHAR(256) COMMENT '协议JAR包路径',
    description     VARCHAR(512) COMMENT '描述',
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='协议配置表';

-- 协议字段映射表
CREATE TABLE iot_protocol_field (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    protocol_code   VARCHAR(32) NOT NULL COMMENT '协议编码',
    field_code      VARCHAR(64) NOT NULL COMMENT '字段编码',
    field_name      VARCHAR(128) COMMENT '字段名称',
    field_type      VARCHAR(32) COMMENT '数据类型',
    byte_offset     INT COMMENT '字节偏移',
    byte_length     INT COMMENT '字节长度',
    scale_factor    DECIMAL(10,4) DEFAULT 1.0000 COMMENT '缩放因子',
    unit            VARCHAR(32) COMMENT '单位',
    description     VARCHAR(256),
    INDEX idx_protocol (protocol_code)
) COMMENT='协议字段映射表';

-- 厂商表
CREATE TABLE iot_manufacturer (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT COMMENT '租户ID（为空表示系统级）',
    manufacturer_name VARCHAR(128) NOT NULL COMMENT '厂商名称',
    contact_name    VARCHAR(64) COMMENT '联系人',
    contact_phone   VARCHAR(20) COMMENT '联系电话',
    address         VARCHAR(256) COMMENT '地址',
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT='厂商表';
```

#### 3.1.4 抄表任务模块

```sql
-- 抄表任务表
CREATE TABLE iot_reading_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL,
    task_name       VARCHAR(128) COMMENT '任务名称',
    task_type       TINYINT COMMENT '任务类型：1定时任务 2手动任务',
    cron_expression VARCHAR(64) COMMENT 'Cron表达式',
    target_type     TINYINT COMMENT '目标类型：1全部 2群组 3设备',
    target_ids      VARCHAR(512) COMMENT '目标ID列表',
    reading_items   VARCHAR(256) COMMENT '抄读项',
    status          TINYINT DEFAULT 0 COMMENT '状态：0停用 1启用',
    last_exec_time  DATETIME COMMENT '最后执行时间',
    next_exec_time  DATETIME COMMENT '下次执行时间',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (status)
) COMMENT='抄表任务表';

-- 抄表任务执行记录表
CREATE TABLE iot_reading_task_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT NOT NULL,
    exec_time       DATETIME COMMENT '执行时间',
    total_count     INT COMMENT '总设备数',
    success_count   INT COMMENT '成功数',
    fail_count      INT COMMENT '失败数',
    fail_devices    TEXT COMMENT '失败设备列表',
    duration        INT COMMENT '执行时长(ms)',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_time (exec_time)
) COMMENT='抄表任务执行记录表';
```

### 3.2 时序数据库设计 (TDengine)

#### 3.2.1 超级表设计

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS iot_data 
    KEEP 365 DAYS 30 BLOCKS 6 
    UPDATE 1;

USE iot_data;

-- 抄表数据超级表
-- 标签：租户ID、设备ID、设备类型、网关ID
CREATE STABLE IF NOT EXISTS meter_reading (
    ts TIMESTAMP,               -- 数据采集时间
    reading_time TIMESTAMP,     -- 表计读数时间
    total_energy DOUBLE,        -- 总电能(kWh) / 总用水量(m³) / 总用气量(m³) / 总热量(GJ)
    total_energy_unit VARCHAR(8), -- 总用量单位
    voltage_a DOUBLE,           -- A相电压(V)
    voltage_b DOUBLE,           -- B相电压(V)
    voltage_c DOUBLE,           -- C相电压(V)
    current_a DOUBLE,           -- A相电流(A)
    current_b DOUBLE,           -- B相电流(A)
    current_c DOUBLE,           -- C相电流(A)
    power_active DOUBLE,        -- 有功功率(kW)
    power_reactive DOUBLE,      -- 无功功率(kvar)
    power_factor DOUBLE,        -- 功率因数
    frequency DOUBLE,           -- 频率(Hz)
    temperature DOUBLE,         -- 温度(°C)
    pressure DOUBLE,            -- 压力(kPa)
    flow_rate DOUBLE,           -- 流量(m³/h)
    signal_quality INT,         -- 信号质量
    battery_level INT,          -- 电池电量(%)
    raw_data BINARY(512)        -- 原始报文数据
) TAGS (
    tenant_id BIGINT,           -- 租户ID
    meter_id BIGINT,            -- 表计ID
    meter_type TINYINT,         -- 表计类型：1电表 2水表 3气表 4热表
    gateway_id BIGINT           -- 网关ID
);

-- 创建子表示例（按设备自动创建）
-- CREATE TABLE meter_1001 USING meter_reading 
--     TAGS (1, 1001, 1, 100);
```

#### 3.2.2 数据保留策略

```sql
-- 设置数据保留策略
-- 原始数据保留1年
ALTER DATABASE iot_data KEEP 365;

-- 创建聚合表（用于报表查询优化）
-- 小时级聚合数据（保留2年）
CREATE DATABASE IF NOT EXISTS iot_hourly KEEP 730;
USE iot_hourly;

CREATE STABLE IF NOT EXISTS meter_hourly (
    ts TIMESTAMP,
    avg_voltage_a DOUBLE,
    max_voltage_a DOUBLE,
    min_voltage_a DOUBLE,
    avg_current_a DOUBLE,
    max_current_a DOUBLE,
    min_current_a DOUBLE,
    total_energy_diff DOUBLE,   -- 本时段用电量
    avg_power_active DOUBLE
) TAGS (
    tenant_id BIGINT,
    meter_id BIGINT,
    meter_type TINYINT
);

-- 日级聚合数据（保留3年）
CREATE DATABASE IF NOT EXISTS iot_daily KEEP 1095;
USE iot_daily;

CREATE STABLE IF NOT EXISTS meter_daily (
    ts TIMESTAMP,
    start_energy DOUBLE,
    end_energy DOUBLE,
    energy_diff DOUBLE,         -- 日电量
    peak_energy DOUBLE,         -- 峰电量
    valley_energy DOUBLE,       -- 谷电量
    reading_count INT           -- 抄表次数
) TAGS (
    tenant_id BIGINT,
    meter_id BIGINT,
    meter_type TINYINT
);
```

### 3.3 缓存设计 (Redis)

```sql
-- 缓存键设计规范

-- 1. 设备在线状态
-- Key: device:online:{meter_id}
-- Value: 1
-- TTL: 5分钟（根据心跳间隔调整）
SET device:online:1001 1 EX 300

-- 2. 设备最后数据（用于快速查询）
-- Key: device:lastdata:{meter_id}
-- Value: JSON格式最新数据
SET device:lastdata:1001 '{"total_energy": 1234.5, "ts": "2026-03-29T10:00:00"}' EX 3600

-- 3. 设备会话信息
-- Key: device:session:{device_id}
-- Value: {channel_id, connect_time, ip, protocol}
HSET device:session:1001 channel_id "ch-001" connect_time "2026-03-29T10:00:00"

-- 4. 租户设备数量缓存
-- Key: tenant:device:count:{tenant_id}
-- Value: 设备总数
SET tenant:device:count:1 1500 EX 3600

-- 5. 速率限制（令牌桶）
-- Key: ratelimit:{device_id}
-- Value: 剩余令牌数
SET ratelimit:1001 100 EX 60

-- 6. 用户登录Token
-- Key: user:token:{token}
-- Value: user_id
SET user:token:abc123 1001 EX 7200

-- 7. 数据大屏统计缓存
-- Key: dashboard:{tenant_id}:{metric}
-- Value: 统计数据
SET dashboard:1:online_gateways 45 EX 60
SET dashboard:1:total_meters 1500 EX 60
```

---

## 4. API 接口设计

### 4.1 通用规范

**基础路径**：`/api/v1`

**认证方式**：JWT Token (Header: `Authorization: Bearer {token}`)

**响应格式**：
```json
{
    "code": 200,
    "message": "success",
    "data": {},
    "timestamp": 1711680000000
}
```

**分页参数**：
- `page`: 页码，从1开始
- `size`: 每页条数，默认20，最大100

**响应分页数据**：
```json
{
    "code": 200,
    "data": {
        "list": [],
        "total": 100,
        "page": 1,
        "size": 20,
        "pages": 5
    }
}
```

### 4.2 认证相关接口

#### 4.2.1 用户登录
```
POST /auth/login

Request:
{
    "username": "admin",
    "password": "encrypted_password",
    "captcha": "1234",
    "captchaKey": "uuid"
}

Response:
{
    "code": 200,
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIs...",
        "refreshToken": "refresh_token...",
        "expiresIn": 7200,
        "user": {
            "id": 1,
            "username": "admin",
            "realName": "管理员",
            "avatar": "...",
            "tenantId": 1
        }
    }
}
```

#### 4.2.2 刷新Token
```
POST /auth/refresh

Request:
{
    "refreshToken": "refresh_token..."
}

Response:
{
    "code": 200,
    "data": {
        "token": "new_access_token...",
        "expiresIn": 7200
    }
}
```

### 4.3 首页大屏接口

#### 4.3.1 获取统计数据
```
GET /dashboard/statistics

Response:
{
    "code": 200,
    "data": {
        "gateways": {
            "total": 100,
            "online": 85,
            "offline": 15
        },
        "meters": {
            "total": 1500,
            "electric": 800,
            "water": 400,
            "gas": 200,
            "heat": 100
        },
        "reading": {
            "shouldRead": 1500,
            "actualRead": 1480,
            "completeRate": 98.67
        }
    }
}
```

#### 4.3.2 获取实时数据列表
```
GET /dashboard/realtime?page=1&size=20

Response:
{
    "code": 200,
    "data": {
        "list": [
            {
                "meterId": 1001,
                "meterNo": "M20240001",
                "meterName": "1号楼电表",
                "meterType": 1,
                "gatewayId": 101,
                "readTime": "2026-03-29T10:30:00",
                "totalEnergy": 1234.56,
                "status": 1
            }
        ],
        "total": 1500
    }
}
```

### 4.4 系统管理接口

#### 4.4.1 租户管理
```
租户列表:    GET    /sys/tenants?page=1&size=20&keyword=
租户详情:    GET    /sys/tenants/{id}
创建租户:    POST   /sys/tenants
更新租户:    PUT    /sys/tenants/{id}
删除租户:    DELETE /sys/tenants/{id}
启用/禁用:   PUT    /sys/tenants/{id}/status
```

**创建租户Request**：
```json
{
    "tenantCode": "tenant001",
    "tenantName": "测试租户",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "maxDevices": 1000,
    "maxGateways": 100,
    "storageDays": 365
}
```

#### 4.4.2 用户管理
```
用户列表:    GET    /sys/users?page=1&size=20&keyword=
用户详情:    GET    /sys/users/{id}
创建用户:    POST   /sys/users
更新用户:    PUT    /sys/users/{id}
删除用户:    DELETE /sys/users/{id}
重置密码:    PUT    /sys/users/{id}/reset-password
修改密码:    PUT    /sys/users/change-password
```

**创建用户Request**：
```json
{
    "username": "zhangsan",
    "password": "initial_password",
    "realName": "张三",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "roleIds": [1, 2]
}
```

#### 4.4.3 角色权限管理
```
角色列表:    GET    /sys/roles
角色详情:    GET    /sys/roles/{id}
创建角色:    POST   /sys/roles
更新角色:    PUT    /sys/roles/{id}
删除角色:    DELETE /sys/roles/{id}
权限树:      GET    /sys/permissions/tree
分配权限:    PUT    /sys/roles/{id}/permissions
```

### 4.5 档案管理接口

#### 4.5.1 网关管理
```
网关列表:    GET    /iot/gateways?page=1&size=20&status=&keyword=
网关详情:    GET    /iot/gateways/{id}
创建网关:    POST   /iot/gateways
更新网关:    PUT    /iot/gateways/{id}
删除网关:    DELETE /iot/gateways/{id}
网关状态:    GET    /iot/gateways/{id}/status
```

**网关Request/Response**：
```json
{
    "gatewayNo": "GW2024001",
    "gatewayName": "1号楼采集网关",
    "gatewayType": "MODBUS_TCP",
    "manufacturerId": 1,
    "protocolCode": "modbus",
    "ipAddress": "192.168.1.100",
    "port": 502,
    "deviceLimit": 100,
    "heartbeatInterval": 60,
    "remark": ""
}
```

#### 4.5.2 表计管理
```
表计列表:        GET    /iot/meters?page=1&size=20&status=&type=&keyword=
表计详情:        GET    /iot/meters/{id}
创建表计:        POST   /iot/meters
更新表计:        PUT    /iot/meters/{id}
删除表计:        DELETE /iot/meters/{id}
绑定网关:        PUT    /iot/meters/{id}/bind-gateway
解绑网关:        PUT    /iot/meters/{id}/unbind-gateway
表计当前数据:     GET    /iot/meters/{id}/current-data
表计历史数据:     GET    /iot/meters/{id}/history-data?startTime=&endTime=
```

**表计Request/Response**：
```json
{
    "gatewayId": 101,
    "meterNo": "M20240001",
    "meterName": "1号楼1层电表",
    "meterType": 1,
    "manufacturerId": 2,
    "protocolCode": "modbus",
    "deviceAddress": "1",
    "ctRatio": 1.0,
    "ptRatio": 1.0,
    "meterRatio": 1.0,
    "address": "1号楼1层配电间",
    "longitude": 116.3974,
    "latitude": 39.9093,
    "installTime": "2024-01-15",
    "remark": ""
}
```

#### 4.5.3 群组管理
```
群组树:      GET    /iot/groups/tree
群组列表:    GET    /iot/groups
创建群组:    POST   /iot/groups
更新群组:    PUT    /iot/groups/{id}
删除群组:    DELETE /iot/groups/{id}
添加设备:    POST   /iot/groups/{id}/meters
移除设备:    DELETE /iot/groups/{id}/meters/{meterId}
群组设备列表: GET   /iot/groups/{id}/meters
```

#### 4.5.4 协议管理
```
协议列表:    GET    /iot/protocols
协议详情:    GET    /iot/protocols/{code}
创建协议:    POST   /iot/protocols
更新协议:    PUT    /iot/protocols/{code}
删除协议:    DELETE /iot/protocols/{code}
上传JAR包:   POST   /iot/protocols/{code}/upload
字段映射:    GET    /iot/protocols/{code}/fields
```

### 4.6 抄表数据接口

#### 4.6.1 当前数据查询
```
GET /iot/reading/current?page=1&size=20&meterType=&gatewayId=&groupId=

Response:
{
    "code": 200,
    "data": {
        "list": [
            {
                "meterId": 1001,
                "meterNo": "M20240001",
                "meterName": "1号楼电表",
                "meterType": 1,
                "gatewayId": 101,
                "readTime": "2026-03-29T10:30:00",
                "totalEnergy": 1234.56,
                "voltageA": 220.5,
                "voltageB": 221.0,
                "voltageC": 219.8,
                "currentA": 5.2,
                "currentB": 4.8,
                "currentC": 5.0,
                "powerActive": 3.45,
                "powerFactor": 0.95,
                "status": 1
            }
        ],
        "total": 1500
    }
}
```

#### 4.6.2 历史数据查询
```
GET /iot/reading/history?meterId=1001&startTime=2026-03-01T00:00:00&endTime=2026-03-29T23:59:59&page=1&size=100

Response:
{
    "code": 200,
    "data": {
        "list": [
            {
                "ts": "2026-03-29T10:00:00",
                "readingTime": "2026-03-29T10:00:00",
                "totalEnergy": 1234.50,
                "voltageA": 220.5,
                "currentA": 5.2,
                "powerActive": 3.45
            }
        ],
        "total": 720
    }
}
```

#### 4.6.3 数据导出
```
GET /iot/reading/export?meterIds=1001,1002&startTime=&endTime=&format=excel

Response: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

#### 4.6.4 抄表成功率统计
```
GET /iot/reading/statistics?startDate=2026-03-01&endDate=2026-03-29

Response:
{
    "code": 200,
    "data": {
        "dateRange": {
            "start": "2026-03-01",
            "end": "2026-03-29"
        },
        "summary": {
            "totalShouldRead": 43500,
            "totalActualRead": 42800,
            "averageRate": 98.39
        },
        "dailyStats": [
            {
                "date": "2026-03-29",
                "shouldRead": 1500,
                "actualRead": 1480,
                "rate": 98.67
            }
        ]
    }
}
```

---

## 5. 核心业务流程设计

### 5.1 设备接入流程

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    设备       │     │   前置通信层  │     │   连接管理器  │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       │ 1.建立TCP连接       │                    │
       │────────────────────▶│                    │
       │                    │                    │
       │ 2.发送认证包        │                    │
       │ (Device ID + Token)│                    │
       │────────────────────▶│                    │
       │                    │                    │
       │                    │ 3.验证认证信息      │
       │                    │ (查数据库/缓存)     │
       │                    ├────────────────────▶│
       │                    │                    │
       │                    │ 4.验证结果         │
       │                    │◀───────────────────│
       │                    │                    │
       │                    │ 5.注册连接         │
       │                    ├────────────────────▶│
       │                    │                    │
       │ 6.返回认证成功      │                    │
       │◀────────────────────│                    │
       │                    │                    │
       │ 7.保持心跳          │                    │
       │◀───────────────────▶│                    │
       │                    │                    │
       │ 8.上报数据          │                    │
       │────────────────────▶│                    │
       │                    │                    │
```

**详细时序**：

| 步骤 | 参与者 | 动作 | 说明 |
|------|--------|------|------|
| 1 | 设备 | 建立TCP连接 | 连接到平台TCP Server端口 |
| 2 | 设备 | 发送认证包 | 首包必须包含Device ID和Token |
| 3 | TCP Server | 验证认证 | DeviceAuthHandler处理 |
| 4 | TCP Server | 查询数据库 | 验证Token有效性 |
| 5 | ConnectionManager | 注册连接 | 建立deviceId -> channel映射 |
| 6 | TCP Server | 返回结果 | 成功/失败响应 |
| 7 | 设备/TCP Server | 心跳保活 | 按配置间隔发送心跳包 |
| 8 | 设备 | 上报数据 | 发送抄表数据报文 |

### 5.2 数据上报流程

```
┌────────┐   ┌────────────┐   ┌────────────┐   ┌────────────┐   ┌──────────┐
│ 设备    │   │ TCP Server │   │ 协议解析引擎 │   │ 数据缓冲队列 │   │ 数据存盘  │
└───┬────┘   └─────┬──────┘   └─────┬──────┘   └─────┬──────┘   └────┬─────┘
    │              │                │                │               │
    │ 1.上报报文    │                │                │               │
    │─────────────▶│                │                │               │
    │              │                │                │               │
    │              │ 2.提取字节流   │                │               │
    │              ├───────────────▶│                │               │
    │              │                │                │               │
    │              │                │ 3.协议识别     │               │
    │              │                │ (端口/特征)    │               │
    │              │                │                │               │
    │              │                │ 4.报文解码     │               │
    │              │                │ (ProtocolParser)│              │
    │              │                │                │               │
    │              │                │ 5.数据校验     │               │
    │              │                │ (CRC/长度/范围) │              │
    │              │                │                │               │
    │              │                │ 6.转换标准格式 │               │
    │              │                ├───────────────▶│               │
    │              │                │                │               │
    │              │                │                │ 7.写入RingBuffer│
    │              │                │                ├──────────────▶│
    │              │                │                │               │
    │              │                │                │ 8.批量写入     │
    │              │                │                │               │
    │              │                │                │   ┌─────────┐ │
    │              │                │                │   │TDengine │ │
    │              │                │                │   │MySQL    │ │
    │              │                │                │   │Redis    │ │
    │              │                │                │   └─────────┘ │
```

**数据流说明**：

1. **报文接收**：TCP Server接收设备上报的二进制报文
2. **字节提取**：从Channel读取ByteBuf字节流
3. **协议识别**：根据端口或报文头部特征识别协议类型
4. **报文解码**：调用对应ProtocolParser解析报文
5. **数据校验**：执行CRC、长度、数值范围等校验
6. **格式转换**：转换为统一的DeviceData对象
7. **写入队列**：推送到Disruptor RingBuffer
8. **批量存盘**：消费端批量写入TDengine/MySQL/Redis

### 5.3 指令下发流程

```
┌────────┐   ┌────────────┐   ┌────────────┐   ┌────────────┐   ┌────────┐
│ Web后台 │   │ 应用服务层  │   │ 连接管理器  │   │ 指令下发器  │   │  设备   │
└───┬────┘   └─────┬──────┘   └─────┬──────┘   └─────┬──────┘   └───┬────┘
    │              │                │                │               │
    │ 1.下发指令   │                │                │               │
    │─────────────▶│                │                │               │
    │              │                │                │               │
    │              │ 2.业务校验     │                │               │
    │              │ (权限/设备状态) │                │               │
    │              │                │                │               │
    │              ├───────────────▶│                │               │
    │              │                │                │               │
    │              │                │ 3.获取Channel │                │
    │              │                │ (设备在线检查) │               │
    │              │                │                │               │
    │              │                ├───────────────▶│               │
    │              │                │                │               │
    │              │                │                │ 4.协议编码    │
    │              │                │                │ (encode)      │
    │              │                │                │               │
    │              │                │                │ 5.发送报文    │
    │              │                │                ├──────────────▶│
    │              │                │                │               │
    │              │                │                │ 6.等待响应    │
    │              │                │                │◀──────────────│
    │              │                │                │               │
    │              │                │◀───────────────│               │
    │              │                │                │               │
    │◀─────────────│                │                │               │
    │              │                │                │               │
```

**指令下发详细流程**：

1. **Web请求**：用户在前端发起指令下发请求
2. **业务校验**：
   - 检查用户是否有权限
   - 检查设备是否存在且未禁用
   - 检查设备协议是否支持该指令
3. **获取连接**：从ConnectionManager获取设备Channel
   - 如果设备不在线，返回错误
4. **协议编码**：
   - 根据设备协议类型获取ProtocolParser
   - 调用encode方法将Command转换为ByteBuf
5. **发送报文**：通过Channel写入报文
6. **等待响应**：
   - 设置Promise等待设备响应
   - 超时时间：10秒（可配置）
   - 收到响应后返回结果给前端

---

## 6. 安全设计

### 6.1 认证与授权

```
┌─────────────────────────────────────────────────────────────┐
│                     认证授权架构                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   登录请求                    JWT Token                      │
│      │                    ┌────────────┐                   │
│      ▼                    │ Header     │                   │
│   ┌──────────┐            │  {         │                   │
│   │ 用户认证  │──────────▶│    "alg":  │                   │
│   │ (查数据库)│            │    "HS256" │                   │
│   └──────────┘            │  }         │                   │
│        │                  ├────────────┤                   │
│        │                  │ Payload    │                   │
│        ▼                  │  {         │                   │
│   ┌──────────┐            │    "sub":  │                   │
│   │ 生成Token │            │    "1001", │                   │
│   │ (JWT)    │            │    "tid":  │                   │
│   └──────────┘            │    "1",    │                   │
│        │                  │    "iat":  │                   │
│        │                  │    1234567 │                   │
│        ▼                  │  }         │                   │
│   返回Token               ├────────────┤                   │
│                          │ Signature  │                   │
│                          └────────────┘                   │
│                                                              │
│   后续请求                     权限校验                       │
│      │                           │                          │
│      ▼                           ▼                          │
│   Header:                   ┌──────────┐                   │
│   Authorization:            │ Shiro    │                   │
│   Bearer {token}    ───────▶│ Realm    │                   │
│                             │ 解析Token│                   │
│                             └──────────┘                   │
│                                  │                          │
│                                  ▼                          │
│                             ┌──────────┐                   │
│                             │ 权限注解  │                   │
│                             │ @Requires│                   │
│                             │ Permissions│                 │
│                             └──────────┘                   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 数据传输安全

- **TLS/SSL**：Web端使用HTTPS
- **Token机制**：JWT Token，2小时有效期
- **防重放攻击**：请求时间戳校验，5分钟窗口
- **敏感数据加密**：密码使用BCrypt加密存储

### 6.3 访问控制

- **RBAC模型**：基于角色的访问控制
- **多租户隔离**：数据级别租户隔离
- **API限流**：基于Token桶算法的限流
- **IP白名单**：设备接入IP白名单控制

---

## 7. 性能设计

### 7.1 并发设计

| 模块 | 并发策略 | 性能指标 |
|------|----------|----------|
| TCP Server | Netty NIO + 多线程 | 10万+并发连接 |
| UDP Server | Netty EventLoop | 5万+ QPS |
| 数据缓冲 | Disruptor无锁队列 | 100万+ TPS |
| 数据库写入 | 批量写入 + 连接池 | 10万+ 条/秒 |

### 7.2 缓存策略

```
┌─────────────────────────────────────────────────────────────┐
│                     多级缓存架构                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   L1: 本地缓存 (Caffeine)                                    │
│   ├─ 热点数据：设备配置、协议配置                             │
│   ├─ 过期策略：5分钟或手动刷新                                │
│   └─ 容量：10000条                                           │
│                                                              │
│   L2: 分布式缓存 (Redis)                                     │
│   ├─ 会话数据：用户Token、设备在线状态                         │
│   ├─ 统计数据：数据大屏统计                                   │
│   └─ 过期策略：根据业务设定                                   │
│                                                              │
│   L3: 数据库                                                 │
│   ├─ MySQL：关系型数据                                       │
│   ├─ TDengine：时序数据                                       │
│   └─ 持久化存储                                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 7.3 数据库优化

- **TDengine**：
  - 按设备自动分表
  - 时间范围查询优化
  - 数据订阅（用于实时推送）
  
- **MySQL**：
  - 租户ID作为分区键
  - 常用查询字段建立索引
  - 读写分离（主从复制）

---

## 8. 部署架构

### 8.1 单机部署

```
┌────────────────────────────────────────┐
│              服务器                    │
│  ┌────────────────────────────────┐   │
│  │         Nginx (反向代理)        │   │
│  │         端口: 80/443            │   │
│  └────────────────────────────────┘   │
│                  │                     │
│  ┌───────────────┴───────────────┐    │
│  │      Spring Boot 应用         │    │
│  │  ├─ Web API (8080)            │    │
│  │  ├─ TCP Server (8081)         │    │
│  │  └─ UDP Server (8082)         │    │
│  └───────────────────────────────┘    │
│                  │                     │
│  ┌───────────────┼───────────────┐    │
│  │               │               │    │
│  ▼               ▼               ▼    │
│  ┌────────┐  ┌────────┐  ┌────────┐  │
│  │TDengine│  │ MySQL  │  │ Redis  │  │
│  │ 6030   │  │ 3306   │  │ 6379   │  │
│  └────────┘  └────────┘  └────────┘  │
└────────────────────────────────────────┘
```

### 8.2 集群部署

```
┌─────────────────────────────────────────────────────────────────┐
│                         负载均衡层                               │
│                      (Nginx / LVS / F5)                         │
└─────────────────────────────┬───────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  前置机集群-1  │    │  前置机集群-2  │    │  前置机集群-N  │
│  ┌─────────┐  │    │  ┌─────────┐  │    │  ┌─────────┐  │
│  │Netty    │  │    │  │Netty    │  │    │  │Netty    │  │
│  │TCP/UDP  │  │    │  │TCP/UDP  │  │    │  │TCP/UDP  │  │
│  └─────────┘  │    │  └─────────┘  │    │  └─────────┘  │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       应用服务集群                               │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐         ┌─────────┐     │
│  │ App-1   │  │ App-2   │  │ App-3   │  ...    │ App-N   │     │
│  │ (Spring)│  │ (Spring)│  │ (Spring)│         │ (Spring)│     │
│  └─────────┘  └─────────┘  └─────────┘         └─────────┘     │
└─────────────────────────────┬───────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  TDengine     │    │    MySQL      │    │    Redis      │
│   集群         │    │   主从集群     │    │   Cluster     │
│  ┌───┬───┐    │    │  ┌───┬───┐    │    │  ┌───┬───┐    │
│  │M1 │M2 │    │    │  │Master│Slave│    │  │M1 │M2 │    │
│  └───┴───┘    │    │  └───┴───┘    │    │  └───┴───┘    │
└───────────────┘    └───────────────┘    └───────────────┘
```

---

## 9. 附录

### 9.1 术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| 前置机 | Front-end Server | 负责设备接入的服务器 |
| 网关 | Gateway | 采集设备数据的边缘设备 |
| 表计 | Meter | 电表/水表/气表/热表等计量设备 |
| 抄表 | Meter Reading | 采集表计数据的过程 |
| 协议 | Protocol | 设备通信的数据格式规范 |
| 报文 | Message | 设备与平台交换的数据包 |
| 心跳 | Heartbeat | 维持长连接的周期性数据包 |
| 时序数据 | Time Series Data | 带时间戳的连续数据 |

### 9.2 错误码定义

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 200 | 成功 | 200 |
| 400 | 请求参数错误 | 400 |
| 401 | 未授权/Token过期 | 401 |
| 403 | 禁止访问（无权限）| 403 |
| 404 | 资源不存在 | 404 |
| 500 | 服务器内部错误 | 500 |
| 1001 | 设备不在线 | 200 |
| 1002 | 设备认证失败 | 200 |
| 1003 | 指令下发超时 | 200 |
| 1004 | 协议解析错误 | 200 |

### 9.3 相关文档索引

- 《物联网平台需求文档（PRD）》- zfh-iot.md
- 《Netty高性能编程指南》- 待补充
- 《TDengine时序数据库使用手册》- 待补充
- 《协议扩展开发指南》- 待补充

---

*文档版本：v1.0*  
*创建日期：2026-03-29*  
*最后更新：2026-03-29*