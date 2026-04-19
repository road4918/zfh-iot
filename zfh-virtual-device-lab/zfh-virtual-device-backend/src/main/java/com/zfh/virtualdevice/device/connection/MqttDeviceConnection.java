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
    
    MqttClient getClient() {
        return client;
    }
}
