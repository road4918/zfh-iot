package com.zfh.iot.modules.stat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_message_daily")
public class StatMessageDaily {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private LocalDate statDate;
    private Long messageCount;
    private Long commandCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
