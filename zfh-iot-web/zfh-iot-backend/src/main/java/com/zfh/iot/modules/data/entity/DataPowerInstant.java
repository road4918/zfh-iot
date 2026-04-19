package com.zfh.iot.modules.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("data_power_instant")
public class DataPowerInstant implements Serializable {

    private Long pointId;

    private LocalDateTime dataTime;

    private BigDecimal p;

    private BigDecimal pa;

    private BigDecimal pb;

    private BigDecimal pc;

    private BigDecimal q;

    private BigDecimal qa;

    private BigDecimal qb;

    private BigDecimal qc;

    private BigDecimal s;

    private BigDecimal sa;

    private BigDecimal sb;

    private BigDecimal sc;

    private BigDecimal ia;

    private BigDecimal ib;

    private BigDecimal ic;

    private BigDecimal iz;

    private BigDecimal ua;

    private BigDecimal ub;

    private BigDecimal uc;

    private BigDecimal uab;

    private BigDecimal ubc;

    private BigDecimal uca;

    private BigDecimal pf;

    private BigDecimal pfa;

    private BigDecimal pfb;

    private BigDecimal pfc;

    private BigDecimal hz;

    private LocalDateTime addTime;

    private LocalDateTime updateTime;
}
