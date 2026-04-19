package com.zfh.iot.modules.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.data.entity.DataPowerInstant;
import com.zfh.iot.modules.data.mapper.DataPowerInstantMapper;
import com.zfh.iot.modules.data.service.DataPowerInstantService;
import org.springframework.stereotype.Service;

@Service
public class DataPowerInstantServiceImpl extends ServiceImpl<DataPowerInstantMapper, DataPowerInstant> implements DataPowerInstantService {
}
