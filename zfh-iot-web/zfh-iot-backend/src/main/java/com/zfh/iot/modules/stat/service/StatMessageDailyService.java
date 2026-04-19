package com.zfh.iot.modules.stat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zfh.iot.modules.stat.entity.StatMessageDaily;

public interface StatMessageDailyService extends IService<StatMessageDaily> {
    void generateDailyStat(Long tenantId);
}
