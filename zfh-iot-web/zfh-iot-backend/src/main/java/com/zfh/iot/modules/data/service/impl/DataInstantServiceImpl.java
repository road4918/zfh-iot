package com.zfh.iot.modules.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.data.entity.DataInstant;
import com.zfh.iot.modules.data.mapper.DataInstantMapper;
import com.zfh.iot.modules.data.service.DataInstantService;
import org.springframework.stereotype.Service;

@Service
public class DataInstantServiceImpl extends ServiceImpl<DataInstantMapper, DataInstant> implements DataInstantService {
}
