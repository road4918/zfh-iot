package com.zfh.iot.modules.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_manufacturer")
public class IotManufacturer {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String manufacturerName;
    private String contactName;
    private String contactPhone;
    private String address;
    private Integer status;
    
    @TableLogic
    private Integer deleted;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
