package com.zfh.iot.modules.stat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.stat.entity.IotCommand;
import com.zfh.iot.modules.stat.mapper.IotCommandMapper;
import com.zfh.iot.modules.stat.service.IotCommandService;
import org.springframework.stereotype.Service;

@Service
public class IotCommandServiceImpl extends ServiceImpl<IotCommandMapper, IotCommand>
        implements IotCommandService {
}
