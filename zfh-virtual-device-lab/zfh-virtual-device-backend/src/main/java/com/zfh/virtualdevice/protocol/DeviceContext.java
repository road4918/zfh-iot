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
        throw new UnsupportedOperationException("sendJson will be implemented in DeviceConnection layer");
    }
    
    public BigDecimal getMeterData(String dataType) {
        throw new UnsupportedOperationException("getMeterData will be implemented in DataSimulationEngine");
    }
    
    public void updateParams(Map<String, Object> params) {
        if (attributes != null) {
            attributes.putAll(params);
        }
    }
}
