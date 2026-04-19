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
                    log.info("Time sync received for device {}", ctx.getDeviceId());
                    break;
                case REMOTE_CONTROL:
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
