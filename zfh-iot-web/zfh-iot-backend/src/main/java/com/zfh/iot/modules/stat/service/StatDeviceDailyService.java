package com.zfh.iot.modules.stat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zfh.iot.modules.stat.entity.StatDeviceDaily;

public interface StatDeviceDailyService extends IService<StatDeviceDaily> {
    void generateDailyStat(Long tenantId);
}
