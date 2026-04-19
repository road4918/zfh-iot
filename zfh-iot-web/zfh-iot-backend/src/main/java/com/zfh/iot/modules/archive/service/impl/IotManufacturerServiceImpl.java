package com.zfh.iot.modules.archive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.archive.entity.IotManufacturer;
import com.zfh.iot.modules.archive.mapper.IotManufacturerMapper;
import com.zfh.iot.modules.archive.service.IotManufacturerService;
import org.springframework.stereotype.Service;

@Service
public class IotManufacturerServiceImpl extends ServiceImpl<IotManufacturerMapper, IotManufacturer> implements IotManufacturerService {
}
