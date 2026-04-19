package com.zfh.iot.modules.archive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.archive.entity.IotMeter;
import com.zfh.iot.modules.archive.mapper.IotMeterMapper;
import com.zfh.iot.modules.archive.service.IotMeterService;
import org.springframework.stereotype.Service;

@Service
public class IotMeterServiceImpl extends ServiceImpl<IotMeterMapper, IotMeter> implements IotMeterService {
}
