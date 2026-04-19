package com.zfh.iot.modules.stat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_command_daily")
public class StatCommandDaily {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private LocalDate statDate;
    private Integer deliveredCount;
    private Integer failedCount;
    private Integer successCount;
    private Integer overdueCount;
    private Integer timeoutCount;
    private Integer cancelledCount;
    private Integer waitingCount;
    private Integer sentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
