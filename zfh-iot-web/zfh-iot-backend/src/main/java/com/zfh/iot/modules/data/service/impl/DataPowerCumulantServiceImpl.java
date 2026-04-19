package com.zfh.iot.modules.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.data.entity.DataPowerCumulant;
import com.zfh.iot.modules.data.mapper.DataPowerCumulantMapper;
import com.zfh.iot.modules.data.service.DataPowerCumulantService;
import org.springframework.stereotype.Service;

@Service
public class DataPowerCumulantServiceImpl extends ServiceImpl<DataPowerCumulantMapper, DataPowerCumulant> implements DataPowerCumulantService {
}
