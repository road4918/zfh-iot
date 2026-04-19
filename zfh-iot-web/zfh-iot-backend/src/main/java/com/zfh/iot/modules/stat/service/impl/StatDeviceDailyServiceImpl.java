package com.zfh.iot.modules.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.archive.entity.IotMeter;
import com.zfh.iot.modules.archive.service.IotMeterService;
import com.zfh.iot.modules.stat.entity.StatDeviceDaily;
import com.zfh.iot.modules.stat.mapper.StatDeviceDailyMapper;
import com.zfh.iot.modules.stat.service.StatDeviceDailyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatDeviceDailyServiceImpl extends ServiceImpl<StatDeviceDailyMapper, StatDeviceDaily>
        implements StatDeviceDailyService {

    private final IotMeterService meterService;

    @Override
    public void generateDailyStat(Long tenantId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成设备每日统计: tenantId={}, date={}", tenantId, yesterday);

        LambdaQueryWrapper<IotMeter> baseWrapper = new LambdaQueryWrapper<IotMeter>()
                .eq(IotMeter::getTenantId, tenantId);

        long total = meterService.count(baseWrapper);
        long online = meterService.count(new LambdaQueryWrapper<IotMeter>()
                .eq(IotMeter::getTenantId, tenantId).eq(IotMeter::getStatus, 1));
        long offline = meterService.count(new LambdaQueryWrapper<IotMeter>()
                .eq(IotMeter::getTenantId, tenantId).eq(IotMeter::getStatus, 0));
        long inactive = meterService.count(new LambdaQueryWrapper<IotMeter>()
                .eq(IotMeter::getTenantId, tenantId).eq(IotMeter::getStatus, 2));
        long abnormal = meterService.count(new LambdaQueryWrapper<IotMeter>()
                .eq(IotMeter::getTenantId, tenantId).eq(IotMeter::getStatus, 3));

        StatDeviceDaily stat = new StatDeviceDaily();
        stat.setTenantId(tenantId);
        stat.setStatDate(yesterday);
        stat.setTotalCount((int) total);
        stat.setOnlineCount((int) online);
        stat.setOfflineCount((int) offline);
        stat.setInactiveCount((int) inactive);
        stat.setAbnormalCount((int) abnormal);

        BigDecimal rate = total > 0
                ? BigDecimal.valueOf(online).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        stat.setOnlineRate(rate);

        StatDeviceDaily existing = getOne(new LambdaQueryWrapper<StatDeviceDaily>()
                .eq(StatDeviceDaily::getTenantId, tenantId)
                .eq(StatDeviceDaily::getStatDate, yesterday));

        if (existing != null) {
            stat.setId(existing.getId());
            updateById(stat);
        } else {
            save(stat);
        }

        log.info("设备每日统计完成: tenantId={}, 总数={}, 在线={}, 离线={}, 异常={}, 未激活={}",
                tenantId, total, online, offline, abnormal, inactive);
    }
}
