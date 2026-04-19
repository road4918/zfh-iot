package com.zfh.iot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GatewayTypeEnum {

    TCP_DIRECT("TCP_DIRECT", "TCP直连"),
    MQTT("MQTT", "MQTT");

    private final String code;
    private final String name;

    public static GatewayTypeEnum getByCode(String code) {
        for (GatewayTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
