package com.zfh.virtualdevice.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zfh.virtualdevice.entity.VirtualGateway;
import com.zfh.virtualdevice.entity.VirtualMeter;
import com.zfh.virtualdevice.enums.DeviceStatus;
import com.zfh.virtualdevice.mapper.VirtualGatewayMapper;
import com.zfh.virtualdevice.mapper.VirtualMeterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
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
        Map<String, Object> message = new HashMap<>();
        message.put("type", "DEVICE_STATUS");
        message.put("deviceType", deviceType);
        message.put("deviceId", deviceId);
        message.put("status", status);
        message.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/device-status", message);
    }
    
    public void sendCommLog(Long logId, Long deviceId, String deviceName, 
                           String direction, String protocol, String rawData, String parsedData) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "COMM_LOG");
        message.put("logId", logId);
        message.put("deviceType", "METER");
        message.put("deviceId", deviceId);
        message.put("deviceName", deviceName);
        message.put("direction", direction);
        message.put("protocol", protocol);
        message.put("rawData", rawData);
        message.put("parsedData", parsedData);
        message.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/comm-logs", message);
    }
    
    @Scheduled(fixedRate = 5000)
    public void sendStats() {
        long totalGateways = gatewayMapper.selectCount(null);
        long onlineGateways = gatewayMapper.selectCount(
            new LambdaQueryWrapper<VirtualGateway>()
                .eq(VirtualGateway::getStatus, DeviceStatus.ONLINE));
        
        long totalMeters = meterMapper.selectCount(null);
        long onlineMeters = meterMapper.selectCount(
            new LambdaQueryWrapper<VirtualMeter>()
                .eq(VirtualMeter::getStatus, DeviceStatus.ONLINE));
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "STATS_UPDATE");
        message.put("onlineGateways", (int) onlineGateways);
        message.put("totalGateways", (int) totalGateways);
        message.put("onlineMeters", (int) onlineMeters);
        message.put("totalMeters", (int) totalMeters);
        message.put("todayUpMessages", 0);
        message.put("todayDownMessages", 0);
        messagingTemplate.convertAndSend("/topic/stats", message);
    }
}
