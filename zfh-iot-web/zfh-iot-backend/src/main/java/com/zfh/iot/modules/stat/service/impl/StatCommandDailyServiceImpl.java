package com.zfh.iot.modules.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.stat.entity.IotCommand;
import com.zfh.iot.modules.stat.entity.StatCommandDaily;
import com.zfh.iot.modules.stat.mapper.StatCommandDailyMapper;
import com.zfh.iot.modules.stat.service.StatCommandDailyService;
import com.zfh.iot.modules.stat.service.IotCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatCommandDailyServiceImpl extends ServiceImpl<StatCommandDailyMapper, StatCommandDaily>
        implements StatCommandDailyService {

    private final IotCommandService commandService;

    @Override
    public void generateDailyStat(Long tenantId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成命令每日统计: tenantId={}, date={}", tenantId, yesterday);

        LocalDateTime dayStart = yesterday.atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atStartOfDay();

        StatCommandDaily stat = new StatCommandDaily();
        stat.setTenantId(tenantId);
        stat.setStatDate(yesterday);

        LambdaQueryWrapper<IotCommand> baseWrapper = new LambdaQueryWrapper<IotCommand>()
                .eq(IotCommand::getTenantId, tenantId)
                .ge(IotCommand::getCreateTime, dayStart)
                .lt(IotCommand::getCreateTime, dayEnd);

        stat.setDeliveredCount((int) commandService.count(new LambdaQueryWrapper<IotCommand>()
                .eq(IotCommand::getTenantId, tenantId)
                .eq(IotCommand::getStatus, IotCommand.STATUS_DELIVERED)
                .ge(IotCommand::getCreateTime, dayStart)
                .lt(IotCommand::getCreateTime, dayEnd)));

        stat.setFailedCount((int) commandService.count(new LambdaQueryWrapper<IotCommand>()
                .eq(IotCommand::getTenantId, tenantId)
                .eq(IotCommand::getStatus, IotCommand.STATUS_FAILED)
                .ge(IotCommand::getCreateTime, dayStart)
                .lt(IotCommand::getCreateTime, dayEnd)));

        stat.setSuccessCount((int) commandService.count(new LambdaQueryWrapper<IotCommand>()
                .eq(IotCommand::getTenantId, tenantId)
                .eq(IotCommand::getStatus, IotCommand.STATUS_SUCCESS)
                .ge(IotCommand::getCreateTime, dayStart)
                .lt(IotCommand::getCreateTime, dayEnd)));

        stat.setOverdueCount((int) commandService.count(new LambdaQueryWrapper<IotCommand>()
                .eq(IotCommand::getTenantId, tenantId)
                .eq(IotCommand::getStatus, IotCommand.STATUS_OVERDUE)
                .ge(IotCommand::getCreateTime, dayStart)
                .lt(IotCommand::getCreateTime, dayEnd)));

        stat.setTimeoutCount((int) commandService.count(new LambdaQueryWrapper<IotCommand>()
                .eq(IotCommand::getTenantId, tenantId)
                .eq(IotCommand::getStatus, IotCommand.STATUS_TIMEOUT)
                .ge(IotCommand::getCreateTime, dayStart)
                .lt(IotCommand::getCreateTime, dayEnd)));

        stat.setCancelledCount((int) commandService.count(new LambdaQueryWrapper<IotCommand>()
                .eq(IotCommand::getTenantId, tenantId)
                .eq(IotCommand::getStatus, IotCommand.STATUS_CANCELLED)
                .ge(IotCommand::getCreateTime, dayStart)
                .lt(IotCommand::getCreateTime, dayEnd)));

        stat.setWaitingCount((int) commandService.count(new LambdaQueryWrapper<IotCommand>()
                .eq(IotCommand::getTenantId, tenantId)
                .eq(IotCommand::getStatus, IotCommand.STATUS_WAITING)
                .ge(IotCommand::getCreateTime, dayStart)
                .lt(IotCommand::getCreateTime, dayEnd)));

        stat.setSentCount((int) commandService.count(new LambdaQueryWrapper<IotCommand>()
                .eq(IotCommand::getTenantId, tenantId)
                .eq(IotCommand::getStatus, IotCommand.STATUS_SENT)
                .ge(IotCommand::getCreateTime, dayStart)
                .lt(IotCommand::getCreateTime, dayEnd)));

        save(stat);
        log.info("命令每日统计完成: tenantId={}, 已送达={}, 失败={}, 成功={}, 超期={}, 超时={}, 取消={}, 等待={}, 已发送={}",
                tenantId, stat.getDeliveredCount(), stat.getFailedCount(), stat.getSuccessCount(),
                stat.getOverdueCount(), stat.getTimeoutCount(), stat.getCancelledCount(),
                stat.getWaitingCount(), stat.getSentCount());
    }
}
