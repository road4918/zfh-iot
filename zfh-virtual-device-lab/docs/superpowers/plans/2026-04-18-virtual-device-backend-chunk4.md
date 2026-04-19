## Chunk 4: Device Connection Management

### Task 8: Device Connection Abstraction

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/connection/DeviceConnection.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/connection/MqttDeviceConnection.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/connection/ConnectionException.java`

Note: `ConnectionStatus.java` from the spec is consolidated into `DeviceStatus` enum for simplicity.

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
            
            // Set up callback for connection loss
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.error("MQTT connection lost for device {}", deviceId, cause);
                    status = DeviceStatus.ERROR;
                }
                
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    log.info("Message arrived on topic {} for device {}", topic, deviceId);
                }
                
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Delivery complete
                }
            });
            
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
    
    // Package-private access for connection manager
    MqttClient getClient() {
        return client;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
cd zfh-virtual-device-backend
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/connection/
git commit -m "feat: add device connection abstraction and MQTT implementation"
```

---

### Task 9: Device Lifecycle Manager

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/DeviceLifecycleManager.java`

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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DeviceLifecycleManager {
    
    @Autowired
    private VirtualGatewayMapper gatewayMapper;
    
    @Autowired
    private VirtualMeterMapper meterMapper;
    
    @org.springframework.beans.factory.annotation.Value("${virtual-device.mqtt.default-broker:tcp://localhost:1883}")
    private String defaultMqttBroker;
    
    private final Map<String, DeviceConnection> activeConnections = new ConcurrentHashMap<>();
    
    private String getDefaultMqttBroker() {
        return defaultMqttBroker;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        log.info("Application started, recovering device connections...");
        
        // Recover gateways that were ONLINE
        var onlineGateways = gatewayMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VirtualGateway>()
                .eq(VirtualGateway::getStatus, DeviceStatus.ONLINE)
        );
        for (VirtualGateway gateway : onlineGateways) {
            try {
                startGateway(gateway.getId());
            } catch (Exception e) {
                log.error("Failed to recover gateway {}", gateway.getId(), e);
            }
        }
        
        // Recover meters that were ONLINE
        var onlineMeters = meterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VirtualMeter>()
                .eq(VirtualMeter::getStatus, DeviceStatus.ONLINE)
        );
        for (VirtualMeter meter : onlineMeters) {
            try {
                startMeter(meter.getId());
            } catch (Exception e) {
                log.error("Failed to recover meter {}", meter.getId(), e);
            }
        }
        
        log.info("Device connection recovery completed");
    }
    
    public void startGateway(Long gatewayId) {
        String key = "GATEWAY_" + gatewayId;
        if (activeConnections.containsKey(key)) {
            log.warn("Gateway {} is already started", gatewayId);
            return;
        }
        
        VirtualGateway gateway = gatewayMapper.selectById(gatewayId);
        if (gateway == null) {
            log.error("Gateway not found: {}", gatewayId);
            return;
        }
        
        try {
            
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
        
        // Cascade stop: stop all meters associated with this gateway
        var meters = meterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VirtualMeter>()
                .eq(VirtualMeter::getGatewayId, gatewayId));
        for (VirtualMeter meter : meters) {
            stopMeter(meter.getId());
        }
        
        VirtualGateway gateway = gatewayMapper.selectById(gatewayId);
        if (gateway != null) {
            gateway.setStatus(DeviceStatus.OFFLINE);
            gatewayMapper.updateById(gateway);
        }
        
        log.info("Gateway {} stopped", gatewayId);
    }
    
    public void restartGateway(Long gatewayId) {
        log.info("Restarting gateway {}", gatewayId);
        stopGateway(gatewayId);
        startGateway(gatewayId);
    }
    
    public void startMeter(Long meterId) {
        String key = "METER_" + meterId;
        if (activeConnections.containsKey(key)) {
            log.warn("Meter {} is already started", meterId);
            return;
        }
        
        VirtualMeter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            log.error("Meter not found: {}", meterId);
            return;
        }
        
        try {
            
            if (meter.getProtocol() == ProtocolType.MQTT) {
                // For MVP, meters connect directly via MQTT using application config
                String broker = getDefaultMqttBroker(); // Read from application.yml virtual-device.mqtt.default-broker
                MqttDeviceConnection conn = new MqttDeviceConnection(
                    meterId,
                    DeviceType.METER,
                    broker,
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
        
        VirtualMeter meter = meterMapper.selectById(meterId);
        if (meter != null) {
            meter.setStatus(DeviceStatus.OFFLINE);
            meterMapper.updateById(meter);
        }
        
        log.info("Meter {} stopped", meterId);
    }
    
    @PreDestroy
    public void onShutdown() {
        log.info("Application shutting down, disconnecting all devices...");
        for (Map.Entry<String, DeviceConnection> entry : activeConnections.entrySet()) {
            try {
                entry.getValue().disconnect();
            } catch (Exception e) {
                log.error("Error disconnecting device {}", entry.getKey(), e);
            }
        }
        activeConnections.clear();
        
        // Update all ONLINE devices to OFFLINE in database
        gatewayMapper.update(null, 
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<VirtualGateway>()
                .set(VirtualGateway::getStatus, DeviceStatus.OFFLINE)
                .eq(VirtualGateway::getStatus, DeviceStatus.ONLINE));
        meterMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<VirtualMeter>()
                .set(VirtualMeter::getStatus, DeviceStatus.OFFLINE)
                .eq(VirtualMeter::getStatus, DeviceStatus.ONLINE));
        
        log.info("All devices disconnected and database status updated");
    }
    
    public DeviceConnection getConnection(Long deviceId, DeviceType type) {
        String key = type.name() + "_" + deviceId;
        return activeConnections.get(key);
    }
    
    public Map<Long, DeviceConnection> getActiveConnections() {
        Map<Long, DeviceConnection> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, DeviceConnection> entry : activeConnections.entrySet()) {
            Long deviceId = entry.getValue().getDeviceId();
            result.put(deviceId, entry.getValue());
        }
        return result;
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

Test gateway lifecycle:
```bash
# Create gateway
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

# Start gateway
curl -X POST http://localhost:8080/api/gateways/1/start

# Check status - should be ONLINE
curl http://localhost:8080/api/gateways/1
```

Test meter lifecycle:
```bash
# Create meter
curl -X POST http://localhost:8080/api/meters \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MQTT电表",
    "meterType": "ELECTRIC",
    "communicationAddress": "METER002",
    "protocol": "MQTT",
    "autoReport": true,
    "reportInterval": 5
  }'

# Start meter
curl -X POST http://localhost:8080/api/meters/1/start

# Check status - should be ONLINE
curl http://localhost:8080/api/meters/1

# Test duplicate start (should be ignored)
curl -X POST http://localhost:8080/api/meters/1/start

# Stop meter
curl -X POST http://localhost:8080/api/meters/1/stop

# Check status - should be OFFLINE
curl http://localhost:8080/api/meters/1
```

- [ ] **Step 4: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/manager/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/
git commit -m "feat: add device lifecycle manager with MQTT connection"
```

---

