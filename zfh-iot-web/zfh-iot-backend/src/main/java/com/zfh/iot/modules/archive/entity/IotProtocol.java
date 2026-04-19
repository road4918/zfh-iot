package com.zfh.iot.modules.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_protocol")
public class IotProtocol {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private String protocolCode;
    private String protocolName;
    private Integer protocolType;
    private String version;
    private String jarPath;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
}
