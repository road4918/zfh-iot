package com.zfh.iot.modules.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.modules.archive.entity.IotMeter;
import com.zfh.iot.modules.archive.service.IotMeterService;
import com.zfh.iot.modules.data.entity.*;
import com.zfh.iot.modules.data.service.*;
import com.zfh.iot.modules.data.vo.CurrentReadingVO;
import com.zfh.iot.modules.data.vo.DailyStatVO;
import com.zfh.iot.modules.data.vo.ReadingStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingDataServiceImpl implements ReadingDataService {

    private final IotMeterService meterService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 历史数据服务
    private final DataPowerCumulantService powerCumulantService;
    private final DataPowerInstantService powerInstantService;
    private final DataCumulantService cumulantService;
    private final DataInstantService instantService;
    
    // 实时数据服务
    private final DataPowerCumulantRealtimeService powerCumulantRealtimeService;
    private final DataPowerInstantRealtimeService powerInstantRealtimeService;
    private final DataCumulantRealtimeService cumulantRealtimeService;
    private final DataInstantRealtimeService instantRealtimeService;

    @Override
    public PageResult<CurrentReadingVO> getCurrentReadings(Long page, Long size, 
                                                           Integer meterType, 
                                                           Long gatewayId, 
                                                           Long groupId) {
        List<IotMeter> meters = meterService.list();
        
        List<IotMeter> filteredMeters = meters.stream()
                .filter(m -> meterType == null || m.getMeterType().equals(meterType))
                .filter(m -> gatewayId == null || gatewayId.equals(m.getGatewayId()))
                .toList();
        
        List<CurrentReadingVO> list = new ArrayList<>();
        for (IotMeter meter : filteredMeters) {
            CurrentReadingVO vo = buildCurrentReadingVO(meter);
            list.add(vo);
        }
        
        int total = list.size();
        int fromIndex = (int) ((page - 1) * size);
        int toIndex = (int) Math.min(fromIndex + size, total);
        
        if (fromIndex >= total) {
            return PageResult.of(List.of(), total, page, size);
        }
        
        List<CurrentReadingVO> pageList = list.subList(fromIndex, toIndex);
        return PageResult.of(pageList, total, page, size);
    }
    
    private CurrentReadingVO buildCurrentReadingVO(IotMeter meter) {
        CurrentReadingVO vo = new CurrentReadingVO();
        BeanUtils.copyProperties(meter, vo);
        
        Long pointId = meter.getId();
        Integer meterType = meter.getMeterType();
        
        try {
            if (meterType == 1) {
                // 电表：优先从实时表查询
                LambdaQueryWrapper<DataPowerInstantRealtime> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DataPowerInstantRealtime::getPointId, pointId);
                DataPowerInstantRealtime instant = powerInstantRealtimeService.getOne(wrapper);
                
                if (instant != null) {
                    vo.setInstantDataTime(instant.getDataTime());
                    vo.setInstantReportTime(instant.getUpdateTime());
                    vo.setDataTime(instant.getDataTime());
                    vo.setReportTime(instant.getUpdateTime());
                    vo.setReadTime(java.sql.Timestamp.valueOf(instant.getDataTime()));
                    vo.setVoltageA(instant.getUa() != null ? instant.getUa().doubleValue() : null);
                    vo.setVoltageB(instant.getUb() != null ? instant.getUb().doubleValue() : null);
                    vo.setVoltageC(instant.getUc() != null ? instant.getUc().doubleValue() : null);
                    vo.setCurrentA(instant.getIa() != null ? instant.getIa().doubleValue() : null);
                    vo.setCurrentB(instant.getIb() != null ? instant.getIb().doubleValue() : null);
                    vo.setCurrentC(instant.getIc() != null ? instant.getIc().doubleValue() : null);
                    vo.setPowerActive(instant.getP() != null ? instant.getP().doubleValue() : null);
                    vo.setPowerReactive(instant.getQ() != null ? instant.getQ().doubleValue() : null);
                    vo.setPowerFactor(instant.getPf() != null ? instant.getPf().doubleValue() : null);
                    vo.setFrequency(instant.getHz() != null ? instant.getHz().doubleValue() : null);
                }

                // 查询实时累积量获取总电能
                LambdaQueryWrapper<DataPowerCumulantRealtime> cumWrapper = new LambdaQueryWrapper<>();
                cumWrapper.eq(DataPowerCumulantRealtime::getPointId, pointId)
                          .eq(DataPowerCumulantRealtime::getPhaseType, (short) 0)
                          .eq(DataPowerCumulantRealtime::getTariffType, (short) 0);
                DataPowerCumulantRealtime cumulant = powerCumulantRealtimeService.getOne(cumWrapper);
                if (cumulant != null) {
                    vo.setCumulantDataTime(cumulant.getDataTime());
                    vo.setCumulantReportTime(cumulant.getUpdateTime());
                    if (vo.getDataTime() == null) {
                        vo.setDataTime(cumulant.getDataTime());
                        vo.setReadTime(java.sql.Timestamp.valueOf(cumulant.getDataTime()));
                    }
                    if (vo.getReportTime() == null) {
                        vo.setReportTime(cumulant.getUpdateTime());
                    }
                    vo.setForwardActive(toDouble(cumulant.getForwardActive()));
                    vo.setReverseActive(toDouble(cumulant.getReverseActive()));
                    vo.setForwardReactive(toDouble(cumulant.getForwardReactive()));
                    vo.setReverseReactive(toDouble(cumulant.getReverseReactive()));
                    vo.setTotalEnergy(toDouble(cumulant.getForwardActive()));
                }
            } else {
                // 水/气/热表：优先从实时表查询
                LambdaQueryWrapper<DataCumulantRealtime> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DataCumulantRealtime::getPointId, pointId)
                       .eq(DataCumulantRealtime::getEnergyType, meterType.shortValue());
                List<DataCumulantRealtime> cumulants = cumulantRealtimeService.list(wrapper);

                DataCumulantRealtime cumulant = pickLatestCumulant(cumulants);
                if (cumulant != null) {
                    vo.setDataTime(cumulant.getDataTime());
                    vo.setReportTime(cumulant.getUpdateTime());
                    vo.setReadTime(java.sql.Timestamp.valueOf(cumulant.getDataTime()));
                    vo.setTotalEnergy(toDouble(cumulant.getValue()));
                    vo.setBatteryStatus(cumulant.getBatteryStatus() != null ? cumulant.getBatteryStatus().intValue() : null);
                }

                if (meterType == 2) {
                    LambdaQueryWrapper<DataInstantRealtime> instantWrapper = new LambdaQueryWrapper<>();
                    instantWrapper.eq(DataInstantRealtime::getPointId, pointId);
                    List<DataInstantRealtime> instantList = instantRealtimeService.list(instantWrapper);
                    DataInstantRealtime valveData = pickLatestInstant(instantList);
                    if (valveData != null) {
                        if (vo.getDataTime() == null) {
                            vo.setDataTime(valveData.getDataTime());
                            vo.setReadTime(java.sql.Timestamp.valueOf(valveData.getDataTime()));
                        }
                        if (vo.getReportTime() == null) {
                            vo.setReportTime(valveData.getUpdateTime());
                        }
                        vo.setValveStatus(formatValveStatus(valveData.getValue()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Query current data failed for meter {}: {}", pointId, e.getMessage());
        }
        
        return vo;
    }

    private Double toDouble(java.math.BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    private DataCumulantRealtime pickLatestCumulant(List<DataCumulantRealtime> list) {
        return list.stream()
                .max(Comparator.comparing(
                        item -> item.getDataTime() != null ? item.getDataTime() : LocalDateTime.MIN
                ))
                .orElse(null);
    }

    private DataInstantRealtime pickLatestInstant(List<DataInstantRealtime> list) {
        return list.stream()
                .max(Comparator.comparing(
                        item -> item.getDataTime() != null ? item.getDataTime() : LocalDateTime.MIN
                ))
                .orElse(null);
    }

    private String formatValveStatus(java.math.BigDecimal value) {
        if (value == null) {
            return null;
        }

        int status = value.intValue();
        if (status == 0) {
            return "关阀";
        }
        if (status == 1) {
            return "开阀";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    @Override
    public PageResult<MeterReading> getHistoryReadings(Long meterId, 
                                                         LocalDateTime startTime, 
                                                         LocalDateTime endTime,
                                                         Long page, 
                                                         Long size) {
        IotMeter meter = meterService.getById(meterId);
        if (meter == null) {
            return PageResult.of(List.of(), 0L, page, size);
        }
        
        // 暂时返回空数据，实际使用时需要根据表计类型查询对应的历史数据表
        // 由于实体结构不同，需要设计统一的VO或者分别处理
        log.info("History data query for meter {} from {} to {}", meterId, startTime, endTime);
        
        return PageResult.of(List.of(), 0L, page, size);
    }

    @Override
    public ReadingStatisticsVO getReadingStatistics(String startDate, String endDate) {
        ReadingStatisticsVO vo = new ReadingStatisticsVO();
        vo.setStartDate(startDate);
        vo.setEndDate(endDate);
        
        long totalMeters = meterService.count();
        
        int days = (int) java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.parse(startDate),
            LocalDate.parse(endDate)
        ) + 1;
        
        long shouldRead = totalMeters * days;
        
        // 统计电表历史数据条数
        long electricCount = 0;
        try {
            LambdaQueryWrapper<DataPowerCumulant> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(DataPowerCumulant::getDataTime, startDate + " 00:00:00")
                   .le(DataPowerCumulant::getDataTime, endDate + " 23:59:59");
            electricCount = powerCumulantService.count(wrapper);
        } catch (Exception e) {
            log.error("Query electric reading statistics failed: {}", e.getMessage());
        }
        
        // 统计水/气/热表历史数据条数
        long otherCount = 0;
        try {
            LambdaQueryWrapper<DataCumulant> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(DataCumulant::getDataTime, startDate + " 00:00:00")
                   .le(DataCumulant::getDataTime, endDate + " 23:59:59");
            otherCount = cumulantService.count(wrapper);
        } catch (Exception e) {
            log.error("Query other reading statistics failed: {}", e.getMessage());
        }
        
        vo.setTotalShouldRead(shouldRead);
        vo.setTotalActualRead(electricCount + otherCount);
        vo.setAverageRate(shouldRead > 0 ? 
                (double) vo.getTotalActualRead() / shouldRead * 100 : 0);
        
        List<DailyStatVO> dailyStats = new ArrayList<>();
        vo.setDailyStats(dailyStats);
        
        return vo;
    }

    @Override
    public MeterReading getMeterCurrentData(Long meterId) {
        // 从Redis缓存获取
        String cacheKey = "device:lastdata:" + meterId;
        MeterReading reading = (MeterReading) redisTemplate.opsForValue().get(cacheKey);
        
        if (reading == null) {
            IotMeter meter = meterService.getById(meterId);
            if (meter == null) {
                return null;
            }
            
            reading = new MeterReading();
            reading.setMeterId(meterId);
            reading.setMeterType(meter.getMeterType());
            reading.setTenantId(meter.getTenantId());
            reading.setGatewayId(meter.getGatewayId());
            
            // 根据表计类型从实时表查询最新数据
            try {
                if (meter.getMeterType() == 1) {
                    // 电表
                    LambdaQueryWrapper<DataPowerInstantRealtime> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(DataPowerInstantRealtime::getPointId, meterId);
                    DataPowerInstantRealtime instant = powerInstantRealtimeService.getOne(wrapper);
                    
                    if (instant != null) {
                        reading.setTs(java.sql.Timestamp.valueOf(instant.getDataTime()));
                        reading.setReadingTime(java.sql.Timestamp.valueOf(instant.getDataTime()));
                        reading.setVoltageA(instant.getUa() != null ? instant.getUa().doubleValue() : null);
                        reading.setVoltageB(instant.getUb() != null ? instant.getUb().doubleValue() : null);
                        reading.setVoltageC(instant.getUc() != null ? instant.getUc().doubleValue() : null);
                        reading.setCurrentA(instant.getIa() != null ? instant.getIa().doubleValue() : null);
                        reading.setCurrentB(instant.getIb() != null ? instant.getIb().doubleValue() : null);
                        reading.setCurrentC(instant.getIc() != null ? instant.getIc().doubleValue() : null);
                        reading.setPowerActive(instant.getP() != null ? instant.getP().doubleValue() : null);
                        reading.setPowerReactive(instant.getQ() != null ? instant.getQ().doubleValue() : null);
                        reading.setPowerFactor(instant.getPf() != null ? instant.getPf().doubleValue() : null);
                        reading.setFrequency(instant.getHz() != null ? instant.getHz().doubleValue() : null);
                    }
                } else {
                    // 水/气/热表
                    LambdaQueryWrapper<DataCumulantRealtime> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(DataCumulantRealtime::getPointId, meterId)
                           .eq(DataCumulantRealtime::getEnergyType, meter.getMeterType().shortValue());
                    DataCumulantRealtime cumulant = cumulantRealtimeService.getOne(wrapper);
                    
                    if (cumulant != null) {
                        reading.setTs(java.sql.Timestamp.valueOf(cumulant.getDataTime()));
                        reading.setReadingTime(java.sql.Timestamp.valueOf(cumulant.getDataTime()));
                        reading.setTotalEnergy(cumulant.getValue() != null ? cumulant.getValue().doubleValue() : null);
                    }
                }
            } catch (Exception e) {
                log.error("Query meter current data failed: {}", e.getMessage());
            }
        }
        
        return reading;
    }
}
