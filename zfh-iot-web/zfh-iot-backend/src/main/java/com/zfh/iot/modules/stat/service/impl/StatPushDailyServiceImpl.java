package com.zfh.iot.modules.stat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.stat.entity.StatPushDaily;
import com.zfh.iot.modules.stat.mapper.StatPushDailyMapper;
import com.zfh.iot.modules.stat.service.StatPushDailyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
public class StatPushDailyServiceImpl extends ServiceImpl<StatPushDailyMapper, StatPushDaily>
        implements StatPushDailyService {

    @Override
    public void generateDailyStat(Long tenantId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成推送每日统计: tenantId={}, date={}", tenantId, yesterday);

        StatPushDaily stat = new StatPushDaily();
        stat.setTenantId(tenantId);
        stat.setStatDate(yesterday);
        stat.setTotalCount(0);
        stat.setSuccessCount(0);
        stat.setFailCount(0);

        save(stat);
        log.info("推送每日统计完成: tenantId={}", tenantId);
    }
}
