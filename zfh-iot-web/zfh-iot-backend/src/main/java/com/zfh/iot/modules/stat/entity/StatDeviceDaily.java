package com.zfh.iot.modules.stat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_device_daily")
public class StatDeviceDaily {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private LocalDate statDate;
    private Integer totalCount;
    private Integer onlineCount;
    private Integer offlineCount;
    private Integer abnormalCount;
    private Integer inactiveCount;
    private BigDecimal onlineRate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
