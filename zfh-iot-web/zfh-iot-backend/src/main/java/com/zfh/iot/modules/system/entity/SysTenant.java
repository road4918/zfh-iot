package com.zfh.iot.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_tenant")
public class SysTenant {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private String tenantName;
    private String contactName;
    private String contactPhone;
    private Integer maxDevices;
    private Integer maxGateways;
    private Integer storageDays;
    private Integer status;
    
    @TableLogic
    private Integer deleted;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
