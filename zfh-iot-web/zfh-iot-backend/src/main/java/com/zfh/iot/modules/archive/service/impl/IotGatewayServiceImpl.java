package com.zfh.iot.modules.archive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.archive.entity.IotGateway;
import com.zfh.iot.modules.archive.mapper.IotGatewayMapper;
import com.zfh.iot.modules.archive.service.IotGatewayService;
import org.springframework.stereotype.Service;

@Service
public class IotGatewayServiceImpl extends ServiceImpl<IotGatewayMapper, IotGateway> implements IotGatewayService {
}
