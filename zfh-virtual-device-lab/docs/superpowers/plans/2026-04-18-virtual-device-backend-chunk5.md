## Chunk 5: Data Simulation Engine

### Task 10: Data Simulation Engine

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/simulation/DataSimulationEngine.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/device/simulation/DataGenerator.java`

- [ ] **Step 1: Create data generator**

```java
package com.zfh.virtualdevice.device.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zfh.virtualdevice.entity.MeterDataConfig;
import com.zfh.virtualdevice.enums.DataCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
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
            ObjectMapper mapper = new ObjectMapper();
            
            // First check override_params (priority)
            String overrideParams = config.getOverrideParams();
            if (overrideParams != null && !overrideParams.isEmpty()) {
                Map<String, Object> overrideMap = mapper.readValue(overrideParams, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                Object value = overrideMap.get(key);
                if (value != null) {
                    return new BigDecimal(value.toString());
                }
            }
            
            // Then check config_params
            String params = config.getConfigParams();
            if (params != null && !params.isEmpty()) {
                Map<String, Object> paramMap = mapper.readValue(params, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                Object value = paramMap.get(key);
                if (value != null) {
                    return new BigDecimal(value.toString());
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
                
                // Build device context for protocol handler
                DeviceContext ctx = new DeviceContext();
                ctx.setDeviceId(meterId);
                ctx.setDeviceType(DeviceType.METER);
                ctx.setCommunicationAddress(meter.getCommunicationAddress());
                ctx.setProtocolType(meter.getProtocol());
                ctx.setMqttClient(mqttConn.getClient());
                
                EncodedMessage message = handler.encode(deviceData, ctx);
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
        try {
            String params = config.getConfigParams();
            if (params != null && !params.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> paramMap = mapper.readValue(params, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                Object unit = paramMap.get("unit");
                return unit != null ? unit.toString() : "";
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

