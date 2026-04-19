package com.zfh.iot.modules.data.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.sql.Timestamp;

@Data
public class CurrentReadingVO {
    private Long meterId;
    private String meterNo;
    private String meterName;
    private Integer meterType;
    private Long gatewayId;
    private Timestamp readTime;
    private LocalDateTime dataTime;
    private LocalDateTime reportTime;
    private LocalDateTime cumulantDataTime;
    private LocalDateTime cumulantReportTime;
    private LocalDateTime instantDataTime;
    private LocalDateTime instantReportTime;
    private Double totalEnergy;
    private Double forwardActive;
    private Double reverseActive;
    private Double forwardReactive;
    private Double reverseReactive;
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
    private Integer batteryStatus;
    private String valveStatus;
    private Integer status;
}
