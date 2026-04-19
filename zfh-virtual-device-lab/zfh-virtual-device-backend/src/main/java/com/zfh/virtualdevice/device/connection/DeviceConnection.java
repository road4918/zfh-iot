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
