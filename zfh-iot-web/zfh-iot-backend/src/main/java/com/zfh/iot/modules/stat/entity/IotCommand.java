package com.zfh.iot.modules.stat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_command")
public class IotCommand {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long deviceId;
    private Long gatewayId;
    private String commandType;
    private String commandContent;
    private Integer status;
    private String responseContent;
    private LocalDateTime sendTime;
    private LocalDateTime ackTime;
    private LocalDateTime completeTime;
    private String remark;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static final int STATUS_WAITING = 0;
    public static final int STATUS_SENT = 1;
    public static final int STATUS_DELIVERED = 2;
    public static final int STATUS_SUCCESS = 3;
    public static final int STATUS_FAILED = 4;
    public static final int STATUS_TIMEOUT = 5;
    public static final int STATUS_OVERDUE = 6;
    public static final int STATUS_CANCELLED = 7;
}
