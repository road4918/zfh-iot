package com.zfh.iot.modules.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_gateway")
public class IotGateway {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String gatewayNo;
    private String gatewayName;
    private String gatewayType;
    private Long manufacturerId;
    private String protocolCode;
    private String commAddr;
    private String ipAddress;
    private Integer port;
    private Integer deviceLimit;
    private Integer heartbeatInterval;
    private String location;
    private Integer status;
    private LocalDateTime lastOnlineTime;
    private String remark;
    
    @TableLogic
    private Integer deleted;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
