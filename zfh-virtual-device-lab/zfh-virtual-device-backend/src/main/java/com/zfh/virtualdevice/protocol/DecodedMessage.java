package com.zfh.virtualdevice.protocol;

import lombok.Data;

@Data
public class DecodedMessage {
    private MessageType type;
    private byte[] binaryData;
    private String mqttTopic;
    private String mqttPayload;
}
