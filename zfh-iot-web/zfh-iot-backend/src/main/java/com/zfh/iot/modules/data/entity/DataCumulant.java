package com.zfh.iot.modules.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("data_cumulant")
public class DataCumulant implements Serializable {

    private Long pointId;

    private Short energyType;

    private Short dataItem;

    private LocalDateTime dataTime;

    private BigDecimal value;

    private Short batteryStatus;

    private LocalDateTime addTime;

    private LocalDateTime updateTime;
}
