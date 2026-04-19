package com.zfh.iot.modules.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("data_cumulant_realtime")
public class DataCumulantRealtime implements Serializable {

    private Long pointId;

    private Short energyType;

    private Short dataItem;

    private LocalDateTime dataTime;

    private BigDecimal value;

    private Short batteryStatus;

    private LocalDateTime updateTime;
}
