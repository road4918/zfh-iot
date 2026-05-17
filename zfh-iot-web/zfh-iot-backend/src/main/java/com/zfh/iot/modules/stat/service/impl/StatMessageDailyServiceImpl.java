package com.zfh.iot.modules.stat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.stat.entity.StatMessageDaily;
import com.zfh.iot.modules.stat.mapper.StatMessageDailyMapper;
import com.zfh.iot.modules.stat.service.StatMessageDailyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
public class StatMessageDailyServiceImpl extends ServiceImpl<StatMessageDailyMapper, StatMessageDaily>
        implements StatMessageDailyService {

    @Override
    public void generateDailyStat(Long tenantId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成消息每日统计: tenantId={}, date={}", tenantId, yesterday);

        long messageCount = 0;

        StatMessageDaily stat = new StatMessageDaily();
        stat.setTenantId(tenantId);
        stat.setStatDate(yesterday);
        stat.setMessageCount(messageCount);
        stat.setCommandCount(0L);

        save(stat);
        log.info("消息每日统计完成: tenantId={}, 消息数={}", tenantId, messageCount);
    }
}
