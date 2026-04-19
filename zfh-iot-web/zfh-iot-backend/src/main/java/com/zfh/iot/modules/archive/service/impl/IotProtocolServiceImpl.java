package com.zfh.iot.modules.archive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.archive.entity.IotProtocol;
import com.zfh.iot.modules.archive.mapper.IotProtocolMapper;
import com.zfh.iot.modules.archive.service.IotProtocolService;
import org.springframework.stereotype.Service;

@Service
public class IotProtocolServiceImpl extends ServiceImpl<IotProtocolMapper, IotProtocol> implements IotProtocolService {
}
