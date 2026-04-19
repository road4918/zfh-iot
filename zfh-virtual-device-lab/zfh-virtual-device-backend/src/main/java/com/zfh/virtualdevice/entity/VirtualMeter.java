package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("virtual_meter")
public class VirtualMeter {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long gatewayId;
    private String name;
    
    @EnumValue
    private MeterType meterType;
    
    private String communicationAddress;
    
    @EnumValue
    private ProtocolType protocol;
    
    private String connectionMode;
    
    @EnumValue
    private DeviceStatus status;
    
    private Boolean autoReport;
    private Integer reportInterval;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
