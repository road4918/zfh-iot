package com.zfh.iot.modules.data.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class RealTimeDataVO {
    private Long meterId;
    private String meterNo;
    private String meterName;
    private Integer meterType;
    private Long gatewayId;
    private Timestamp readTime;
    private Double totalEnergy;
    private Integer status;
}
