package com.zfh.virtualdevice.device.manager;

import com.zfh.virtualdevice.device.connection.*;
import com.zfh.virtualdevice.device.simulation.DataSimulationEngine;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.enums.*;
import com.zfh.virtualdevice.mapper.VirtualGatewayMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import com.zfh.virtualdevice.websocket.DeviceWebSocketController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${virtual-device.mqtt.default-broker:tcp://localhost:1883}")
    private String defaultMqttBroker;
    
    @Autowired
    private DataSimulationEngine simulationEngine;
    
    @Autowired
    private DeviceWebSocketController webSocketController;
    
    private final Map<String, DeviceConnection> activeConnections = new ConcurrentHashMap<>();
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        log.info("Application started, recovering device connections...");
        
        var onlineGateways = gatewayMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VirtualGateway>()
                .eq(VirtualGateway::getStatus, DeviceStatus.ONLINE));
        for (VirtualGateway gateway : onlineGateways) {
            try {
                startGateway(gateway.getId());
            } catch (Exception e) {
                log.error("Failed to recover gateway {}", gateway.getId(), e);
            }
        }
        
        var onlineMeters = meterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VirtualMeter>()
                .eq(VirtualMeter::getStatus, DeviceStatus.ONLINE));
        for (VirtualMeter meter : onlineMeters) {
            try {
                startMeter(meter.getId());
            } catch (Exception e) {
                log.error("Failed to recover meter {}", meter.getId(), e);
            }
        }
        
        log.info("Device connection recovery completed");
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
                
                webSocketController.sendDeviceStatus(gatewayId, "GATEWAY", DeviceStatus.ONLINE.name());
                
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
            webSocketController.sendDeviceStatus(gatewayId, "GATEWAY", DeviceStatus.OFFLINE.name());
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
                String broker = defaultMqttBroker;
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
                
                webSocketController.sendDeviceStatus(meterId, "METER", DeviceStatus.ONLINE.name());
                simulationEngine.startAutoReport(meterId);
                
                log.info("Meter {} started successfully", meterId);
            }
        } catch (Exception e) {
            log.error("Failed to start meter {}", meterId, e);
            meter.setStatus(DeviceStatus.ERROR);
            meterMapper.updateById(meter);
        }
    }
    
    public void stopMeter(Long meterId) {
        simulationEngine.stopAutoReport(meterId);
        
        String key = "METER_" + meterId;
        DeviceConnection conn = activeConnections.remove(key);
        if (conn != null) {
            conn.disconnect();
        }
        
        VirtualMeter meter = meterMapper.selectById(meterId);
        if (meter != null) {
            meter.setStatus(DeviceStatus.OFFLINE);
            meterMapper.updateById(meter);
            webSocketController.sendDeviceStatus(meterId, "METER", DeviceStatus.OFFLINE.name());
        }
        
        log.info("Meter {} stopped", meterId);
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
