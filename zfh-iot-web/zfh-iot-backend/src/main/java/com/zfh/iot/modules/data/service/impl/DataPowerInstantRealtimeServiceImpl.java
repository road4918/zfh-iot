package com.zfh.iot.modules.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.data.entity.DataPowerInstantRealtime;
import com.zfh.iot.modules.data.mapper.DataPowerInstantRealtimeMapper;
import com.zfh.iot.modules.data.service.DataPowerInstantRealtimeService;
import org.springframework.stereotype.Service;

@Service
public class DataPowerInstantRealtimeServiceImpl extends ServiceImpl<DataPowerInstantRealtimeMapper, DataPowerInstantRealtime> implements DataPowerInstantRealtimeService {
}
