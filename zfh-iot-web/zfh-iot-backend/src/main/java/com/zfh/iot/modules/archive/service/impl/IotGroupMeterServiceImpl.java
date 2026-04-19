package com.zfh.iot.modules.archive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.archive.entity.IotGroupMeter;
import com.zfh.iot.modules.archive.mapper.IotGroupMeterMapper;
import com.zfh.iot.modules.archive.service.IotGroupMeterService;
import org.springframework.stereotype.Service;

@Service
public class IotGroupMeterServiceImpl extends ServiceImpl<IotGroupMeterMapper, IotGroupMeter> implements IotGroupMeterService {
}
