package com.zfh.virtualdevice.protocol.factory;

import com.zfh.virtualdevice.enums.ProtocolType;
import com.zfh.virtualdevice.protocol.ProtocolHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProtocolFactory {
    
    @Autowired
    private List<ProtocolHandler> handlers;
    
    private final Map<ProtocolType, ProtocolHandler> handlerMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        for (ProtocolHandler handler : handlers) {
            handlerMap.put(handler.getProtocolType(), handler);
        }
    }
    
    public ProtocolHandler getHandler(ProtocolType type) {
        ProtocolHandler handler = handlerMap.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for protocol: " + type);
        }
        return handler;
    }
}
