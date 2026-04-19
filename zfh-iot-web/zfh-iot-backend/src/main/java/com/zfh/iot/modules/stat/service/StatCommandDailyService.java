package com.zfh.iot.modules.stat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zfh.iot.modules.stat.entity.StatCommandDaily;

public interface StatCommandDailyService extends IService<StatCommandDaily> {
    void generateDailyStat(Long tenantId);
}
