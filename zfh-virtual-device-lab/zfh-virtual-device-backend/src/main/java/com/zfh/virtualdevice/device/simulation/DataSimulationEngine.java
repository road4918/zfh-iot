package com.zfh.virtualdevice.device.simulation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zfh.virtualdevice.device.connection.MqttDeviceConnection;
import com.zfh.virtualdevice.device.manager.DeviceLifecycleManager;
import com.zfh.virtualdevice.entity.CommunicationLog;
import com.zfh.virtualdevice.entity.MeterDataConfig;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.enums.DeviceType;
import com.zfh.virtualdevice.enums.Direction;
import com.zfh.virtualdevice.mapper.MeterDataConfigMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import com.zfh.virtualdevice.protocol.DeviceContext;
import com.zfh.virtualdevice.protocol.DeviceData;
import com.zfh.virtualdevice.protocol.EncodedMessage;
import com.zfh.virtualdevice.protocol.ProtocolFactory;
import com.zfh.virtualdevice.service.CommunicationLogService;
import com.zfh.virtualdevice.websocket.DeviceWebSocketController;
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
    
    @Autowired
    private CommunicationLogService logService;
    
    @Autowired
    private DeviceWebSocketController webSocketController;
    
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
            
            var connection = lifecycleManager.getConnection(meterId, DeviceType.METER);
            if (connection instanceof MqttDeviceConnection) {
                MqttDeviceConnection mqttConn = (MqttDeviceConnection) connection;
                var handler = protocolFactory.getHandler(meter.getProtocol());
                
                DeviceContext ctx = new DeviceContext();
                ctx.setDeviceId(meterId);
                ctx.setDeviceType(DeviceType.METER);
                ctx.setCommunicationAddress(meter.getCommunicationAddress());
                ctx.setProtocolType(meter.getProtocol());
                ctx.setMqttClient(mqttConn.getClient());
                
                EncodedMessage message = handler.encode(deviceData, ctx);
                mqttConn.sendJson(message.getMqttTopic(), message.getMqttPayload());
                
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
                
                // Send real-time update via WebSocket
                webSocketController.sendCommLog(
                    null, meterId, meter.getName(), 
                    "UP", "MQTT", message.getMqttPayload(), 
                    objectMapper.writeValueAsString(data)
                );
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
                Map<String, Object> paramMap = objectMapper.readValue(params, new TypeReference<Map<String, Object>>() {});
                Object unit = paramMap.get("unit");
                return unit != null ? unit.toString() : "";
            }
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }
}
