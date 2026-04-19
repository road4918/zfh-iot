package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("meter_data_config")
public class MeterDataConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long meterId;
    private Long templateId;
    private String dataType;
    
    @EnumValue
    private DataCategory dataCategory;
    
    private BigDecimal currentValue;
    private String configParams;
    private String overrideParams;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
