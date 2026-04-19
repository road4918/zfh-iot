package com.zfh.iot.modules.data.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReadingStatisticsVO {
    private String startDate;
    private String endDate;
    private Long totalShouldRead;
    private Long totalActualRead;
    private Double averageRate;
    private List<DailyStatVO> dailyStats;
}
