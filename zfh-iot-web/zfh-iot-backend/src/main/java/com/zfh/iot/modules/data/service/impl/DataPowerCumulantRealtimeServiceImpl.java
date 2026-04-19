package com.zfh.iot.modules.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.data.entity.DataPowerCumulantRealtime;
import com.zfh.iot.modules.data.mapper.DataPowerCumulantRealtimeMapper;
import com.zfh.iot.modules.data.service.DataPowerCumulantRealtimeService;
import org.springframework.stereotype.Service;

@Service
public class DataPowerCumulantRealtimeServiceImpl extends ServiceImpl<DataPowerCumulantRealtimeMapper, DataPowerCumulantRealtime> implements DataPowerCumulantRealtimeService {
}
