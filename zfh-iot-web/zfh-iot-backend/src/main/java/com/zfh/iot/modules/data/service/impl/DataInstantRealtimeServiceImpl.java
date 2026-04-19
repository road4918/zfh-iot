package com.zfh.iot.modules.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.data.entity.DataInstantRealtime;
import com.zfh.iot.modules.data.mapper.DataInstantRealtimeMapper;
import com.zfh.iot.modules.data.service.DataInstantRealtimeService;
import org.springframework.stereotype.Service;

@Service
public class DataInstantRealtimeServiceImpl extends ServiceImpl<DataInstantRealtimeMapper, DataInstantRealtime> implements DataInstantRealtimeService {
}
