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

    private final JdbcTemplate tdengineJdbcTemplate;

    public StatMessageDailyServiceImpl(@Qualifier("tdengineTemplate") JdbcTemplate tdengineJdbcTemplate) {
        this.tdengineJdbcTemplate = tdengineJdbcTemplate;
    }

    @Override
    public void generateDailyStat(Long tenantId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成消息每日统计: tenantId={}, date={}", tenantId, yesterday);

        long messageCount = 0;
        try {
            String sql = "SELECT COUNT(*) FROM meter_reading WHERE ts >= ? AND ts < ? AND tenant_id = ?";
            String startTs = yesterday.atStartOfDay().toString();
            String endTs = LocalDate.now().atStartOfDay().toString();
            Long count = tdengineJdbcTemplate.queryForObject(sql, Long.class, startTs, endTs, tenantId);
            messageCount = count != null ? count : 0;
        } catch (Exception e) {
            log.warn("查询TDengine消息数失败，使用0: tenantId={}, error={}", tenantId, e.getMessage());
        }

        StatMessageDaily stat = new StatMessageDaily();
        stat.setTenantId(tenantId);
        stat.setStatDate(yesterday);
        stat.setMessageCount(messageCount);
        stat.setCommandCount(0L);

        save(stat);
        log.info("消息每日统计完成: tenantId={}, 消息数={}", tenantId, messageCount);
    }
}
