package com.zfh.iot.modules.data.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.modules.archive.entity.IotMeter;
import com.zfh.iot.modules.archive.service.IotMeterService;
import com.zfh.iot.modules.data.vo.*;
import com.zfh.iot.modules.stat.entity.*;
import com.zfh.iot.modules.stat.service.*;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IotMeterService meterService;
    private final StatDeviceDailyService deviceDailyService;
    private final StatMessageDailyService messageDailyService;
    private final StatPushDailyService pushDailyService;
    private final StatCommandDailyService commandDailyService;

    @GetMapping("/statistics")
    public Result<DashboardStatisticsVO> getStatistics(
            @RequestParam(required = false) Long tenantId) {

        DashboardStatisticsVO vo = new DashboardStatisticsVO();

        long total = meterService.count(new LambdaQueryWrapper<IotMeter>()
                .eq(tenantId != null, IotMeter::getTenantId, tenantId));
        long online = meterService.count(new LambdaQueryWrapper<IotMeter>()
                .eq(IotMeter::getStatus, 1)
                .eq(tenantId != null, IotMeter::getTenantId, tenantId));
        long offline = meterService.count(new LambdaQueryWrapper<IotMeter>()
                .eq(IotMeter::getStatus, 0)
                .eq(tenantId != null, IotMeter::getTenantId, tenantId));
        long inactive = meterService.count(new LambdaQueryWrapper<IotMeter>()
                .eq(IotMeter::getStatus, 2)
                .eq(tenantId != null, IotMeter::getTenantId, tenantId));
        long abnormal = meterService.count(new LambdaQueryWrapper<IotMeter>()
                .eq(IotMeter::getStatus, 3)
                .eq(tenantId != null, IotMeter::getTenantId, tenantId));

        DashboardStatisticsVO.DeviceStat deviceStat = new DashboardStatisticsVO.DeviceStat();
        deviceStat.setTotal(total);
        deviceStat.setOnline(online);
        deviceStat.setOffline(offline);
        deviceStat.setAbnormal(abnormal);
        deviceStat.setInactive(inactive);
        vo.setDevices(deviceStat);

        DashboardStatisticsVO.MessageCommandStat msgStat = new DashboardStatisticsVO.MessageCommandStat();
        List<StatMessageDaily> msgList = messageDailyService.list(
                new LambdaQueryWrapper<StatMessageDaily>()
                        .eq(tenantId != null, StatMessageDaily::getTenantId, tenantId));
        msgStat.setMessageCount(msgList.stream().mapToLong(StatMessageDaily::getMessageCount).sum());
        msgStat.setCommandCount(msgList.stream().mapToLong(StatMessageDaily::getCommandCount).sum());
        vo.setMessages(msgStat);

        return Result.success(vo);
    }

    @GetMapping("/device-trend")
    public Result<List<TrendDataVO>> getDeviceTrend(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Long tenantId) {
        int normalizedDays = normalizeDays(days);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(normalizedDays - 1);
        Map<LocalDate, StatDeviceDaily> statMap = queryDeviceDailyStats(tenantId, normalizedDays).stream()
                .collect(Collectors.toMap(StatDeviceDaily::getStatDate, s -> s, (first, second) -> second, LinkedHashMap::new));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<TrendDataVO> list = buildDateRange(startDate, endDate).stream().map(date -> {
            StatDeviceDaily stat = statMap.get(date);
            TrendDataVO vo = new TrendDataVO();
            vo.setDate(date.format(formatter));
            vo.setTotal(stat != null ? (long) stat.getTotalCount() : 0L);
            vo.setOnline(stat != null ? (long) stat.getOnlineCount() : 0L);
            return vo;
        }).toList();

        return Result.success(list);
    }

    @GetMapping("/online-rate")
    public Result<List<OnlineRateVO>> getOnlineRate(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Long tenantId) {
        int normalizedDays = normalizeDays(days);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(normalizedDays - 1);
        Map<LocalDate, StatDeviceDaily> statMap = queryDeviceDailyStats(tenantId, normalizedDays).stream()
                .collect(Collectors.toMap(StatDeviceDaily::getStatDate, s -> s, (first, second) -> second, LinkedHashMap::new));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<OnlineRateVO> list = buildDateRange(startDate, endDate).stream().map(date -> {
            StatDeviceDaily stat = statMap.get(date);
            OnlineRateVO vo = new OnlineRateVO();
            vo.setDate(date.format(formatter));
            double onRate = stat != null && stat.getOnlineRate() != null ? stat.getOnlineRate().doubleValue() : 0.0;
            vo.setOnlineRate(onRate);
            double offlineRate = 100.0 - onRate;
            vo.setOfflineRate(Math.round(offlineRate * 100.0) / 100.0);
            return vo;
        }).toList();

        return Result.success(list);
    }

    @GetMapping("/device-data-stat")
    public Result<List<DeviceDataStatVO>> getDeviceDataStat(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Long tenantId) {
        int normalizedDays = normalizeDays(days);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(normalizedDays - 1);
        Map<LocalDate, StatDeviceDaily> statMap = queryDeviceDailyStats(tenantId, normalizedDays).stream()
                .collect(Collectors.toMap(StatDeviceDaily::getStatDate, s -> s, (first, second) -> second, LinkedHashMap::new));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<DeviceDataStatVO> list = buildDateRange(startDate, endDate).stream().map(date -> {
            StatDeviceDaily stat = statMap.get(date);
            DeviceDataStatVO vo = new DeviceDataStatVO();
            vo.setDate(date.format(formatter));
            vo.setAbnormal(stat != null ? (long) stat.getAbnormalCount() : 0L);
            vo.setOffline(stat != null ? (long) stat.getOfflineCount() : 0L);
            vo.setInactive(stat != null ? (long) stat.getInactiveCount() : 0L);
            return vo;
        }).toList();

        return Result.success(list);
    }

    @GetMapping("/push-stat")
    public Result<List<PushStatVO>> getPushStat(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Long tenantId) {
        int normalizedDays = normalizeDays(days);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(normalizedDays - 1);

        LambdaQueryWrapper<StatPushDaily> wrapper = new LambdaQueryWrapper<StatPushDaily>()
                .ge(StatPushDaily::getStatDate, startDate)
                .le(StatPushDaily::getStatDate, endDate)
                .orderByAsc(StatPushDaily::getStatDate);
        if (tenantId != null) {
            wrapper.eq(StatPushDaily::getTenantId, tenantId);
        }

        Map<LocalDate, StatPushDaily> statMap = pushDailyService.list(wrapper).stream()
                .collect(Collectors.toMap(StatPushDaily::getStatDate, s -> s, (first, second) -> second, LinkedHashMap::new));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<PushStatVO> list = buildDateRange(startDate, endDate).stream().map(date -> {
            StatPushDaily stat = statMap.get(date);
            PushStatVO vo = new PushStatVO();
            vo.setDate(date.format(formatter));
            vo.setTotal(stat != null ? (long) stat.getTotalCount() : 0L);
            vo.setSuccess(stat != null ? (long) stat.getSuccessCount() : 0L);
            vo.setFail(stat != null ? (long) stat.getFailCount() : 0L);
            return vo;
        }).toList();

        return Result.success(list);
    }

    @GetMapping("/command-status")
    public Result<List<CommandStatusStatVO>> getCommandStatus(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Long tenantId) {
        int normalizedDays = normalizeDays(days);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(normalizedDays - 1);

        LambdaQueryWrapper<StatCommandDaily> wrapper = new LambdaQueryWrapper<StatCommandDaily>()
                .ge(StatCommandDaily::getStatDate, startDate)
                .le(StatCommandDaily::getStatDate, endDate)
                .orderByAsc(StatCommandDaily::getStatDate);
        if (tenantId != null) {
            wrapper.eq(StatCommandDaily::getTenantId, tenantId);
        }

        Map<LocalDate, StatCommandDaily> statMap = commandDailyService.list(wrapper).stream()
                .collect(Collectors.toMap(StatCommandDaily::getStatDate, s -> s, (first, second) -> second, LinkedHashMap::new));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<CommandStatusStatVO> list = buildDateRange(startDate, endDate).stream().map(date -> {
            StatCommandDaily stat = statMap.get(date);
            CommandStatusStatVO vo = new CommandStatusStatVO();
            vo.setDate(date.format(formatter));
            vo.setDelivered(stat != null ? (long) stat.getDeliveredCount() : 0L);
            vo.setFailed(stat != null ? (long) stat.getFailedCount() : 0L);
            vo.setSuccess(stat != null ? (long) stat.getSuccessCount() : 0L);
            vo.setOverdue(stat != null ? (long) stat.getOverdueCount() : 0L);
            vo.setTimeout(stat != null ? (long) stat.getTimeoutCount() : 0L);
            vo.setCancelled(stat != null ? (long) stat.getCancelledCount() : 0L);
            vo.setWaiting(stat != null ? (long) stat.getWaitingCount() : 0L);
            vo.setSent(stat != null ? (long) stat.getSentCount() : 0L);
            return vo;
        }).toList();

        return Result.success(list);
    }

    private List<StatDeviceDaily> queryDeviceDailyStats(Long tenantId, int days) {
        int normalizedDays = normalizeDays(days);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(normalizedDays - 1);

        LambdaQueryWrapper<StatDeviceDaily> wrapper = new LambdaQueryWrapper<StatDeviceDaily>()
                .ge(StatDeviceDaily::getStatDate, startDate)
                .le(StatDeviceDaily::getStatDate, endDate)
                .orderByAsc(StatDeviceDaily::getStatDate);
        if (tenantId != null) {
            wrapper.eq(StatDeviceDaily::getTenantId, tenantId);
        }

        return deviceDailyService.list(wrapper);
    }

    private int normalizeDays(Integer days) {
        return days == null || days < 1 ? 7 : days;
    }

    private List<LocalDate> buildDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return Collections.emptyList();
        }
        return startDate.datesUntil(endDate.plusDays(1)).toList();
    }
}
