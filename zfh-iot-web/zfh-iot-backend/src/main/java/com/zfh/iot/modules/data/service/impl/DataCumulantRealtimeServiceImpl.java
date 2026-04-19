package com.zfh.iot.modules.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.data.entity.DataCumulantRealtime;
import com.zfh.iot.modules.data.mapper.DataCumulantRealtimeMapper;
import com.zfh.iot.modules.data.service.DataCumulantRealtimeService;
import org.springframework.stereotype.Service;

@Service
public class DataCumulantRealtimeServiceImpl extends ServiceImpl<DataCumulantRealtimeMapper, DataCumulantRealtime> implements DataCumulantRealtimeService {
}
