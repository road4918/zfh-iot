package com.zfh.iot.modules.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("data_power_cumulant")
public class DataPowerCumulant implements Serializable {

    private Long pointId;

    private Short phaseType;

    private Short tariffType;

    private LocalDateTime dataTime;

    private BigDecimal forwardActive;

    private BigDecimal reverseActive;

    private BigDecimal combinedActive;

    private BigDecimal forwardReactive;

    private BigDecimal reverseReactive;

    private BigDecimal combinedReactive;

    private LocalDateTime addTime;

    private LocalDateTime updateTime;
}
