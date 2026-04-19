package com.zfh.iot.modules.stat.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zfh.iot.modules.stat.entity.*;
import com.zfh.iot.modules.stat.service.*;
import com.zfh.iot.modules.system.entity.SysTenant;
import com.zfh.iot.modules.system.service.SysTenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStatJob {

    private final StatDeviceDailyService deviceDailyService;
    private final StatMessageDailyService messageDailyService;
    private final StatPushDailyService pushDailyService;
    private final StatCommandDailyService commandDailyService;
    private final SysTenantService tenantService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailyStatistics() {
        log.info("===== 开始执行每日统计任务 =====");
        long start = System.currentTimeMillis();

        List<SysTenant> tenants = tenantService.list(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getStatus, 1));

        for (SysTenant tenant : tenants) {
            try {
                deviceDailyService.generateDailyStat(tenant.getId());
            } catch (Exception e) {
                log.error("租户[{}]设备每日统计失败", tenant.getId(), e);
            }
            try {
                messageDailyService.generateDailyStat(tenant.getId());
            } catch (Exception e) {
                log.error("租户[{}]消息每日统计失败", tenant.getId(), e);
            }
            try {
                pushDailyService.generateDailyStat(tenant.getId());
            } catch (Exception e) {
                log.error("租户[{}]推送每日统计失败", tenant.getId(), e);
            }
            try {
                commandDailyService.generateDailyStat(tenant.getId());
            } catch (Exception e) {
                log.error("租户[{}]命令每日统计失败", tenant.getId(), e);
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.info("===== 每日统计任务完成，处理{}个租户，耗时: {}ms =====", tenants.size(), cost);
    }
}
