package com.zfh.iot.modules.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.data.entity.DataCumulant;
import com.zfh.iot.modules.data.mapper.DataCumulantMapper;
import com.zfh.iot.modules.data.service.DataCumulantService;
import org.springframework.stereotype.Service;

@Service
public class DataCumulantServiceImpl extends ServiceImpl<DataCumulantMapper, DataCumulant> implements DataCumulantService {
}
