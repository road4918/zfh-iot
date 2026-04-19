package com.zfh.iot.modules.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("data_instant_realtime")
public class DataInstantRealtime implements Serializable {

    private Long pointId;

    private Short dataItem;

    private LocalDateTime dataTime;

    private BigDecimal value;

    private LocalDateTime updateTime;
}
