package com.zfh.iot.modules.data.entity;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class MeterReading {
    private Timestamp ts;
    private Timestamp readingTime;
    private Double totalEnergy;
    private String totalEnergyUnit;
    private Double voltageA;
    private Double voltageB;
    private Double voltageC;
    private Double currentA;
    private Double currentB;
    private Double currentC;
    private Double powerActive;
    private Double powerReactive;
    private Double powerFactor;
    private Double frequency;
    private Double temperature;
    private Double pressure;
    private Double flowRate;
    private Integer signalQuality;
    private Integer batteryLevel;
    private String rawData;
    private Long tenantId;
    private Long meterId;
    private Integer meterType;
    private Long gatewayId;
}
