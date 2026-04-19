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

@Autowired
private DeviceWebSocketController webSocketController;
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

// Save log asynchronously
logService.logAsync(commLog);

// Also send real-time update via WebSocket (without log ID since async)
webSocketController.sendCommLog(
    null, meterId, meter.getName(), 
    "UP", "MQTT", message.getMqttPayload(), 
    objectMapper.writeValueAsString(data)
);
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

- [ ] **Step 2: Create WebSocket controller and stats scheduler**

```java
package com.zfh.virtualdevice.websocket;

import com.zfh.virtualdevice.enums.DeviceStatus;
import com.zfh.virtualdevice.mapper.VirtualGatewayMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class DeviceWebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private VirtualGatewayMapper gatewayMapper;
    
    @Autowired
    private VirtualMeterMapper meterMapper;
    
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
    
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void sendStats() {
        long totalGateways = gatewayMapper.selectCount(null);
        long onlineGateways = gatewayMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>()
                .eq(com.zfh.virtualdevice.entity.VirtualGateway::getStatus, DeviceStatus.ONLINE));
        
        long totalMeters = meterMapper.selectCount(null);
        long onlineMeters = meterMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>()
                .eq(com.zfh.virtualdevice.entity.VirtualMeter::getStatus, DeviceStatus.ONLINE));
        
        Map<String, Object> message = Map.of(
            "type", "STATS_UPDATE",
            "onlineGateways", (int) onlineGateways,
            "totalGateways", (int) totalGateways,
            "onlineMeters", (int) onlineMeters,
            "totalMeters", (int) totalMeters,
            "todayUpMessages", 0,
            "todayDownMessages", 0
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

Use browser console with SockJS and STOMP:
```html
<!-- Open browser dev console on http://localhost:8080 -->
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
<script>
var socket = new SockJS('/ws/virtual-device');
var stompClient = Stomp.over(socket);
stompClient.connect({}, function(frame) {
    stompClient.subscribe('/topic/device-status', function(message) {
        console.log(JSON.parse(message.body));
    });
});
</script>
```

Start a device via API and verify real-time status update is received in browser console.

Alternative: Use curl for SockJS info endpoint:
```bash
curl http://localhost:8080/ws/virtual-device/info
```
Expected: JSON response with SockJS server info

- [ ] **Step 5: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/websocket/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/
git commit -m "feat: add WebSocket real-time updates"
```

---

