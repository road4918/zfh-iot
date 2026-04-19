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
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/factory/ProtocolFactory.java`

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
        // This will be implemented in Chunk 4 (Device Connection Management)
        // The connection manager will inject the actual MQTT client
        throw new UnsupportedOperationException("sendJson will be implemented in DeviceConnection layer");
    }
    
    public BigDecimal getMeterData(String dataType) {
        // This will be implemented in Chunk 5 (Data Simulation Engine)
        // The simulation engine will inject MeterDataConfig repository
        throw new UnsupportedOperationException("getMeterData will be implemented in DataSimulationEngine");
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
package com.zfh.virtualdevice.protocol.factory;

import com.zfh.virtualdevice.enums.ProtocolType;
import com.zfh.virtualdevice.protocol.ProtocolHandler;
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
        ProtocolHandler handler = handlerMap.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for protocol: " + type);
        }
        return handler;
    }
}
```

- [ ] **Step 4: Verify compilation**

Run:
```bash
cd zfh-virtual-device-backend
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/factory/
git commit -m "feat: add protocol framework core"
```

---

### Task 7: MQTT+JSON Protocol Handler

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/handler/MqttJsonHandler.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/codec/` (empty package for future binary protocol codecs)

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
                    // MVP: Time sync acknowledged but no actual clock update
                    log.info("Time sync received for device {}", ctx.getDeviceId());
                    break;
                case REMOTE_CONTROL:
                    // MVP: Remote control not supported for MQTT+JSON protocol
                    response.put("status", "UNSUPPORTED");
                    response.put("message", "Remote control not supported in MQTT+JSON protocol");
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
        
        // Subscribe to command topic
        if (ctx.getMqttClient() != null && ctx.getMqttClient().isConnected()) {
            String commandTopic = "command/" + ctx.getDeviceType().name().toLowerCase() + "/" + ctx.getDeviceId();
            try {
                ctx.getMqttClient().subscribe(commandTopic, 1);
                log.info("Subscribed to command topic: {}", commandTopic);
            } catch (Exception e) {
                log.error("Failed to subscribe to command topic: {}", commandTopic, e);
            }
        }
    }
    
    @Override
    public void onDisconnected(DeviceContext ctx) {
        log.info("MQTT device disconnected: {}", ctx.getDeviceId());
    }
}
```

- [ ] **Step 2: Verify compilation**

Run:
```bash
cd zfh-virtual-device-backend
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/protocol/handler/
git commit -m "feat: add MQTT+JSON protocol handler"
```

---

