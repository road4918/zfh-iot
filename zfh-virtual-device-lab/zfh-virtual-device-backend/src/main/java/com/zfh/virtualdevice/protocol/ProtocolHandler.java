package com.zfh.virtualdevice.protocol;

import com.zfh.virtualdevice.enums.ProtocolType;

public interface ProtocolHandler {
    ProtocolType getProtocolType();
    
    EncodedMessage encode(DeviceData data, DeviceContext ctx);
    
    Command parseCommand(DecodedMessage message, DeviceContext ctx);
    
    void processCommand(DeviceContext ctx, Command cmd);
    
    void onConnected(DeviceContext ctx);
    
    void onDisconnected(DeviceContext ctx);
}
