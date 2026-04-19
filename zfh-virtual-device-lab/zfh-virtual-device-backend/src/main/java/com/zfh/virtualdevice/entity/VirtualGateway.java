package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("virtual_gateway")
public class VirtualGateway {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String communicationAddress;
    
    @EnumValue
    private ProtocolType protocol;
    
    @EnumValue
    private CommMode commMode;
    
    private Integer serverPort;
    private String clientHost;
    private Integer clientPort;
    
    @EnumValue
    private DeviceStatus status;
    
    private String mqttBroker;
    private String mqttClientId;
    private String mqttUsername;
    private String mqttPassword;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
