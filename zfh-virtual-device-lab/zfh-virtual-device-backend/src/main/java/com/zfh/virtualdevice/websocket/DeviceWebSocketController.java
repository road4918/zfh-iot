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
    
    @Scheduled(fixedRate = 5000)
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
