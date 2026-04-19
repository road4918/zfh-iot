package com.zfh.iot.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.system.entity.SysTenant;
import com.zfh.iot.modules.system.mapper.SysTenantMapper;
import com.zfh.iot.modules.system.service.SysTenantService;
import org.springframework.stereotype.Service;

@Service
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements SysTenantService {
}
