package com.zfh.iot.modules.data.service;

import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.modules.data.entity.MeterReading;
import com.zfh.iot.modules.data.vo.CurrentReadingVO;
import com.zfh.iot.modules.data.vo.ReadingStatisticsVO;

import java.time.LocalDateTime;

public interface ReadingDataService {
    
    PageResult<CurrentReadingVO> getCurrentReadings(Long page, Long size, 
                                                     Integer meterType, 
                                                     Long gatewayId, 
                                                     Long groupId);
    
    PageResult<MeterReading> getHistoryReadings(Long meterId, 
                                                 LocalDateTime startTime, 
                                                 LocalDateTime endTime,
                                                 Long page, 
                                                 Long size);
    
    ReadingStatisticsVO getReadingStatistics(String startDate, String endDate);
    
    MeterReading getMeterCurrentData(Long meterId);
}
