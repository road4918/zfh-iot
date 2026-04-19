package com.zfh.iot.modules.stat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_push_daily")
public class StatPushDaily {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private LocalDate statDate;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
