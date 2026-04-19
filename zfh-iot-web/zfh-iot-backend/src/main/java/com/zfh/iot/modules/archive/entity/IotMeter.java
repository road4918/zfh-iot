package com.zfh.iot.modules.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("iot_meter")
public class IotMeter {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long gatewayId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long groupId;
    private String meterNo;
    private String meterName;
    private Integer meterType;
    private Long manufacturerId;
    private String protocolCode;
    private String deviceAddress;
    private BigDecimal ctRatio;
    private BigDecimal ptRatio;
    private BigDecimal meterRatio;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private LocalDate installTime;
    private Integer status;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime lastReadingTime;
    private String remark;
    
    @TableLogic
    private Integer deleted;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
