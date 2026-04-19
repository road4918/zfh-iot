package com.zfh.iot.modules.stat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zfh.iot.modules.stat.entity.StatPushDaily;

public interface StatPushDailyService extends IService<StatPushDaily> {
    void generateDailyStat(Long tenantId);
}
