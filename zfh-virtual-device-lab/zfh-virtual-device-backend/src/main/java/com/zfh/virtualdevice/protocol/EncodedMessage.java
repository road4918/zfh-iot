package com.zfh.virtualdevice.protocol;

import lombok.Data;

@Data
public class EncodedMessage {
    private MessageType type;
    private byte[] binaryData;
    private String mqttTopic;
    private String mqttPayload;
    private int mqttQos;
}
