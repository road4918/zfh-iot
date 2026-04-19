package com.zfh.iot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GatewayProtocolEnum {

    DLT645_2007("DLT645_2007", "DL/T645-2007扩展协议"),
    MQTT_ENERGY("MQTT_ENERGY", "MQTT能源管理物联协议");

    private final String code;
    private final String name;

    public static GatewayProtocolEnum getByCode(String code) {
        for (GatewayProtocolEnum protocol : values()) {
            if (protocol.getCode().equals(code)) {
                return protocol;
            }
        }
        return null;
    }
}
