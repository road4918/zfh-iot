package com.zfh.virtualdevice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zfh.virtualdevice.enums.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("communication_log")
public class CommunicationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @EnumValue
    private DeviceType deviceType;
    
    private Long deviceId;
    
    @EnumValue
    private Direction direction;
    
    private String protocol;
    private String rawData;
    private String parsedData;
    
    private LocalDateTime timestamp;
}
